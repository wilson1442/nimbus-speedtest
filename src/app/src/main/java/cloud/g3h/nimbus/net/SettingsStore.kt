package cloud.g3h.nimbus.net

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore by preferencesDataStore(name = "nimbus_settings")

// Typed DataStore keys.
val KEY_SERVER_URL = stringPreferencesKey("server_url")
val KEY_IP_LOOKUP_URL = stringPreferencesKey("ip_lookup_url")
val KEY_DURATION_SHORT = booleanPreferencesKey("duration_short")

/**
 * User settings (DataStore, per spec §3.5): speed-test server URL, IP lookup URL,
 * test duration. Build-time BuildConfig defaults act as fallbacks.
 */
object SettingsStore {

    /** Configured speed-test server base URL; blank = unconfigured. */
    suspend fun serverUrl(ctx: Context): String {
        val stored = ctx.settingsDataStore.data.first()[KEY_SERVER_URL]
        val v = (stored ?: cloud.g3h.nimbus.BuildConfig.API_BASE_URL).orEmpty().trim()
        return v.trimEnd('/')
    }

    suspend fun ipLookupUrl(ctx: Context): String {
        val stored = ctx.settingsDataStore.data.first()[KEY_IP_LOOKUP_URL]
        val v = (stored ?: cloud.g3h.nimbus.BuildConfig.IP_LOOKUP_URL).orEmpty().trim()
        return v
    }

    /** true = short test, false = normal (default). */
    suspend fun shortDuration(ctx: Context): Boolean =
        ctx.settingsDataStore.data.first()[KEY_DURATION_SHORT] ?: false

    suspend fun setServerUrl(ctx: Context, value: String) = ctx.settingsDataStore.edit {
        it[KEY_SERVER_URL] = value.trim().trimEnd('/')
    }

    suspend fun setIpLookupUrl(ctx: Context, value: String) = ctx.settingsDataStore.edit {
        it[KEY_IP_LOOKUP_URL] = value.trim()
    }

    suspend fun setShortDuration(ctx: Context, value: Boolean) = ctx.settingsDataStore.edit {
        it[KEY_DURATION_SHORT] = value
    }
}
