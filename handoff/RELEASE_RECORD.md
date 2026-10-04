# Nimbus Speed Test — Release Record

| Field | Value |
|---|---|
| App name | Nimbus Speed Test |
| Package ID | cloud.g3h.nimbus |
| versionName | **1.3.0** (current) — 1.2.1, 1.2.0, 1.1.1, 1.1.0, 1.0.2, 1.0.1, 1.0.0 (superseded) |
| versionCode | **8** (current) — 7, 6, 5, 4, 3, 2, 1 (builder `GET /api/v1/builds` history hangs server-side, noted gap per standard §22.3) |
| Build format | apk (zipaligned, apksigner v2+v3) |
| Keystore | id=7, name "nimbus", alias `nimbus-key` (created 2026-10-03 14:03) |
| Builder buildId | **2472f5e5-bc82-4ec0-9324-693d1721ed3c** (current) · 1ecfd3b9-dd70-4ff4-aa24-bd2a8db8eff3 (v1.2.1) · 007f6686-bb0f-4be4-a80c-ad7e4a0ac5c9 (v1.2.0) · e3627e29-055f-4903-84ec-cc7fcd9b8c37 (v1.1.1) · 76c8d384-24a0-4dbd-b7a4-b5b96874f4f2 (v1.1.0) · 8bc85e33-328b-4fea-af65-af2b43aeee56 (v1.0.2) · 0e5c4e26-43bd-452b-b724-36b3107a490b (v1.0.1) · da74e82f-24f7-44ee-a05a-56ee49999d47 (v1.0.0) |
| App type | id=9 "Nimbus Speed Test" (created 2026-10-03 via skill-key `POST /api/v1/app-types`, defaultPackage cloud.g3h.nimbus, no template — archive-driven; note: `appName` form field is REQUIRED on `POST /api/v1/builds` or the API returns 400) |
| Source | git repo `wilson1442/nimbus-speedtest` @ tag `v1.3.0` (commit `5f08c336975f`) — previous releases: `v1.2.1` (`883f4488028f`), `v1.2.0` (`dbb548a71f0b`), `v1.1.1` (`9bd8b3168b23`), `v1.1.0` (`b74cd544ce66`), `v1.0.2` (`233b711b9e6e`), v1.0.1/v1.0.0 (archive). (Commits for v1.0.2–v1.2.0 were corrected from tag-object SHAs in the v1.2.1 round.) |
| Build status | completed |
| Built at | 2026-10-03 (builder local time, via `./release.sh v1.3.0 8`) |
| APK SHA-256 | **a3b462c8c4efba67d497811557bdb002393c67073340df5de3a47f91a8c21f59** (current) · 7c1508620bdd4199a25e4601ef8bc9e48f9dbce749163443b5aa2681cb4e43fc (v1.2.1) · bda6b95ebb43fc0212fb6ad03d3248b33807b77527a6964d09e8fdc59f1ecc5b (v1.2.0) · 2a3d376b39518fd49b130349f2cefba24496ca3d4ac1a711275fb1c5c5f9db5a (v1.1.1) · 25e88002f45da675f0b83820f25145cd13759572949c83b43aa20b6ffa4862b6 (v1.1.0) · e91c7b73fbf2e61360b47dc98e3743d285f805d1d070c8a3de909ac317996060 (v1.0.2) · bf7523399ee0edf18ffaf408b71f24a2904c5fb8d0359837f9a407085b60508a (v1.0.1) · b575d33c11d53e3415e79a4914dea2e4ee19bf6c872d4d3c8d95fb993c7089d7 (v1.0.0) |
| APK size | 8,183,128 bytes |
| Cert DN | CN=Nimbus Speed Test, O=Nimbus Speed Test, L=Unknown, ST=Unknown, C=US |
| Cert SHA-256 | 1940e036fd139e4562882628ba98d6ab1247998490ba251cf71da41257865f72 (identical across all builds — upgrade-compatible) |
| Cert SHA-1 | 23a36fd99eec8d220af5da56fd55a3fb5eae0cc0 |
| Signing schemes verified | v2 + v3 |
| minSdk / targetSdk | 24 / 35 |
| Label | Nimbus Speed Test |
| Launcher activity | cloud.g3h.nimbus.MainActivity (LAUNCHER + LEANBACK_LAUNCHER) |
| Features | android.software.leanback required=false; android.hardware.touchscreen required=false |
| Permissions | INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, **REQUEST_INSTALL_PACKAGES** (auto-update installer, added v1.3.0) (+ auto DYNAMIC_RECEIVER_NOT_EXPORTED) |
| Default speed-test server | **baked in 1.0.1**: `https://nyc.speedtest.clouvider.net/backend` (public LibreSpeed backend, verified 2026-10-03: empty.php ping 200 @121ms, garbage.php streams full line rate, empty.php POST 200). User can override in Settings (DataStore wins over BuildConfig). |
| Placeholder scan | 0 unreplaced tokens in DEX |
| Fonts bundled | chakrapetch regular/medium/semibold/bold + oxanium medium/semibold/bold (confirmed in resources.arsc) |
| App code | MainActivity, LibreSpeedEngine, HomeScreen present in classes2.dex |
| Local gate | assembleDebug EXIT=0; unit tests pass (27/27: 10 PingMath + 2 ServerProbe + 1 LiveEmission + 8 UpdateChecker + 6 QualityScore) |
| Publication | **GitHub Release `v1.3.0`** (https://github.com/wilson1442/nimbus-speedtest/releases/tag/v1.3.0) — signed APK attached as asset `nimbus-speedtest-1.3.0-v8-signed.apk`; published by `release.sh` itself this run (no API 500), served bytes verified byte-identical to `handoff/`; DEX probe confirms UpdateBanner/isCheckDue/canRequestPackageInstalls/autoCheckForUpdates present. Builder's publish route unused, §23. |
| Source repo | `https://github.com/wilson1442/nimbus-speedtest` (public, `main`) — local path `F:\hermes-work\coder\nimbus-speedtest`; release keystore is NOT in the repo (apk-builder holds it) |

## v1.3.0 change (2026-10-03) — automatic updates + visible version number

**Auto-update (previously: manual "Check" only, buried in Settings).** A release now reaches the TV unattended — check → download → hand to the system installer:

- **On launch** the ViewModel runs `autoCheckForUpdates()`: it skips when the user disabled auto-update, and otherwise honours a persisted throttle (`SettingsStore.lastUpdateCheck` + `UpdateChecker.isCheckDue`, 6 h) so the GitHub API budget (60 reads/hour/IP) is respected.
- When the feed's `versionCode` is strictly greater it downloads the APK and, on completion, launches the installer itself (`downloadUpdate(auto = true)` → `viewModelScope.launch { installUpdate() }`). This is as automatic as Android allows for a sideloaded app: the user only confirms in the system installer dialog.
- **`UpdateBanner`** (new, rendered at the composition root in `MainActivity`) shows "update available → downloading N% → ready · Install now" **on every screen**, not just Settings. The Install button is the fallback when the automatic installer launch is blocked (e.g. app backgrounded).
- **New Settings row: Auto-update On/Off** (default On), persisted in DataStore. Turning it back on resets the last-check stamp and triggers an immediate check.
- **Android 8+ install permission:** `installUpdate()` now checks `packageManager.canRequestPackageInstalls()` and, when false, sends the user straight to the *install unknown apps* toggle for this app (`ACTION_MANAGE_UNKNOWN_APP_SOURCES`) instead of silently doing nothing. The manifest declares `REQUEST_INSTALL_PACKAGES`. **On a device where Nimbus was side-loaded, the first automatic update will land on that Settings toggle — allow it once and subsequent updates proceed.**
- `UpdateChecker.isCheckDue(last, now, interval)` extracted as a pure function with 4 new unit tests (never-checked is due; inside the interval is not; past it is; default interval is 6 h).

**Version number now visible:**
- Home subtitle: `… · Server: <host> · v<versionName>`.
- Settings → About: `v<versionName> (build <versionCode>)`.
- `SettingsUiState.versionName` now defaults to `BuildConfig.VERSION_NAME`, so it is correct on the first frame (no brief "1.0.0").

**Caveat:** automatic update only begins with *this* build. An installed v1.2.1-or-earlier still needs one manual Settings → Check (or a re-sideload) to get onto v1.3.0; from v1.3.0 onward updates are unattended.

**Tests:** 27/27 (was 23; +4 throttle). Release published by `release.sh` itself this run (no GitHub 500); the tag→commit fix from v1.2.1 is confirmed in the real log line (`commit=5f08c336975f` = the peeled commit).

## v1.2.1 change (2026-10-03) — codebase review: performance/correctness fixes + dead-code removal

No user-visible feature change. Findings from a whole-codebase review, each verified by grep/test rather than inspection:

**Correctness / performance**
- **Network I/O was running on the main thread.** A `callbackFlow`'s producer coroutine inherits the *collector's* context, so `LibreSpeedEngine.run()` (collected from `viewModelScope`, i.e. `Dispatchers.Main.immediate`) executed the ping phase's blocking OkHttp `execute()` on the UI thread — ~10 sequential calls (with TLS on the first) per test, plus `probeServer()` from Settings. `timeGet()` now runs in `withContext(Dispatchers.IO)` and the flow is `.flowOn(Dispatchers.IO)`.
- **Removed shared mutable state on the engine singleton.** `private var lastPingError` was written/read across `run()` and `probeServer()`, so a concurrent test + probe could cross-talk. `timeGet()` now returns the failure reason (`PingAttempt(ms, error)`).
- **`ConnectionMonitor.fetchWanIp` leaked a thread per call** (invoked on every network change *and* every test start), never cancelled, with a `runBlocking` inside. Now one `CoroutineScope(SupervisorJob() + Dispatchers.IO)` + a cancellable `wanJob`; `stop()` cancels it.
- **`FocusRequester()` was not `remember`ed** on Home/Results/Settings — a new instance per recomposition while `LaunchedEffect(Unit)` drove only the first, so focus could be lost. All three now `remember { FocusRequester() }`.
- **`SettingsScreen` wrote `vm.dialogBack` during composition** (side effect invisible to Compose); moved into `DisposableEffect(editing)`.

**Allocation / recomposition**
- Hoisted per-recomposition allocations to file-level `val`s: the top-bar nav pills and the TV keyboard rows. Wrapped the history chart's scale (`max(points.max(), avg)`) in `remember(points, avg) { … }` so it is no longer recomputed per animation frame.
- Removed the dead y-label loop inside `HistoryScreen.LineChart`.

**Dead code (~124 lines removed)**
- `data/Sample` (unused), `TestResultDao.recent()` / `sinceNoVpn()` (unused), `drawRowDivider` (no-op), and 8 unreferenced composables: `StatCard`, `SurfaceBox`, `DashedLine`, `BigNumber`, `ClockIcon`, `PlayIcon`, `EthernetIcon`, `ConnectionIcon` — plus the imports they were the sole users of. Some were only reachable from each other (`ConnectionIcon` → `EthernetIcon`, `StatCard` → `SurfaceBox`).

**Release-tooling fix**
- `release.sh` resolved the tag→commit via `git ls-remote origin <ref>`, which for an **annotated** tag returns the *tag object* SHA, not the commit — so the logged `commit=`, the `gh --target`, and every prior release record carried tag-object SHAs. Now queries `<ref>^{}` first (falls back to the plain ref for lightweight tags). The commits for v1.0.2–v1.2.0 in this record were corrected accordingly.

**Tests / net result:** 23/23 pass, build green; `LiveEmissionTest` verifies the refactored engine against the live server (`sawPing=true finalPing=8.4 ms`, 31 download + 27 upload live emissions). Net **−124 lines**. Shipped signed APK: `nimbus-speedtest-1.2.1-v7-signed.apk`, SHA-256 `7c150862…4e43fc`.

**Not applied (deliberate):** R8/`isMinifyEnabled` is still `false`. Enabling it would roughly halve the 8.18 MB APK, but shrinking+obfuscation cannot be runtime-verified here (no emulator/device) and a stripped Room/OkHttp class would only surface as a crash on the TV — a bad trade for a live, self-updating app. Opt-in path: set `isMinifyEnabled = true` + `isShrinkResources = true` on the release build type with a `proguard-rules.pro`, then smoke-test install + one full test run on a device before publishing.

## v1.2.0 change (2026-10-03) — latency becomes a first-class, always-visible result

**Why:** the speed test *was* measuring ping (median of 10 × `empty.php` RTTs), but the user saw "no ping results." Two separate bugs + one missing-metric gap:

- **Ping card was hidden after the ping phase.** On the Test screen `if (test.phase == PING)MetricCard("Ping",…)` removed the Ping metric the instant the phase moved to DOWNLOAD/UPLOAD, so by the time the user looked, latency was gone from the live view.
- **Unmeasured ping rendered as a bare `0`**, not an em-dash, on the Home "Last test" and PING card — read as "zero ms" (fastest possible) instead of "unknown."
- **No min/max context**, and no single "how good is this" number.

**Fix (v1.2.0):**
- Test screen: **Ping/Jitter/Packet-loss is now an always-present cluster** (was Ping-conditional). Ping is an accent `LatencyCard` that flags **not measured** in the warn colour when the box can't reach `empty.php`, instead of a dim em-dash. Label flips `ms · live` → `ms · final`.
- Results screen: new **Quality hero** — median ping (1 dp) **always shown** with **min/max/jitter/loss** inline, plus a **0-100 quality score + grade** (Excellent/Great/Good/Fair/Poor, colour-coded). PING card line now reports min **and** max.
- Home: "Last test" ping shows an em-dash (not 0) when unmeasured.
- `QualityScore` (engine/): pure, unit-tested 0-100 + grade from the numbers a run already produces — **no new DB column**. Download 40 / upload 25 / ping 15 / jitter 10 / loss 10, each log/linear-eased to 0..1. **Unmeasured ping (0.0) is neutral (0.60), not a hard zero**, so a box that can speed-test but not ping a specific endpoint isn't punished to the floor.
- (Pre-existing uncommitted work also shipped this round, in service of the same concern): a **Settings → "Test connection"** probe (`probeServer`) that reports latency or a human-readable failure reason, `min_ping_ms`/`peak_mbps` persisted (Room v1→v2 migration), and `ServerProbeTest`.

**Note on the real "no ping":** `empty.php` was probed live and returns **200 in ~40–50 ms** — the server and engine are healthy. So a *blank* ping on the user's TV almost certainly means that specific box/network failed to reach the endpoint (TLS/handshake/CDN/DNS), in which case v1.2.0 now says **"not measured"** and points them to Settings → Test connection, instead of silently showing 0.

- **Tests:** 23/23 (10 PingMath · 2 ServerProbe · 1 LiveEmission · 4 UpdateChecker · **6 QualityScore (new)**). Build ~25 s green.
- **Shipped APK classes verified:** `QualityScore`, `QualityHero`, `LatencyCard`, `Excellent`, `median`, `not measured` all present in the signed `v6` DEX (not a cache).
- versionCode 5 → 6, versionName 1.1.1 → 1.2.0.

## v1.1.1 change (2026-10-03) — BACK no longer exits the app from Home

- **Behavior fix:** BACK now walks the in-app hierarchy instead of finishing the activity (`androidx.activity.compose.BackHandler` at the composition root → `NimbusViewModel.onBack()`):
  - Results / History / Settings → Home
  - running test → cancel, back to Home
  - History "Clear" confirm or Settings edit dialog open → dismiss the dialog (via `vm.dialogBack`)
  - **Home → no-op: the app stays open.** The launcher's HOME key is the way out; BACK never drops the user to the home screen.
- Previously BACK always fell through to the default `OnBackPressed` behavior → `MainActivity` finished → launcher, from *any* screen.
- versionCode 4 → 5, versionName 1.1.0 → 1.1.1.

## v1.1.0 change (2026-10-03) — in-app update checker + GitHub Releases distribution

- **Update distribution (the "site"):** the app no longer needs a separate site or Play Store. GitHub Releases for this repo are the distribution endpoint — `release.sh` now publishes each signed APK as a release asset (`nimbus-speedtest-<version>-v<code>-signed.apk`) with a machine-readable `nimbus-versionCode=<code>` line in the notes (GitHub's API carries no versionCode field). `release.sh` verifies the release is actually live via the public API before reporting success (the GitHub API returned a transient 500 during the v1.1.0 publish; the retry logic handles it).
- **In-app updater:** new `UpdateChecker` (net/) fetches `GET /repos/wilson1442/nimbus-speedtest/releases/latest` (public, no auth — 60 reads/hour/IP), compares `versionCode`, streams the APK into the app cache with live progress, verifies it (zip + `AndroidManifest.xml`) before promoting a `.part` file to final name, and hands it to the system installer through a `FileProvider` URI (`NimbusFileProvider` + `res/xml/file_paths.xml`). Browser fallback to the release page if the installer intent fails on a box.
- **UI:** Settings gains an **App update** row — Check → Download (with progress bar) → Install; highlights when an update is available.
- **Tests:** `UpdateCheckerTest` (4 parser tests: asset discovery, versionCode extraction, missing-code→0, malformed→null). Needed `org.json:json:20240303` on the JVM test classpath (Android's bundled org.json is a stub that throws under plain JUnit). Full suite: 15/15 (10 PingMath + 1 LiveEmission + 4 UpdateChecker).
- versionCode 3 → 4, versionName 1.0.2 → 1.1.0.

## v1.0.2 change (2026-10-03) — realtime bandwidth display

- **Bug fix (root cause):** `LibreSpeedEngine.run()` only emitted the *final* progress for the download and upload phases — the per-sample `onSample` callbacks built progress objects but never forwarded them to the flow, so the gauge, live number, and background bars stayed frozen at 0 Mbps for the entire transfer. Both phases now `producer.trySend(p)` on every 200 ms sample (ping already did).
- **UI:** `TestScreen` smooths `liveMbps` between samples via `animateFloatAsState` (280 ms FastOutSlowIn) so the gauge and 64-bar background sweep fluidly; the DOWNLOAD/UPLOAD phase steppers now show the live speed while measuring instead of "Measuring…", and the final values show the per-phase averages (`downloadMbps`/`uploadMbps` added to `TestUiState`).
- **Regression guard:** new `LiveEmissionTest` runs the real engine against the live NYC server and asserts ≥3 live emissions during download and ≥2 during upload (plus liveMbps > 0 observed). Verified: `PHASES=[DOWNLOAD, PING, UPLOAD] downEmits=31 upEmits=27 sawDownLive=true sawUpLive=true`. Test self-skips (assumeTrue) when the server is unreachable so the builder host stays green offline.
- versionCode 2 → 3, versionName 1.0.1 → 1.0.2.

## v1.0.1 change (2026-10-03)

- Baked in a public LibreSpeed-compatible default speed-test server (NYC, Clouvider) so the app runs out-of-the-box; the in-app Settings server field still overrides it at runtime. Source: official LibreSpeed backend-server list (`https://librespeed.org/backend-servers/servers.php`, 22 servers probed; NYC chosen — lowest US latency 121 ms, all three engine endpoints verified).
- versionCode 1 → 2, versionName 1.0.0 → 1.0.1.

## Verification evidence (current v1.3.0)

- `v8-badging.txt` — aapt dump badging (versionCode 8 / 1.3.0, minSdk 24 / target 35, `uses-permission REQUEST_INSTALL_PACKAGES` present, leanback+touchscreen not-required)
- `v8-apksigner.txt` — apksigner verify --verbose --print-certs (v2+v3 true, cert 1940e036…f72)
- `v8-sha256.txt` — sha256sum (a3b462c8…21f59)
- Update-feed check: unauthenticated `GET /releases/latest` returns tag v1.3.0, body tail `nimbus-versionCode=8`, asset `nimbus-speedtest-1.3.0-v8-signed.apk` (8,183,128 bytes); served download **byte-identical** to `handoff/`
- DEX class probe: `UpdateBanner`, `isCheckDue`, `canRequestPackageInstalls`, `autoCheckForUpdates`, `auto_update` and `android.settings.MANAGE_UNKNOWN_APP_SOURCES` all present in the signed v8 APK
- `release-130.log` — full `./release.sh v1.3.0 8` transcript (builder build 2472f5e5; **published by the script, no API 500**; logs the peeled commit `5f08c336975f`)
- `build-autoupdate2.log` — local `assembleDebug testDebugUnitTest` (27/27 green)
- (superseded) `v7-*.txt` (v1.2.1), `v6-*.txt` (v1.2.0), `v5-*.txt` (v1.1.1), `v4-*.txt` (v1.1.0), `v3-*.txt` (v1.0.2), `v2-*.txt` (v1.0.1), `signed-*.txt` (v1.0.0)

## Artifacts

- **`nimbus-speedtest-1.3.0-v8-signed.apk`** — current production-signed release (keystore id=7; automatic updates + visible version; carries all prior work: latency/quality-score, codebase-review fixes). Also attached to GitHub Release `v1.3.0` (verified byte-identical).
- `nimbus-speedtest-1.2.1-v7-signed.apk` — previous release (codebase-review build), superseded.
- `nimbus-speedtest-1.0.2-v3-signed.apk` — previous release (realtime bandwidth), superseded.
- `nimbus-speedtest-1.0.1-v2-signed.apk` — superseded.
- `nimbus-speedtest-1.0.0-v1-signed.apk` — superseded.
- `nimbus-speedtest-1.0.0-v1.apk` — local debug build. Do not ship; superseded.
