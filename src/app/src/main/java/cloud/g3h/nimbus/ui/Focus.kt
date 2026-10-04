package cloud.g3h.nimbus.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Smallest focus ring a viewer can pick out at set-top-box distance. */
private val MinFocusRing = 3.dp

/**
 * TV focus treatment (spec §2).
 *
 * Rewritten because the previous version was effectively invisible on a real
 * set-top box. It drew the ring with `drawBehind` using **centred** strokes, so
 * half of every ring fell outside the node and was clipped, and it mixed
 * `BlueSoft` (#9CC2E6, pale) with a `Ground`-coloured gap ring (#F4F6F3). What
 * survived on screen was roughly a 1 dp hairline at ~35% contrast — nothing at
 * D-pad distance.
 *
 * Now it:
 *  - draws the ring **on top** (`drawWithContent`) so no sibling background can
 *    paint over it;
 *  - **clamps thickness to a 3 dp floor**, so the call sites that asked for
 *    1.5–2 dp all get something legible (6 px at 1080p, ~4 px at 720p);
 *  - uses a **deep blue** ([FocusRing], not pale BlueSoft) plus a translucent
 *    glow ([FocusHalo]) that contrasts with the light app palette;
 *  - scales the focused element up.
 *
 * Layout is deliberately untouched: the ring sits in the padding band this
 * modifier already reserved, and `scale` is layout-neutral — so no screen
 * re-flows (the Home location dropdown is height-sensitive and was sized to fit).
 *
 * Built on the core Compose focus API, which is what makes D-pad/remote input work
 * without the tv-* libraries.
 *
 * The v1.7.2 version of this file got the contrast and thickness right but declared
 * `focusable()` *before* `onFocusChanged`, so the state it watched never turned
 * true and the ring never drew at all. A bright invisible ring is still invisible.
 */
@Composable
fun Modifier.nimbusFocus(
    cornerRadius: Dp = 28.dp,
    ringThickness: Dp = 3.dp,
    gap: Dp = 6.dp
): Modifier {
    val focusedState = remember { mutableStateOf(false) }
    var focused by focusedState
    // A floor, not an override: a call site may ask for a thicker ring, never an invisible one.
    val stroke = maxOf(ringThickness, MinFocusRing)
    return this
        // ORDER IS LOAD-BEARING. Per Android's docs, "the onFocusChanged() modifier
        // refers to the first focusable element that appears AFTER" it, so it must be
        // declared BEFORE focusable(). With them the other way round it observes
        // nothing at all, `focused` never turns true and the ring never draws —
        // silently. FocusSemanticsTest pins both directions of this.
        .onFocusChanged { focused = it.isFocused }
        .focusable()
        .then(if (focused) Modifier.scale(1.04f) else Modifier)
        .drawWithContent {
            drawContent()
            if (!focused) return@drawWithContent
            val t = stroke.toPx()
            val r = cornerRadius.toPx()
            // Glow first — wider and translucent, so the crisp ring reads on top of it.
            drawRoundRect(
                color = FocusHalo,
                topLeft = Offset(-t / 2, -t / 2),
                size = Size(size.width + t, size.height + t),
                cornerRadius = CornerRadius(r + t / 2, r + t / 2),
                style = Stroke(t * 2.2f)
            )
            // The ring straddles the node edge: half lands in the reserved band,
            // half just outside it, so it can never be clipped down to nothing.
            drawRoundRect(
                color = FocusRing,
                topLeft = Offset(-t / 2, -t / 2),
                size = Size(size.width + t, size.height + t),
                cornerRadius = CornerRadius(r + t / 2, r + t / 2),
                style = Stroke(t)
            )
        }
        .padding(gap)
}
