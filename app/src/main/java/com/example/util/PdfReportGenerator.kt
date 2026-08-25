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

/**
 * Generator Laporan PDF Resmi SEJAHTERA BERSAMA
 * Memenuhi standar dokumen A4, integrasi foto bukti beresolusi proporsional,
 * format mata uang Rupiah Indonesia (Rp 500.000), serta generator independen untuk setiap fitur.
 */
object PdfReportGenerator {

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
    // 1. LAPORAN HARIAN KANDANG PDF
    // =========================================================================
    fun generateDailyReportPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        partner: PartnerEntity?,
        cycle: CycleEntity,
        dailyLog: DailyLogEntity,
        mortalities: List<MortalityLogEntity> = emptyList(),
        feedRecords: List<FeedStockEntity> = emptyList(),
        medicines: List<MedicineEntity> = emptyList(),
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/LPH/${dailyLog.ageDays}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Portrait
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN HARIAN OPERASIONAL KANDANG", reportNumber)

        var y = 140f
        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Perusahaan Mitra" to (partner?.companyName ?: "-"),
                "Nomor Siklus" to cycle.cycleNumber,
                "Tanggal Chick-In" to cycle.chickInDate,
                "Tanggal Laporan" to dailyLog.date,
                "Umur Ayam" to FormatHelper.formatHari(dailyLog.ageDays),
                "Populasi Awal" to FormatHelper.formatEkor(cycle.docCount),
                "Kondisi Cuaca" to "${dailyLog.weather} (${dailyLog.tempCelsius}°C, ${dailyLog.humidityPercent}%)"
            )
        )

        y += 15f
        val calculatedCurrentPop = dailyLog.afternoonPopulation
        y = drawKpiCards(
            canvas, y, listOf(
                "Populasi Sore" to FormatHelper.formatEkor(calculatedCurrentPop),
                "Kematian Hari Ini" to FormatHelper.formatEkor(dailyLog.deadCount),
                "Afkir Hari Ini" to FormatHelper.formatEkor(dailyLog.cullCount),
                "Pakan Diberikan" to FormatHelper.formatKg(dailyLog.feedGivenKg),
                "Air Minum" to FormatHelper.formatLiter(dailyLog.waterIntakeLiters),
                "Kondisi Ayam" to dailyLog.chickenCondition
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("RINCIAN OPERASIONAL & KESEHATAN", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("Parameter", "Keterangan / Nilai"))
        tableData.add(listOf("Kondisi Sekam / Litter", dailyLog.litterCondition))
        tableData.add(listOf("Obat Diberikan", dailyLog.medicineGiven.ifEmpty { "-" }))
        tableData.add(listOf("Vitamin Diberikan", dailyLog.vitaminGiven.ifEmpty { "-" }))
        tableData.add(listOf("Vaksin Diberikan", dailyLog.vaccineGiven.ifEmpty { "-" }))
        tableData.add(listOf("Sisa Pakan Tempat Pakan", FormatHelper.formatKg(dailyLog.feedRemainingKg)))
        tableData.add(listOf("Catatan / Kejadian Penting", dailyLog.notes.ifEmpty { "Kondisi kandang normal dan terkendali." }))

        y = drawTable(canvas, y, tableData, listOf(180f, 335f))

        y += 30f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "Perusahaan Mitra")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Filter foto harian atau gabungan foto terkait
        val targetPhotos = if (photos.isNotEmpty()) photos else {
            val list = mutableListOf<PhotoEvidenceEntity>()
            if (dailyLog.photoUri.isNotBlank()) {
                list.add(PhotoEvidenceEntity(photoUri = dailyLog.photoUri, reportType = "Harian", date = dailyLog.date, time = "Operasional", caption = "Bukti Foto Operasional Harian"))
            }
            list
        }
        appendPhotoEvidencePages(context, pdfDoc, targetPhotos)

        val file = File(getReportsDir(context), "Laporan_Harian_Hari_${dailyLog.ageDays}_${dailyLog.date}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 2. LAPORAN DATA KANDANG PDF
    // =========================================================================
    fun generateCoopPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/KND/${coop.id}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
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
                "Kode Kandang" to coop.code,
                "Tipe Kandang" to (coop.coopType.ifBlank { "Closed House" }),
                "Kapasitas Tampung" to FormatHelper.formatEkor(coop.capacity),
                "Dimensi (P x L)" to "${FormatHelper.formatMeter(coop.lengthM)} x ${FormatHelper.formatMeter(coop.widthM)}",
                "Luas Efektif" to "${FormatHelper.formatOneDecimal(luasM2)} m² (Kepadatan ${FormatHelper.formatOneDecimal(density)} ekor/m²)",
                "Alamat Lengkap" to (coop.address.ifBlank { "-" }),
                "Desa / Kecamatan" to "${coop.village.ifBlank { "-" }} / ${coop.district.ifBlank { "-" }}",
                "Kabupaten / Provinsi" to "${coop.regency.ifBlank { "-" }}, ${coop.province.ifBlank { "-" }}",
                "Koordinat GPS" to if (coop.latitude != null && coop.longitude != null) "${coop.latitude}, ${coop.longitude}" else "Belum direkam"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Kapasitas" to FormatHelper.formatEkor(coop.capacity),
                "Luas Total" to "${FormatHelper.formatOneDecimal(luasM2)} m²",
                "Tipe Bangunan" to coop.coopType.ifBlank { "Closed House" },
                "Panjang" to FormatHelper.formatMeter(coop.lengthM),
                "Lebar" to FormatHelper.formatMeter(coop.widthM),
                "Pemilik" to coop.ownerName.ifBlank { profile.ownerName }
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("RINGKASAN INFRASTRUKTUR & BIOSEKURITI", 40f, y, paint)
        y += 10f

        val tableData = listOf(
            listOf("Komponen Fasilitas", "Standar Teknis / Keterangan"),
            listOf("Sistem Ventilasi", "Exhaust Fan, Inlets, Cooling Pad / Tunnel Ventilation"),
            listOf("Sistem Pemanas", "Gasolec / Heater Otomatis dengan pre-heating 24-48 jam"),
            listOf("Sistem Tempat Minum", "Nipple drinker otomatis dengan regulator tekanan & flushing"),
            listOf("Sistem Tempat Pakan", "Feeder pan otomatis / manual dengan pengaturan ketinggian"),
            listOf("Zona Biosekuriti", "Gerbang disinfeksi kendaraan, Footbath 3 zona, Pembatasan tamu"),
            listOf("Catatan Kandang", coop.notes.ifBlank { "Kondisi fisik kandang memenuhi standar budidaya modern." })
        )

        y = drawTable(canvas, y, tableData, listOf(180f, 335f))

        y += 30f
        drawSignatures(canvas, y, profile.ownerName, "Kepala Kandang / TS")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Foto bukti kandang
        val coopPhotos = mutableListOf<PhotoEvidenceEntity>()
        if (coop.photoUri.isNotBlank()) {
            coopPhotos.add(
                PhotoEvidenceEntity(
                    photoUri = coop.photoUri,
                    reportType = "Kandang",
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    caption = "Foto Dokumentasi Fisik Kandang: ${coop.name}",
                    latitude = coop.latitude,
                    longitude = coop.longitude
                )
            )
        }
        coopPhotos.addAll(photos.filter { it.reportType.equals("Kandang", ignoreCase = true) || it.reportType.equals("COOP", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, coopPhotos)

        val file = File(getReportsDir(context), "Laporan_Kandang_${coop.name.replace(" ", "_")}_${coop.id}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 3. LAPORAN KEUANGAN KANDANG PDF
    // =========================================================================
    fun generateExpensePdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        expenses: List<ExpenseEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/KEU/${cycle?.id ?: 0}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN KEUANGAN & BIAYA OPERASIONAL", reportNumber)

        var y = 140f
        val totalIn = expenses.filter { it.transactionType == "IN" }.sumOf { it.totalAmount }
        val totalOut = expenses.filter { it.transactionType == "OUT" }.sumOf { it.totalAmount }
        val netBalance = totalIn - totalOut

        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "Semua Kandang"),
                "Siklus Pemeliharaan" to (cycle?.cycleNumber ?: "Semua Siklus"),
                "Total Pemasukan" to FormatHelper.formatRupiah(totalIn),
                "Total Pengeluaran" to FormatHelper.formatRupiah(totalOut),
                "Saldo Operasional" to FormatHelper.formatRupiah(netBalance),
                "Jumlah Transaksi" to "${expenses.size} Catatan Keuangan",
                "Periode Laporan" to SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date()),
                "Status Keuangan" to if (netBalance >= 0) "SURPLUS KAS" else "DEFISIT KAS"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Total Pengeluaran" to FormatHelper.formatRupiah(totalOut),
                "Total Pemasukan" to FormatHelper.formatRupiah(totalIn),
                "Saldo Bersih" to FormatHelper.formatRupiah(netBalance),
                "Biaya Operasional" to FormatHelper.formatRupiah(expenses.filter { it.category.contains("Operasional", true) || it.category.contains("Perawatan", true) }.sumOf { it.totalAmount }),
                "Biaya Sekam/Gas/Listrik" to FormatHelper.formatRupiah(expenses.filter { it.category.contains("Sekam", true) || it.category.contains("Pemanas", true) || it.category.contains("Listrik", true) }.sumOf { it.totalAmount }),
                "Biaya Lain-lain" to FormatHelper.formatRupiah(expenses.filter { it.category.contains("Lain", true) }.sumOf { it.totalAmount })
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("BUKU KAS & CATATAN BIAYA PEMBELIAN", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("Tgl", "Tipe", "Kategori", "Item / Keterangan", "Nominal (Rp)"))

        expenses.sortedByDescending { it.date }.forEach { item ->
            val noteText = if (item.notes.isNotBlank()) " (${item.notes.take(15)})" else ""
            tableData.add(
                listOf(
                    item.date,
                    if (item.transactionType == "IN") "MASUK" else "KELUAR",
                    item.category,
                    (item.expenseName.take(25) + noteText),
                    FormatHelper.formatRupiah(item.totalAmount)
                )
            )
        }

        if (expenses.isEmpty()) {
            tableData.add(listOf("-", "-", "-", "Belum ada data transaksi keuangan tercatat", "Rp 0"))
        }

        y = drawTable(canvas, y, tableData, listOf(75f, 60f, 100f, 160f, 120f))

        y += 30f
        drawSignatures(canvas, y, profile.ownerName, "Admin Keuangan / Verifikator")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Foto bukti nota / kuitansi
        val expensePhotos = mutableListOf<PhotoEvidenceEntity>()
        expenses.forEach { exp ->
            if (exp.photoUri.isNotBlank()) {
                expensePhotos.add(
                    PhotoEvidenceEntity(
                        photoUri = exp.photoUri,
                        reportType = "Keuangan",
                        date = exp.date,
                        time = "Kas",
                        caption = "Nota/Kuitansi: ${exp.expenseName} (${FormatHelper.formatRupiah(exp.totalAmount)})"
                    )
                )
            }
        }
        expensePhotos.addAll(photos.filter { it.reportType.equals("Keuangan", ignoreCase = true) || it.reportType.equals("EXPENSE", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, expensePhotos)

        val file = File(getReportsDir(context), "Laporan_Keuangan_Kandang_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 4. LAPORAN MORTALITAS & AFKIR PDF
    // =========================================================================
    fun generateMortalityPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        mortalities: List<MortalityLogEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/MRT/${cycle?.id ?: 0}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN MORTALITAS & AFKIR AYAM BROILER", reportNumber)

        var y = 140f
        val docCount = cycle?.docCount ?: 1
        val totalDead = mortalities.sumOf { it.count }
        val totalLoss = totalDead
        val mortRate = (totalDead.toDouble() / docCount) * 100.0

        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Nomor Siklus" to (cycle?.cycleNumber ?: "-"),
                "DOC Masuk" to FormatHelper.formatEkor(docCount),
                "Tanggal Chick-In" to (cycle?.chickInDate ?: "-"),
                "Total Kematian" to FormatHelper.formatEkor(totalDead),
                "Mortalitas Kumulatif" to FormatHelper.formatPersen(mortRate),
                "Jumlah Kejadian" to "${mortalities.size} Catatan Kematian",
                "Status Deplesi" to if (mortRate <= 3.0) "STANDAR BAIK (<3%)" else "PERLU EVALUASI (>3%)"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Total Kematian" to FormatHelper.formatEkor(totalDead),
                "Mortalitas Kumulatif" to FormatHelper.formatPersen(mortRate),
                "Survival Rate" to FormatHelper.formatPersen(100.0 - mortRate),
                "Penyebab Dominan" to (mortalities.groupBy { it.cause }.maxByOrNull { it.value.size }?.key ?: "-"),
                "DOC Awal" to FormatHelper.formatEkor(docCount),
                "Sisa Hidup" to FormatHelper.formatEkor(docCount - totalDead)
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("LOG HARIAN KEMATIAN & INDIKASI GEJALA", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("Tgl", "Umur", "Mati", "Lokasi/Blok", "Penyebab / Gejala Klinis", "Tindakan / Solusi"))

        mortalities.sortedBy { it.date }.forEach { m ->
            tableData.add(
                listOf(
                    m.date,
                    FormatHelper.formatHari(m.ageDays),
                    FormatHelper.formatInteger(m.count),
                    m.locationBlock.ifBlank { "Semua Blok" },
                    m.cause.take(24),
                    m.notes.ifBlank { "Pemusnahan & desinfeksi litter" }.take(30)
                )
            )
        }

        if (mortalities.isEmpty()) {
            tableData.add(listOf("-", "-", "0", "-", "Tidak ada kejadian kematian ayam", "-"))
        }

        y = drawTable(canvas, y, tableData, listOf(75f, 55f, 50f, 65f, 125f, 145f))

        y += 30f
        drawSignatures(canvas, y, profile.ownerName, "Dokter Hewan / Tim Kesehatan")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Foto bukti kematian / bedah bangkai
        val mortPhotos = mutableListOf<PhotoEvidenceEntity>()
        mortalities.forEach { m ->
            if (m.photoUri.isNotBlank()) {
                mortPhotos.add(
                    PhotoEvidenceEntity(
                        photoUri = m.photoUri,
                        reportType = "Mortalitas",
                        date = m.date,
                        time = "Kandang",
                        caption = "Bukti Kematian: Hari ke-${m.ageDays}, Mati: ${m.count} ekor, Gejala: ${m.cause}"
                    )
                )
            }
        }
        mortPhotos.addAll(photos.filter { it.reportType.equals("Mortalitas", ignoreCase = true) || it.reportType.equals("MORTALITY", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, mortPhotos)

        val file = File(getReportsDir(context), "Laporan_Mortalitas_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 5. LAPORAN PENERIMAAN & PENGGUNAAN PAKAN PDF
    // =========================================================================
    fun generateFeedPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        feedStocks: List<FeedStockEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/PKN/${cycle?.id ?: 0}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN PENERIMAAN & PENGGUNAAN PAKAN", reportNumber)

        var y = 140f
        val totalInKg = feedStocks.filter { it.movementType == "IN" }.sumOf { it.totalKg }
        val totalOutKg = feedStocks.filter { it.movementType == "OUT" }.sumOf { it.totalKg }
        val totalInBags = feedStocks.filter { it.movementType == "IN" }.sumOf { it.bags }
        val totalOutBags = feedStocks.filter { it.movementType == "OUT" }.sumOf { it.bags }
        val remainingKg = totalInKg - totalOutKg
        val remainingBags = totalInBags - totalOutBags

        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Nomor Siklus" to (cycle?.cycleNumber ?: "-"),
                "Total Pakan Masuk (DO)" to "${FormatHelper.formatKg(totalInKg)} (${FormatHelper.formatSak(totalInBags.toInt())})",
                "Total Pakan Terpakai" to "${FormatHelper.formatKg(totalOutKg)} (${FormatHelper.formatSak(totalOutBags.toInt())})",
                "Sisa Stok Gudang" to "${FormatHelper.formatKg(remainingKg)} (${FormatHelper.formatSak(remainingBags.toInt())})",
                "Target FCR Siklus" to "${cycle?.targetFcr ?: 1.40}",
                "Tanggal Cetak" to SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date()),
                "Status Gudang" to if (remainingKg >= 500) "STOK AMAN" else "PERLU RE-ORDER"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Pakan Masuk" to FormatHelper.formatKg(totalInKg),
                "Pakan Keluar" to FormatHelper.formatKg(totalOutKg),
                "Sisa Stok" to FormatHelper.formatKg(remainingKg),
                "Sak Masuk" to FormatHelper.formatSak(totalInBags.toInt()),
                "Sak Keluar" to FormatHelper.formatSak(totalOutBags.toInt()),
                "Sisa Sak" to FormatHelper.formatSak(remainingBags.toInt())
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("LOG SURAT JALAN & PENGELUARAN PAKAN HARIAN", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("Tgl", "Tipe", "Jenis / Merk", "Sak", "Total (Kg)", "No Surat Jalan (DO) / Catatan"))

        feedStocks.sortedBy { it.date }.forEach { f ->
            val doText = if (f.doNumber.isNotBlank()) "DO: ${f.doNumber}" else f.notes.ifBlank { "-" }
            tableData.add(
                listOf(
                    f.date,
                    if (f.movementType == "IN") "MASUK (DO)" else "KELUAR (PAKAN)",
                    f.feedType,
                    "${FormatHelper.formatNumber(f.bags)} Sak",
                    FormatHelper.formatKg(f.totalKg),
                    doText
                )
            )
        }

        if (feedStocks.isEmpty()) {
            tableData.add(listOf("-", "-", "-", "0 Sak", "0 Kg", "Belum ada catatan mutasi pakan"))
        }

        y = drawTable(canvas, y, tableData, listOf(75f, 90f, 100f, 55f, 75f, 120f))

        y += 30f
        drawSignatures(canvas, y, profile.ownerName, "Kepala Gudang / Pengawas Pakan")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Foto bukti pakan / DO surat jalan
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

        val file = File(getReportsDir(context), "Laporan_Pakan_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 6. LAPORAN OBAT, VAKSIN & VITAMIN (OVK) PDF
    // =========================================================================
    fun generateMedicinePdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        medicines: List<MedicineEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/OVK/${cycle?.id ?: 0}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN PENGGUNAAN OBAT, VAKSIN & VITAMIN", reportNumber)

        var y = 140f
        val totalMed = medicines.count { it.category.equals("Obat", true) }
        val totalVac = medicines.count { it.category.equals("Vaksin", true) }
        val totalVit = medicines.count { it.category.equals("Vitamin", true) }

        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Nomor Siklus" to (cycle?.cycleNumber ?: "-"),
                "Total Vaksinasi" to "$totalVac Aplikasi",
                "Total Vitamin" to "$totalVit Aplikasi",
                "Total Pengobatan" to "$totalMed Aplikasi",
                "Total Record OVK" to "${medicines.size} Tindakan",
                "Tanggal Cetak" to SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date()),
                "Kepatuhan Prokes" to "Sesuai Standar Kemitraan"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Vaksin Diberikan" to "$totalVac Kali",
                "Vitamin Diberikan" to "$totalVit Kali",
                "Obat Diberikan" to "$totalMed Kali",
                "Total OVK" to "${medicines.size} Kali",
                "Status Kandang" to "Terkontrol Sehat",
                "Biosekuriti" to "Level 3 Aktif"
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("LOG PEMBERIAN OBAT, VAKSIN & SUPLEMEN", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("Tgl", "Umur", "Kategori", "Nama Produk / Merk", "Dosis / Aturan", "Aplikasi / Tujuan"))

        medicines.sortedBy { it.date }.forEach { med ->
            val purposeText = if (med.purpose.isNotBlank()) " (${med.purpose})" else ""
            tableData.add(
                listOf(
                    med.date,
                    FormatHelper.formatHari(med.ageDays),
                    med.category.uppercase(Locale.ROOT),
                    med.productName,
                    med.dose.ifBlank { "-" },
                    ("${med.method}$purposeText").take(30)
                )
            )
        }

        if (medicines.isEmpty()) {
            tableData.add(listOf("-", "-", "-", "Belum ada tindakan pemberian obat/vaksin", "-", "-"))
        }

        y = drawTable(canvas, y, tableData, listOf(70f, 50f, 65f, 120f, 100f, 110f))

        y += 30f
        drawSignatures(canvas, y, profile.ownerName, "Technical Service / Paramedis")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Foto bukti OVK / kemasan obat
        val medPhotos = mutableListOf<PhotoEvidenceEntity>()
        medicines.forEach { med ->
            if (med.photoUri.isNotBlank()) {
                medPhotos.add(
                    PhotoEvidenceEntity(
                        photoUri = med.photoUri,
                        reportType = "Medis",
                        date = med.date,
                        time = "Kandang",
                        caption = "Bukti OVK: [${med.category}] ${med.productName}, Dosis: ${med.dose}"
                    )
                )
            }
        }
        medPhotos.addAll(photos.filter { it.reportType.equals("Medis", ignoreCase = true) || it.reportType.equals("MEDICINE", ignoreCase = true) || it.reportType.equals("Obat", ignoreCase = true) || it.reportType.equals("Vaksin", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, medPhotos)

        val file = File(getReportsDir(context), "Laporan_Obat_Vaksin_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 7. LAPORAN SAMPLING PENIMBANGAN BOBOT AYAM PDF
    // =========================================================================
    fun generateWeightPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity?,
        weights: List<WeightSampleEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/BBT/${cycle?.id ?: 0}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
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
        val targetGram = targetWeightKg * 1000.0

        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Nomor Siklus" to (cycle?.cycleNumber ?: "-"),
                "Umur Sampling Terkini" to FormatHelper.formatHari(latestWeight?.ageDays ?: 0),
                "Rata-rata Bobot Terkini" to FormatHelper.formatGram(avgGram),
                "Target Bobot Panen" to "${FormatHelper.formatTwoDecimals(targetWeightKg)} Kg",
                "Jumlah Sesi Sampling" to "${weights.size} Kali Sampling",
                "Tanggal Cetak" to SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date()),
                "Status Bobot" to if (avgGram > 0) "TERPANTAU SECARA RUTIN" else "BELUM ADA DATA"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Bobot Terkini" to FormatHelper.formatGram(avgGram),
                "Target Panen" to "${FormatHelper.formatTwoDecimals(targetWeightKg)} Kg",
                "Rata-rata Kg" to "${FormatHelper.formatTwoDecimals(latestWeight?.averageWeightKg ?: (avgGram / 1000.0))} Kg",
                "Sampel Terkini" to FormatHelper.formatEkor(latestWeight?.sampleCount ?: 0),
                "Target FCR" to "${cycle?.targetFcr ?: 1.40}",
                "Target IP" to "> 400"
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("HISTORI SAMPLING TIMBANGAN AYAM", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("Tgl", "Umur", "Sampel", "Total Kg", "Rata-rata (Gram)", "Rata-rata (Kg)", "Catatan / Evaluasi"))

        weights.sortedBy { it.ageDays }.forEach { w ->
            tableData.add(
                listOf(
                    w.date,
                    FormatHelper.formatHari(w.ageDays),
                    FormatHelper.formatEkor(w.sampleCount),
                    FormatHelper.formatKg(w.totalWeightKg),
                    FormatHelper.formatGram(w.averageWeightGram),
                    "${FormatHelper.formatTwoDecimals(w.averageWeightKg)} Kg",
                    w.notes.ifBlank { "Sampling timbangan normal" }
                )
            )
        }

        if (weights.isEmpty()) {
            tableData.add(listOf("-", "-", "0", "0", "0 g", "0 Kg", "Belum ada sampling bobot"))
        }

        y = drawTable(canvas, y, tableData, listOf(70f, 50f, 65f, 65f, 85f, 75f, 105f))

        y += 30f
        drawSignatures(canvas, y, profile.ownerName, "Tim Sampling / QC")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Foto bukti timbangan
        val weightPhotos = mutableListOf<PhotoEvidenceEntity>()
        weights.forEach { w ->
            if (w.photoUri.isNotBlank()) {
                weightPhotos.add(
                    PhotoEvidenceEntity(
                        photoUri = w.photoUri,
                        reportType = "Bobot",
                        date = w.date,
                        time = "Kandang",
                        caption = "Sampling Hari ke-${w.ageDays}: Avg ${FormatHelper.formatGram(w.averageWeightGram)}, Sampel ${w.sampleCount} ekor"
                    )
                )
            }
        }
        weightPhotos.addAll(photos.filter { it.reportType.equals("Bobot", ignoreCase = true) || it.reportType.equals("WEIGHT", ignoreCase = true) })
        appendPhotoEvidencePages(context, pdfDoc, weightPhotos)

        val file = File(getReportsDir(context), "Laporan_Bobot_Ayam_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
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
        reportNumber: String = "SB/PNN/${cycle?.id ?: 0}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN PENJUALAN & REALISASI PANEN AYAM", reportNumber)

        var y = 140f
        val totalBirds = harvests.sumOf { it.birdCount }
        val totalKg = harvests.sumOf { it.totalWeightKg }
        val totalRevenue = harvests.sumOf { it.totalRevenue }
        val avgWeight = if (totalBirds > 0) totalKg / totalBirds else 0.0
        val avgPriceKg = if (totalKg > 0) totalRevenue / totalKg else 0.0

        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Perusahaan Mitra" to (partner?.companyName ?: "-"),
                "Nomor Siklus" to (cycle?.cycleNumber ?: "-"),
                "Tanggal Chick-In" to (cycle?.chickInDate ?: "-"),
                "Total Ayam Terpanen" to FormatHelper.formatEkor(totalBirds),
                "Total Tonase Panen" to FormatHelper.formatKg(totalKg),
                "Rata-rata Bobot Panen" to "${FormatHelper.formatTwoDecimals(avgWeight)} Kg/Ekor",
                "Total Nilai Penjualan" to FormatHelper.formatRupiah(totalRevenue)
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
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("RINCIAN SURAT JALAN & PENIMBANGAN PANEN (DO)", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("Tgl", "No DO / DO Panen", "Pembeli", "Ekor", "Total Kg", "Avg Kg", "Penerimaan (Rp)"))

        harvests.sortedBy { it.harvestDate }.forEach { h ->
            tableData.add(
                listOf(
                    h.harvestDate,
                    h.doNumber.ifBlank { "DO-${h.id}" },
                    h.buyerName.take(20).ifBlank { "-" },
                    FormatHelper.formatInteger(h.birdCount),
                    FormatHelper.formatNumber(h.totalWeightKg),
                    FormatHelper.formatTwoDecimals(h.averageWeightKg),
                    FormatHelper.formatRupiah(h.totalRevenue)
                )
            )
        }

        if (harvests.isEmpty()) {
            tableData.add(listOf("-", "-", "Belum ada transaksi panen", "0", "0 Kg", "0 Kg", "Rp 0"))
        }

        y = drawTable(canvas, y, tableData, listOf(65f, 85f, 110f, 45f, 60f, 45f, 105f))

        y += 30f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "Koordinator Panen")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)

        // Foto bukti panen / DO / timbangan armada
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

        val file = File(getReportsDir(context), "Laporan_Panen_Ayam_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 9. LAPORAN KEMITRAAN & REKONSILIASI PDF
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
        reportNumber: String = "SB/LKM/${cycle.id}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN KEMITRAAN BUDIDAYA BROILER", reportNumber)

        var y = 140f
        val totalHarvestBirds = harvests.sumOf { it.birdCount }
        val totalHarvestWeight = harvests.sumOf { it.totalWeightKg }
        val totalHarvestRevenue = harvests.sumOf { it.totalRevenue }
        val totalDead = dailyLogs.sumOf { it.deadCount }
        val totalCulls = dailyLogs.sumOf { it.cullCount }
        val totalFeedUsed = feedStocks.filter { it.movementType == "OUT" }.sumOf { it.totalKg }.let { if (it > 0) it else dailyLogs.sumOf { log -> log.feedGivenKg } }
        val mortalityRate = if (cycle.docCount > 0) (totalDead.toDouble() / cycle.docCount) * 100.0 else 0.0
        val fcr = if (totalHarvestWeight > 0) totalFeedUsed / totalHarvestWeight else 0.0
        val avgWeight = if (totalHarvestBirds > 0) totalHarvestWeight / totalHarvestBirds else 0.0

        val totalExpensesAmount = expenses.filter { it.transactionType == "OUT" }.sumOf { it.totalAmount }
        val netIncome = totalHarvestRevenue - totalExpensesAmount

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
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("REKAPITULASI HASIL KEMITRAAN & KEUANGAN", 40f, y, paint)
        y += 10f

        val tableData = listOf(
            listOf("Komponen Kemitraan", "Jumlah / Satuan", "Nilai (Rp)"),
            listOf("Hasil Penjualan Panen", FormatHelper.formatKg(totalHarvestWeight), FormatHelper.formatRupiah(totalHarvestRevenue)),
            listOf("Biaya DOC Masuk", FormatHelper.formatEkor(cycle.docCount), FormatHelper.formatRupiah(cycle.docCount * cycle.docPricePerHead)),
            listOf("Biaya Pakan", FormatHelper.formatKg(totalFeedUsed), FormatHelper.formatRupiah(totalFeedUsed * (partner?.feedPrice ?: 0.0))),
            listOf("Biaya Operasional & OVK", "${expenses.count { it.transactionType == "OUT" }} Transaksi", FormatHelper.formatRupiah(totalExpensesAmount)),
            listOf("ESTIMASI HASIL USAHA (LABA BERSIH)", if (netIncome >= 0) "SURPLUS" else "DEFISIT", FormatHelper.formatRupiah(netIncome))
        )

        y = drawTable(canvas, y, tableData, listOf(200f, 155f, 160f))

        y += 25f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "PIC Perusahaan Mitra")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)
        appendPhotoEvidencePages(context, pdfDoc, photos)
        val file = File(getReportsDir(context), "Laporan_Kemitraan_${cycle.id}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 10. LAPORAN AKHIR SIKLUS (TUTUP BUKU 19 POIN) PDF
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
        reportNumber: String = "SB/LAK/${cycle.id}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN AKHIR SIKLUS BUDIDAYA BROILER", reportNumber)

        var y = 140f
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
        val performanceLabel = when {
            ipIndex >= 400.0 -> "Sangat Baik (IP > 400)"
            ipIndex >= 350.0 -> "Baik (IP 350-400)"
            ipIndex >= 300.0 -> "Cukup (IP 300-350)"
            ipIndex > 0.0 -> "Perlu Perhatian"
            else -> "Belum Tersedia"
        }
        val totalExp = expenses.sumOf { it.totalAmount }
        val profit = totalHarvestRevenue - totalExp

        y = drawInfoBox(
            canvas, y,
            listOf(
                "Nama Usaha" to profile.farmName,
                "Pemilik / Pengelola" to profile.ownerName,
                "Nama Kandang" to (coop?.name ?: "-"),
                "Tipe & Kapasitas" to "${coop?.coopType?.takeIf { it.isNotBlank() } ?: "-"} (${FormatHelper.formatEkor(coop?.capacity ?: 0)})",
                "Perusahaan Mitra" to (partner?.companyName ?: "-"),
                "Nomor Kontrak" to (partner?.contractNumber ?: "-"),
                "Nomor Siklus" to cycle.cycleNumber,
                "DOC Masuk / Strain" to "${FormatHelper.formatEkor(cycle.docCount)} / ${cycle.docStrain}"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Indeks Performa (IP)" to "${FormatHelper.formatIp(ipIndex)} ($performanceLabel)",
                "FCR Realisasi" to FormatHelper.formatFcr(fcr),
                "Mortalitas Total" to FormatHelper.formatPersen(mortalityRate),
                "Survival Rate" to FormatHelper.formatPersen(survivalRate),
                "Rata-rata Bobot" to "${FormatHelper.formatTwoDecimals(avgWeight)} Kg",
                "Laba Bersih" to FormatHelper.formatRupiah(profit)
            )
        )

        y += 20f
        val paint = newPdfPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("19 POIN EVALUASI AKHIR SIKLUS", 40f, y, paint)
        y += 10f

        val tableData = listOf(
            listOf("No", "Parameter Evaluasi", "Standar / Target", "Realisasi Akhir"),
            listOf("1", "Jumlah DOC Masuk", FormatHelper.formatEkor(cycle.docCount), FormatHelper.formatEkor(cycle.docCount)),
            listOf("2", "Kematian Total", "< 3.0 %", "$totalDead Ekor (${FormatHelper.formatPersen(mortalityRate)})"),
            listOf("3", "Ayam Afkir / Culling", "< 1.0 %", "$totalCulls Ekor"),
            listOf("4", "Persentase Hidup (Survival)", "> 96.0 %", FormatHelper.formatPersen(survivalRate)),
            listOf("5", "Total Pakan Terkonsumsi", "-", FormatHelper.formatKg(totalFeedUsed)),
            listOf("6", "FCR (Feed Conversion Ratio)", if (cycle.targetFcr > 0) "< ${cycle.targetFcr}" else "< 1.40", FormatHelper.formatFcr(fcr)),
            listOf("7", "Rata-rata Bobot Panen", if (cycle.targetWeightKg > 0) "> ${cycle.targetWeightKg} Kg" else "> 2.00 Kg", "${FormatHelper.formatTwoDecimals(avgWeight)} Kg"),
            listOf("8", "Umur Panen Rata-rata", "${targetAge} Hari", "${targetAge} Hari"),
            listOf("9", "Indeks Performa (IP)", "> 400", "${FormatHelper.formatIp(ipIndex)} Point"),
            listOf("10", "Total Ayam Terpanen", "-", FormatHelper.formatEkor(totalHarvestBirds)),
            listOf("11", "Total Tonase Panen", "-", FormatHelper.formatKg(totalHarvestWeight)),
            listOf("12", "Harga Jual Kontrak", "-", FormatHelper.formatRupiah(partner?.liveBirdPrice ?: 0.0)),
            listOf("13", "Total Pendapatan Panen", "-", FormatHelper.formatRupiah(totalHarvestRevenue)),
            listOf("14", "Total Biaya Operasional", "-", FormatHelper.formatRupiah(totalExp)),
            listOf("15", "Hasil Usaha Bersih", "Profit", FormatHelper.formatRupiah(profit)),
            listOf("16", "Status Performa Siklus", "-", if (cycle.status == "HARVESTED" || totalHarvestBirds > 0) "SELESAI" else "BERJALAN")
        )

        y = drawTable(canvas, y, tableData, listOf(30f, 210f, 135f, 140f))

        y += 20f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "Manajemen Mitra")
        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)
        appendPhotoEvidencePages(context, pdfDoc, photos)
        val file = File(getReportsDir(context), "Laporan_Akhir_Siklus_${cycle.id}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // 11. LAPORAN PERIODIK PERKEMBANGAN HARIAN (Landscape Table) PDF
    // =========================================================================
    fun generatePeriodReportPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity,
        logs: List<DailyLogEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        periodTitle: String = "Perkembangan Harian Lengkap",
        reportNumber: String = "SB/LPR/${cycle.id}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // Landscape A4
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialLandscapeHeader(canvas, context, profile, "LAPORAN PERIODIK PERKEMBANGAN HARIAN ($periodTitle)", reportNumber)

        var y = 125f
        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("Tgl", "Umur", "Pop. Pagi", "Mati", "Afkir", "Pakan (Kg)", "Air (L)", "Suhu", "Kondisi Ayam", "Catatan"))

        logs.forEach { log ->
            tableData.add(
                listOf(
                    log.date.takeLast(5),
                    FormatHelper.formatHari(log.ageDays),
                    FormatHelper.formatInteger(log.morningPopulation),
                    FormatHelper.formatInteger(log.deadCount),
                    FormatHelper.formatInteger(log.cullCount),
                    FormatHelper.formatNumber(log.feedGivenKg),
                    FormatHelper.formatNumber(log.waterIntakeLiters),
                    "${log.tempCelsius}°C",
                    log.chickenCondition.take(15),
                    log.notes.take(25)
                )
            )
        }

        y = drawTable(canvas, y, tableData, listOf(60f, 50f, 65f, 50f, 50f, 80f, 65f, 55f, 130f, 155f), isLandscape = true)

        y += 25f
        drawLandscapeSignatures(canvas, y, profile.ownerName, "Petugas / TS Perusahaan")
        drawFooter(canvas, 1, 1, isLandscape = true)

        pdfDoc.finishPage(page)
        appendPhotoEvidencePages(context, pdfDoc, photos)
        val file = File(getReportsDir(context), "Laporan_Periodik_${System.currentTimeMillis()}.pdf")
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
        reportNumber: String = "SB/FTO/${cycle?.id ?: 0}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
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
        canvas.drawText("RINGKASAN ARSIP FOTO BUKTI LAPANGAN", 40f, y, paint)
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

        // Lampirkan halaman foto lengkap
        appendPhotoEvidencePages(context, pdfDoc, photos)

        val file = File(getReportsDir(context), "Laporan_Foto_Bukti_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
        pdfDoc.close()
        return file
    }

    // =========================================================================
    // DRAWING HELPERS (Headers, Boxes, Tables, Cards, Signatures, Footers)
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
        paint.textSize = 18f
        canvas.drawText(profile.farmName.ifBlank { "SEJAHTERA BERSAMA" }, 120f, 52f, paint)

        paint.color = Color.parseColor("#2E7D32")
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = 10f
        if (profile.slogan.isNotBlank()) canvas.drawText("« ${profile.slogan} »", 120f, 67f, paint)

        paint.color = Color.DKGRAY
        paint.textSize = 8.5f
        val address = listOf(profile.address, profile.village, profile.district, profile.regency, profile.province)
            .map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ")
        if (address.isNotBlank()) canvas.drawText(address, 120f, 82f, paint)
        val contact = listOf(
            profile.phoneNumber.trim().takeIf { it.isNotBlank() }?.let { "Telp: $it" },
            profile.email.trim().takeIf { it.isNotBlank() }?.let { "Email: $it" }
        ).filterNotNull().joinToString(" | ")
        if (contact.isNotBlank()) canvas.drawText(contact, 120f, 94f, paint)

        paint.color = Color.parseColor("#1B5E20")
        paint.strokeWidth = 2.5f
        canvas.drawLine(40f, 104f, 555f, 104f, paint)
        paint.strokeWidth = 0.8f
        paint.color = Color.LTGRAY
        canvas.drawLine(40f, 107f, 555f, 107f, paint)

        paint.color = Color.parseColor("#1B5E20")
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText(reportTitle, 40f, 123f, paint)
        paint.color = Color.DKGRAY
        paint.textSize = 9f
        canvas.drawText("No: $reportNo", 445f, 123f, paint)
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

        paint.color = Color.parseColor("#F1F8E9") // Very light green
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
        val cardHeight = 44f
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
            canvas.drawText(title.uppercase(Locale.ROOT), curX + 8f, curY + 14f, paint)

            paint.color = Color.parseColor("#1B5E20")
            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(value, curX + 8f, curY + 32f, paint)

            if ((index + 1) % 3 == 0) {
                curX = 40f
                curY += cardHeight + 8f
            } else {
                curX += cardWidth + 10f
            }
        }
        return if (cards.size % 3 == 0) curY else curY + cardHeight + 8f
    }

    private fun drawTable(canvas: Canvas, startY: Float, rows: List<List<String>>, colWidths: List<Float>, isLandscape: Boolean = false): Float {
        val paint = newPdfPaint()
        var curY = startY
        val startX = 40f
        val totalWidth = colWidths.sum()
        val rowHeight = 18f

        rows.forEachIndexed { rowIndex, row ->
            val isHeader = rowIndex == 0
            val isZebra = rowIndex % 2 == 1 && !isHeader

            // Background
            val rowRect = RectF(startX, curY, startX + totalWidth, curY + rowHeight)
            paint.style = Paint.Style.FILL
            paint.color = if (isHeader) Color.parseColor("#1B5E20") else if (isZebra) Color.parseColor("#F9FBE7") else Color.WHITE
            canvas.drawRect(rowRect, paint)

            // Cell border
            paint.style = Paint.Style.STROKE
            paint.color = if (isHeader) Color.parseColor("#1B5E20") else Color.parseColor("#E0E0E0")
            paint.strokeWidth = 0.8f
            canvas.drawRect(rowRect, paint)

            // Cell texts
            paint.style = Paint.Style.FILL
            paint.color = if (isHeader) Color.WHITE else Color.BLACK
            paint.typeface = if (isHeader) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            paint.textSize = if (isHeader) 8.5f else 8f

            var cellX = startX
            row.forEachIndexed { colIndex, cellText ->
                val colW = colWidths.getOrElse(colIndex) { 80f }
                val textToDraw = if (cellText.length > 35) cellText.take(33) + ".." else cellText
                canvas.drawText(textToDraw, cellX + 5f, curY + 12.5f, paint)

                // Column divider
                if (colIndex > 0) {
                    paint.style = Paint.Style.STROKE
                    paint.color = if (isHeader) Color.parseColor("#2E7D32") else Color.parseColor("#E0E0E0")
                    canvas.drawLine(cellX, curY, cellX, curY + rowHeight, paint)
                    paint.style = Paint.Style.FILL
                    paint.color = if (isHeader) Color.WHITE else Color.BLACK
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
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT

        canvas.drawText("Dibuat & Diverifikasi,", 70f, startY, paint)
        canvas.drawText("PETERNAK / PENGELOLA", 70f, startY + 12f, paint)

        canvas.drawText("Mengetahui & Menyetujui,", 380f, startY, paint)
        canvas.drawText("PERUSAHAAN MITRA", 380f, startY + 12f, paint)

        // Signature Lines
        paint.strokeWidth = 0.8f
        canvas.drawLine(50f, startY + 65f, 210f, startY + 65f, paint)
        canvas.drawLine(360f, startY + 65f, 520f, startY + 65f, paint)

        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(ownerName, 60f, startY + 77f, paint)
        canvas.drawText(partnerPicName, 370f, startY + 77f, paint)
    }

    private fun drawLandscapeSignatures(canvas: Canvas, startY: Float, ownerName: String, partnerPicName: String) {
        val paint = newPdfPaint()
        paint.color = Color.BLACK
        paint.textSize = 9f

        canvas.drawText("Peternak / Pengelola Kandang,", 100f, startY, paint)
        canvas.drawText("Technical Support Perusahaan Mitra,", 540f, startY, paint)

        paint.strokeWidth = 0.8f
        canvas.drawLine(80f, startY + 50f, 260f, startY + 50f, paint)
        canvas.drawLine(520f, startY + 50f, 700f, startY + 50f, paint)

        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(ownerName, 90f, startY + 62f, paint)
        canvas.drawText(partnerPicName, 530f, startY + 62f, paint)
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int, isLandscape: Boolean = false) {
        val paint = newPdfPaint()
        paint.color = Color.GRAY
        paint.textSize = 7.5f
        val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        val y = if (isLandscape) 575f else 820f
        val rightX = if (isLandscape) 720f else 480f

        canvas.drawText("Dicetak otomatis oleh Aplikasi SEJAHTERA BERSAMA pada $dateStr WIB", 40f, y, paint)
        canvas.drawText("Halaman $pageNum dari $totalPages", rightX, y, paint)
    }

    // =========================================================================
    // MULTI-PAGE PHOTO EVIDENCE RENDERER WITH CRASH SAFETY
    // =========================================================================
    private fun appendPhotoEvidencePages(context: Context, pdfDoc: PdfDocument, photos: List<PhotoEvidenceEntity>) {
        val usable = photos
            .sortedBy { it.createdAt }
            .filter { it.photoUri.isNotBlank() || it.watermarkedUri.isNotBlank() }

        if (usable.isEmpty()) return

        val perPage = 2
        usable.chunked(perPage).forEachIndexed { pageIndex, pair ->
            val pageNumber = pageIndex + 2
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas
            val titlePaint = newPdfPaint().apply {
                color = Color.parseColor("#1B5E20")
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("LAMPIRAN FOTO BUKTI LAPORAN", 40f, 50f, titlePaint)
            canvas.drawText("Dokumentasi Resmi Terintegrasi (Halaman $pageNumber)", 40f, 68f, newPdfPaint().apply { color = Color.DKGRAY; textSize = 8.5f })

            pair.forEachIndexed { index, photo ->
                val globalNumber = pageIndex * perPage + index + 1
                val targetPath = photo.watermarkedUri.ifBlank { photo.photoUri }
                val top = 88f + index * 365f
                val maxW = 515f
                val maxH = 260f

                val bitmap = PhotoStorageHelper.loadBitmapSafe(context, targetPath, maxDim = 1200)
                if (bitmap != null) {
                    val scale = minOf(maxW / bitmap.width.toFloat(), maxH / bitmap.height.toFloat())
                    val w = bitmap.width * scale
                    val h = bitmap.height * scale
                    val left = 40f + (maxW - w) / 2f
                    canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), newPdfPaint())
                    bitmap.recycle()
                } else {
                    // Gambar kotak penampung placeholder aman jika file foto hilang / corrupt
                    val placeholderRect = RectF(40f, top, 40f + maxW, top + maxH)
                    val boxPaint = newPdfPaint().apply {
                        color = Color.parseColor("#F5F5F5")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(placeholderRect, 8f, 8f, boxPaint)
                    boxPaint.style = Paint.Style.STROKE
                    boxPaint.color = Color.LTGRAY
                    boxPaint.strokeWidth = 1f
                    canvas.drawRoundRect(placeholderRect, 8f, 8f, boxPaint)

                    val textP = newPdfPaint().apply {
                        color = Color.GRAY
                        textSize = 11f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("Bukti foto tidak dapat dimuat / file telah dipindahkan", 40f + maxW / 2f, top + maxH / 2f, textP)
                }

                val textPaint = newPdfPaint().apply { color = Color.DKGRAY; textSize = 9f }
                canvas.drawText("BUKTI FOTO #$globalNumber", 40f, top + maxH + 18f, newPdfPaint().apply { color = Color.parseColor("#1B5E20"); textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD })
                canvas.drawText("Kategori: ${photo.reportType} | Tanggal/Waktu: ${photo.date} ${photo.time}", 40f, top + maxH + 34f, textPaint)
                if (photo.caption.isNotBlank()) canvas.drawText("Keterangan: ${photo.caption.take(80)}", 40f, top + maxH + 49f, textPaint)
                if (photo.latitude != null && photo.longitude != null) {
                    canvas.drawText("Koordinat GPS: ${photo.latitude}, ${photo.longitude}", 40f, top + maxH + 64f, textPaint)
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
