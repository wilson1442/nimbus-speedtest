# Picking this project up on another machine

Short answer: **`git clone` gives you the whole source tree, the release pipeline, the
full release history and the brand/design assets. It does not contain the ~1 GB build
toolchain, and it cannot contain the two credentials you need to ship a signed release.**
Those are the only gaps, and everything short of signing is reproducible from the repo.

Verified by inspecting what is actually tracked (`git ls-files`) against what exists
locally.

## What a clone already includes

| Path | What it is |
|---|---|
| `src/` | The whole Android project (Kotlin/Compose), **including the Gradle wrapper** — no separate Gradle install needed |
| `release.sh` | The full release pipeline; its header documents every step |
| `HERMES_BUILD_SPEC.md` | The build standard the app is built to |
| `handoff/RELEASE_RECORD.md` | Complete release history: every version, `versionCode`, source commit, builder build id, APK SHA-256, signing-cert fingerprint |
| `handoff/*.png` | Visual evidence (focus treatment, icons, dropdown, flags) |
| `branding/` | Master logo artwork + brand notes |
| `design/` | Original mockups |
| `librespeed-servers.json` | The public speed-test backend list used to build the server catalogue |
| `.github/workflows/ci.yml` | The canonical "what a build machine needs" recipe, and CI builds + tests on every push |

## What is NOT in the repo

### 1. The build toolchain (~1 GB) — gitignored by design

`.tools/` holds JDK 17, Gradle 8.10 and the Android SDK. It is **not needed as such** —
it is reproducible:

| Need | Version | Notes |
|---|---|---|
| JDK | **17** | Build targets Java 17 |
| Android SDK | `platform-tools`, `platforms;android-35`, `build-tools;35.0.0` | minSdk 24 / target 35 |
| Gradle | **not needed** | `src/gradlew` bootstraps Gradle 8.10 from the wrapper (`src/gradle/wrapper/`) |

Then point Gradle at the SDK, either way:

```bash
export ANDROID_HOME=/path/to/android-sdk
# or create src/local.properties containing:
#   sdk.dir=/path/to/android-sdk
```

`src/local.properties` is gitignored on purpose — it is machine-specific.

On CI this is exactly: `actions/setup-java@v4` (temurin 17) +
`android-actions/setup-android@v3` with `packages: 'platform-tools platforms;android-35
build-tools;35.0.0'`. See `.github/workflows/ci.yml`.

### 2. Credentials — cannot be recovered from a clone

| What | Where it lives | Needed for |
|---|---|---|
| `APK_BUILDER_SKILL_KEY` (`apb_sk_…`) | Environment variable only, never committed | Producing a **signed** release via apk-builder |
| GitHub auth (`gh auth login`, or `GITHUB_TOKEN`) | Local credential store / keyring | Publishing a GitHub Release |

Get the builder key from whoever holds the apk-builder (apk.g3h.cloud) account.

### 3. Server-side release identity (not files)

- Builder app type **id = 9** ("Nimbus Speed Test")
- Builder keystore **id = 7** (alias `nimbus-key`)

**The release keystore is deliberately not in the repo** — it exists only in apk-builder.
That matters: an update only installs over an existing install if it is signed with the
same key. The fingerprint to match is
`1940e036fd139e4562882628ba98d6ab1247998490ba251cf71da41257865f72`.

### 4. Built APKs and gate dumps — gitignored

`handoff/*.apk` and `handoff/v*-*.txt` are ignored (`*.apk`, `*.txt`). Every signed APK is
attached to its GitHub Release, so released binaries are recoverable from there. The
`.txt` dumps are regenerable with the commands below; their substantive values
(versionCode, cert fingerprint, APK SHA-256) are also recorded directly in
`RELEASE_RECORD.md`, so nothing important is lost.

## Build and test locally

```bash
cd src
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug                 # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest        # 47 tests
```

The test suite includes a **Compose focus suite under Robolectric** (`FocusSemanticsTest`),
which downloads Robolectric's `android-all` runtime on first run — it needs network the
first time.

A debug APK is not signed with the release key: fine for sideloading and testing, not for
shipping.

## Ship a release

```bash
export APK_BUILDER_SKILL_KEY=apb_sk_...
./release.sh v1.7.5 17
```

That resolves the tag to a commit, submits the build, downloads the signed APK into
`handoff/`, publishes the GitHub Release (including the version-free `nimbus.apk` alias
that powers the permanent download link), and prints the SHA-256.

Then run the §10.1 gate:

```bash
aapt dump badging           handoff/nimbus-1.7.5-v17-signed.apk
apksigner verify --verbose --print-certs handoff/nimbus-1.7.5-v17-signed.apk
sha256sum                   handoff/nimbus-1.7.5-v17-signed.apk
```

and append the evidence to `handoff/RELEASE_RECORD.md`.

**Bump `versionCode` / `versionName` in `src/app/build.gradle.kts` and commit before
tagging** — the builder does not derive them.

Permanent download link handed to users (always the newest release):

```
https://github.com/wilson1442/nimbus-speedtest/releases/latest/download/nimbus.apk
```

## Read this first if you touch focus

All TV focus behaviour is `nimbusFocus` in `src/app/src/main/java/cloud/g3h/nimbus/ui/Focus.kt`,
and its **modifier order is load-bearing**: `onFocusChanged` must be declared *before*
`focusable()`, or it observes nothing and the focus ring silently never draws. That exact
bug shipped twice. `FocusSemanticsTest` pins the rule in both directions — read the
comment in that file before changing it.
