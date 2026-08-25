package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.*
import com.example.data.repository.FarmRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Backup/restore data pengguna tanpa data demo. Foto tetap dirujuk melalui path lokalnya. */
object BackupHelper {
    private fun getBackupDir(context: Context): File = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }

    suspend fun createBackupJson(context: Context, repository: FarmRepository): File {
        val userId = UserSessionManager.getCurrentUserId(context)
        require(userId > 0L) { "Akun belum aktif." }
        val data = repository.getAllDirectData(userId)
        val root = JSONObject().apply {
            put("appName", "SEJAHTERA BERSAMA")
            put("backupVersion", 2)
            put("backupDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("userId", userId)
        }

        fun <T> arrayOfItems(items: List<T>, mapper: (T) -> JSONObject): JSONArray = JSONArray().apply { items.forEach { put(mapper(it)) } }

        root.put("coops", arrayOfItems(data["coops"] as? List<CoopEntity> ?: emptyList()) { c -> JSONObject().apply {
            put("id", c.id); put("name", c.name); put("code", c.code); put("address", c.address); put("village", c.village); put("district", c.district); put("regency", c.regency); put("province", c.province)
            put("areaSqm", c.areaSqm); put("lengthM", c.lengthM); put("widthM", c.widthM); put("capacity", c.capacity); put("coopType", c.coopType); put("ownerName", c.ownerName); put("phoneNumber", c.phoneNumber); put("notes", c.notes)
            put("latitude", c.latitude ?: JSONObject.NULL); put("longitude", c.longitude ?: JSONObject.NULL); put("gpsAccuracy", c.gpsAccuracy ?: JSONObject.NULL); put("gpsTimestamp", c.gpsTimestamp ?: JSONObject.NULL)
        }})
        root.put("partners", arrayOfItems(data["partners"] as? List<PartnerEntity> ?: emptyList()) { p -> JSONObject().apply {
            put("id", p.id); put("companyName", p.companyName); put("address", p.address); put("picName", p.picName); put("picPhone", p.picPhone); put("contractNumber", p.contractNumber); put("partnershipNumber", p.partnershipNumber); put("contractDate", p.contractDate)
            put("contractPrice", p.contractPrice); put("chickPrice", p.chickPrice); put("feedPrice", p.feedPrice); put("liveBirdPrice", p.liveBirdPrice); put("bonusTerms", p.bonusTerms); put("penaltyTerms", p.penaltyTerms); put("notes", p.notes)
        }})
        root.put("cycles", arrayOfItems(data["cycles"] as? List<CycleEntity> ?: emptyList()) { c -> JSONObject().apply {
            put("id", c.id); put("cycleNumber", c.cycleNumber); put("coopId", c.coopId); put("partnerId", c.partnerId); put("chickInDate", c.chickInDate); put("targetHarvestDate", c.targetHarvestDate); put("targetHarvestAgeDays", c.targetHarvestAgeDays); put("docCount", c.docCount); put("docStrain", c.docStrain); put("docType", c.docType); put("docPricePerHead", c.docPricePerHead); put("targetFcr", c.targetFcr); put("targetWeightKg", c.targetWeightKg); put("status", c.status); put("notes", c.notes)
        }})
        root.put("daily_logs", arrayOfItems(data["daily_logs"] as? List<DailyLogEntity> ?: emptyList()) { d -> JSONObject().apply {
            put("id", d.id); put("cycleId", d.cycleId); put("date", d.date); put("ageDays", d.ageDays); put("morningPopulation", d.morningPopulation); put("afternoonPopulation", d.afternoonPopulation); put("deadCount", d.deadCount); put("cullCount", d.cullCount); put("outCount", d.outCount); put("feedGivenBags", d.feedGivenBags); put("feedGivenKg", d.feedGivenKg); put("feedRemainingKg", d.feedRemainingKg); put("waterIntakeLiters", d.waterIntakeLiters); put("medicineGiven", d.medicineGiven); put("vitaminGiven", d.vitaminGiven); put("vaccineGiven", d.vaccineGiven); put("chickenCondition", d.chickenCondition); put("tempCelsius", d.tempCelsius); put("humidityPercent", d.humidityPercent); put("weather", d.weather); put("litterCondition", d.litterCondition); put("notes", d.notes)
        }})
        root.put("mortality_logs", arrayOfItems(data["mortality_logs"] as? List<MortalityLogEntity> ?: emptyList()) { m -> JSONObject().apply {
            put("id", m.id); put("cycleId", m.cycleId); put("date", m.date); put("ageDays", m.ageDays); put("count", m.count); put("cause", m.cause); put("locationBlock", m.locationBlock); put("photoUri", m.photoUri); put("notes", m.notes)
        }})
        root.put("feed_stocks", arrayOfItems(data["feed_stocks"] as? List<FeedStockEntity> ?: emptyList()) { f -> JSONObject().apply {
            put("id", f.id); put("cycleId", f.cycleId); put("coopId", f.coopId); put("date", f.date); put("movementType", f.movementType); put("feedType", f.feedType); put("feedCode", f.feedCode); put("bags", f.bags); put("kgPerBag", f.kgPerBag); put("totalKg", f.totalKg); put("doNumber", f.doNumber); put("supplier", f.supplier); put("pricePerBag", f.pricePerBag); put("totalPrice", f.totalPrice); put("photoUri", f.photoUri); put("notes", f.notes)
        }})
        root.put("weight_samples", arrayOfItems(data["weight_samples"] as? List<WeightSampleEntity> ?: emptyList()) { w -> JSONObject().apply {
            put("id", w.id); put("cycleId", w.cycleId); put("date", w.date); put("ageDays", w.ageDays); put("sampleCount", w.sampleCount); put("totalWeightKg", w.totalWeightKg); put("averageWeightGram", w.averageWeightGram); put("averageWeightKg", w.averageWeightKg); put("photoUri", w.photoUri); put("notes", w.notes)
        }})
        root.put("medicines", arrayOfItems(data["medicines"] as? List<MedicineEntity> ?: emptyList()) { m -> JSONObject().apply {
            put("id", m.id); put("cycleId", m.cycleId); put("date", m.date); put("productName", m.productName); put("category", m.category); put("dose", m.dose); put("quantity", m.quantity); put("unit", m.unit); put("method", m.method); put("ageDays", m.ageDays); put("purpose", m.purpose); put("photoUri", m.photoUri); put("notes", m.notes)
        }})
        root.put("expenses", arrayOfItems(data["expenses"] as? List<ExpenseEntity> ?: emptyList()) { e -> JSONObject().apply {
            put("id", e.id); put("cycleId", e.cycleId); put("transactionType", e.transactionType); put("date", e.date); put("category", e.category); put("expenseName", e.expenseName); put("quantity", e.quantity); put("unit", e.unit); put("unitPrice", e.unitPrice); put("totalAmount", e.totalAmount); put("proofNote", e.proofNote); put("photoUri", e.photoUri); put("notes", e.notes)
        }})
        root.put("harvests", arrayOfItems(data["harvests"] as? List<HarvestEntity> ?: emptyList()) { h -> JSONObject().apply {
            put("id", h.id); put("cycleId", h.cycleId); put("harvestDate", h.harvestDate); put("ageDays", h.ageDays); put("populationBeforeHarvest", h.populationBeforeHarvest); put("birdCount", h.birdCount); put("totalWeightKg", h.totalWeightKg); put("averageWeightKg", h.averageWeightKg); put("pricePerKg", h.pricePerKg); put("totalRevenue", h.totalRevenue); put("doNumber", h.doNumber); put("buyerName", h.buyerName); put("photoUri", h.photoUri); put("notes", h.notes)
        }})
        root.put("photos", arrayOfItems(data["photos"] as? List<PhotoEvidenceEntity> ?: emptyList()) { p -> JSONObject().apply {
            put("id", p.id); put("cycleId", p.cycleId); put("coopId", p.coopId); put("reportType", p.reportType); put("reportId", p.reportId); put("date", p.date); put("time", p.time); put("caption", p.caption); put("photoUri", p.photoUri); put("watermarkedUri", p.watermarkedUri); put("latitude", p.latitude ?: JSONObject.NULL); put("longitude", p.longitude ?: JSONObject.NULL); put("gpsAccuracy", p.gpsAccuracy ?: JSONObject.NULL)
        }})
        root.put("dispatch_history", arrayOfItems(data["dispatch_history"] as? List<ReportDispatchHistoryEntity> ?: emptyList()) { h -> JSONObject().apply {
            put("id", h.id); put("cycleId", h.cycleId); put("date", h.date); put("time", h.time); put("reportType", h.reportType); put("fileName", h.fileName); put("destination", h.destination); put("method", h.method); put("status", h.status); put("notes", h.notes)
        }})
        root.put("profile", arrayOfItems(data["profile"] as? List<FarmProfileEntity> ?: emptyList()) { p -> JSONObject().apply {
            put("farmName", p.farmName); put("slogan", p.slogan); put("ownerName", p.ownerName); put("address", p.address); put("village", p.village); put("district", p.district); put("regency", p.regency); put("province", p.province); put("phoneNumber", p.phoneNumber); put("email", p.email); put("logoUri", p.logoUri); put("signatureOwnerUri", p.signatureOwnerUri); put("signaturePartnerUri", p.signaturePartnerUri)
        }})

        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(getBackupDir(context), "BACKUP_SEJAHTERA_BERSAMA_$dateStamp.json")
        FileOutputStream(file).use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
        return file
    }

    suspend fun exportCsv(context: Context, cycle: CycleEntity, dailyLogs: List<DailyLogEntity>): File {
        val file = File(getBackupDir(context), "Laporan_Harian_Siklus_${cycle.id}_${System.currentTimeMillis()}.csv")
        val builder = StringBuilder("Tanggal,Umur Hari,Populasi Pagi,Populasi Sore,Mati,Afkir,Pakan (Kg),Air (Liter),Kondisi,Catatan\n")
        dailyLogs.forEach { d ->
            fun esc(v: String) = "\"${v.replace("\"", "\"\"")}\""
            builder.append("${d.date},${d.ageDays},${d.morningPopulation},${d.afternoonPopulation},${d.deadCount},${d.cullCount},${d.feedGivenKg},${d.waterIntakeLiters},${esc(d.chickenCondition)},${esc(d.notes)}\n")
        }
        FileOutputStream(file).use { it.write(builder.toString().toByteArray(Charsets.UTF_8)) }
        return file
    }

    suspend fun restoreFromJson(context: Context, repository: FarmRepository, jsonString: String): Boolean {
        val userId = UserSessionManager.getCurrentUserId(context)
        if (userId <= 0L) return false
        return try {
            val root = JSONObject(jsonString)
            require(root.optString("appName") == "SEJAHTERA BERSAMA")
            repository.clearAllData(userId)

            val coopMap = mutableMapOf<Long, Long>()
            val partnerMap = mutableMapOf<Long, Long>()
            val cycleMap = mutableMapOf<Long, Long>()

            root.optJSONArray("coops")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); val old=o.optLong("id"); val id=repository.saveCoop(CoopEntity(userId=userId,name=o.optString("name"),code=o.optString("code"),address=o.optString("address"),village=o.optString("village"),district=o.optString("district"),regency=o.optString("regency"),province=o.optString("province"),areaSqm=o.optDouble("areaSqm"),lengthM=o.optDouble("lengthM"),widthM=o.optDouble("widthM"),capacity=o.optInt("capacity"),coopType=o.optString("coopType"),ownerName=o.optString("ownerName"),phoneNumber=o.optString("phoneNumber"),notes=o.optString("notes"),latitude=o.optNullableDouble("latitude"),longitude=o.optNullableDouble("longitude"),gpsAccuracy=o.optNullableDouble("gpsAccuracy")?.toFloat(),gpsTimestamp=o.optNullableLong("gpsTimestamp"))); coopMap[old]=id } }
            root.optJSONArray("partners")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); val old=o.optLong("id"); val id=repository.savePartner(PartnerEntity(userId=userId,companyName=o.optString("companyName"),address=o.optString("address"),picName=o.optString("picName"),picPhone=o.optString("picPhone"),contractNumber=o.optString("contractNumber"),partnershipNumber=o.optString("partnershipNumber"),contractDate=o.optString("contractDate"),contractPrice=o.optDouble("contractPrice"),chickPrice=o.optDouble("chickPrice"),feedPrice=o.optDouble("feedPrice"),liveBirdPrice=o.optDouble("liveBirdPrice"),bonusTerms=o.optString("bonusTerms"),penaltyTerms=o.optString("penaltyTerms"),notes=o.optString("notes"))); partnerMap[old]=id } }
            root.optJSONArray("cycles")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); val old=o.optLong("id"); val id=repository.saveCycle(CycleEntity(userId=userId,cycleNumber=o.optString("cycleNumber"),coopId=coopMap[o.optLong("coopId")] ?: 0L,partnerId=partnerMap[o.optLong("partnerId")] ?: 0L,chickInDate=o.optString("chickInDate"),targetHarvestDate=o.optString("targetHarvestDate"),targetHarvestAgeDays=o.optInt("targetHarvestAgeDays"),docCount=o.optInt("docCount"),docStrain=o.optString("docStrain"),docType=o.optString("docType"),docPricePerHead=o.optDouble("docPricePerHead"),targetFcr=o.optDouble("targetFcr"),targetWeightKg=o.optDouble("targetWeightKg"),status=o.optString("status","ACTIVE"),notes=o.optString("notes"))); cycleMap[old]=id } }

            root.optJSONArray("daily_logs")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); repository.saveDailyLog(DailyLogEntity(cycleId=cycleMap[o.optLong("cycleId")] ?: 0L,date=o.optString("date"),ageDays=o.optInt("ageDays"),morningPopulation=o.optInt("morningPopulation"),afternoonPopulation=o.optInt("afternoonPopulation"),deadCount=o.optInt("deadCount"),cullCount=o.optInt("cullCount"),outCount=o.optInt("outCount"),feedGivenBags=o.optDouble("feedGivenBags"),feedGivenKg=o.optDouble("feedGivenKg"),feedRemainingKg=o.optDouble("feedRemainingKg"),waterIntakeLiters=o.optDouble("waterIntakeLiters"),medicineGiven=o.optString("medicineGiven"),vitaminGiven=o.optString("vitaminGiven"),vaccineGiven=o.optString("vaccineGiven"),chickenCondition=o.optString("chickenCondition"),tempCelsius=o.optDouble("tempCelsius"),humidityPercent=o.optDouble("humidityPercent"),weather=o.optString("weather"),litterCondition=o.optString("litterCondition"),notes=o.optString("notes"))) } }
            root.optJSONArray("mortality_logs")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); repository.insertMortality(MortalityLogEntity(cycleId=cycleMap[o.optLong("cycleId")] ?: 0L,date=o.optString("date"),ageDays=o.optInt("ageDays"),count=o.optInt("count"),cause=o.optString("cause"),locationBlock=o.optString("locationBlock"),photoUri=o.optString("photoUri"),notes=o.optString("notes"))) } }
            root.optJSONArray("feed_stocks")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); repository.insertFeedStock(FeedStockEntity(cycleId=cycleMap[o.optLong("cycleId")] ?: 0L,coopId=coopMap[o.optLong("coopId")] ?: 0L,date=o.optString("date"),movementType=o.optString("movementType"),feedType=o.optString("feedType"),feedCode=o.optString("feedCode"),bags=o.optDouble("bags"),kgPerBag=o.optDouble("kgPerBag"),totalKg=o.optDouble("totalKg"),doNumber=o.optString("doNumber"),supplier=o.optString("supplier"),pricePerBag=o.optDouble("pricePerBag"),totalPrice=o.optDouble("totalPrice"),photoUri=o.optString("photoUri"),notes=o.optString("notes"))) } }
            root.optJSONArray("weight_samples")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); repository.insertWeightSample(WeightSampleEntity(cycleId=cycleMap[o.optLong("cycleId")] ?: 0L,date=o.optString("date"),ageDays=o.optInt("ageDays"),sampleCount=o.optInt("sampleCount"),totalWeightKg=o.optDouble("totalWeightKg"),averageWeightGram=o.optDouble("averageWeightGram"),averageWeightKg=o.optDouble("averageWeightKg"),photoUri=o.optString("photoUri"),notes=o.optString("notes"))) } }
            root.optJSONArray("medicines")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); repository.insertMedicine(MedicineEntity(cycleId=cycleMap[o.optLong("cycleId")] ?: 0L,date=o.optString("date"),productName=o.optString("productName"),category=o.optString("category"),dose=o.optString("dose"),quantity=o.optDouble("quantity"),unit=o.optString("unit"),method=o.optString("method"),ageDays=o.optInt("ageDays"),purpose=o.optString("purpose"),photoUri=o.optString("photoUri"),notes=o.optString("notes"))) } }
            root.optJSONArray("expenses")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); repository.insertExpense(ExpenseEntity(cycleId=cycleMap[o.optLong("cycleId")] ?: 0L,transactionType=o.optString("transactionType","OUT"),date=o.optString("date"),category=o.optString("category"),expenseName=o.optString("expenseName"),quantity=o.optDouble("quantity"),unit=o.optString("unit"),unitPrice=o.optDouble("unitPrice"),totalAmount=o.optDouble("totalAmount"),proofNote=o.optString("proofNote"),photoUri=o.optString("photoUri"),notes=o.optString("notes"))) } }
            root.optJSONArray("harvests")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); repository.insertHarvest(HarvestEntity(cycleId=cycleMap[o.optLong("cycleId")] ?: 0L,harvestDate=o.optString("harvestDate"),ageDays=o.optInt("ageDays"),populationBeforeHarvest=o.optInt("populationBeforeHarvest"),birdCount=o.optInt("birdCount"),totalWeightKg=o.optDouble("totalWeightKg"),averageWeightKg=o.optDouble("averageWeightKg"),pricePerKg=o.optDouble("pricePerKg"),totalRevenue=o.optDouble("totalRevenue"),doNumber=o.optString("doNumber"),buyerName=o.optString("buyerName"),photoUri=o.optString("photoUri"),notes=o.optString("notes"))) } }
            root.optJSONArray("photos")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); databaseInsertPhoto(context, PhotoEvidenceEntity(userId=userId,cycleId=cycleMap[o.optLong("cycleId")] ?: 0L,coopId=coopMap[o.optLong("coopId")] ?: 0L,reportType=o.optString("reportType"),reportId=o.optLong("reportId"),date=o.optString("date"),time=o.optString("time"),caption=o.optString("caption"),photoUri=o.optString("photoUri"),watermarkedUri=o.optString("watermarkedUri"),latitude=o.optNullableDouble("latitude"),longitude=o.optNullableDouble("longitude"),gpsAccuracy=o.optNullableDouble("gpsAccuracy")?.toFloat())) } }
            root.optJSONArray("dispatch_history")?.let { a -> for (i in 0 until a.length()) { val o=a.getJSONObject(i); databaseInsertDispatch(context, ReportDispatchHistoryEntity(userId=userId,cycleId=cycleMap[o.optLong("cycleId")] ?: 0L,date=o.optString("date"),time=o.optString("time"),reportType=o.optString("reportType"),fileName=o.optString("fileName"),destination=o.optString("destination"),method=o.optString("method"),status=o.optString("status"),notes=o.optString("notes"))) } }
            root.optJSONArray("profile")?.optJSONObject(0)?.let { o -> repository.saveFarmProfile(FarmProfileEntity(id=userId,userId=userId,farmName=o.optString("farmName","SEJAHTERA BERSAMA"),slogan=o.optString("slogan","REZEKI LANCAR, USAHA MAKMUR"),ownerName=o.optString("ownerName"),address=o.optString("address"),village=o.optString("village"),district=o.optString("district"),regency=o.optString("regency"),province=o.optString("province"),phoneNumber=o.optString("phoneNumber"),email=o.optString("email"),logoUri=o.optString("logoUri"),signatureOwnerUri=o.optString("signatureOwnerUri"),signaturePartnerUri=o.optString("signaturePartnerUri"))) }
            true
        } catch (_: Exception) { false }
    }

    private suspend fun databaseInsertPhoto(context: Context, photo: PhotoEvidenceEntity) {
        FarmRepository(com.example.data.local.AppDatabase.getDatabase(context).farmDao()).let { repo ->
            // Photo insertion is exposed through DAO; use the local database directly here.
            com.example.data.local.AppDatabase.getDatabase(context).farmDao().insertPhoto(photo)
        }
    }

    private suspend fun databaseInsertDispatch(context: Context, history: ReportDispatchHistoryEntity) {
        com.example.data.local.AppDatabase.getDatabase(context).farmDao().insertDispatchHistory(history)
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Bagikan Berkas") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply { type = mimeType; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun JSONObject.optNullableDouble(name: String): Double? = if (isNull(name) || !has(name)) null else optDouble(name).takeIf { !it.isNaN() }
private fun JSONObject.optNullableLong(name: String): Long? = if (isNull(name) || !has(name)) null else optLong(name)
