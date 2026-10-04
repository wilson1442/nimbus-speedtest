# Nimbus Speed Test

Speed test for Android TV and phones — **Kotlin + Jetpack Compose**, LibreSpeed-compatible engine with **realtime bandwidth streaming** (live gauge, per-phase averages, 64-bar background that surges with throughput).

| | |
|---|---|
| Package | `cloud.g3h.nimbus` |
| Current release | v1.4.0 / versionCode 9 (see `handoff/RELEASE_RECORD.md`) |
| minSdk / targetSdk | 24 / 35 |
| Leanback / touchscreen | both optional (one APK for TV + phones) |
| Engine | LibreSpeed protocol: `empty.php` ping/upload · `garbage.php` download |
| Default server | `https://nyc.speedtest.clouvider.net/backend` — plus a picker of 12 verified public backends (US East/Central/West, Europe, Asia) in Settings; any custom URL accepted |
| Persistence | Room (test history) + DataStore (settings) |
| UI | Compose 1.7.6, material3, D-pad focus, bundled Chakra Petch + Oxanium fonts |

## Layout

```
src/                      Gradle project root (./gradlew here)
  app/                    Android app module
    src/main/java/cloud/g3h/nimbus/
      engine/             LibreSpeedEngine (realtime emissions), PingMath, SpeedTestEngine
      data/               Room entities/DAO/database
      net/                SettingsStore (DataStore), ConnectionMonitor, UpdateChecker, SpeedServers
      ui/                 Compose screens (Home/Test/Results/History/Settings) + UpdateBanner
    src/test/             PingMathTest (10), QualityScoreTest (6), ServerProbeTest (2),
                          UpdateCheckerTest (8), SpeedServersTest (6),
                          LiveEmissionTest (live server, self-skips offline)
design/                   HTML mockups the UI was built from
handoff/                  RELEASE_RECORD.md (per-version verification evidence)
HERMES_BUILD_SPEC.md      original build spec
```

## Build

Requires JDK 17 and an Android SDK with platform 35 / build-tools 35.0.0 (`ANDROID_HOME`).

```bash
cd src
./gradlew assembleDebug          # debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest # unit + live-emission tests
```

Build-time overrides: `-PAPI_BASE_URL=<url>` bakes a different default server (the user's in-app Settings always wins at runtime).

## Release

Production APKs are built and signed by the g3h **apk-builder** service (`apk.g3h.cloud`), which holds the release keystore — **the keystore never lives in this repo**. Per-version SHA-256, cert fingerprint, and §10.1 gate evidence are in `handoff/RELEASE_RECORD.md`.

The pipeline is **git-tag driven**:

1. Bump `versionCode` / `versionName` in `src/app/build.gradle.kts`, commit, push.
2. Tag the release: `git tag -a v1.0.3 -m "..." && git push origin v1.0.3`.
3. Run:

   ```bash
   export APK_BUILDER_SKILL_KEY=***
   ./release.sh v1.0.3 4
   ```

   `release.sh` downloads the tag archive from GitHub, packages `src/`, submits the builder build (appTypeId 9, keystore 7), polls to completion, writes `handoff/nimbus-speedtest-1.0.3-v4-signed.apk`, **and publishes a GitHub Release** with the signed APK as an asset (the in-app update endpoint — see below). It does **not** call the builder publish route (ATV-Store-only, human-approved).
4. Run the §10.1 gate on the downloaded APK (badging, apksigner, sha256, placeholder scan) and append evidence to `handoff/RELEASE_RECORD.md`.

The public builder domain is behind Cloudflare: API calls need a browser-like `User-Agent` or they 403 (WAF 1010) — `release.sh` handles this.

## Updates (no Play Store)

Installed apps update themselves from **GitHub Releases** — this repo's release page *is* the distribution endpoint:

- `release.sh` attaches the signed APK to the tag's GitHub Release (asset name `nimbus-speedtest-<version>-v<code>-signed.apk`) and writes a machine-readable `nimbus-versionCode=<code>` line into the release notes.
- **Automatic.** On launch the app checks the public GitHub API (`GET /repos/wilson1442/nimbus-speedtest/releases/latest`), compares `versionCode` against its own, and when a newer build exists it downloads the APK in-app and hands it straight to the system installer — no manual step beyond confirming the installer dialog. Checks are throttled to once per 6 h (`UpdateChecker.CHECK_INTERVAL_MS`) so the unauthenticated 60-requests/hour/IP budget is never approached, and the whole behaviour has an **Settings → Auto-update On/Off** toggle (default On).
- Progress is shown by a global **update banner** (`ui/UpdateBanner.kt`, rendered at the composition root) on every screen: *available → downloading N% → ready · Install now*.
- The APK is verified (zip + `AndroidManifest.xml`) before promotion and handed over via a `FileProvider` URI. Android 8+ requires allowing Nimbus to install unknown apps once (`REQUEST_INSTALL_PACKAGES`); the app detects this and opens the right Settings toggle instead of failing silently.
- **Settings → App update** still offers a manual *Check* / *Download* / *Install*, and **Settings → About** shows the running version and build number. The version also appears on the Home screen under the START button.
- No backend, no server, no Play Store.

To push an update to devices: ship a new tag via the pipeline above. Devices on v1.3.0+ pick it up automatically on next launch; anything older needs one manual Settings → Check.

## Tests

- `PingMathTest` — 10 unit tests (median ping, jitter, loss math).
- `QualityScoreTest` — 6 unit tests (0–100 quality score + grade bands, bounds, missing-ping neutrality).
- `ServerProbeTest` — 2 tests (probe reports a reason when unreachable; latency when reachable).
- `UpdateCheckerTest` — 8 tests (release-JSON parsing + the 6 h auto-check throttle gate).
- `SpeedServersTest` — 6 tests (catalogue is https/no-trailing-slash, unique labels+urls, all regions present, default selectable, `forUrl`/`displayName` behaviour).
- `LiveEmissionTest` — runs the real engine against the default server and asserts the engine emits live progress *during* download and upload (regression guard for the realtime display). Self-skips via `assumeTrue` when the server is unreachable, so CI stays green offline.

Full suite: **33/33**.

## Speed-test servers

Settings → **Speed test server → Choose** lists 12 public LibreSpeed-compatible backends, grouped by region:

| Region | Servers |
|---|---|
| US · East | New York, Atlanta |
| US · Central | Chicago, Denver, Grand Rapids |
| US · West | Los Angeles (Clouvider), Las Vegas |
| Europe | London, Amsterdam, Frankfurt, Prague |
| Asia | Tokyo |

Every entry was probed against the engine's real contract before being listed (`GET empty.php` 200; `GET garbage.php?ckSize=1` 200 with a complete 1,048,576-byte body; `POST empty.php` 200). Picking one selects it *and* probes it immediately; **Custom URL…** still allows any endpoint. There is **no public Canadian LibreSpeed backend** — the official list has none, so the nearest US server is the substitute.
