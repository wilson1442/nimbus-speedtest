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
import cloud.g3h.nimbus.net.SpeedServer
import cloud.g3h.nimbus.net.SpeedServers
import kotlinx.coroutines.delay

/** Settings screen (spec §3.5 — not designed; reuses house styles). */
@Composable
fun SettingsScreen(
    vm: NimbusViewModel,
    modifier: Modifier = Modifier
) {
    val settings by vm.settings.collectAsState()
    val probe by vm.probe.collectAsState()
    var editing by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pickingServer by remember { mutableStateOf(false) }

    // BACK closes whichever dialog is open before doing anything else. Keyed on
    // the open-dialog state so the callback tracks it instead of being written
    // during composition.
    DisposableEffect(editing, pickingServer) {
        vm.dialogBack = when {
            pickingServer -> { { pickingServer = false } }
            editing != null -> { { editing = null } }
            else -> null
        }
        onDispose { vm.dialogBack = null }
    }

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
                else "${SpeedServers.displayName(settings.serverUrl)} · ${settings.serverUrl}",
                trailing = {
                    if (vm.probe.value.checking) {
                        Text("Testing…", fontSize = 9.sp, color = Ink2)
                    } else if (vm.probe.value.ok) {
                        Text("Reachable · ${fmt(vm.probe.value.bestMs, 0)} ms", fontSize = 9.sp, color = Ink)
                    } else if (vm.probe.value.error != null) {
                        Text(vm.probe.value.error ?: "unreachable", fontSize = 9.sp, color = WarnText)
                    }
                    SecondaryButton("Test", onClick = { vm.testServer() }, sizeSp = 9f)
                    Spacer(Modifier.width(8.dp))
                    SecondaryButton("Choose", onClick = { pickingServer = true }, sizeSp = 10f)
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
            SettingsRow(
                label = "Auto-update",
                sub = if (settings.autoUpdate)
                    "Checks for a new release on launch and installs it"
                else "Off — use Check below to look manually",
                trailing = {
                    Box(
                        modifier = Modifier
                            .background(if (settings.autoUpdate) Tint else NeutralPill, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            if (settings.autoUpdate) "On" else "Off",
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            color = if (settings.autoUpdate) Ink else Ink2,
                            fontFamily = ChakraPetch
                        )
                    }
                    SecondaryButton("Change", onClick = { vm.setAutoUpdate(!settings.autoUpdate) }, sizeSp = 10f)
                }
            )
            UpdateRow(settings = settings, vm = vm)
            Spacer(Modifier.weight(1f))
            SettingsRow(
                label = "About",
                sub = "Nimbus Speed Test · v${settings.versionName} (build ${cloud.g3h.nimbus.BuildConfig.VERSION_CODE}) · cloud.g3h.nimbus"
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
    if (pickingServer) {
        ServerPickerDialog(
            current = settings.serverUrl,
            onPick = { url ->
                vm.setServerUrl(url)
                vm.testServer(url)   // immediately report reachability
                pickingServer = false
            },
            onCustomUrl = {
                pickingServer = false
                editing = "server" to settings.serverUrl
            },
            onDismiss = { pickingServer = false }
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
 * Server picker: the curated public backends, grouped by region, with the
 * current selection ticked. D-pad navigable; BACK or Cancel closes.
 * "Custom URL…" falls through to the free-text entry dialog.
 */
@Composable
private fun ServerPickerDialog(
    current: String,
    onPick: (String) -> Unit,
    onCustomUrl: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedUrl = current.trim().trimEnd('/')
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x8022384E))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(600.dp)
                .background(Surface, RoundedCornerShape(16.dp))
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Speed test server",
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = ChakraPetch
            )
            Text(
                "All verified working. Pick the region closest to you for the most accurate numbers.",
                fontSize = 9.5.sp, color = Ink2, fontFamily = ChakraPetch
            )
            Spacer(Modifier.height(2.dp))

            // Two columns so all presets + the custom row fit a 1080p TV height.
            SpeedServers.ALL
                .map { it to it.url.trimEnd('/').equals(selectedUrl, ignoreCase = true) }
                .chunked(2)
                .forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { (server, isSel) ->
                            ServerOption(
                                server = server,
                                selected = isSel,
                                onClick = { onPick(server.url) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

            // A manually-typed endpoint the catalogue doesn't cover.
            val isCustom = selectedUrl.isNotBlank() && SpeedServers.forUrl(selectedUrl) == null
            ServerOption(
                server = SpeedServer(
                    label = if (isCustom) "Custom: " + SpeedServers.displayName(current) else "Custom URL…",
                    region = "Manual",
                    url = current
                ),
                selected = isCustom,
                onClick = onCustomUrl
            )

            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SecondaryButton("Cancel", onClick = onDismiss, sizeSp = 11f)
            }
        }
    }
}

@Composable
private fun ServerOption(
    server: SpeedServer,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .nimbusFocus(cornerRadius = 10.dp, ringThickness = 1.5.dp, gap = 2.dp)
            .background(if (selected) Tint else Surface, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) CardBorderActive else Line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            server.label,
            modifier = Modifier.weight(1f),
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = Ink, fontFamily = ChakraPetch,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(server.region, fontSize = 8.5.sp, color = Ink2, fontFamily = ChakraPetch)
        if (selected) {
            Spacer(Modifier.width(9.dp))
            CheckIcon(BlueStrong, Modifier.size(12.dp))
        }
    }
}

/** D-pad character rows for the TV text-entry dialog (built once, not per frame). */
private val keyboardRows = listOf(
    "a b c d e f g",
    "h i j k l m n",
    "o p q r s t u",
    "v w x y z 0-9"
)

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
    val doneFocus = remember { FocusRequester() }

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
            keyboardRows.forEach { row ->
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

/**
 * In-app update row (GitHub Releases feed).
 * IDLE → "Check"; AVAILABLE → "Download"; DOWNLOADING → progress;
 * READY → "Install"; UP_TO_DATE / ERROR → status line + retry.
 */
@Composable
private fun UpdateRow(settings: SettingsUiState, vm: NimbusViewModel) {
    val status = settings.updateStatus
    val info = settings.updateInfo
    val sizeText = String.format(
        java.util.Locale.getDefault(), "%.1f MB", (info?.assetSize ?: 0L) / 1_048_576.0
    )
    val highlighted = status == cloud.g3h.nimbus.net.UpdateChecker.Status.AVAILABLE ||
        status == cloud.g3h.nimbus.net.UpdateChecker.Status.READY

    val sub = when (status) {
        cloud.g3h.nimbus.net.UpdateChecker.Status.IDLE -> "Check for new versions (GitHub Releases)"
        cloud.g3h.nimbus.net.UpdateChecker.Status.CHECKING -> "Checking for updates…"
        cloud.g3h.nimbus.net.UpdateChecker.Status.AVAILABLE ->
            "v${info?.tagName?.trimStart('v') ?: "?"} is available · $sizeText"
        cloud.g3h.nimbus.net.UpdateChecker.Status.DOWNLOADING -> "Downloading… ${settings.downloadPct}%"
        cloud.g3h.nimbus.net.UpdateChecker.Status.READY -> "Downloaded · $sizeText — install when ready"
        cloud.g3h.nimbus.net.UpdateChecker.Status.UP_TO_DATE ->
            "You're on the latest version (v${settings.versionName})"
        cloud.g3h.nimbus.net.UpdateChecker.Status.ERROR ->
            settings.updateError ?: "Update check failed"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) Tint else Surface, RoundedCornerShape(14.dp))
            .border(1.dp, if (highlighted) CardBorderActive else Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            SectionLabel("App update")
            Text(sub, fontSize = 10.5.sp, color = Ink, fontFamily = ChakraPetch)
        }
        Spacer(Modifier.width(16.dp))
        when (status) {
            cloud.g3h.nimbus.net.UpdateChecker.Status.IDLE ->
                SecondaryButton("Check", onClick = { vm.checkForUpdates() }, sizeSp = 10f)
            cloud.g3h.nimbus.net.UpdateChecker.Status.CHECKING -> Unit
            cloud.g3h.nimbus.net.UpdateChecker.Status.AVAILABLE ->
                PrimaryButton("Download", onClick = { vm.downloadUpdate() }, sizeSp = 10f)
            cloud.g3h.nimbus.net.UpdateChecker.Status.DOWNLOADING ->
                ProgressPill(settings.downloadPct)
            cloud.g3h.nimbus.net.UpdateChecker.Status.READY ->
                PrimaryButton("Install", onClick = { vm.installUpdate() }, sizeSp = 10f)
            cloud.g3h.nimbus.net.UpdateChecker.Status.UP_TO_DATE -> Unit
            cloud.g3h.nimbus.net.UpdateChecker.Status.ERROR ->
                SecondaryButton("Retry", onClick = { vm.checkForUpdates() }, sizeSp = 10f)
        }
    }
}

@Composable
private fun ProgressPill(pct: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(90.dp)
            .height(8.dp)
            .background(NeutralPill, RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth((pct.coerceIn(0, 100) / 100f))
                .background(Blue, RoundedCornerShape(999.dp))
        )
    }
}
