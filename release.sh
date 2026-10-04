#!/usr/bin/env bash
# ============================================================
#  release.sh — build a signed Nimbus Speed Test release from a
#  git tag via the apk-builder (apk.g3h.cloud).
#
#  Usage (from the repo root):
#      ./release.sh v1.0.3 4 [https://apk.g3h.cloud]
#
#  Args:
#    TAG        annotated tag in this repo (e.g. v1.0.3)
#    CODE       versionCode (integer, must be > previous)
#    BASE       builder base URL (default: public endpoint)
#
#  Prereqs:
#    - APK_BUILDER_SKILL_KEY env var set (apb_sk_...; never committed)
#    - versionName = TAG without the leading v
#    - gradle/wrapper present in src/ (committed)
#
#  Steps:
#    1. verify tag + extract versionName/versionCode
#    2. download the tag archive from GitHub
#    3. zip its src/ (the Gradle root the builder expects)
#    4. POST /api/v1/builds (appTypeId=9, keystoreId=7)
#    5. poll GET /api/v1/builds/:id to a terminal state
#    6. GET /api/v1/builds/:id/download -> handoff/<name>-v<CODE>-signed.apk
#    7. publish a GitHub Release with the APK as an asset (the in-app
#       update endpoint). Needs `gh` auth or GITHUB_TOKEN; pass --no-publish
#       to skip (the app's update check stays on the previous release).
#    8. print SHA-256 + remind: run the section 10.1 gate before shipping
#
#  NOTE: "publish" here means a GitHub Release for in-app updates. It does
#  NOT call the builder's publish route (ATV-Store-only, human-approved,
#  standard section 23).
# ============================================================
set -euo pipefail

TAG="${1:?usage: release.sh <tag> <versionCode> [builder-base-url] [--no-publish]}"
CODE="${2:?usage: release.sh <tag> <versionCode> [builder-base-url] [--no-publish]}"
BASE="${3:-https://apk.g3h.cloud}"
NO_PUBLISH=0
for a in "$@"; do [ "$a" = "--no-publish" ] && NO_PUBLISH=1; done
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

if [ -z "$APK_BUILDER_SKILL_KEY" ]; then
  echo "FATAL: the builder skill key env var must be set (export it; never commit it)" >&2
  exit 1
fi

# --- 1. resolve tag -> commit + versionName ------------------
OWNER_REPO="$(git remote get-url origin | sed -E 's#(https?://|git@)github\.com[:/]##; s#\.git$##')"
TAGREF="refs/tags/$TAG"
# An annotated tag is its own object: `ls-remote <ref>` returns the TAG OBJECT
# sha, and the peeled COMMIT sha only for the `<ref>^{}` refspec. A lightweight
# tag has no peeled row. Prefer the commit so the log line and `gh --target`
# reference a commit, not the tag object.
COMMIT="$(git ls-remote origin "$TAGREF^{}" | awk 'NR==1{print $1}')"
[ -n "$COMMIT" ] || COMMIT="$(git ls-remote origin "$TAGREF" | awk 'NR==1{print $1}')"
[ -n "$COMMIT" ] || { echo "FATAL: tag $TAG not found on origin" >&2; exit 1; }
VERSION_NAME="${TAG#v}"
case "$CODE" in (*[!0-9]*|'') echo "FATAL: versionCode must be an integer" >&2; exit 1;; esac
echo "tag=$TAG commit=${COMMIT:0:12} versionName=$VERSION_NAME versionCode=$CODE"

# --- 2. download the tag archive from GitHub -----------------
# NOTE: a relative work dir — MSYS git-bash curl refuses to write to
# /tmp/... paths (exit 23 or a 0-byte file) but relative paths work.
WORK=".release-work"
rm -rf "$WORK" && mkdir -p "$WORK"
trap 'rm -rf "$WORK"' EXIT
echo "downloading https://api.github.com/repos/$OWNER_REPO/tarball/$TAG ..."
curl -sL -m 120 "https://api.github.com/repos/$OWNER_REPO/tarball/$TAG" -o "$WORK/tag.tar"
tar -xf "$WORK/tag.tar" -C "$WORK"
ROOT="$(find "$WORK" -maxdepth 1 -mindepth 1 -type d ! -name 'tag.tar' | head -1)"
[ -f "$ROOT/src/gradlew" ] || { echo "FATAL: no src/gradlew in tag archive" >&2; exit 1; }

# --- 3. package src/ (the Gradle root the builder expects) ---
SRC_ZIP="$WORK/nimbus-source-${TAG}.zip"
python3 - "$ROOT/src" "$SRC_ZIP" <<'PY'
import os, sys, zipfile
src, out = sys.argv[1], sys.argv[2]
n = 0
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for dp, dns, fns in os.walk(src):
        for f in fns:
            full = os.path.join(dp, f)
            z.write(full, os.path.relpath(full, src))
            n += 1
print(f"zipped {n} files")
PY
echo "archive: $SRC_ZIP ($(du -h "$SRC_ZIP" | cut -f1))"

# --- 4. submit the build -------------------------------------
BID="$(curl -s -m 120 -X POST "$BASE/api/v1/builds" \
  -H "User-Agent: $UA" \
  -H "X-API-Key: $APK_BUILDER_SKILL_KEY" \
  -F "appTypeId=9" \
  -F "appName=Nimbus Speed Test" \
  -F "packageId=cloud.g3h.nimbus" \
  -F "versionName=$VERSION_NAME" \
  -F "versionCode=$CODE" \
  -F "buildFormat=apk" \
  -F "keystoreId=7" \
  -F "configValues={}" \
  -F "projectArchive=@$SRC_ZIP;type=application/zip" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["buildId"])')"
[ -n "$BID" ] || { echo "FATAL: build submit failed (no buildId)" >&2; exit 1; }
echo "submitted build $BID"

# --- 5. poll to terminal state (max 15 min) ------------------
for i in $(seq 1 180); do
  REC="$(curl -s -m 30 "$BASE/api/v1/builds/$BID" -H "User-Agent: $UA" -H "X-API-Key: $APK_BUILDER_SKILL_KEY")"
  ST="$(echo "$REC" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("status",""))')"
  case "$ST" in
    completed) break ;;
    failed) echo "FATAL: build failed: $(echo "$REC" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("statusMessage",""))')" >&2; exit 1 ;;
  esac
  sleep 5
done
[ "$ST" = "completed" ] || { echo "FATAL: build timed out (still $ST)" >&2; exit 1; }
echo "build completed"

# --- 6. download the signed APK ------------------------------
# The /download route is a 302 -> signed R2/Cloudflare URL, so follow
# redirects (-L). Validate it's a real APK (a zip) and non-trivial size;
# never trust the write blindly.
OUT="handoff/nimbus-speedtest-${VERSION_NAME}-v${CODE}-signed.apk"
mkdir -p handoff
curl -sL -m 300 "$BASE/api/v1/builds/$BID/download" \
  -H "User-Agent: $UA" -H "X-API-Key: $APK_BUILDER_SKILL_KEY" -o "$OUT"
SZ="$(stat -c%s "$OUT")"
if [ ! -s "$OUT" ] || [ "$SZ" -lt 102400 ] || ! python3 -c "import sys,zipfile;z=zipfile.ZipFile(sys.argv[1]);sys.exit(0 if 'AndroidManifest.xml' in z.namelist() else 1)" "$OUT" 2>/dev/null; then
  echo "FATAL: downloaded file is not a valid APK ($SZ bytes) — refusing to clobber a good release" >&2
  rm -f "$OUT"
  exit 1
fi
echo "wrote $OUT ($SZ bytes)"

# --- 7. publish GitHub Release (in-app update endpoint) ------
# The app checks GET /releases/latest on this repo and installs the
# -signed.apk asset. The body carries a machine-readable versionCode
# line (the GitHub API has no versionCode field of its own).
if [ "$NO_PUBLISH" = "1" ]; then
  echo "skip: --no-publish (the app will keep offering the previous release)"
else
  if ! command -v gh >/dev/null 2>&1 && [ -z "${GITHUB_TOKEN:-}" ]; then
    echo "FATAL: publishing needs `gh` auth or GITHUB_TOKEN (or pass --no-publish)" >&2
    exit 1
  fi
  TAGMSG="$(git tag -l --format='%(contents)' "$TAG" | head -20)"
  SHA="$(sha256sum "$OUT" | cut -d' ' -f1)"
  BODY="$(cat <<EOF
$TAGMSG

- APK: nimbus-speedtest-${VERSION_NAME}-signed.apk ($SZ bytes)
- SHA-256: $SHA
- Source: https://github.com/$OWNER_REPO/tree/$TAG

nimbus-versionCode=$CODE
EOF
)"
  echo "publishing GitHub Release $TAG ..."
  APK_NAME="nimbus-speedtest-${VERSION_NAME}-v${CODE}-signed.apk"
  if command -v gh >/dev/null 2>&1; then
    # gh CLI path (auth already configured). The API occasionally returns a
    # transient 500 on create — retry a few times, then VERIFY the release
    # actually exists (a claimed upload is not a published release).
    for attempt in 1 2 3; do
      if gh release view "$TAG" >/dev/null 2>&1; then
        # release already exists (re-run): replace it
        gh release delete "$TAG" --yes --cleanup-tag=false
      fi
      if gh release create "$TAG" "$OUT" \
        --title "Nimbus Speed Test $TAG" \
        --notes "$BODY" \
        --target "$COMMIT"; then
        break
      fi
      echo "attempt $attempt failed (transient API error?), retrying in 5s ..."
      sleep 5
    done
  else
    # token-only path (no gh binary): raw GitHub API
    gha() {  # gha <method> <path> [json-body]
      curl -sL -m 60 "https://api.github.com/repos/$OWNER_REPO$2" \
        -X "$1" \
        -H "Authorization: Bearer $GITHUB_TOKEN" \
        -H "Accept: application/vnd.github+json" \
        ${3:+-H "Content-Type: application/json" -d "$3"}
    }
    PAYLOAD="$(python3 - "$TAG" "$BODY" <<'PY'
import json, sys
tag, body = sys.argv[1], sys.argv[2]
print(json.dumps({"name": f"Nimbus Speed Test {tag}", "tag_name": tag,
                  "body": body, "draft": False, "prerelease": False}))
PY
)"
    EXISTING="$(gha GET "/releases/tags/$TAG" | python3 -c 'import sys,json
try: print(json.load(sys.stdin).get("id",""))
except Exception: print("")' 2>/dev/null || true)"
    if [ -n "$EXISTING" ]; then
      gha PATCH "/releases/$EXISTING" "$PAYLOAD" >/dev/null
    else
      RID="$(gha POST /releases "$PAYLOAD" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("id",""))' 2>/dev/null || true)"
      [ -n "$RID" ] || { echo "FATAL: release create failed" >&2; exit 1; }
    fi
    # upload the APK asset (multipart)
    ASSET_URL="https://uploads.github.com/repos/$OWNER_REPO/releases/${RID:-$EXISTING}/assets?name=$APK_NAME"
    curl -sL -m 120 "$ASSET_URL" \
      -X POST \
      -H "Authorization: Bearer $GITHUB_TOKEN" \
      -H "Content-Type: application/octet-stream" \
      --data-binary @"$OUT" >/dev/null
  fi
  # Also attach a version-free alias, which gives a PERMANENT download link that
  # keeps working when a new version ships:
  #   https://github.com/$OWNER_REPO/releases/latest/download/nimbus-speed-test.apk
  # It deliberately does NOT end in "-signed.apk": UpdateChecker picks the FIRST
  # asset with that suffix, so a second match could be selected instead of the build.
  STABLE_NAME="nimbus-speed-test.apk"
  STABLE_PATH=".release-work/$STABLE_NAME"
  if command -v gh >/dev/null 2>&1; then
    mkdir -p .release-work
    cp "$OUT" "$STABLE_PATH"
    if gh release upload "$TAG" "$STABLE_PATH" --clobber >/dev/null 2>&1; then
      echo "stable download link: https://github.com/$OWNER_REPO/releases/latest/download/$STABLE_NAME"
    else
      echo "warn: stable alias not attached (the versioned asset is still published)"
    fi
  fi

  # hard check: the app's feed is /releases/latest — confirm it resolves
  LIVE="$(curl -sL -m 30 -H "User-Agent: $UA" \
    "https://api.github.com/repos/$OWNER_REPO/releases/tags/$TAG" \
    | python3 -c 'import sys,json
try: print(json.load(sys.stdin).get("tag_name",""))
except Exception: print("")' 2>/dev/null || true)"
  if [ "$LIVE" != "$TAG" ]; then
    echo "FATAL: GitHub Release $TAG is not live (transient API error?) — re-run the publish step" >&2
    exit 1
  fi
  echo "release live: https://github.com/$OWNER_REPO/releases/tag/$TAG"
fi

# --- 8. report + remind --------------------------------------
sha256sum "$OUT"
echo
echo "DONE. Before shipping, run the section 10.1 gate on $OUT:"
echo "  aapt dump badging / apksigner verify --verbose --print-certs / placeholder scan"
echo "and append the evidence to handoff/RELEASE_RECORD.md."
