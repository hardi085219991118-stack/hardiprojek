# SEJAHTERA BERSAMA

Aplikasi Android untuk manajemen budidaya ayam broiler, pencatatan operasional, kemitraan, GPS kandang, foto bukti, dan laporan PDF.

## Build di Google Cloud Shell

Project ini sengaja tidak menyimpan credential pribadi atau `google-services.json`.
Untuk build debug:

```bash
chmod +x gradlew
./gradlew clean
./gradlew assembleDebug
```

APK debug:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Script `gradlew` akan menggunakan Gradle yang tersedia di Cloud Shell. Jika belum ada, script mengunduh Gradle 9.3.1 ke cache pengguna.

## Build release

Gunakan keystore milik sendiri melalui environment variable:

```bash
export KEYSTORE_PATH="$HOME/my-upload-key.jks"
export STORE_PASSWORD='***'
export KEY_PASSWORD='***'
./gradlew assembleRelease
```

Jangan memasukkan keystore, password, token, atau credential ke dalam ZIP/source code.

## Catatan verifikasi akun

Project tidak membuat OTP palsu atau menampilkan OTP di layar. Tanpa backend/provider online, aplikasi hanya dapat membuka Email/WhatsApp dengan pesan kode yang sudah disiapkan; pengiriman aktual tetap dilakukan oleh aplikasi Email/WhatsApp. Verifikasi online otomatis memerlukan backend/provider resmi dan credential milik pemilik aplikasi.
