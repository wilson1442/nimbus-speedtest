package cloud.g3h.nimbus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import cloud.g3h.nimbus.net.ConnectionState
import cloud.g3h.nimbus.ui.NimbusAppScreen
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/** The four main screens. */
enum class NimbusAppScreen { TEST, HISTORY, SETTINGS }

/** Top-bar nav destinations (built once, not on every recomposition). */
private val navPills = listOf(
    NimbusAppScreen.TEST to "Test",
    NimbusAppScreen.HISTORY to "History",
    NimbusAppScreen.SETTINGS to "Settings"
)

/** Map a top-bar pill to the ViewModel screen. */
fun NimbusAppScreen.toScreen(): Screen = when (this) {
    NimbusAppScreen.TEST -> Screen.HOME
    NimbusAppScreen.HISTORY -> Screen.HISTORY
    NimbusAppScreen.SETTINGS -> Screen.SETTINGS
}

/** 28×28 logo tile (56 px ÷ 2): blue rounded square with two arcs + a dot. */
@Composable
fun NimbusLogoTile(tile: Dp = 28.dp, modifier: Modifier = Modifier) {
    val w = tile
    Box(
        modifier = modifier
            .size(tile)
            .background(Blue, RoundedCornerShape(8.dp))
            .drawBehind {
                val px = w.toPx()
                val cx = px / 2f
                val cy = px * 0.61f
                val stroke = px * 0.065f
                drawArc(
                    color = Surface,
                    startAngle = 180f,
                    sweepAngle = -180f,
                    useCenter = false,
                    style = Stroke(stroke),
                    topLeft = Offset(cx - px * 0.25f, cy - px * 0.25f),
                    size = Size(px * 0.5f, px * 0.5f)
                )
                drawArc(
                    color = Surface,
                    startAngle = 180f,
                    sweepAngle = -180f,
                    useCenter = false,
                    style = Stroke(stroke),
                    topLeft = Offset(cx - px * 0.145f, cy - px * 0.145f),
                    size = Size(px * 0.29f, px * 0.29f)
                )
                drawCircle(color = Surface, radius = px * 0.055f, center = Offset(cx, cy))
            }
    )
}

/** Top bar shown on every screen: logo + wordmark, centered nav pills, status on the right. */
@Composable
fun NimbusTopBar(
    active: NimbusAppScreen,
    onNavigate: (NimbusAppScreen) -> Unit,
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // left: logo + wordmark
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            NimbusLogoTile()
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    "Nimbus",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ChakraPetch,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.02f.em,
                        color = Ink
                    )
                )
                Text(
                    "SPEED TEST",
                    style = LabelStyle(7.5f).copy(letterSpacing = (0.24f * 7.5f).sp)
                )
            }
        }
        // center: nav pills
        NavPills(active = active, onNavigate = onNavigate)
        Spacer(Modifier.width(16.dp))
        // right: connection status bar
        StatusBar(state = state)
    }
}

@Composable
private fun NavPills(
    active: NimbusAppScreen,
    onNavigate: (NimbusAppScreen) -> Unit
) {
    Row(
        modifier = Modifier
            .background(Surface, RoundedCornerShape(999.dp))
            .border(1.dp, Line, RoundedCornerShape(999.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        navPills.forEach { (screen, label) ->
            val isActive = screen == active
            PillButton(
                label = label,
                active = isActive,
                onClick = { onNavigate(screen) }
            )
        }
    }
}

/** A focusable pill; active = filled tint, others flat. */
@Composable
fun PillButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillActive: Color = Tint,
    activeText: Color = Ink,
    sizeSp: Float = 11f
) {
    Box(
        modifier = modifier
            .nimbusFocus(cornerRadius = 999.dp, ringThickness = 2.dp, gap = 3.dp)
            .background(if (active) fillActive else Color.Transparent, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = sizeSp.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) activeText else Ink2,
            fontFamily = ChakraPetch
        )
    }
}

/** Right-hand status cluster: [VPN chip] | WAN IP x.x.x | <icon> <type> */
@Composable
fun StatusBar(state: ConnectionState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Surface, RoundedCornerShape(999.dp))
            .border(1.dp, Line, RoundedCornerShape(999.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // VPN chip
        val vpnOn = state.vpnActive
        Box(
            modifier = Modifier
                .background(if (vpnOn) Tint else WarnBg, RoundedCornerShape(999.dp))
                .padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                VpnIcon(vpnOn = vpnOn, tint = if (vpnOn) BlueStrong else WarnText)
                Text(
                    if (vpnOn) "VPN connected" else "No VPN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (vpnOn) Ink else WarnText,
                    fontFamily = ChakraPetch
                )
            }
        }
        Spacer(Modifier.width(0.dp))
        // WAN IP
        StatusSegment(divider = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("WAN IP", fontSize = 10.sp, color = Ink2, fontFamily = ChakraPetch)
                val ipText = when {
                    state.wanIpLoading -> "Detecting…"
                    state.wanIp == null -> "Unavailable"
                    else -> state.wanIp
                }
                Text(
                    ipText,
                    style = NumberStyle(10f, if (state.wanIp == null) Ink2 else Ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // connection type
        StatusSegment(divider = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                WifiIcon(tint = BlueStrong)
                Text(state.connectionLabel(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Ink, fontFamily = ChakraPetch)
            }
        }
    }
}

@Composable
private fun VpnIcon(vpnOn: Boolean, tint: Color) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val w = size.width; val h = size.height
        val stroke = w * 0.09f
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.86f, h * 0.22f)
            lineTo(w * 0.86f, h * 0.5f)
            cubicTo(w * 0.86f, h * 0.72f, w * 0.65f, 0.88f * h, w * 0.5f, h * 0.94f)
            cubicTo(w * 0.35f, h * 0.88f, w * 0.14f, h * 0.72f, w * 0.14f, h * 0.5f)
            lineTo(w * 0.14f, h * 0.22f)
            close()
        }
        drawPath(shield, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        if (vpnOn) {
            drawPath(
                Path().apply {
                    moveTo(w * 0.32f, h * 0.5f); lineTo(w * 0.45f, h * 0.64f); lineTo(w * 0.68f, h * 0.38f)
                }, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 0.9f)
            )
        } else {
            drawLine(tint, Offset(w * 0.3f, h * 0.36f), Offset(w * 0.7f, h * 0.64f), strokeWidth = stroke * 0.9f)
        }
    }
}

@Composable
private fun StatusSegment(divider: Boolean, content: @Composable () -> Unit) {
    if (divider) {
        Box(Modifier.width(1.dp).height(18.dp).background(Line))
    }
    Box(Modifier.padding(horizontal = 11.dp), contentAlignment = Alignment.Center) {
        content()
    }
}
