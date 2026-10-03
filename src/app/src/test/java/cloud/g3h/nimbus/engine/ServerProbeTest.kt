package cloud.g3h.nimbus.engine

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Server reachability probe (Settings → Test):
 *  - unreachable host  -> ok=false AND a human-usable error reason
 *  - reachable host    -> ok=true with a positive latency (needs network, skips otherwise)
 */
class ServerProbeTest {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Test
    fun probe_reports_reason_when_host_unreachable() = runBlocking {
        // port 9 (discard) on loopback: instant connection-refused, no network needed
        val p = withTimeout(15_000) { LibreSpeedEngine(client).probeServer("http://127.0.0.1:9/backend") }
        println("UNREACHABLE probe -> ok=${p.ok} error=${p.error}")
        assertFalse("unreachable host must not report ok", p.ok)
        assertNotNull("failure must carry a reason", p.error)
        assertTrue("reason must be non-empty", (p.error ?: "").isNotBlank())
    }

    @Test
    fun probe_reports_latency_when_reachable() = runBlocking {
        val base = "https://nyc.speedtest.clouvider.net/backend"
        val up = runCatching {
            client.newCall(okhttp3.Request.Builder().url("$base/empty.php").build())
                .execute().use { it.isSuccessful }
        }.getOrDefault(false)
        assumeTrue("speed server unreachable, skipping", up)

        val p = withTimeout(30_000) { LibreSpeedEngine(client).probeServer(base) }
        println("REACHABLE probe -> ok=${p.ok} best=${p.bestMs}ms error=${p.error}")
        assertTrue("reachable host must report ok", p.ok)
        assertTrue("best latency must be positive (got ${p.bestMs})", p.bestMs in 0.1..60_000.0)
        assertFalse("no error on success", p.error != null)
    }
}
