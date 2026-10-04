package cloud.g3h.nimbus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.Text
import cloud.g3h.nimbus.data.TestResult
import cloud.g3h.nimbus.engine.QualityScore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val resTimeFmt = SimpleDateFormat("EEE · h:mm a", Locale.getDefault())

/** Results screen (Results.dc.html). */
@Composable
fun ResultsScreen(
    vm: NimbusViewModel,
    modifier: Modifier = Modifier
) {
    val rs by vm.result.collectAsState()
    val avg by vm.avg30.collectAsState()
    val conn by vm.conn.collectAsState()

    if (rs == null) {
        Box(modifier = modifier.fillMaxSize().background(Ground), contentAlignment = Alignment.Center) {
            Text("No result yet", fontSize = 14.sp, color = Ink2, fontFamily = ChakraPetch)
        }
        return
    }
    val r = rs!!.result
    val samples = rs!!

    val headline = when {
        r.downloadMbps >= 100 && r.pingMs in 1.0..40.0 -> "Your connection is running strong"
        r.downloadMbps >= 25 -> "Solid connection — ready for 4K"
        r.downloadMbps >= 5 -> "Usable, but not for heavy streaming"
        else -> "Slow connection detected"
    }
    val pingFailed = r.pingMs <= 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .padding(horizontal = 48.dp, vertical = 27.dp)
    ) {
        // top bar
        NimbusTopBar(
            active = NimbusAppScreen.TEST,
            onNavigate = { vm.navigate(it.toScreen()) },
            state = conn
        )
        Spacer(Modifier.height(14.dp))

        // heading row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "TEST COMPLETE",
                    fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.22f.sp, color = BlueStrong, fontFamily = ChakraPetch
                )
                Text(
                    headline,
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = ChakraPetch
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // VPN chip / saved chip
                Row(
                    modifier = Modifier
                        .background(if (r.vpnActive) Tint else Surface, RoundedCornerShape(999.dp))
                        .border(1.dp, if (r.vpnActive) CardBorderActive else Line, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (r.vpnActive) {
                        ShieldIcon(true, BlueStrong, Modifier.size(11.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Tested over VPN${r.wanIp?.let { " · WAN IP $it" } ?: ""}",
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Ink, fontFamily = ChakraPetch
                        )
                    } else {
                        CheckIcon(BlueStrong, Modifier.size(11.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("No VPN · Saved to history", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Ink, fontFamily = ChakraPetch)
                    }
                }
                Text(
                    "${resTimeFmt.format(Date(r.timestamp))} · ${conn.connectionLabel()} · Server: ${r.serverName}",
                    fontSize = 11.sp, color = Ink2, fontFamily = ChakraPetch
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ping-failure diagnostic (every ping sample failed → 0 ms is not a speed, explain why)
        if (pingFailed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarnBg, RoundedCornerShape(13.dp))
                    .border(1.dp, WarnBorder, RoundedCornerShape(13.dp))
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoCircleIcon(tint = WarnText, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Ping could not be measured", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WarnText, fontFamily = ChakraPetch)
                    Text(
                        "All 10 latency samples to ${r.serverName} failed — likely the server is unreachable from this TV, or its TLS handshake is timing out. Upload/download below were reached. In Settings, try a different speed-test server, check the TV's date/time & DNS, or test on another device on the same network to isolate it.",
                        fontSize = 9.sp, color = WarnText, fontFamily = ChakraPetch
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // connection-quality hero: the headline latency number (median ping in ms,
        // always visible) plus min/max, jitter, loss, and a 0-100 grade.
        val maxPing = samples.pingSamples.maxOrNull() ?: 0.0
        QualityHero(
            score = QualityScore.score(r.downloadMbps, r.uploadMbps, r.pingMs, r.jitterMs, r.packetLossPct),
            pingMs = r.pingMs,
            minPingMs = if (r.minPingMs > 0.0) r.minPingMs else (samples.pingSamples.minOrNull() ?: 0.0),
            maxPingMs = if (maxPing > 0.0) maxPing else (samples.pingSamples.maxOrNull() ?: 0.0),
            jitterMs = r.jitterMs,
            lossPct = r.packetLossPct
        )
        Spacer(Modifier.height(14.dp))

        // three cards
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResultCard(
                modifier = Modifier.weight(1f),
                label = "DOWNLOAD",
                value = fmt(r.downloadMbps, 1),
                unit = if (r.peakMbps > 0.0 && r.peakMbps > r.downloadMbps) "Mbps · peak ${fmt(r.peakMbps, 1)}" else "Mbps sustained",
                delta = deltaPill(avg?.avgDownloadMbps, r.downloadMbps, "vs avg", lowerIsBetter = false),
                samples = samples.downloadSamples,
                lineColor = Blue,
                highlight = true
            )
            ResultCard(
                modifier = Modifier.weight(1f),
                label = "UPLOAD",
                value = fmt(r.uploadMbps, 1),
                unit = "Mbps",
                delta = deltaPill(avg?.avgUploadMbps, r.uploadMbps, "vs avg", lowerIsBetter = false),
                samples = samples.uploadSamples,
                lineColor = BlueSoft
            )
            ResultCard(
                modifier = Modifier.weight(1f),
                label = "PING",
                value = if (r.pingMs > 0.0) fmt(r.pingMs, 1) else "—",
                unit = if (r.minPingMs > 0.0 || r.pingMs > 0.0) {
                    val mn = if (r.minPingMs > 0.0) r.minPingMs else (samples.pingSamples.minOrNull() ?: r.pingMs)
                    val mx = samples.pingSamples.maxOrNull() ?: r.pingMs
                    "ms · min ${fmt(mn, 1)} · max ${fmt(mx, 1)} · jitter ${fmt(r.jitterMs, 1)}"
                } else "latency not measured",
                delta = if (r.pingMs > 0.0) deltaPill(avg?.avgPingMs, r.pingMs, "vs avg", lowerIsBetter = true) else null,
                samples = samples.pingSamples,
                lineColor = BlueSoft
            )
        }

        Spacer(Modifier.height(12.dp))

        // Good-for chips
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("Good for")
            Spacer(Modifier.width(4.dp))
            GoodForChip("4K HDR streaming", r.downloadMbps >= 25) { TvIcon(it) }
            GoodForChip("Online gaming", r.pingMs > 0 && r.pingMs <= 40 && r.jitterMs <= 10) { GameIcon(it) }
            GoodForChip("Video calls", r.uploadMbps >= 5) { VideoIcon(it) }
            GoodForChip("Multiple devices", r.downloadMbps >= 100) { DevicesIcon(it) }
        }

        Spacer(Modifier.height(12.dp))

        // footer buttons
        val againFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { againFocus.requestFocus() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryButton(
                label = "Test again",
                onClick = { vm.startTest() },
                icon = { RefreshIcon(Surface, it) },
                modifier = Modifier.focusRequester(againFocus),
                sizeSp = 12f
            )
            Spacer(Modifier.width(12.dp))
            SecondaryButton("View history", onClick = { vm.navigate(Screen.HISTORY) }, icon = { ChartIcon(Ink, it) })
            Spacer(Modifier.width(12.dp))
            SecondaryButton("Done", onClick = { vm.navigate(Screen.HOME) })
        }
    }
}

private fun deltaPill(avg: Double?, value: Double, suffix: String, lowerIsBetter: Boolean): Pair<String, Boolean>? {
    if (avg == null || avg == 0.0) return null
    val diff = value - avg
    val pct = (diff / avg) * 100.0
    val positive = if (lowerIsBetter) diff <= 0 else diff >= 0
    val sign = if (pct >= 0) "+" else "−"
    val text = if (Math.abs(pct) < 1) "±0% $suffix" else "${sign}${Math.abs(pct).toInt()}% $suffix"
    return text to positive
}

/**
 * Connection-quality hero (Results): makes the *latency* a first-class,
 * always-visible number. Left: median ping in ms (or a clear "not measured"),
 * flanked by min/max. Right: a 0-100 score + grade. Jitter and loss ride
 * along as the small latency context. Color-coded by grade.
 */
@Composable
fun QualityHero(
    score: cloud.g3h.nimbus.engine.QualityScore.Report,
    pingMs: Double,
    minPingMs: Double,
    maxPingMs: Double,
    jitterMs: Double,
    lossPct: Double,
    modifier: Modifier = Modifier
) {
    val pinged = pingMs > 0.0
    val accent = when {
        score.score >= 75 -> BlueStrong
        score.score >= 45 -> AvgLine
        pinged -> WarnText
        else -> Ink2
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Surface, RoundedCornerShape(16.dp))
            .border(1.5.dp, accent.copy(alpha = (if (pinged) 0.9f else 0.55f)), RoundedCornerShape(16.dp))
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- left: latency headline ---
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Latency")
                if (!pinged) {
                    Spacer(Modifier.width(4.dp))
                    Text("not measured", fontSize = 8.5.sp, color = WarnText, fontFamily = ChakraPetch)
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    if (pinged) fmt(pingMs, 1) else "—",
                    style = NumberStyle(38f, if (pinged) Ink else Ink2)
                )
                Text("ms · median", fontSize = 11.sp, color = Ink2, fontFamily = ChakraPetch)
            }
            Spacer(Modifier.height(1.dp))
            Text(
                if (pinged)
                    "min ${fmt(if (minPingMs > 0.0) minPingMs else pingMs, 1)}  ·  max ${fmt(if (maxPingMs > 0.0) maxPingMs else pingMs, 1)}  ·  jitter ${fmt(jitterMs, 1)} ms  ·  loss ${fmt(lossPct, 0)}%"
                else "the box could not reach the server for latency — download/upload above still ran",
                fontSize = 10.sp, color = if (pinged) Ink2 else WarnText, fontFamily = ChakraPetch
            )
        }
        // --- divider ---
        Box(Modifier.width(0.5.dp).height(56.dp).background(Line))
        Spacer(Modifier.width(20.dp))
        // --- right: quality score ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SectionLabel("Quality")
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(score.score.toString(), style = NumberStyle(38f, accent))
                Text("/ 100", fontSize = 11.sp, color = Ink2, fontFamily = ChakraPetch)
            }
            Spacer(Modifier.height(1.dp))
            Text(score.grade, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accent, fontFamily = ChakraPetch)
        }
    }
}

@Composable
fun ResultCard(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String,
    delta: Pair<String, Boolean>?,
    samples: List<Double>,
    lineColor: Color,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier
            .background(Surface, RoundedCornerShape(16.dp))
            .border(
                width = if (highlight) 1.5.dp else 1.dp,
                color = if (highlight) Blue else Line,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2f.sp, color = BlueStrong, fontFamily = ChakraPetch,
                modifier = Modifier.weight(1f)
            )
            if (delta != null) DeltaPill(delta.first, delta.second)
        }
        Text(value, style = NumberStyle(42f, Ink))
        Text(unit, fontSize = 11.sp, color = Ink2, fontFamily = ChakraPetch)
        Spacer(Modifier.height(2.dp))
        Sparkline(samples, Modifier.fillMaxWidth().height(28.dp), color = lineColor)
    }
}

@Composable
fun GoodForChip(label: String, passes: Boolean, icon: @Composable (Modifier) -> Unit) {
    Row(
        modifier = Modifier
            .background(Surface, RoundedCornerShape(999.dp))
            .border(1.dp, if (passes) CardBorderActive else Line, RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon(Modifier.size(11.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (passes) FontWeight.SemiBold else FontWeight.Medium,
            color = if (passes) Ink else Ink2,
            fontFamily = ChakraPetch
        )
    }
}

// small icons
@Composable fun TvIcon(m: Modifier) = Canvas(m) {
    val w = size.width
    drawRoundRect(color = BlueStrong, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, w * 0.7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f, w * 0.1f), style = Stroke(w * 0.09f))
    drawLine(BlueStrong, Offset(w * 0.3f, w), Offset(w * 0.7f, w), strokeWidth = w * 0.09f)
}

@Composable fun GameIcon(m: Modifier) = Canvas(m) {
    val w = size.width
    drawRoundRect(color = BlueStrong, topLeft = Offset(0f, w * 0.15f), size = androidx.compose.ui.geometry.Size(w, w * 0.7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.35f, w * 0.35f), style = Stroke(w * 0.09f))
    drawLine(BlueStrong, Offset(w * 0.25f, w * 0.5f), Offset(w * 0.45f, w * 0.5f), strokeWidth = w * 0.08f)
}

@Composable fun VideoIcon(m: Modifier) = Canvas(m) {
    val w = size.width
    drawRoundRect(color = BlueStrong, topLeft = Offset(0f, w * 0.2f), size = androidx.compose.ui.geometry.Size(w * 0.62f, w * 0.6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f, w * 0.1f), style = Stroke(w * 0.09f))
    drawPath(
        Path().apply {
            moveTo(w * 0.68f, w * 0.45f); lineTo(w, w * 0.3f); lineTo(w, w * 0.7f); lineTo(w * 0.68f, w * 0.55f); close()
        }, BlueStrong
    )
}

@Composable fun DevicesIcon(m: Modifier) = Canvas(m) {
    val w = size.width
    drawCircle(color = BlueStrong, radius = w * 0.18f, center = Offset(w * 0.5f, w * 0.5f), style = Stroke(w * 0.09f))
    for (a in floatArrayOf(0f, 90f, 180f, 270f)) {
        val rad = a * kotlin.math.PI.toFloat() / 180f
        drawLine(
            BlueStrong,
            Offset(w * 0.5f + w * 0.28f * kotlin.math.cos(rad), w * 0.5f + w * 0.28f * kotlin.math.sin(rad)),
            Offset(w * 0.5f + w * 0.44f * kotlin.math.cos(rad), w * 0.5f + w * 0.44f * kotlin.math.sin(rad)),
            strokeWidth = w * 0.09f
        )
    }
}

@Composable fun RefreshIcon(tint: Color, m: Modifier) = Canvas(m) {
    val w = size.width
    drawArc(tint, 300f, 250f, false, style = Stroke(w * 0.1f, cap = StrokeCap.Round), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, w))
    drawPath(
        Path().apply {
            moveTo(w * 0.95f, w * 0.05f); lineTo(w * 0.95f, w * 0.35f); lineTo(w * 0.65f, w * 0.35f)
        }, tint, style = Stroke(w * 0.1f, cap = StrokeCap.Round)
    )
}

@Composable fun ChartIcon(tint: Color, m: Modifier) = Canvas(m) {
    val w = size.width
    drawLine(tint, Offset(0f, w), Offset(w, w), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(w * 0.1f, w * 0.7f); lineTo(w * 0.35f, w * 0.35f); lineTo(w * 0.6f, w * 0.55f); lineTo(w * 0.9f, w * 0.15f)
        }, tint, style = Stroke(w * 0.09f, cap = StrokeCap.Round)
    )
}

/** Map a TopBar nav pill to the ViewModel screen enum. */
