package cloud.g3h.nimbus.net

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Live connection state shown in the top status bar (§5). */
data class ConnectionState(
    val vpnActive: Boolean = false,
    val connectionType: String = "OTHER",
    val wanIp: String? = null,
    val wanIpLoading: Boolean = true,
    val online: Boolean = true
) {
    /** "Wi‑Fi 5 GHz", "Wi‑Fi 2.4 GHz", "Wi‑Fi", "Ethernet" or "Other". */
    fun connectionLabel(): String = when (connectionType) {
        "ETHERNET" -> "Ethernet"
        "WIFI_5GHZ" -> "Wi‑Fi 5 GHz"
        "WIFI_2_4GHZ" -> "Wi‑Fi 2.4 GHz"
        "WIFI" -> "Wi‑Fi"
        else -> "Other"
    }
}

/**
 * Watches the default network for VPN transport, Wi‑Fi band and connectivity,
 * and fetches the WAN IP from a plain-text HTTPS endpoint.
 */
class ConnectionMonitor(context: Context) {

    private val context = context.applicationContext
    private val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
        as android.net.ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE)
        as android.net.wifi.WifiManager

    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state

    private val callback = object : android.net.ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            _state.update { it.copy(online = true, wanIpLoading = true) }
            refreshVpn(network)
            refreshConnectionType(network)
            fetchWanIp(context)
        }

        override fun onLosing(network: android.net.Network, millisUntilLost: Int) {}

        override fun onLost(network: android.net.Network) {
            _state.update { it.copy(online = false) }
        }

        override fun onCapabilitiesChanged(
            network: android.net.Network,
            caps: android.net.NetworkCapabilities
        ) {
            refreshVpn(network)
            refreshConnectionType(network)
        }
    }

    fun start() {
        @Suppress("DEPRECATION")
        cm.registerDefaultNetworkCallback(callback)
        refreshVpn(cm.activeNetwork)
        refreshConnectionType(cm.activeNetwork)
        fetchWanIp(context)
    }

    fun stop() {
        runCatching { cm.unregisterNetworkCallback(callback) }
    }

    private fun refreshVpn(network: android.net.Network?) {
        val vpn = network?.let {
            @Suppress("DEPRECATION")
            cm.getNetworkCapabilities(it)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
        } ?: false
        _state.update { it.copy(vpnActive = vpn) }
    }

    private fun refreshConnectionType(network: android.net.Network?) {
        val caps = network?.let { runCatching { cm.getNetworkCapabilities(it) }.getOrNull() } ?: return
        val type = when {
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ->
                @Suppress("DEPRECATION")
                runCatching {
                    val freq = wifiManager.connectionInfo.frequency
                    if (freq >= 4900) "WIFI_5GHZ" else if (freq > 0) "WIFI_2_4GHZ" else "WIFI"
                }.getOrDefault("WIFI")
            else -> "OTHER"
        }
        _state.update { it.copy(connectionType = type) }
    }

    /** Plain-text GET; refreshes the WAN IP (VPN exit IP when a VPN is active). */
    fun fetchWanIp(ctx: Context) {
        Thread {
            val endpoint = kotlinx.coroutines.runBlocking { SettingsStore.ipLookupUrl(ctx) }
            if (endpoint.isBlank()) { _state.update { it.copy(wanIpLoading = false) }; return@Thread }
            runCatching {
                val conn = java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val ip = if (conn.responseCode == 200)
                    conn.inputStream.bufferedReader().readText().trim().takeIf { it.isNotEmpty() }
                else null
                conn.disconnect()
                if (ip != null) _state.update { it.copy(wanIp = ip, wanIpLoading = false) }
                else _state.update { it.copy(wanIpLoading = false) }
            }.onFailure {
                _state.update { it.copy(wanIpLoading = false) }
            }
        }.start()
    }
}
