package cloud.g3h.nimbus.engine

import cloud.g3h.nimbus.data.Phase

/** Progress emitted to the UI roughly every 200 ms during a run. */
data class SpeedProgress(
    val phase: Phase = Phase.PING,
    val liveMbps: Double = 0.0,
    val peakMbps: Double = 0.0,
    val pingMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val lossPct: Double = 0.0,
    val elapsedMs: Long = 0,
    val done: Boolean = false,
    val error: String? = null,
    val pingSamples: List<Double> = emptyList(),
    val downloadSamples: List<Double> = emptyList(),
    val uploadSamples: List<Double> = emptyList(),
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val minPingMs: Double = 0.0,
    val serverReachable: Boolean = false,
    val pingError: String? = null
)

/**
 * A speed test runs ping → download → upload. Implementations emit live
 * [SpeedProgress] and must be cancellable by cancelling the caller's scope.
 */
interface SpeedTestEngine {
    fun run(serverBaseUrl: String, shortTest: Boolean): kotlinx.coroutines.flow.Flow<SpeedProgress>

    /**
     * Probes [serverBaseUrl] for reachability: 3 sequential empty.php
     * round-trips. Returns (ok, bestMs, errorReason) where errorReason
     * explains the failure (timeout, connection refused, DNS, HTTP code…) so
     * the UI can tell the user *why* instead of a blank failure.
     */
    suspend fun probeServer(serverBaseUrl: String): ServerProbe
}

/** Outcome of a server reachability probe. */
data class ServerProbe(
    val ok: Boolean,
    val bestMs: Double,
    val error: String?
)

/** Final numbers of a completed run (before persistence). */
data class TestOutcome(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val packetLossPct: Double,
    val minPingMs: Double,
    val pingSamples: List<Double>,
    val downloadSamples: List<Double>,
    val uploadSamples: List<Double>,
    val peakMbps: Double = 0.0
)
