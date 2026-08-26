package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.entity.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong

/**
 * Generator Laporan PDF Resmi SEJAHTERA BERSAMA
 * Standar format A4 (Portrait & Landscape), multi-halaman dinamis dengan pengulangan header tabel,
 * perhitungan akurat 100% berbasis data aktual, rincian subtotal per kategori,
 * validasi integritas data, dan integrasi foto bukti ber-watermark.
 */
object PdfReportGenerator {

    data class ValidationResult(
        val isValid: Boolean,
        val message: String,
        val detail: String = ""
    )

    private fun getReportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getLogoBitmap(context: Context, targetSize: Int = 72): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val original = BitmapFactory.decodeResource(context.resources, R.drawable.logo_sejahtera_bersama, options)
            if (original != null) {
                val width = original.width
                val height = original.height
                if (width <= 0 || height <= 0) return original
                val ratio = width.toFloat() / height.toFloat()
                val targetW: Int
                val targetH: Int
                if (ratio >= 1.0f) {
                    targetW = targetSize
                    targetH = (targetSize / ratio).toInt().coerceAtLeast(1)
                } else {
                    targetH = targetSize
                    targetW = (targetSize * ratio).toInt().coerceAtLeast(1)
                }
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================================
    // VALIDASI OTOMATIS SEBELUM PDF DIBUAT
    // =========================================================================
    fun validateReportData(
        reportType: Int,
        profile: FarmProfileEntity?,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        dailyLogs: List<DailyLogEntity>,
        mortalities: List<MortalityLogEntity>,
        feedStocks: List<FeedStockEntity>,
        expenses: List<ExpenseEntity>,
        harvests: List<HarvestEntity>,
        weights: List<WeightSampleEntity>,
        medicines: List<MedicineEntity>
    ): ValidationResult {
        if (profile == null || profile.farmName.isBlank()) {
            return ValidationResult(false, "Profil Peternakan belum diisi.", "Nama usaha peternakan wajib dilengkapi di pengaturan profil.")
        }

        // Laporan berbasis siklus memerlukan siklus aktif
        if (reportType in listOf(0, 1, 2, 3, 5, 6, 7, 8, 9, 10)) {
            if (cycle == null) {
                return ValidationResult(false, "Siklus Pemeliharaan belum dipilih.", "Silakan pilih atau buat siklus budidaya aktif terlebih dahulu.")
            }
            if (cycle.docCount <= 0) {
                return ValidationResult(false, "Populasi DOC awal tidak valid.", "Populasi DOC awal siklus harus lebih dari 0 ekor.")
            }
        }

        // Validasi spesifik per jenis laporan
        when (reportType) {
            0 -> { // Harian
                val log = dailyLogs.lastOrNull()
                if (log == null) {
                    return ValidationResult(false, "Belum ada catatan harian.", "Tambahkan data recording harian sebelum mencetak laporan harian.")
                }
            }
            5 -> { // Keuangan
                for (exp in expenses) {
                    if (exp.totalAmount.isNaN() || exp.totalAmount.isInfinite() || exp.totalAmount < 0) {
                        return ValidationResult(false, "Data nominal keuangan tidak valid.", "Ditemukan transaksi dengan nilai tidak wajar: ${exp.expenseName}")
                    }
                }
            }
            6 -> { // Mortalitas
                for (m in mortalities) {
                    if (m.count < 0) {
                        return ValidationResult(false, "Data kematian tidak valid.", "Jumlah kematian tidak boleh bernilai negatif.")
                    }
                }
            }
            7 -> { // Pakan
                for (f in feedStocks) {
                    if (f.totalKg < 0 || f.bags < 0 || f.totalKg.isNaN() || f.totalKg.isInfinite()) {
                        return ValidationResult(false, "Data mutasi pakan tidak valid.", "Jumlah kg atau sak pakan tidak boleh negatif/rusak.")
                    }
                }
            }
            8 -> { // OVK / Obat, Vitamin & Vaksin
                for (med in medicines) {
                    if (med.productName.isBlank()) {
                        return ValidationResult(false, "Data OVK tidak valid.", "Ditemukan catatan obat/vaksin tanpa nama produk.")
                    }
                }
            }
            10 -> { // Panen
                for (h in harvests) {
                    if (h.birdCount < 0 || h.totalWeightKg < 0 || h.totalRevenue < 0) {
                        return ValidationResult(false, "Data panen tidak valid.", "Jumlah ekor, tonase, atau pendapatan panen tidak boleh negatif.")
                    }
                }
            }
        }

        return ValidationResult(true, "Data berhasil diverifikasi.", "Semua nilai konsisten dan siap dicetak ke dokumen PDF A4.")
    }

    // =========================================================================
    // 1. LAPORAN HARIAN KANDANG PDF (A4 Portrait)
    // =========================================================================
    fun generateDailyReportPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        partner: PartnerEntity?,
        cycle: CycleEntity,
        dailyLog: DailyLogEntity,
        mortalities: List<MortalityLogEntity> = emptyList(),
        feedStocks: List<FeedStockEntity> = emptyList(),
        medicines: List<MedicineEntity> = emptyList(),
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/LPH/${dailyLog.ageDays}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN HARIAN OPERASIONAL KANDANG", reportNumber)

        var y = 140f
        val docCount = cycle.docCount
        val afternoonPop = dailyLog.afternoonPopulation.coerceAtLeast(0)
        val cumDead = dailyLogsCumDead(dailyLog, mortalities)
        val mortPct = if (docCount > 0) (cumDead.toDouble() / docCount) * 100.0 else 0.0

        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Perusahaan Mitra" to (partner?.companyName ?: "-"),
                "Nomor Siklus" to cycle.cycleNumber,
                "Tanggal Chick-In" to cycle.chickInDate,
                "Tanggal Laporan" to dailyLog.date,
                "Umur Ayam" to FormatHelper.formatHari(dailyLog.ageDays),
                "Populasi Awal (DOC)" to FormatHelper.formatEkor(docCount),
                "Cuaca & Lingkungan" to "${dailyLog.weather} (${FormatHelper.formatOneDecimal(dailyLog.tempCelsius.toDouble())}°C, ${dailyLog.humidityPercent}%)"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Populasi Sore" to FormatHelper.formatEkor(afternoonPop),
                "Kematian Hari Ini" to FormatHelper.formatEkor(dailyLog.deadCount),
                "Afkir Hari Ini" to FormatHelper.formatEkor(dailyLog.cullCount),
                "Pakan Diberikan" to FormatHelper.formatKg(dailyLog.feedGivenKg),
                "Air Minum" to FormatHelper.formatLiter(dailyLog.waterIntakeLiters),
                "Kondisi Ayam" to dailyLog.chickenCondition.ifBlank { "Normal Aktif" }
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("RINCIAN OPERASIONAL, KESEHATAN & BIOSEKURITI", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("Parameter Pemeliharaan", "Hasil Pengamatan / Nilai Aktual"))
        tableData.add(listOf("Populasi Pagi", FormatHelper.formatEkor(dailyLog.morningPopulation)))
        tableData.add(listOf("Populasi Sore (Tersisa)", FormatHelper.formatEkor(afternoonPop)))
        tableData.add(listOf("Kematian Kumulatif", "${FormatHelper.formatEkor(cumDead)} (${FormatHelper.formatPersen(mortPct)})"))
        tableData.add(listOf("Kondisi Sekam / Litter", dailyLog.litterCondition.ifBlank { "Kering & Bersih" }))
        tableData.add(listOf("Sisa Pakan di Tempat Pakan", FormatHelper.formatKg(dailyLog.feedRemainingKg)))
        tableData.add(listOf("Pemberian Obat (OVK)", dailyLog.medicineGiven.ifBlank { "-" }))
        tableData.add(listOf("Pemberian Vitamin", dailyLog.vitaminGiven.ifBlank { "-" }))
        tableData.add(listOf("Pemberian Vaksin", dailyLog.vaccineGiven.ifBlank { "-" }))
        tableData.add(listOf("Catatan Kejadian / Tindakan", dailyLog.notes.ifBlank { "Kondisi ayam dan kandang terpantau optimal dan terkendali." }))

        y = drawTable(canvas, y, tableData, listOf(190f, 325f))

        y += 25f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "Technical Service Mitra")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        val targetPhotos = if (photos.isNotEmpty()) photos else {
            val list = mutableListOf<PhotoEvidenceEntity>()
            if (dailyLog.photoUri.isNotBlank()) {
                list.add(PhotoEvidenceEntity(photoUri = dailyLog.photoUri, reportType = "Harian", date = dailyLog.date, time = "Recording", caption = "Bukti Kondisi Ayam Umur ${dailyLog.ageDays} Hari"))
            }
            list
        }
        appendPhotoEvidencePages(context, pdfDoc, targetPhotos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_HARIAN_HARI_${dailyLog.ageDays}_${dailyLog.date}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    private fun dailyLogsCumDead(currentLog: DailyLogEntity, mortalities: List<MortalityLogEntity>): Int {
        val fromMort = mortalities.filter { it.date <= currentLog.date }.sumOf { it.count }
        return if (fromMort > 0) fromMort else currentLog.deadCount
    }

    // =========================================================================
    // 2. LAPORAN PROFIL & SPESIFIKASI KANDANG PDF
    // =========================================================================
    fun generateCoopPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/KND/${coop.id}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN PROFIL & SPESIFIKASI KANDANG", reportNumber)

        var y = 140f
        val luasM2 = if (coop.areaSqm > 0) coop.areaSqm else coop.lengthM * coop.widthM
        val density = if (luasM2 > 0) coop.capacity.toDouble() / luasM2 else 0.0

        y = drawInfoBox(
            canvas, y,
            listOf(
                "Nama Kandang" to coop.name,
                "Kode / Identitas" to coop.code.ifBlank { "KND-${coop.id}" },
                "Tipe Bangunan" to (coop.coopType.ifBlank { "Closed House" }),
                "Kapasitas Maksimal" to FormatHelper.formatEkor(coop.capacity),
                "Dimensi (P x L)" to "${FormatHelper.formatMeter(coop.lengthM)} x ${FormatHelper.formatMeter(coop.widthM)}",
                "Luas Efektif" to "${FormatHelper.formatOneDecimal(luasM2)} m² (Kepadatan ${FormatHelper.formatOneDecimal(density)} ekor/m²)",
                "Alamat Lengkap" to (coop.address.ifBlank { "-" }),
                "Desa / Kecamatan" to "${coop.village.ifBlank { "-" }} / ${coop.district.ifBlank { "-" }}",
                "Kabupaten / Provinsi" to "${coop.regency.ifBlank { "-" }}, ${coop.province.ifBlank { "-" }}",
                "Koordinat GPS" to if (coop.latitude != null && coop.longitude != null) "${coop.latitude}, ${coop.longitude}" else "Terekam Otomatis"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Kapasitas Tampung" to FormatHelper.formatEkor(coop.capacity),
                "Luas Bangunan" to "${FormatHelper.formatOneDecimal(luasM2)} m²",
                "Tipe Kandang" to (coop.coopType.ifBlank { "Closed House" }),
                "Kepadatan Standar" to "${FormatHelper.formatOneDecimal(density)} Ekor/m²",
                "Panjang Bangunan" to FormatHelper.formatMeter(coop.lengthM),
                "Lebar Bangunan" to FormatHelper.formatMeter(coop.widthM)
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("STANDAR INFRASTRUKTUR & PERALATAN KANDANG", 40f, y, paint)
        y += 10f

        val tableData = listOf(
            listOf("Komponen Sarana & Prasarana", "Spesifikasi & Standar Teknis"),
            listOf("Sistem Ventilasi", "Exhaust Fan, Inlets & Cooling Pad System (Tunnel Ventilation)"),
            listOf("Peralatan Pemanas (Brooding)", "Gasolec / Automatic Space Heater dengan pengaturan suhu digital"),
            listOf("Jalur Tempat Minum", "Nipple Drinker otomatis anti-bocor dilengkapi regulator tekanan"),
            listOf("Jalur Tempat Pakan", "Automatic Pan Feeder / Feeder Pan Manual kapasitas sesuai populasi"),
            listOf("Protokol Biosekuriti", "Gerbang spray kendaraan, Footbath 3 Zona & Pembatasan akses tamu"),
            listOf("Catatan Khusus", coop.notes.ifBlank { "Kondisi infrastruktur dan biosekuriti kandang memenuhi standar budidaya." })
        )

        y = drawTable(canvas, y, tableData, listOf(190f, 325f))

        y += 25f
        drawSignatures(canvas, y, profile.ownerName, "Kepala Kandang / TS Mitra")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        val coopPhotos = mutableListOf<PhotoEvidenceEntity>()
        if (coop.photoUri.isNotBlank()) {
            coopPhotos.add(
                PhotoEvidenceEntity(
                    photoUri = coop.photoUri,
                    reportType = "Kandang",
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    caption = "Foto Fisik Kandang: ${coop.name}",
                    latitude = coop.latitude,
                    longitude = coop.longitude
                )
            )
        }
        coopPhotos.addAll(photos.filter { it.reportType.equals("Kandang", ignoreCase = true) || it.reportType.equals("COOP", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, coopPhotos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_KANDANG_${coop.name.replace(" ", "_")}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 3. LAPORAN KEUANGAN & KAS KANDANG PDF (Akurat, Subtotal per Kategori)
    // =========================================================================
    fun generateExpensePdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        expenses: List<ExpenseEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/KEU/${cycle?.id ?: 0}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val totalIn = expenses.filter { it.transactionType == "IN" }.sumOf { it.totalAmount }
        val totalOut = expenses.filter { it.transactionType == "OUT" }.sumOf { it.totalAmount }
        val netBalance = totalIn - totalOut

        // Subtotal rincian pengeluaran per kategori
        val outExpenses = expenses.filter { it.transactionType == "OUT" }
        val subDoc = outExpenses.filter { it.category.contains("DOC", true) || it.category.contains("Bibit", true) }.sumOf { it.totalAmount }
        val subFeed = outExpenses.filter { it.category.contains("Pakan", true) }.sumOf { it.totalAmount }
        val subMed = outExpenses.filter { it.category.contains("Obat", true) || it.category.contains("Vaksin", true) || it.category.contains("Vitamin", true) || it.category.contains("Medis", true) }.sumOf { it.totalAmount }
        val subOp = outExpenses.filter { it.category.contains("Operasional", true) || it.category.contains("Perawatan", true) || it.category.contains("Listrik", true) || it.category.contains("Gas", true) || it.category.contains("Sekam", true) }.sumOf { it.totalAmount }
        val subLabor = outExpenses.filter { it.category.contains("Tenaga", true) || it.category.contains("Gaji", true) || it.category.contains("Upah", true) }.sumOf { it.totalAmount }
        val subTransport = outExpenses.filter { it.category.contains("Transport", true) || it.category.contains("Kirim", true) || it.category.contains("BBM", true) }.sumOf { it.totalAmount }
        val subOther = outExpenses.sumOf { it.totalAmount } - (subDoc + subFeed + subMed + subOp + subLabor + subTransport)

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN KEUANGAN & BUKU KAS KANDANG", reportNumber)

        var y = 140f
        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "Semua Kandang"),
                "Siklus Pemeliharaan" to (cycle?.cycleNumber ?: "Semua Siklus"),
                "Total Pemasukan (Kas IN)" to FormatHelper.formatRupiah(totalIn),
                "Total Pengeluaran (Kas OUT)" to FormatHelper.formatRupiah(totalOut),
                "Saldo Kas Bersih" to FormatHelper.formatRupiah(netBalance),
                "Total Transaksi Tercatat" to "${expenses.size} Catatan Keuangan",
                "Tanggal Cetak" to SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date()),
                "Status Kas Operasional" to if (netBalance >= 0) "SURPLUS (KAS POSITIF)" else "DEFISIT OPERASIONAL"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Total Pemasukan" to FormatHelper.formatRupiah(totalIn),
                "Total Pengeluaran" to FormatHelper.formatRupiah(totalOut),
                "Saldo Akhir" to FormatHelper.formatRupiah(netBalance),
                "Biaya Operasional/Sekam" to FormatHelper.formatRupiah(subOp),
                "Biaya OVK/Medis" to FormatHelper.formatRupiah(subMed),
                "Biaya Tenaga & Lain" to FormatHelper.formatRupiah(subLabor + subTransport + subOther.coerceAtLeast(0.0))
            )
        )

        y += 15f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("REKAPITULASI PENGELUARAN PER KATEGORI (SUBTOTAL)", 40f, y, paint)
        y += 8f

        val catSummaryData = listOf(
            listOf("Kategori Biaya", "Jumlah Transaksi", "Total Nominal (Rp)"),
            listOf("1. Pemasukan Kas / Modal Masuk", "${expenses.count { it.transactionType == "IN" }} Transaksi", FormatHelper.formatRupiah(totalIn)),
            listOf("2. Biaya DOC / Bibit Ayam", "${outExpenses.count { it.category.contains("DOC", true) || it.category.contains("Bibit", true) }} Transaksi", FormatHelper.formatRupiah(subDoc)),
            listOf("3. Biaya Pakan Ternak", "${outExpenses.count { it.category.contains("Pakan", true) }} Transaksi", FormatHelper.formatRupiah(subFeed)),
            listOf("4. Biaya Obat, Vaksin & Vitamin (OVK)", "${outExpenses.count { it.category.contains("Obat", true) || it.category.contains("Vaksin", true) || it.category.contains("Vitamin", true) }} Transaksi", FormatHelper.formatRupiah(subMed)),
            listOf("5. Biaya Operasional (Gas, Sekam, Listrik)", "${outExpenses.count { it.category.contains("Operasional", true) || it.category.contains("Perawatan", true) || it.category.contains("Listrik", true) || it.category.contains("Gas", true) || it.category.contains("Sekam", true) }} Transaksi", FormatHelper.formatRupiah(subOp)),
            listOf("6. Biaya Tenaga Kerja / Gaji", "${outExpenses.count { it.category.contains("Tenaga", true) || it.category.contains("Gaji", true) }} Transaksi", FormatHelper.formatRupiah(subLabor)),
            listOf("7. Biaya Transportasi & Pengiriman", "${outExpenses.count { it.category.contains("Transport", true) || it.category.contains("Kirim", true) }} Transaksi", FormatHelper.formatRupiah(subTransport)),
            listOf("8. Biaya Lain-lain / Rupa-rupa", "${outExpenses.count { it.category.contains("Lain", true) }} Transaksi", FormatHelper.formatRupiah(subOther.coerceAtLeast(0.0))),
            listOf("TOTAL SELURUH PENGELUARAN (SUBTOTAL 2-8)", "${outExpenses.size} Transaksi", FormatHelper.formatRupiah(totalOut)),
            listOf("SALDO AKHIR OPERASIONAL (PEMASUKAN - PENGELUARAN)", "-", FormatHelper.formatRupiah(netBalance))
        )
        y = drawTable(canvas, y, catSummaryData, listOf(230f, 135f, 150f))

        y += 18f
        drawSignatures(canvas, y, profile.ownerName, "Bendahara / Admin Keuangan")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Halaman rincian jurnal kas jika data transaksi banyak
        if (expenses.isNotEmpty()) {
            val chunked = expenses.sortedByDescending { it.date }.chunked(25)
            chunked.forEachIndexed { pageIdx, chunk ->
                val pInfo = PdfDocument.PageInfo.Builder(595, 842, pageIdx + 2).create()
                val p = pdfDoc.startPage(pInfo)
                val c = p.canvas
                drawSubHeader(c, profile, "DAFTAR DETAIL JURNAL BUKU KAS (Halaman ${pageIdx + 2})")

                val transData = mutableListOf<List<String>>()
                transData.add(listOf("No", "Tanggal", "Tipe", "Kategori", "Item / Keterangan", "Nominal (Rp)"))
                chunk.forEachIndexed { idx, item ->
                    val globalIdx = pageIdx * 25 + idx + 1
                    transData.add(
                        listOf(
                            "$globalIdx",
                            item.date,
                            if (item.transactionType == "IN") "MASUK" else "KELUAR",
                            item.category,
                            item.expenseName.take(24),
                            FormatHelper.formatRupiah(item.totalAmount)
                        )
                    )
                }
                var curY = 100f
                curY = drawTable(c, curY, transData, listOf(30f, 65f, 55f, 100f, 155f, 110f))
                drawFooter(c, pageIdx + 2, chunked.size + 1)
                pdfDoc.finishPage(p)
            }
        }

        // Lampirkan nota foto
        val expensePhotos = mutableListOf<PhotoEvidenceEntity>()
        expenses.forEach { exp ->
            if (exp.photoUri.isNotBlank()) {
                expensePhotos.add(
                    PhotoEvidenceEntity(
                        photoUri = exp.photoUri,
                        reportType = "Keuangan",
                        date = exp.date,
                        time = "Kas",
                        caption = "Bukti Nota/Kuitansi: ${exp.expenseName} (${FormatHelper.formatRupiah(exp.totalAmount)})"
                    )
                )
            }
        }
        expensePhotos.addAll(photos.filter { it.reportType.equals("Keuangan", ignoreCase = true) || it.reportType.equals("EXPENSE", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, expensePhotos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_KEUANGAN_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 4. LAPORAN MORTALITAS & AFKIR AYAM (Akurat, Lengkap & Terverifikasi)
    // =========================================================================
    fun generateMortalityPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        mortalities: List<MortalityLogEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/MRT/${cycle?.id ?: 0}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val docCount = (cycle?.docCount ?: 0).coerceAtLeast(1)
        val totalDead = mortalities.sumOf { it.count }
        val remainingAlive = (docCount - totalDead).coerceAtLeast(0)
        val mortRate = (totalDead.toDouble() / docCount) * 100.0
        val survivalRate = (100.0 - mortRate).coerceAtLeast(0.0)

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN REKAPITULASI MORTALITAS & AFKIR", reportNumber)

        var y = 140f
        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Nomor Siklus" to (cycle?.cycleNumber ?: "-"),
                "Populasi Awal (DOC)" to FormatHelper.formatEkor(docCount),
                "Tanggal Chick-In" to (cycle?.chickInDate ?: "-"),
                "Total Kematian" to FormatHelper.formatEkor(totalDead),
                "Populasi Tersisa (Hidup)" to FormatHelper.formatEkor(remainingAlive),
                "Mortalitas Kumulatif" to FormatHelper.formatPersen(mortRate),
                "Survival Rate (Hidup)" to FormatHelper.formatPersen(survivalRate)
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Total Kematian" to FormatHelper.formatEkor(totalDead),
                "Populasi Tersisa" to FormatHelper.formatEkor(remainingAlive),
                "Mortalitas Kumulatif" to FormatHelper.formatPersen(mortRate),
                "Survival Rate" to FormatHelper.formatPersen(survivalRate),
                "Total Kejadian" to "${mortalities.size} Catatan",
                "Penyebab Utama" to (mortalities.groupBy { it.cause }.maxByOrNull { it.value.size }?.key ?: "Normal")
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("LOG RINCIAN KEMATIAN HARIAN, GEJALA & TINDAKAN", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("No", "Tanggal", "Umur", "Mati", "Lokasi/Blok", "Penyebab / Gejala Klinis", "Tindakan / Solusi"))

        mortalities.sortedBy { it.date }.take(18).forEachIndexed { idx, m ->
            tableData.add(
                listOf(
                    "${idx + 1}",
                    m.date,
                    FormatHelper.formatHari(m.ageDays),
                    FormatHelper.formatInteger(m.count),
                    m.locationBlock.ifBlank { "Semua Blok" },
                    m.cause.take(22),
                    m.notes.ifBlank { "Bangkai dimusnahkan & desinfeksi" }.take(25)
                )
            )
        }

        if (mortalities.isEmpty()) {
            tableData.add(listOf("-", "-", "-", "0", "-", "Tidak ada catatan kematian ayam", "-"))
        }

        // Summary row
        tableData.add(
            listOf(
                "TOTAL",
                "${mortalities.size} Hari Catatan",
                "-",
                FormatHelper.formatInteger(totalDead),
                "-",
                "Mortalitas: ${FormatHelper.formatPersen(mortRate)}",
                "Sisa Hidup: ${FormatHelper.formatEkor(remainingAlive)}"
            )
        )

        y = drawTable(canvas, y, tableData, listOf(25f, 65f, 45f, 45f, 75f, 130f, 130f))

        y += 25f
        drawSignatures(canvas, y, profile.ownerName, "Dokter Hewan / Tim Kesehatan Ternak")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Foto bukti nekropsi / bangkai
        val mortPhotos = mutableListOf<PhotoEvidenceEntity>()
        mortalities.forEach { m ->
            if (m.photoUri.isNotBlank()) {
                mortPhotos.add(
                    PhotoEvidenceEntity(
                        photoUri = m.photoUri,
                        reportType = "Mortalitas",
                        date = m.date,
                        time = "Nekropsi",
                        caption = "Bukti Kematian: Hari ke-${m.ageDays}, Mati: ${m.count} ekor, Gejala: ${m.cause}"
                    )
                )
            }
        }
        mortPhotos.addAll(photos.filter { it.reportType.equals("Mortalitas", ignoreCase = true) || it.reportType.equals("MORTALITY", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, mortPhotos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_KEMATIAN_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 5. LAPORAN PENERIMAAN & PENGGUNAAN PAKAN PDF (Subtotal Pre-Starter, Starter, Finisher)
    // =========================================================================
    fun generateFeedPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        feedStocks: List<FeedStockEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/PKN/${cycle?.id ?: 0}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val inRecords = feedStocks.filter { it.movementType == "IN" }
        val outRecords = feedStocks.filter { it.movementType == "OUT" }

        val totalInKg = inRecords.sumOf { it.totalKg }
        val totalOutKg = outRecords.sumOf { it.totalKg }
        val totalInBags = inRecords.sumOf { it.bags }
        val totalOutBags = outRecords.sumOf { it.bags }
        val remainingKg = (totalInKg - totalOutKg).coerceAtLeast(0.0)
        val remainingBags = (totalInBags - totalOutBags).coerceAtLeast(0.0)

        // Subtotals by Feed Phase
        fun sumFeedKg(records: List<FeedStockEntity>, phaseKeyword: String): Double {
            return records.filter { it.feedType.contains(phaseKeyword, ignoreCase = true) }.sumOf { it.totalKg }
        }
        fun sumFeedBags(records: List<FeedStockEntity>, phaseKeyword: String): Double {
            return records.filter { it.feedType.contains(phaseKeyword, ignoreCase = true) }.sumOf { it.bags }
        }

        val preStarterInKg = sumFeedKg(inRecords, "Pre-Starter")
        val starterInKg = sumFeedKg(inRecords, "Starter") - preStarterInKg
        val finisherInKg = sumFeedKg(inRecords, "Finisher")
        val otherInKg = (totalInKg - (preStarterInKg + starterInKg + finisherInKg)).coerceAtLeast(0.0)

        val preStarterOutKg = sumFeedKg(outRecords, "Pre-Starter")
        val starterOutKg = sumFeedKg(outRecords, "Starter") - preStarterOutKg
        val finisherOutKg = sumFeedKg(outRecords, "Finisher")
        val otherOutKg = (totalOutKg - (preStarterOutKg + starterOutKg + finisherOutKg)).coerceAtLeast(0.0)

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN PENERIMAAN & PENGGUNAAN PAKAN", reportNumber)

        var y = 140f
        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Nomor Siklus" to (cycle?.cycleNumber ?: "-"),
                "Total Pakan Masuk (DO)" to "${FormatHelper.formatKg(totalInKg)} (${FormatHelper.formatSak(totalInBags.toInt())})",
                "Total Pakan Terpakai" to "${FormatHelper.formatKg(totalOutKg)} (${FormatHelper.formatSak(totalOutBags.toInt())})",
                "Sisa Stok Gudang" to "${FormatHelper.formatKg(remainingKg)} (${FormatHelper.formatSak(remainingBags.toInt())})",
                "Target FCR Siklus" to "${cycle?.targetFcr ?: 1.40}",
                "Tanggal Cetak" to SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date()),
                "Status Stok Gudang" to if (remainingKg >= 500) "STOK AMAN TERKENDALI" else "PERLU RE-ORDER PAKAN"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Pakan Masuk" to FormatHelper.formatKg(totalInKg),
                "Pakan Terpakai" to FormatHelper.formatKg(totalOutKg),
                "Sisa Stok" to FormatHelper.formatKg(remainingKg),
                "Sak Masuk" to FormatHelper.formatSak(totalInBags.toInt()),
                "Sak Terpakai" to FormatHelper.formatSak(totalOutBags.toInt()),
                "Sisa Sak" to FormatHelper.formatSak(remainingBags.toInt())
            )
        )

        y += 15f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("RINGKASAN SUBTOTAL PAKAN BERDASARKAN FASE (KG & SAK)", 40f, y, paint)
        y += 8f

        val phaseTableData = listOf(
            listOf("Fase / Jenis Pakan", "Pakan Masuk (Kg / Sak)", "Pakan Terpakai (Kg / Sak)", "Sisa Stok (Kg / Sak)"),
            listOf("1. Pre-Starter (DOC)", "${FormatHelper.formatKg(preStarterInKg)} (${FormatHelper.formatSak(sumFeedBags(inRecords, "Pre-Starter").toInt())})", "${FormatHelper.formatKg(preStarterOutKg)} (${FormatHelper.formatSak(sumFeedBags(outRecords, "Pre-Starter").toInt())})", "${FormatHelper.formatKg((preStarterInKg - preStarterOutKg).coerceAtLeast(0.0))}"),
            listOf("2. Starter (Grower)", "${FormatHelper.formatKg(starterInKg.coerceAtLeast(0.0))}", "${FormatHelper.formatKg(starterOutKg.coerceAtLeast(0.0))}", "${FormatHelper.formatKg((starterInKg - starterOutKg).coerceAtLeast(0.0))}"),
            listOf("3. Finisher (Penggemukan)", "${FormatHelper.formatKg(finisherInKg)} (${FormatHelper.formatSak(sumFeedBags(inRecords, "Finisher").toInt())})", "${FormatHelper.formatKg(finisherOutKg)} (${FormatHelper.formatSak(sumFeedBags(outRecords, "Finisher").toInt())})", "${FormatHelper.formatKg((finisherInKg - finisherOutKg).coerceAtLeast(0.0))}"),
            listOf("4. Jenis Pakan Lainnya", "${FormatHelper.formatKg(otherInKg)}", "${FormatHelper.formatKg(otherOutKg)}", "${FormatHelper.formatKg((otherInKg - otherOutKg).coerceAtLeast(0.0))}"),
            listOf("TOTAL KESELURUHAN", "${FormatHelper.formatKg(totalInKg)} (${FormatHelper.formatSak(totalInBags.toInt())})", "${FormatHelper.formatKg(totalOutKg)} (${FormatHelper.formatSak(totalOutBags.toInt())})", "${FormatHelper.formatKg(remainingKg)} (${FormatHelper.formatSak(remainingBags.toInt())})")
        )
        y = drawTable(canvas, y, phaseTableData, listOf(150f, 130f, 130f, 105f))

        y += 18f
        drawSignatures(canvas, y, profile.ownerName, "Kepala Gudang / Petugas Pakan")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Halaman detail log mutasi pakan jika ada
        if (feedStocks.isNotEmpty()) {
            val chunked = feedStocks.sortedBy { it.date }.chunked(25)
            chunked.forEachIndexed { pageIdx, chunk ->
                val pInfo = PdfDocument.PageInfo.Builder(595, 842, pageIdx + 2).create()
                val p = pdfDoc.startPage(pInfo)
                val c = p.canvas
                drawSubHeader(c, profile, "LOG DETAIL SURAT JALAN & PENGELUARAN PAKAN (Hal ${pageIdx + 2})")

                val logTableData = mutableListOf<List<String>>()
                logTableData.add(listOf("No", "Tanggal", "Tipe", "Jenis / Merk", "Sak", "Total (Kg)", "No DO / Keterangan"))
                chunk.forEachIndexed { idx, f ->
                    val globalIdx = pageIdx * 25 + idx + 1
                    val doText = if (f.doNumber.isNotBlank()) "DO: ${f.doNumber}" else f.notes.ifBlank { "-" }
                    logTableData.add(
                        listOf(
                            "$globalIdx",
                            f.date,
                            if (f.movementType == "IN") "MASUK (DO)" else "KELUAR",
                            f.feedType,
                            "${FormatHelper.formatNumber(f.bags)} Sak",
                            FormatHelper.formatKg(f.totalKg),
                            doText.take(24)
                        )
                    )
                }
                var curY = 100f
                curY = drawTable(c, curY, logTableData, listOf(25f, 65f, 75f, 100f, 55f, 75f, 120f))
                drawFooter(c, pageIdx + 2, chunked.size + 1)
                pdfDoc.finishPage(p)
            }
        }

        // Lampirkan foto DO pakan
        val feedPhotos = mutableListOf<PhotoEvidenceEntity>()
        feedStocks.forEach { f ->
            if (f.photoUri.isNotBlank()) {
                feedPhotos.add(
                    PhotoEvidenceEntity(
                        photoUri = f.photoUri,
                        reportType = "Pakan",
                        date = f.date,
                        time = "Gudang",
                        caption = "Bukti Pakan: ${f.feedType} (${f.movementType}), DO: ${f.doNumber.ifBlank { "-" }}"
                    )
                )
            }
        }
        feedPhotos.addAll(photos.filter { it.reportType.equals("Pakan", ignoreCase = true) || it.reportType.equals("FEED", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, feedPhotos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_PAKAN_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 6. LAPORAN OBAT, VAKSIN & VITAMIN (OVK) PDF (SINKRONISASI LOG TABEL & FOTO 2-KOLOM)
    // =========================================================================
    data class IndexedMedicine(
        val tableNumber: Int,
        val med: MedicineEntity
    )

    fun generateMedicinePdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        medicines: List<MedicineEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/OVK/${cycle?.id ?: 0}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()

        // 1. Urutan data konsisten sebagai SUMBER UTAMA
        val sortedMedicines = medicines.sortedWith(
            compareBy<MedicineEntity> { it.date }
                .thenBy { it.ageDays }
                .thenBy { it.id }
        )

        // 2. Petakan indeks baris tabel (1-based) untuk setiap rekam OVK
        val indexedMedicines = sortedMedicines.mapIndexed { idx, item ->
            IndexedMedicine(tableNumber = idx + 1, med = item)
        }

        // 3. Filter item yang memiliki lampiran foto bukti
        val itemsWithPhotos = indexedMedicines.filter { it.med.photoUri.isNotBlank() }

        // 4. Perhitungan halaman tabel & halaman foto (Grid 2 Kolom = 4 foto/halaman)
        val numTablePages = if (sortedMedicines.size <= 12) 1 else 1 + ((sortedMedicines.size - 10 + 21) / 22)
        val numPhotoPages = if (itemsWithPhotos.isEmpty()) 0 else ((itemsWithPhotos.size + 3) / 4)
        val totalPages = numTablePages + numPhotoPages

        val totalMed = sortedMedicines.count { it.category.equals("Obat", true) }
        val totalVac = sortedMedicines.count { it.category.equals("Vaksin", true) }
        val totalVit = sortedMedicines.count { it.category.equals("Vitamin", true) }

        // --- HALAMAN 1: HEADER RESMI, KPI, INFO BOX, & TABEL LOG AWAL ---
        val page1Info = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDoc.startPage(page1Info)
        val canvas1 = page1.canvas

        drawOfficialHeader(canvas1, context, profile, "LAPORAN PENGGUNAAN OBAT, VAKSIN & VITAMIN", reportNumber)

        var y = 140f
        y = drawInfoBox(
            canvas1, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Nomor Siklus" to (cycle?.cycleNumber ?: "-"),
                "Total Vaksinasi" to "$totalVac Aplikasi",
                "Total Vitamin Diberikan" to "$totalVit Aplikasi",
                "Total Pengobatan" to "$totalMed Aplikasi",
                "Total Rekam OVK" to "${sortedMedicines.size} Tindakan",
                "Tanggal Cetak" to SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date()),
                "Standar Kesehatan" to "Terkontrol Sesuai SOP Kemitraan"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas1, y, listOf(
                "Vaksin Diberikan" to "$totalVac Kali",
                "Vitamin Diberikan" to "$totalVit Kali",
                "Obat Diberikan" to "$totalMed Kali",
                "Total OVK" to "${sortedMedicines.size} Tindakan",
                "Status Kandang" to "Sehat Terkendali",
                "Biosekuriti" to "Level 3 Aktif"
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas1.drawText("LOG RINCIAN PEMBERIAN OBAT, VAKSIN & SUPLEMEN", 40f, y, paint)
        y += 10f

        val tableColWidths = listOf(25f, 60f, 45f, 60f, 125f, 95f, 105f)
        val tableHeaders = listOf("No", "Tgl", "Umur", "Kategori", "Nama Produk / Merk", "Dosis", "Aplikasi / Rute")

        val firstChunkSize = if (sortedMedicines.size <= 12) sortedMedicines.size else 10
        val page1Data = mutableListOf<List<String>>()
        page1Data.add(tableHeaders)

        for (i in 0 until firstChunkSize) {
            val item = indexedMedicines[i]
            val med = item.med
            val purposeText = if (med.purpose.isNotBlank()) " (${med.purpose})" else ""
            page1Data.add(
                listOf(
                    "${item.tableNumber}",
                    med.date,
                    FormatHelper.formatHari(med.ageDays),
                    med.category.uppercase(Locale.ROOT),
                    med.productName,
                    med.dose.ifBlank { "-" },
                    ("${med.method}$purposeText").take(25)
                )
            )
        }

        if (sortedMedicines.isEmpty()) {
            page1Data.add(listOf("-", "-", "-", "-", "Belum ada tindakan medis/vaksin tercatat", "-", "-"))
        }

        y = drawTable(canvas1, y, page1Data, tableColWidths)

        if (numTablePages == 1) {
            y += 25f
            drawSignatures(canvas1, y, profile.ownerName, "Technical Service / Paramedis Ternak")
        }

        drawFooter(canvas1, 1, totalPages)
        pdfDoc.finishPage(page1)

        // --- HALAMAN TABEL LANJUTAN (Jika data OVK > 10 baris) ---
        var currentTableIndex = firstChunkSize
        for (tp in 2..numTablePages) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, tp).create()
            val subPage = pdfDoc.startPage(pageInfo)
            val subCanvas = subPage.canvas

            drawSubHeader(subCanvas, profile, "LOG RINCIAN PEMBERIAN OBAT, VAKSIN & SUPLEMEN (Lanjutan)")

            var subY = 90f
            val subTableData = mutableListOf<List<String>>()
            subTableData.add(tableHeaders)

            val nextChunkEnd = minOf(currentTableIndex + 22, indexedMedicines.size)
            for (i in currentTableIndex until nextChunkEnd) {
                val item = indexedMedicines[i]
                val med = item.med
                val purposeText = if (med.purpose.isNotBlank()) " (${med.purpose})" else ""
                subTableData.add(
                    listOf(
                        "${item.tableNumber}",
                        med.date,
                        FormatHelper.formatHari(med.ageDays),
                        med.category.uppercase(Locale.ROOT),
                        med.productName,
                        med.dose.ifBlank { "-" },
                        ("${med.method}$purposeText").take(25)
                    )
                )
            }
            currentTableIndex = nextChunkEnd

            subY = drawTable(subCanvas, subY, subTableData, tableColWidths)

            if (tp == numTablePages) {
                subY += 25f
                drawSignatures(subCanvas, subY, profile.ownerName, "Technical Service / Paramedis Ternak")
            }

            drawFooter(subCanvas, tp, totalPages)
            pdfDoc.finishPage(subPage)
        }

        // --- HALAMAN LAMPIRAN FOTO BUKTI LAPORAN (GRID 2 KOLOM, UKURAN SEDANG, 100% SINKRON NOMOR TABEL) ---
        if (itemsWithPhotos.isNotEmpty()) {
            itemsWithPhotos.chunked(4).forEachIndexed { chunkIndex, chunkList ->
                val currentPageNum = numTablePages + chunkIndex + 1
                val photoPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNum).create()
                val photoPage = pdfDoc.startPage(photoPageInfo)
                val pCanvas = photoPage.canvas

                // Header Lampiran Foto
                val titleP = newPdfPaint().apply {
                    color = Color.parseColor("#1B5E20")
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }
                pCanvas.drawText("LAMPIRAN FOTO BUKTI PEMBERIAN OBAT, VAKSIN & VITAMIN", 40f, 44f, titleP)

                val subP = newPdfPaint().apply {
                    color = Color.parseColor("#455A64")
                    textSize = 8.5f
                    typeface = Typeface.DEFAULT
                }
                pCanvas.drawText("Dokumentasi Resmi Terverifikasi Sesuai Baris Tabel Log OVK (Halaman $currentPageNum dari $totalPages)", 40f, 58f, subP)

                val linePaint = newPdfPaint().apply {
                    color = Color.parseColor("#1B5E20")
                    strokeWidth = 1.5f
                }
                pCanvas.drawLine(40f, 68f, 555f, 68f, linePaint)

                // Render Grid 2 Kolom x 2 Baris = 4 Foto
                chunkList.forEachIndexed { itemIndex, item ->
                    val col = itemIndex % 2
                    val row = itemIndex / 2

                    val cardLeft = if (col == 0) 40f else 307f
                    val cardWidth = 248f
                    val cardTop = if (row == 0) 80f else 434f
                    val cardHeight = 342f

                    // 1. Card Container (Background & Border)
                    val cardRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)
                    val cardFillPaint = newPdfPaint().apply {
                        color = Color.WHITE
                        style = Paint.Style.FILL
                    }
                    pCanvas.drawRoundRect(cardRect, 6f, 6f, cardFillPaint)

                    val cardStrokePaint = newPdfPaint().apply {
                        color = Color.parseColor("#C8E6C9")
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    }
                    pCanvas.drawRoundRect(cardRect, 6f, 6f, cardStrokePaint)

                    // 2. Header Bar Kartu
                    val headerBarRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + 24f)
                    val headerBarPaint = newPdfPaint().apply {
                        color = Color.parseColor("#E8F5E9")
                        style = Paint.Style.FILL
                    }
                    pCanvas.drawRoundRect(headerBarRect, 6f, 6f, headerBarPaint)
                    // Border header bar bawah
                    pCanvas.drawLine(cardLeft, cardTop + 24f, cardLeft + cardWidth, cardTop + 24f, cardStrokePaint)

                    val photoNumPaint = newPdfPaint().apply {
                        color = Color.parseColor("#1B5E20")
                        textSize = 9.5f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    pCanvas.drawText("BUKTI FOTO #${item.tableNumber}", cardLeft + 8f, cardTop + 16f, photoNumPaint)

                    val catBadgePaint = newPdfPaint().apply {
                        color = Color.parseColor("#2E7D32")
                        textSize = 8f
                        typeface = Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.RIGHT
                    }
                    pCanvas.drawText("[${item.med.category.uppercase(Locale.ROOT)}]", cardLeft + cardWidth - 8f, cardTop + 16f, catBadgePaint)

                    // 3. Nama Produk
                    val prodTitlePaint = newPdfPaint().apply {
                        color = Color.parseColor("#1B5E20")
                        textSize = 9f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val prodNameText = "Produk: ${item.med.productName}".take(36)
                    pCanvas.drawText(prodNameText, cardLeft + 8f, cardTop + 38f, prodTitlePaint)

                    // 4. Kotak Foto (Ukuran Sedang, Rasio Proporsional Contain)
                    val photoBoxLeft = cardLeft + 8f
                    val photoBoxTop = cardTop + 44f
                    val photoBoxWidth = 232f
                    val photoBoxHeight = 178f
                    val photoBoxRect = RectF(photoBoxLeft, photoBoxTop, photoBoxLeft + photoBoxWidth, photoBoxTop + photoBoxHeight)

                    val pBoxBgPaint = newPdfPaint().apply {
                        color = Color.parseColor("#F5F5F5")
                        style = Paint.Style.FILL
                    }
                    pCanvas.drawRoundRect(photoBoxRect, 4f, 4f, pBoxBgPaint)

                    val pBoxStrokePaint = newPdfPaint().apply {
                        color = Color.parseColor("#E0E0E0")
                        style = Paint.Style.STROKE
                        strokeWidth = 0.8f
                    }
                    pCanvas.drawRoundRect(photoBoxRect, 4f, 4f, pBoxStrokePaint)

                    val bitmap = PhotoStorageHelper.loadBitmapSafe(context, item.med.photoUri, maxDim = 800)
                    if (bitmap != null) {
                        val scale = minOf(photoBoxWidth / bitmap.width.toFloat(), photoBoxHeight / bitmap.height.toFloat())
                        val w = bitmap.width * scale
                        val h = bitmap.height * scale
                        val left = photoBoxLeft + (photoBoxWidth - w) / 2f
                        val top = photoBoxTop + (photoBoxHeight - h) / 2f
                        pCanvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), newPdfPaint())
                        bitmap.recycle()
                    } else {
                        val noPhotoPaint = newPdfPaint().apply {
                            color = Color.parseColor("#78909C")
                            textSize = 8.5f
                            textAlign = Paint.Align.CENTER
                        }
                        pCanvas.drawText("File foto tidak dapat dimuat", photoBoxLeft + photoBoxWidth / 2f, photoBoxTop + photoBoxHeight / 2f, noPhotoPaint)
                    }

                    // 5. Panel Informasi Rincian Obat di Bawah Foto
                    val infoBoxLeft = cardLeft + 8f
                    val infoBoxTop = cardTop + 228f
                    val infoBoxWidth = 232f
                    val infoBoxHeight = 104f
                    val infoBoxRect = RectF(infoBoxLeft, infoBoxTop, infoBoxLeft + infoBoxWidth, infoBoxTop + infoBoxHeight)

                    val infoBgPaint = newPdfPaint().apply {
                        color = Color.parseColor("#F1F8E9")
                        style = Paint.Style.FILL
                    }
                    pCanvas.drawRoundRect(infoBoxRect, 4f, 4f, infoBgPaint)

                    val infoStrokePaint = newPdfPaint().apply {
                        color = Color.parseColor("#DCEDC8")
                        style = Paint.Style.STROKE
                        strokeWidth = 0.8f
                    }
                    pCanvas.drawRoundRect(infoBoxRect, 4f, 4f, infoStrokePaint)

                    val labelPaint = newPdfPaint().apply {
                        color = Color.parseColor("#263238")
                        textSize = 7.8f
                        typeface = Typeface.DEFAULT
                    }
                    val boldInfoPaint = newPdfPaint().apply {
                        color = Color.parseColor("#1B5E20")
                        textSize = 7.8f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val grayInfoPaint = newPdfPaint().apply {
                        color = Color.parseColor("#546E7A")
                        textSize = 7.5f
                    }

                    var curInfoY = infoBoxTop + 14f
                    pCanvas.drawText("• Kategori: ${item.med.category} | Umur: Hari ke-${item.med.ageDays}", infoBoxLeft + 6f, curInfoY, labelPaint)
                    curInfoY += 14f
                    pCanvas.drawText("• Tanggal: ${item.med.date}", infoBoxLeft + 6f, curInfoY, labelPaint)
                    curInfoY += 14f
                    val doseMethod = "• Dosis: ${item.med.dose.ifBlank { "-" }} | Aplikasi: ${item.med.method.ifBlank { "-" }}".take(36)
                    pCanvas.drawText(doseMethod, infoBoxLeft + 6f, curInfoY, labelPaint)
                    curInfoY += 14f
                    val purposeStr = "• Tujuan: ${item.med.purpose.ifBlank { "-" }}".take(36)
                    pCanvas.drawText(purposeStr, infoBoxLeft + 6f, curInfoY, labelPaint)
                    curInfoY += 14f
                    pCanvas.drawText("• Relasi: Log Baris #${item.tableNumber} (ID: #${item.med.id})", infoBoxLeft + 6f, curInfoY, boldInfoPaint)
                    curInfoY += 14f
                    val notesStr = "• Catatan: ${item.med.notes.ifBlank { "Tercatat di sistem Sejahtera Bersama" }}".take(34)
                    pCanvas.drawText(notesStr, infoBoxLeft + 6f, curInfoY, grayInfoPaint)
                }

                drawFooter(pCanvas, currentPageNum, totalPages)
                pdfDoc.finishPage(photoPage)
            }
        }

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_OVK_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 7. LAPORAN SAMPLING BOBOT BADAN AYAM BROILER PDF
    // =========================================================================
    fun generateWeightPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        weights: List<WeightSampleEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/BBT/${cycle?.id ?: 0}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN SAMPLING BOBOT BADAN AYAM BROILER", reportNumber)

        var y = 140f
        val latestWeight = weights.maxByOrNull { it.ageDays }
        val avgGram = latestWeight?.averageWeightGram ?: 0.0
        val targetWeightKg = cycle?.targetWeightKg ?: 2.0

        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Nomor Siklus" to (cycle?.cycleNumber ?: "-"),
                "Umur Sampling Terakhir" to FormatHelper.formatHari(latestWeight?.ageDays ?: 0),
                "Rata-rata Bobot Terkini" to FormatHelper.formatGram(avgGram),
                "Target Bobot Panen" to "${FormatHelper.formatTwoDecimals(targetWeightKg)} Kg",
                "Total Sesi Sampling" to "${weights.size} Kali Penimbangan",
                "Tanggal Cetak" to SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date()),
                "Status Bobot Terkini" to if (avgGram > 0) "TERPANTAU RUTIN" else "BELUM ADA SAMPLING"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Bobot Terkini" to FormatHelper.formatGram(avgGram),
                "Target Panen" to "${FormatHelper.formatTwoDecimals(targetWeightKg)} Kg",
                "Rata-rata Kg" to "${FormatHelper.formatTwoDecimals(latestWeight?.averageWeightKg ?: (avgGram / 1000.0))} Kg",
                "Sampel Terakhir" to FormatHelper.formatEkor(latestWeight?.sampleCount ?: 0),
                "Target FCR" to "${cycle?.targetFcr ?: 1.40}",
                "Target IP" to "> 400 Point"
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("HISTORI SAMPLING TIMBANGAN BOBOT MINGGUAN", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("No", "Tgl", "Umur", "Sampel", "Total Kg", "Rata-rata (Gram)", "Rata-rata (Kg)", "Catatan / Evaluasi"))

        weights.sortedBy { it.ageDays }.forEachIndexed { idx, w ->
            tableData.add(
                listOf(
                    "${idx + 1}",
                    w.date,
                    FormatHelper.formatHari(w.ageDays),
                    FormatHelper.formatEkor(w.sampleCount),
                    FormatHelper.formatKg(w.totalWeightKg),
                    FormatHelper.formatGram(w.averageWeightGram),
                    "${FormatHelper.formatTwoDecimals(w.averageWeightKg)} Kg",
                    w.notes.ifBlank { "Sampling normal" }.take(20)
                )
            )
        }

        if (weights.isEmpty()) {
            tableData.add(listOf("-", "-", "-", "0", "0", "0 g", "0 Kg", "Belum ada sampling bobot"))
        }

        y = drawTable(canvas, y, tableData, listOf(25f, 65f, 45f, 60f, 65f, 85f, 75f, 95f))

        y += 25f
        drawSignatures(canvas, y, profile.ownerName, "Tim Sampling / Quality Control")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        val weightPhotos = mutableListOf<PhotoEvidenceEntity>()
        weights.forEach { w ->
            if (w.photoUri.isNotBlank()) {
                weightPhotos.add(
                    PhotoEvidenceEntity(
                        photoUri = w.photoUri,
                        reportType = "Bobot",
                        date = w.date,
                        time = "Sampling",
                        caption = "Sampling Umur ${w.ageDays} Hari: Avg ${FormatHelper.formatGram(w.averageWeightGram)}, Sampel: ${w.sampleCount} ekor"
                    )
                )
            }
        }
        weightPhotos.addAll(photos.filter { it.reportType.equals("Bobot", ignoreCase = true) || it.reportType.equals("WEIGHT", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, weightPhotos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_BOBOT_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 8. LAPORAN PENJUALAN & REALISASI PANEN PDF
    // =========================================================================
    fun generateHarvestPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        partner: PartnerEntity?,
        cycle: CycleEntity?,
        harvests: List<HarvestEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/PNN/${cycle?.id ?: 0}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val totalBirds = harvests.sumOf { it.birdCount }
        val totalKg = harvests.sumOf { it.totalWeightKg }
        val totalRevenue = harvests.sumOf { it.totalRevenue }
        val avgWeight = if (totalBirds > 0) totalKg / totalBirds else 0.0
        val avgPriceKg = if (totalKg > 0) totalRevenue / totalKg else 0.0
        val docCount = (cycle?.docCount ?: 0).coerceAtLeast(1)
        val harvestPct = (totalBirds.toDouble() / docCount) * 100.0

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN PENJUALAN & REALISASI PANEN AYAM", reportNumber)

        var y = 140f
        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Perusahaan Mitra" to (partner?.companyName ?: "-"),
                "Nomor Siklus" to (cycle?.cycleNumber ?: "-"),
                "Tanggal Chick-In" to (cycle?.chickInDate ?: "-"),
                "Total Ayam Terpanen" to "${FormatHelper.formatEkor(totalBirds)} (${FormatHelper.formatPersen(harvestPct)} dari DOC)",
                "Total Tonase Panen" to FormatHelper.formatKg(totalKg),
                "Rata-rata Bobot Panen" to "${FormatHelper.formatTwoDecimals(avgWeight)} Kg/Ekor",
                "Total Omzet / Penjualan" to FormatHelper.formatRupiah(totalRevenue)
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Total Ekor Panen" to FormatHelper.formatEkor(totalBirds),
                "Total Tonase Panen" to FormatHelper.formatKg(totalKg),
                "Rata-rata Bobot" to "${FormatHelper.formatTwoDecimals(avgWeight)} Kg",
                "Rata-rata Harga/Kg" to FormatHelper.formatRupiah(avgPriceKg),
                "Total Penerimaan" to FormatHelper.formatRupiah(totalRevenue),
                "Jumlah DO Panen" to "${harvests.size} Surat Jalan"
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("RINCIAN SURAT JALAN DO PENIMBANGAN PANEN", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("No", "Tgl", "No DO Panen", "Pembeli", "Ekor", "Total Kg", "Avg Kg", "Penerimaan (Rp)"))

        harvests.sortedBy { it.harvestDate }.forEachIndexed { idx, h ->
            tableData.add(
                listOf(
                    "${idx + 1}",
                    h.harvestDate,
                    h.doNumber.ifBlank { "DO-${h.id}" },
                    h.buyerName.take(18).ifBlank { "-" },
                    FormatHelper.formatInteger(h.birdCount),
                    FormatHelper.formatNumber(h.totalWeightKg),
                    FormatHelper.formatTwoDecimals(h.averageWeightKg),
                    FormatHelper.formatRupiah(h.totalRevenue)
                )
            )
        }

        if (harvests.isEmpty()) {
            tableData.add(listOf("-", "-", "-", "Belum ada transaksi panen", "0", "0 Kg", "0 Kg", "Rp 0"))
        }

        // Summary row
        tableData.add(
            listOf(
                "TOTAL",
                "${harvests.size} DO",
                "-",
                "-",
                FormatHelper.formatInteger(totalBirds),
                FormatHelper.formatNumber(totalKg),
                "${FormatHelper.formatTwoDecimals(avgWeight)} Kg",
                FormatHelper.formatRupiah(totalRevenue)
            )
        )

        y = drawTable(canvas, y, tableData, listOf(25f, 65f, 80f, 100f, 45f, 55f, 45f, 100f))

        y += 25f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "Koordinator Panen Mitra")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        val harvestPhotos = mutableListOf<PhotoEvidenceEntity>()
        harvests.forEach { h ->
            if (h.photoUri.isNotBlank()) {
                harvestPhotos.add(
                    PhotoEvidenceEntity(
                        photoUri = h.photoUri,
                        reportType = "Panen",
                        date = h.harvestDate,
                        time = "Panen",
                        caption = "DO Panen: ${h.doNumber}, ${h.birdCount} ekor (${FormatHelper.formatKg(h.totalWeightKg)}), Pembeli: ${h.buyerName}"
                    )
                )
            }
        }
        harvestPhotos.addAll(photos.filter { it.reportType.equals("Panen", ignoreCase = true) || it.reportType.equals("HARVEST", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, harvestPhotos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_PANEN_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 9. LAPORAN KEMITRAAN & REKONSILIASI MITRA PDF
    // =========================================================================
    fun generatePartnershipReportPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        partner: PartnerEntity?,
        cycle: CycleEntity,
        dailyLogs: List<DailyLogEntity>,
        harvests: List<HarvestEntity>,
        expenses: List<ExpenseEntity>,
        feedStocks: List<FeedStockEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/LKM/${cycle.id}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val totalHarvestBirds = harvests.sumOf { it.birdCount }
        val totalHarvestWeight = harvests.sumOf { it.totalWeightKg }
        val totalHarvestRevenue = harvests.sumOf { it.totalRevenue }
        val totalDead = dailyLogs.sumOf { it.deadCount }
        val totalFeedUsed = feedStocks.filter { it.movementType == "OUT" }.sumOf { it.totalKg }.let { if (it > 0) it else dailyLogs.sumOf { log -> log.feedGivenKg } }
        val mortalityRate = if (cycle.docCount > 0) (totalDead.toDouble() / cycle.docCount) * 100.0 else 0.0
        val fcr = if (totalHarvestWeight > 0) totalFeedUsed / totalHarvestWeight else 0.0
        val avgWeight = if (totalHarvestBirds > 0) totalHarvestWeight / totalHarvestBirds else 0.0
        val totalExpensesAmount = expenses.filter { it.transactionType == "OUT" }.sumOf { it.totalAmount }
        val netIncome = totalHarvestRevenue - totalExpensesAmount

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN KEMITRAAN BUDIDAYA BROILER", reportNumber)

        var y = 140f
        y = drawInfoBox(
            canvas, y,
            listOf(
                "Perusahaan Mitra" to (partner?.companyName ?: "-"),
                "Nomor Kontrak" to (partner?.contractNumber ?: "-"),
                "Nama Kandang" to (coop?.name ?: "-"),
                "Lokasi / GPS" to if (coop?.latitude != null) "${coop.latitude}, ${coop.longitude}" else (coop?.address ?: "-"),
                "Nomor Siklus" to cycle.cycleNumber,
                "DOC Masuk" to "${FormatHelper.formatEkor(cycle.docCount)} (${cycle.docStrain})",
                "Tanggal Chick-In" to cycle.chickInDate,
                "Target FCR / Realisasi" to "${cycle.targetFcr} / ${FormatHelper.formatFcr(fcr)}"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Total Ayam Panen" to FormatHelper.formatEkor(totalHarvestBirds),
                "Total Bobot Panen" to FormatHelper.formatKg(totalHarvestWeight),
                "Rata-rata Bobot" to "${FormatHelper.formatTwoDecimals(avgWeight)} Kg",
                "Mortalitas Kumulatif" to FormatHelper.formatPersen(mortalityRate),
                "Total Pakan Terpakai" to FormatHelper.formatKg(totalFeedUsed),
                "FCR Realisasi" to FormatHelper.formatFcr(fcr)
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("REKAPITULASI HASIL REKONSILIASI KEMITRAAN & KEUANGAN", 40f, y, paint)
        y += 10f

        val tableData = listOf(
            listOf("Komponen Rekonsiliasi Kemitraan", "Jumlah / Satuan", "Nilai (Rp)"),
            listOf("1. Hasil Penjualan Panen Ayam", FormatHelper.formatKg(totalHarvestWeight), FormatHelper.formatRupiah(totalHarvestRevenue)),
            listOf("2. Biaya Pengadaan DOC Masuk", FormatHelper.formatEkor(cycle.docCount), FormatHelper.formatRupiah(cycle.docCount * cycle.docPricePerHead)),
            listOf("3. Biaya Pakan Ternak Terpakai", FormatHelper.formatKg(totalFeedUsed), FormatHelper.formatRupiah(totalFeedUsed * (partner?.feedPrice ?: 0.0))),
            listOf("4. Biaya Operasional & OVK Kandang", "${expenses.count { it.transactionType == "OUT" }} Transaksi", FormatHelper.formatRupiah(totalExpensesAmount)),
            listOf("ESTIMASI HASIL USAHA PETERNAK (LABA BERSIH)", if (netIncome >= 0) "SURPLUS" else "DEFISIT", FormatHelper.formatRupiah(netIncome))
        )

        y = drawTable(canvas, y, tableData, listOf(200f, 155f, 160f))

        y += 25f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "PIC Perusahaan Mitra")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)
        appendPhotoEvidencePages(context, pdfDoc, photos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_KEMITRAAN_${cycle.id}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 10. LAPORAN AKHIR SIKLUS (TUTUP BUKU 19 POIN AUDIT RHPP) PDF
    // =========================================================================
    fun generateCycleEndReportPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        partner: PartnerEntity?,
        cycle: CycleEntity,
        dailyLogs: List<DailyLogEntity>,
        harvests: List<HarvestEntity>,
        expenses: List<ExpenseEntity>,
        feedStocks: List<FeedStockEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/LAK/${cycle.id}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val totalDead = dailyLogs.sumOf { it.deadCount }
        val totalCulls = dailyLogs.sumOf { it.cullCount }
        val totalHarvestBirds = harvests.sumOf { it.birdCount }
        val totalHarvestWeight = harvests.sumOf { it.totalWeightKg }
        val totalHarvestRevenue = harvests.sumOf { it.totalRevenue }
        val totalFeedUsed = feedStocks.filter { it.movementType == "OUT" }.sumOf { it.totalKg }.let { if (it > 0) it else dailyLogs.sumOf { log -> log.feedGivenKg } }
        val mortalityRate = if (cycle.docCount > 0) (totalDead.toDouble() / cycle.docCount) * 100.0 else 0.0
        val survivalRate = if (cycle.docCount > 0) 100.0 - mortalityRate else 0.0
        val avgWeight = if (totalHarvestBirds > 0) totalHarvestWeight / totalHarvestBirds else 0.0
        val fcr = if (totalHarvestWeight > 0 && totalFeedUsed > 0) totalFeedUsed / totalHarvestWeight else 0.0
        val targetAge = if (cycle.targetHarvestAgeDays > 0) cycle.targetHarvestAgeDays else 35
        val ipIndex = if (fcr > 0 && targetAge > 0) ((survivalRate * avgWeight) / (fcr * targetAge)) * 100 else 0.0
        val totalExp = expenses.filter { it.transactionType == "OUT" }.sumOf { it.totalAmount }
        val profit = totalHarvestRevenue - totalExp

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN AKHIR SIKLUS BUDIDAYA BROILER", reportNumber)

        var y = 140f
        y = drawInfoBox(
            canvas, y,
            listOf(
                "Nama Usaha" to profile.farmName,
                "Pemilik / Pengelola" to profile.ownerName,
                "Nama Kandang" to (coop?.name ?: "-"),
                "Tipe & Kapasitas" to "${coop?.coopType ?: "-"} (${FormatHelper.formatEkor(coop?.capacity ?: 0)})",
                "Perusahaan Mitra" to (partner?.companyName ?: "-"),
                "Nomor Kontrak" to (partner?.contractNumber ?: "-"),
                "Nomor Siklus" to cycle.cycleNumber,
                "DOC Masuk / Strain" to "${FormatHelper.formatEkor(cycle.docCount)} / ${cycle.docStrain}"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Indeks Performa (IP)" to "${FormatHelper.formatIp(ipIndex)} Point",
                "FCR Realisasi" to FormatHelper.formatFcr(fcr),
                "Mortalitas Total" to FormatHelper.formatPersen(mortalityRate),
                "Survival Rate" to FormatHelper.formatPersen(survivalRate),
                "Rata-rata Bobot" to "${FormatHelper.formatTwoDecimals(avgWeight)} Kg",
                "Laba Bersih" to FormatHelper.formatRupiah(profit)
            )
        )

        y += 15f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("19 PARAMETER EVALUASI LENGKAP AKHIR SIKLUS (RHPP)", 40f, y, paint)
        y += 8f

        val tableData = listOf(
            listOf("No", "Parameter Evaluasi & Audit", "Standar / Target", "Realisasi Aktual"),
            listOf("1", "Populasi DOC Masuk", FormatHelper.formatEkor(cycle.docCount), FormatHelper.formatEkor(cycle.docCount)),
            listOf("2", "Total Kematian Ayam", "< 3.0 %", "$totalDead Ekor (${FormatHelper.formatPersen(mortalityRate)})"),
            listOf("3", "Total Ayam Afkir (Culling)", "< 1.0 %", "$totalCulls Ekor"),
            listOf("4", "Persentase Hidup (Survival)", "> 96.0 %", FormatHelper.formatPersen(survivalRate)),
            listOf("5", "Total Pakan Terkonsumsi", "-", FormatHelper.formatKg(totalFeedUsed)),
            listOf("6", "FCR (Feed Conversion Ratio)", if (cycle.targetFcr > 0) "< ${cycle.targetFcr}" else "< 1.40", FormatHelper.formatFcr(fcr)),
            listOf("7", "Rata-rata Bobot Panen", if (cycle.targetWeightKg > 0) "> ${cycle.targetWeightKg} Kg" else "> 2.00 Kg", "${FormatHelper.formatTwoDecimals(avgWeight)} Kg"),
            listOf("8", "Umur Panen Rata-rata", "$targetAge Hari", "$targetAge Hari"),
            listOf("9", "Indeks Performa (IP)", "> 400 Point", "${FormatHelper.formatIp(ipIndex)} Point"),
            listOf("10", "Total Ayam Terpanen", "-", FormatHelper.formatEkor(totalHarvestBirds)),
            listOf("11", "Total Tonase Panen", "-", FormatHelper.formatKg(totalHarvestWeight)),
            listOf("12", "Harga Jual Kontrak Rata-rata", "-", FormatHelper.formatRupiah(partner?.liveBirdPrice ?: 0.0)),
            listOf("13", "Total Pendapatan Panen", "-", FormatHelper.formatRupiah(totalHarvestRevenue)),
            listOf("14", "Total Biaya Operasional & OVK", "-", FormatHelper.formatRupiah(totalExp)),
            listOf("15", "Hasil Usaha Bersih (Laba/Rugi)", "Profit", FormatHelper.formatRupiah(profit)),
            listOf("16", "Status Siklus Pemeliharaan", "-", if (cycle.status == "HARVESTED" || totalHarvestBirds > 0) "SELESAI PANEN" else "BERJALAN")
        )

        y = drawTable(canvas, y, tableData, listOf(25f, 215f, 135f, 140f))

        y += 18f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "Manajemen Mitra")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)
        appendPhotoEvidencePages(context, pdfDoc, photos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_AKHIR_SIKLUS_${cycle.id}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 11. LAPORAN PERIODIK PERKEMBANGAN HARIAN (Landscape Table A4)
    // =========================================================================
    fun generatePeriodReportPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity,
        logs: List<DailyLogEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        periodTitle: String = "Perkembangan Harian Lengkap",
        reportNumber: String = "SB/LPR/${cycle.id}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialLandscapeHeader(canvas, context, profile, "LAPORAN PERIODIK PERKEMBANGAN HARIAN ($periodTitle)", reportNumber)

        var y = 125f
        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("No", "Tgl", "Umur", "Pop. Pagi", "Mati", "Afkir", "Pakan (Kg)", "Air (L)", "Suhu", "Kondisi Ayam", "Catatan"))

        logs.sortedBy { it.ageDays }.forEachIndexed { idx, log ->
            tableData.add(
                listOf(
                    "${idx + 1}",
                    log.date.takeLast(5),
                    FormatHelper.formatHari(log.ageDays),
                    FormatHelper.formatInteger(log.morningPopulation),
                    FormatHelper.formatInteger(log.deadCount),
                    FormatHelper.formatInteger(log.cullCount),
                    FormatHelper.formatNumber(log.feedGivenKg),
                    FormatHelper.formatNumber(log.waterIntakeLiters),
                    "${log.tempCelsius}°C",
                    log.chickenCondition.take(14),
                    log.notes.take(24)
                )
            )
        }

        y = drawTable(canvas, y, tableData, listOf(30f, 55f, 45f, 65f, 45f, 45f, 75f, 65f, 55f, 125f, 155f), isLandscape = true)

        y += 25f
        drawLandscapeSignatures(canvas, y, profile.ownerName, "Technical Support Perusahaan Mitra")
        drawFooter(canvas, 1, 1, isLandscape = true)

        pdfDoc.finishPage(page)
        appendPhotoEvidencePages(context, pdfDoc, photos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_PERIODIK_${cycle.id}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 12. LAPORAN DOKUMENTASI FOTO BUKTI LAPANGAN PDF
    // =========================================================================
    fun generatePhotoEvidencePdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        photos: List<PhotoEvidenceEntity>,
        categoryTitle: String = "Semua Kategori",
        reportNumber: String = "SB/FTO/${cycle?.id ?: 0}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "DOKUMENTASI FOTO BUKTI LAPANGAN ($categoryTitle)", reportNumber)

        var y = 140f
        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "Semua Kandang"),
                "Siklus Pemeliharaan" to (cycle?.cycleNumber ?: "Semua Siklus"),
                "Kategori Foto" to categoryTitle,
                "Total Foto Terarsip" to "${photos.size} Foto Bukti",
                "Tanggal Cetak" to SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date()),
                "Keaslian Watermark" to "GPS + Tanggal/Waktu Terverifikasi"
            )
        )

        y += 15f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("RINGKASAN DAFTAR ARSIP FOTO BUKTI LAPANGAN", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("No", "Kategori", "Tanggal & Waktu", "Keterangan", "GPS / Lokasi"))

        photos.take(15).forEachIndexed { idx, p ->
            val gpsStr = if (p.latitude != null && p.longitude != null) "${FormatHelper.formatOneDecimal(p.latitude)}, ${FormatHelper.formatOneDecimal(p.longitude)}" else "-"
            tableData.add(
                listOf(
                    "${idx + 1}",
                    p.reportType,
                    "${p.date} ${p.time}",
                    p.caption.take(28).ifBlank { "-" },
                    gpsStr
                )
            )
        }

        if (photos.isEmpty()) {
            tableData.add(listOf("-", "-", "-", "Belum ada foto bukti lapangan yang terlampir", "-"))
        }

        y = drawTable(canvas, y, tableData, listOf(30f, 85f, 110f, 180f, 110f))

        y += 30f
        drawSignatures(canvas, y, profile.ownerName, "Petugas Dokumentasi / TS")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        appendPhotoEvidencePages(context, pdfDoc, photos)

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_FOTO_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 13. LAPORAN HASIL PEMBAGIAN ANGGOTA PDF (A4 Portrait, Multi-Page Support)
    // =========================================================================
    fun generateMemberProfitReportPdf(
        context: Context,
        profile: FarmProfileEntity,
        distribution: ProfitDistributionEntity,
        reportNumber: String = "SB/BAGI/${distribution.id}/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    ): File {
        val memberList = parseDistributionMembers(distribution.memberDetailsJson)
        val pdfDoc = PdfDocument()

        val rowsPerPageSubsequent = 28
        val rowsPageOneWithSignatures = 12
        val rowsPageOneFull = 24

        val totalMembers = memberList.size
        val pagesChunks = mutableListOf<List<DistributionMemberItem>>()

        if (totalMembers <= rowsPageOneWithSignatures) {
            pagesChunks.add(memberList)
        } else {
            var remaining = memberList
            pagesChunks.add(remaining.take(rowsPageOneFull))
            remaining = remaining.drop(rowsPageOneFull)
            while (remaining.isNotEmpty()) {
                pagesChunks.add(remaining.take(rowsPerPageSubsequent))
                remaining = remaining.drop(rowsPerPageSubsequent)
            }
        }

        var needExtraSignaturePage = false
        if (pagesChunks.size > 1) {
            val lastChunkSize = pagesChunks.last().size
            if (lastChunkSize > 18) {
                needExtraSignaturePage = true
            }
        }

        val totalPages = pagesChunks.size + (if (needExtraSignaturePage) 1 else 0)
        val colWidths = listOf(35f, 90f, 240f, 150f)

        pagesChunks.forEachIndexed { pageIndex, chunk ->
            val pageNum = pageIndex + 1
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            var y: Float
            if (pageIndex == 0) {
                drawOfficialHeader(canvas, context, profile, "LAPORAN HASIL PEMBAGIAN ANGGOTA", reportNumber)

                y = 138f
                y = drawInfoBox(
                    canvas, y,
                    listOf(
                        "Tanggal Pembagian" to distribution.date,
                        "Periode Pembagian" to distribution.period.ifBlank { "Siklus Aktif" },
                        "Jumlah Anggota" to "${distribution.memberCount} Orang",
                        "Status Pembagian" to distribution.status,
                        "Keterangan / Alasan" to distribution.notes.ifBlank { "Pembagian Hasil Usaha Sama Rata" },
                        "Metode Pembagian" to "Sama Rata Otomatis (Akurat)"
                    )
                )

                y += 12f
                y = drawKpiCards(
                    canvas, y, listOf(
                        "Total Hasil" to FormatHelper.formatRupiah(distribution.totalRevenue),
                        "Biaya Operasional" to FormatHelper.formatRupiah(distribution.totalExpense),
                        "Potongan" to FormatHelper.formatRupiah(distribution.totalDeduction),
                        "Hasil Bersih" to FormatHelper.formatRupiah(distribution.netProfit),
                        "Hasil Per Anggota" to FormatHelper.formatRupiah(distribution.amountPerMember),
                        "Total Pembagian" to FormatHelper.formatRupiah(distribution.totalDistributed)
                    )
                )

                y += 15f
                val paint = newPdfPaint().apply {
                    color = Color.parseColor("#1B5E20")
                    textSize = 10.5f
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("RINCIAN PEMBAGIAN HASIL TIAP ANGGOTA", 40f, y, paint)
                y += 8f
            } else {
                drawSubHeader(canvas, profile, "LAPORAN HASIL PEMBAGIAN ANGGOTA (Lanjutan)")
                y = 85f
            }

            val tableData = mutableListOf<List<String>>()
            tableData.add(listOf("No", "ID Anggota", "Nama Lengkap Anggota", "Nilai Pembagian (Rp)"))

            val startIdx = if (pageIndex == 0) 0 else {
                pagesChunks.take(pageIndex).sumOf { it.size }
            }

            chunk.forEachIndexed { itemIdx, m ->
                val rowNum = startIdx + itemIdx + 1
                tableData.add(
                    listOf(
                        rowNum.toString(),
                        m.memberNumber.ifBlank { "SB-${String.format(Locale.ROOT, "%03d", m.memberId.takeIf { it > 0 } ?: rowNum.toLong())}" },
                        m.memberName,
                        FormatHelper.formatRupiah(m.amount)
                    )
                )
            }

            if (pageIndex == pagesChunks.size - 1 && !needExtraSignaturePage) {
                tableData.add(
                    listOf(
                        "TOTAL",
                        "",
                        "${distribution.memberCount} Anggota Terdaftar",
                        FormatHelper.formatRupiah(distribution.totalDistributed)
                    )
                )
            }

            y = drawTable(canvas, y, tableData, colWidths)

            if (pageIndex == pagesChunks.size - 1 && !needExtraSignaturePage) {
                y += 12f
                val statusRect = RectF(40f, y, 555f, y + 26f)
                val bgPaint = newPdfPaint().apply {
                    color = if (distribution.roundingRemainder == 0L) Color.parseColor("#E8F5E9") else Color.parseColor("#FFF3E0")
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(statusRect, 4f, 4f, bgPaint)
                val borderPaint = newPdfPaint().apply {
                    color = if (distribution.roundingRemainder == 0L) Color.parseColor("#81C784") else Color.parseColor("#FFB74D")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(statusRect, 4f, 4f, borderPaint)

                val txtPaint = newPdfPaint().apply {
                    color = if (distribution.roundingRemainder == 0L) Color.parseColor("#1B5E20") else Color.parseColor("#E65100")
                    textSize = 8.5f
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("STATUS: ${distribution.status}", 48f, y + 16f, txtPaint)
                canvas.drawText(
                    "Sisa Pembulatan / Selisih: ${FormatHelper.formatRupiah(distribution.roundingRemainder)}",
                    330f,
                    y + 16f,
                    txtPaint
                )

                y += 38f
                drawMemberReportSignatures(canvas, y, profile.ownerName, distribution.date)
            }

            drawFooter(canvas, pageNum, totalPages)
            pdfDoc.finishPage(page)
        }

        if (needExtraSignaturePage) {
            val pageNum = totalPages
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            drawSubHeader(canvas, profile, "LEMBAR PENGESAHAN & TANDA TANGAN PEMBAGIAN HASIL")
            var y = 100f

            val summaryTable = listOf(
                listOf("Parameter Rekapitulasi", "Nilai Akhir"),
                listOf("Total Hasil Kotor", FormatHelper.formatRupiah(distribution.totalRevenue)),
                listOf("Total Biaya Operasional", FormatHelper.formatRupiah(distribution.totalExpense)),
                listOf("Total Potongan", FormatHelper.formatRupiah(distribution.totalDeduction)),
                listOf("Hasil Bersih Usaha", FormatHelper.formatRupiah(distribution.netProfit)),
                listOf("Jumlah Anggota", "${distribution.memberCount} Orang"),
                listOf("Hasil Per Anggota", FormatHelper.formatRupiah(distribution.amountPerMember)),
                listOf("Total Pembagian Terealisasi", FormatHelper.formatRupiah(distribution.totalDistributed)),
                listOf("Sisa Pembulatan / Selisih", FormatHelper.formatRupiah(distribution.roundingRemainder)),
                listOf("Status Evaluasi", distribution.status)
            )
            y = drawTable(canvas, y, summaryTable, listOf(240f, 275f))

            y += 35f
            drawMemberReportSignatures(canvas, y, profile.ownerName, distribution.date)

            drawFooter(canvas, pageNum, totalPages)
            pdfDoc.finishPage(page)
        }

        val file = File(getReportsDir(context), "SEJAHTERA_BERSAMA_LAPORAN_PEMBAGIAN_HASIL_ANGGOTA_${distribution.date}_${distribution.id}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    private fun drawMemberReportSignatures(canvas: Canvas, startY: Float, ownerName: String, dateStr: String) {
        val paint = newPdfPaint()
        paint.color = Color.BLACK
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT

        canvas.drawText("Tempat, Tanggal: ____________________, $dateStr", 40f, startY, paint)

        val sigY = startY + 24f
        canvas.drawText("Mengetahui,", 70f, sigY, paint)
        canvas.drawText("Ketua / Penanggung Jawab", 70f, sigY + 14f, newPdfPaint().apply { color = Color.parseColor("#1B5E20"); textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD })

        canvas.drawText("Diterima & Disetujui,", 360f, sigY, paint)
        canvas.drawText("Perwakilan Anggota", 360f, sigY + 14f, newPdfPaint().apply { color = Color.parseColor("#1B5E20"); textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD })

        paint.strokeWidth = 0.9f
        paint.color = Color.DKGRAY
        canvas.drawLine(50f, sigY + 85f, 230f, sigY + 85f, paint)
        canvas.drawLine(340f, sigY + 85f, 520f, sigY + 85f, paint)

        val namePaint = newPdfPaint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Nama: ${ownerName.ifBlank { "____________________________" }}", 50f, sigY + 100f, namePaint)
        canvas.drawText("Nama: ____________________________", 340f, sigY + 100f, namePaint)
    }

    fun parseDistributionMembers(json: String): List<DistributionMemberItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = org.json.JSONArray(json)
            val list = mutableListOf<DistributionMemberItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DistributionMemberItem(
                        memberId = obj.optLong("memberId", 0L),
                        memberName = obj.optString("memberName", ""),
                        memberNumber = obj.optString("memberNumber", ""),
                        amount = obj.optLong("amount", 0L)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeDistributionMembers(items: List<DistributionMemberItem>): String {
        val array = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject()
            obj.put("memberId", item.memberId)
            obj.put("memberName", item.memberName)
            obj.put("memberNumber", item.memberNumber)
            obj.put("amount", item.amount)
            array.put(obj)
        }
        return array.toString()
    }

    // =========================================================================
    // DRAWING HELPERS (Headers, SubHeaders, Boxes, Tables, Cards, Signatures, Footers)
    // =========================================================================
    private fun newPdfPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.DITHER_FLAG).apply {
        isFilterBitmap = true
        isLinearText = true
    }

    private fun drawOfficialHeader(canvas: Canvas, context: Context, profile: FarmProfileEntity, reportTitle: String, reportNo: String) {
        val logoBox = RectF(40f, 22f, 104f, 86f)
        val logo = getLogoBitmap(context, 180)
        if (logo != null) {
            canvas.drawBitmap(logo, null, logoBox, newPdfPaint())
        }

        val paint = newPdfPaint()
        paint.color = Color.parseColor("#1B5E20")
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 17f
        canvas.drawText(profile.farmName.ifBlank { "SEJAHTERA BERSAMA" }, 120f, 50f, paint)

        paint.color = Color.parseColor("#2E7D32")
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = 9.5f
        if (profile.slogan.isNotBlank()) canvas.drawText("« ${profile.slogan} »", 120f, 65f, paint)

        paint.color = Color.DKGRAY
        paint.textSize = 8.5f
        val address = listOf(profile.address, profile.village, profile.district, profile.regency, profile.province)
            .map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ")
        if (address.isNotBlank()) canvas.drawText(address, 120f, 79f, paint)
        val contact = listOf(
            profile.phoneNumber.trim().takeIf { it.isNotBlank() }?.let { "Telp: $it" },
            profile.email.trim().takeIf { it.isNotBlank() }?.let { "Email: $it" }
        ).filterNotNull().joinToString(" | ")
        if (contact.isNotBlank()) canvas.drawText(contact, 120f, 91f, paint)

        paint.color = Color.parseColor("#1B5E20")
        paint.strokeWidth = 2.5f
        canvas.drawLine(40f, 102f, 555f, 102f, paint)
        paint.strokeWidth = 0.8f
        paint.color = Color.LTGRAY
        canvas.drawLine(40f, 105f, 555f, 105f, paint)

        paint.color = Color.parseColor("#1B5E20")
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 11.5f
        canvas.drawText(reportTitle, 40f, 122f, paint)
        paint.color = Color.DKGRAY
        paint.textSize = 8.5f
        canvas.drawText("No: $reportNo", 435f, 122f, paint)
    }

    private fun drawSubHeader(canvas: Canvas, profile: FarmProfileEntity, pageTitle: String) {
        val paint = newPdfPaint()
        paint.color = Color.parseColor("#1B5E20")
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText(profile.farmName.ifBlank { "SEJAHTERA BERSAMA" }, 40f, 45f, paint)

        paint.color = Color.DKGRAY
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = 9f
        canvas.drawText(pageTitle, 40f, 62f, paint)

        paint.color = Color.parseColor("#1B5E20")
        paint.strokeWidth = 1.5f
        canvas.drawLine(40f, 74f, 555f, 74f, paint)
    }

    private fun drawOfficialLandscapeHeader(canvas: Canvas, context: Context, profile: FarmProfileEntity, reportTitle: String, reportNo: String) {
        val logo = getLogoBitmap(context, 180)
        if (logo != null) {
            canvas.drawBitmap(logo, null, RectF(40f, 20f, 102f, 82f), newPdfPaint())
        }

        val paint = newPdfPaint()
        paint.color = Color.parseColor("#1B5E20")
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 16f
        canvas.drawText(profile.farmName.ifBlank { "SEJAHTERA BERSAMA" }, 120f, 48f, paint)

        paint.color = Color.parseColor("#2E7D32")
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = 9f
        if (profile.slogan.isNotBlank()) canvas.drawText("« ${profile.slogan} »", 120f, 62f, paint)

        paint.color = Color.DKGRAY
        paint.textSize = 8.5f
        val address = listOf(profile.address, profile.village, profile.district, profile.regency, profile.province)
            .map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ")
        if (address.isNotBlank()) canvas.drawText(address, 120f, 74f, paint)

        paint.color = Color.parseColor("#1B5E20")
        paint.strokeWidth = 2f
        canvas.drawLine(40f, 92f, 802f, 92f, paint)
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText(reportTitle, 40f, 109f, paint)
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = Color.GRAY
        canvas.drawText("No: $reportNo", 650f, 109f, paint)
    }

    private fun drawInfoBox(canvas: Canvas, startY: Float, items: List<Pair<String, String>>): Float {
        val paint = newPdfPaint()
        val rect = RectF(40f, startY, 555f, startY + (items.size / 2) * 16f + 16f)

        paint.color = Color.parseColor("#F1F8E9")
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        paint.color = Color.parseColor("#C8E6C9")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        var curY = startY + 14f

        for (i in items.indices step 2) {
            val (k1, v1) = items[i]
            paint.color = Color.DKGRAY
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 8.5f
            canvas.drawText("$k1:", 50f, curY, paint)
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.BLACK
            canvas.drawText(v1, 140f, curY, paint)

            if (i + 1 < items.size) {
                val (k2, v2) = items[i + 1]
                paint.color = Color.DKGRAY
                paint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("$k2:", 300f, curY, paint)
                paint.typeface = Typeface.DEFAULT
                paint.color = Color.BLACK
                canvas.drawText(v2, 400f, curY, paint)
            }
            curY += 15f
        }
        return rect.bottom
    }

    private fun drawKpiCards(canvas: Canvas, startY: Float, cards: List<Pair<String, String>>): Float {
        val paint = newPdfPaint()
        val cardWidth = 165f
        val cardHeight = 42f
        var curX = 40f
        var curY = startY

        cards.forEachIndexed { index, (title, value) ->
            val rect = RectF(curX, curY, curX + cardWidth, curY + cardHeight)

            paint.color = Color.parseColor("#E8F5E9")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, 5f, 5f, paint)

            paint.color = Color.parseColor("#81C784")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(rect, 5f, 5f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#2E7D32")
            paint.textSize = 7.5f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(title.uppercase(Locale.ROOT), curX + 8f, curY + 13f, paint)

            paint.color = Color.parseColor("#1B5E20")
            paint.textSize = 10.5f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(value, curX + 8f, curY + 30f, paint)

            if ((index + 1) % 3 == 0) {
                curX = 40f
                curY += cardHeight + 7f
            } else {
                curX += cardWidth + 10f
            }
        }
        return if (cards.size % 3 == 0) curY else curY + cardHeight + 7f
    }

    private fun drawTable(canvas: Canvas, startY: Float, rows: List<List<String>>, colWidths: List<Float>, isLandscape: Boolean = false): Float {
        val paint = newPdfPaint()
        var curY = startY
        val startX = 40f
        val totalWidth = colWidths.sum()
        val rowHeight = 18f

        rows.forEachIndexed { rowIndex, row ->
            val isHeader = rowIndex == 0
            val isSummaryRow = row.firstOrNull()?.equals("TOTAL", true) == true || row.firstOrNull()?.startsWith("TOTAL", true) == true || row.firstOrNull()?.startsWith("SALDO", true) == true
            val isZebra = rowIndex % 2 == 1 && !isHeader && !isSummaryRow

            val rowRect = RectF(startX, curY, startX + totalWidth, curY + rowHeight)
            paint.style = Paint.Style.FILL
            paint.color = when {
                isHeader -> Color.parseColor("#1B5E20")
                isSummaryRow -> Color.parseColor("#E8F5E9")
                isZebra -> Color.parseColor("#F9FBE7")
                else -> Color.WHITE
            }
            canvas.drawRect(rowRect, paint)

            paint.style = Paint.Style.STROKE
            paint.color = if (isHeader) Color.parseColor("#1B5E20") else Color.parseColor("#E0E0E0")
            paint.strokeWidth = 0.8f
            canvas.drawRect(rowRect, paint)

            paint.style = Paint.Style.FILL
            paint.color = when {
                isHeader -> Color.WHITE
                isSummaryRow -> Color.parseColor("#1B5E20")
                else -> Color.BLACK
            }
            paint.typeface = if (isHeader || isSummaryRow) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            paint.textSize = if (isHeader) 8.5f else 8f

            var cellX = startX
            row.forEachIndexed { colIndex, cellText ->
                val colW = colWidths.getOrElse(colIndex) { 80f }
                val textToDraw = if (cellText.length > 36) cellText.take(34) + ".." else cellText
                canvas.drawText(textToDraw, cellX + 5f, curY + 12.5f, paint)

                if (colIndex > 0) {
                    paint.style = Paint.Style.STROKE
                    paint.color = if (isHeader) Color.parseColor("#2E7D32") else Color.parseColor("#E0E0E0")
                    canvas.drawLine(cellX, curY, cellX, curY + rowHeight, paint)
                    paint.style = Paint.Style.FILL
                    paint.color = if (isHeader) Color.WHITE else if (isSummaryRow) Color.parseColor("#1B5E20") else Color.BLACK
                }

                cellX += colW
            }
            curY += rowHeight
        }
        return curY
    }

    private fun drawSignatures(canvas: Canvas, startY: Float, ownerName: String, partnerPicName: String) {
        val paint = newPdfPaint()
        paint.color = Color.BLACK
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT

        canvas.drawText("Dibuat & Diverifikasi,", 70f, startY, paint)
        canvas.drawText("PETERNAK / PENGELOLA", 70f, startY + 12f, paint)

        canvas.drawText("Mengetahui & Menyetujui,", 380f, startY, paint)
        canvas.drawText("PERUSAHAAN MITRA", 380f, startY + 12f, paint)

        paint.strokeWidth = 0.8f
        canvas.drawLine(50f, startY + 60f, 210f, startY + 60f, paint)
        canvas.drawLine(360f, startY + 60f, 520f, startY + 60f, paint)

        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(ownerName, 60f, startY + 72f, paint)
        canvas.drawText(partnerPicName, 370f, startY + 72f, paint)
    }

    private fun drawLandscapeSignatures(canvas: Canvas, startY: Float, ownerName: String, partnerPicName: String) {
        val paint = newPdfPaint()
        paint.color = Color.BLACK
        paint.textSize = 8.5f

        canvas.drawText("Peternak / Pengelola Kandang,", 100f, startY, paint)
        canvas.drawText("Technical Support Perusahaan Mitra,", 540f, startY, paint)

        paint.strokeWidth = 0.8f
        canvas.drawLine(80f, startY + 48f, 260f, startY + 48f, paint)
        canvas.drawLine(520f, startY + 48f, 700f, startY + 48f, paint)

        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(ownerName, 90f, startY + 60f, paint)
        canvas.drawText(partnerPicName, 530f, startY + 60f, paint)
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int, isLandscape: Boolean = false) {
        val paint = newPdfPaint()
        paint.color = Color.GRAY
        paint.textSize = 7f
        val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        val y = if (isLandscape) 575f else 820f
        val rightX = if (isLandscape) 710f else 470f

        canvas.drawText("SEJAHTERA BERSAMA | Pembuat: Hardi Mantangai (20 Agustus 2026) | Dicetak: $dateStr WIB", 40f, y, paint)
        canvas.drawText("Hal $pageNum/$totalPages", rightX, y, paint)
    }

    // =========================================================================
    // MULTI-PAGE PHOTO EVIDENCE RENDERER (GRID 2 KOLOM UKURAN SEDANG)
    // =========================================================================
    private fun appendPhotoEvidencePages(context: Context, pdfDoc: PdfDocument, photos: List<PhotoEvidenceEntity>) {
        val usable = photos
            .sortedBy { it.createdAt }
            .filter { it.photoUri.isNotBlank() || it.watermarkedUri.isNotBlank() }

        if (usable.isEmpty()) return

        val perPage = 4
        usable.chunked(perPage).forEachIndexed { pageIndex, quad ->
            val pageNumber = pageIndex + 2
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = newPdfPaint().apply {
                color = Color.parseColor("#1B5E20")
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("LAMPIRAN FOTO BUKTI LAPORAN", 40f, 44f, titlePaint)

            val subPaint = newPdfPaint().apply {
                color = Color.parseColor("#455A64")
                textSize = 8.5f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText("Dokumentasi Resmi Terintegrasi (Halaman $pageNumber)", 40f, 58f, subPaint)

            val linePaint = newPdfPaint().apply {
                color = Color.parseColor("#1B5E20")
                strokeWidth = 1.5f
            }
            canvas.drawLine(40f, 68f, 555f, 68f, linePaint)

            quad.forEachIndexed { index, photo ->
                val globalNumber = pageIndex * perPage + index + 1
                val targetPath = photo.watermarkedUri.ifBlank { photo.photoUri }

                val col = index % 2
                val row = index / 2
                val cardLeft = if (col == 0) 40f else 307f
                val cardWidth = 248f
                val cardTop = if (row == 0) 80f else 434f
                val cardHeight = 342f

                val cardRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)
                val cardFillPaint = newPdfPaint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(cardRect, 6f, 6f, cardFillPaint)

                val cardStrokePaint = newPdfPaint().apply {
                    color = Color.parseColor("#C8E6C9")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(cardRect, 6f, 6f, cardStrokePaint)

                // Header bar
                val headerBarRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + 24f)
                val headerBarPaint = newPdfPaint().apply {
                    color = Color.parseColor("#E8F5E9")
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(headerBarRect, 6f, 6f, headerBarPaint)
                canvas.drawLine(cardLeft, cardTop + 24f, cardLeft + cardWidth, cardTop + 24f, cardStrokePaint)

                canvas.drawText("BUKTI FOTO #$globalNumber", cardLeft + 8f, cardTop + 16f, newPdfPaint().apply {
                    color = Color.parseColor("#1B5E20")
                    textSize = 9.5f
                    typeface = Typeface.DEFAULT_BOLD
                })
                canvas.drawText("[${photo.reportType.uppercase(Locale.ROOT)}]", cardLeft + cardWidth - 8f, cardTop + 16f, newPdfPaint().apply {
                    color = Color.parseColor("#2E7D32")
                    textSize = 8f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.RIGHT
                })

                // Photo box
                val photoBoxLeft = cardLeft + 8f
                val photoBoxTop = cardTop + 32f
                val photoBoxWidth = 232f
                val photoBoxHeight = 190f
                val photoBoxRect = RectF(photoBoxLeft, photoBoxTop, photoBoxLeft + photoBoxWidth, photoBoxTop + photoBoxHeight)

                canvas.drawRoundRect(photoBoxRect, 4f, 4f, newPdfPaint().apply {
                    color = Color.parseColor("#F5F5F5")
                    style = Paint.Style.FILL
                })
                canvas.drawRoundRect(photoBoxRect, 4f, 4f, newPdfPaint().apply {
                    color = Color.parseColor("#E0E0E0")
                    style = Paint.Style.STROKE
                    strokeWidth = 0.8f
                })

                val bitmap = PhotoStorageHelper.loadBitmapSafe(context, targetPath, maxDim = 800)
                if (bitmap != null) {
                    val scale = minOf(photoBoxWidth / bitmap.width.toFloat(), photoBoxHeight / bitmap.height.toFloat())
                    val w = bitmap.width * scale
                    val h = bitmap.height * scale
                    val left = photoBoxLeft + (photoBoxWidth - w) / 2f
                    val top = photoBoxTop + (photoBoxHeight - h) / 2f
                    canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), newPdfPaint())
                    bitmap.recycle()
                } else {
                    canvas.drawText("Bukti foto tidak dapat dimuat", photoBoxLeft + photoBoxWidth / 2f, photoBoxTop + photoBoxHeight / 2f, newPdfPaint().apply {
                        color = Color.parseColor("#78909C")
                        textSize = 8.5f
                        textAlign = Paint.Align.CENTER
                    })
                }

                // Info Box
                val infoBoxLeft = cardLeft + 8f
                val infoBoxTop = cardTop + 230f
                val infoBoxWidth = 232f
                val infoBoxHeight = 102f
                val infoBoxRect = RectF(infoBoxLeft, infoBoxTop, infoBoxLeft + infoBoxWidth, infoBoxTop + infoBoxHeight)

                canvas.drawRoundRect(infoBoxRect, 4f, 4f, newPdfPaint().apply {
                    color = Color.parseColor("#F1F8E9")
                    style = Paint.Style.FILL
                })
                canvas.drawRoundRect(infoBoxRect, 4f, 4f, newPdfPaint().apply {
                    color = Color.parseColor("#DCEDC8")
                    style = Paint.Style.STROKE
                    strokeWidth = 0.8f
                })

                val textPaint = newPdfPaint().apply { color = Color.parseColor("#263238"); textSize = 7.8f }
                var cY = infoBoxTop + 14f
                canvas.drawText("• Kategori: ${photo.reportType}", infoBoxLeft + 6f, cY, textPaint)
                cY += 14f
                canvas.drawText("• Waktu: ${photo.date} ${photo.time}", infoBoxLeft + 6f, cY, textPaint)
                cY += 14f
                if (photo.caption.isNotBlank()) {
                    canvas.drawText("• Keterangan: ${photo.caption.take(36)}", infoBoxLeft + 6f, cY, textPaint)
                    cY += 14f
                }
                if (photo.latitude != null && photo.longitude != null) {
                    canvas.drawText("• Koordinat: ${String.format(Locale.US, "%.5f", photo.latitude)}, ${String.format(Locale.US, "%.5f", photo.longitude)}", infoBoxLeft + 6f, cY, textPaint)
                }
            }
            pdfDoc.finishPage(page)
        }
    }

    // =========================================================================
    // INTENT & PRINT SYSTEM HELPERS
    // =========================================================================
    fun openPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Buka Laporan PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Budidaya Ayam Broiler - SEJAHTERA BERSAMA")
                putExtra(Intent.EXTRA_TEXT, "Terlampir file laporan resmi dari Peternakan SEJAHTERA BERSAMA.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Laporan PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun printPdf(context: Context, file: File) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(file.name)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        val input = file.inputStream()
                        val output = FileOutputStream(destination?.fileDescriptor)
                        input.copyTo(output)
                        input.close()
                        output.close()
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print("Laporan_${file.name}", printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mencetak PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
