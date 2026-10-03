package cloud.g3h.nimbus.engine

import cloud.g3h.nimbus.data.Phase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * LibreSpeed-compatible engine (spec §6):
 *  - ping:   GET  {base}/empty.php              — 10 sequential requests
 *  - down:   GET  {base}/garbage.php?ckSize=100 — 4–6 parallel streams, ~12 s
 *  - up:     POST {base}/empty.php              — 3–4 parallel streams, ~10 s
 *
 * The first 2 s of each transfer's ramp-up are discarded from the averaged
 * sample. Cancelling the collecting coroutine cancels the whole run.
 */
class LibreSpeedEngine(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : SpeedTestEngine {

    private val sampleIntervalMs = 200L
    private val rampMs = 2000L

    override fun run(serverBaseUrl: String, shortTest: Boolean): Flow<SpeedProgress> =
        callbackFlow {
            val producer = this
            val base = serverBaseUrl.trimEnd('/')
            val startedAt = System.currentTimeMillis()
            val pingSamples = mutableListOf<Double>()
            val downloadSamples = mutableListOf<Double>()
            val uploadSamples = mutableListOf<Double>()
            var peak = 0.0
            var progress = SpeedProgress(elapsedMs = 0)
            var pingMs = 0.0; var jitterMs = 0.0; var lossPct = 0.0
            var minPing = 0.0
            var downMean = 0.0; var upMean = 0.0
            var pingFailReason: String? = null

            try {
                // ---------------- PING ----------------
                val raw = ArrayList<Double?>()
                repeat(10) {
                    ensureActive()
                    raw.add(timeGet("$base/empty.php"))
                    val m = PingMath.compute(raw)
                    raw.last()?.let { pingSamples.add(it) }
                    if (raw.last() == null) pingFailReason = lastPingError
                    pingMs = m.pingMs; jitterMs = m.jitterMs; lossPct = m.lossPct
                    if (m.pingMs > 0.0) minPing = if (minPing == 0.0) m.pingMs else minOf(minPing, m.pingMs)
                    progress = progress.copy(
                        phase = Phase.PING,
                        pingMs = pingMs, jitterMs = jitterMs, lossPct = lossPct,
                        pingSamples = pingSamples.toList(),
                        minPingMs = minPing,
                        serverReachable = minPing > 0.0,
                        pingError = pingFailReason,
                        elapsedMs = System.currentTimeMillis() - startedAt
                    )
                    producer.trySend(progress)
                    delay(100)
                }

                // ---------------- DOWNLOAD ----------------
                progress = runTransfer(
                    url = "$base/garbage.php?ckSize=100",
                    durationMs = (if (shortTest) 6L else 12L) * 1000,
                    streams = if (shortTest) 4 else 6,
                    upload = false,
                    samples = downloadSamples
                ) { live, mean ->
                    peak = maxOf(peak, live)
                    downMean = mean
                    val p = progress.copy(
                        phase = Phase.DOWNLOAD,
                        liveMbps = live, peakMbps = peak, downloadMbps = mean,
                        downloadSamples = downloadSamples.toList(),
                        pingMs = pingMs, jitterMs = jitterMs, lossPct = lossPct,
                        elapsedMs = System.currentTimeMillis() - startedAt
                    )
                    producer.trySend(p)
                    p
                }.also { final -> producer.trySend(final) }

                // ---------------- UPLOAD ----------------
                progress = runTransfer(
                    url = "$base/empty.php",
                    durationMs = (if (shortTest) 5L else 10L) * 1000,
                    streams = if (shortTest) 3 else 4,
                    upload = true,
                    samples = uploadSamples
                ) { live, mean ->
                    peak = maxOf(peak, live)
                    upMean = mean
                    val p = progress.copy(
                        phase = Phase.UPLOAD,
                        liveMbps = live, peakMbps = peak, uploadMbps = mean,
                        uploadSamples = uploadSamples.toList(),
                        pingMs = pingMs, jitterMs = jitterMs, lossPct = lossPct,
                        elapsedMs = System.currentTimeMillis() - startedAt
                    )
                    producer.trySend(p)
                    p
                }.also { final -> producer.trySend(final) }

                progress = progress.copy(
                    done = true,
                    pingMs = pingMs, jitterMs = jitterMs, lossPct = lossPct,
                    downloadMbps = downMean, uploadMbps = upMean,
                    minPingMs = minPing,
                    serverReachable = minPing > 0.0,
                    pingError = pingFailReason,
                    elapsedMs = System.currentTimeMillis() - startedAt
                )
                producer.trySend(progress)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                producer.trySend(progress.copy(error = t.message ?: "speed test failed"))
            } finally {
                close()
            }
        }

    /**
     * Runs [streams] parallel workers against [url] for [durationMs]. Every
     * [sampleIntervalMs] it measures aggregate throughput, appends the value
     * to [samples] (once past the ramp-up window) and calls [onSample]; the
     * returned [SpeedProgress] is the last one built.
     */
    private suspend fun runTransfer(
        url: String,
        durationMs: Long,
        streams: Int,
        upload: Boolean,
        samples: MutableList<Double>,
        onSample: (liveMbps: Double, meanMbps: Double) -> SpeedProgress
    ): SpeedProgress {
        val totalBytes = AtomicLong(0)
        var last = SpeedProgress()
        val phaseStart = System.currentTimeMillis()
        val stopAt = phaseStart + durationMs
        val chunk = ByteArray(64 * 1024).also { SecureRandom().nextBytes(it) }
        val upBody = chunk.toRequestBody("application/octet-stream".toMediaType())
        val downReq = Request.Builder().url(url).build()
        val upReq = Request.Builder().url(url).post(upBody).build()

        supervisorScope {
            repeat(streams) {
                val job = launch(Dispatchers.IO) {
                    if (upload) {
                        while (isActive && System.currentTimeMillis() < stopAt) {
                            runCatching {
                                httpClient.newCall(upReq).execute().use {
                                    if (it.isSuccessful) totalBytes.addAndGet(chunk.size.toLong())
                                }
                            }
                        }
                    } else {
                        while (isActive && System.currentTimeMillis() < stopAt) {
                            try {
                                httpClient.newCall(downReq).execute().use { resp ->
                                    if (!resp.isSuccessful) return@use
                                    val body = resp.body ?: return@use
                                    val source = body.source()
                                    val buf = ByteArray(256 * 1024)
                                    var n = source.read(buf)
                                    while (n > 0 && isActive && System.currentTimeMillis() < stopAt) {
                                        totalBytes.addAndGet(n.toLong())
                                        n = source.read(buf)
                                    }
                                }
                            } catch (e: Exception) {
                                delay(200)
                            }
                        }
                    }
                }
                if (System.currentTimeMillis() >= stopAt) job.cancelAndJoin()
            }

            // Sampling loop (runs on the producer thread).
            var lastBytes = 0L
            while (isActive && System.currentTimeMillis() < stopAt) {
                delay(sampleIntervalMs)
                ensureActive()
                val now = System.currentTimeMillis()
                val bytes = totalBytes.get()
                val mbps = (bytes - lastBytes) * 8.0 / (sampleIntervalMs / 1000.0) / 1_000_000.0
                lastBytes = bytes
                if (now - phaseStart > rampMs && mbps > 0) samples.add(mbps)
                val mean = if (samples.isEmpty()) 0.0 else samples.sum() / samples.size
                last = onSample(mbps, mean)
            }
        }
        return last
    }

    private var lastPingError: String? = null

    override suspend fun probeServer(serverBaseUrl: String): ServerProbe {
        val base = serverBaseUrl.trimEnd('/')
        var best = Double.MAX_VALUE
        var lastError: String? = null
        repeat(3) {
            val t = timeGet("$base/empty.php")
            if (t != null) best = minOf(best, t) else lastError = lastPingError
        }
        return if (best != Double.MAX_VALUE) ServerProbe(true, best, null)
        else ServerProbe(false, 0.0, lastError ?: "unknown")
    }

    private suspend fun timeGet(url: String): Double? =
        runCatching {
            val t0 = System.nanoTime()
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use {
                if (!it.isSuccessful) error("HTTP ${it.code}")
            }
            (System.nanoTime() - t0) / 1e6
        }.fold(
            onSuccess = { lastPingError = null; it },
            onFailure = { e ->
                // Capture a human-usable reason so the UI can explain a zero ping
                // instead of silently rendering "0 ms".
                lastPingError = e.message ?: e::class.java.simpleName
                null
            }
        )
}
