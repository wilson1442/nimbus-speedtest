package cloud.g3h.nimbus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay

/** Settings screen (spec §3.5 — not designed; reuses house styles). */
@Composable
fun SettingsScreen(
    vm: NimbusViewModel,
    modifier: Modifier = Modifier
) {
    val settings by vm.settings.collectAsState()
    var editing by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .padding(horizontal = 48.dp, vertical = 27.dp)
    ) {
        // top bar
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            }
            Text(
                "Settings",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = ChakraPetch
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsRow(
                label = "Speed test server",
                sub = if (settings.serverUrl.isBlank()) "Not set — required to run a test"
                else settings.serverUrl,
                trailing = {
                    SecondaryButton("Edit", onClick = { editing = "server" to settings.serverUrl }, sizeSp = 10f)
                }
            )
            SettingsRow(
                label = "IP lookup URL",
                sub = if (settings.ipLookupUrl.isBlank()) "Not set — WAN IP shows as unavailable"
                else settings.ipLookupUrl,
                trailing = {
                    SecondaryButton("Edit", onClick = { editing = "ip" to settings.ipLookupUrl }, sizeSp = 10f)
                }
            )
            SettingsRow(
                label = "Test duration",
                sub = if (settings.shortDuration) "Short (~20 s): quick ballpark" else "Normal (~30 s): more accurate",
                trailing = {
                    Box(
                        modifier = Modifier
                            .background(if (settings.shortDuration) Tint else NeutralPill, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            if (settings.shortDuration) "Short" else "Normal",
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            color = if (settings.shortDuration) Ink else Ink2,
                            fontFamily = ChakraPetch
                        )
                    }
                    SecondaryButton("Change", onClick = { vm.toggleShortDuration() }, sizeSp = 10f)
                }
            )
            SettingsRow(
                label = "History",
                sub = "Tests are saved on this TV",
                trailing = {
                    WarnButton("Clear history", onClick = { vm.askClearHistory() }, icon = { TrashIcon(WarnText, it) }, sizeSp = 9.5f)
                }
            )
            Spacer(Modifier.weight(1f))
            SettingsRow(
                label = "About",
                sub = "Nimbus Speed Test · v${settings.versionName} · cloud.g3h.nimbus"
            )
        }
    }

    val edit = editing
    if (edit != null) {
        val isServer = edit.first == "server"
        TextEntryDialog(
            title = if (isServer) "Speed test server" else "IP lookup URL",
            initial = edit.second,
            onDone = { value ->
                if (isServer) vm.setServerUrl(value) else vm.setIpLookupUrl(value)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
    if (vm.history.value.confirmClear) {
        ConfirmClearDialog(
            onConfirm = { vm.confirmClearHistory() },
            onDismiss = { vm.dismissClearHistory() }
        )
    }
}

/**
 * Text entry for a D-pad-only TV: focusable character grid + Backspace/Done.
 * Focus the letter to append it; Backspace to delete; Done to save.
 */
@Composable
private fun TextEntryDialog(
    title: String,
    initial: String,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val doneFocus = FocusRequester()

    val rows = listOf(
        "a b c d e f g",
        "h i j k l m n",
        "o p q r s t u",
        "v w x y z 0-9"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x8022384E))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(460.dp)
                .background(Surface, RoundedCornerShape(16.dp))
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = ChakraPetch)
            Spacer(Modifier.height(10.dp))
            // input line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Tint, RoundedCornerShape(9.dp))
                    .border(1.dp, CardBorderActive, RoundedCornerShape(9.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    if (text.isEmpty()) " " else text,
                    style = NumberStyle(12f, Ink),
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(12.dp))
            // character grid (each letter focusable)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.trim().split(" ").forEach { ch ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .nimbusFocus(cornerRadius = 8.dp, ringThickness = 1.5.dp, gap = 2.dp)
                                .background(Surface, RoundedCornerShape(8.dp))
                                .border(1.dp, Line, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (ch == "0-9") {
                                        // cycle digits 0-9 with repeated presses
                                        val last = text.lastOrNull()
                                        if (last != null && last.isDigit() && last < '9') text = text.dropLast(1) + (last + 1)
                                        else text += '0'
                                    } else if (text.length < 60) {
                                        text += ch
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (ch == "0-9") "0–9" else ch.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink,
                                fontFamily = ChakraPetch
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton("Backspace", onClick = { text = text.dropLast(1) }, modifier = Modifier.weight(1f), sizeSp = 10.5f)
                SecondaryButton("Clear", onClick = { text = "" }, modifier = Modifier.weight(1f), sizeSp = 10.5f)
                PrimaryButton(
                    label = "Done",
                    onClick = { onDone(text.trim().trimEnd('/')) },
                    modifier = Modifier.weight(1.4f).focusRequester(doneFocus),
                    sizeSp = 11f
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Focus a character and press OK to type. BACK cancels.", fontSize = 8.5.sp, color = Ink2, fontFamily = ChakraPetch)
        }
    }
    LaunchedEffect(Unit) { doneFocus.requestFocus() }
}
