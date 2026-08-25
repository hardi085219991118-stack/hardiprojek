# AUDIT FITUR KODE PENGHAPUSAN 8888

Tanggal audit: 25 Agustus 2026

## Tujuan
Semua aksi penghapusan data yang terlihat oleh pengguna harus meminta kode `8888` sebelum operasi destruktif dijalankan, untuk mengurangi risiko salah sentuh.

## Implementasi
Komponen pusat: `app/src/main/java/com/example/ui/components/DeletePinGuard.kt`

Komponen yang disediakan:
- `DeletePinProtectedButton`
- `DeletePinProtectedTextButton`
- `DeletePinProtectedIconButton`
- `DeletePinDialog`

Perilaku:
1. Pengguna memilih aksi hapus.
2. Dialog konfirmasi data tetap muncul.
3. Tombol hapus membuka dialog kode keamanan.
4. Hanya kode `8888` yang menjalankan callback penghapusan.
5. Kode salah menampilkan pesan dan data tidak dihapus.
6. Batal atau menutup dialog tidak menghapus data.

## Titik penghapusan yang diproteksi
- Kandang
- Mitra
- Siklus
- Laporan harian
- Kematian / mortalitas
- Stok / transaksi pakan
- Penimbangan bobot
- Obat / vitamin / vaksin
- Keuangan / transaksi
- Panen
- Foto bukti tersimpan
- Foto bukti yang sedang dipilih pada formulir

## Catatan keamanan
Kode `8888` pada tahap ini adalah PIN pengaman operasional terhadap penghapusan tidak sengaja. Ini bukan autentikasi tingkat akun dan bukan penyimpanan rahasia kriptografis. Untuk keamanan yang lebih kuat, PIN dapat dipindahkan ke pengaturan akun dan disimpan dalam bentuk hash.

## Status
- Audit source statis: PASS untuk seluruh aksi hapus UI yang ditemukan.
- Build Gradle: belum dapat diklaim PASS sampai GitHub Actions / Cloud Shell menjalankan build nyata.
