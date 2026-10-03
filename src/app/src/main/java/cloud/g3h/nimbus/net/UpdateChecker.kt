package cloud.g3h.nimbus.net

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/**
 * In-app updates via GitHub Releases (no backend of our own).
 *
 * The "update feed" is the public GitHub API for this repo:
 *   GET https://api.github.com/repos/{REPO}/releases/latest
 * Each release published by release.sh carries the signed APK as an
 * asset named <name>-<versionName>-signed.apk. Unauthenticated reads are
 * rate-limited to 60/hour/IP — the app checks at most once per 6 h
 * (plus manual checks from Settings).
 *
 * Install: the ViewModel hands the cached APK to the installer through a
 * FileProvider content URI (required on Android 7+).
 */
object UpdateChecker {

    const val REPO = "wilson1442/nimbus-speedtest"
    const val API_LATEST = "https://api.github.com/repos/$REPO/releases/latest"
    const val ASSET_SUFFIX = "-signed.apk"
    const val CHECK_INTERVAL_MS = 6L * 3600_000

    enum class Status { IDLE, CHECKING, AVAILABLE, UP_TO_DATE, ERROR, DOWNLOADING, READY }

    /** Parsed release info. */
    data class UpdateInfo(
        val tagName: String,
        val name: String,
        val versionCode: Int,
        val body: String,
        val assetName: String?,
        val assetUrl: String?,
        val assetSize: Long
    )

    /**
     * Pure parse — unit-testable. Returns null when no signed-APK asset
     * is present or the payload is malformed.
     *
     * GitHub's API carries no versionCode of its own: release.sh writes a
     * machine-readable `nimbus-versionCode=N` line into the release body
     * and we extract it here (0 when absent → treated as "no update").
     */
    fun parseRelease(json: String): UpdateInfo? {
        return try {
            val o = JSONObject(json)
            val assets = o.optJSONArray("assets")
            if (assets == null) return null
            var name: String? = null
            var url: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val n = a.optString("name")
                if (n.endsWith(ASSET_SUFFIX)) {
                    name = n
                    url = a.optString("browser_download_url")
                    size = a.optLong("size")
                    break
                }
            }
            if (name == null || url == null) return null
            val body = o.optString("body")
            val vc = Regex("nimbus-versionCode=(\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            UpdateInfo(
                tagName = o.optString("tag_name"),
                name = o.optString("name"),
                versionCode = vc,
                body = body,
                assetName = name,
                assetUrl = url,
                assetSize = size
            )
        } catch (e: Exception) {
            null
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** One request → parse → result. Throws on transport/HTTP error. */
    fun fetchLatest(): UpdateInfo {
        val req = Request.Builder().url(API_LATEST)
            .header("User-Agent", "nimbus-android")
            .header("Accept", "application/vnd.github+json")
            .build()
        return httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            parseRelease(resp.body?.string().orEmpty())
                ?: error("no signed APK asset on the latest release")
        }
    }

    fun checkNow(onResult: (Status, UpdateInfo?, String?) -> Unit) {
        scope.launch {
            runCatching { fetchLatest() }.fold(
                onSuccess = { onResult(Status.AVAILABLE, it, null) },
                onFailure = { onResult(Status.ERROR, null, "Could not reach the update feed. Check your connection and try again.") }
            )
        }
    }

    /**
     * Streams [info]'s APK into [ctx]'s cache dir. [onProgress] receives
     * 0–99 while downloading, 100 on success, -2 on failure. The file is
     * verified (zip + AndroidManifest.xml) and atomically renamed, so a
     * crashed download never leaves a half-APK behind.
     */
    fun download(ctx: Context, info: UpdateInfo, onProgress: (pct: Int) -> Unit) {
        scope.launch {
            val url = info.assetUrl
            val finalName = info.assetName ?: "nimbus-update.apk"
            if (url == null) { onProgress(-2); return@launch }
            val tmp = File(ctx.cacheDir, "update-$finalName.part")
            val ok = runCatching {
                val req = Request.Builder().url(url).header("User-Agent", "nimbus-android").build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val body = resp.body ?: error("empty body")
                    val total = body.contentLength().coerceAtLeast(1L)
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(128 * 1024)
                        var got = 0L
                        while (true) {
                            val n = body.source().read(buf)
                            if (n == -1) break
                            out.write(buf, 0, n)
                            got += n
                            onProgress((got * 100 / total).toInt().coerceIn(0, 99))
                        }
                    }
                }
                val hasManifest = ZipFile(tmp).use { z ->
                    val e = z.entries()
                    var found = false
                    while (e.hasMoreElements() && !found) {
                        found = e.nextElement().name == "AndroidManifest.xml"
                    }
                    found
                }
                var promoted: Boolean
                if (hasManifest) {
                    val final = File(ctx.cacheDir, finalName)
                    promoted = tmp.renameTo(final)
                    if (!promoted) {
                        tmp.copyTo(final, overwrite = true)
                        promoted = true
                    }
                    if (promoted) tmp.delete()
                } else {
                    promoted = false
                }
                promoted
            }.getOrDefault(false)
            onProgress(if (ok) 100 else -2)
            if (!ok) tmp.delete()
        }
    }

    /** The completed download, if present. */
    fun cachedApk(ctx: Context, info: UpdateInfo): File? =
        File(ctx.cacheDir, info.assetName ?: "nimbus-update.apk").takeIf { it.exists() && it.length() > 102_400 }
}
