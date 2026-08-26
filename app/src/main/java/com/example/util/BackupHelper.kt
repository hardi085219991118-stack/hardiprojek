package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.FarmRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * BackupPackageResult: Hasil proses pembuatan cadangan paket ZIP lengkap.
 */
data class BackupPackageResult(
    val success: Boolean,
    val file: File? = null,
    val totalRecords: Int = 0,
    val totalPhotos: Int = 0,
    val photosArchived: Int = 0,
    val photosFailed: Int = 0,
    val sizeBytes: Long = 0L,
    val errorMessage: String? = null,
    val warnings: List<String> = emptyList(),
    val categoriesCount: Map<String, Int> = emptyMap()
) {
    val sizeMb: Double get() = (sizeBytes / (1024.0 * 1024.0)).let { Math.round(it * 100.0) / 100.0 }
}

/**
 * BackupInspectionResult: Hasil inspeksi awal sebelum melakukan pemulihan (restore).
 */
data class BackupInspectionResult(
    val isValid: Boolean,
    val isZip: Boolean = true,
    val appName: String = "",
    val backupDate: String = "",
    val farmName: String = "",
    val ownerName: String = "",
    val totalRecords: Int = 0,
    val totalPhotos: Int = 0,
    val photoCategories: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null
)

/**
 * RestoreResult: Hasil proses pemulihan data dan foto ke perangkat lokal.
 */
data class RestoreResult(
    val success: Boolean,
    val restoredRecords: Int = 0,
    val restoredPhotos: Int = 0,
    val message: String = "",
    val details: String = ""
)

/**
 * Helper terpadu untuk Backup & Restore SEJAHTERA BERSAMA:
 * - Menyimpan seluruh basis data (Room Database).
 * - Mengemas seluruh file fisik foto (Mortalitas, OVK, Pakan, Panen, Bobot, Biaya, Dokumentasi, Profil).
 * - Memverifikasi integritas file ZIP secara internal.
 * - Memulihkan data dan foto ke perangkat baru dengan pembaruan path lokal yang otomatis dan valid.
 */
object BackupHelper {

    private const val TAG = "BackupHelper"
    private const val CURRENT_BACKUP_VERSION = 3

    private fun getBackupDir(context: Context): File =
        File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }

    private fun getReportsDir(context: Context): File =
        File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }

    /**
     * Membuka InputStream dari path file lokal, URI content://, atau file internal.
     */
    private fun openPhotoInputStream(context: Context, pathOrUri: String?): InputStream? {
        if (pathOrUri.isNullOrBlank()) return null
        val trimmed = pathOrUri.trim()

        try {
            // 1. Direct file path
            val directFile = File(trimmed)
            if (directFile.exists() && directFile.isFile && directFile.length() > 0L) {
                return FileInputStream(directFile)
            }

            // 2. Relative to filesDir
            val relFile = File(context.filesDir, trimmed.removePrefix("/"))
            if (relFile.exists() && relFile.isFile && relFile.length() > 0L) {
                return FileInputStream(relFile)
            }

            // 3. Inside filesDir/photos/
            val photosFile = File(File(context.filesDir, "photos"), trimmed.removePrefix("photos/").removePrefix("/"))
            if (photosFile.exists() && photosFile.isFile && photosFile.length() > 0L) {
                return FileInputStream(photosFile)
            }

            // 4. Content URI or File URI
            if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
                return context.contentResolver.openInputStream(Uri.parse(trimmed))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membuka input stream foto: $trimmed (${e.message})")
        }
        return null
    }

    /**
     * Membuat paket cadangan ZIP lengkap (Database + Seluruh File Foto Asli).
     */
    suspend fun createFullBackupPackage(context: Context, repository: FarmRepository): BackupPackageResult {
        val userId = UserSessionManager.getCurrentUserId(context)
        if (userId <= 0L) {
            return BackupPackageResult(success = false, errorMessage = "Akun belum aktif atau sesi telah berakhir.")
        }

        return try {
            val data = repository.getAllDirectData(userId)
            val farmProfile = (data["profile"] as? List<FarmProfileEntity>)?.firstOrNull()
            val farmName = farmProfile?.farmName?.ifBlank { "SEJAHTERA BERSAMA" } ?: "SEJAHTERA BERSAMA"
            val ownerName = farmProfile?.ownerName ?: ""

            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val readableDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val zipFile = File(getBackupDir(context), "BACKUP_SEJAHTERA_BERSAMA_$dateStamp.zip")

            // Kumpulkan semua referensi foto unik yang ada di seluruh tabel
            data class PhotoRef(
                val originalPath: String,
                val category: String,
                val prefix: String
            )

            val photoRefs = mutableListOf<PhotoRef>()

            // 1. Mortalitas
            (data["mortality_logs"] as? List<MortalityLogEntity>)?.forEach { m ->
                if (m.photoUri.isNotBlank()) photoRefs.add(PhotoRef(m.photoUri, "mortalitas", "mortalitas_${m.id}_h${m.ageDays}"))
            }
            // 2. Obat / Vaksin / Vitamin (OVK)
            (data["medicines"] as? List<MedicineEntity>)?.forEach { med ->
                val cat = when (med.category.lowercase(Locale.ROOT)) {
                    "vaksin" -> "vaksin"
                    "vitamin" -> "vitamin"
                    else -> "obat"
                }
                if (med.photoUri.isNotBlank()) photoRefs.add(PhotoRef(med.photoUri, "ovk", "${cat}_${med.id}"))
            }
            // 3. Pakan
            (data["feed_stocks"] as? List<FeedStockEntity>)?.forEach { f ->
                if (f.photoUri.isNotBlank()) photoRefs.add(PhotoRef(f.photoUri, "pakan", "pakan_${f.id}"))
            }
            // 4. Bobot
            (data["weight_samples"] as? List<WeightSampleEntity>)?.forEach { w ->
                if (w.photoUri.isNotBlank()) photoRefs.add(PhotoRef(w.photoUri, "bobot", "bobot_${w.id}_h${w.ageDays}"))
            }
            // 5. Panen
            (data["harvests"] as? List<HarvestEntity>)?.forEach { h ->
                if (h.photoUri.isNotBlank()) photoRefs.add(PhotoRef(h.photoUri, "panen", "panen_${h.id}"))
            }
            // 6. Biaya
            (data["expenses"] as? List<ExpenseEntity>)?.forEach { e ->
                if (e.photoUri.isNotBlank()) photoRefs.add(PhotoRef(e.photoUri, "biaya", "biaya_${e.id}"))
            }
            // 7. Kandang
            (data["coops"] as? List<CoopEntity>)?.forEach { c ->
                if (c.photoUri.isNotBlank()) photoRefs.add(PhotoRef(c.photoUri, "kandang", "kandang_${c.id}"))
            }
            // 8. Catatan Harian
            (data["daily_logs"] as? List<DailyLogEntity>)?.forEach { d ->
                if (d.photoUri.isNotBlank()) photoRefs.add(PhotoRef(d.photoUri, "harian", "harian_${d.id}_h${d.ageDays}"))
            }
            // 9. Galeri Dokumentasi Foto
            (data["photos"] as? List<PhotoEvidenceEntity>)?.forEach { p ->
                if (p.photoUri.isNotBlank()) photoRefs.add(PhotoRef(p.photoUri, "dokumentasi", "dok_${p.id}"))
                if (p.watermarkedUri.isNotBlank()) photoRefs.add(PhotoRef(p.watermarkedUri, "dokumentasi", "dok_wm_${p.id}"))
            }
            // 10. Profil Peternakan (Logo, Tanda Tangan)
            farmProfile?.let { p ->
                if (p.logoUri.isNotBlank()) photoRefs.add(PhotoRef(p.logoUri, "profil", "logo_farm"))
                if (p.signatureOwnerUri.isNotBlank()) photoRefs.add(PhotoRef(p.signatureOwnerUri, "profil", "ttd_peternak"))
                if (p.signaturePartnerUri.isNotBlank()) photoRefs.add(PhotoRef(p.signaturePartnerUri, "profil", "ttd_mitra"))
            }

            val warnings = mutableListOf<String>()
            val photoPathMapping = mutableMapOf<String, String>() // Original Path -> Relative ZIP Path (e.g. photos/mortalitas/xxx.jpg)
            val categoryStats = mutableMapOf<String, Int>()

            var photosArchived = 0
            var photosFailed = 0

            // Buat File ZIP
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val usedZipEntryNames = mutableSetOf<String>()

                // Langkah A: Masukkan semua file foto fisik ke dalam ZIP
                for ((index, ref) in photoRefs.withIndex()) {
                    if (photoPathMapping.containsKey(ref.originalPath)) {
                        continue // Sudah diproses sebelumnya (deduplikasi)
                    }

                    val inputStream = openPhotoInputStream(context, ref.originalPath)
                    if (inputStream != null) {
                        try {
                            // Tentukan ekstensi & nama file yang bersih
                            val originalName = File(ref.originalPath).name
                            val ext = when {
                                originalName.endsWith(".png", true) -> ".png"
                                originalName.endsWith(".webp", true) -> ".webp"
                                else -> ".jpg"
                            }
                            val cleanBase = originalName.substringBeforeLast(".").ifBlank { "${ref.prefix}_${index + 1}" }
                            var entryName = "photos/${ref.category}/${cleanBase}$ext"
                            var counter = 1
                            while (usedZipEntryNames.contains(entryName)) {
                                entryName = "photos/${ref.category}/${cleanBase}_$counter$ext"
                                counter++
                            }
                            usedZipEntryNames.add(entryName)

                            zos.putNextEntry(ZipEntry(entryName))
                            inputStream.copyTo(zos, bufferSize = 8192)
                            zos.closeEntry()

                            photoPathMapping[ref.originalPath] = entryName
                            photosArchived++
                            categoryStats[ref.category] = (categoryStats[ref.category] ?: 0) + 1
                        } catch (e: Exception) {
                            photosFailed++
                            warnings.add("Gagal mengompres foto ${ref.originalPath}: ${e.message}")
                        } finally {
                            try { inputStream.close() } catch (_: Exception) {}
                        }
                    } else {
                        photosFailed++
                        warnings.add("File fisik foto tidak ditemukan pada: ${ref.originalPath}")
                    }
                }

                // Helper untuk memetakan path lokal ke path relatif ZIP
                fun mapPath(original: String?): String {
                    if (original.isNullOrBlank()) return ""
                    return photoPathMapping[original] ?: original
                }

                // Helper Array JSON
                fun <T> arrayOfItems(items: List<T>, mapper: (T) -> JSONObject): JSONArray =
                    JSONArray().apply { items.forEach { put(mapper(it)) } }

                // Langkah B: Bangun database.json dengan referensi foto relatif yang portable
                val dbRoot = JSONObject().apply {
                    put("appName", "SEJAHTERA BERSAMA")
                    put("backupVersion", CURRENT_BACKUP_VERSION)
                    put("backupDate", readableDate)
                    put("userId", userId)
                }

                dbRoot.put("coops", arrayOfItems(data["coops"] as? List<CoopEntity> ?: emptyList()) { c -> JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("code", c.code); put("address", c.address); put("village", c.village); put("district", c.district); put("regency", c.regency); put("province", c.province)
                    put("areaSqm", c.areaSqm); put("lengthM", c.lengthM); put("widthM", c.widthM); put("capacity", c.capacity); put("coopType", c.coopType); put("ownerName", c.ownerName); put("phoneNumber", c.phoneNumber); put("notes", c.notes)
                    put("latitude", c.latitude ?: JSONObject.NULL); put("longitude", c.longitude ?: JSONObject.NULL); put("gpsAccuracy", c.gpsAccuracy ?: JSONObject.NULL); put("gpsTimestamp", c.gpsTimestamp ?: JSONObject.NULL)
                    put("photoUri", mapPath(c.photoUri))
                }})

                dbRoot.put("partners", arrayOfItems(data["partners"] as? List<PartnerEntity> ?: emptyList()) { p -> JSONObject().apply {
                    put("id", p.id); put("companyName", p.companyName); put("address", p.address); put("picName", p.picName); put("picPhone", p.picPhone); put("contractNumber", p.contractNumber); put("partnershipNumber", p.partnershipNumber); put("contractDate", p.contractDate)
                    put("contractPrice", p.contractPrice); put("chickPrice", p.chickPrice); put("feedPrice", p.feedPrice); put("liveBirdPrice", p.liveBirdPrice); put("bonusTerms", p.bonusTerms); put("penaltyTerms", p.penaltyTerms); put("notes", p.notes)
                }})

                dbRoot.put("cycles", arrayOfItems(data["cycles"] as? List<CycleEntity> ?: emptyList()) { c -> JSONObject().apply {
                    put("id", c.id); put("cycleNumber", c.cycleNumber); put("coopId", c.coopId); put("partnerId", c.partnerId); put("chickInDate", c.chickInDate); put("targetHarvestDate", c.targetHarvestDate); put("targetHarvestAgeDays", c.targetHarvestAgeDays); put("docCount", c.docCount); put("docStrain", c.docStrain); put("docType", c.docType); put("docPricePerHead", c.docPricePerHead); put("targetFcr", c.targetFcr); put("targetWeightKg", c.targetWeightKg); put("status", c.status); put("notes", c.notes)
                }})

                dbRoot.put("daily_logs", arrayOfItems(data["daily_logs"] as? List<DailyLogEntity> ?: emptyList()) { d -> JSONObject().apply {
                    put("id", d.id); put("cycleId", d.cycleId); put("date", d.date); put("ageDays", d.ageDays); put("morningPopulation", d.morningPopulation); put("afternoonPopulation", d.afternoonPopulation); put("deadCount", d.deadCount); put("cullCount", d.cullCount); put("outCount", d.outCount); put("feedGivenBags", d.feedGivenBags); put("feedGivenKg", d.feedGivenKg); put("feedRemainingKg", d.feedRemainingKg); put("waterIntakeLiters", d.waterIntakeLiters); put("medicineGiven", d.medicineGiven); put("vitaminGiven", d.vitaminGiven); put("vaccineGiven", d.vaccineGiven); put("chickenCondition", d.chickenCondition); put("tempCelsius", d.tempCelsius); put("humidityPercent", d.humidityPercent); put("weather", d.weather); put("litterCondition", d.litterCondition); put("notes", d.notes)
                    put("photoUri", mapPath(d.photoUri))
                }})

                dbRoot.put("mortality_logs", arrayOfItems(data["mortality_logs"] as? List<MortalityLogEntity> ?: emptyList()) { m -> JSONObject().apply {
                    put("id", m.id); put("cycleId", m.cycleId); put("date", m.date); put("ageDays", m.ageDays); put("count", m.count); put("cause", m.cause); put("locationBlock", m.locationBlock); put("notes", m.notes)
                    put("photoUri", mapPath(m.photoUri))
                }})

                dbRoot.put("feed_stocks", arrayOfItems(data["feed_stocks"] as? List<FeedStockEntity> ?: emptyList()) { f -> JSONObject().apply {
                    put("id", f.id); put("cycleId", f.cycleId); put("coopId", f.coopId); put("date", f.date); put("movementType", f.movementType); put("feedType", f.feedType); put("feedCode", f.feedCode); put("bags", f.bags); put("kgPerBag", f.kgPerBag); put("totalKg", f.totalKg); put("doNumber", f.doNumber); put("supplier", f.supplier); put("pricePerBag", f.pricePerBag); put("totalPrice", f.totalPrice); put("notes", f.notes)
                    put("photoUri", mapPath(f.photoUri))
                }})

                dbRoot.put("weight_samples", arrayOfItems(data["weight_samples"] as? List<WeightSampleEntity> ?: emptyList()) { w -> JSONObject().apply {
                    put("id", w.id); put("cycleId", w.cycleId); put("date", w.date); put("ageDays", w.ageDays); put("sampleCount", w.sampleCount); put("totalWeightKg", w.totalWeightKg); put("averageWeightGram", w.averageWeightGram); put("averageWeightKg", w.averageWeightKg); put("notes", w.notes)
                    put("photoUri", mapPath(w.photoUri))
                }})

                dbRoot.put("medicines", arrayOfItems(data["medicines"] as? List<MedicineEntity> ?: emptyList()) { m -> JSONObject().apply {
                    put("id", m.id); put("cycleId", m.cycleId); put("date", m.date); put("productName", m.productName); put("category", m.category); put("dose", m.dose); put("quantity", m.quantity); put("unit", m.unit); put("method", m.method); put("ageDays", m.ageDays); put("purpose", m.purpose); put("notes", m.notes)
                    put("photoUri", mapPath(m.photoUri))
                }})

                dbRoot.put("expenses", arrayOfItems(data["expenses"] as? List<ExpenseEntity> ?: emptyList()) { e -> JSONObject().apply {
                    put("id", e.id); put("cycleId", e.cycleId); put("transactionType", e.transactionType); put("date", e.date); put("category", e.category); put("expenseName", e.expenseName); put("quantity", e.quantity); put("unit", e.unit); put("unitPrice", e.unitPrice); put("totalAmount", e.totalAmount); put("proofNote", e.proofNote); put("notes", e.notes)
                    put("photoUri", mapPath(e.photoUri))
                }})

                dbRoot.put("harvests", arrayOfItems(data["harvests"] as? List<HarvestEntity> ?: emptyList()) { h -> JSONObject().apply {
                    put("id", h.id); put("cycleId", h.cycleId); put("harvestDate", h.harvestDate); put("ageDays", h.ageDays); put("populationBeforeHarvest", h.populationBeforeHarvest); put("birdCount", h.birdCount); put("totalWeightKg", h.totalWeightKg); put("averageWeightKg", h.averageWeightKg); put("pricePerKg", h.pricePerKg); put("totalRevenue", h.totalRevenue); put("doNumber", h.doNumber); put("buyerName", h.buyerName); put("notes", h.notes)
                    put("photoUri", mapPath(h.photoUri))
                }})

                dbRoot.put("photos", arrayOfItems(data["photos"] as? List<PhotoEvidenceEntity> ?: emptyList()) { p -> JSONObject().apply {
                    put("id", p.id); put("cycleId", p.cycleId); put("coopId", p.coopId); put("reportType", p.reportType); put("reportId", p.reportId); put("date", p.date); put("time", p.time); put("caption", p.caption)
                    put("photoUri", mapPath(p.photoUri))
                    put("watermarkedUri", mapPath(p.watermarkedUri))
                    put("latitude", p.latitude ?: JSONObject.NULL); put("longitude", p.longitude ?: JSONObject.NULL); put("gpsAccuracy", p.gpsAccuracy ?: JSONObject.NULL)
                }})

                dbRoot.put("dispatch_history", arrayOfItems(data["dispatch_history"] as? List<ReportDispatchHistoryEntity> ?: emptyList()) { h -> JSONObject().apply {
                    put("id", h.id); put("cycleId", h.cycleId); put("date", h.date); put("time", h.time); put("reportType", h.reportType); put("fileName", h.fileName); put("destination", h.destination); put("method", h.method); put("status", h.status); put("notes", h.notes)
                }})

                dbRoot.put("members", arrayOfItems(data["members"] as? List<MemberEntity> ?: emptyList()) { m -> JSONObject().apply {
                    put("id", m.id); put("name", m.name); put("memberNumber", m.memberNumber); put("phone", m.phone); put("notes", m.notes); put("isActive", m.isActive)
                }})

                dbRoot.put("profit_distributions", arrayOfItems(data["profit_distributions"] as? List<ProfitDistributionEntity> ?: emptyList()) { p -> JSONObject().apply {
                    put("id", p.id); put("cycleId", p.cycleId); put("coopId", p.coopId); put("date", p.date); put("period", p.period); put("totalRevenue", p.totalRevenue); put("totalExpense", p.totalExpense); put("totalDeduction", p.totalDeduction); put("netProfit", p.netProfit); put("memberCount", p.memberCount); put("amountPerMember", p.amountPerMember); put("totalDistributed", p.totalDistributed); put("roundingRemainder", p.roundingRemainder); put("status", p.status); put("notes", p.notes); put("memberDetailsJson", p.memberDetailsJson)
                }})

                dbRoot.put("profile", arrayOfItems(data["profile"] as? List<FarmProfileEntity> ?: emptyList()) { p -> JSONObject().apply {
                    put("farmName", p.farmName); put("slogan", p.slogan); put("ownerName", p.ownerName); put("address", p.address); put("village", p.village); put("district", p.district); put("regency", p.regency); put("province", p.province); put("phoneNumber", p.phoneNumber); put("email", p.email)
                    put("logoUri", mapPath(p.logoUri))
                    put("signatureOwnerUri", mapPath(p.signatureOwnerUri))
                    put("signaturePartnerUri", mapPath(p.signaturePartnerUri))
                }})

                // Tulis database.json ke ZIP
                zos.putNextEntry(ZipEntry("database.json"))
                zos.write(dbRoot.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Hitung total seluruh baris data
                var totalRecordsCount = 0
                val recordCounts = mutableMapOf<String, Int>()
                listOf(
                    "coops", "partners", "cycles", "daily_logs", "mortality_logs",
                    "feed_stocks", "weight_samples", "medicines", "expenses",
                    "harvests", "photos", "dispatch_history", "members",
                    "profit_distributions", "profile"
                ).forEach { key ->
                    val cnt = (data[key] as? List<*>)?.size ?: 0
                    recordCounts[key] = cnt
                    totalRecordsCount += cnt
                }

                // Langkah C: Buat manifest.json resmi
                val manifestRoot = JSONObject().apply {
                    put("appName", "SEJAHTERA BERSAMA")
                    put("backupVersion", CURRENT_BACKUP_VERSION)
                    put("backupDate", readableDate)
                    put("dateStamp", dateStamp)
                    put("userId", userId)
                    put("farmName", farmName)
                    put("ownerName", ownerName)
                    put("totalRecords", totalRecordsCount)
                    put("totalPhotos", photoRefs.size)
                    put("photosArchived", photosArchived)
                    put("photosFailed", photosFailed)
                    put("recordsSummary", JSONObject(recordCounts as Map<*, *>))
                    put("photoCategories", JSONObject(categoryStats as Map<*, *>))
                }

                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(manifestRoot.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            // Langkah D: Validasi Internal Integritas File ZIP
            val validationOk = validateZipArchive(zipFile, photoPathMapping.values.toList())
            if (!validationOk) {
                return BackupPackageResult(
                    success = false,
                    errorMessage = "Gagal memvalidasi integritas file ZIP cadangan yang dibuat."
                )
            }

            return BackupPackageResult(
                success = true,
                file = zipFile,
                totalRecords = (data.values.sumOf { (it as? List<*>)?.size ?: 0 }),
                totalPhotos = photoRefs.size,
                photosArchived = photosArchived,
                photosFailed = photosFailed,
                sizeBytes = zipFile.length(),
                warnings = warnings,
                categoriesCount = categoryStats
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuat paket cadangan: ${e.message}", e)
            return BackupPackageResult(
                success = false,
                errorMessage = "Terjadi kesalahan saat membuat paket cadangan: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Memverifikasi struktur dan konten file ZIP secara internal sebelum dibagikan atau dipulihkan.
     */
    private fun validateZipArchive(zipFile: File, expectedPhotoEntries: List<String>): Boolean {
        if (!zipFile.exists() || zipFile.length() == 0L) return false
        return try {
            ZipFile(zipFile).use { zf ->
                val manifestEntry = zf.getEntry("manifest.json")
                val dbEntry = zf.getEntry("database.json")
                if (manifestEntry == null || dbEntry == null) return false

                // Verifikasi manifest terbaca
                val manifestStr = zf.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
                val manifestObj = JSONObject(manifestStr)
                if (manifestObj.optString("appName") != "SEJAHTERA BERSAMA") return false

                // Verifikasi setiap file foto fisik yang tercatat memiliki ukuran > 0
                for (photoEntry in expectedPhotoEntries) {
                    val entry = zf.getEntry(photoEntry)
                    if (entry == null || entry.size == 0L) {
                        Log.w(TAG, "Validasi ZIP: Entry $photoEntry tidak ditemukan atau kosong")
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Validasi ZIP gagal: ${e.message}", e)
            false
        }
    }

    /**
     * Membaca dan menginspeksi file cadangan (baik .zip maupun legacy .json) sebelum pemulihan.
     */
    suspend fun inspectBackup(context: Context, sourceUri: Uri): BackupInspectionResult {
        return try {
            val tempFile = File(getBackupDir(context), "inspect_temp_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: return BackupInspectionResult(isValid = false, errorMessage = "Tidak dapat membaca file yang dipilih.")

            val isZip = try {
                ZipFile(tempFile).use { true }
            } catch (_: Exception) {
                false
            }

            if (isZip) {
                ZipFile(tempFile).use { zf ->
                    val manifestEntry = zf.getEntry("manifest.json")
                    if (manifestEntry == null) {
                        tempFile.delete()
                        return BackupInspectionResult(isValid = false, errorMessage = "Bukan berkas cadangan resmi SEJAHTERA BERSAMA (manifest.json tidak ditemukan).")
                    }
                    val jsonStr = zf.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
                    val obj = JSONObject(jsonStr)
                    if (obj.optString("appName") != "SEJAHTERA BERSAMA") {
                        tempFile.delete()
                        return BackupInspectionResult(isValid = false, errorMessage = "File cadangan ini bukan berasal dari aplikasi SEJAHTERA BERSAMA.")
                    }

                    val catMap = mutableMapOf<String, Int>()
                    obj.optJSONObject("photoCategories")?.let { catObj ->
                        catObj.keys().forEach { k -> catMap[k] = catObj.optInt(k) }
                    }

                    tempFile.delete()
                    BackupInspectionResult(
                        isValid = true,
                        isZip = true,
                        appName = obj.optString("appName"),
                        backupDate = obj.optString("backupDate"),
                        farmName = obj.optString("farmName", "SEJAHTERA BERSAMA"),
                        ownerName = obj.optString("ownerName"),
                        totalRecords = obj.optInt("totalRecords"),
                        totalPhotos = obj.optInt("totalPhotos"),
                        photoCategories = catMap
                    )
                }
            } else {
                // Coba parse sebagai legacy JSON
                val jsonStr = tempFile.bufferedReader().use { it.readText() }
                tempFile.delete()
                val obj = JSONObject(jsonStr)
                if (obj.optString("appName") != "SEJAHTERA BERSAMA") {
                    return BackupInspectionResult(isValid = false, errorMessage = "Format file tidak dikenali atau bukan cadangan SEJAHTERA BERSAMA.")
                }
                BackupInspectionResult(
                    isValid = true,
                    isZip = false,
                    appName = obj.optString("appName"),
                    backupDate = obj.optString("backupDate"),
                    farmName = "SEJAHTERA BERSAMA",
                    totalRecords = 0,
                    totalPhotos = 0
                )
            }
        } catch (e: Exception) {
            BackupInspectionResult(isValid = false, errorMessage = "Gagal memeriksa file: ${e.localizedMessage}")
        }
    }

    /**
     * Memulihkan data dan seluruh file foto dari paket cadangan ZIP (atau legacy JSON) ke perangkat aktif.
     */
    suspend fun restoreBackup(context: Context, repository: FarmRepository, sourceUri: Uri): RestoreResult {
        val userId = UserSessionManager.getCurrentUserId(context)
        if (userId <= 0L) return RestoreResult(success = false, message = "Akun belum aktif.")

        val tempBackupFile = File(getBackupDir(context), "restore_package_${System.currentTimeMillis()}")
        try {
            // Salin file input ke cache lokal untuk ekstraksi yang stabil
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempBackupFile).use { output -> input.copyTo(output) }
            } ?: return RestoreResult(success = false, message = "Gagal membuka file cadangan dari URI.")

            val isZip = try {
                ZipFile(tempBackupFile).use { true }
            } catch (_: Exception) {
                false
            }

            return if (isZip) {
                restoreFromZipFile(context, repository, tempBackupFile, userId)
            } else {
                val jsonString = tempBackupFile.bufferedReader().use { it.readText() }
                val ok = restoreFromJsonString(context, repository, jsonString, userId)
                if (ok) {
                    RestoreResult(success = true, message = "Data berhasil dipulihkan dari format JSON.")
                } else {
                    RestoreResult(success = false, message = "Gagal memulihkan database dari JSON.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memulihkan cadangan: ${e.message}", e)
            return RestoreResult(success = false, message = "Gagal memulihkan cadangan: ${e.localizedMessage}")
        } finally {
            try { if (tempBackupFile.exists()) tempBackupFile.delete() } catch (_: Exception) {}
        }
    }

    /**
     * Logika inti pemulihan paket ZIP:
     * 1. Ekstrak seluruh file gambar di folder photos/ ke direktori internal context.filesDir/photos/
     * 2. Perbarui seluruh referensi photoUri di database.json agar menunjuk ke file lokal fisik yang baru.
     * 3. Masukkan seluruh data ke Room Database.
     */
    private suspend fun restoreFromZipFile(
        context: Context,
        repository: FarmRepository,
        zipFile: File,
        userId: Long
    ): RestoreResult {
        var restoredPhotosCount = 0
        val basePhotosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
        var databaseJsonString = ""

        ZipFile(zipFile).use { zf ->
            val manifestEntry = zf.getEntry("manifest.json")
                ?: return RestoreResult(success = false, message = "Berkas tidak memiliki manifest.json valid.")
            val manifestObj = JSONObject(zf.getInputStream(manifestEntry).bufferedReader().use { it.readText() })
            if (manifestObj.optString("appName") != "SEJAHTERA BERSAMA") {
                return RestoreResult(success = false, message = "Berkas ini bukan cadangan resmi SEJAHTERA BERSAMA.")
            }

            val dbEntry = zf.getEntry("database.json")
                ?: return RestoreResult(success = false, message = "Berkas database.json tidak ditemukan di dalam paket ZIP.")
            databaseJsonString = zf.getInputStream(dbEntry).bufferedReader().use { it.readText() }

            // Ekstrak semua file foto fisik ke penyimpanan aplikasi HP baru
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith("photos/") && !entry.isDirectory) {
                    val relativeSubPath = entry.name.removePrefix("photos/")
                    val targetFile = File(basePhotosDir, relativeSubPath)
                    targetFile.parentFile?.mkdirs()

                    zf.getInputStream(entry).use { inStream ->
                        FileOutputStream(targetFile).use { outStream ->
                            inStream.copyTo(outStream, bufferSize = 8192)
                        }
                    }
                    if (targetFile.exists() && targetFile.length() > 0L) {
                        restoredPhotosCount++
                    }
                }
            }
        }

        // Helper untuk mengubah path relatif ZIP menjadi absolute path lokal di HP baru
        fun resolveLocalPhoto(relOrOldPath: String?): String {
            if (relOrOldPath.isNullOrBlank()) return ""
            val clean = relOrOldPath.trim()

            // Jika formatnya 'photos/category/file.jpg'
            if (clean.startsWith("photos/")) {
                val file = File(context.filesDir, clean)
                if (file.exists() && file.length() > 0L) return file.absolutePath
            }

            // Jika hanya nama file atau relatif
            val directInPhotos = File(File(context.filesDir, "photos"), clean.removePrefix("photos/").removePrefix("/"))
            if (directInPhotos.exists() && directInPhotos.length() > 0L) {
                return directInPhotos.absolutePath
            }

            val fileName = File(clean).name
            // Cari di seluruh subfolder photos/
            val subFolders = listOf("mortalitas", "ovk", "pakan", "panen", "bobot", "biaya", "kandang", "profil", "dokumentasi", "umum")
            for (sub in subFolders) {
                val candidate = File(File(basePhotosDir, sub), fileName)
                if (candidate.exists() && candidate.length() > 0L) {
                    return candidate.absolutePath
                }
            }

            return clean
        }

        // Pulihkan seluruh tabel Room Database dengan path foto baru
        val restoredOk = restoreDatabaseWithNewPhotoPaths(context, repository, databaseJsonString, userId, ::resolveLocalPhoto)
        return if (restoredOk) {
            RestoreResult(
                success = true,
                restoredPhotos = restoredPhotosCount,
                message = "Cadangan lengkap berhasil dipulihkan.",
                details = "$restoredPhotosCount foto bukti berhasil diekstrak dan ditautkan ke database HP baru."
            )
        } else {
            RestoreResult(success = false, message = "Gagal memproses dan menyimpan database.")
        }
    }

    private suspend fun restoreDatabaseWithNewPhotoPaths(
        context: Context,
        repository: FarmRepository,
        jsonString: String,
        userId: Long,
        resolvePhoto: (String?) -> String
    ): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (root.optString("appName") != "SEJAHTERA BERSAMA") return false

            repository.clearAllData(userId)

            val coopMap = mutableMapOf<Long, Long>()
            val partnerMap = mutableMapOf<Long, Long>()
            val cycleMap = mutableMapOf<Long, Long>()

            // 1. Kandang
            root.optJSONArray("coops")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val oldId = o.optLong("id")
                    val id = repository.saveCoop(
                        CoopEntity(
                            userId = userId,
                            name = o.optString("name"),
                            code = o.optString("code"),
                            address = o.optString("address"),
                            village = o.optString("village"),
                            district = o.optString("district"),
                            regency = o.optString("regency"),
                            province = o.optString("province"),
                            areaSqm = o.optDouble("areaSqm"),
                            lengthM = o.optDouble("lengthM"),
                            widthM = o.optDouble("widthM"),
                            capacity = o.optInt("capacity"),
                            coopType = o.optString("coopType"),
                            ownerName = o.optString("ownerName"),
                            phoneNumber = o.optString("phoneNumber"),
                            notes = o.optString("notes"),
                            latitude = o.optNullableDouble("latitude"),
                            longitude = o.optNullableDouble("longitude"),
                            gpsAccuracy = o.optNullableDouble("gpsAccuracy")?.toFloat(),
                            gpsTimestamp = o.optNullableLong("gpsTimestamp"),
                            photoUri = resolvePhoto(o.optString("photoUri"))
                        )
                    )
                    coopMap[oldId] = id
                }
            }

            // 2. Mitra
            root.optJSONArray("partners")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val oldId = o.optLong("id")
                    val id = repository.savePartner(
                        PartnerEntity(
                            userId = userId,
                            companyName = o.optString("companyName"),
                            address = o.optString("address"),
                            picName = o.optString("picName"),
                            picPhone = o.optString("picPhone"),
                            contractNumber = o.optString("contractNumber"),
                            partnershipNumber = o.optString("partnershipNumber"),
                            contractDate = o.optString("contractDate"),
                            contractPrice = o.optDouble("contractPrice"),
                            chickPrice = o.optDouble("chickPrice"),
                            feedPrice = o.optDouble("feedPrice"),
                            liveBirdPrice = o.optDouble("liveBirdPrice"),
                            bonusTerms = o.optString("bonusTerms"),
                            penaltyTerms = o.optString("penaltyTerms"),
                            notes = o.optString("notes")
                        )
                    )
                    partnerMap[oldId] = id
                }
            }

            // 3. Siklus
            root.optJSONArray("cycles")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val oldId = o.optLong("id")
                    val id = repository.saveCycle(
                        CycleEntity(
                            userId = userId,
                            cycleNumber = o.optString("cycleNumber"),
                            coopId = coopMap[o.optLong("coopId")] ?: 0L,
                            partnerId = partnerMap[o.optLong("partnerId")] ?: 0L,
                            chickInDate = o.optString("chickInDate"),
                            targetHarvestDate = o.optString("targetHarvestDate"),
                            targetHarvestAgeDays = o.optInt("targetHarvestAgeDays"),
                            docCount = o.optInt("docCount"),
                            docStrain = o.optString("docStrain"),
                            docType = o.optString("docType"),
                            docPricePerHead = o.optDouble("docPricePerHead"),
                            targetFcr = o.optDouble("targetFcr"),
                            targetWeightKg = o.optDouble("targetWeightKg"),
                            status = o.optString("status", "ACTIVE"),
                            notes = o.optString("notes")
                        )
                    )
                    cycleMap[oldId] = id
                }
            }

            // 4. Catatan Harian
            root.optJSONArray("daily_logs")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    repository.saveDailyLog(
                        DailyLogEntity(
                            cycleId = cycleMap[o.optLong("cycleId")] ?: 0L,
                            date = o.optString("date"),
                            ageDays = o.optInt("ageDays"),
                            morningPopulation = o.optInt("morningPopulation"),
                            afternoonPopulation = o.optInt("afternoonPopulation"),
                            deadCount = o.optInt("deadCount"),
                            cullCount = o.optInt("cullCount"),
                            outCount = o.optInt("outCount"),
                            feedGivenBags = o.optDouble("feedGivenBags"),
                            feedGivenKg = o.optDouble("feedGivenKg"),
                            feedRemainingKg = o.optDouble("feedRemainingKg"),
                            waterIntakeLiters = o.optDouble("waterIntakeLiters"),
                            medicineGiven = o.optString("medicineGiven"),
                            vitaminGiven = o.optString("vitaminGiven"),
                            vaccineGiven = o.optString("vaccineGiven"),
                            chickenCondition = o.optString("chickenCondition"),
                            tempCelsius = o.optDouble("tempCelsius"),
                            humidityPercent = o.optDouble("humidityPercent"),
                            weather = o.optString("weather"),
                            litterCondition = o.optString("litterCondition"),
                            notes = o.optString("notes"),
                            photoUri = resolvePhoto(o.optString("photoUri"))
                        )
                    )
                }
            }

            // 5. Mortalitas
            root.optJSONArray("mortality_logs")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    repository.insertMortality(
                        MortalityLogEntity(
                            cycleId = cycleMap[o.optLong("cycleId")] ?: 0L,
                            date = o.optString("date"),
                            ageDays = o.optInt("ageDays"),
                            count = o.optInt("count"),
                            cause = o.optString("cause"),
                            locationBlock = o.optString("locationBlock"),
                            photoUri = resolvePhoto(o.optString("photoUri")),
                            notes = o.optString("notes")
                        )
                    )
                }
            }

            // 6. Stok Pakan
            root.optJSONArray("feed_stocks")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    repository.insertFeedStock(
                        FeedStockEntity(
                            cycleId = cycleMap[o.optLong("cycleId")] ?: 0L,
                            coopId = coopMap[o.optLong("coopId")] ?: 0L,
                            date = o.optString("date"),
                            movementType = o.optString("movementType"),
                            feedType = o.optString("feedType"),
                            feedCode = o.optString("feedCode"),
                            bags = o.optDouble("bags"),
                            kgPerBag = o.optDouble("kgPerBag"),
                            totalKg = o.optDouble("totalKg"),
                            doNumber = o.optString("doNumber"),
                            supplier = o.optString("supplier"),
                            pricePerBag = o.optDouble("pricePerBag"),
                            totalPrice = o.optDouble("totalPrice"),
                            photoUri = resolvePhoto(o.optString("photoUri")),
                            notes = o.optString("notes")
                        )
                    )
                }
            }

            // 7. Sampling Bobot
            root.optJSONArray("weight_samples")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    repository.insertWeightSample(
                        WeightSampleEntity(
                            cycleId = cycleMap[o.optLong("cycleId")] ?: 0L,
                            date = o.optString("date"),
                            ageDays = o.optInt("ageDays"),
                            sampleCount = o.optInt("sampleCount"),
                            totalWeightKg = o.optDouble("totalWeightKg"),
                            averageWeightGram = o.optDouble("averageWeightGram"),
                            averageWeightKg = o.optDouble("averageWeightKg"),
                            photoUri = resolvePhoto(o.optString("photoUri")),
                            notes = o.optString("notes")
                        )
                    )
                }
            }

            // 8. Obat, Vaksin, Vitamin (OVK)
            root.optJSONArray("medicines")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    repository.insertMedicine(
                        MedicineEntity(
                            cycleId = cycleMap[o.optLong("cycleId")] ?: 0L,
                            date = o.optString("date"),
                            productName = o.optString("productName"),
                            category = o.optString("category"),
                            dose = o.optString("dose"),
                            quantity = o.optDouble("quantity"),
                            unit = o.optString("unit"),
                            method = o.optString("method"),
                            ageDays = o.optInt("ageDays"),
                            purpose = o.optString("purpose"),
                            photoUri = resolvePhoto(o.optString("photoUri")),
                            notes = o.optString("notes")
                        )
                    )
                }
            }

            // 9. Biaya & Operasional
            root.optJSONArray("expenses")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    repository.insertExpense(
                        ExpenseEntity(
                            cycleId = cycleMap[o.optLong("cycleId")] ?: 0L,
                            transactionType = o.optString("transactionType", "OUT"),
                            date = o.optString("date"),
                            category = o.optString("category"),
                            expenseName = o.optString("expenseName"),
                            quantity = o.optDouble("quantity"),
                            unit = o.optString("unit"),
                            unitPrice = o.optDouble("unitPrice"),
                            totalAmount = o.optDouble("totalAmount"),
                            proofNote = o.optString("proofNote"),
                            photoUri = resolvePhoto(o.optString("photoUri")),
                            notes = o.optString("notes")
                        )
                    )
                }
            }

            // 10. Panen
            root.optJSONArray("harvests")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    repository.insertHarvest(
                        HarvestEntity(
                            cycleId = cycleMap[o.optLong("cycleId")] ?: 0L,
                            harvestDate = o.optString("harvestDate"),
                            ageDays = o.optInt("ageDays"),
                            populationBeforeHarvest = o.optInt("populationBeforeHarvest"),
                            birdCount = o.optInt("birdCount"),
                            totalWeightKg = o.optDouble("totalWeightKg"),
                            averageWeightKg = o.optDouble("averageWeightKg"),
                            pricePerKg = o.optDouble("pricePerKg"),
                            totalRevenue = o.optDouble("totalRevenue"),
                            doNumber = o.optString("doNumber"),
                            buyerName = o.optString("buyerName"),
                            photoUri = resolvePhoto(o.optString("photoUri")),
                            notes = o.optString("notes")
                        )
                    )
                }
            }

            // 11. Bukti Foto Galeri
            root.optJSONArray("photos")?.let { a ->
                val dao = AppDatabase.getDatabase(context).farmDao()
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    dao.insertPhoto(
                        PhotoEvidenceEntity(
                            userId = userId,
                            cycleId = cycleMap[o.optLong("cycleId")] ?: 0L,
                            coopId = coopMap[o.optLong("coopId")] ?: 0L,
                            reportType = o.optString("reportType"),
                            reportId = o.optLong("reportId"),
                            date = o.optString("date"),
                            time = o.optString("time"),
                            caption = o.optString("caption"),
                            photoUri = resolvePhoto(o.optString("photoUri")),
                            watermarkedUri = resolvePhoto(o.optString("watermarkedUri")),
                            latitude = o.optNullableDouble("latitude"),
                            longitude = o.optNullableDouble("longitude"),
                            gpsAccuracy = o.optNullableDouble("gpsAccuracy")?.toFloat()
                        )
                    )
                }
            }

            // 12. Riwayat Pengiriman Laporan
            root.optJSONArray("dispatch_history")?.let { a ->
                val dao = AppDatabase.getDatabase(context).farmDao()
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    dao.insertDispatchHistory(
                        ReportDispatchHistoryEntity(
                            userId = userId,
                            cycleId = cycleMap[o.optLong("cycleId")] ?: 0L,
                            date = o.optString("date"),
                            time = o.optString("time"),
                            reportType = o.optString("reportType"),
                            fileName = o.optString("fileName"),
                            destination = o.optString("destination"),
                            method = o.optString("method"),
                            status = o.optString("status"),
                            notes = o.optString("notes")
                        )
                    )
                }
            }

            // 13. Data Anggota
            root.optJSONArray("members")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    repository.saveMember(
                        MemberEntity(
                            userId = userId,
                            name = o.optString("name"),
                            memberNumber = o.optString("memberNumber"),
                            phone = o.optString("phone"),
                            notes = o.optString("notes"),
                            isActive = o.optBoolean("isActive", true)
                        )
                    )
                }
            }

            // 14. Bagi Hasil
            root.optJSONArray("profit_distributions")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    repository.saveProfitDistribution(
                        ProfitDistributionEntity(
                            userId = userId,
                            cycleId = cycleMap[o.optLong("cycleId")] ?: 0L,
                            coopId = coopMap[o.optLong("coopId")] ?: 0L,
                            date = o.optString("date"),
                            period = o.optString("period"),
                            totalRevenue = o.optLong("totalRevenue"),
                            totalExpense = o.optLong("totalExpense"),
                            totalDeduction = o.optLong("totalDeduction"),
                            netProfit = o.optLong("netProfit"),
                            memberCount = o.optInt("memberCount"),
                            amountPerMember = o.optLong("amountPerMember"),
                            totalDistributed = o.optLong("totalDistributed"),
                            roundingRemainder = o.optLong("roundingRemainder"),
                            status = o.optString("status", "PEMBAGIAN SESUAI"),
                            notes = o.optString("notes"),
                            memberDetailsJson = o.optString("memberDetailsJson")
                        )
                    )
                }
            }

            // 15. Profil Peternakan
            root.optJSONArray("profile")?.optJSONObject(0)?.let { o ->
                repository.saveFarmProfile(
                    FarmProfileEntity(
                        id = userId,
                        userId = userId,
                        farmName = o.optString("farmName", "SEJAHTERA BERSAMA"),
                        slogan = o.optString("slogan", "REZEKI LANCAR, USAHA MAKMUR"),
                        ownerName = o.optString("ownerName"),
                        address = o.optString("address"),
                        village = o.optString("village"),
                        district = o.optString("district"),
                        regency = o.optString("regency"),
                        province = o.optString("province"),
                        phoneNumber = o.optString("phoneNumber"),
                        email = o.optString("email"),
                        logoUri = resolvePhoto(o.optString("logoUri")),
                        signatureOwnerUri = resolvePhoto(o.optString("signatureOwnerUri")),
                        signaturePartnerUri = resolvePhoto(o.optString("signaturePartnerUri"))
                    )
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengurai database JSON: ${e.message}", e)
            false
        }
    }

    private suspend fun restoreFromJsonString(
        context: Context,
        repository: FarmRepository,
        jsonString: String,
        userId: Long
    ): Boolean {
        return restoreDatabaseWithNewPhotoPaths(context, repository, jsonString, userId) { it ?: "" }
    }

    /**
     * Ekspor Catatan Harian ke format CSV untuk Microsoft Excel / Google Sheets.
     */
    suspend fun exportCsv(context: Context, cycle: CycleEntity, dailyLogs: List<DailyLogEntity>): File {
        val file = File(getReportsDir(context), "Laporan_Harian_Siklus_${cycle.cycleNumber}_${System.currentTimeMillis()}.csv")
        val builder = StringBuilder("Tanggal,Umur Hari,Populasi Pagi,Populasi Sore,Mati,Afkir,Pakan (Kg),Air (Liter),Kondisi,Catatan\n")
        dailyLogs.forEach { d ->
            fun esc(v: String) = "\"${v.replace("\"", "\"\"")}\""
            builder.append("${d.date},${d.ageDays},${d.morningPopulation},${d.afternoonPopulation},${d.deadCount},${d.cullCount},${d.feedGivenKg},${d.waterIntakeLiters},${esc(d.chickenCondition)},${esc(d.notes)}\n")
        }
        FileOutputStream(file).use { it.write(builder.toString().toByteArray(Charsets.UTF_8)) }
        return file
    }

    /**
     * Membagikan file (ZIP, PDF, CSV, JSON) menggunakan Android System Share Sheet.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Bagikan Berkas") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun JSONObject.optNullableDouble(name: String): Double? =
    if (isNull(name) || !has(name)) null else optDouble(name).takeIf { !it.isNaN() }

private fun JSONObject.optNullableLong(name: String): Long? =
    if (isNull(name) || !has(name)) null else optLong(name)
