#!/usr/bin/env bash
# ==============================================================================
# Script Build APK Otomatis di Google Cloud Shell
# Aplikasi: SEJAHTERA BERSAMA (Manajemen Ayam Broiler & Kemitraan)
# ==============================================================================
set -e

echo "=== [1/5] Memeriksa & Mengatur Lingkungan Java (JDK 17) ==="
# Cloud Shell biasanya sudah menyediakan Java 17 / OpenJDK
if command -v update-java-alternatives >/dev/null 2>&1; then
  sudo update-java-alternatives --set java-1.17.0-openjdk-amd64 2>/dev/null || true
fi

# Set JAVA_HOME jika belum ada
if [ -z "${JAVA_HOME:-}" ]; then
  if [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
    export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    export PATH="$JAVA_HOME/bin:$PATH"
  elif [ -d "$HOME/.sdkman/candidates/java/17" ]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/17"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
fi

echo "Versi Java yang digunakan:"
java -version

echo
echo "=== [2/5] Menyiapkan Permission & Android SDK ==="
chmod +x ./gradlew

# Setup Android SDK Root jika ada di Cloud Shell standar
if [ -d "/usr/local/share/android-sdk" ]; then
  export ANDROID_SDK_ROOT="/usr/local/share/android-sdk"
  export ANDROID_HOME="/usr/local/share/android-sdk"
fi

echo
echo "=== [3/5] Memulai Proses Kompilasi & Build APK ==="
./gradlew :app:assembleDebug --stacktrace

echo
echo "=== [4/5] Memeriksa File APK Hasil Build ==="
APK_SOURCE="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_SOURCE" ]; then
  mkdir -p build-output
  TARGET_APK="build-output/SejahteraBersama-Debug.apk"
  cp "$APK_SOURCE" "$TARGET_APK"
  
  echo "======================================================================"
  echo "✅ BUILD BERHASIL!"
  echo "======================================================================"
  echo "Lokasi File APK: $(pwd)/$TARGET_APK"
  ls -lh "$TARGET_APK"
  echo
  echo "=== [5/5] Cara Download APK ke Komputer / HP dari Cloud Shell ==="
  echo "1. Klik ikon titik tiga (⋮) atau menu 'More' di pojok kanan atas Cloud Shell."
  echo "2. Pilih 'Download File'."
  echo "3. Masukkan path lengkap berikut ini:"
  echo "   $(pwd)/$TARGET_APK"
  echo "4. Klik 'Download' dan file APK akan tersimpan di laptop/HP Anda."
  echo "======================================================================"
else
  echo "❌ Error: File APK tidak ditemukan di $APK_SOURCE"
  exit 1
fi
