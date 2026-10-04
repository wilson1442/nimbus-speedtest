package cloud.g3h.nimbus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.g3h.nimbus.NimbusApp
import cloud.g3h.nimbus.data.Phase
import cloud.g3h.nimbus.data.RangeStats
import cloud.g3h.nimbus.data.TestResult
import cloud.g3h.nimbus.engine.SpeedProgress
import cloud.g3h.nimbus.net.ConnectionMonitor
import cloud.g3h.nimbus.net.ConnectionState
import cloud.g3h.nimbus.net.SettingsStore
import cloud.g3h.nimbus.net.UpdateChecker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class Screen { HOME, TESTING, RESULTS, HISTORY, SETTINGS }

/** What the running-test screen renders. */
data class TestUiState(
    val running: Boolean = false,
    val phase: Phase = Phase.PING,
    val liveMbps: Double = 0.0,
    val peakMbps: Double = 0.0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val pingMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val lossPct: Double = 0.0,
    val elapsedMs: Long = 0,
    val pingDone: Boolean = false,
    val downloadDone: Boolean = false,
    val uploadDone: Boolean = false,
    val pingSamples: List<Double> = emptyList(),
    val downloadSamples: List<Double> = emptyList(),
    val uploadSamples: List<Double> = emptyList(),
    val minPingMs: Double = 0.0,
    val pingError: String? = null,
    val error: String? = null
)

/** A finished run handed to the Results screen. */
data class ResultUiState(
    val result: TestResult,
    val pingSamples: List<Double>,
    val downloadSamples: List<Double>,
    val uploadSamples: List<Double>,
    val saved: Boolean
)

data class HistoryUiState(
    val rangeDays: Int = 30,          // 7, 30, 90, 0 = all
    val excludeVpn: Boolean = false,
    val loading: Boolean = false,
    val stats: RangeStats? = null,
    val results: List<TestResult> = emptyList(),
    val consistencyPct: Double = 0.0,
    val confirmClear: Boolean = false
)

data class SettingsUiState(
    val serverUrl: String = "",
    val ipLookupUrl: String = "",
    val shortDuration: Boolean = false,
    val autoUpdate: Boolean = true,
    val versionName: String = cloud.g3h.nimbus.BuildConfig.VERSION_NAME,
    // update-check state
    val updateStatus: UpdateChecker.Status = UpdateChecker.Status.IDLE,
    val updateInfo: UpdateChecker.UpdateInfo? = null,
    val updateError: String? = null,
    val downloadPct: Int = -1
)

/** Server reachability probe result (Settings → Test connection). */
data class ProbeUiState(
    val checking: Boolean = false,
    val ok: Boolean = false,
    val bestMs: Double = 0.0,
    val error: String? = null
)

class NimbusViewModel(private val app: NimbusApp) : ViewModel() {

    private val db = app.database
    private val engine = app.speedTestEngine
    private val monitor = app.connectionMonitor

    val screen = MutableStateFlow(Screen.HOME)
    val conn = monitor.state

    // ---- Home ----
    val lastTest = MutableStateFlow<TestResult?>(null)
    val avg30 = MutableStateFlow<RangeStats?>(null)

    // ---- Test ----
    val test = MutableStateFlow(TestUiState())
    private var testJob: Job? = null

    // ---- Results ----
    val result = MutableStateFlow<ResultUiState?>(null)

    // ---- History ----
    val history = MutableStateFlow(HistoryUiState())

    // ---- Settings ----
    val settings = MutableStateFlow(SettingsUiState())
    val probe = MutableStateFlow(ProbeUiState())

    init {
        refreshHome()
        refreshHistory()
        viewModelScope.launch {
            app.applicationContext.let { ctx ->
                val sUrl = SettingsStore.serverUrl(ctx)
                val ip = SettingsStore.ipLookupUrl(ctx)
                val short = SettingsStore.shortDuration(ctx)
                val auto = SettingsStore.autoUpdate(ctx)
                settings.update {
                    it.copy(
                        serverUrl = sUrl,
                        ipLookupUrl = ip,
                        shortDuration = short,
                        autoUpdate = auto,
                        versionName = cloud.g3h.nimbus.BuildConfig.VERSION_NAME
                    )
                }
            }
        }
        // Check for a newer release on launch (throttled, honours the pref).
        autoCheckForUpdates()
    }

    // ---------- Navigation ----------
    fun navigate(s: Screen) {
        screen.value = s
        if (s == Screen.HOME) refreshHome()
        if (s == Screen.HISTORY) refreshHistory()
    }

    // ---------- Home data ----------
    private fun refreshHome() {
        viewModelScope.launch {
            lastTest.value = db.testResultDao().latest()
            val since30 = System.currentTimeMillis() - 30L * 86_400_000
            avg30.value = db.testResultDao().stats(since30, null)
        }
    }

    // ---------- Test lifecycle ----------
    fun startTest() {
        if (test.value.running) return
        testJob?.cancel()
        test.value = TestUiState(running = true)
        screen.value = Screen.TESTING
        testJob = viewModelScope.launch {
            val ctx = app.applicationContext
            val serverUrl = SettingsStore.serverUrl(ctx)
            val short = SettingsStore.shortDuration(ctx)
            monitor.fetchWanIp(ctx)
            if (serverUrl.isBlank()) {
                test.update { it.copy(running = false, error = "No speed-test server configured. Open Settings first.") }
                return@launch
            }
            try {
                var final: SpeedProgress? = null
                engine.run(serverUrl, short).collect { p ->
                    applyProgress(p)
                    if (p.done || p.error != null) final = p
                }
                val done = final ?: return@launch
                if (done.error != null) {
                    test.update { it.copy(running = false, error = done.error) }
                    return@launch
                }
                val connNow = conn.first()
                val saved = db.testResultDao().insert(
                    TestResult(
                        timestamp = System.currentTimeMillis(),
                        downloadMbps = done.downloadMbps,
                        uploadMbps = done.uploadMbps,
                        pingMs = done.pingMs,
                        jitterMs = done.jitterMs,
                        packetLossPct = done.lossPct,
                        minPingMs = done.minPingMs,
                        peakMbps = done.peakMbps,
                        connectionType = connNow.connectionType,
                        vpnActive = connNow.vpnActive,
                        wanIp = connNow.wanIp,
                        serverName = hostOf(serverUrl)
                    )
                )
                val outcome = TestResult(
                    id = saved,
                    timestamp = System.currentTimeMillis(),
                    downloadMbps = done.downloadMbps,
                    uploadMbps = done.uploadMbps,
                    pingMs = done.pingMs,
                    jitterMs = done.jitterMs,
                    packetLossPct = done.lossPct,
                    minPingMs = done.minPingMs,
                    peakMbps = done.peakMbps,
                    connectionType = connNow.connectionType,
                    vpnActive = connNow.vpnActive,
                    wanIp = connNow.wanIp,
                    serverName = hostOf(serverUrl)
                )
                result.value = ResultUiState(
                    result = outcome,
                    pingSamples = done.pingSamples,
                    downloadSamples = done.downloadSamples,
                    uploadSamples = done.uploadSamples,
                    saved = true
                )
                test.update {
                    it.copy(running = false,
                        pingDone = true, downloadDone = true, uploadDone = true,
                        pingMs = done.pingMs, jitterMs = done.jitterMs, lossPct = done.lossPct,
                        pingSamples = done.pingSamples,
                        downloadSamples = done.downloadSamples,
                        uploadSamples = done.uploadSamples,
                        minPingMs = done.minPingMs,
                        peakMbps = done.peakMbps,
                        pingError = if (done.minPingMs <= 0.0) done.pingError else null,
                        liveMbps = done.uploadMbps,
                        downloadMbps = done.downloadMbps,
                        uploadMbps = done.uploadMbps)
                }
                refreshHome()
                refreshHistory()
                screen.value = Screen.RESULTS
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                test.update { it.copy(running = false, error = e.message ?: "speed test failed") }
            }
        }
    }

    /** BACK on the test screen: cancel, discard, go home. */
    fun cancelTest() {
        testJob?.cancel()
        testJob = null
        test.update { it.copy(running = false) }
        screen.value = Screen.HOME
    }

    /** Called by the flow collector in the test screen; mirrors engine progress. */
    fun applyProgress(p: SpeedProgress) {
        test.update {
            it.copy(
                phase = p.phase,
                liveMbps = p.liveMbps,
                peakMbps = p.peakMbps,
                downloadMbps = p.downloadMbps,
                uploadMbps = p.uploadMbps,
                pingMs = p.pingMs, jitterMs = p.jitterMs, lossPct = p.lossPct,
                elapsedMs = p.elapsedMs,
                pingDone = p.pingMs > 0,
                downloadDone = p.downloadSamples.isNotEmpty(),
                uploadDone = p.uploadSamples.isNotEmpty(),
                pingSamples = p.pingSamples,
                downloadSamples = p.downloadSamples,
                uploadSamples = p.uploadSamples,
                minPingMs = p.minPingMs,
                pingError = if (p.minPingMs <= 0.0) p.pingError else null
            )
        }
    }

    // ---------- History ----------
    private fun refreshHistory() {
        val h = history.value
        viewModelScope.launch {
            history.update { it.copy(loading = true) }
            val sinceMs = if (h.rangeDays == 0) 0L
            else System.currentTimeMillis() - h.rangeDays.toLong() * 86_400_000
            val list = db.testResultDao().since(sinceMs)
            val pool = if (h.excludeVpn) list.filter { !it.vpnActive } else list
            val stats = if (pool.isEmpty()) null else {
                val avgDown = pool.map { it.downloadMbps }.average()
                val avgUp = pool.map { it.uploadMbps }.average()
                val avgPing = pool.map { it.pingMs }.average()
                RangeStats(
                    avgDownloadMbps = avgDown,
                    avgUploadMbps = avgUp,
                    avgPingMs = avgPing,
                    count = pool.size,
                    minDownloadMbps = pool.minOf { it.downloadMbps },
                    maxDownloadMbps = pool.maxOf { it.downloadMbps }
                )
            }
            val within = if (stats != null) pool.count { Math.abs(it.downloadMbps - stats.avgDownloadMbps) <= stats.avgDownloadMbps * 0.15 } else 0
            val consistency = if (stats != null && stats.count > 0) within * 100.0 / stats.count else 0.0
            history.update {
                it.copy(
                    loading = false,
                    stats = stats,
                    results = list,
                    consistencyPct = consistency
                )
            }
        }
    }

    fun setRange(days: Int) {
        history.update { it.copy(rangeDays = days, confirmClear = false) }
        refreshHistory()
    }

    fun toggleExcludeVpn() {
        history.update { it.copy(excludeVpn = !it.excludeVpn, confirmClear = false) }
        refreshHistory()
    }

    fun askClearHistory() { history.update { it.copy(confirmClear = true) } }

    fun confirmClearHistory() {
        viewModelScope.launch {
            db.testResultDao().deleteAll()
            history.update { it.copy(confirmClear = false) }
            refreshHome()
            refreshHistory()
        }
    }

    fun dismissClearHistory() { history.update { it.copy(confirmClear = false) } }

    // ---------- Settings ----------
    fun setServerUrl(v: String) {
        settings.update { it.copy(serverUrl = v) }
        viewModelScope.launch { SettingsStore.setServerUrl(app.applicationContext, v) }
    }

    fun setIpLookupUrl(v: String) {
        settings.update { it.copy(ipLookupUrl = v) }
        viewModelScope.launch { SettingsStore.setIpLookupUrl(app.applicationContext, v) }
    }

    fun toggleShortDuration() {
        val v = !settings.value.shortDuration
        settings.update { it.copy(shortDuration = v) }
        viewModelScope.launch { SettingsStore.setShortDuration(app.applicationContext, v) }
    }

    /** Probes the configured server (or a candidate) — why the ping phase fails. */
    fun testServer(candidateUrl: String? = null) {
        val url = (candidateUrl ?: settings.value.serverUrl).trim()
        if (url.isBlank()) {
            probe.value = ProbeUiState(ok = false, error = "No server entered")
            return
        }
        probe.value = ProbeUiState(checking = true)
        viewModelScope.launch {
            val p = runCatching { engine.probeServer(url) }.getOrElse {
                cloud.g3h.nimbus.engine.ServerProbe(false, 0.0, it.message ?: "probe failed")
            }
            probe.value = ProbeUiState(ok = p.ok, bestMs = p.bestMs, error = p.error)
        }
    }

    private fun hostOf(url: String): String =
        runCatching { java.net.URI(url.trimEnd('/').replaceFirst("https://", "").replaceFirst("http://", "")).host }
            .getOrNull() ?: url

    // ---------- Back / exit handling ----------
    /**
     * Set by a screen while a modal dialog is open; onBack() routes BACK
     * to it first (dismiss the dialog) before navigating.
     */
    var dialogBack: (() -> Unit)? = null

    /**
     * TV-style BACK: walk the in-app hierarchy instead of exiting.
     *   dialog open      -> dismiss the dialog
     *   running test     -> cancel and go home
     *   RESULTS/HISTORY/SETTINGS -> home
     *   HOME             -> no-op: the app stays open (the launcher's
     *                        HOME key is the way out; BACK never kills us)
     */
    fun onBack() {
        if (history.value.confirmClear) {
            dismissClearHistory()
            return
        }
        dialogBack?.let { back -> back(); return }
        when (screen.value) {
            Screen.HOME -> Unit
            Screen.TESTING -> cancelTest()
            Screen.RESULTS, Screen.HISTORY, Screen.SETTINGS -> navigate(Screen.HOME)
        }
    }

    // ---------- Updates ----------

    /** Manual check (Settings → Check / Retry). */
    fun checkForUpdates() = runUpdateCheck(auto = false)

    /**
     * Launch-time check: throttled to [UpdateChecker.CHECK_INTERVAL_MS] and
     * skipped entirely when the user turned auto-update off. When a newer
     * release exists the whole chain runs unattended (check → download →
     * hand to the system installer); the user only confirms in the installer.
     */
    private fun autoCheckForUpdates() {
        viewModelScope.launch {
            val ctx = app.applicationContext
            if (!SettingsStore.autoUpdate(ctx)) return@launch
            val last = SettingsStore.lastUpdateCheck(ctx)
            if (!UpdateChecker.isCheckDue(last, System.currentTimeMillis())) return@launch
            SettingsStore.setLastUpdateCheck(ctx, System.currentTimeMillis())
            runUpdateCheck(auto = true)
        }
    }

    private fun runUpdateCheck(auto: Boolean) {
        val cur = cloud.g3h.nimbus.BuildConfig.VERSION_CODE
        settings.update { it.copy(updateStatus = UpdateChecker.Status.CHECKING, updateError = null) }
        UpdateChecker.checkNow { status, info, err ->
            when {
                status == UpdateChecker.Status.ERROR ->
                    settings.update { it.copy(updateStatus = status, updateError = err, updateInfo = null, downloadPct = -1) }
                info != null && info.versionCode > cur -> {
                    settings.update { it.copy(updateStatus = UpdateChecker.Status.AVAILABLE, updateInfo = info, updateError = null, downloadPct = -1) }
                    if (auto) downloadUpdate(auto = true)
                }
                else ->
                    settings.update { it.copy(updateStatus = UpdateChecker.Status.UP_TO_DATE, updateInfo = null, updateError = null, downloadPct = -1) }
            }
        }
    }

    /** [auto] = start the installer as soon as the download finishes. */
    fun downloadUpdate(auto: Boolean = false) {
        val info = settings.value.updateInfo ?: return
        settings.update { it.copy(updateStatus = UpdateChecker.Status.DOWNLOADING, downloadPct = 0) }
        UpdateChecker.download(app.applicationContext, info) { pct ->
            when {
                pct == 100 -> {
                    settings.update { it.copy(updateStatus = UpdateChecker.Status.READY, downloadPct = 100) }
                    if (auto) viewModelScope.launch { installUpdate() }
                }
                pct >= 0 -> settings.update { it.copy(downloadPct = pct) }
                else -> settings.update { it.copy(updateStatus = UpdateChecker.Status.ERROR, updateError = "Download failed. Try again.", downloadPct = -1) }
            }
        }
    }

    /** Auto-update preference; re-enabling allows an immediate check. */
    fun setAutoUpdate(v: Boolean) {
        settings.update { it.copy(autoUpdate = v) }
        viewModelScope.launch {
            val ctx = app.applicationContext
            SettingsStore.setAutoUpdate(ctx, v)
            if (v) {
                SettingsStore.setLastUpdateCheck(ctx, 0L)
                autoCheckForUpdates()
            }
        }
    }

    /** Hands the cached APK to the system installer (FileProvider URI). */
    fun installUpdate() {
        val info = settings.value.updateInfo ?: return
        val file = UpdateChecker.cachedApk(app.applicationContext, info) ?: return
        val ctx = app.applicationContext

        // Android 8+: the installer refuses APKs from an app that isn't allowed
        // to install unknown apps. Send the user straight to that toggle instead
        // of failing silently.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            !ctx.packageManager.canRequestPackageInstalls()
        ) {
            runCatching {
                ctx.startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        android.net.Uri.parse("package:${ctx.packageName}")
                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(intent)
        } catch (e: Exception) {
            // No chooser on some TV boxes — fall back to the release page in a browser.
            runCatching {
                ctx.startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/wilson1442/nimbus-speedtest/releases")
                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        testJob?.cancel()
    }
}
