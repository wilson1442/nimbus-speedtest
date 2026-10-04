package cloud.g3h.nimbus.net

import androidx.annotation.DrawableRes
import cloud.g3h.nimbus.R

/** A user-selectable public LibreSpeed-compatible backend. */
data class SpeedServer(
    val label: String,
    val region: String,
    val url: String,
    @DrawableRes val flag: Int
) {
    /** "New York" from "New York, US" — for the tight chips on the Home screen. */
    val shortName: String get() = label.substringBefore(',')
}

/**
 * Curated public LibreSpeed-compatible backends, so the user can pick a nearby
 * region instead of typing a URL.
 *
 * Every entry was probed 2026-10-03 from the build host and passed the engine's
 * full contract: ping `GET empty.php` → 200, download `GET garbage.php?ckSize=1`
 * → 200 with a complete 1,048,576-byte body, upload `POST empty.php` → 200.
 * A candidate that returns 200 with an empty/short body is NOT usable and must
 * not be listed. (Helsinki/librespeed.fi returned 503 and was excluded.)
 *
 * The build-time default (BuildConfig.API_BASE_URL) is New York; a user choice
 * in Settings always wins over it.
 */
object SpeedServers {

    /** Flag shown for a manually-entered endpoint with no country of its own. */
    @DrawableRes val CUSTOM_FLAG: Int = R.drawable.ic_globe

    val ALL: List<SpeedServer> = listOf(
        // US — East
        SpeedServer("New York, US",     "US · East",    "https://nyc.speedtest.clouvider.net/backend",  R.drawable.flag_us),
        SpeedServer("Atlanta, US",      "US · East",    "https://atl.speedtest.clouvider.net/backend",  R.drawable.flag_us),
        // US — Central
        SpeedServer("Chicago, US",      "US · Central", "https://chispeed.sharktech.net/backend",       R.drawable.flag_us),
        SpeedServer("Denver, US",       "US · Central", "https://denspeed.sharktech.net/backend",       R.drawable.flag_us),
        SpeedServer("Grand Rapids, US", "US · Central", "https://mispeed.rackgenius.com/backend",       R.drawable.flag_us),
        // US — West
        SpeedServer("Los Angeles, US",  "US · West",    "https://la.speedtest.clouvider.net/backend",   R.drawable.flag_us),
        SpeedServer("Las Vegas, US",    "US · West",    "https://lasspeed.sharktech.net/backend",       R.drawable.flag_us),
        // Europe
        SpeedServer("London, UK",       "Europe",       "https://lon.speedtest.clouvider.net/backend",  R.drawable.flag_gb),
        SpeedServer("Amsterdam, NL",    "Europe",       "https://ams.speedtest.clouvider.net/backend",  R.drawable.flag_nl),
        SpeedServer("Frankfurt, DE",    "Europe",       "https://fra.speedtest.clouvider.net/backend",  R.drawable.flag_de),
        SpeedServer("Prague, CZ",       "Europe",       "https://librespeed.turris.cz/backend",         R.drawable.flag_cz),
        // Asia
        SpeedServer("Tokyo, JP",        "Asia",         "https://librespeed.a573.net/backend",          R.drawable.flag_jp)
    )

    /** The preset matching [url], or null for a custom/blank URL. */
    fun forUrl(url: String): SpeedServer? {
        val u = url.trim().trimEnd('/')
        return ALL.firstOrNull { it.url.trimEnd('/').equals(u, ignoreCase = true) }
    }

    /** Friendly display name for any configured URL (preset label, else the host). */
    fun displayName(url: String): String {
        if (url.isBlank()) return "Auto (nearest)"
        forUrl(url)?.let { return it.label }
        return runCatching {
            java.net.URI(if (url.startsWith("http")) url else "https://$url").host ?: url
        }.getOrDefault(url)
    }
}
