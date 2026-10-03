package cloud.g3h.nimbus

import cloud.g3h.nimbus.engine.LibreSpeedEngine
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Integration probe against the live default server (requires network).
 * Guards the realtime-emission contract: the engine MUST emit progress
 * during the download and upload transfers (not only at phase end), so
 * the UI can render live bandwidth. Skips itself (assumeTrue) when the
 * server is unreachable, so the builder host's CI stays green offline.
 */
class LiveEmissionTest {

    private val base = "https://nyc.speedtest.clouvider.net/backend"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Test
    fun `engine emits live progress during download and upload`() = runBlocking {
        // Skip (not fail) if the server is unreachable from this host.
        val up = runCatching {
            client.newCall(Request.Builder().url("$base/empty.php").build())
                .execute().use { it.isSuccessful }
        }.getOrDefault(false)
        assumeTrue("speed server unreachable, skipping", up)

        val engine = LibreSpeedEngine(client)
        var downEmits = 0
        var upEmits = 0
        var sawDownLive = false
        var sawUpLive = false
        var sawPing = false
        var finalPing = 0.0
        var finalMin = 0.0
        var finalLoss = 0.0
        val phases = sortedSetOf<String>()
        withTimeout(120_000) {
            engine.run(base, shortTest = true).collect { p ->
                phases += p.phase.toString()
                if (p.pingMs > 0.0) sawPing = true
                when (p.phase.toString()) {
                    "DOWNLOAD" -> { downEmits++; if (p.liveMbps > 0) sawDownLive = true }
                    "UPLOAD" -> { upEmits++; if (p.liveMbps > 0) sawUpLive = true }
                }
                if (p.done) {
                    finalPing = p.pingMs
                    finalMin = p.minPingMs
                    finalLoss = p.lossPct
                }
            }
        }
        println("PHASES=$phases downEmits=$downEmits upEmits=$upEmits sawDownLive=$sawDownLive sawUpLive=$sawUpLive sawPing=$sawPing finalPing=$finalPing minPing=$finalMin loss=$finalLoss")
        assertTrue("saw DOWNLOAD phase", phases.contains("DOWNLOAD"))
        assertTrue("saw UPLOAD phase", phases.contains("UPLOAD"))
        assertTrue("download phase emitted live progress (got $downEmits)", downEmits >= 3)
        assertTrue("upload phase emitted live progress (got $upEmits)", upEmits >= 2)
        assertTrue("download liveMbps > 0 observed", sawDownLive)
        assertTrue("upload liveMbps > 0 observed", sawUpLive)
        assertTrue("ping was measured live (sawPing=$sawPing)", sawPing)
        assertTrue("final ping > 0 (got $finalPing)", finalPing > 0)
        assertTrue("final minPing > 0 and <= ping (min=$finalMin, ping=$finalPing)", finalMin > 0 && finalMin <= finalPing + 0.0001)
        assertTrue("no packet loss (got $finalLoss%)", finalLoss < 100.0)
        assertFalse("no error phase", phases.contains("ERROR"))
    }
}
