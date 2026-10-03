package cloud.g3h.nimbus.engine

/**
 * Pure latency math (unit-tested): ping = median of samples,
 * jitter = mean absolute difference of consecutive samples,
 * loss = failed / total.
 */
object PingMath {

    data class Result(
        val pingMs: Double,
        val jitterMs: Double,
        val lossPct: Double
    )

    /**
     * @param samplesMs per-request round-trip times; a failed request is null.
     */
    fun compute(samplesMs: List<Double?>): Result {
        val ok = samplesMs.filterNotNull()
        val total = samplesMs.size
        if (total == 0 || ok.isEmpty()) {
            return Result(0.0, 0.0, 100.0)
        }
        val ping = median(ok)
        val jitter = if (ok.size >= 2) {
            (0 until ok.size - 1).sumOf { kotlin.math.abs(ok[it + 1] - ok[it]) } / (ok.size - 1)
        } else {
            0.0
        }
        val lossPct = ((total - ok.size).toDouble() / total) * 100.0
        return Result(ping, jitter, lossPct)
    }

    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val s = values.sorted()
        val mid = s.size / 2
        return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2.0
    }
}
