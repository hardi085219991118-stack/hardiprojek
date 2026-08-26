package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.FeedGuideRules
import com.example.alarm.FarmAudioCatalog
import com.example.data.local.AppDatabase
import com.example.data.local.dao.FarmDao
import com.example.data.local.entity.*
import com.example.data.repository.FarmRepository
import com.example.util.BackupHelper
import com.example.util.FormatHelper
import com.example.util.PdfReportGenerator
import com.example.util.SecurityHelper
import com.example.util.UserSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FarmDao
    private lateinit var repository: FarmRepository
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.farmDao()
        repository = FarmRepository(dao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("SEJAHTERA BERSAMA", appName)
    }

    @Test
    fun `test Coop and Cycle CRUD in Room database`() = runBlocking {
        val coop = CoopEntity(
            userId = 1L,
            name = "Kandang Utama A",
            code = "KD-01",
            coopType = "Closed House",
            capacity = 10000,
            address = "Kec. Singosari, Malang",
            lengthM = 60.0,
            widthM = 10.0,
            ownerName = "Pak Hardi",
            phoneNumber = "08123456789",
            notes = "Kandang Uji Coba"
        )
        val coopId = repository.saveCoop(coop)
        assertTrue(coopId > 0)

        val coops = repository.getCoops(1L).first()
        assertEquals(1, coops.size)
        assertEquals("Kandang Utama A", coops[0].name)

        val cycle = CycleEntity(
            userId = 1L,
            coopId = coopId,
            partnerId = 0L,
            cycleNumber = "Siklus 01",
            chickInDate = "2026-08-01",
            docStrain = "Cobb 500",
            docType = "Broiler Super",
            docCount = 5000,
            docPricePerHead = 7500.0,
            targetFcr = 1.5,
            targetWeightKg = 2.0,
            status = "ACTIVE"
        )
        val cycleId = repository.saveCycle(cycle)
        assertTrue(cycleId > 0)

        val activeCycle = repository.getCycleById(cycleId, 1L)
        assertNotNull(activeCycle)
        assertEquals(5000, activeCycle?.docCount)
    }

    @Test
    fun `test Feed Stock and Logging in Room database`() = runBlocking {
        val feed = FeedStockEntity(
            cycleId = 1L,
            coopId = 1L,
            date = "2026-08-01",
            movementType = "IN",
            feedType = "Starter",
            feedCode = "BR-1",
            bags = 50.0,
            kgPerBag = 50.0,
            totalKg = 2500.0,
            pricePerBag = 450000.0,
            totalPrice = 22500000.0,
            supplier = "Malindo Feedmill",
            notes = "Pakan Masuk Awal"
        )
        val feedId = repository.insertFeedStock(feed)
        assertTrue(feedId > 0)

        val feeds = repository.getFeedStocks(1L).first()
        assertEquals(1, feeds.size)
        assertEquals(2500.0, feeds[0].totalKg, 0.01)
    }

    @Test
    fun `test Member and Profit Distribution CRUD`() = runBlocking {
        val member1 = MemberEntity(
            userId = 1L,
            name = "Budi Santoso",
            memberNumber = "ANG-001",
            phone = "08123456789",
            notes = "Ketua Kelompok",
            isActive = true
        )
        val member2 = MemberEntity(
            userId = 1L,
            name = "Siti Aminah",
            memberNumber = "ANG-002",
            phone = "08129876543",
            notes = "Anggota",
            isActive = true
        )
        repository.saveMember(member1)
        repository.saveMember(member2)

        val activeMembers = repository.getMembers(1L).first()
        assertEquals(2, activeMembers.size)

        // Hitung bagi hasil
        val netProfit = 10000000L // 10 Juta
        val memberCount = activeMembers.size
        val amountPerMember = netProfit / memberCount // 5 Juta
        val remainder = netProfit % memberCount

        val distribution = ProfitDistributionEntity(
            userId = 1L,
            cycleId = 1L,
            coopId = 1L,
            date = "2026-08-25",
            period = "Siklus 1 (Agustus 2026)",
            totalRevenue = 50000000L,
            totalExpense = 40000000L,
            totalDeduction = 0L,
            netProfit = netProfit,
            memberCount = memberCount,
            amountPerMember = amountPerMember,
            totalDistributed = amountPerMember * memberCount,
            roundingRemainder = remainder,
            status = "PEMBAGIAN SESUAI",
            notes = "Bagi hasil periode panen raya",
            memberDetailsJson = "[]"
        )
        val distId = repository.saveProfitDistribution(distribution)
        assertTrue(distId > 0)

        val history = repository.getProfitDistributions(1L).first()
        assertEquals(1, history.size)
        assertEquals(5000000L, history[0].amountPerMember)
        assertEquals(0L, history[0].roundingRemainder)
    }

    @Test
    fun `test Feed Guide Rules and Audio Catalog`() {
        val phaseStarter = FeedGuideRules.getPhaseDetailForAge(5)
        assertEquals("Fase Brooding / Pre-Starter", phaseStarter.phaseName)
        assertTrue(phaseStarter.feedType.contains("Prestarter") || phaseStarter.feedType.contains("Crumble"))

        val phaseFinisher = FeedGuideRules.getPhaseDetailForAge(25)
        assertEquals("Fase Finisher / Akhir", phaseFinisher.phaseName)
        assertTrue(phaseFinisher.feedType.contains("Finisher") || phaseFinisher.feedType.contains("Pellet"))

        val slots = FeedGuideRules.STANDARD_SLOTS
        assertEquals(4, slots.size)
        assertEquals("06:00", slots[0].time)
        assertEquals("11:00", slots[1].time)
        assertEquals("16:00", slots[2].time)
        assertEquals("20:00", slots[3].time)

        val sounds = FarmAudioCatalog.ALL_SOUNDS
        assertEquals(8, sounds.size)
    }

    @Test
    fun `test Security Password Hashing and Format Helpers`() {
        val rawPassword = "password123"
        val salt = SecurityHelper.generateSalt()
        val hashedPassword = SecurityHelper.hashPassword(rawPassword, salt)
        assertNotNull(hashedPassword)
        assertTrue(SecurityHelper.verifyPassword(rawPassword, salt, hashedPassword))
        assertFalse(SecurityHelper.verifyPassword("wrongpass", salt, hashedPassword))

        val formattedCurrency = FormatHelper.formatRupiah(1500000.0)
        assertTrue(formattedCurrency.contains("1.500.000") || formattedCurrency.contains("1,500,000"))

        val formattedNumber = FormatHelper.formatNumber(1250.5)
        assertTrue(formattedNumber.contains("1.250") || formattedNumber.contains("1,250") || formattedNumber.contains("1250"))
    }

    @Test
    fun `test Medicine CRUD and PDF generation validation`() = runBlocking {
        val med1 = MedicineEntity(
            cycleId = 1L,
            date = "2026-08-05",
            category = "Vaksin",
            productName = "ND-IB Live Clone",
            dose = "1 Dosis/Ekor",
            quantity = 1.0,
            unit = "Aplikasi",
            method = "Tetes Mata",
            ageDays = 4,
            purpose = "Kekebalan ND dan Gumboro",
            notes = "Vaksinasi pagi hari lancar",
            photoUri = ""
        )
        val med2 = MedicineEntity(
            cycleId = 1L,
            date = "2026-08-08",
            category = "Vitamin",
            productName = "Nopstress Organik",
            dose = "5 gr / 10L",
            quantity = 1.0,
            unit = "Aplikasi",
            method = "Air Minum",
            ageDays = 7,
            purpose = "Anti Stres Masa Brooding",
            notes = "Diberikan selama 3 hari berturut-turut",
            photoUri = "content://media/external/images/media/100"
        )
        val id1 = repository.insertMedicine(med1)
        val id2 = repository.insertMedicine(med2)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        val meds = repository.getMedicines(1L).first()
        assertEquals(2, meds.size)

        val sortedMeds = meds.sortedWith(compareBy<MedicineEntity> { it.date }.thenBy { it.ageDays }.thenBy { it.id })
        assertEquals("ND-IB Live Clone", sortedMeds[0].productName)
        assertEquals("Nopstress Organik", sortedMeds[1].productName)

        val profile = FarmProfileEntity(
            farmName = "CV Sejahtera Bersama",
            ownerName = "Hardi Mantangai",
            phoneNumber = "08123456789",
            address = "Malang, Jawa Timur"
        )
        val cycle = CycleEntity(
            id = 1L,
            userId = 1L,
            coopId = 1L,
            partnerId = 0L,
            cycleNumber = "Siklus 01",
            chickInDate = "2026-08-01",
            docCount = 5000
        )

        val validation = PdfReportGenerator.validateReportData(
            reportType = 8,
            profile = profile,
            coop = null,
            cycle = cycle,
            dailyLogs = emptyList(),
            mortalities = emptyList(),
            feedStocks = emptyList(),
            expenses = emptyList(),
            harvests = emptyList(),
            weights = emptyList(),
            medicines = sortedMeds
        )
        assertTrue(validation.isValid)
    }

    @Test
    fun `test Full ZIP Package Backup and Restore with Physical Photos`() = runBlocking {
        // Setup initial user session
        UserSessionManager.saveSession(context, 1L, "Peternak Uji", "peternak@sejahtera.com", "08123456789", "CV Sejahtera", "ACTIVE")

        // Create sample photo files on disk
        val photosDir = File(context.filesDir, "photos/mortalitas").apply { mkdirs() }
        val sampleMortalityPhoto = File(photosDir, "mortalitas_test_001.jpg").apply {
            writeBytes("DUMMY_IMAGE_BYTES_MORTALITAS_12345".toByteArray())
        }

        val ovkDir = File(context.filesDir, "photos/ovk").apply { mkdirs() }
        val sampleOvkPhoto = File(ovkDir, "vitamin_test_001.jpg").apply {
            writeBytes("DUMMY_IMAGE_BYTES_VITAMIN_67890".toByteArray())
        }

        // Insert database entities
        val coopId = repository.saveCoop(CoopEntity(userId = 1L, name = "Kandang Alpha", code = "K-01", address = "Malang"))
        val cycleId = repository.saveCycle(CycleEntity(userId = 1L, cycleNumber = "Siklus 01", coopId = coopId, partnerId = 0L, chickInDate = "2026-08-01", docCount = 5000))
        repository.insertMortality(MortalityLogEntity(cycleId = cycleId, date = "2026-08-02", ageDays = 2, count = 3, photoUri = sampleMortalityPhoto.absolutePath))
        repository.insertMedicine(MedicineEntity(cycleId = cycleId, date = "2026-08-03", productName = "Vitamin Broiler", category = "Vitamin", ageDays = 3, photoUri = sampleOvkPhoto.absolutePath))

        // Create Full ZIP Backup Package
        val backupResult = BackupHelper.createFullBackupPackage(context, repository)
        assertTrue(backupResult.success)
        assertNotNull(backupResult.file)
        assertTrue(backupResult.file!!.exists())
        assertTrue(backupResult.file!!.length() > 0)
        assertEquals(2, backupResult.photosArchived)
        assertEquals(0, backupResult.photosFailed)

        // Inspect ZIP before restore
        val inspection = BackupHelper.inspectBackup(context, Uri.fromFile(backupResult.file))
        assertTrue(inspection.isValid)
        assertTrue(inspection.isZip)
        assertEquals("SEJAHTERA BERSAMA", inspection.appName)
        assertEquals(2, inspection.totalPhotos)

        // Simulate new device: delete original photos and clear database
        sampleMortalityPhoto.delete()
        sampleOvkPhoto.delete()
        repository.clearAllData(1L)
        assertEquals(0, dao.getAllMortalityLogsDirect().size)
        assertEquals(0, dao.getAllMedicinesDirect().size)

        // Restore from the ZIP Package
        val restoreResult = BackupHelper.restoreBackup(context, repository, Uri.fromFile(backupResult.file))
        assertTrue(restoreResult.success)
        assertEquals(2, restoreResult.restoredPhotos)

        // Verify that photos are extracted to disk on the new device
        val restoredMortalities = dao.getAllMortalityLogsDirect()
        assertEquals(1, restoredMortalities.size)
        val restoredMortPhotoPath = restoredMortalities[0].photoUri
        assertTrue(restoredMortPhotoPath.isNotBlank())
        val restoredMortPhotoFile = File(restoredMortPhotoPath)
        assertTrue(restoredMortPhotoFile.exists())
        assertEquals("DUMMY_IMAGE_BYTES_MORTALITAS_12345", restoredMortPhotoFile.readText())

        val restoredMeds = dao.getAllMedicinesDirect()
        assertEquals(1, restoredMeds.size)
        val restoredMedPhotoPath = restoredMeds[0].photoUri
        assertTrue(restoredMedPhotoPath.isNotBlank())
        val restoredMedPhotoFile = File(restoredMedPhotoPath)
        assertTrue(restoredMedPhotoFile.exists())
        assertEquals("DUMMY_IMAGE_BYTES_VITAMIN_67890", restoredMedPhotoFile.readText())
    }
}
