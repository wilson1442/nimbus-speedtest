package cloud.g3h.nimbus.engine

import cloud.g3h.nimbus.data.Phase
import cloud.g3h.nimbus.data.Sample

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
    val uploadMbps: Double = 0.0
)

/**
 * A speed test runs ping → download → upload. Implementations emit live
 * [SpeedProgress] and must be cancellable by cancelling the caller's scope.
 */
interface SpeedTestEngine {
    fun run(serverBaseUrl: String, shortTest: Boolean): kotlinx.coroutines.flow.Flow<SpeedProgress>
}

/** Final numbers of a completed run (before persistence). */
data class TestOutcome(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val packetLossPct: Double,
    val pingSamples: List<Double>,
    val downloadSamples: List<Double>,
    val uploadSamples: List<Double>
)
