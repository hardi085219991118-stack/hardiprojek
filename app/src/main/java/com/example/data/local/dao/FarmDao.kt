package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {

    // --- USERS ---
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE whatsappNumber = :phone LIMIT 1")
    suspend fun getUserByWhatsapp(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :identifier OR whatsappNumber = :identifier LIMIT 1")
    suspend fun getUserByIdentifier(identifier: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    // --- PHOTO EVIDENCES ---
    @Query("SELECT * FROM photo_evidences WHERE cycleId = :cycleId ORDER BY createdAt DESC")
    fun getPhotosByCycle(cycleId: Long): Flow<List<PhotoEvidenceEntity>>

    @Query("SELECT * FROM photo_evidences WHERE cycleId = :cycleId AND reportType = :reportType ORDER BY createdAt DESC")
    fun getPhotosByReportType(cycleId: Long, reportType: String): Flow<List<PhotoEvidenceEntity>>

    @Query("SELECT * FROM photo_evidences WHERE reportId = :reportId AND reportType = :reportType ORDER BY createdAt DESC")
    fun getPhotosByReportId(reportId: Long, reportType: String): Flow<List<PhotoEvidenceEntity>>

    @Query("SELECT * FROM photo_evidences WHERE cycleId = :cycleId ORDER BY createdAt DESC")
    suspend fun getPhotosByCycleDirect(cycleId: Long): List<PhotoEvidenceEntity>

    @Query("SELECT * FROM photo_evidences WHERE cycleId = :cycleId AND reportType = :reportType ORDER BY createdAt DESC")
    suspend fun getPhotosByReportTypeDirect(cycleId: Long, reportType: String): List<PhotoEvidenceEntity>

    @Query("SELECT * FROM photo_evidences ORDER BY createdAt DESC")
    fun getAllPhotos(): Flow<List<PhotoEvidenceEntity>>

    @Query("SELECT * FROM photo_evidences")
    suspend fun getAllPhotosDirect(): List<PhotoEvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEvidenceEntity): Long

    @Delete
    suspend fun deletePhoto(photo: PhotoEvidenceEntity)

    @Query("DELETE FROM photo_evidences WHERE id = :id")
    suspend fun deletePhotoById(id: Long)

    // --- REPORT DISPATCH HISTORY ---
    @Query("SELECT * FROM report_dispatches ORDER BY createdAt DESC")
    fun getAllDispatchHistory(): Flow<List<ReportDispatchHistoryEntity>>

    @Query("SELECT * FROM report_dispatches WHERE cycleId = :cycleId ORDER BY createdAt DESC")
    fun getDispatchHistoryByCycle(cycleId: Long): Flow<List<ReportDispatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispatchHistory(history: ReportDispatchHistoryEntity): Long

    @Delete
    suspend fun deleteDispatchHistory(history: ReportDispatchHistoryEntity)

    // --- COOPS ---
    @Query("SELECT * FROM coops ORDER BY createdAt DESC")
    fun getAllCoops(): Flow<List<CoopEntity>>

    @Query("SELECT * FROM coops WHERE id = :id LIMIT 1")
    suspend fun getCoopById(id: Long): CoopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoop(coop: CoopEntity): Long

    @Update
    suspend fun updateCoop(coop: CoopEntity)

    @Delete
    suspend fun deleteCoop(coop: CoopEntity)

    @Query("DELETE FROM coops WHERE id = :id")
    suspend fun deleteCoopById(id: Long)

    // --- PARTNERS ---
    @Query("SELECT * FROM partners ORDER BY companyName ASC")
    fun getAllPartners(): Flow<List<PartnerEntity>>

    @Query("SELECT * FROM partners WHERE id = :id LIMIT 1")
    suspend fun getPartnerById(id: Long): PartnerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: PartnerEntity): Long

    @Update
    suspend fun updatePartner(partner: PartnerEntity)

    @Delete
    suspend fun deletePartner(partner: PartnerEntity)

    // --- CYCLES ---
    @Query("SELECT * FROM cycles ORDER BY createdAt DESC")
    fun getAllCycles(): Flow<List<CycleEntity>>

    @Query("SELECT * FROM cycles WHERE status = 'ACTIVE' ORDER BY createdAt DESC LIMIT 1")
    fun getActiveCycle(): Flow<CycleEntity?>

    @Query("SELECT * FROM cycles WHERE id = :id LIMIT 1")
    suspend fun getCycleById(id: Long): CycleEntity?

    @Query("SELECT * FROM cycles WHERE coopId = :coopId ORDER BY createdAt DESC")
    fun getCyclesByCoop(coopId: Long): Flow<List<CycleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: CycleEntity): Long

    @Update
    suspend fun updateCycle(cycle: CycleEntity)

    @Delete
    suspend fun deleteCycle(cycle: CycleEntity)

    // --- DAILY LOGS ---
    @Query("SELECT * FROM daily_logs WHERE cycleId = :cycleId ORDER BY ageDays ASC")
    fun getDailyLogsByCycle(cycleId: Long): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE cycleId = :cycleId ORDER BY ageDays ASC")
    suspend fun getDailyLogsDirect(cycleId: Long): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE cycleId = :cycleId AND date = :date LIMIT 1")
    suspend fun getDailyLogByDate(cycleId: Long, date: String): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE id = :id LIMIT 1")
    suspend fun getDailyLogById(id: Long): DailyLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(log: DailyLogEntity): Long

    @Update
    suspend fun updateDailyLog(log: DailyLogEntity)

    @Delete
    suspend fun deleteDailyLog(log: DailyLogEntity)

    // --- MORTALITY LOGS ---
    @Query("SELECT * FROM mortality_logs WHERE cycleId = :cycleId ORDER BY date DESC, ageDays DESC")
    fun getMortalityLogsByCycle(cycleId: Long): Flow<List<MortalityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMortalityLog(log: MortalityLogEntity): Long

    @Delete
    suspend fun deleteMortalityLog(log: MortalityLogEntity)

    // --- FEED STOCKS ---
    @Query("SELECT * FROM feed_stocks WHERE cycleId = :cycleId ORDER BY date DESC")
    fun getFeedStocksByCycle(cycleId: Long): Flow<List<FeedStockEntity>>

    @Query("SELECT * FROM feed_stocks WHERE cycleId = :cycleId ORDER BY date DESC")
    suspend fun getFeedStocksDirect(cycleId: Long): List<FeedStockEntity>

    @Query("SELECT * FROM feed_stocks WHERE coopId = :coopId ORDER BY date DESC")
    fun getFeedStocksByCoop(coopId: Long): Flow<List<FeedStockEntity>>

    @Query("SELECT * FROM feed_stocks ORDER BY date DESC")
    fun getAllFeedStocks(): Flow<List<FeedStockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedStock(feed: FeedStockEntity): Long

    @Delete
    suspend fun deleteFeedStock(feed: FeedStockEntity)

    // --- WEIGHT SAMPLES ---
    @Query("SELECT * FROM weight_samples WHERE cycleId = :cycleId ORDER BY ageDays ASC")
    fun getWeightSamplesByCycle(cycleId: Long): Flow<List<WeightSampleEntity>>

    @Query("SELECT * FROM weight_samples WHERE cycleId = :cycleId ORDER BY ageDays ASC")
    suspend fun getWeightSamplesDirect(cycleId: Long): List<WeightSampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightSample(sample: WeightSampleEntity): Long

    @Delete
    suspend fun deleteWeightSample(sample: WeightSampleEntity)

    // --- MEDICINES ---
    @Query("SELECT * FROM medicines WHERE cycleId = :cycleId ORDER BY date DESC")
    fun getMedicinesByCycle(cycleId: Long): Flow<List<MedicineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(med: MedicineEntity): Long

    @Delete
    suspend fun deleteMedicine(med: MedicineEntity)

    // --- EXPENSES ---
    @Query("SELECT * FROM expenses WHERE cycleId = :cycleId ORDER BY date DESC")
    fun getExpensesByCycle(cycleId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE cycleId = :cycleId ORDER BY date DESC")
    suspend fun getExpensesDirect(cycleId: Long): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // --- HARVESTS ---
    @Query("SELECT * FROM harvests WHERE cycleId = :cycleId ORDER BY harvestDate DESC")
    fun getHarvestsByCycle(cycleId: Long): Flow<List<HarvestEntity>>

    @Query("SELECT * FROM harvests WHERE cycleId = :cycleId ORDER BY harvestDate DESC")
    suspend fun getHarvestsDirect(cycleId: Long): List<HarvestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHarvest(harvest: HarvestEntity): Long

    @Delete
    suspend fun deleteHarvest(harvest: HarvestEntity)

    // --- FARM PROFILE ---
    @Query("SELECT * FROM farm_profile WHERE userId = :userId LIMIT 1")
    fun getFarmProfile(userId: Long): Flow<FarmProfileEntity?>

    @Query("SELECT * FROM farm_profile WHERE userId = :userId LIMIT 1")
    suspend fun getFarmProfileDirect(userId: Long): FarmProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFarmProfile(profile: FarmProfileEntity)

    // --- BULK QUERIES FOR BACKUP & SUMMARY ---
    @Query("SELECT * FROM coops")
    suspend fun getAllCoopsDirect(): List<CoopEntity>

    @Query("SELECT * FROM partners")
    suspend fun getAllPartnersDirect(): List<PartnerEntity>

    @Query("SELECT * FROM cycles")
    suspend fun getAllCyclesDirect(): List<CycleEntity>

    @Query("SELECT * FROM daily_logs")
    suspend fun getAllDailyLogsDirect(): List<DailyLogEntity>

    @Query("SELECT * FROM mortality_logs")
    suspend fun getAllMortalityLogsDirect(): List<MortalityLogEntity>

    @Query("SELECT * FROM feed_stocks")
    suspend fun getAllFeedStocksDirect(): List<FeedStockEntity>

    @Query("SELECT * FROM weight_samples")
    suspend fun getAllWeightSamplesDirect(): List<WeightSampleEntity>

    @Query("SELECT * FROM medicines")
    suspend fun getAllMedicinesDirect(): List<MedicineEntity>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesDirect(): List<ExpenseEntity>

    @Query("SELECT * FROM harvests")
    suspend fun getAllHarvestsDirect(): List<HarvestEntity>

    @Query("SELECT * FROM report_dispatches")
    suspend fun getAllDispatchHistoryDirect(): List<ReportDispatchHistoryEntity>

    @Query("DELETE FROM coops")
    suspend fun clearCoops()

    @Query("DELETE FROM partners")
    suspend fun clearPartners()

    @Query("DELETE FROM cycles")
    suspend fun clearCycles()

    @Query("DELETE FROM daily_logs")
    suspend fun clearDailyLogs()

    @Query("DELETE FROM mortality_logs")
    suspend fun clearMortalityLogs()

    @Query("DELETE FROM feed_stocks")
    suspend fun clearFeedStocks()

    @Query("DELETE FROM weight_samples")
    suspend fun clearWeightSamples()

    @Query("DELETE FROM medicines")
    suspend fun clearMedicines()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM harvests")
    suspend fun clearHarvests()

    @Query("DELETE FROM photo_evidences")
    suspend fun clearPhotos()

    @Query("DELETE FROM report_dispatches")
    suspend fun clearDispatchHistory()
}
