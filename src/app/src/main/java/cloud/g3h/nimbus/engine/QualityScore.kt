package cloud.g3h.nimbus.engine

import kotlin.math.log10

/**
 * A 0-100 connection-quality score + a human grade, derived from the numbers
 * a run already produces (no new measurement, no DB column). Pure and
 * deterministic, so it is unit-tested.
 *
 * Weights: download (fastest axis, weighted most) 40, upload 25, latency 15,
 * jitter 10, packet loss 10. Each component is normalised to 0..1 first.
 *
 * A ping that could not be measured (0.0) is treated as "unknown" (a fixed
 * 0.60 neutral) rather than a hard zero, so a box that can speed-test but
 * cannot ping a specific endpoint is not punished to the floor.
 */
object QualityScore {

    data class Report(
        val score: Int,          // 0..100
        val grade: String        // Excellent / Great / Good / Fair / Poor
    )

    fun score(
        downloadMbps: Double,
        uploadMbps: Double,
        pingMs: Double,
        jitterMs: Double,
        lossPct: Double
    ): Report {
        val down = logScore(downloadMbps, reference = 200.0)  // 200 Mbps ≈ full marks
        val up = logScore(uploadMbps, reference = 50.0)       // 50 Mbps  ≈ full marks
        val ping = if (pingMs > 0.0) clamp01(1.0 - pingMs / 300.0) else 0.60
        val jit = clamp01(1.0 - jitterMs / 100.0)
        val loss = clamp01(1.0 - (lossPct / 100.0) * 10.0)     // 100% loss → 0

        val s = (down * 40.0 + up * 25.0 + ping * 15.0 + jit * 10.0 + loss * 10.0)
            .coerceIn(0.0, 100.0)
        val v = s.toInt()
        return Report(v, grade(v))
    }

    /** Log-eased 0..1: hits 1.0 at [reference], 0 at 0, monotonic in between. */
    private fun logScore(value: Double, reference: Double): Double =
        clamp01(log10(1.0 + value.coerceAtLeast(0.0)) / log10(1.0 + reference))

    fun grade(score: Int): String = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Great"
        score >= 60 -> "Good"
        score >= 45 -> "Fair"
        else -> "Poor"
    }

    private fun clamp01(v: Double): Double = if (v < 0.0) 0.0 else if (v > 1.0) 1.0 else v
}
