package com.example.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class BroilerGuide(
    val day: Int,
    val targetWeightG: Int,
    val feedG: Int,
    val waterMl: Int,
    val temp: String,
    val humidity: String,
    val light: String,
    val ventilation: String,
    val tasks: List<String>
)

data class GuideChapter(
    val id: Int,
    val tag: String,
    val title: String,
    val subtitle: String,
    val targetStandard: String = "Target IP > 400 (Standar Profesional Kemitraan Broiler)",
    val sections: List<GuideSection>
)

data class GuideSection(
    val heading: String,
    val items: List<String> = emptyList(),
    val tableHeaders: List<String> = emptyList(),
    val tableRows: List<List<String>> = emptyList(),
    val note: String? = null
)

object BroilerGuideData {
    // Sesuai Bab 3 & Bab 4 PDF PANDUAN SEJAHTERA BERSAMA
    // Hari 0 s.d. 35
    private val feedDailyTarget = intArrayOf(
        15, 18, 22, 28, 34, 40, 46, 52, // 0-7 hari (H0:14-16, H1:18, H2:22, H3:28, H4:34, H5:40, H6:46, H7:52)
        56, 60, 65, 70, 75, 78, 80,     // 8-14 hari (H14: 80g)
        85, 90, 96, 102, 108, 114, 120, // 15-21 hari (H21: 120g)
        125, 131, 137, 143, 150, 155, 160, // 22-28 hari (H28: 160g)
        165, 170, 176, 182, 188, 194, 200  // 29-35 hari (H35: 190-200g)
    )

    private val weightDailyTarget = intArrayOf(
        42, 55, 75, 95, 120, 145, 170, 200, // 0-7 hari (H0:40-42, H1:55, H2:75, H3:95, H4:120, H5:145, H6:170, H7:195-200)
        235, 270, 310, 350, 395, 445, 500,  // 8-14 hari (H14: 470-500g)
        550, 610, 670, 735, 800, 870, 950,  // 15-21 hari (H21: 900-950g)
        1020, 1090, 1170, 1250, 1330, 1410, 1500, // 22-28 hari (H28: 1400-1500g)
        1570, 1640, 1710, 1780, 1850, 1920, 2000  // 29-35 hari (H35: 1900-2000g)
    )

    private val waterDailyTarget = intArrayOf(
        15, 20, 30, 40, 30, 60, 70, 85, // 0-7 hari (H0:12-15ml, H1:20ml, H2:30ml, H3:40ml, H4:30ml, H5:60ml, H6:70ml, H7:85ml)
        95, 105, 118, 130, 142, 156, 170, // 8-14 hari (H14: 170ml)
        180, 192, 205, 218, 230, 240, 250, // 15-21 hari (H21: 250ml)
        265, 280, 295, 310, 325, 340, 350, // 22-28 hari (H28: 350ml)
        370, 390, 410, 430, 450, 475, 500  // 29-35 hari (H35: 450-500ml)
    )

    fun forDay(day: Int): BroilerGuide {
        val d = day.coerceIn(0, 35)
        val temp = when (d) {
            in 0..3 -> "32–34 °C"
            in 4..7 -> "31–32 °C"
            in 8..14 -> "29–30 °C"
            in 15..21 -> "27–28 °C"
            else -> "24–26 °C"
        }
        val hum = when (d) {
            in 0..7 -> "60–70%"
            in 8..14 -> "60–70%"
            else -> "50–70%"
        }
        val light = when (d) {
            in 0..3 -> "24 jam menyala (terang)"
            in 4..7 -> "23 jam terang + 1 jam gelap"
            in 8..14 -> "20–22 jam terang + 2–4 jam gelap"
            in 15..21 -> "18–20 jam terang + 4–6 jam gelap"
            else -> "18 jam terang + 6 jam gelap"
        }
        val vent = when (d) {
            in 0..7 -> "Kecepatan 0,1–0,3 m/dtk (Udara segar tanpa angin langsung mengenai DOC)"
            in 8..14 -> "Kecepatan 0,3–0,5 m/dtk (Ventilasi ditambah bertahap, amonia tidak boleh tercium)"
            in 15..21 -> "Kecepatan 0,5–1,0 m/dtk (Pertukaran udara aktif optimal)"
            else -> "Kecepatan 1,5–2,5 m/dtk (Maksimalkan pertukaran udara, cegah heat stress)"
        }

        val tasks = when (d) {
            0 -> listOf(
                "Pagi: Pastikan suhu brooder 32–34°C dan pemanas siap",
                "Periksa air minum suhu 25–28°C dan pakan awal di tray (1/4–1/3 bagian)",
                "Sebarkan pakan di tray (1 tray untuk 50 DOC)",
                "Siang: Cek penyebaran DOC dan periksa crop fill (2 jam min 75%)",
                "Tambah air jika berkurang dan pantau ventilasi minimum",
                "Sore: Tambah pakan baru sedikit demi sedikit dan hitung DOC mati",
                "Malam: Cek suhu brooder dan dengarkan suara DOC (tidak boleh terus-menerus menjerit)"
            )
            in 1..7 -> listOf(
                "Pagi: Cek suhu kandang & isi ulang pakan dan air segar",
                "Pagi: Buang DOC mati dan catat mortalitas harian",
                "Siang: Periksa crop fill tembolok dan cek kondisi litter sekam",
                "Sore: Tambah pakan sedikit demi sedikit (isi 1/4–1/3 feeder)",
                "Sore: Bersihkan tempat minum / nipple secara menyeluruh",
                "Pemeriksaan berat badan & keseragaman (sampling 100 ekor di Hari 7)",
                "Lakukan biosekuriti gerbang, footbath, dan pembatasan tamu"
            )
            in 8..14 -> listOf(
                "Pagi: Bersihkan tempat minum dan kurangi tray pakan bertahap",
                "Mulai operasikan feeder gantung/otomatis (tinggi sejajar punggung)",
                "Pemberian pakan 4–5 kali/hari (jadwal 06.00, 11.00, 16.00, 20.00)",
                "Cek litter sekam, bolak-balik jika mulai lembab / ganti jika basah",
                "Tingkatkan ventilasi bertahap dan pastikan bau amonia tidak tercium",
                "Sampling bobot berkala dan evaluasi FCR sementara"
            )
            in 15..21 -> listOf(
                "Pastikan semua ayam bisa makan serentak tanpa berebut",
                "Pemberian pakan 4–5 kali/hari dengan kontrol feed waste (<1%)",
                "Flushing pipa air minum 1–2 kali sehari (Pagi 06.00 & Sore 17.00)",
                "Cek debit nipple 60–80 ml/menit dan tinggi nipple (leher 45°)",
                "Atur tirai / kipas ventilasi (target kecepatan angin 0,5–1,0 m/dtk)",
                "Sampling bobot mingguan (target Hari 21: 900–950 gram)"
            )
            in 22..28 -> listOf(
                "Pemberian pakan 3–4 kali/hari (hindari mengisi feeder kepenuhan)",
                "Atur ketinggian tempat pakan setiap hari sejajar punggung ayam",
                "Pantau risiko heat stress di siang hari (11.00–15.00), maksimalkan aliran udara",
                "Pastikan rasio konsumsi air : pakan terjaga 1,8–2,0 : 1",
                "Flushing pipa minum dan cek debit nipple (target >80 ml/menit)",
                "Sampling bobot mingguan (target Hari 28: 1.400–1.500 gram)"
            )
            else -> listOf(
                "Pemberian pakan 3–4 kali/hari dengan pencatatan teliti per ekor",
                "Maksimalkan pertukaran udara (kecepatan angin 1,5–2,5 m/dtk)",
                "Jaga litter tetap kering untuk mencegah penyakit dan lepuh dada",
                "Pantau tanda ayam makan & minum normal serta kotoran ayam",
                "Persiapan panen: jadwalkan penangkapan, siapkan keranjang & timbangan",
                "Ikuti instruksi penghentian pakan sebelum panen (air minum tetap tersedia)",
                "Target panen Hari 35: Bobot 1,90–2,00 kg, FCR ≤1,40, Deplesi ≤2,5%, IP >400"
            )
        }

        return BroilerGuide(
            day = d,
            targetWeightG = weightDailyTarget[d],
            feedG = feedDailyTarget[d],
            waterMl = waterDailyTarget[d],
            temp = temp,
            humidity = hum,
            light = light,
            ventilation = vent,
            tasks = tasks
        )
    }

    fun ageDays(chickInDate: String, now: Date = Date()): Int {
        return try {
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val start = f.parse(chickInDate) ?: return 0
            TimeUnit.MILLISECONDS.toDays(now.time - start.time).toInt().coerceAtLeast(0)
        } catch (_: Exception) { 0 }
    }

    // 15 BAB LENGKAP DARI PDF PANDUAN SEJAHTERA BERSAMA
    val chapters: List<GuideChapter> = listOf(
        GuideChapter(
            id = 1,
            tag = "Bab 1",
            title = "Persiapan Kandang",
            subtitle = "Sanitasi, biosekuriti, masa istirahat, desinfeksi, litter, pemanasan awal (pre-heating), tempat pakan dan minum.",
            sections = listOf(
                GuideSection(
                    heading = "1. Sanitasi dan Biosekuriti",
                    items = listOf(
                        "Sanitasi adalah proses membersihkan kandang, sedangkan biosekuriti adalah upaya mencegah bibit penyakit masuk ke kandang.",
                        "Setelah Panen (Hari ke-0): Semua ayam keluar dari kandang, keluarkan seluruh sisa pakan, bersihkan tempat pakan dan minum, buang seluruh sekam bekas.",
                        "Hari ke-1: Sapu debu hingga bersih, kerok kotoran yang menempel, bersihkan langit-langit, tirai, kipas, dan dinding.",
                        "Pencucian (Gunakan air bertekanan tinggi): Urutan pencucian: Atap -> Langit-langit -> Dinding -> Tiang -> Lantai -> Saluran air. Gunakan deterjen khusus peternakan agar biofilm hilang."
                    )
                ),
                GuideSection(
                    heading = "2. Masa Istirahat Kandang (Down Time)",
                    items = listOf(
                        "Masa kosong kandang sangat penting untuk memutus siklus penyakit.",
                        "Target ideal: Minimal 14 hari, Ideal 14–21 hari.",
                        "Selama masa ini: Jangan ada ayam lain masuk, tutup akses orang luar, bersihkan lingkungan sekitar kandang."
                    )
                ),
                GuideSection(
                    heading = "3. Desinfeksi Bertahap",
                    items = listOf(
                        "Tahap 1 (Setelah kandang dicuci bersih): Semprot seluruh kandang dengan desinfektan, diamkan 24 jam.",
                        "Tahap 2 (Semprot kembali): Lantai, Dinding, Peralatan, Gudang pakan. Diamkan 24 jam.",
                        "Tahap 3 (Dua hari sebelum DOC datang): Semprot ulang seluruh kandang. Satu hari sebelum DOC datang: Semprot jalan masuk, semprot halaman kandang, semprot kendaraan yang masuk."
                    )
                ),
                GuideSection(
                    heading = "4. Persiapan Litter (Sekam)",
                    items = listOf(
                        "Sekam adalah 'kasur' ayam. Kriteria sekam yang baik: Kering, Tidak berjamur, Tidak berbau, Bersih, Tidak bercampur tanah.",
                        "Ketebalan sekam: Musim panas: 8–10 cm, Musim hujan: 10–12 cm.",
                        "JANGAN MENGGUNAKAN: Sekam basah, Sekam hitam, Sekam berjamur, Sekam bekas."
                    )
                ),
                GuideSection(
                    heading = "5. Persiapan Brooder (Pemanas) & Pre-heating",
                    items = listOf(
                        "Brooder dinyalakan 24–48 jam sebelum DOC datang agar suhu lantai sudah stabil.",
                        "Pre-heating: Kesalahan umum adalah hanya memanaskan udara. Yang harus hangat adalah: Sekam, Lantai, Tempat pakan, Tempat minum.",
                        "Pre-heating 24–48 jam membuat DOC tidak kehilangan panas tubuh saat baru datang."
                    ),
                    tableHeaders = listOf("Lokasi", "Target Suhu"),
                    tableRows = listOf(
                        listOf("Di bawah brooder", "32–34°C"),
                        listOf("Pinggir brooder", "30–31°C"),
                        listOf("Luar brooder", "28–29°C")
                    )
                ),
                GuideSection(
                    heading = "6. Persiapan Tempat Pakan dan Minum",
                    items = listOf(
                        "Tempat Pakan: Bersihkan cuci deterjen, bilas, keringkan, semprot desinfektan. Jumlah: 1 tray pakan untuk 50 DOC pada minggu pertama. Isi tray sekitar 1/4–1/3 bagian agar pakan tetap segar dan tidak banyak terbuang.",
                        "Tempat Minum: Bersihkan cuci, bilas, keringkan, semprot desinfektan. Jumlah: 1 tempat minum untuk 50 DOC pada minggu pertama. Sebelum DOC datang: Isi air bersih, suhu air sekitar 25–28°C (tidak terlalu dingin)."
                    )
                )
            )
        ),
        GuideChapter(
            id = 2,
            tag = "Bab 2",
            title = "Manajemen DOC (Day Old Chick)",
            subtitle = "Standar DOC berkualitas, penerimaan, penempatan, air minum & pakan pertama, crop fill, pengamatan 24 jam.",
            sections = listOf(
                GuideSection(
                    heading = "1. Standar DOC Berkualitas",
                    items = listOf(
                        "Ciri-ciri DOC Berkualitas Fisik: Mata cerah dan terbuka lebar, Aktif bergerak, Bulu kering dan mengembang, Warna bulu cerah, Tidak lemas, Tidak pincang, Nafas normal, Dubur bersih, Tidak cacat.",
                        "Berat DOC: Target 38–42 gram/ekor (Ideal 40–42 gram).",
                        "Pusar (Navel) Harus: Kering, Menutup sempurna, Tidak berdarah, Tidak bengkak."
                    )
                ),
                GuideSection(
                    heading = "2. Penerimaan & Penempatan DOC",
                    items = listOf(
                        "Sebelum truk DOC datang (min 2 jam): Brooder menyala, Suhu stabil 32–34°C, Air minum sudah tersedia, Pakan sudah disebar di tray, Lampu menyala, Form penerimaan DOC siap.",
                        "Pemeriksaan Kualitas: Ambil sampel ±100 ekor. Keseragaman (Uniformity) target: ≥85% seragam.",
                        "Penempatan: Jangan dilempar, letakkan perlahan, sebarkan merata di area brooder. Kepadatan minggu pertama: 40–50 ekor/m²."
                    )
                ),
                GuideSection(
                    heading = "3. Air Minum Pertama & Pakan Pertama",
                    items = listOf(
                        "Air Minum Pertama: DOC HARUS MINUM TERLEBIH DAHULU, KEMUDIAN MAKAN. Air bersih, suhu 25–28°C, mudah dijangkau. Selama 2 jam pertama amati apakah sebagian besar DOC sudah menemukan air minum.",
                        "Pakan Pertama: Setelah DOC mulai minum. Isi tray pakan sekitar 1/4–1/3 bagian. Gunakan pakan prestarter berkualitas baik. Hindari penumpukan pakan agar tetap segar."
                    )
                ),
                GuideSection(
                    heading = "4. Pemeriksaan Crop Fill (Tembolok)",
                    items = listOf(
                        "Crop fill menunjukkan apakah DOC sudah minum dan makan dengan baik.",
                        "Setelah 2 jam: Minimal 75% DOC sudah memiliki tembolok berisi air.",
                        "Setelah 8 jam: Target 85%.",
                        "Setelah 24 jam: Target 95%.",
                        "Kondisi Tembolok: Lunak = cukup air; Berisi pakan dan air = ideal; Keras = kurang minum; Kosong = belum menemukan pakan/minum."
                    )
                ),
                GuideSection(
                    heading = "5. Target Hari Pertama",
                    tableHeaders = listOf("Parameter", "Target"),
                    tableRows = listOf(
                        listOf("Mortalitas", "<0,10%"),
                        listOf("Crop fill 24 jam", "≥95%"),
                        listOf("Suhu brooder", "32–34°C"),
                        listOf("Kelembapan", "60–70%"),
                        listOf("Air tersedia", "24 jam"),
                        listOf("Pakan tersedia", "24 jam")
                    )
                )
            )
        ),
        GuideChapter(
            id = 3,
            tag = "Bab 3",
            title = "Program Harian Umur 0–35 Hari",
            subtitle = "Pekerjaan pagi/siang/sore, target suhu, konsumsi pakan, air minum, bobot badan, pencahayaan, ventilasi.",
            sections = listOf(
                GuideSection(
                    heading = "1. Prinsip Program Harian",
                    items = listOf(
                        "Target: FCR ≤1,40 | Deplesi ≤2,5% | IP >400",
                        "Prinsip utama: Ayam harus diperiksa minimal 3 kali sehari (pagi, siang, sore), ditambah pengecekan malam pada umur 0–14 hari."
                    )
                ),
                GuideSection(
                    heading = "2. Target Produksi Harian Broiler (Umur 0–35 Hari)",
                    tableHeaders = listOf("Hari / Umur", "Suhu (°C)", "Pakan (g/ekor)", "Air (ml/ekor)", "Target Bobot (g)"),
                    tableRows = listOf(
                        listOf("Hari 0 (DOC Masuk)", "32–34 °C", "14–16 g", "12–15 ml", "40–42 g"),
                        listOf("Hari 1", "32–33 °C", "18 g", "20 ml", "55 g"),
                        listOf("Hari 2", "32 °C", "22 g", "30 ml", "75 g"),
                        listOf("Hari 3", "31–32 °C", "28 g", "40 ml", "95 g"),
                        listOf("Hari 4", "31 °C", "34 g", "30 ml", "120 g"),
                        listOf("Hari 5", "31 °C", "40 g", "60 ml", "145 g"),
                        listOf("Hari 6", "30 °C", "46 g", "70 ml", "170 g"),
                        listOf("Hari 7", "30 °C", "52 g", "85 ml", "195–200 g"),
                        listOf("Hari 21 (Minggu 3)", "27–28 °C", "120 g", "250 ml", "900–950 g"),
                        listOf("Hari 28 (Minggu 4)", "26–27 °C", "160 g", "350 ml", "1.400–1.500 g"),
                        listOf("Hari 35 (Minggu 5)", "24–26 °C", "190–200 g", "450–500 ml", "1.900–2.000 g")
                    )
                ),
                GuideSection(
                    heading = "3. Program Pencahayaan & Ventilasi",
                    items = listOf(
                        "Umur 0–3 hari: 24 jam lampu menyala",
                        "Umur 4–7 hari: 23 jam terang + 1 jam gelap",
                        "Umur 8–14 hari: 20–22 jam terang + 2–4 jam gelap",
                        "Umur 15–35 hari: 18–20 jam terang + 4–6 jam gelap",
                        "Ventilasi Minggu 1: Udara segar masuk tanpa angin mengenai DOC secara langsung.",
                        "Ventilasi Minggu 2: Ventilasi mulai ditambah bertahap. Amonia tidak boleh tercium.",
                        "Ventilasi Minggu 3–5: Maksimalkan pertukaran udara. Saat cuaca panas, tingkatkan aliran udara untuk membantu ayam membuang panas tubuh."
                    )
                )
            )
        ),
        GuideChapter(
            id = 4,
            tag = "Bab 4",
            title = "Manajemen Pakan",
            subtitle = "Strategi profesional mencapai FCR ≤1,40 & IP >400, jadwal feeding, pencegahan feed waste, tempat pakan, target konsumsi.",
            sections = listOf(
                GuideSection(
                    heading = "1. Prinsip Utama & Target Manajemen Pakan",
                    items = listOf(
                        "Prinsip utama: Pakan menyumbang sekitar 65–75% dari total biaya produksi. Setiap gram pakan yang terbuang akan langsung meningkatkan FCR dan mengurangi keuntungan."
                    ),
                    tableHeaders = listOf("Parameter", "Target Akhir Umur 35 Hari"),
                    tableRows = listOf(
                        listOf("FCR", "≤ 1,40"),
                        listOf("Feed Waste", "< 1%"),
                        listOf("Uniformity (Keseragaman)", "≥ 85%"),
                        listOf("Bobot Panen", "1,90 – 2,00 kg")
                    )
                ),
                GuideSection(
                    heading = "2. Cara Memperoleh FCR < 1,40",
                    items = listOf(
                        "A. Brooding harus sempurna: Keberhasilan 7 hari pertama menentukan performa berikutnya. Target bobot hari ke-7: 195–200 g, Mortalitas minggu pertama: <0,7%.",
                        "B. Air minum harus selalu tersedia: Perbandingan konsumsi Air : Pakan = 1,8–2,0 : 1. Jika ayam kurang minum, konsumsi pakan turun, pertumbuhan melambat, FCR memburuk.",
                        "C. Berikan pakan sedikit tetapi sering: Jangan mengisi tempat pakan sampai penuh. Lebih baik: Isi 1/4–1/3 bagian, Tambah berkala sepanjang hari. Keuntungan: Pakan selalu segar, Ayam lebih lahap, Feed waste berkurang.",
                        "D. Hindari Heat Stress: Saat suhu tinggi, konsumsi pakan turun dan FCR naik. Maksimalkan ventilasi, berikan air segar, dan tambah aliran udara.",
                        "E. Pertahankan kesehatan usus: Usus yang sehat menyerap nutrisi lebih baik. Jaga litter tetap kering, air minum bersih, dan hindari perubahan pakan mendadak."
                    )
                ),
                GuideSection(
                    heading = "3. Teknik Feeding Berdasarkan Umur",
                    tableHeaders = listOf("Fase / Umur", "Tempat Pakan Digunakan", "Frekuensi Pemberian Pakan"),
                    tableRows = listOf(
                        listOf("Umur 0–7 Hari", "Tray pakan (isi sedikit tetapi sering)", "5–6 kali / hari"),
                        listOf("Umur 8–21 Hari", "Mulai gunakan feeder utama", "4–5 kali / hari"),
                        listOf("Umur 22–35 Hari", "Gunakan feeder penuh", "3–4 kali / hari")
                    )
                ),
                GuideSection(
                    heading = "4. Jadwal Feeding Profesional",
                    tableHeaders = listOf("Waktu / Jam", "Aktivitas & Instruksi"),
                    tableRows = listOf(
                        listOf("Pagi (06.00)", "Isi feeder, Bersihkan pakan basah, Cek feed waste"),
                        listOf("Siang (11.00)", "Tambah pakan, Ratakan pakan, Cek ayam makan"),
                        listOf("Sore (16.00)", "Isi feeder, Pastikan cukup sampai malam"),
                        listOf("Malam (20.00)", "Cek sisa pakan, Tambah jika perlu")
                    )
                ),
                GuideSection(
                    heading = "5. Tinggi Tempat Pakan & Pencegahan Feed Waste",
                    items = listOf(
                        "Tinggi Tempat Pakan: Harus sejajar dengan tinggi punggung ayam. Jika terlalu rendah: banyak pakan terinjak. Jika terlalu tinggi: ayam sulit makan.",
                        "Kesalahan yang sering terjadi: Feeder terlalu penuh, Feeder terlalu rendah, Ayam berebut karena feeder kurang, Pakan terkena air, Pakan menggumpal.",
                        "Cara Mengurangi Feed Waste: Isi feeder maksimal 1/3–1/2 bagian, atur tinggi feeder setiap hari, bersihkan pakan yang basah, tambahkan jumlah feeder bila perlu, periksa apakah pakan tercecer di sekitar feeder.",
                        "Target kehilangan pakan (Feed Waste): < 1% dari total pakan."
                    )
                ),
                GuideSection(
                    heading = "6. Manajemen Tempat Pakan & Pergantian Pakan",
                    items = listOf(
                        "Minggu Pertama: 1 tray untuk 50 DOC.",
                        "Minggu Kedua: Mulai kurangi tray, gunakan feeder gantung atau feeder otomatis.",
                        "Minggu Ketiga–Panen: Pastikan semua ayam bisa makan bersamaan. Tidak boleh ada ayam yang harus menunggu terlalu lama.",
                        "Pergantian Jenis Pakan (Lakukan secara bertahap):",
                        "• Hari pertama: 75% pakan lama + 25% pakan baru",
                        "• Hari kedua: 50% pakan lama + 50% pakan baru",
                        "• Hari ketiga: 25% pakan lama + 75% pakan baru",
                        "• Hari keempat: 100% pakan baru"
                    )
                ),
                GuideSection(
                    heading = "7. Target Konsumsi Pakan Harian",
                    tableHeaders = listOf("Umur Ayam", "Konsumsi Harian (gram/ekor)"),
                    tableRows = listOf(
                        listOf("1 hari", "18 g"),
                        listOf("7 hari", "52 g"),
                        listOf("14 hari", "80 g"),
                        listOf("21 hari", "120 g"),
                        listOf("28 hari", "160 g"),
                        listOf("35 hari", "190–200 g")
                    )
                ),
                GuideSection(
                    heading = "8. Monitoring Harian & Tanda Ayam Makan",
                    items = listOf(
                        "Setiap pagi catat: Total pakan masuk, Total pakan habis, Sisa pakan, Feed waste, Konsumsi per ekor, Bobot badan, FCR sementara.",
                        "Tanda Ayam Makan Normal: Tembolok terisi, Ayam aktif, Pertumbuhan merata, Kotoran normal.",
                        "Tanda Ada Masalah: Banyak pakan tersisa, Ayam malas makan, Feed waste meningkat, Bobot di bawah target, FCR mulai naik (segera evaluasi kualitas pakan, ketersediaan air, suhu, ventilasi, dan kesehatan ayam)."
                    )
                )
            )
        ),
        GuideChapter(
            id = 5,
            tag = "Bab 5",
            title = "Manajemen Air Minum",
            subtitle = "Standar kualitas air, target konsumsi harian, rasio air:pakan 1,8-2,0:1, flushing pipa, debit & tinggi nipple.",
            sections = listOf(
                GuideSection(
                    heading = "1. Standar Kualitas Air Minum",
                    items = listOf(
                        "Prinsip utama: Ayam dapat bertahan beberapa hari tanpa pakan, tetapi tidak dapat bertahan lama tanpa air. Air minum yang bersih, cukup, dan mudah diakses adalah faktor penting untuk pertumbuhan, kesehatan usus, dan efisiensi pakan.",
                        "Standar: Warna Jernih, Bau Tidak berbau, Rasa Tidak asin/tidak pahit, pH 6,5–7,5, Suhu 25–28°C, Kekeruhan Sangat rendah, Besi (Fe) Serendah mungkin, Amonia Tidak ada.",
                        "Air yang tidak layak (Keruh, Berbau lumpur, Berwarna kuning/cokelat, Mengandung endapan besi) dapat menyebabkan konsumsi pakan turun, diare, gangguan pencernaan, FCR meningkat."
                    )
                ),
                GuideSection(
                    heading = "2. Target Konsumsi Air Minum",
                    tableHeaders = listOf("Umur", "Air (ml/ekor/hari)"),
                    tableRows = listOf(
                        listOf("1 hari", "20 ml"),
                        listOf("7 hari", "85 ml"),
                        listOf("14 hari", "170 ml"),
                        listOf("21 hari", "250 ml"),
                        listOf("28 hari", "350 ml"),
                        listOf("35 hari", "450–500 ml")
                    ),
                    note = "Perbandingan Air : Pakan = 1,8–2,0 liter air untuk setiap 1 kg pakan. Jika cuaca panas, konsumsi air dapat meningkat hingga 2,5 kali dibanding konsumsi pakan."
                ),
                GuideSection(
                    heading = "3. Jadwal Flushing Pipa & Target Debit Nipple",
                    tableHeaders = listOf("Umur Ayam", "Debit Air Nipple"),
                    tableRows = listOf(
                        listOf("0–7 hari", "40–60 ml/menit"),
                        listOf("8–21 hari", "60–80 ml/menit"),
                        listOf(">21 hari", "80–100 ml/menit")
                    ),
                    items = listOf(
                        "Flushing pipa: Minggu 1: Pagi (06.00), Siang (12.00), Sore (17.00); Minggu 2: 2 kali sehari; Minggu 3-Panen: 1-2 kali sehari. Tambahkan flushing saat cuaca sangat panas.",
                        "Tinggi Nipple: Atur setiap hari. Target: Ayam sedikit mendongakkan kepala saat minum, leher membentuk sudut sekitar 45°."
                    )
                )
            )
        ),
        GuideChapter(
            id = 6,
            tag = "Bab 6",
            title = "Manajemen Ventilasi",
            subtitle = "Open house vs semi closed house, target kecepatan angin, target suhu & kelembapan, amonia, heat stress.",
            sections = listOf(
                GuideSection(
                    heading = "1. Fungsi & Target Ventilasi",
                    items = listOf(
                        "Fungsi: Menyediakan oksigen segar, mengeluarkan karbon dioksida, mengurangi amonia, mengontrol suhu kandang, mengurangi kelembapan, mengeringkan litter.",
                        "Open House: Pagi (06.00-09.00) buka tirai bertahap, Siang (10.00-15.00) buka tirai lebih lebar, Sore (16.00-18.00) sesuaikan bukaan tirai, Malam ditutup sebagian dengan tetap menyisakan jalur masuk udara segar."
                    )
                ),
                GuideSection(
                    heading = "2. Target Kecepatan Angin, Suhu & Kelembapan",
                    tableHeaders = listOf("Umur", "Kecepatan Angin", "Target Suhu", "Target Kelembapan"),
                    tableRows = listOf(
                        listOf("0–3 hari", "0,1–0,3 m/detik", "32–34°C", "60–70%"),
                        listOf("4–7 hari", "0,1–0,3 m/detik", "31–32°C", "60–70%"),
                        listOf("8–14 hari", "0,3–0,5 m/detik", "29–30°C", "60–70%"),
                        listOf("15–21 hari", "0,5–1,0 m/detik", "27–28°C", "50–70%"),
                        listOf("22–35 hari", "1,5–2,5 m/detik", "24–26°C", "50–70%")
                    )
                ),
                GuideSection(
                    heading = "3. Cara Membaca Perilaku Ayam & Heat Stress",
                    items = listOf(
                        "Ayam menyebar merata: Suhu sesuai, ventilasi baik, ayam nyaman.",
                        "Ayam bergerombol di bawah pemanas: Suhu terlalu rendah / brooder kurang panas -> Tambah pemanas / kurangi bukaan tirai.",
                        "Ayam menjauh dari pemanas: Suhu terlalu tinggi -> Kurangi panas brooder, tingkatkan ventilasi bertahap.",
                        "Ayam berkumpul di satu sisi kandang: Ada angin langsung (draft) atau distribusi udara tidak merata -> Atur posisi tirai/aliran udara.",
                        "Ayam membuka paruh dan terengah-engah: Heat stress -> Tambah ventilasi, pastikan air minum cukup & segar, kurangi kepadatan jika perlu."
                    )
                )
            )
        ),
        GuideChapter(
            id = 7,
            tag = "Bab 7",
            title = "Manajemen Pencahayaan",
            subtitle = "Jadwal lampu umur 0-35 hari, intensitas cahaya (lux), warna lampu, program saat listrik padam.",
            sections = listOf(
                GuideSection(
                    heading = "1. Jadwal Lampu Umur 0–35 Hari",
                    tableHeaders = listOf("Umur", "Lama Terang", "Lama Gelap", "Tujuan"),
                    tableRows = listOf(
                        listOf("0–3 hari", "24 jam", "0 jam", "DOC cepat belajar makan dan minum"),
                        listOf("4–7 hari", "23 jam", "1 jam", "Mulai melatih ayam beristirahat, kurangi stres"),
                        listOf("8–14 hari", "20–22 jam", "2–4 jam", "Istirahat lebih baik, pertumbuhan tetap optimal"),
                        listOf("15–21 hari", "18–20 jam", "4–6 jam", "Ayam lebih tenang, kurangi aktivitas berlebihan"),
                        listOf("22–35 hari", "18 jam", "6 jam", "Efisiensi konsumsi pakan, perbaiki konversi pakan (FCR)")
                    )
                ),
                GuideSection(
                    heading = "2. Intensitas Cahaya & Warna Lampu",
                    tableHeaders = listOf("Umur", "Intensitas"),
                    tableRows = listOf(
                        listOf("0–7 hari", "30–40 lux"),
                        listOf("8–14 hari", "20–30 lux"),
                        listOf("15–35 hari", "5–15 lux")
                    ),
                    items = listOf(
                        "Warna Lampu: Putih hangat (warm white) atau putih alami (neutral white).",
                        "Terlalu terang: Ayam mudah stres, banyak bergerak, energi terbuang, FCR memburuk.",
                        "Terlalu gelap: Ayam sulit menemukan pakan, bobot badan tertinggal, keseragaman menurun."
                    )
                )
            )
        ),
        GuideChapter(
            id = 8,
            tag = "Bab 8",
            title = "Biosekuriti",
            subtitle = "3 sistem zona (hijau, kuning, merah), SOP masuk/keluar kandang, program desinfeksi, pengendalian kendaraan & hama.",
            sections = listOf(
                GuideSection(
                    heading = "1. Sistem 3 Zona Biosekuriti",
                    items = listOf(
                        "Zona Hijau (Zona Bersih / Steril): Dalam kandang ayam, gudang pakan, ruang peralatan bersih. Hanya petugas kandang, wajib pakai pakaian & sepatu khusus kandang, cuci tangan.",
                        "Zona Kuning (Zona Transisi): Ruang ganti pakaian, tempat cuci tangan, footbath (bak desinfektan). Untuk membersihkan diri sebelum masuk zona hijau.",
                        "Zona Merah (Zona Kotor / Luar): Jalan kendaraan, area parkir, halaman luar. Orang dari zona merah tidak boleh langsung masuk ke zona hijau."
                    )
                ),
                GuideSection(
                    heading = "2. SOP Masuk & Keluar Kandang",
                    items = listOf(
                        "SOP Masuk: 1. Catat tamu yang datang (Nama, asal, tujuan, tanggal & jam); 2. Lepas sepatu luar & jaket luar; 3. Cuci tangan pakai sabun / hand sanitizer; 4. Gunakan baju, celana & sepatu boot kandang; 5. Lewati footbath berisi larutan desinfektan; 6. Masuk kandang dengan tenang agar ayam tidak panik.",
                        "SOP Keluar: 1. Bersihkan sepatu boot; 2. Lewati footbath; 3. Lepas pakaian kandang; 4. Cuci tangan."
                    )
                ),
                GuideSection(
                    heading = "3. Penanganan Ayam Mati & Monitoring",
                    items = listOf(
                        "Penanganan Ayam Mati: Ambil ayam mati segera, catat jumlahnya, buang sesuai prosedur setempat (penguburan/metode lain yang dianjurkan kemitraan). Jangan biarkan bangkai ayam berada di dalam kandang.",
                        "Monitoring Setiap Pagi: Hitung ayam mati, periksa footbath, cek kebersihan kandang, cek kebocoran air, cek litter, cek bau amonia."
                    )
                )
            )
        ),
        GuideChapter(
            id = 9,
            tag = "Bab 9",
            title = "Program Vaksin Broiler",
            subtitle = "Jadwal vaksin, cara vaksin (air minum, tetes mata, suntikan), hal yang harus dihindari, persiapan & penanganan.",
            sections = listOf(
                GuideSection(
                    heading = "1. Jadwal Vaksin Umum",
                    tableHeaders = listOf("Umur", "Program Umum*", "Penyakit yang Dicegah"),
                    tableRows = listOf(
                        listOf("Hari 4–7", "ND atau ND-IB", "Newcastle Disease / Infectious Bronchitis"),
                        listOf("Hari 10–14", "Gumboro (IBD)", "Infectious Bursal Disease"),
                        listOf("Hari 18–21", "Booster ND atau sesuai program", "Newcastle Disease")
                    ),
                    note = "*Jadwal ini adalah contoh umum. Gunakan jadwal resmi dan vaksin dari perusahaan kemitraan / dokter hewan pendamping."
                ),
                GuideSection(
                    heading = "2. Cara Vaksinasi yang Benar",
                    items = listOf(
                        "A. Melalui Air Minum: Bersihkan saluran air bila diperlukan; Hentikan air minum sementara sesuai arahan TS agar ayam haus secukupnya; Larutkan vaksin sesuai petunjuk produsen; Berikan segera setelah dilarutkan; Pastikan semua ayam mendapat kesempatan minum dalam waktu yang dianjurkan.",
                        "B. Melalui Tetes Mata/Hidung: Pegang ayam dengan lembut, berikan dosis sesuai petunjuk, tunggu hingga tetesan terserap.",
                        "C. Melalui Suntikan: Dilakukan oleh petugas yang terlatih. Pastikan jarum steril, dosis tepat, dan teknik penyuntikan benar."
                    )
                ),
                GuideSection(
                    heading = "3. Hal yang Harus Dihindari & Penanganan Pasca Vaksin",
                    items = listOf(
                        "Hindari: Vaksin terkena sinar matahari langsung, menggunakan vaksin kedaluwarsa, melarutkan vaksin terlalu lama sebelum digunakan, peralatan kotor, dosis tidak sesuai.",
                        "Penanganan Pasca Vaksin: Selama 1–2 hari setelah vaksin kurangi aktivitas yang menyebabkan stres, hindari penangkapan/seleksi bila tidak mendesak, pastikan air & pakan selalu tersedia, pantau ayam lebih sering."
                    )
                )
            )
        ),
        GuideChapter(
            id = 10,
            tag = "Bab 10",
            title = "Program Vitamin",
            subtitle = "Vitamin sebelum/setelah vaksin, saat cuaca panas, saat stres, setelah hujan lebat/mati lampu, waktu terbaik pemberian.",
            sections = listOf(
                GuideSection(
                    heading = "1. Tujuan & Jenis Vitamin Umum",
                    items = listOf(
                        "Prinsip utama: Vitamin bukan pengganti pakan atau vaksin, tetapi berfungsi membantu ayam menghadapi kondisi yang menyebabkan stres (vaksinasi, cuaca panas, pindah kandang, gangguan lingkungan).",
                        "Vitamin A: Mendukung kesehatan saluran pernapasan dan mata.",
                        "Vitamin D3: Membantu pembentukan tulang.",
                        "Vitamin E: Mendukung sistem kekebalan.",
                        "Vitamin K: Membantu proses pembekuan darah.",
                        "Vitamin B Kompleks: Mendukung metabolisme dan nafsu makan.",
                        "Vitamin C: Membantu ayam menghadapi stres panas dan stres lingkungan.",
                        "Elektrolit: Membantu menjaga keseimbangan cairan tubuh."
                    )
                ),
                GuideSection(
                    heading = "2. Waktu Terbaik & Program Kondisi Khusus",
                    items = listOf(
                        "Waktu Terbaik: Pagi hari (06.00–09.00). Hindari pemberian saat siang hari ketika suhu sangat tinggi, kecuali ada instruksi khusus dari dokter hewan/TS.",
                        "Sebelum Vaksin (±1 hari sebelum): Mengurangi stres akibat vaksinasi dan menjaga kondisi prima.",
                        "Setelah Vaksin (1–2 hari setelah): Membantu pemulihan tubuh dan menjaga konsumsi pakan.",
                        "Saat Cuaca Panas: Meningkatkan ketahanan tubuh dan menjaga konsumsi air/pakan.",
                        "Saat Stres (seleksi, pindah sekat, cuaca ekstrem): Membantu ayam kembali aktif dan memulihkan konsumsi."
                    )
                )
            )
        ),
        GuideChapter(
            id = 11,
            tag = "Bab 11",
            title = "Target Mingguan",
            subtitle = "Panduan evaluasi performa broiler menuju IP >400, bobot, konsumsi pakan & air kumulatif, uniformity, mortalitas, FCR.",
            sections = listOf(
                GuideSection(
                    heading = "1. Target Performa Mingguan (Standar IP >400)",
                    tableHeaders = listOf("Parameter", "Minggu 1 (H1-7)", "Minggu 2 (H8-14)", "Minggu 3 (H15-21)", "Minggu 4 (H22-28)", "Minggu 5 (H29-35)"),
                    tableRows = listOf(
                        listOf("Bobot Badan", "195–200 g", "470–500 g", "900–950 g", "1.450–1.550 g", "1.900–2.000 g"),
                        listOf("Konsumsi Pakan Kumulatif", "160–170 g/ekor", "500–600 g/ekor", "1.150–1.250 g/ekor", "2.300–2.450 g/ekor", "2.700–2.900 g/ekor"),
                        listOf("Konsumsi Air Kumulatif", "300–340 ml/ekor", "1.000–1.200 ml/ekor", "2.000–2.300 ml/ekor", "4.200–4.600 ml/ekor", "6.000–6.500 ml/ekor"),
                        listOf("Uniformity (Keseragaman)", "≥ 85%", "≥ 86%", "≥ 86%", "≥ 87%", "≥ 88%"),
                        listOf("Mortalitas Kumulatif", "≤ 0,70%", "≤ 1,20%", "≤ 1,80%", "≤ 2,20%", "≤ 2,50%"),
                        listOf("FCR", "0,80–0,90", "1,00–1,10", "1,20–1,25", "1,30–1,35", "≤ 1,40")
                    )
                ),
                GuideSection(
                    heading = "2. Tindakan Koreksi Jika di Bawah Target",
                    items = listOf(
                        "Bobot di bawah target: Periksa kualitas pakan, pastikan air minum cukup, evaluasi ventilasi & suhu, pisahkan ayam kecil bila perlu.",
                        "Uniformity rendah: Tambah tempat pakan/minum bila diperlukan, pastikan distribusi pakan merata, periksa apakah ada ayam yang tertinggal pertumbuhannya.",
                        "Mortalitas meningkat: Segera laporkan ke TS atau dokter hewan, evaluasi biosekuriti, periksa kualitas air dan pakan, cari penyebab sebelum memberikan pengobatan.",
                        "FCR mulai naik: Kurangi feed waste, periksa kualitas litter, periksa kesehatan ayam, evaluasi kenyamanan kandang."
                    )
                )
            )
        ),
        GuideChapter(
            id = 12,
            tag = "Bab 12",
            title = "Penanganan Masalah",
            subtitle = "Panduan profesional mengenali & menangani gangguan kesehatan: Heat stress, CRD, ND, Gumboro, Koksidiosis, Colibacillosis, Nekrotik enteritis.",
            sections = listOf(
                GuideSection(
                    heading = "1. Heat Stress & Gangguan Pernapasan",
                    items = listOf(
                        "Heat Stress: Gejala membuka paruh (panting), sayap direntangkan, banyak diam, nafsu makan turun, minum meningkat. Penanganan: Tingkatkan ventilasi, sediakan air minum dingin/segar, kurangi aktivitas siang hari, berikan elektrolit/vitamin C sesuai instruksi TS.",
                        "CRD (Chronic Respiratory Disease): Gejala batuk, bersin, napas berbunyi, mata berair, pertumbuhan melambat. Faktor risiko: Amonia tinggi, litter basah, ventilasi buruk, kepadatan berlebih. Penanganan: Pisahkan ayam lemah, perbaiki ventilasi, jaga litter tetap kering, pengobatan sesuai petunjuk dokter hewan/TS."
                    )
                ),
                GuideSection(
                    heading = "2. Penyakit Virus & Bakteri Penting",
                    items = listOf(
                        "Newcastle Disease (ND): Gejala nafsu makan turun, gangguan pernapasan, leher terpuntir pada sebagian ayam, kelumpuhan, kematian meningkat. Penanganan: Segera laporkan ke TS/dokter hewan, perketat biosekuriti, batasi lalu lintas orang & peralatan.",
                        "Gumboro (IBD): Gejala ayam lesu, bulu kusam, diare putih, konsumsi pakan turun, mortalitas meningkat. Penanganan: Segera laporkan, pastikan air dan pakan tetap tersedia, kurangi stres.",
                        "Koksidiosis: Gejala diare, kadang terdapat darah pada kotoran, ayam pucat, pertumbuhan lambat. Penanganan: Jaga litter tetap kering, perbaiki ventilasi, pengobatan antikoksidia sesuai petunjuk.",
                        "Colibacillosis: Gejala ayam lesu, nafsu makan turun, gangguan pernapasan, mortalitas meningkat. Faktor risiko: Air minum kotor, ventilasi buruk, infeksi lain.",
                        "Nekrotik Enteritis: Gejala nafsu makan turun, pertumbuhan melambat, kotoran berubah, mortalitas meningkat. Faktor risiko: Gangguan usus, litter lembap, perubahan pakan mendadak."
                    )
                ),
                GuideSection(
                    heading = "3. Kapan Harus Segera Menghubungi TS?",
                    items = listOf(
                        "Kematian meningkat secara tiba-tiba.",
                        "Ayam menunjukkan gejala penyakit yang parah.",
                        "Produksi pakan atau air menurun drastis.",
                        "FCR meningkat tanpa sebab yang jelas.",
                        "Masalah tidak membaik setelah tindakan awal dilakukan.",
                        "Perlu arahan penggunaan obat atau program kesehatan."
                    )
                )
            )
        ),
        GuideChapter(
            id = 13,
            tag = "Bab 13",
            title = "Cara Mencapai IP > 400",
            subtitle = "Rahasia brooding, ventilasi, air minum, FCR rendah, keseragaman, deplesi rendah, jam kritis heat stress, teknik panen.",
            sections = listOf(
                GuideSection(
                    heading = "1. Target Akhir Peternak Berprestasi",
                    tableHeaders = listOf("Parameter", "Target Standar", "Kategori"),
                    tableRows = listOf(
                        listOf("Indeks Performa (IP)", "> 400 (400–430)", "Sangat Baik"),
                        listOf("FCR", "≤ 1,40", "Sangat Baik"),
                        listOf("Deplesi", "≤ 2,5%", "Sangat Baik"),
                        listOf("Livability", "≥ 97,5%", "Sangat Baik"),
                        listOf("Bobot Panen", "1,90–2,00 kg (umur 35 hari)", "Sangat Baik"),
                        listOf("Uniformity (Keseragaman)", "≥ 88%", "Sangat Baik")
                    )
                ),
                GuideSection(
                    heading = "2. 10 Kunci Sukses Peternak IP > 400",
                    items = listOf(
                        "1. Brooding sempurna selama 7 hari pertama.",
                        "2. Air minum selalu bersih dan cukup.",
                        "3. Ventilasi disesuaikan dengan kondisi ayam, bukan hanya suhu.",
                        "4. Feed waste dijaga serendah mungkin (<1%).",
                        "5. Timbang ayam setiap minggu dan lakukan evaluasi.",
                        "6. Jaga litter tetap kering dan amonia serendah mungkin.",
                        "7. Biosekuriti dijalankan tanpa kompromi.",
                        "8. Tanggapi setiap penurunan konsumsi pakan atau air dengan cepat.",
                        "9. Ikuti program kesehatan dan arahan Technical Service (TS).",
                        "10. Lakukan evaluasi setiap selesai panen untuk memperbaiki putaran berikutnya."
                    )
                ),
                GuideSection(
                    heading = "3. Jam Kritis Heat Stress & Teknik Panen",
                    items = listOf(
                        "Jam Kritis Heat Stress: Jam 11.00–15.00. Maksimalkan ventilasi, pastikan air selalu tersedia, kurangi aktivitas penanganan ayam, pantau ayam lebih sering.",
                        "Teknik Panen Agar Susut Minimal: Persiapan panen 12 jam sebelum panen (jadwal panen jelas, bersihkan jalur, siapkan keranjang & timbangan).",
                        "Ikuti instruksi perusahaan mengenai waktu penghentian pakan sebelum panen. Penghentian yang terlalu lama meningkatkan susut bobot, sedangkan terlalu singkat memengaruhi proses di rumah potong. Air minum umumnya tetap tersedia hingga mendekati waktu pemuatan sesuai prosedur perusahaan.",
                        "Penangkapan Ayam: Lakukan perlahan, hindari ayam panik, kurangi suara keras, pencahayaan diredupkan. Pegang dengan hati-hati, jangan melempar ayam, jangan menumpuk terlalu padat."
                    )
                )
            )
        ),
        GuideChapter(
            id = 14,
            tag = "Bab 14",
            title = "Analisis Produksi",
            subtitle = "Panduan profesional menghitung performa & keuntungan: FCR, IP, EEF, deplesi, livability, biaya produksi, laba & bonus kemitraan.",
            sections = listOf(
                GuideSection(
                    heading = "1. Rumus Perhitungan Performa",
                    items = listOf(
                        "FCR (Feed Conversion Ratio) = Total Pakan (kg) ÷ Total Bobot Panen (kg)",
                        "Index Performance (IP) = (Livability % × Bobot Panen (kg) × 100) ÷ (Umur Panen (hari) × FCR)",
                        "EEF (European Efficiency Factor) = (Livability % × Bobot Panen (kg) × 100) ÷ (Umur × FCR)",
                        "Deplesi (%) = (Ayam Hilang + DOC Awal Mati) ÷ DOC Awal × 100",
                        "Livability (%) = 100 – Deplesi (%)"
                    )
                ),
                GuideSection(
                    heading = "2. Standar Penilaian Indeks Performa (IP)",
                    tableHeaders = listOf("Nilai IP", "Penilaian"),
                    tableRows = listOf(
                        listOf("< 300", "Kurang"),
                        listOf("300 – 349", "Cukup"),
                        listOf("350 – 399", "Baik"),
                        listOf("400 – 449", "Sangat Baik"),
                        listOf("> 450", "Istimewa")
                    )
                ),
                GuideSection(
                    heading = "3. Analisis Biaya, Pendapatan & Laba",
                    items = listOf(
                        "Biaya Produksi = Biaya Tetap (penyusutan kandang, peralatan) + Biaya Variabel (DOC, Pakan, Vitamin, Sekam, Listrik, Obat, Vaksin, Tenaga kerja, BBM).",
                        "Pendapatan = Berat Total Panen × Harga per Kg",
                        "Keuntungan / Laba = Pendapatan – Biaya Produksi",
                        "Biaya Produksi per Kg = Total Biaya ÷ Berat Total Panen",
                        "Keuntungan per Ekor = Total Keuntungan ÷ Total Ayam Terjual",
                        "Komponen Bonus Kemitraan: Bonus FCR, Bonus IP/EEF, Bonus deplesi rendah, Bonus harga pasar, Bonus prestasi."
                    )
                )
            )
        ),
        GuideChapter(
            id = 15,
            tag = "Bab 15",
            title = "Target Produksi Profesional",
            subtitle = "Standar mencapai FCR ≤1,40 & IP 400-430 (Panen Umur 35 Hari) untuk kandang semi terbuka / semi open house.",
            sections = listOf(
                GuideSection(
                    heading = "1. Target Produksi Profesional (Panen Umur 35 Hari)",
                    tableHeaders = listOf("Parameter", "Target", "Kategori"),
                    tableRows = listOf(
                        listOf("DOC awal", "100% sehat", "Sangat Baik"),
                        listOf("Deplesi", "≤ 2,50%", "Sangat Baik"),
                        listOf("Livability", "≥ 97,50%", "Sangat Baik"),
                        listOf("Bobot Panen (ABW)", "1,90 – 2,00 kg", "Sangat Baik"),
                        listOf("FCR", "≤ 1,40", "Sangat Baik"),
                        listOf("Uniformity", "≥ 88%", "Sangat Baik"),
                        listOf("IP (Index Performance)", "400 – 430", "Sangat Baik"),
                        listOf("Feed Waste", "< 1%", "Sangat Baik"),
                        listOf("Rasio Air : Pakan", "1,8 – 2,0 : 1", "Ideal"),
                        listOf("Suhu Kandang", "Sesuai umur", "Stabil"),
                        listOf("Amonia", "Tidak tercium", "Aman")
                    )
                ),
                GuideSection(
                    heading = "2. Indikator Bahaya (Segera Lakukan Evaluasi)",
                    items = listOf(
                        "⚠️ Bobot turun >5% dari target -> Evaluasi manajemen brooding, pakan & air",
                        "⚠️ Konsumsi pakan turun >5% -> Evaluasi suhu, ventilasi, kualitas pakan & kesehatan",
                        "⚠️ Konsumsi air turun >5% -> Evaluasi jalur pipa, suhu air, kebocoran & nipple",
                        "⚠️ Mortalitas >0,1% per hari -> Evaluasi gejala penyakit, penanganan & lapor TS",
                        "ℹ️ Ayam panting (membuka paruh) -> Kurangi panas, tingkatkan ventilasi",
                        "⚠️ Bau amonia tercium -> Perbaiki ventilasi, ganti sekam/litter yang basah"
                    )
                )
            )
        )
    )
}
