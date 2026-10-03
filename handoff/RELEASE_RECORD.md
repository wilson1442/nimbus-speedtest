# Nimbus Speed Test — Release Record

| Field | Value |
|---|---|
| App name | Nimbus Speed Test |
| Package ID | cloud.g3h.nimbus |
| versionName | **1.1.1** (current) — 1.1.0, 1.0.2, 1.0.1, 1.0.0 (superseded) |
| versionCode | **5** (current) — 4, 3, 2, 1 (builder `GET /api/v1/builds` history hangs server-side, noted gap per standard §22.3) |
| Build format | apk (zipaligned, apksigner v2+v3) |
| Keystore | id=7, name "nimbus", alias `nimbus-key` (created 2026-10-03 14:03) |
| Builder buildId | **e3627e29-055f-4903-84ec-cc7fcd9b8c37** (current) · 76c8d384-24a0-4dbd-b7a4-b5b96874f4f2 (v1.1.0) · 8bc85e33-328b-4fea-af65-af2b43aeee56 (v1.0.2) · 0e5c4e26-43bd-452b-b724-36b3107a490b (v1.0.1) · da74e82f-24f7-44ee-a05a-56ee49999d47 (v1.0.0) |
| App type | id=9 "Nimbus Speed Test" (created 2026-10-03 via skill-key `POST /api/v1/app-types`, defaultPackage cloud.g3h.nimbus, no template — archive-driven; note: `appName` form field is REQUIRED on `POST /api/v1/builds` or the API returns 400) |
| Source | git repo `wilson1442/nimbus-speedtest` @ tag `v1.1.1` (commit `8b7d8a403cd5`) — previous releases: `v1.1.0` (`39f35bec36e1`), `v1.0.2` (`e602470`), v1.0.1/v1.0.0 (archive) |
| Build status | completed |
| Built at | 2026-10-03 (builder local time, via `./release.sh v1.1.1 5`) |
| APK SHA-256 | **2a3d376b39518fd49b130349f2cefba24496ca3d4ac1a711275fb1c5c5f9db5a** (current) · 25e88002f45da675f0b83820f25145cd13759572949c83b43aa20b6ffa4862b6 (v1.1.0) · e91c7b73fbf2e61360b47dc98e3743d285f805d1d070c8a3de909ac317996060 (v1.0.2) · bf7523399ee0edf18ffaf408b71f24a2904c5fb8d0359837f9a407085b60508a (v1.0.1) · b575d33c11d53e3415e79a4914dea2e4ee19bf6c872d4d3c8d95fb993c7089d7 (v1.0.0) |
| APK size | 8,166,744 bytes |
| Cert DN | CN=Nimbus Speed Test, O=Nimbus Speed Test, L=Unknown, ST=Unknown, C=US |
| Cert SHA-256 | 1940e036fd139e4562882628ba98d6ab1247998490ba251cf71da41257865f72 (identical across all builds — upgrade-compatible) |
| Cert SHA-1 | 23a36fd99eec8d220af5da56fd55a3fb5eae0cc0 |
| Signing schemes verified | v2 + v3 |
| minSdk / targetSdk | 24 / 35 |
| Label | Nimbus Speed Test |
| Launcher activity | cloud.g3h.nimbus.MainActivity (LAUNCHER + LEANBACK_LAUNCHER) |
| Features | android.software.leanback required=false; android.hardware.touchscreen required=false |
| Permissions | INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE (+ auto DYNAMIC_RECEIVER_NOT_EXPORTED) |
| Default speed-test server | **baked in 1.0.1**: `https://nyc.speedtest.clouvider.net/backend` (public LibreSpeed backend, verified 2026-10-03: empty.php ping 200 @121ms, garbage.php streams full line rate, empty.php POST 200). User can override in Settings (DataStore wins over BuildConfig). |
| Placeholder scan | 0 unreplaced tokens in DEX |
| Fonts bundled | chakrapetch regular/medium/semibold/bold + oxanium medium/semibold/bold (confirmed in resources.arsc) |
| App code | MainActivity, LibreSpeedEngine, HomeScreen present in classes2.dex |
| Local gate | assembleDebug EXIT=0; unit tests pass (10/10 PingMath + LiveEmissionTest) |
| Publication | **GitHub Release `v1.1.1`** (https://github.com/wilson1442/nimbus-speedtest/releases/tag/v1.1.1) — signed APK attached as asset `nimbus-speedtest-1.1.1-v5-signed.apk`; served bytes verified byte-identical to `handoff/`. (The in-script publish hit a persistent GitHub-API 500 across all 3 retries and correctly FAILED LOUD; the release was created manually and verified. The builder's publish route remains unused, §23.) |
| Source repo | `https://github.com/wilson1442/nimbus-speedtest` (public, `main`) — local path `F:\hermes-work\coder\nimbus-speedtest`; release keystore is NOT in the repo (apk-builder holds it) |

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

## Verification evidence (current v1.1.1)

- `v5-badging.txt` — aapt dump badging (versionCode 5 / 1.1.1, minSdk 24 / target 35, leanback+touchscreen optional)
- `v5-apksigner.txt` — apksigner verify --verbose --print-certs (v2+v3, cert 1940e036…f72)
- `v5-sha256.txt` — sha256sum (2a3d376b…db5a)
- Update-feed check: unauthenticated `GET /releases/latest` returns tag v1.1.1, body tail `nimbus-versionCode=5`, asset `nimbus-speedtest-1.1.1-v5-signed.apk`; served download byte-identical to `handoff/`
- `release-111.log` — full `./release.sh v1.1.1 5` transcript (builder build e3627e29; publish section shows the failed-loud GitHub 500)
- (superseded) `v4-*.txt` (v1.1.0), `v3-*.txt` (v1.0.2), `v2-*.txt` (v1.0.1), `signed-*.txt` (v1.0.0)

## Artifacts

- **`nimbus-speedtest-1.1.1-v5-signed.apk`** — current production-signed release (keystore id=7, BACK navigation fix + in-app updater). Also attached to GitHub Release `v1.1.1` (verified byte-identical).
- `nimbus-speedtest-1.1.0-v4-signed.apk` — previous release (in-app update checker), superseded.
- `nimbus-speedtest-1.0.2-v3-signed.apk` — previous release (realtime bandwidth), superseded.
- `nimbus-speedtest-1.0.1-v2-signed.apk` — superseded.
- `nimbus-speedtest-1.0.0-v1-signed.apk` — superseded.
- `nimbus-speedtest-1.0.0-v1.apk` — local debug build. Do not ship; superseded.
