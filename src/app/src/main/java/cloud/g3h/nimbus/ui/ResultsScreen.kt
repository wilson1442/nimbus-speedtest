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
        r.downloadMbps >= 100 && r.pingMs <= 40 -> "Your connection is running strong"
        r.downloadMbps >= 25 -> "Solid connection — ready for 4K"
        r.downloadMbps >= 5 -> "Usable, but not for heavy streaming"
        else -> "Slow connection detected"
    }

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

        // three cards
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResultCard(
                modifier = Modifier.weight(1f),
                label = "DOWNLOAD",
                value = fmt(r.downloadMbps, 1),
                unit = "Mbps",
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
                value = fmt(r.pingMs, 0),
                unit = "ms · jitter ${fmt(r.jitterMs, 1)} ms · loss ${fmt(r.packetLossPct, 0)}%",
                delta = deltaPill(avg?.avgPingMs, r.pingMs, "vs avg", lowerIsBetter = true),
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
            GoodForChip("Online gaming", r.pingMs <= 40 && r.jitterMs <= 10) { GameIcon(it) }
            GoodForChip("Video calls", r.uploadMbps >= 5) { VideoIcon(it) }
            GoodForChip("Multiple devices", r.downloadMbps >= 100) { DevicesIcon(it) }
        }

        Spacer(Modifier.height(12.dp))

        // footer buttons
        val againFocus = FocusRequester()
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
