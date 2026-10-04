package cloud.g3h.nimbus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.g3h.nimbus.net.UpdateChecker
import java.util.Locale

/**
 * Modal consent prompt for an available / downloaded update.
 *
 * The launch check only *detects* a release — the APK is never fetched or
 * installed without the user accepting here. Rendered at the composition root
 * so it appears on whatever screen is showing when the check completes.
 *
 * Three states, mirroring the view model: AVAILABLE ("Download it now?"), READY
 * ("Install it now?") and READY + awaiting permission, where Android's
 * unknown-sources grant is the only thing missing. That last one used to stall
 * until the app was force-closed; now `NimbusViewModel.onAppResumed()` finishes
 * the install as soon as the user returns from the settings toggle.
 */
@Composable
fun UpdatePrompt(vm: NimbusViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsState()
    if (!settings.updatePromptVisible) return

    val ready = settings.updateStatus == UpdateChecker.Status.READY
    if (!ready && settings.updateStatus != UpdateChecker.Status.AVAILABLE) return
    val awaiting = settings.updateAwaitingPermission

    val version = settings.updateInfo?.tagName?.trimStart('v') ?: "?"
    val sizeMb = (settings.updateInfo?.assetSize ?: 0L) / 1_048_576.0

    val title = when {
        awaiting -> "One more step"
        ready -> "Update ready to install"
        else -> "Update available"
    }
    val body = when {
        // Android's unknown-sources grant is the only thing missing. The install
        // resumes on its own in NimbusViewModel.onAppResumed() when the user comes
        // back — the old code stalled here until the app was force-closed.
        awaiting ->
            "Android needs your permission to install the update. Turn on " +
                "\"Allow from this source\" for Nimbus, then come back here — the " +
                "update installs on its own, no restart needed."
        ready ->
            "Nimbus v$version has been downloaded. Install it now? The app will restart."
        else ->
            "Nimbus v$version is available (${String.format(Locale.getDefault(), "%.1f", sizeMb)} MB). " +
                "Download it now?"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x8022384E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(430.dp)
                .background(Surface, RoundedCornerShape(16.dp))
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                title,
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = ChakraPetch
            )
            Text(
                body,
                fontSize = 11.sp, color = Ink2, fontFamily = ChakraPetch,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    label = if (awaiting) "Not now" else "Later",
                    onClick = { vm.dismissUpdatePrompt() },
                    sizeSp = 11f
                )
                PrimaryButton(
                    label = when {
                        awaiting -> "Open settings"
                        ready -> "Install now"
                        else -> "Download"
                    },
                    onClick = { if (ready) vm.installUpdate() else vm.downloadUpdate() },
                    sizeSp = 11f
                )
            }
        }
    }
}
