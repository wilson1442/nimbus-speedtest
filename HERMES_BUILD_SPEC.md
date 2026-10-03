# Nimbus Speed Test — Android TV build spec

Build target for a Hermes worker. The visual reference is in `design/`: four HTML mockups, each a 1920×1080 TV screen. Treat them as **markup and style reference only**. They use a design-tool runtime (`support.js`, `<x-dc>`, `<sc-if>`) that is not part of the app. Read the inline styles for exact colors, sizes and layout, but don't port the HTML.

---

## 1. Platform & stack

- **Target:** Android TV / Google TV, landscape 1080p. Must be fully usable with a D-pad remote; no touch.
- **Language:** Kotlin
- **UI:** Jetpack Compose with `androidx.tv:tv-material` (Compose for TV). Don't use Leanback fragments.
- **Min SDK:** 24 · **Target SDK:** latest stable
- **Persistence:** Room (SQLite)
- **Networking:** OkHttp
- **Architecture:** single activity, MVVM (ViewModel + StateFlow), Hilt or manual DI. Either is fine; keep it simple.
- **Manifest:** `android.software.leanback` uses-feature with `required="false"`, `android.hardware.touchscreen` with `required="false"`, a `LEANBACK_LAUNCHER` intent filter, and a 320×180 TV banner.
- **Permissions:** `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`
- **Deliverable:** a signed release APK (`./gradlew assembleRelease`) plus the source repo.

> **Build host requirements:** JDK 17, Android SDK command-line tools (platform + build-tools), Gradle wrapper. The worker can't build an APK without these.

### Unit conversion (important)
The mockups are drawn at **1920×1080 px**. A 1080p Android TV reports **960×540 dp**, so **divide every px value in the mockups by 2** for dp and sp. Example: 54 px font → 27 sp; 96 px side padding → 48 dp. Keep about 5% overscan safe margins (48 dp horizontal, 27 dp vertical).

---

## 2. Design tokens

| Token | Hex | Use |
|---|---|---|
| ground | `#F4F6F3` | app background (off-white) |
| surface | `#FBFCFA` | cards, pills |
| line | `#D9E3EC` | card borders, dividers |
| tint | `#E1EDF8` | active nav pill, highlighted card, VPN-on chip |
| blue | `#5B9BD5` | logo tile, gauge progress, chart, accents |
| blueStrong | `#3D7DBF` | primary buttons, START button, active range pill |
| blueSoft | `#9CC2E6` | focus ring, idle rings |
| wave | `#C9DCEE` | background wave lines |
| ink | `#22384E` | primary text (do **not** use black) |
| ink2 | `#586C80` | secondary text, labels |
| warnBg / warnText | `#FBEFE5` / `#9A4F1C` | "No VPN" chip, Clear history |
| avgLine | `#E08A4C` | dashed average line on chart |

**Never use purple or black.**

**Fonts** (Google Fonts, OFL; bundle them in `res/font`):
- **Chakra Petch** 400/500/600/700: all UI text and labels
- **Oxanium** 500/600/700: every number (speeds, ping, IP, timers)

**Shapes:** pills fully rounded; cards 24–32 px radius (12–16 dp).
**Labels:** uppercase, letter-spacing about 0.18em, ink2.

**Focus (critical for TV):** every focusable element shows a visible focus state. Use a 2-ring halo (a gap ring in the ground color, then a blueSoft ring) plus a slight scale of 1.03–1.05. The START button in the mockup shows the focused state.

---

## 3. Screens

Top bar (every screen): logo tile and the "Nimbus / SPEED TEST" wordmark on the left. In the centre are nav pills **Test · History · Settings**; the active one is filled with tint. On the right is the **connection status bar** (see §5).

### 3.1 Home (`design/Main.dc.html`)
- A large circular **START** button in the exact centre: 360 px circle, blueStrong fill, pulse icon, "START", "PRESS OK". It has default focus on launch.
- Behind it: 3 concentric rings expanding and fading on a loop (3.6 s, staggered), a slowly rotating dotted orbit ring, and a faint heartbeat (ECG) line drawing across the screen.
- Under the button: "Measures ping, download and upload in about 30 seconds · Server: <name>".
- When a VPN is active, also show: "VPN detected — results will reflect your VPN route, not your raw ISP speed".
- Footer: a **Last test** card (time, download, upload, ping) and a **30-day average** card. The average card is focusable and opens History.
- With no history yet, hide the last-test card and show "No tests yet". Show the average as "—".

### 3.2 Test running (`design/Testing.dc.html`)
- Centre gauge: a 270° arc from 0 to 1000 Mbps on a log or eased scale. It has a track (tint) and a progress arc (blue), a small comet segment spinning around the outside, and a slow-rotating dotted outer ring.
- Inside the gauge: phase label (DOWNLOAD / UPLOAD / PING), a big live number in Oxanium, and the unit.
- Left column: live **Peak**, **Jitter** and **Packet loss** cards.
- Right column: a **phase stepper** for Ping → Download → Upload. Done phases show a check and their value. The active phase is highlighted with a pulsing icon. Pending phases have a dashed border and read "Waiting".
- Background (key visual): a scrolling ECG/heartbeat trace across the middle (continuous loop) and about 64 animated bandwidth bars along the bottom. **Drive the bar heights and ECG amplitude from live throughput** if feasible; otherwise animate them.
- Top bar centre: a blinking dot and "TEST IN PROGRESS". Top bar right: VPN state, WAN IP and an elapsed timer.
- **BACK** cancels the test (confirm not needed) and returns Home; nothing is saved.
- On completion, save the result and go automatically to Results.
- Ignore the "Skip to results (prototype)" button in the mockup; it's not part of the app.

### 3.3 Results (`design/Results.dc.html`)
- "TEST COMPLETE" heading with a status line ("Your connection is running strong" or a variant based on thresholds).
- Right side: a VPN chip ("Tested over VPN · WAN IP x.x.x.x", or "No VPN") and a meta line (time · connection · server).
- Three cards: **Download**, **Upload**, **Ping** (ping also shows jitter and loss). Each has a "vs avg" delta pill and a sparkline of throughput samples from this run (ping card: latency samples).
- "Good for" chips decided by thresholds:
  - 4K HDR streaming ≥ 25 Mbps down
  - Online gaming: ping ≤ 40 ms and jitter ≤ 10 ms
  - Video calls ≥ 5 Mbps up
  - Multiple devices ≥ 100 Mbps down
- Buttons: **Test again** (default focus), **View history**, **Done**.
- A "Saved to history" confirmation chip appears in the top bar.

### 3.4 History & averages (`design/History.dc.html`)
- Range selector at top right: **7D · 30D · 90D · All** (default 30D).
- Left column:
  - Big **Avg download** card with the min–max range
  - **Avg upload**, **Avg ping**, **Tests saved**, and **Consistency** (% of tests within ±15% of the period's mean download)
  - Footer: "History is saved on this TV" and a **Clear history** button, which needs a confirm dialog
- Right top: a **chart** of download speed per test across the range, with a dashed line at the period average and a y-axis.
- Right bottom: a **Recent tests** list with columns Date · Download · Upload · Ping · Connection · VPN (On/Off pill) · WAN IP. It scrolls with the D-pad, newest first, and the latest row is highlighted.
- Optional: an "Exclude VPN tests" toggle that recalculates the averages.

### 3.5 Settings (not designed; keep it simple and use the same styles)
- Speed test server URL
- IP lookup URL
- Test duration (short/normal)
- Clear history
- About/version

---

## 4. Data model (Room)

```kotlin
@Entity(tableName = "test_results")
data class TestResult(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val timestamp: Long,          // epoch ms
  val downloadMbps: Double,
  val uploadMbps: Double,
  val pingMs: Double,
  val jitterMs: Double,
  val packetLossPct: Double,
  val connectionType: String,   // "WIFI_5GHZ" | "WIFI_2_4GHZ" | "ETHERNET" | "OTHER"
  val vpnActive: Boolean,
  val wanIp: String?,
  val serverName: String
)
```

DAO needs:
- insert
- latest
- list by range, newest first
- averages by range: AVG for download, upload and ping; COUNT; MIN and MAX of download
- an optional `vpnActive` filter
- deleteAll

Work out consistency in the ViewModel.

---

## 5. Connection status (auto-detected)

Shown on Home, Test, Results, and stored with every result.

- **VPN detection:** `connectivityManager.getNetworkCapabilities(activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)`. Watch with `registerDefaultNetworkCallback` so the chip updates live.
  - VPN on: tint chip, shield-check icon, "VPN connected"
  - VPN off: warn chip, shield-slash icon, "No VPN"
- **Connection type:** `TRANSPORT_ETHERNET` or `TRANSPORT_WIFI`. For Wi-Fi, read the band from `WifiManager.connectionInfo.frequency` (above 4900 MHz → "5 GHz", otherwise "2.4 GHz"). If the band can't be read, show "Wi-Fi".
- **WAN IP:** HTTPS GET to a configurable plain-text endpoint (default `https://api.ipify.org`). Fetch on app start, on any network change and before each test.
  - Show "Detecting…" while loading and "Unavailable" on failure.
  - With a VPN on, this shows the VPN exit IP. That's expected.

Status bar layout: `[VPN chip] | WAN IP <ip in Oxanium> | <wifi/ethernet icon> <type>`

---

## 6. Speed test engine

- Server is configurable and defaults to a **LibreSpeed-compatible** endpoint (self-hostable):
  - `GET /garbage.php?ckSize=100` for download
  - `POST /empty.php` for upload
  - `GET /empty.php` for ping
- Behind a `SpeedTestEngine` interface, so another backend (Cloudflare, iperf) can be swapped in later.
- **Ping:** 10 sequential requests. Ping = median, jitter = mean absolute difference between consecutive samples, loss = failed / total.
- **Download:** 4–6 parallel streams for about 12 s; discard the first 2 s of ramp-up.
- **Upload:** 3–4 parallel streams posting random data for about 10 s, same ramp-up rule.
- Emit live samples every 200 ms as a `Flow<TestProgress>(phase, currentMbps, peakMbps, samples)`. The UI reads this for the gauge, the background graphics and the sparklines.
- Support cancellation by cancelling the coroutine scope.

---

## 7. Animations (use Compose `rememberInfiniteTransition`)

| Element | Spec |
|---|---|
| Home rings | 3 rings, scale 0.9→1.6, alpha 0.8→0, 3.6 s ease-out, 1.2 s stagger |
| Orbit ring | dotted circle rotating 360°/24 s with one dot |
| Home ECG | faint (35% alpha) path drawing across the screen, 7 s loop |
| Test ECG | heartbeat path tiled horizontally, translateX loop 3.2 s, 55% alpha, with a vertical scan line sweeping across every 4 s |
| Bandwidth bars | ~64 bars, scaleY 0.18↔1.0, varied 0.85–2.1 s durations, or tied to live throughput |
| Gauge comet | short arc spinning outside the gauge, 2.4 s/rev |
| Active phase | pulse halo, 1.2 s |
| Chart | line draws in left-to-right on entry, 2.4 s |

Heartbeat path unit (repeat every 320 px; y relative to the baseline):
`l60,0 l14,-14 l14,14 l20,0 l10,30 l16,-150 l16,190 l12,-70 l20,0 l18,-16 l18,16 l102,0`

---

## 8. Acceptance criteria

1. Installs and launches from the Android TV home row with a banner.
2. Everything can be done with the D-pad: focus is always visible, START has focus on launch, and BACK behaves as described.
3. A test runs ping → download → upload, with live gauge and background graphics, finishing in under 40 s.
4. Every completed test is saved and survives app restarts.
5. The History averages are correct for 7D/30D/90D/All and match a manual calculation.
6. VPN state updates within about 2 s of a VPN connecting or disconnecting, and the WAN IP refreshes on network change.
7. Colors, fonts and layout match the mockups, using px ÷ 2 for dp.
8. No purple, no black, no crashes when offline (show an "No internet connection" state on Home).

---

## 9. Suggested task breakdown (for the decomposer)

1. Scaffold the Gradle project, TV manifest, banner, fonts and theme tokens
2. Shared components: top bar, nav pills, status bar, stat card, pill chip, focus modifier
3. Network status service: VPN, connection type, WAN IP
4. Room database: entity, DAO, repository
5. Speed test engine (LibreSpeed client) with progress Flow and unit tests for the ping/jitter math
6. Home screen and its animations
7. Test running screen: gauge, stepper and background graphics
8. Results screen: deltas, sparklines, "Good for" chips
9. History screen: range selector, averages, chart, list, clear
10. Settings screen
11. Release signing and `assembleRelease`; deliver the APK
