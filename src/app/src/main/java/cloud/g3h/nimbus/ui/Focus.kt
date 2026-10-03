package cloud.g3h.nimbus.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TV focus treatment (spec §2): a visible 2-ring halo — a ground gap ring
 * plus a blueSoft ring — and a slight scale-up while focused.
 *
 * Built on the core Compose focus API (focusable + onFocusChanged), which is
 * what makes D-pad/remote input work without the tv-* libraries.
 */
@Composable
fun Modifier.nimbusFocus(
    cornerRadius: Dp = 28.dp,
    ringThickness: Dp = 4.dp,
    gap: Dp = 6.dp
): Modifier {
    val focusedState = remember { mutableStateOf(false) }
    var focused by focusedState
    return this
        .focusable()
        .onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.scale(1.03f) else Modifier)
        .drawBehind {
            if (!focused) return@drawBehind
            val ring = ringThickness.toPx()
            val r = cornerRadius.toPx()
            drawRoundRect(
                color = BlueSoft,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(r + ring, r + ring),
                style = Stroke(ring)
            )
            drawRoundRect(
                color = Ground,
                topLeft = Offset(ring, ring),
                size = Size(size.width - 2 * ring, size.height - 2 * ring),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(ring)
            )
        }
        .padding(gap)
}
