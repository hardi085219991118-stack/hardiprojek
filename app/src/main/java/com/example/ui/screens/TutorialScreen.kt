package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.paint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FarmGreenPrimary
import com.example.util.GuidePdfHelper

private data class TutorialStep(
    val number: String,
    val title: String,
    val description: String,
    val tips: String = ""
)

private val tutorialSteps = listOf(
    TutorialStep("1", "Mulai Aplikasi & Akun", "Login dengan akun peternak yang sudah terdaftar. Jika pengguna baru, pilih Daftar lalu selesaikan proses verifikasi aktivasi.", "Pastikan nomor WhatsApp dan email yang digunakan dapat menerima kode verifikasi."),
    TutorialStep("2", "Profil Usaha & Kop Laporan", "Isi nama usaha, slogan, nama pemilik/kepala kandang, nomor WhatsApp, email, alamat, desa, kecamatan, kabupaten, dan provinsi. Simpan profil sebelum membuat laporan.", "Data ini digunakan sebagai identitas dan kop pada dokumen laporan PDF."),
    TutorialStep("3", "Tambah Data Kandang", "Buka Kandang & GPS, lalu isi nama kandang, kode, kapasitas, panjang, lebar, tipe kandang, lokasi, wilayah, pemilik, dan nomor telepon. Ambil koordinat menggunakan GPS bila diperlukan.", "Selesaikan data kandang terlebih dahulu agar pencatatan siklus dan operasional memiliki kandang yang benar."),
    TutorialStep("4", "Mitra Kontrak", "Isi data mitra/perusahaan yang berhubungan dengan kandang atau siklus pemeliharaan.", "Periksa kembali nama mitra dan nomor dokumen agar laporan kemitraan mudah dicocokkan."),
    TutorialStep("5", "Siklus Pemeliharaan", "Buat siklus baru untuk periode pemeliharaan. Pastikan kandang, tanggal mulai, populasi/DOC, dan data dasar siklus sudah benar.", "Gunakan siklus yang aktif saat melakukan pencatatan harian."),
    TutorialStep("6", "Laporan Harian", "Catat tanggal, umur ayam, populasi pagi, ayam mati, afkir/cull, ayam keluar, pakan, air minum, kondisi ayam dan nafsu makan, suhu, kelembaban, obat, vitamin, serta vaksin.", "Masukkan angka sesuai kondisi lapangan pada hari tersebut dan simpan setelah pemeriksaan ulang."),
    TutorialStep("7", "Stok & Penggunaan Pakan", "Gunakan Pakan Masuk untuk mencatat penerimaan DO, jenis pakan, jumlah sak, kg per sak, surat jalan, supplier/mitra, harga, dan keterangan. Gunakan Pakan Digunakan untuk mencatat pemakaian.", "Pantau stok agar jumlah pakan masuk dan pemakaian tetap konsisten."),
    TutorialStep("8", "Penimbangan Bobot & ADG", "Masukkan tanggal, umur, jumlah ayam sampel, total bobot, rata-rata bobot, dan keterangan lokasi penimbangan. Aplikasi membantu menghitung perkembangan bobot dan ADG.", "Gunakan metode pengambilan sampel yang konsisten supaya hasil antarhari dapat dibandingkan."),
    TutorialStep("9", "Kematian & Mortalitas", "Catat tanggal, umur, jumlah ayam mati, penyebab kematian, lokasi/blok kandang, dan keterangan tambahan.", "Catat segera setelah kejadian agar mortalitas harian tidak terlupa."),
    TutorialStep("10", "Obat, Vitamin & Vaksin", "Pilih kategori Vitamin, Vaksin, Obat, atau Desinfektan lalu isi nama produk/merk, dosis, aplikasi, indikasi/tujuan, dan data lain yang tersedia.", "Simpan dokumentasi pemberian sesuai kegiatan lapangan."),
    TutorialStep("11", "Pencatatan Biaya Operasional", "Catat tanggal, kategori biaya, nama pengeluaran/barang, jumlah, satuan, nominal total, dan keterangan.", "Masukkan nominal sesuai bukti transaksi agar total biaya siklus tetap akurat."),
    TutorialStep("12", "Panen & Penjualan Ayam", "Catat tanggal panen, umur, jumlah ayam, total timbang, harga beli/kg, nomor surat jalan, pembeli/bakul/mitra, dan keterangan tambahan.", "Periksa jumlah ekor dan total timbang sebelum menyimpan data panen."),
    TutorialStep("13", "Bukti Foto Kamera & Galeri", "Pada fitur yang menyediakan bukti foto, pilih KAMERA untuk mengambil foto langsung atau GALERI untuk memilih foto yang sudah tersimpan. Foto dapat ditinjau sebelum disimpan.", "Gunakan foto sebagai bukti kondisi kandang, pakan, penimbangan, panen, biaya, obat/vaksin, kematian, atau kegiatan terkait."),
    TutorialStep("14", "Foto Bukti Watermark", "Gunakan fitur foto bukti untuk membuat dokumentasi dengan watermark aplikasi sesuai fungsi yang tersedia.", "Pastikan objek dan informasi penting terlihat jelas sebelum menyimpan foto."),
    TutorialStep("15", "Laporan PDF", "Buka Laporan PDF untuk membuat dokumen laporan dari data yang sudah dicatat. Pastikan Profil & Kop, kandang, mitra, dan siklus sudah benar.", "Periksa pratinjau sebelum mencetak atau membagikan laporan."),
    TutorialStep("16", "Kirim Laporan", "Setelah laporan siap, gunakan fitur pengiriman untuk membagikan laporan melalui kanal yang tersedia di perangkat.", "Pastikan WhatsApp/email aktif dan file laporan sudah benar sebelum dikirim."),
    TutorialStep("17", "Backup & Ekspor", "Gunakan Backup & Ekspor untuk menyimpan cadangan data dan melakukan ekspor data sesuai format yang disediakan aplikasi.", "Lakukan backup secara berkala, terutama sebelum pergantian perangkat atau reset aplikasi."),
    TutorialStep("18", "Profil & Akun", "Gunakan Profil & Akun untuk melihat identitas usaha, status akun, serta fungsi akun yang tersedia.", "Jangan menghapus atau mengganti data akun sebelum memastikan data penting sudah dibackup.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tutorial & Panduan Aplikasi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_tutorial")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmGreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_panduan), contentScale = ContentScale.Crop)
                .testTag("screen_tutorial"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Button(
                    onClick = { GuidePdfHelper.open(context) },
                    modifier = Modifier.fillMaxWidth().testTag("btn_open_panduan_pdf"),
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BUKA PANDUAN SEJAHTERA BERSAMA (PDF)", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF6EA))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Urutan penggunaan yang disarankan", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Profil & Kop → Kandang & GPS → Mitra → Siklus → Laporan Harian → Pakan → Bobot/ADG → Kematian → Obat/Vitamin/Vaksin → Biaya → Panen → Bukti Foto → PDF → Kirim Laporan → Backup.", fontSize = 13.sp)
                        }
                    }
                }
            }

            items(tutorialSteps) { step ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = FarmGreenPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(step.number, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(step.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FarmGreenPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(step.description, fontSize = 13.sp, lineHeight = 19.sp)
                            if (step.tips.isNotBlank()) {
                                Spacer(modifier = Modifier.height(7.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(17.dp))
                                    Text("Tips: ${step.tips}", fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        "Jika bingung, ikuti urutan panduan dari nomor 1 sampai 18. Data yang sudah benar dan tersimpan akan menjadi dasar perhitungan, laporan, PDF, dan backup.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}
