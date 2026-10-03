package cloud.g3h.nimbus.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PingMathTest {

    @Test
    fun `ping is the median of samples`() {
        val r = PingMath.compute(listOf(20.0, 10.0, 30.0))
        assertEquals(20.0, r.pingMs, 0.001)
    }

    @Test
    fun `ping is the median with an even count`() {
        val r = PingMath.compute(listOf(10.0, 20.0, 30.0, 40.0))
        assertEquals(25.0, r.pingMs, 0.001)
    }

    @Test
    fun `jitter is the mean absolute difference between consecutive samples`() {
        // samples 10, 20, 30 -> diffs 10, 10 -> jitter 10
        val r = PingMath.compute(listOf(10.0, 20.0, 30.0))
        assertEquals(10.0, r.jitterMs, 0.001)
    }

    @Test
    fun `jitter counts both directions`() {
        // 10, 20, 10 -> diffs 10, 10 -> jitter 10
        val r = PingMath.compute(listOf(10.0, 20.0, 10.0))
        assertEquals(10.0, r.jitterMs, 0.001)
    }

    @Test
    fun `single sample has zero jitter`() {
        val r = PingMath.compute(listOf(15.0))
        assertEquals(0.0, r.jitterMs, 0.0001)
    }

    @Test
    fun `loss is failed over total`() {
        // 1 failed of 5 -> 20%
        val r = PingMath.compute(listOf(10.0, null, 12.0, 11.0, 13.0))
        assertEquals(20.0, r.lossPct, 0.001)
    }

    @Test
    fun `failed requests do not skew ping`() {
        // only 15, 25 count -> median 20
        val r = PingMath.compute(listOf(null, 15.0, null, 25.0))
        assertEquals(20.0, r.pingMs, 0.001)
        assertEquals(50.0, r.lossPct, 0.001)
    }

    @Test
    fun `all failed yields 100 percent loss`() {
        val r = PingMath.compute(listOf(null, null))
        assertEquals(100.0, r.lossPct, 0.001)
        assertEquals(0.0, r.pingMs, 0.001)
    }

    @Test
    fun `median handles unsorted input`() {
        assertEquals(4.0, PingMath.median(listOf(9.0, 1.0, 4.0)), 0.0001)
    }

    @Test
    fun `empty samples yield zero ping and 100 percent loss`() {
        val r = PingMath.compute(emptyList())
        assertEquals(0.0, r.pingMs, 0.001)
        assertEquals(100.0, r.lossPct, 0.001)
    }
}
