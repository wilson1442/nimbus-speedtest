package cloud.g3h.nimbus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.Canvas

// ---------- Icons (24dp grid, stroked, drawn with Canvas) ----------

@Composable
fun ShieldIcon(vpnOn: Boolean, tint: Color, modifier: Modifier = Modifier.size(14.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = w * 0.09f
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.06f)
            lineTo(w * 0.88f, h * 0.22f)
            lineTo(w * 0.88f, h * 0.52f)
            cubicTo(w * 0.88f, h * 0.74f, w * 0.66f, h * 0.92f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.34f, h * 0.92f, w * 0.12f, h * 0.74f, w * 0.12f, h * 0.52f)
            lineTo(w * 0.12f, h * 0.22f)
            close()
        }
        drawPath(shield, tint, style = Stroke(stroke))
        if (vpnOn) {
            // check
            drawPath(
                Path().apply {
                    moveTo(w * 0.32f, h * 0.52f); lineTo(w * 0.45f, h * 0.65f); lineTo(w * 0.68f, h * 0.38f)
                }, tint, style = Stroke(stroke * 0.9f)
            )
        } else {
            // slash
            drawLine(tint, Offset(w * 0.30f, h * 0.36f), Offset(w * 0.70f, h * 0.64f), strokeWidth = stroke * 0.9f)
        }
    }
}

@Composable
fun WifiIcon(tint: Color, modifier: Modifier = Modifier.size(14.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = w * 0.09f
        for (i in 0..2) {
            val r = w * (0.16f + i * 0.14f)
            drawArc(
                color = tint, startAngle = 225f, sweepAngle = 90f, useCenter = false,
                style = Stroke(stroke),
                topLeft = Offset(w * 0.5f - r, h * 0.42f - r * 0.9f),
                size = Size(r * 2, r * 2 * 0.9f)
            )
        }
        drawCircle(color = tint, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.78f))
    }
}

@Composable
fun EthernetIcon(tint: Color, modifier: Modifier = Modifier.size(14.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = w * 0.1f
        drawRoundRect(
            color = tint, topLeft = Offset(w * 0.14f, h * 0.32f),
            size = Size(w * 0.72f, h * 0.42f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(stroke)
        )
        for (x in floatArrayOf(0.3f, 0.5f, 0.7f)) {
            drawLine(tint, Offset(w * x, h * 0.32f), Offset(w * x, h * 0.14f), strokeWidth = stroke * 0.7f)
        }
        drawLine(tint, Offset(w * 0.5f, h * 0.74f), Offset(w * 0.5f, h * 0.9f), strokeWidth = stroke * 0.7f)
    }
}

@Composable
fun PulseIcon(tint: Color, modifier: Modifier = Modifier.size(16.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawPath(
            Path().apply {
                moveTo(0f, h * 0.55f)
                lineTo(w * 0.30f, h * 0.55f)
                lineTo(w * 0.38f, h * 0.34f)
                lineTo(w * 0.48f, h * 0.78f)
                lineTo(w * 0.56f, h * 0.18f)
                lineTo(w * 0.66f, h * 0.55f)
                lineTo(w, h * 0.55f)
            },
            tint, style = Stroke(w * 0.09f)
        )
    }
}

@Composable
fun CheckIcon(tint: Color, modifier: Modifier = Modifier.size(14.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawPath(
            Path().apply {
                moveTo(w * 0.18f, h * 0.54f); lineTo(w * 0.42f, h * 0.76f); lineTo(w * 0.82f, h * 0.28f)
            }, tint, style = Stroke(w * 0.1f)
        )
    }
}

@Composable
fun TrashIcon(tint: Color, modifier: Modifier = Modifier.size(14.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val stroke = w * 0.09f
        drawLine(tint, Offset(w * 0.2f, h * 0.26f), Offset(w * 0.8f, h * 0.26f), strokeWidth = stroke)
        drawLine(tint, Offset(w * 0.38f, h * 0.26f), Offset(w * 0.38f, h * 0.16f), strokeWidth = stroke * 0.8f)
        drawLine(tint, Offset(w * 0.62f, h * 0.26f), Offset(w * 0.62f, h * 0.16f), strokeWidth = stroke * 0.8f)
        drawRoundRect(
            color = tint, topLeft = Offset(w * 0.26f, h * 0.26f),
            size = Size(w * 0.48f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f), style = Stroke(stroke)
        )
    }
}

@Composable
fun PlayIcon(tint: Color, modifier: Modifier = Modifier.size(16.dp)) {
    Canvas(modifier = modifier) {
        drawPath(
            Path().apply {
                moveTo(size.width * 0.32f, size.height * 0.22f)
                lineTo(size.width * 0.8f, size.height * 0.5f)
                lineTo(size.width * 0.32f, size.height * 0.78f)
                close()
            }, tint
        )
    }
}

@Composable
fun ClockIcon(tint: Color, modifier: Modifier = Modifier.size(14.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawCircle(color = tint, radius = w * 0.42f, style = Stroke(w * 0.09f))
        drawPath(
            Path().apply {
                moveTo(w * 0.5f, h * 0.28f); lineTo(w * 0.5f, h * 0.5f); lineTo(w * 0.68f, h * 0.6f)
            }, tint, style = Stroke(w * 0.08f)
        )
    }
}

// ---------- Cards ----------

/** A bordered surface card with a label on top and big content below. */
@Composable
fun StatCard(
    label: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    fill: Color = Surface,
    borderColor: Color = Line
) {
    SurfaceBox(
        modifier = modifier,
        corner = corner,
        fill = fill,
        borderColor = borderColor,
        padding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(label.uppercase(), style = LabelStyle(7.5f))
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
fun SurfaceBox(
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    fill: Color = Surface,
    borderColor: Color = Line,
    padding: PaddingValues = PaddingValues(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(fill, RoundedCornerShape(corner))
            .border(1.dp, borderColor, RoundedCornerShape(corner))
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/** "vs avg" delta pill: up = tint/blue, down = warnBg/warnText. */
@Composable
fun DeltaPill(deltaText: String, positive: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = if (positive) Tint else WarnBg,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            deltaText,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (positive) BlueStrong else WarnText,
            fontFamily = ChakraPetch
        )
    }
}

/** A sparkline of sample values, drawn as a filled area + line in blue. */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = Blue,
    fillColor: Color = Color(0x335B9BD5)
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val maxV = values.max().coerceAtLeast(1.0)
        val step = size.width / (values.size - 1)
        val path = Path().apply {
            moveTo(0f, size.height)
            values.forEachIndexed { i, v ->
                lineTo(i * step, size.height - (v / maxV).toFloat() * size.height * 0.92f - size.height * 0.04f)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path, fillColor)
        val line = Path().apply {
            values.forEachIndexed { i, v ->
                val x = i * step
                val y = size.height - (v / maxV).toFloat() * size.height * 0.92f - size.height * 0.04f
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(line, color, style = Stroke(2f))
    }
}

/** Dashed divider line. */
@Composable
fun DashedLine(color: Color = Line, modifier: Modifier = Modifier, dashLength: Dp = 6.dp) {
    Box(
        modifier = modifier
            .height(1.dp)
            .drawBehind {
                val dashPx = dashLength.value * density
                val step = dashPx * 2f
                var x = 0f
                while (x < size.width) {
                    drawLine(color, Offset(x, 0.5f), Offset(minOf(x + dashPx, size.width), 0.5f), strokeWidth = 1f)
                    x += step
                }
            }
    )
}

/** Small uppercase section label with letter-spacing. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Ink2) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = LabelStyle(7.5f).copy(color = color)
    )
}

/** Big Oxanium number + small unit, side by side (baseline aligned). */
@Composable
fun BigNumber(value: String, unit: String, modifier: Modifier = Modifier, sizeSp: Float = 27f, color: Color = Ink) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(value, style = NumberStyle(sizeSp, color))
        Spacer(Modifier.width(4.dp))
        Text(unit, style = NumberStyle(sizeSp * 0.5f, Ink2, FontWeight.Medium))
    }
}

/** Connection icon by type. */
@Composable
fun ConnectionIcon(type: String, modifier: Modifier = Modifier) {
    when (type) {
        "ETHERNET" -> EthernetIcon(BlueStrong, modifier)
        else -> WifiIcon(BlueStrong, modifier)
    }
}

/** VPN on/off pill for the History list. */
@Composable
fun VpnPill(on: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = if (on) ChipVpnOn else NeutralPill,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            if (on) "On" else "Off",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (on) ChipVpnOnText else Ink2,
            fontFamily = ChakraPetch
        )
    }
}

/** A primary filled (blueStrong) TV button. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable (Modifier) -> Unit)? = null,
    sizeSp: Float = 12f
) {
    Box(
        modifier = modifier
            .nimbusFocus(cornerRadius = 999.dp, ringThickness = 2.dp, gap = 4.dp)
            .background(BlueStrong, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            icon?.invoke(Modifier.size(14.dp))
            Text(label, fontSize = sizeSp.sp, fontWeight = FontWeight.SemiBold, color = Surface, fontFamily = ChakraPetch)
        }
    }
}

/** A secondary flat TV button (surface + border). */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable (Modifier) -> Unit)? = null,
    sizeSp: Float = 11f,
    textColor: Color = Ink,
    borderColor: Color = Line
) {
    Box(
        modifier = modifier
            .nimbusFocus(cornerRadius = 999.dp, ringThickness = 2.dp, gap = 4.dp)
            .background(Surface, RoundedCornerShape(999.dp))
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            icon?.invoke(Modifier.size(13.dp))
            Text(label, fontSize = sizeSp.sp, fontWeight = FontWeight.Medium, color = textColor, fontFamily = ChakraPetch)
        }
    }
}

/** Warn-styled button (Clear history). */
@Composable
fun WarnButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable (Modifier) -> Unit)? = null,
    sizeSp: Float = 10.5f
) {
    Box(
        modifier = modifier
            .nimbusFocus(cornerRadius = 999.dp, ringThickness = 2.dp, gap = 4.dp)
            .background(WarnBg, RoundedCornerShape(999.dp))
            .border(1.dp, WarnBorder, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            icon?.invoke(Modifier.size(12.dp))
            Text(label, fontSize = sizeSp.sp, fontWeight = FontWeight.SemiBold, color = WarnText, fontFamily = ChakraPetch)
        }
    }
}

/** A settings row: label left, value/edit control right, focusable as one row. */
@Composable
fun SettingsRow(
    label: String,
    sub: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .nimbusFocus(cornerRadius = 14.dp, ringThickness = 2.dp, gap = 5.dp)
            .background(Surface, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink, fontFamily = ChakraPetch)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, fontSize = 9.5.sp, color = Ink2, fontFamily = ChakraPetch, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing?.invoke(this)
    }
}
