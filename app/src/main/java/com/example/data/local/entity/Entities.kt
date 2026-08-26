package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val businessName: String,
    val whatsappNumber: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val status: String = "MENUNGGU_VERIFIKASI", // MENUNGGU_VERIFIKASI, AKTIF, DITANGGUHKAN, DINONAKTIFKAN
    val isEmailVerified: Boolean = false,
    val isWhatsappVerified: Boolean = false,
    val emailVerificationCode: String = "",
    val whatsappVerificationCode: String = "",
    val verificationCodeExpiry: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "photo_evidences")
data class PhotoEvidenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val cycleId: Long = 0,
    val coopId: Long = 0,
    val reportType: String, // MORTALITY, DAILY_LOG, FEED, MEDICINE, HARVEST, EXPENSE, COOP, PARTNERSHIP
    val reportId: Long = 0,
    val date: String,
    val time: String,
    val caption: String = "",
    val photoUri: String,
    val watermarkedUri: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gpsAccuracy: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "report_dispatches")
data class ReportDispatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val cycleId: Long = 0,
    val date: String,
    val time: String,
    val reportType: String,
    val fileName: String,
    val destination: String,
    val method: String, // WhatsApp, Email, System Share
    val status: String = "Berhasil disiapkan / dibagikan",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "coops")
data class CoopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val name: String,
    val code: String,
    val address: String,
    val village: String = "",
    val district: String = "",
    val regency: String = "",
    val province: String = "",
    val areaSqm: Double = 0.0,
    val lengthM: Double = 0.0,
    val widthM: Double = 0.0,
    val capacity: Int = 0,
    val coopType: String = "", // Open House, Semi-Closed, Closed House
    val ownerName: String = "",
    val phoneNumber: String = "",
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gpsAccuracy: Float? = null,
    val gpsTimestamp: Long? = null,
    val photoUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "partners")
data class PartnerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val companyName: String,
    val address: String = "",
    val picName: String = "",
    val picPhone: String = "",
    val contractNumber: String = "",
    val partnershipNumber: String = "",
    val contractDate: String = "",
    val contractPrice: Double = 0.0,
    val chickPrice: Double = 0.0,      // Harga DOC
    val feedPrice: Double = 0.0,       // Harga Pakan / kg
    val liveBirdPrice: Double = 0.0,   // Harga Ayam / kg
    val bonusTerms: String = "",       // Ketentuan bonus (FCR, Mortalitas)
    val penaltyTerms: String = "",     // Ketentuan potongan
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val cycleNumber: String,          // e.g. "Siklus 001 - Agustus 2026"
    val coopId: Long,
    val partnerId: Long,
    val chickInDate: String,          // YYYY-MM-DD
    val targetHarvestDate: String = "",
    val targetHarvestAgeDays: Int = 0,
    val docCount: Int,                // Jumlah DOC masuk
    val docStrain: String = "", // Cobb 500, Ross 308, Hubbard, dll
    val docType: String = "",
    val docPricePerHead: Double = 0.0,
    val targetFcr: Double = 0.0,
    val targetWeightKg: Double = 0.0,
    val status: String = "ACTIVE",    // ACTIVE, HARVESTED, CLOSED
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val date: String,                 // YYYY-MM-DD
    val ageDays: Int,
    val morningPopulation: Int = 0,
    val afternoonPopulation: Int = 0,
    val deadCount: Int = 0,
    val cullCount: Int = 0,           // Afkir
    val outCount: Int = 0,            // Keluar / Terjual
    val feedGivenBags: Double = 0.0,
    val feedGivenKg: Double = 0.0,
    val feedRemainingKg: Double = 0.0,
    val waterIntakeLiters: Double = 0.0,
    val medicineGiven: String = "",
    val vitaminGiven: String = "",
    val vaccineGiven: String = "",
    val chickenCondition: String = "",
    val tempCelsius: Double = 0.0,
    val humidityPercent: Double = 0.0,
    val weather: String = "",    // Cerah, Panas, Hujan, Mendung
    val litterCondition: String = "",
    val notes: String = "",
    val photoUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mortality_logs")
data class MortalityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val date: String,
    val ageDays: Int,
    val count: Int,
    val cause: String = "", // Penyakit, Lemah, Terinjak, Cuaca panas, Gangguan pernapasan, Gangguan pencernaan, Lainnya
    val locationBlock: String = "",
    val photoUri: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "feed_stocks")
data class FeedStockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val coopId: Long,
    val date: String,
    val movementType: String,         // "IN" (Pakan Masuk) atau "OUT" (Pakan Terpakai)
    val feedType: String,             // Pre-Starter, Starter, Grower, Finisher
    val feedCode: String = "",
    val bags: Double = 0.0,
    val kgPerBag: Double = 0.0,
    val totalKg: Double = 0.0,
    val doNumber: String = "",        // Nomor Surat Jalan
    val supplier: String = "",
    val pricePerBag: Double = 0.0,
    val totalPrice: Double = 0.0,
    val photoUri: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weight_samples")
data class WeightSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val date: String,
    val ageDays: Int,
    val sampleCount: Int,             // Jumlah ayam yang ditimbang
    val totalWeightKg: Double,        // Total bobot sampel (kg)
    val averageWeightGram: Double,    // Rata-rata bobot (gram)
    val averageWeightKg: Double,      // Rata-rata bobot (kg)
    val photoUri: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val date: String,
    val productName: String,
    val category: String,             // Vaksin, Vitamin, Obat, Elektrolit, Desinfektan, Lainnya
    val dose: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",          // ml, gram, sachet, liter
    val method: String = "", // Air Minum, Pakan, Suntik, Tetes Mata, Semprot
    val ageDays: Int = 0,
    val purpose: String = "",
    val photoUri: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val date: String,
    val transactionType: String = "OUT", // OUT = Pengeluaran/Debit, IN = Uang Masuk/Kredit
    val category: String,             // DOC, Pakan, Obat, Vitamin, Vaksin, Listrik, Air, Tenaga kerja, Transportasi, Perawatan kandang, Peralatan, Sekam, Desinfektan, Biaya lain
    val expenseName: String,
    val quantity: Double = 1.0,
    val unit: String = "",
    val unitPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val proofNote: String = "",
    val photoUri: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "harvests")
data class HarvestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val harvestDate: String,
    val ageDays: Int,
    val populationBeforeHarvest: Int = 0,
    val birdCount: Int,               // Jumlah ekor panen
    val totalWeightKg: Double,        // Total bobot hidup (kg)
    val averageWeightKg: Double,      // Bobot rata-rata (kg)
    val pricePerKg: Double,           // Harga per kg
    val totalRevenue: Double,         // Total nilai panen (Rp)
    val doNumber: String = "",        // Nomor Surat Jalan
    val buyerName: String = "",       // Nama Pembeli / Perusahaan Mitra
    val photoUri: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "farm_profile")
data class FarmProfileEntity(
    @PrimaryKey val id: Long = 1,
    val userId: Long = 0,
    val farmName: String = "SEJAHTERA BERSAMA",
    val slogan: String = "REZEKI LANCAR, USAHA MAKMUR",
    val ownerName: String = "",
    val address: String = "",
    val village: String = "",
    val district: String = "",
    val regency: String = "",
    val province: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val logoUri: String = "",
    val signatureOwnerUri: String = "",
    val signaturePartnerUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "feed_schedule_logs")
data class FeedScheduleLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val cycleId: Long = 0,
    val coopId: Long = 0,
    val date: String,                  // YYYY-MM-DD
    val scheduledTime: String,         // "06:00", "11:00", "16:00", "20:00"
    val slotName: String = "",         // "Pagi", "Siang", "Sore", "Malam"
    val instruction: String = "",      // Instruksi resmi Bab 4 PDF
    val actualTime: String = "",       // Jam aktual saat ditandai/dicatat (HH:mm)
    val ageDays: Int = 0,              // Umur ayam hari ini
    val phase: String = "",            // Pre-Starter / Starter / Finisher
    val feedType: String = "",         // Jenis pakan panduan
    val status: String = "BELUM",      // "SELESAI", "DITUNDA", "DILEWATI", "BELUM"
    val feedAmountKg: Double = 0.0,    // Jumlah kg pakan jika dicatat
    val snoozeMinutes: Int = 0,
    val snoozeUntilEpoch: Long = 0L,
    val notes: String = "",
    val isManual: Boolean = false,     // false = jadwal asli Bab 4 PDF, true = manual
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val name: String,
    val memberNumber: String = "",
    val phone: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "profit_distributions")
data class ProfitDistributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val cycleId: Long = 0,
    val coopId: Long = 0,
    val date: String,                  // YYYY-MM-DD
    val period: String = "",           // Periode pembagian
    val totalRevenue: Long = 0L,       // Total Hasil (Rp)
    val totalExpense: Long = 0L,       // Biaya (Rp)
    val totalDeduction: Long = 0L,     // Potongan (Rp)
    val netProfit: Long = 0L,          // Hasil Bersih = Total Hasil - Biaya - Potongan
    val memberCount: Int = 0,          // Jumlah Anggota
    val amountPerMember: Long = 0L,    // Hasil Per Anggota = Hasil Bersih ÷ Jumlah Anggota
    val totalDistributed: Long = 0L,   // Total Pembagian = Hasil Per Anggota × Jumlah Anggota
    val roundingRemainder: Long = 0L,  // Sisa Pembulatan / Selisih = Hasil Bersih - Total Pembagian
    val status: String = "PEMBAGIAN SESUAI", // "PEMBAGIAN SESUAI" atau "PEMBAGIAN MEMILIKI SISA PEMBULATAN"
    val notes: String = "",            // Keterangan / Alasan Perubahan
    val memberDetailsJson: String = "",// JSON array data anggota & nilai pembagian
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class DistributionMemberItem(
    val memberId: Long,
    val memberName: String,
    val memberNumber: String = "",
    val amount: Long
)


