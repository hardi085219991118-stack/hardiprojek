# FITUR FINAL — KEUANGAN KANDANG

## Menu
Dashboard > **Keuangan Kandang**

## Pengeluaran / Debit
Tersedia tombol **Tambah +** dengan form:
- Tanggal pengeluaran
- Biaya atau pembelian
- Nilai uang (Rp)
- Kategori
- Keterangan
- Bukti foto melalui kamera/galeri

## Uang Masuk / Kredit
Tersedia tab **Uang Masuk** dengan form yang sama:
- Tanggal pemasukan
- Nama uang masuk / sumber
- Nilai uang (Rp)
- Kategori
- Keterangan
- Bukti foto melalui kamera/galeri

## Saldo
Fitur otomatis menghitung:
- Total Uang Masuk
- Total Pengeluaran
- **Jumlah Uang / Saldo Saat Ini = Uang Masuk - Pengeluaran**

## Database
ExpenseEntity menggunakan transactionType:
- OUT = pengeluaran/debit
- IN = uang masuk/kredit

Database dinaikkan dari versi 3 ke versi 4 dengan migrasi 3→4 sehingga instalasi lama tetap memiliki jalur migrasi yang benar.

## Backup & PDF
Jenis transaksi ikut disimpan pada backup JSON. Uang masuk tidak dihitung sebagai biaya operasional pada perhitungan biaya dan laporan PDF.
