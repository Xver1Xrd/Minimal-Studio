#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if command -v gradle >/dev/null 2>&1 && gradle --version 2>/dev/null | grep -q 'Gradle 8.13'; then
  exec gradle -p "$ROOT" "$@"
fi
DIST_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/minimal-studio-gradle"
BIN="$DIST_DIR/gradle-8.13/bin/gradle"
if [ ! -x "$BIN" ]; then
  mkdir -p "$DIST_DIR"
  URL="https://services.gradle.org/distributions/gradle-8.13-bin.zip"
  echo 'Downloading Gradle 8.13 (requires internet)...'
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --retry 3 "$URL" -o "$DIST_DIR/gradle.zip"
  elif command -v wget >/dev/null 2>&1; then
    wget "$URL" -O "$DIST_DIR/gradle.zip"
  else
    echo 'Install curl or wget first.' >&2; exit 1
  fi
  unzip -q -o "$DIST_DIR/gradle.zip" -d "$DIST_DIR"
  rm "$DIST_DIR/gradle.zip"
fi
exec "$BIN" -p "$ROOT" "$@"
