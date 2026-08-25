#!/usr/bin/env sh
set -eu

GRADLE_VERSION="9.3.1"
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/sejahtera-bersama-gradle/gradle-$GRADLE_VERSION"
GRADLE_BIN="$DIST_DIR/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_USER_HOME/sejahtera-bersama-gradle"
  ARCHIVE="$GRADLE_USER_HOME/sejahtera-bersama-gradle/gradle-$GRADLE_VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  echo "Gradle $GRADLE_VERSION belum tersedia. Mengunduh..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 --connect-timeout 15 -o "$ARCHIVE" "$URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
  else
    echo "ERROR: curl/wget tidak tersedia." >&2
    exit 1
  fi
  rm -rf "$DIST_DIR"
  mkdir -p "$DIST_DIR"
  TMP_DIR="$GRADLE_USER_HOME/sejahtera-bersama-gradle/extract-$GRADLE_VERSION"
  rm -rf "$TMP_DIR"
  mkdir -p "$TMP_DIR"
  unzip -q "$ARCHIVE" -d "$TMP_DIR"
  mv "$TMP_DIR/gradle-$GRADLE_VERSION" "$DIST_DIR"
  rm -rf "$TMP_DIR"
fi

exec "$GRADLE_BIN/bin/gradle" "$@"
