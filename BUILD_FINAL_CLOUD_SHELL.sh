#!/usr/bin/env bash
set -euo pipefail

# SEJAHTERA BERSAMA — clean final build
# Run from the project root in Google Cloud Shell.

if command -v java >/dev/null 2>&1; then
  java -version
fi

chmod +x ./gradlew

# Prefer JDK 17 when available.
if [ -d "$HOME/.sdkman/candidates/java/17" ]; then
  export JAVA_HOME="$HOME/.sdkman/candidates/java/17"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

./gradlew clean :app:testDebugUnitTest :app:assembleDebug --no-daemon --no-configuration-cache

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
  echo "ERROR: APK tidak ditemukan: $APK" >&2
  exit 1
fi

mkdir -p final-output
cp "$APK" "final-output/SEJAHTERA_BERSAMA_FINAL_DEBUG.apk"
sha256sum "final-output/SEJAHTERA_BERSAMA_FINAL_DEBUG.apk"
ls -lh "final-output/SEJAHTERA_BERSAMA_FINAL_DEBUG.apk"

echo
 echo "BUILD BERHASIL. Install ke HP untuk uji nyata:"
echo "adb install -r final-output/SEJAHTERA_BERSAMA_FINAL_DEBUG.apk"
