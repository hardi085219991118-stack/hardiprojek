# WORKFLOW AI STUDIO + GITHUB — SEJAHTERA BERSAMA

## Aturan utama untuk AI Studio
Jangan membuat project baru. Audit project yang sudah ada.
Jangan menghapus fitur lama.
Jangan mengganti fitur nyata dengan dummy.
Jangan mengubah database tanpa migrasi yang aman.
Jangan menyatakan build sukses tanpa hasil build nyata.

## Prompt audit yang dapat dipakai di Google AI Studio
"Audit project Android SEJAHTERA BERSAMA yang sudah ada secara menyeluruh. Pertahankan semua fitur yang sudah berfungsi. Jangan membuat ulang aplikasi dari nol. Periksa Gradle, dependency, Kotlin, Jetpack Compose, Room database, lifecycle, coroutine, I/O, PDF, foto bukti, dan navigasi. Cari error build nyata dan perbaiki akar masalah tanpa menonaktifkan fitur. Semua aksi hapus data pengguna wajib meminta PIN 8888. Setelah perubahan, jalankan build dan test yang tersedia. Jika build belum berhasil, jangan klaim sukses. Tampilkan file yang diubah dan alasan setiap perubahan."

## GitHub
Push source ke repository GitHub lalu gunakan workflow .github/workflows/android-build.yml.
Status audit akhir hanya PASS setelah GitHub Actions menunjukkan build/test nyata berhasil.
