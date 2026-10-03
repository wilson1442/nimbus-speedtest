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
#    7. print SHA-256 + remind: run the section 10.1 gate before shipping
#
#  NOTE: this script submits and downloads only. It does NOT publish
#  (publish is ATV-Store-only and human-approved, standard section 23).
# ============================================================
set -euo pipefail

TAG="${1:?usage: release.sh <tag> <versionCode> [builder-base-url]}"
CODE="${2:?usage: release.sh <tag> <versionCode> [builder-base-url]}"
BASE="${3:-https://apk.g3h.cloud}"
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

: "${APK_BUILDER_SKILL_KEY:*** APK_BUILDER_SKILL_KEY is required}"

# --- 1. resolve tag -> commit + versionName ------------------
OWNER_REPO="$(git remote get-url origin | sed -E 's#(https?://|git@)github\.com[:/]##; s#\.git$##')"
TAGREF="refs/tags/$TAG"
COMMIT="$(git ls-remote origin "$TAGREF" | awk '{print $1}')"
[ -n "$COMMIT" ] || { echo "FATAL: tag $TAG not found on origin" >&2; exit 1; }
VERSION_NAME="${TAG#v}"
case "$CODE" in (*[!0-9]*|'') echo "FATAL: versionCode must be an integer" >&2; exit 1;; esac
echo "tag=$TAG commit=${COMMIT:0:12} versionName=$VERSION_NAME versionCode=$CODE"

# --- 2. download the tag archive from GitHub -----------------
WORK="$(mktemp -d)"
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
OUT="handoff/nimbus-speedtest-${VERSION_NAME}-v${CODE}-signed.apk"
mkdir -p handoff
curl -s -m 180 "$BASE/api/v1/builds/$BID/download" -H "User-Agent: $UA" -H "X-API-Key: $APK_BUILDER_SKILL_KEY" -o "$OUT"
echo "wrote $OUT ($(stat -c%s "$OUT") bytes)"

# --- 7. report + remind --------------------------------------
sha256sum "$OUT"
echo
echo "DONE. Before shipping, run the section 10.1 gate on $OUT:"
echo "  aapt dump badging / apksigner verify --verbose --print-certs / placeholder scan"
echo "and append the evidence to handoff/RELEASE_RECORD.md."
