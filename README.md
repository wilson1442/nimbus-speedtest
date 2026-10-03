# Nimbus Speed Test

Speed test for Android TV and phones — **Kotlin + Jetpack Compose**, LibreSpeed-compatible engine with **realtime bandwidth streaming** (live gauge, per-phase averages, 64-bar background that surges with throughput).

| | |
|---|---|
| Package | `cloud.g3h.nimbus` |
| Current release | v1.0.2 / versionCode 3 (see `handoff/RELEASE_RECORD.md`) |
| minSdk / targetSdk | 24 / 35 |
| Leanback / touchscreen | both optional (one APK for TV + phones) |
| Engine | LibreSpeed protocol: `empty.php` ping/upload · `garbage.php` download |
| Default server | `https://nyc.speedtest.clouvider.net/backend` (public LibreSpeed backend; changeable at runtime in Settings) |
| Persistence | Room (test history) + DataStore (settings) |
| UI | Compose 1.7.6, material3, D-pad focus, bundled Chakra Petch + Oxanium fonts |

## Layout

```
src/                      Gradle project root (./gradlew here)
  app/                    Android app module
    src/main/java/cloud/g3h/nimbus/
      engine/             LibreSpeedEngine (realtime emissions), PingMath, SpeedTestEngine
      data/               Room entities/DAO/database
      net/                SettingsStore (DataStore), ConnectionMonitor
      ui/                 Compose screens (Home/Test/Results/History/Settings)
    src/test/             PingMathTest (10), LiveEmissionTest (live server, self-skips offline)
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

Production APKs are built and signed by the g3h **apk-builder** service (`apk.g3h.cloud`), which holds the release keystore — **the keystore never lives in this repo**. The builder consumes a clean source archive (same exclusions as `.gitignore`); per-version SHA-256, cert fingerprint, and §10.1 gate evidence are in `handoff/RELEASE_RECORD.md`.

The public domain is behind Cloudflare: API calls need a browser-like `User-Agent` or they 403 (WAF 1010).

## Tests

- `PingMathTest` — 10 unit tests (median ping, jitter, loss math).
- `LiveEmissionTest` — runs the real engine against the default server and asserts the engine emits live progress *during* download and upload (regression guard for the realtime display). Self-skips via `assumeTrue` when the server is unreachable, so CI stays green offline.
