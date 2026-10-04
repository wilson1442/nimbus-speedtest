package cloud.g3h.nimbus.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import cloud.g3h.nimbus.data.TestResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val histDayFmt = SimpleDateFormat("MMM d", Locale.getDefault())
private val histTimeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun dateLabel(ts: Long): String {
    val now = System.currentTimeMillis()
    val dayStart = 86_400_000L
    val today = now - (now % dayStart)
    return when {
        ts >= today -> "Today · " + histTimeFmt.format(Date(ts))
        ts >= today - dayStart -> "Yesterday · " + histTimeFmt.format(Date(ts))
        else -> "${histDayFmt.format(Date(ts))} · " + histTimeFmt.format(Date(ts))
    }
}

private val ranges = listOf(7 to "7D", 30 to "30D", 90 to "90D", 0 to "All")

/** History & averages screen (History.dc.html). */
@Composable
fun HistoryScreen(
    vm: NimbusViewModel,
    modifier: Modifier = Modifier
) {
    val h by vm.history.collectAsState()
    val conn by vm.conn.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .padding(horizontal = 48.dp, vertical = 27.dp)
    ) {
        // top bar
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                NimbusLogoTile()
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("Nimbus", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = ChakraPetch)
                    Text("SPEED TEST", style = LabelStyle(7.5f).copy(letterSpacing = (0.24f * 7.5f).sp))
                }
            }
            // range selector (right, per mockup)
            Row(
                modifier = Modifier
                    .background(Surface, RoundedCornerShape(999.dp))
                    .border(1.dp, Line, RoundedCornerShape(999.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ranges.forEach { (days, label) ->
                    RangePill(label = label, active = h.rangeDays == days, onClick = { vm.setRange(days) })
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // ---- left column ----
            Column(
                modifier = Modifier.width(200.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val stats = h.stats
                // big avg download card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BlueStrong, RoundedCornerShape(14.dp))
                        .padding(horizontal = 17.dp, vertical = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "AVG DOWNLOAD · ${if (h.rangeDays == 0) "ALL TIME" else "${h.rangeDays} DAYS"}",
                        fontSize = 8.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2f.sp, color = PressOk, fontFamily = ChakraPetch
                    )
                    Text(
                        if (stats != null) fmt(stats.avgDownloadMbps, 1) else "—",
                        style = NumberStyle(32f, Surface)
                    )
                    Text(
                        if (stats != null) "Mbps · range ${fmt(stats.minDownloadMbps, 0)}–${fmt(stats.maxDownloadMbps, 0)}"
                        else "No data in range",
                        fontSize = 10.sp, color = PressOk, fontFamily = ChakraPetch
                    )
                }
                // 2×2 grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("AVG UPLOAD", stats?.let { fmt(it.avgUploadMbps, 1) } ?: "—", "Mbps", Modifier.weight(1f))
                        MiniStat("AVG PING", stats?.let { fmt(it.avgPingMs, 0) } ?: "—", "ms", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("TESTS SAVED", stats?.count?.toString() ?: "0", "this period", Modifier.weight(1f))
                        MiniStat("CONSISTENCY", "${fmt(h.consistencyPct, 0)}%", "within ±15%", Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.weight(1f))
                // footer
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TvIcon(Modifier.size(10.dp))
                    Text("History is saved on this TV", fontSize = 9.sp, color = Ink2, fontFamily = ChakraPetch)
                }
                WarnButton("Clear history", onClick = { vm.askClearHistory() }, icon = { TrashIcon(WarnText, it) })
            }

            // ---- right column ----
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // chart
                ChartCard(
                    title = "Download speed · last ${max(h.results.size, 1)} tests",
                    results = h.results,
                    avg = h.stats?.avgDownloadMbps,
                    modifier = Modifier.height(160.dp)
                )
                // recent tests list
                RecentTestsCard(
                    results = h.results,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // clear-history confirm dialog
    if (h.confirmClear) {
        ConfirmClearDialog(
            onConfirm = { vm.confirmClearHistory() },
            onDismiss = { vm.dismissClearHistory() }
        )
    }
}

@Composable
private fun RangePill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .nimbusFocus(cornerRadius = 999.dp, ringThickness = 1.5.dp, gap = 2.dp)
            .background(if (active) BlueStrong else Color.Transparent, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) Surface else Ink2,
            fontFamily = ChakraPetch
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Surface, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        SectionLabel(label)
        Text(value, style = NumberStyle(17f, Ink))
        Text(sub, fontSize = 8.5.sp, color = Ink2, fontFamily = ChakraPetch)
    }
}

/** Line chart of download speed per test, with dashed period-average. */
@Composable
fun ChartCard(
    title: String,
    results: List<TestResult>,
    avg: Double?,
    modifier: Modifier = Modifier
) {
    // oldest → newest, max 30 points
    val points = results.takeLast(30).reversed()
    Column(
        modifier = modifier
            .background(Surface, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink, fontFamily = ChakraPetch, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.width(14.dp).height(2.dp).background(BlueStrong))
                    Text("Each test", fontSize = 8.5.sp, color = Ink2, fontFamily = ChakraPetch)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.width(14.dp).height(2.dp).drawDashed(AvgLine))
                    Text(
                        "Average ${avg?.let { fmt(it, 1) } ?: "—"}",
                        fontSize = 8.5.sp, color = Ink2, fontFamily = ChakraPetch
                    )
                }
            }
        }
        if (points.size >= 2 && avg != null) {
            LineChart(points = points.map { it.downloadMbps }, avg = avg, modifier = Modifier.fillMaxWidth().height(90.dp))
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Not enough tests yet to draw a chart.", fontSize = 10.sp, color = Ink2, fontFamily = ChakraPetch)
            }
        }
    }
}

/** Area + line chart with dashed average line and y grid. */
@Composable
fun LineChart(points: List<Double>, avg: Double, modifier: Modifier = Modifier, drawProgressMs: Int = 2400) {
    val t = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(points, avg) {
        t.snapTo(0f)
        t.animateTo(1f, androidx.compose.animation.core.tween(drawProgressMs))
    }
    val progress = t.value
    // Scale is computed once per dataset, not on every animation frame.
    val maxV = remember(points, avg) { max(points.maxOrNull() ?: 0.0, avg) * 1.12 }
    Canvas(modifier = modifier) {
        val minV = 0.0
        val n = points.size
        val xStep = size.width / (n - 1)
        fun y(v: Double) = size.height * (1f - ((v - minV) / (maxV - minV)).toFloat())

        // gridlines
        for (i in 0..3) {
            val gy = size.height * i / 3f
            drawLine(
                if (i == 3) Line else ChartGrid,
                Offset(0f, gy), Offset(size.width, gy),
                strokeWidth = 1f
            )
        }

        // visible prefix (left→right draw-in)
        val visible = (n * progress).coerceAtLeast(1f).coerceAtMost(n.toFloat())
        fun px(i: Int) = i * xStep

        // average dashed line
        drawDashedLineY(size.width, y(avg), AvgLine)

        val fullVisible = min(visible.toInt(), n - 1)
        // area
        val area = Path().apply {
            moveTo(0f, size.height)
            for (i in 0..fullVisible) lineTo(px(i), y(points[i]))
            if (fullVisible + 1 < n && visible > fullVisible + 0.5f) {
                val frac = (visible - fullVisible).coerceIn(0f, 1f)
                val x = px(fullVisible) + (px(fullVisible + 1) - px(fullVisible)) * frac
                val v = points[fullVisible] + (points[fullVisible + 1] - points[fullVisible]) * frac
                lineTo(x, y(v))
            }
            lineTo(px(fullVisible), size.height)
            close()
        }
        drawPath(area, Color(0x1F5B9BD5))

        // line
        val line = Path().apply {
            moveTo(0f, y(points[0]))
            for (i in 1..fullVisible) lineTo(px(i), y(points[i]))
            if (fullVisible + 1 < n && visible > fullVisible + 0.5f) {
                val frac = (visible - fullVisible).coerceIn(0f, 1f)
                val x = px(fullVisible) + (px(fullVisible + 1) - px(fullVisible)) * frac
                val v = points[fullVisible] + (points[fullVisible + 1] - points[fullVisible]) * frac
                lineTo(x, y(v))
            }
        }
        drawPath(line, BlueStrong, style = Stroke(1.8f, cap = StrokeCap.Round))
    }
}

/** Draw a horizontal dashed line across the canvas at [yPos]. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDashedLineY(width: Float, yPos: Float, color: Color, dash: Float = 8f) {
    var x = 0f
    while (x < width) {
        drawLine(color, Offset(x, yPos), Offset(min(x + dash, width), yPos), strokeWidth = 1.5f)
        x += dash * 2f
    }
}

private fun Modifier.drawDashed(color: Color): Modifier = drawBehind {
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, size.height / 2f), Offset(min(x + 4f, size.width), size.height / 2f), strokeWidth = 2f)
        x += 8f
    }
}

/** Recent tests table (D-pad scrollable, latest row highlighted). */
@Composable
fun RecentTestsCard(results: List<TestResult>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    Column(
        modifier = modifier
            .background(Surface, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent tests", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink, fontFamily = ChakraPetch, modifier = Modifier.weight(1f))
            Text("Scroll with ▲ ▼ on the remote", fontSize = 8.5.sp, color = Ink2, fontFamily = ChakraPetch)
        }
        if (results.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                Text("No tests in this range.", fontSize = 10.sp, color = Ink2, fontFamily = ChakraPetch)
            }
            return
        }
        // header
        Row(Modifier.fillMaxWidth()) {
            HeadCell("DATE", 0.16f)
            HeadCell("DOWNLOAD", 0.14f)
            HeadCell("UPLOAD", 0.13f)
            HeadCell("PING", 0.10f)
            HeadCell("CONNECTION", 0.17f)
            HeadCell("VPN", 0.09f)
            HeadCell("WAN IP", 0.21f)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(results) { index, r ->
                HistoryRow(r, highlight = index == 0)
            }
        }
    }
}

@Composable
private fun RowScope.HeadCell(text: String, w: Float) {
    Text(
        text,
        modifier = Modifier.weight(w),
        fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.12f.sp, color = Ink2, fontFamily = ChakraPetch
    )
}

@Composable
private fun HistoryRow(r: TestResult, highlight: Boolean) {
    Row(
        modifier = Modifier
                .fillMaxWidth()
                .background(if (highlight) HighlightRow else Color.Transparent)
            .padding(start = 6.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dateLabel(r.timestamp),
            modifier = Modifier.weight(0.16f),
            fontSize = 9.sp, fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Medium,
            color = Ink, fontFamily = ChakraPetch,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(fmt(r.downloadMbps, 1) + " Mbps", modifier = Modifier.weight(0.14f), style = NumberStyle(9.5f, Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(fmt(r.uploadMbps, 1) + " Mbps", modifier = Modifier.weight(0.13f), style = NumberStyle(9.5f, Ink2, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(fmt(r.pingMs, 0) + " ms", modifier = Modifier.weight(0.10f), style = NumberStyle(9.5f, Ink2, FontWeight.Medium), maxLines = 1)
        Text(r.connectionLabel(r.connectionType), modifier = Modifier.weight(0.17f), fontSize = 9.sp, color = Ink2, fontFamily = ChakraPetch, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.weight(0.09f)) { VpnPill(r.vpnActive) }
        Text(
            r.wanIp ?: "—",
            modifier = Modifier.weight(0.21f),
            style = NumberStyle(9.5f, Ink2, FontWeight.Medium),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

private fun TestResult.connectionLabel(type: String): String = when (type) {
    "ETHERNET" -> "Ethernet"
    "WIFI_5GHZ" -> "Wi‑Fi 5 GHz"
    "WIFI_2_4GHZ" -> "Wi‑Fi 2.4 GHz"
    "WIFI" -> "Wi‑Fi"
    else -> "Other"
}

@Composable
fun ConfirmClearDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x8022384E))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .background(Surface, RoundedCornerShape(16.dp))
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Clear history?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = ChakraPetch)
            Spacer(Modifier.height(8.dp))
            Text(
                "This deletes every saved test on this TV. This can't be undone.",
                fontSize = 10.5.sp, color = Ink2, fontFamily = ChakraPetch,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                WarnButton("Delete", onClick = onConfirm, modifier = Modifier.weight(1f))
            }
        }
    }
}
