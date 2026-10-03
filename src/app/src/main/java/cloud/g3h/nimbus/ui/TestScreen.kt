package cloud.g3h.nimbus.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import java.util.Locale
import kotlin.math.ln
import kotlin.math.min

private fun fmtTime(ms: Long): String {
    val total = ms / 1000
    return String.format(Locale.getDefault(), "%02d:%02d", total / 60, total % 60)
}

/** Test-running screen (Testing.dc.html). */
@Composable
fun TestScreen(
    vm: NimbusViewModel,
    modifier: Modifier = Modifier
) {
    val test by vm.test.collectAsState()
    val conn by vm.conn.collectAsState()
    val settings by vm.settings.collectAsState()
    // Smooth the live bandwidth between 200 ms engine samples so the gauge and
    // background bars sweep fluidly instead of stepping.
    val liveTarget = test.liveMbps.toFloat()
    val liveSmooth by animateFloatAsState(
        targetValue = liveTarget,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "liveMbps"
    )

    Box(modifier = modifier.fillMaxSize().background(Ground)) {
        // ---- background: ECG scroll + bandwidth bars ----
        TestBackground(liveMbps = liveSmooth.toDouble())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 27.dp)
        ) {
            // ---- top bar (test variant) ----
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    NimbusLogoTile()
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text(
                            "Nimbus",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = ChakraPetch
                        )
                        Text("SPEED TEST", style = LabelStyle(7.5f).copy(letterSpacing = (0.24f * 7.5f).sp))
                    }
                }
                // blinking TEST IN PROGRESS pill
                val blink = rememberInfiniteTransition(label = "blink")
                val blinkAlpha by blink.animateFloat(
                    0.25f, 1f,
                    infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
                    label = "blinkAlpha"
                )
                Row(
                    modifier = Modifier
                        .background(Surface, RoundedCornerShape(999.dp))
                        .border(1.dp, Line, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(BlueStrong.copy(alpha = blinkAlpha), CircleShape)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "TEST IN PROGRESS",
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.16f.sp, color = Ink, fontFamily = ChakraPetch
                    )
                }
                // right: VPN · IP · Elapsed
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(Surface, RoundedCornerShape(999.dp))
                        .border(1.dp, Line, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    ShieldIcon(vpnOn = conn.vpnActive, tint = if (conn.vpnActive) BlueStrong else WarnText, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (conn.vpnActive) "VPN" else "No VPN", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Ink, fontFamily = ChakraPetch)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        (when {
                            conn.wanIpLoading -> "Detecting…"
                            conn.wanIp == null -> "—"
                            else -> conn.wanIp
                        } ?: "—"),
                        style = NumberStyle(10f, Ink)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text("·", fontSize = 10.sp, color = Ink2, fontFamily = Oxanium)
                    Spacer(Modifier.width(7.dp))
                    Text("Elapsed", fontSize = 10.sp, color = Ink2, fontFamily = ChakraPetch)
                    Spacer(Modifier.width(6.dp))
                    Text(fmtTime(test.elapsedMs), style = NumberStyle(10f, Ink))
                }
            }

            Spacer(Modifier.height(10.dp))

            // ---- main: metrics | gauge | phases ----
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // left: live metrics — the latency cluster is ALWAYS present so
                // ping never "disappears" once the ping phase ends.
                Column(
                    modifier = Modifier.width(158.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    val pingLabel = when (test.phase) {
                        cloud.g3h.nimbus.data.Phase.PING -> "ms · live"
                        else -> "ms · final"
                    }
                    LatencyCard(
                        value = if (test.pingMs > 0.0) fmt(test.pingMs, 1) else "—",
                        unit = pingLabel,
                        active = test.phase == cloud.g3h.nimbus.data.Phase.PING,
                        failed = test.pingMs <= 0.0 && test.phase != cloud.g3h.nimbus.data.Phase.PING
                    )
                    MetricCard("Jitter", fmt(test.jitterMs, 1), "ms")
                    MetricCard("Packet loss", fmt(test.lossPct, 1), "%")
                    MetricCard("Peak", fmt(test.peakMbps, 1), "Mbps")
                }

                // centre gauge
                Gauge(
                    phase = test.phase,
                    value = when (test.phase) {
                        cloud.g3h.nimbus.data.Phase.PING -> test.pingMs
                        cloud.g3h.nimbus.data.Phase.DOWNLOAD -> liveSmooth.toDouble()
                        cloud.g3h.nimbus.data.Phase.UPLOAD -> liveSmooth.toDouble()
                    },
                    unit = when (test.phase) {
                        cloud.g3h.nimbus.data.Phase.PING -> "ms"
                        else -> "Mbps"
                    },
                    isPing = test.phase == cloud.g3h.nimbus.data.Phase.PING
                )

                // right: phase stepper
                Column(
                    modifier = Modifier.width(150.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    PhaseStep(
                        label = "PING",
                        done = test.pingDone,
                        active = test.phase == cloud.g3h.nimbus.data.Phase.PING,
                        text = when {
                            test.pingMs > 0.0 -> "${fmt(test.pingMs, 0)} ms"
                            test.phase == cloud.g3h.nimbus.data.Phase.PING -> "Measuring…"
                            else -> "Unreached — ${test.pingError ?: "see results"}"
                        }
                    )
                    PhaseStep(
                        label = "DOWNLOAD",
                        done = test.downloadDone,
                        active = test.phase == cloud.g3h.nimbus.data.Phase.DOWNLOAD,
                        text = when {
                            test.downloadDone -> "${fmt(test.downloadMbps, 1)} Mbps"
                            test.phase == cloud.g3h.nimbus.data.Phase.DOWNLOAD -> "${fmt(liveSmooth.toDouble(), 1)} Mbps"
                            else -> "Waiting"
                        }
                    )
                    PhaseStep(
                        label = "UPLOAD",
                        done = test.uploadDone,
                        active = test.phase == cloud.g3h.nimbus.data.Phase.UPLOAD,
                        text = when {
                            test.uploadDone -> "${fmt(test.uploadMbps, 1)} Mbps"
                            test.phase == cloud.g3h.nimbus.data.Phase.UPLOAD -> "${fmt(liveSmooth.toDouble(), 1)} Mbps"
                            else -> "Waiting"
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ---- footer: BACK hint ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .background(Surface, RoundedCornerShape(999.dp))
                        .border(1.dp, Line, RoundedCornerShape(999.dp))
                        .padding(horizontal = 15.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackIcon(tint = Ink, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Press BACK to cancel", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Ink, fontFamily = ChakraPetch)
                }
            }
        }

        // ---- error overlay ----
        if (!test.running && test.error != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x66F4F6F3)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Test failed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = ChakraPetch)
                    Spacer(Modifier.height(6.dp))
                    Text(test.error!!, fontSize = 11.sp, color = Ink2, fontFamily = ChakraPetch)
                    Spacer(Modifier.height(14.dp))
                    SecondaryButton("Try again", onClick = { vm.startTest() })
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, unit: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        SectionLabel(label)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = NumberStyle(17f, Ink))
            Text(unit, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Ink2, fontFamily = Oxanium)
        }
    }
}

/**
 * The always-visible ping card. Unlike [MetricCard] it is tinted so latency
 * reads as a first-class metric, and it flags the unmeasured case in the
 * accent colour instead of a dim em-dash.
 */
@Composable
private fun LatencyCard(value: String, unit: String, active: Boolean, failed: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) Tint else (if (failed) WarnBg else Surface), RoundedCornerShape(12.dp))
            .border(
                if (failed) 1.2.dp else 1.dp,
                if (failed) WarnBorder else (if (active) Blue else Line),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            SectionLabel("Ping")
            if (failed) {
                Text("not measured", fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold, color = WarnText, fontFamily = ChakraPetch)
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = NumberStyle(19f, if (failed) WarnText else Ink))
            Text(unit, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = if (failed) WarnText else Ink2, fontFamily = Oxanium)
        }
    }
}

@Composable
private fun PhaseStep(label: String, done: Boolean, active: Boolean, text: String) {
    val pulse = rememberInfiniteTransition(label = "phasePulse")
    val pulseAlpha by pulse.animateFloat(
        0.45f, 0f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseAlpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (active) Tint else Surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) Blue else Line,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // icon disc
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (active) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Blue.copy(alpha = 0.45f * pulseAlpha), size.width / 2f)
                }
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        color = when {
                            active -> BlueStrong
                            done -> Tint
                            else -> Surface
                        }, shape = CircleShape
                    )
                    .then(
                        if (!done && !active) Modifier.border(1.dp, DashedPending, CircleShape) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    done -> CheckIcon(tint = BlueStrong, modifier = Modifier.size(11.dp))
                    active -> ArrowDownIcon(tint = Surface, modifier = Modifier.size(11.dp))
                    else -> ArrowUpIcon(tint = Ink2, modifier = Modifier.size(11.dp))
                }
            }
        }
        Spacer(Modifier.width(9.dp))
        Column {
            SectionLabel(label, color = if (active) SubInk else Ink2)
            Text(
                text,
                style = if (active || done) NumberStyle(13f, Ink)
                else NumberStyle(13f, Ink2, FontWeight.Medium)
            )
        }
    }
}

/** 270° gauge, 0–1000 Mbps on a log-eased scale (Testing.dc.html). */
@Composable
fun Gauge(phase: cloud.g3h.nimbus.data.Phase, value: Double, unit: String, isPing: Boolean, modifier: Modifier = Modifier) {
    val comet = rememberInfiniteTransition(label = "comet")
    val cometAngle by comet.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "cometAngle"
    )
    val slowRing = rememberInfiniteTransition(label = "slowRing")
    val slowAngle by slowRing.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
        label = "slowAngle"
    )

    val frac = if (isPing) (min(value, 300.0) / 300.0).toFloat()
    else (ln(1.0 + min(value, 1000.0) / 10.0) / ln(101.0)).toFloat()

    Box(
        modifier = modifier.size(310.dp),
        contentAlignment = Alignment.Center
    ) {
        // white disc
        Box(
            Modifier
                .size(270.dp)
                .background(Surface, CircleShape)
                .border(1.dp, Line, CircleShape)
        )
        Canvas(Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.width * 0.387f // 240/620
            val arcW = size.width * 0.042f // 26px/2 / 620
            // track
            drawArc(
                color = Tint, startAngle = 135f, sweepAngle = 270f,
                useCenter = false, size = Size(size.width, size.height),
                topLeft = Offset(cx - size.width / 2f, cy - size.height / 2f),
                style = Stroke(arcW, cap = StrokeCap.Round)
            )
            // progress
            drawArc(
                color = Blue, startAngle = 135f, sweepAngle = 270f * frac.coerceIn(0.001f, 1f),
                useCenter = false, size = Size(size.width, size.height),
                topLeft = Offset(cx - size.width / 2f, cy - size.height / 2f),
                style = Stroke(arcW, cap = StrokeCap.Round)
            )
            // slow dotted outer ring
            val rOut = size.width * 0.484f
            val dashes = 90
            for (i in 0 until dashes) {
                val a = (i * 2f * kotlin.math.PI.toFloat() / dashes) + slowAngle * kotlin.math.PI.toFloat() / 180f
                val a2 = a + 0.01f
                drawLine(
                    OrbitDots,
                    Offset(cx + rOut * kotlin.math.cos(a), cy + rOut * kotlin.math.sin(a)),
                    Offset(cx + rOut * kotlin.math.cos(a2), cy + rOut * kotlin.math.sin(a2)),
                    strokeWidth = 1f
                )
            }
            // comet
            val cr = size.width * 0.445f
            val cStart = cometAngle
            drawArc(
                color = BlueStrong,
                startAngle = cStart, sweepAngle = 26f,
                useCenter = false,
                size = Size(cr * 2, cr * 2),
                topLeft = Offset(cx - cr, cy - cr),
                style = Stroke(size.width * 0.0065f, cap = StrokeCap.Round)
            )
        }
        // centre content
        Column(
            modifier = Modifier.matchParentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.28f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                PhaseArrowIcon(tint = BlueStrong, modifier = Modifier.size(12.dp))
                Text(
                    phase.name,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.22f.sp, color = BlueStrong, fontFamily = ChakraPetch
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (isPing) {
                    if (value > 0.0) fmt(value, 0) else "—"
                } else fmt(value, 1),
                style = NumberStyle(54f, Ink)
            )
            Text(unit, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink2, fontFamily = ChakraPetch)
            Spacer(Modifier.weight(0.24f))
        }
        // scale labels (0 / 1000)
        Row(
            modifier = Modifier.matchParentSize().padding(horizontal = 60.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text("0", style = NumberStyle(8f, Ink2), modifier = Modifier.weight(1f))
            Text(
                if (isPing) "300" else "1000",
                style = NumberStyle(8f, Ink2),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

@Composable
private fun ArrowDownIcon(tint: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        drawLine(tint, Offset(w * 0.5f, w * 0.2f), Offset(w * 0.5f, w * 0.8f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.28f, w * 0.58f), Offset(w * 0.5f, w * 0.8f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.72f, w * 0.58f), Offset(w * 0.5f, w * 0.8f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
    }
}

@Composable
private fun ArrowUpIcon(tint: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        drawLine(tint, Offset(w * 0.5f, w * 0.2f), Offset(w * 0.5f, w * 0.8f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.28f, w * 0.42f), Offset(w * 0.5f, w * 0.2f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.72f, w * 0.42f), Offset(w * 0.5f, w * 0.2f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
    }
}

@Composable
private fun PhaseArrowIcon(tint: Color, modifier: Modifier) {
    ArrowDownIcon(tint = tint, modifier = modifier)
}

@Composable
private fun BackIcon(tint: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        drawPath(
            androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.62f, w * 0.2f)
                lineTo(w * 0.3f, w * 0.5f)
                lineTo(w * 0.62f, w * 0.8f)
            }, tint, style = Stroke(w * 0.11f, cap = StrokeCap.Round)
        )
    }
}

/** Scrolling ECG + 64 bandwidth bars driven by live throughput (§7). */
@Composable
fun TestBackground(liveMbps: Double, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bg")
    val scroll by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "scroll"
    )
    val scan by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4000, easing = androidx.compose.animation.core.EaseInOut), RepeatMode.Restart),
        label = "scan"
    )
    val barsPhase by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bars"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // ECG band
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.CenterStart)
                .offset(y = (-10).dp)
        ) {
            val path = heartbeatPath(size.width * 2f, size.height, units = 12)
            val shift = -scroll * size.width.toFloat()
            // scroll the ECG path horizontally by [shift]
            drawContext.canvas.save()
            drawContext.canvas.translate(shift, 0f)
            drawPath(path, Blue.copy(alpha = 0.55f), style = Stroke(1.8f, cap = StrokeCap.Round))
            drawContext.canvas.restore()
            // scan line (not offset)
            val sx = scan * size.width
            drawRect(
                color = Blue.copy(alpha = 0.5f),
                topLeft = Offset(sx, 0f),
                size = androidx.compose.ui.geometry.Size(3f, size.height)
            )
        }
        // bandwidth bars
        val intensity = (min(liveMbps, 1000.0) / 1000.0).toFloat().coerceAtLeast(0.15f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .align(Alignment.BottomCenter)
        ) {
            val n = 64
            val gap = size.width * 0.0052f
            val bw = (size.width - gap * (n + 1)) / n
            for (i in 0 until n) {
                // pseudo-random per-bar speed/duration variation, driven by barsPhase + intensity
                val dur = (0.85f + ((i * 37) % 13) / 10f) // 0.85–1.97
                val p = ((barsPhase / dur + i * 0.13f) % 1f)
                val s = 0.18f + (1f - p) * (0.55f + 0.45f * intensity)
                val h = size.height * s
                val x = gap + i * (bw + gap)
                drawRoundRect(
                    color = if (i % 2 == 0) BarSoft else BarSoft2,
                    topLeft = Offset(x, size.height - h),
                    size = androidx.compose.ui.geometry.Size(bw, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
            }
        }
    }
}
