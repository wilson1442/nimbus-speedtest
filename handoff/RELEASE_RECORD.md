# Nimbus Speed Test — Release Record

| Field | Value |
|---|---|
| App name | Nimbus Speed Test |
| Package ID | cloud.g3h.nimbus |
| versionName | **1.7.0** (current) — 1.6.0, 1.5.0, 1.4.0, 1.3.0, 1.2.1, 1.2.0, 1.1.1, 1.1.0, 1.0.2, 1.0.1, 1.0.0 (superseded) |
| versionCode | **12** (current) — 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 (builder `GET /api/v1/builds` history hangs server-side, noted gap per standard §22.3) |
| Build format | apk (zipaligned, apksigner v2+v3) |
| Keystore | id=7, name "nimbus", alias `nimbus-key` (created 2026-10-03 14:03) |
| Builder buildId | **81b227b8-3a74-45f4-a6d9-c7bb5724fbd4** (current) · 1f17daed-1f1e-4b9c-b623-91ea924f2417 (v1.6.0) · 5ff126ae-16cb-494a-a7f8-e84ccbe5f061 (v1.5.0) · ed316b2f-f698-46dd-abbb-959a8b0261c4 (v1.4.0) · 2472f5e5-bc82-4ec0-9324-693d1721ed3c (v1.3.0) · 1ecfd3b9-dd70-4ff4-aa24-bd2a8db8eff3 (v1.2.1) · 007f6686-bb0f-4be4-a80c-ad7e4a0ac5c9 (v1.2.0) · e3627e29-055f-4903-84ec-cc7fcd9b8c37 (v1.1.1) · 76c8d384-24a0-4dbd-b7a4-b5b96874f4f2 (v1.1.0) · 8bc85e33-328b-4fea-af65-af2b43aeee56 (v1.0.2) · 0e5c4e26-43bd-452b-b724-36b3107a490b (v1.0.1) · da74e82f-24f7-44ee-a05a-56ee49999d47 (v1.0.0) |
| App type | id=9 "Nimbus Speed Test" (created 2026-10-03 via skill-key `POST /api/v1/app-types`, defaultPackage cloud.g3h.nimbus, no template — archive-driven; note: `appName` form field is REQUIRED on `POST /api/v1/builds` or the API returns 400) |
| Source | git repo `wilson1442/nimbus-speedtest` @ tag `v1.7.0` (commit `8ac7384d2b7b`) — previous releases: `v1.6.0` (`d17394bb2b8a`), `v1.5.0` (`f2361608006f`), `v1.4.0` (`bfffbac1b164`), `v1.3.0` (`5f08c336975f`), `v1.2.1` (`883f4488028f`), `v1.2.0` (`dbb548a71f0b`), `v1.1.1` (`9bd8b3168b23`), `v1.1.0` (`b74cd544ce66`), `v1.0.2` (`233b711b9e6e`), v1.0.1/v1.0.0 (archive) |
| Build status | completed |
| Built at | 2026-10-03 (builder local time, via `./release.sh v1.7.0 12`) |
| APK SHA-256 | **95f2964046a020a7b9a8300071988a889eb450807cfa2a6fdbede0a7a53fbca3** (current) · 9ccd393a08c6ae0e6b227701a31c9023d07ff31c995729f575f43fcd2947f10f (v1.6.0) · a2c592800594eb586990e6694012e4d98bae09a48437f55f9cc95690c7f0f779 (v1.5.0) · 1c8b8e7c3adbedb5e60d64b74d7168bbb900b14a722be736319bd288b8f22a2b (v1.4.0) · a3b462c8c4efba67d497811557bdb002393c67073340df5de3a47f91a8c21f59 (v1.3.0) · 7c1508620bdd4199a25e4601ef8bc9e48f9dbce749163443b5aa2681cb4e43fc (v1.2.1) · bda6b95ebb43fc0212fb6ad03d3248b33807b77527a6964d09e8fdc59f1ecc5b (v1.2.0) · 2a3d376b39518fd49b130349f2cefba24496ca3d4ac1a711275fb1c5c5f9db5a (v1.1.1) · 25e88002f45da675f0b83820f25145cd13759572949c83b43aa20b6ffa4862b6 (v1.1.0) · e91c7b73fbf2e61360b47dc98e3743d285f805d1d070c8a3de909ac317996060 (v1.0.2) · bf7523399ee0edf18ffaf408b71f24a2904c5fb8d0359837f9a407085b60508a (v1.0.1) · b575d33c11d53e3415e79a4914dea2e4ee19bf6c872d4d3c8d95fb993c7089d7 (v1.0.0) |
| APK size | 8,360,040 bytes |
| Cert DN | CN=Nimbus Speed Test, O=Nimbus Speed Test, L=Unknown, ST=Unknown, C=US |
| Cert SHA-256 | 1940e036fd139e4562882628ba98d6ab1247998490ba251cf71da41257865f72 (identical across all builds — upgrade-compatible) |
| Cert SHA-1 | 23a36fd99eec8d220af5da56fd55a3fb5eae0cc0 |
| Signing schemes verified | v2 + v3 |
| minSdk / targetSdk | 24 / 35 |
| Label | Nimbus Speed Test |
| Launcher activity | cloud.g3h.nimbus.MainActivity (LAUNCHER + LEANBACK_LAUNCHER) |
| Features | android.software.leanback required=false; android.hardware.touchscreen required=false |
| Permissions | INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, **REQUEST_INSTALL_PACKAGES** (auto-update installer, added v1.3.0) (+ auto DYNAMIC_RECEIVER_NOT_EXPORTED) |
| Default speed-test server | **baked default**: `https://nyc.speedtest.clouvider.net/backend` (NYC Clouvider). Since **v1.4.0** Settings offers a catalogue of 12 verified public backends (US East/Central/West, Europe, Asia) — see `net/SpeedServers.kt`; a user pick (DataStore) always wins over the BuildConfig default, and a custom URL can still be typed. |
| Server catalogue (v1.4.0) | 12 entries, each probed for the full engine contract (ping 200, download 200 + complete 1,048,576-byte body, upload 200): NYC, Atlanta, Chicago, Denver, Grand Rapids, Los Angeles (Clouvider), Las Vegas, London, Amsterdam, Frankfurt, Prague, Tokyo. **Helsinki (librespeed.fi) excluded — 503.** **No public Canadian backend exists** in the official LibreSpeed list. |
| Placeholder scan | 0 unreplaced tokens in DEX |
| Fonts bundled | chakrapetch regular/medium/semibold/bold + oxanium medium/semibold/bold (confirmed in resources.arsc) |
| App code | MainActivity, LibreSpeedEngine, HomeScreen present in classes2.dex |
| Local gate | assembleDebug EXIT=0; unit tests pass (39/39: 10 PingMath + 2 ServerProbe + 1 LiveEmission + 8 UpdateChecker + 6 QualityScore + 8 SpeedServers + 4 InstallResume) |
| Publication | **GitHub Release `v1.7.0`** (https://github.com/wilson1442/nimbus-speedtest/releases/tag/v1.7.0) — signed APK attached as asset `nimbus-speedtest-1.7.0-v12-signed.apk`; published by `release.sh` (no API 500), served bytes verified byte-identical to `handoff/`; icon resources confirmed in the signed APK (`ic_launcher`, `ic_launcher_foreground`, `ic_launcher_background`, `ic_launcher_round`, `tv_banner` all in `resources.arsc`; 432/324/216/162/108 px foreground layers and 192/144/96 px legacy icons present) and `shouldCompleteInstall`/`onAppResumed`/`INSTALL_PACKAGE` in the DEX. Builder's publish route unused, §23. |
| Source repo | `https://github.com/wilson1442/nimbus-speedtest` (public, `main`) — local path `F:\hermes-work\coder\nimbus-speedtest`; release keystore is NOT in the repo (apk-builder holds it) |

## v1.7.0 change (2026-10-03) — Nimbus logo as the app icon; updates install without a force-close

**App icon.** Built from the supplied logo art (the cloud + speedometer mark, the "Nimbus" wordmark, the "SPEED TEST" tagline):
- **Adaptive icon (API 26+)**: `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` with `@color/ic_launcher_background` (#FCFDFC) and `@mipmap/ic_launcher_foreground` at mdpi→xxxhdpi (108/162/216/324/432 px).
- **Legacy icons (API 24/25)**: rounded-square `ic_launcher.png` and circular `ic_launcher_round.png` at 48/72/96/144/192 px.
- **TV banner**: `drawable/tv_banner.png` (320×180) with the full lockup for the leanback launcher.
- The art is drawn on a light plate, so the adaptive **background layer is that exact plate colour** and the foreground carries the same plate — a seamless tile with **no background cut-out**. I first tried alpha-extracting the mark (flood-fill the plate from the border) and it **punched holes through the artwork**: the mark's own white speed-lines touch its outline, so the fill leaked inside and even the dial centre came back transparent. Rejected after looking at the composite over dark/white/blue.

**Automatic update: prompt, then install — no force-close.**
- **The bug:** `installUpdate()` dismissed the prompt, opened the "install unknown apps" toggle and returned. Nothing re-ran the install when the user came back, so the update only landed after force-closing the app (which restarted it and re-ran the launch check — hence "force-close makes it work").
- **The fix:** `installUpdate()` now remembers it owes an install and keeps the prompt up; `MainActivity.onResume()` → `NimbusViewModel.onAppResumed()` completes it the moment the user returns with the grant on. `UpdatePrompt` gained a third state (*"One more step"* · **Open settings** / **Not now**) explaining what Android is asking for.
- The rule is a pure function, `shouldCompleteInstall(awaitingPermission, stillNeedsPermission)`, with **4 unit tests** (`InstallResumeTest`).
- `launchInstaller()` tries `ACTION_VIEW` then `INSTALL_PACKAGE` (boxes differ) before falling back to the release page.

**Also fixed:** an XML comment containing `--` broke `mergeDebugResources` (XML forbids `--` inside comments).

**Tests:** 39/39 (was 35; +4 `InstallResumeTest`).

## v1.6.0 change (2026-10-03) — Home location selector is a dropdown list

Replaces the v1.5.0 chip row with a real dropdown, as requested.

- The collapsed control is a **pill** showing the current location's **flag + name** plus a chevron that flips when open.
- Tapping it opens a **focusable `Popup`** anchored under the trigger (`Alignment.BottomStart`, so it hangs directly below) listing all 12 locations as **flag + city + region** rows, the active one highlighted and ticked (`CheckIcon`). Picking one applies it, probes it and closes; **Custom URL…** jumps to Settings. Focus starts on the current location; BACK dismisses (the popup consumes the key, with `vm.dialogBack` as a fallback).
- **Height took two passes.** The first version's natural height was 887 px and its bottom edge landed at **1135 px — off the bottom of a 1080p screen**, so the last rows were unreachable and its `verticalScroll` would have scrolled focus out of view (a plain `Column` scroll does not track focus). Row padding 6→4 dp, item gaps 2→1 dp and a 412→400 dp cap bring it to **753 px with 40 dp to spare and no scrolling**.
- Verified by rendering the dropdown at 1920×1080 (TV density 2.0) using the real theme colours and the **actual `pathData` from the flag drawables**, then measuring: `menu.bottom = 1000 ≤ 1080`, `needsScroll = false`, left edge aligned to the trigger. Evidence: `handoff/home-location-dropdown.png`.

**Tests:** 35/35 unchanged (the selector is UI-only; the `SpeedServers` catalogue tests still cover the data behind it).

## v1.5.0 change (2026-10-03) — update prompt on every launch + Home location selector with flags

**Auto-update: check every launch, and ASK before downloading (was: 6-hourly, silent download + install).**
- The launch check now runs on **every app open**. The old 6 h throttle is replaced by `UpdateChecker.MIN_CHECK_INTERVAL_MS = 60 s`, whose only purpose is to stop a crash/restart loop from hammering the unauthenticated GitHub API (60 req/hour/IP).
- When a newer `versionCode` exists the app **prompts instead of acting**: a modal `UpdatePrompt` (root composable, so it appears over whatever screen is showing) says *"Update available — Nimbus vX is available (Y MB). Download it now?"* with **Download / Later**; once fetched it becomes *"Update ready to install … Install it now? The app will restart."* with **Install now / Later**. Nothing is downloaded or installed without consent; BACK dismisses the prompt.
- `UpdateBanner` is now only the in-flight download progress pill.
- Settings → Auto-update wording updated to "Checks every time the app opens, then asks before downloading".

**Home screen location selector (was: only reachable inside Settings).**
- A **Location** row at the top of Home: a D-pad-navigable `LazyRow` of chips, each showing the **country flag + city**, with the current selection highlighted. Tapping one selects that server *and* probes it immediately; the row auto-scrolls to the current selection on load.
- Selecting a location surfaces reachability under the START button (`reachable · N ms` / `unreachable — try another location`).
- The Settings picker rows now show the flag too.

**Country flags.** Six hand-authored vector drawables — `flag_us`, `flag_gb` (Union Jack), `flag_nl`, `flag_de`, `flag_cz`, `flag_jp` — plus `ic_globe` for a manually-entered URL. `SpeedServer` now carries `@DrawableRes flag` and a `shortName`. The flag geometry was verified by rendering the same coordinates to a PNG and inspecting it (`handoff/flags-preview.png`).

**Release-APK gotcha recorded:** the builder's release build **shortens resource paths** (`res/drawable/flag_us.xml` → `res/2C.xml`), so grepping a *signed* APK for `res/drawable/<name>` reports the file missing even though it shipped. Verify resources by their **name in `resources.arsc`** (all 7 present here), not by zip path.

**Tests:** 35/35 (was 33; +2 in `SpeedServersTest` for the flag drawable ids and `shortName`, and the throttle test rewritten for the 60 s launch guard).

## v1.4.0 change (2026-10-03) — 12-region public server picker

Previously the only server was the baked New York default plus a free-text URL box. Settings now offers a **catalogue of verified public backends** so a nearby region can be chosen for accurate numbers:

| Region | Servers |
|---|---|
| US · East | New York, Atlanta |
| US · Central | Chicago, Denver, Grand Rapids |
| US · West | Los Angeles (Clouvider), Las Vegas |
| Europe | London, Amsterdam, Frankfurt, Prague |
| Asia | Tokyo |

- New **`net/SpeedServers.kt`** — the catalogue plus `forUrl()` (preset lookup, slash/case-insensitive) and `displayName()` (friendly label, else the host).
- **Every entry was probed before listing** against the engine's real contract: `GET empty.php` → 200, `GET garbage.php?ckSize=1` → 200 with a **complete 1,048,576-byte body**, `POST empty.php` → 200. Candidates that fail are excluded, not listed: **Helsinki (`librespeed.fi`) returned 503** on all three endpoints and was dropped. **No public Canadian LibreSpeed backend exists** in the official list, so none could be added — the nearest US option is the substitute.
- UI: Settings → *Choose* opens a **two-column, D-pad-navigable picker** (sized to fit a 1080p TV height) with the current selection ticked, plus **Custom URL…** which falls through to the existing free-text dialog (so an arbitrary endpoint is still possible). Picking a server also **probes it immediately**, so reachability is visible right away.
- Home and Settings now show the **friendly region label** instead of a raw host.
- `SpeedServersTest` (6 tests) guards the data: https + no trailing slash, unique labels/URLs, all advertised regions present, the NYC default is selectable, `forUrl` slash/case handling, `displayName` host fallback.

**Probe gotcha recorded:** `curl -w '%{size_download}'` reported `0` for every one of these servers even when the full body arrived (chunked transfer + `-o /dev/null`). Sizing must be done by writing to a file and `stat`-ing it — a `0` size reading here is a measurement artefact, not a broken server.

Tests: 33/33 (was 27; +6). Removed the now-unused `HomeScreen.hostOf`.

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

## Verification evidence (current v1.7.0)

- `v12-badging.txt` — aapt dump badging (versionCode 12 / 1.7.0, minSdk 24 / target 35; `application: label='Nimbus Speed Test' icon='res/BW.xml' banner='res/gU.png'`, all `application-icon-*` densities resolving to the adaptive icon)
- `v12-apksigner.txt` — apksigner verify --verbose --print-certs (v2+v3 true, cert 1940e036…f72)
- `v12-sha256.txt` — sha256sum (95f29640…53fbca3)
- Update-feed check: unauthenticated `GET /releases/latest` returns tag v1.7.0, body tail `nimbus-versionCode=12`, asset `nimbus-speedtest-1.7.0-v12-signed.apk` (8,360,040 bytes); served download **byte-identical** to `handoff/`
- Icon probe on the **signed** APK: `ic_launcher`, `ic_launcher_foreground`, `ic_launcher_background`, `ic_launcher_round`, `tv_banner` all present in `resources.arsc`; square PNGs at 432/324/216/162/108 px (adaptive foreground layers) and 192/144/96 px ×2 (legacy square + round) and the 320×180 banner extracted and **visually confirmed** — evidence `shipped-icon-check.png`
- DEX probe: `shouldCompleteInstall`, `onAppResumed`, `installAwaitingPermission`, `One more step`, `Open settings`, `android.intent.action.INSTALL_PACKAGE` all present
- `logo-assets-check.png` — the generated icon set inspected before building
- `release-170.log` — full `./release.sh v1.7.0 12` transcript (builder build 81b227b8; published by the script, no API 500)
- `build-icon2.log` — local `assembleDebug testDebugUnitTest` (39/39 green)
- (superseded) `v11-*.txt` (v1.6.0), `v10-*.txt` (v1.5.0), `v9-*.txt` (v1.4.0), `v8-*.txt` (v1.3.0), `v7-*.txt` (v1.2.1), `v6-*.txt` (v1.2.0), `v5-*.txt` (v1.1.1), `v4-*.txt` (v1.1.0), `v3-*.txt` (v1.0.2), `v2-*.txt` (v1.0.1), `signed-*.txt` (v1.0.0)

## Artifacts

- **`nimbus-speedtest-1.7.0-v12-signed.apk`** — current production-signed release (keystore id=7; Nimbus logo icon + TV banner, update installs on return from the permission toggle, Home location dropdown, update prompt on every launch, country flags). Also attached to GitHub Release `v1.7.0` (verified byte-identical).
- `nimbus-speedtest-1.6.0-v11-signed.apk` — previous release (location dropdown), superseded.
- `nimbus-speedtest-1.0.2-v3-signed.apk` — previous release (realtime bandwidth), superseded.
- `nimbus-speedtest-1.0.1-v2-signed.apk` — superseded.
- `nimbus-speedtest-1.0.0-v1-signed.apk` — superseded.
- `nimbus-speedtest-1.0.0-v1.apk` — local debug build. Do not ship; superseded.
