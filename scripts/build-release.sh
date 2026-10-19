#!/usr/bin/env bash
# Builds and signs Minimal Studio with YOUR private key (do not commit your key/password).
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]; then
  echo 'Set ANDROID_HOME to the local Android SDK path.' >&2
  exit 2
fi
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT}}"
if [ "$#" -ne 2 ]; then
  echo "Usage: $0 /path/to/release-keystore.jks KEY_ALIAS" >&2
  exit 2
fi
KEYSTORE="$1"
ALIAS="$2"
[ -f "$KEYSTORE" ] || { echo "Keystore not found" >&2; exit 2; }
./gradlew --no-daemon clean lintRelease testDebugUnitTest assembleRelease
BIN="$SDK/build-tools/35.0.0"
if [ ! -x "$BIN/zipalign" ] || [ ! -x "$BIN/apksigner" ]; then
  echo 'Install Android SDK Build-Tools 35.0.0.' >&2; exit 2
fi
mkdir -p dist
"$BIN/zipalign" -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk dist/minimal-studio-aligned.apk
"$BIN/apksigner" sign --ks "$KEYSTORE" --ks-key-alias "$ALIAS" --out dist/minimal-studio-1.1.0.apk dist/minimal-studio-aligned.apk
"$BIN/apksigner" verify --verbose dist/minimal-studio-1.1.0.apk
rm dist/minimal-studio-aligned.apk
printf 'Signed APK: %s\n' "$PWD/dist/minimal-studio-1.1.0.apk"
