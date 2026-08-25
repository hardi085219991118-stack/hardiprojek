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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    private val idRupiahFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    private val decimalFormatter = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    private fun formatRupiah(amount: Double): String = idRupiahFormatter.format(amount)
    private fun formatNum(amount: Double): String = decimalFormatter.format(amount)

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

    // --- 1. LAPORAN HARIAN PDF ---
    fun generateDailyReportPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        partner: PartnerEntity?,
        cycle: CycleEntity,
        dailyLog: DailyLogEntity,
        mortalities: List<MortalityLogEntity>,
        feedRecords: List<FeedStockEntity>,
        medicines: List<MedicineEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        reportNumber: String = "SB/LPH/${dailyLog.ageDays}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Portrait (points)
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawOfficialHeader(canvas, context, profile, "LAPORAN HARIAN BUDIDAYA AYAM BROILER", reportNumber)

        var y = 140f
        val paint = newPdfPaint()

        // Metadata Box
        y = drawInfoBox(
            canvas, y,
            listOf(
                "Kandang" to (coop?.name ?: "-"),
                "Perusahaan Mitra" to (partner?.companyName ?: "-"),
                "Nomor Siklus" to cycle.cycleNumber,
                "Tanggal Chick-In" to cycle.chickInDate,
                "Tanggal Laporan" to dailyLog.date,
                "Umur Ayam" to "${dailyLog.ageDays} Hari",
                "Populasi Awal" to "${cycle.docCount} Ekor",
                "Kondisi Cuaca" to "${dailyLog.weather} (${dailyLog.tempCelsius}°C, ${dailyLog.humidityPercent}%)"
            )
        )

        y += 15f
        // Daily Performance Summary Cards
        val calculatedCurrentPop = dailyLog.afternoonPopulation
        val mortCount = dailyLog.deadCount
        val cullCount = dailyLog.cullCount
        val feedKg = dailyLog.feedGivenKg
        val waterL = dailyLog.waterIntakeLiters

        y = drawKpiCards(
            canvas, y, listOf(
                "Populasi Sore" to "$calculatedCurrentPop Ekor",
                "Kematian Hari Ini" to "$mortCount Ekor",
                "Afkir Hari Ini" to "$cullCount Ekor",
                "Pakan Diberikan" to "${formatNum(feedKg)} Kg",
                "Air Minum" to "${formatNum(waterL)} Liter",
                "Kondisi Ayam" to dailyLog.chickenCondition
            )
        )

        y += 20f
        // Detail Kematian & Pakan
        paint.color = Color.parseColor("#1B5E20")
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("RINCIAN OPERASIONAL & KESEHATAN", 40f, y, paint)
        y += 10f

        val tableData = mutableListOf<List<String>>()
        tableData.add(listOf("Parameter", "Keterangan / Nilai"))
        tableData.add(listOf("Kondisi Sekam / Litter", dailyLog.litterCondition))
        tableData.add(listOf("Obat Diberikan", dailyLog.medicineGiven.ifEmpty { "-" }))
        tableData.add(listOf("Vitamin Diberikan", dailyLog.vitaminGiven.ifEmpty { "-" }))
        tableData.add(listOf("Vaksin Diberikan", dailyLog.vaccineGiven.ifEmpty { "-" }))
        tableData.add(listOf("Sisa Pakan Tempat Pakan", "${formatNum(dailyLog.feedRemainingKg)} Kg"))
        tableData.add(listOf("Catatan / Kejadian Penting", dailyLog.notes.ifEmpty { "Kondisi kandang normal dan terkendali." }))

        y = drawTable(canvas, y, tableData, listOf(180f, 335f))

        y += 30f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "Perusahaan Mitra")

        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)
        appendPhotoEvidencePages(context, pdfDoc, photos)
        val file = File(getReportsDir(context), "Laporan_Harian_Hari_${dailyLog.ageDays}_${dailyLog.date}.pdf")
        FileOutputStream(file).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        return file
    }

    // --- 2. LAPORAN KEMITRAAN PDF ---
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
        // Metadata
        val totalHarvestBirds = harvests.sumOf { it.birdCount }
        val totalHarvestWeight = harvests.sumOf { it.totalWeightKg }
        val totalHarvestRevenue = harvests.sumOf { it.totalRevenue }
        val totalDead = dailyLogs.sumOf { it.deadCount }
        val totalCulls = dailyLogs.sumOf { it.cullCount }
        val totalFeedUsed = feedStocks.filter { it.movementType == "OUT" }.sumOf { it.totalKg }.let { if (it > 0) it else dailyLogs.sumOf { log -> log.feedGivenKg } }
        val finalPop = cycle.docCount - totalDead - totalCulls - totalHarvestBirds
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
                "DOC Masuk" to "${cycle.docCount} Ekor (${cycle.docStrain})",
                "Tanggal Chick-In" to cycle.chickInDate,
                "Target FCR / Realisasi" to "${cycle.targetFcr} / ${formatNum(fcr)}"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Total Ayam Panen" to "$totalHarvestBirds Ekor",
                "Total Bobot Panen" to "${formatNum(totalHarvestWeight)} Kg",
                "Rata-rata Bobot" to "${formatNum(avgWeight)} Kg",
                "Mortalitas Kumulatif" to "${formatNum(mortalityRate)} %",
                "Total Pakan Terpakai" to "${formatNum(totalFeedUsed)} Kg",
                "FCR Realisasi" to formatNum(fcr)
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
            listOf("Hasil Penjualan Panen", "${formatNum(totalHarvestWeight)} Kg", formatRupiah(totalHarvestRevenue)),
            listOf("Biaya DOC Masuk", "${cycle.docCount} Ekor", formatRupiah(cycle.docCount * cycle.docPricePerHead)),
            listOf("Biaya Pakan", "${formatNum(totalFeedUsed)} Kg", formatRupiah(totalFeedUsed * (partner?.feedPrice ?: 0.0))),
            listOf("Biaya Operasional & OVK", "${expenses.count { it.transactionType == "OUT" }} Transaksi", formatRupiah(totalExpensesAmount)),
            listOf("ESTIMASI HASIL USAHA (LABA BERSIH)", if (netIncome >= 0) "SURPLUS" else "DEFISIT", formatRupiah(netIncome))
        )

        y = drawTable(canvas, y, tableData, listOf(200f, 155f, 160f))

        y += 25f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "PIC Perusahaan Mitra")

        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)
        appendPhotoEvidencePages(context, pdfDoc, photos)
        val file = File(getReportsDir(context), "Laporan_Kemitraan_${cycle.id}.pdf")
        FileOutputStream(file).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        return file
    }

    // --- 3. LAPORAN AKHIR SIKLUS PDF (Comprehensive 19 Points) ---
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
        val totalFeedIn = feedStocks.filter { it.movementType == "IN" }.sumOf { it.totalKg }
        val remainingFeed = totalFeedIn - totalFeedUsed
        val mortalityRate = if (cycle.docCount > 0) (totalDead.toDouble() / cycle.docCount) * 100.0 else 0.0
        val survivalRate = if (cycle.docCount > 0) 100.0 - mortalityRate else 0.0
        val avgWeight = if (totalHarvestBirds > 0) totalHarvestWeight / totalHarvestBirds else 0.0
        val fcr = if (totalHarvestWeight > 0 && totalFeedUsed > 0) totalFeedUsed / totalHarvestWeight else 0.0
        val adg = 0.0
        val ipIndex = if (fcr > 0 && cycle.targetHarvestAgeDays > 0) ((survivalRate * avgWeight) / (fcr * cycle.targetHarvestAgeDays)) * 100 else 0.0
        val performanceLabel = when {
            ipIndex >= 350.0 -> "Sangat Baik"
            ipIndex >= 300.0 -> "Baik"
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
                "Tipe & Kapasitas" to "${coop?.coopType?.takeIf { it.isNotBlank() } ?: "-"} (${coop?.capacity ?: 0} Ekor)",
                "Perusahaan Mitra" to (partner?.companyName ?: "-"),
                "Nomor Kontrak" to (partner?.contractNumber ?: "-"),
                "Nomor Siklus" to cycle.cycleNumber,
                "DOC Masuk / Strain" to "${cycle.docCount} Ekor / ${cycle.docStrain}"
            )
        )

        y += 15f
        y = drawKpiCards(
            canvas, y, listOf(
                "Indeks Performa (IP)" to "${formatNum(ipIndex)} ($performanceLabel)",
                "FCR Realisasi" to formatNum(fcr),
                "Mortalitas Total" to "${formatNum(mortalityRate)} %",
                "Survival Rate" to "${formatNum(survivalRate)} %",
                "ADG Harian" to "${formatNum(adg)} gr/hari",
                "Laba Bersih" to formatRupiah(profit)
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
            listOf("1", "Jumlah DOC Masuk", "${cycle.docCount} Ekor", "${cycle.docCount} Ekor"),
            listOf("2", "Kematian Total", "< 4.0 %", "$totalDead Ekor (${formatNum(mortalityRate)}%)"),
            listOf("3", "Ayam Afkir / Culling", "< 1.0 %", "$totalCulls Ekor"),
            listOf("4", "Persentase Hidup (Survival)", "> 95.0 %", "${formatNum(survivalRate)} %"),
            listOf("5", "Total Pakan Terkonsumsi", "-", "${formatNum(totalFeedUsed)} Kg"),
            listOf("6", "FCR (Feed Conversion Ratio)", if (cycle.targetFcr > 0) "< ${cycle.targetFcr}" else "-", formatNum(fcr)),
            listOf("7", "Rata-rata Bobot Panen", if (cycle.targetWeightKg > 0) "> ${cycle.targetWeightKg} Kg" else "-", "${formatNum(avgWeight)} Kg"),
            listOf("8", "Average Daily Gain (ADG)", "> 55 gr/hari", "${formatNum(adg)} gr/hari"),
            listOf("9", "Indeks Performa (IP)", "> 350", "${formatNum(ipIndex)} Point"),
            listOf("10", "Total Ayam Terpanen", "-", "$totalHarvestBirds Ekor"),
            listOf("11", "Total Tonase Panen", "-", "${formatNum(totalHarvestWeight)} Kg"),
            listOf("12", "Harga Jual Kontrak", "-", formatRupiah(partner?.liveBirdPrice ?: 0.0)),
            listOf("13", "Total Pendapatan Panen", "-", formatRupiah(totalHarvestRevenue)),
            listOf("14", "Total Biaya Operasional", "-", formatRupiah(totalExp)),
            listOf("15", "Hasil Usaha Bersih", "Profit", formatRupiah(profit)),
            listOf("16", "Status Performa Siklus", "-", if (cycle.status == "HARVESTED" || totalHarvestBirds > 0) "SELESAI" else "BERJALAN")
        )

        y = drawTable(canvas, y, tableData, listOf(30f, 210f, 135f, 140f))

        y += 20f
        drawSignatures(canvas, y, profile.ownerName, partner?.picName ?: "Manajemen Mitra")

        drawFooter(canvas, 1, 1)

        pdfDoc.finishPage(page)
        appendPhotoEvidencePages(context, pdfDoc, photos)
        val file = File(getReportsDir(context), "Laporan_Akhir_Siklus_${cycle.id}.pdf")
        FileOutputStream(file).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        return file
    }

    // --- 4. LAPORAN PERIODE / REKAP HARIAN (Landscape Table) ---
    fun generatePeriodReportPdf(
        context: Context,
        profile: FarmProfileEntity,
        coop: CoopEntity?,
        cycle: CycleEntity,
        logs: List<DailyLogEntity>,
        photos: List<PhotoEvidenceEntity> = emptyList(),
        periodTitle: String = "7 Hari Terakhir",
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
                    "${log.ageDays} hr",
                    "${log.morningPopulation}",
                    "${log.deadCount}",
                    "${log.cullCount}",
                    formatNum(log.feedGivenKg),
                    formatNum(log.waterIntakeLiters),
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
        FileOutputStream(file).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        return file
    }

    // --- DRAWING HELPERS ---
    private fun newPdfPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.DITHER_FLAG).apply {
        isFilterBitmap = true
        isLinearText = true
    }

    private fun drawOfficialHeader(canvas: Canvas, context: Context, profile: FarmProfileEntity, reportTitle: String, reportNo: String) {
        // Render logo dari bitmap sumber yang lebih besar, tetapi gambar di area kop yang aman.
        // Area logo dijaga agar tidak menyentuh garis kop dan tetap tajam saat dicetak.
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

    // --- ACTIONS: OPEN, SHARE, PRINT ---
    private fun appendPhotoEvidencePages(context: Context, pdfDoc: PdfDocument, photos: List<PhotoEvidenceEntity>) {
        // Foto mengikuti urutan laporan/penyimpanan (createdAt lama -> baru), tanpa batas jumlah.
        // Jika halaman penuh, foto berikutnya otomatis dilanjutkan ke halaman lampiran berikutnya.
        val usable = photos
            .sortedBy { it.createdAt }
            .filter { File(it.watermarkedUri.ifBlank { it.photoUri }).exists() }
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
            canvas.drawText("Halaman lampiran $pageNumber", 40f, 68f, newPdfPaint().apply { color = Color.DKGRAY; textSize = 8f })

            pair.forEachIndexed { index, photo ->
                val globalNumber = pageIndex * perPage + index + 1
                val file = File(photo.watermarkedUri.ifBlank { photo.photoUri })
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@forEachIndexed
                val top = 88f + index * 365f
                val maxW = 515f
                val maxH = 260f
                val scale = minOf(maxW / bitmap.width.toFloat(), maxH / bitmap.height.toFloat())
                val w = bitmap.width * scale
                val h = bitmap.height * scale
                val left = 40f + (maxW - w) / 2f
                canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), newPdfPaint())

                val textPaint = newPdfPaint().apply { color = Color.DKGRAY; textSize = 9f }
                canvas.drawText("Foto Bukti #$globalNumber", 40f, top + maxH + 18f, textPaint)
                canvas.drawText("Kategori: ${photo.reportType} | Tanggal/Jam: ${photo.date} ${photo.time}", 40f, top + maxH + 34f, textPaint)
                if (photo.caption.isNotBlank()) canvas.drawText("Keterangan: ${photo.caption.take(80)}", 40f, top + maxH + 50f, textPaint)
                if (photo.latitude != null && photo.longitude != null) canvas.drawText("Koordinat: ${photo.latitude}, ${photo.longitude}", 40f, top + maxH + 66f, textPaint)
                bitmap.recycle()
            }
            pdfDoc.finishPage(page)
        }
    }

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
