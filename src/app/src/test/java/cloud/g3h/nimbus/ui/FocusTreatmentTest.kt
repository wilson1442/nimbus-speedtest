package cloud.g3h.nimbus.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * The focus ring has to stay visible on a TV.
 *
 * Guards the v1.7.2 fix. The previous treatment drew a BlueSoft (#9CC2E6) ring
 * on the app background: 1.71:1 contrast, which on a set-top box at viewing
 * distance is effectively invisible — the reported symptom was "I can't really
 * see what's selected". WCAG 2.1 asks for at least 3:1 for non-text UI
 * indicators, so that is the floor asserted here.
 */
class FocusTreatmentTest {

    private fun luminance(c: Color): Double {
        fun channel(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun `focus ring clears the 3 to 1 floor on the app background`() {
        val ratio = contrast(FocusRing, Ground)
        assertTrue("focus ring contrast was ${"%.2f".format(ratio)}:1, need >= 3:1", ratio >= 3.0)
    }

    @Test
    fun `focus ring clears the floor on cards too`() {
        // Rows, pills and dropdown items sit on Surface, not only on Ground.
        val ratio = contrast(FocusRing, Surface)
        assertTrue("focus ring contrast on Surface was ${"%.2f".format(ratio)}:1, need >= 3:1", ratio >= 3.0)
    }

    @Test
    fun `the old pale ring would have failed this test`() {
        // Documents why the colour changed: BlueSoft was the previous ring colour
        // and is kept here purely as the counter-example.
        val old = contrast(BlueSoft, Ground)
        assertTrue("BlueSoft measured ${"%.2f".format(old)}:1 — if this ever reaches 3:1 the premise changed", old < 3.0)
    }
}
