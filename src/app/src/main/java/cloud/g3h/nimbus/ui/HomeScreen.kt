package cloud.g3h.nimbus.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.material3.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val timeFmt = SimpleDateFormat("EEE · h:mm a", Locale.getDefault())
private val dayFmt = SimpleDateFormat("MMM d", Locale.getDefault())

/** Home screen (Main.dc.html). */
@Composable
fun HomeScreen(
    vm: NimbusViewModel,
    modifier: Modifier = Modifier
) {
    val conn by vm.conn.collectAsState()
    val last by vm.lastTest.collectAsState()
    val avg by vm.avg30.collectAsState()
    val settings by vm.settings.collectAsState()

    if (!conn.online) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No internet connection", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink, fontFamily = ChakraPetch)
                Spacer(Modifier.height(8.dp))
                Text("Check your network, then focus START to test.", fontSize = 12.sp, color = Ink2, fontFamily = ChakraPetch)
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ---- background wave lines ----
        Box(
            Modifier.fillMaxSize().drawBehind {
                val w = size.width; val h = size.height
                fun wave(base: Float, amp: Float, alpha: Float) {
                    val p = Path().apply {
                        moveTo(0f, h * base)
                        cubicTo(w * 0.16f, h * base - amp, w * 0.33f, h * base + amp, w * 0.5f, h * base)
                        cubicTo(w * 0.66f, h * base - amp, w * 0.83f, h * base + amp, w, h * base)
                    }
                    drawPath(p, Wave.copy(alpha = alpha), style = Stroke(1.5f))
                }
                wave(0.795f, 36f, 0.8f)
                wave(0.833f, 38f, 0.55f)
                wave(0.872f, 40f, 0.35f)
                wave(0.176f, 30f, 0.4f)
            }
        )

        // ---- top bar ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(start = 48.dp, top = 27.dp, end = 48.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            NimbusTopBar(
                active = NimbusAppScreen.TEST,
                onNavigate = { vm.navigate(it.toScreen()) },
                state = conn
            )
        }

        // ---- center stage: orbit + rings + ECG + START ----
        val startFocus = FocusRequester()
        LaunchedEffect(Unit) { startFocus.requestFocus() }

        Box(
            modifier = Modifier.align(Alignment.Center).size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            HomeAnimations()

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(BlueStrong, CircleShape)
                    .focusRequester(startFocus)
                    .nimbusFocus(cornerRadius = 1000.dp, ringThickness = 3.dp, gap = 7.dp)
                    .clickable { vm.startTest() }
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    PulseIcon(Surface, Modifier.size(22.dp))
                    Text(
                        "START",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.14f.sp,
                        color = Surface,
                        fontFamily = ChakraPetch
                    )
                    Text(
                        "PRESS OK",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2f.sp,
                        color = PressOk,
                        fontFamily = ChakraPetch
                    )
                }
            }
        }

        // ---- server note under the button ----
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 174.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val serverName = if (settings.serverUrl.isBlank()) "Auto (nearest)"
            else hostOf(settings.serverUrl)
            Text(
                "Measures ping, download and upload in about 30 seconds · Server: $serverName",
                fontSize = 11.sp, color = Ink2, fontFamily = ChakraPetch
            )
            if (conn.vpnActive) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    InfoCircleIcon(tint = SubInk, modifier = Modifier.size(10.dp))
                    Text(
                        "VPN detected — results will reflect your VPN route, not your raw ISP speed",
                        fontSize = 9.5.sp, color = SubInk, fontFamily = ChakraPetch
                    )
                }
            }
        }

        // ---- footer: last test + 30-day average ----
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (last != null) {
                val l = last!!
                val whenText = if (l.timestamp > System.currentTimeMillis() - 86_400_000)
                    "Today · " + timeFmt.format(Date(l.timestamp))
                else dayFmt.format(Date(l.timestamp))
                Row(
                    modifier = Modifier
                        .background(Surface, RoundedCornerShape(14.dp))
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.width(110.dp)) {
                        SectionLabel("Last test")
                        Text(whenText, fontSize = 10.sp, color = Ink, fontFamily = ChakraPetch)
                    }
                    Spacer(Modifier.width(24.dp))
                    Box(Modifier.width(0.5.dp).height(44.dp).background(Line))
                    Spacer(Modifier.width(24.dp))
                    FooterStat("Download", fmt(l.downloadMbps, 1), "Mbps")
                    Spacer(Modifier.width(24.dp))
                    FooterStat("Upload", fmt(l.uploadMbps, 1), "Mbps")
                    Spacer(Modifier.width(24.dp))
                    FooterStat("Ping", if (l.pingMs > 0.0) fmt(l.pingMs, 1) else "—", "ms")
                }
            } else {
                Row(
                    modifier = Modifier
                        .background(Surface, RoundedCornerShape(14.dp))
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SectionLabel("No tests yet")
                        Text("Your results will appear here.", fontSize = 10.sp, color = Ink2, fontFamily = ChakraPetch)
                    }
                }
            }
            // 30-day average card (focusable → History)
            Row(
                modifier = Modifier
                    .nimbusFocus(cornerRadius = 14.dp, ringThickness = 2.dp, gap = 5.dp)
                    .background(Tint, RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorderActive, RoundedCornerShape(14.dp))
                    .clickable { vm.navigate(Screen.HISTORY) }
                    .padding(horizontal = 20.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SectionLabel("30-day average", color = SubInk)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            if (avg != null && avg!!.count > 0) fmt(avg!!.avgDownloadMbps, 1) else "—",
                            style = NumberStyle(18f, Ink)
                        )
                        Text("Mbps", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = SubInk, fontFamily = Oxanium)
                    }
                }
            }
        }
    }
}

fun hostOf(url: String): String =
    runCatching {
        java.net.URI(if (url.startsWith("http")) url else "https://$url").host ?: url
    }.getOrDefault(url)

@Composable
fun FooterStat(label: String, value: String, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SectionLabel(label)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = NumberStyle(18f, Ink))
            Text(unit, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Ink2, fontFamily = Oxanium)
        }
    }
}

fun fmt(v: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", v)

/** Pulsing rings + rotating dotted orbit + faint ECG behind the START button (§7). */
@Composable
fun HomeAnimations(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "home")
    val ringProgress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring"
    )
    val orbitRotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit"
    )
    val ecgProgress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "ecg"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val ringBase = size.width * 0.32f

        // dotted orbit + one dot
        val orbitR = size.width * 0.49f
        val dashes = 48
        val rot = orbitRotation * PI.toFloat() / 180f
        for (i in 0 until dashes) {
            val a1 = (i * 2f * PI.toFloat() / dashes) + rot
            val a2 = a1 + 0.013f
            drawLine(
                OrbitDots,
                Offset(cx + orbitR * cos(a1), cy + orbitR * sin(a1)),
                Offset(cx + orbitR * cos(a2), cy + orbitR * sin(a2)),
                strokeWidth = 1.2f
            )
        }
        drawCircle(Blue, 3f, Offset(cx + orbitR * cos(rot), cy + orbitR * sin(rot)))

        // 3 staggered expanding rings (scale 0.9→1.6, alpha 0.8→0)
        for (k in 0 until 3) {
            val p = (ringProgress + k / 3f) % 1f
            drawCircle(
                color = BlueSoft.copy(alpha = 0.8f * (1f - p)),
                radius = ringBase * (0.9f + p * 0.7f),
                style = Stroke(1.2f)
            )
        }

        // faint ECG trace across the stage (35% alpha, 7 s loop)
        drawPath(
            heartbeatPath(size.width, size.height, units = 4),
            Color(0xFF7FB0DE).copy(alpha = 0.35f),
            style = Stroke(1.5f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Heartbeat sub-path (spec §7), repeated [units] times, baseline at 50% height.
 * Unit: l60,0 l14,-14 l14,14 l20,0 l10,30 l16,-150 l16,190 l12,-70 l20,0 l18,-16 l18,16 l102,0
 */
fun heartbeatPath(width: Float, height: Float, units: Int): Path = Path().apply {
    val baseline = height * 0.5f
    val s = 0.5f // mockup px ÷ 2
    moveTo(0f, baseline)
    repeat(units) {
        relativeLineTo(60f * s, 0f)
        relativeLineTo(14f * s, -14f * s)
        relativeLineTo(14f * s, 14f * s)
        relativeLineTo(20f * s, 0f)
        relativeLineTo(10f * s, 30f * s)
        relativeLineTo(16f * s, -150f * s)
        relativeLineTo(16f * s, 190f * s)
        relativeLineTo(12f * s, -70f * s)
        relativeLineTo(20f * s, 0f)
        relativeLineTo(18f * s, -16f * s)
        relativeLineTo(18f * s, 16f * s)
        relativeLineTo(102f * s, 0f)
    }
}

/** Info circle icon (used by the VPN note). */
@Composable
fun InfoCircleIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        drawCircle(color = tint, radius = w * 0.45f, style = Stroke(w * 0.1f))
        drawLine(tint, Offset(w * 0.5f, w * 0.42f), Offset(w * 0.5f, w * 0.68f), strokeWidth = w * 0.1f)
        drawCircle(tint, w * 0.05f, Offset(w * 0.5f, w * 0.3f))
    }
}
