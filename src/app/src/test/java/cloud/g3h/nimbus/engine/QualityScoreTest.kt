package cloud.g3h.nimbus.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityScoreTest {

    @Test
    fun `fast low-latency connection scores excellent`() {
        val r = QualityScore.score(downloadMbps = 300.0, uploadMbps = 80.0, pingMs = 12.0, jitterMs = 2.0, lossPct = 0.0)
        assertTrue("got ${r.score} (${r.grade})", r.score >= 85)
        assertEquals("Excellent", r.grade)
    }

    @Test
    fun `slow connection scores poorly`() {
        val r = QualityScore.score(downloadMbps = 0.5, uploadMbps = 0.2, pingMs = 250.0, jitterMs = 120.0, lossPct = 40.0)
        assertTrue("got ${r.score} (${r.grade})", r.score < 40)
        assertEquals("Poor", r.grade)
    }

    @Test
    fun `score is bounded to zero through one hundred`() {
        assertTrue(QualityScore.score(99999.0, 99999.0, 0.0, 0.0, 0.0).score <= 100)
        assertTrue(QualityScore.score(0.0, 0.0, 99999.0, 99999.0, 100.0).score >= 0)
    }

    @Test
    fun `unmeasured ping is neutral not a hard zero`() {
        val withPing = QualityScore.score(100.0, 40.0, 20.0, 3.0, 0.0)
        val noPing = QualityScore.score(100.0, 40.0, 0.0, 3.0, 0.0)
        // Both should still be a healthy "Good+"; the missing ping must not tank the grade.
        assertTrue("noPing=${noPing.score}", noPing.score >= 60)
        assertTrue("ping present should be >= unknown-neutral", withPing.score >= noPing.score)
    }

    @Test
    fun `higher download raises score`() {
        val low = QualityScore.score(10.0, 5.0, 30.0, 5.0, 0.0)
        val high = QualityScore.score(150.0, 5.0, 30.0, 5.0, 0.0)
        assertTrue(high.score > low.score)
    }

    @Test
    fun `grade bands are monotonic`() {
        assertEquals("Excellent", QualityScore.grade(95))
        assertEquals("Great", QualityScore.grade(80))
        assertEquals("Good", QualityScore.grade(65))
        assertEquals("Fair", QualityScore.grade(50))
        assertEquals("Poor", QualityScore.grade(30))
    }
}
