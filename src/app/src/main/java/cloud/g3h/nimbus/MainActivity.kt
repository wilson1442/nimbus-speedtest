package cloud.g3h.nimbus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cloud.g3h.nimbus.ui.Ground
import cloud.g3h.nimbus.ui.HistoryScreen
import cloud.g3h.nimbus.ui.HomeScreen
import cloud.g3h.nimbus.ui.NimbusViewModel
import cloud.g3h.nimbus.ui.ResultsScreen
import cloud.g3h.nimbus.ui.Screen
import cloud.g3h.nimbus.ui.SettingsScreen
import cloud.g3h.nimbus.ui.TestScreen
import cloud.g3h.nimbus.ui.UpdateBanner
import cloud.g3h.nimbus.ui.UpdatePrompt
import cloud.g3h.nimbus.ui.NimbusTypography
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as NimbusApp
        setContent {
            MaterialTheme(typography = NimbusTypography) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel {
                    NimbusViewModel(app)
                }
                val screen by vm.screen.collectAsState()
                // TV-style BACK: walk the in-app hierarchy (sub-screen -> Home,
                // running test -> cancel, dialog -> dismiss). On the Home
                // screen BACK is a no-op — the app stays open; the launcher's
                // HOME key is the way out.
                BackHandler { vm.onBack() }
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(Ground)) {
                    when (screen) {
                        Screen.HOME -> HomeScreen(vm = vm, modifier = Modifier.fillMaxSize())
                        Screen.TESTING -> TestScreen(vm = vm, modifier = Modifier.fillMaxSize())
                        Screen.RESULTS -> ResultsScreen(vm = vm, modifier = Modifier.fillMaxSize())
                        Screen.HISTORY -> HistoryScreen(vm = vm, modifier = Modifier.fillMaxSize())
                        Screen.SETTINGS -> SettingsScreen(vm = vm, modifier = Modifier.fillMaxSize())
                    }
                    // Automatic-update progress/done is surfaced on every screen.
                    UpdateBanner(
                        vm = vm,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.TopCenter)
                            .padding(top = 80.dp)
                    )
                    // Modal consent prompt for an available/downloaded update.
                    UpdatePrompt(vm = vm)
                }
            }
        }
    }
}
