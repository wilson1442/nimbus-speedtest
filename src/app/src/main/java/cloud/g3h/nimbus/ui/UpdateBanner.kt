package cloud.g3h.nimbus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.g3h.nimbus.net.UpdateChecker

/**
 * Global "update available / downloading / ready" banner, rendered at the
 * composition root so an automatic update is visible from every screen — not
 * only in Settings. Renders nothing unless an update is in flight.
 */
@Composable
fun UpdateBanner(vm: NimbusViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsState()
    val status = settings.updateStatus
    // Only the in-flight download needs a persistent banner; "available" and
    // "ready" are handled by the modal UpdatePrompt (explicit consent).
    if (status != UpdateChecker.Status.DOWNLOADING) return

    val version = settings.updateInfo?.tagName?.trimStart('v') ?: "?"
    val text = "Downloading update v$version · ${settings.downloadPct}%"

    Row(
        modifier = modifier
            .background(Tint, RoundedCornerShape(999.dp))
            .border(1.dp, CardBorderActive, RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        PulseIcon(BlueStrong, Modifier.size(14.dp))
        Text(
            text,
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = Ink, fontFamily = ChakraPetch
        )
    }
}
