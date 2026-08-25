package com.example.data.repository

import com.example.data.local.dao.FarmDao
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FarmRepository(private val dao: FarmDao) {

    fun getCoops(userId: Long): Flow<List<CoopEntity>> = dao.getAllCoops().map { list -> list.filter { it.userId == userId } }
    fun getPartners(userId: Long): Flow<List<PartnerEntity>> = dao.getAllPartners().map { list -> list.filter { it.userId == userId } }
    fun getCycles(userId: Long): Flow<List<CycleEntity>> = dao.getAllCycles().map { list -> list.filter { it.userId == userId } }
    fun getFarmProfile(userId: Long): Flow<FarmProfileEntity?> = dao.getFarmProfile(userId)

    suspend fun getCoopById(id: Long, userId: Long): CoopEntity? = dao.getCoopById(id)?.takeIf { it.userId == userId }
    suspend fun getPartnerById(id: Long, userId: Long): PartnerEntity? = dao.getPartnerById(id)?.takeIf { it.userId == userId }
    suspend fun getCycleById(id: Long, userId: Long): CycleEntity? = dao.getCycleById(id)?.takeIf { it.userId == userId }
    suspend fun getFarmProfileDirect(userId: Long): FarmProfileEntity? = dao.getFarmProfileDirect(userId)

    fun getDailyLogs(cycleId: Long): Flow<List<DailyLogEntity>> = dao.getDailyLogsByCycle(cycleId)
    fun getMortalityLogs(cycleId: Long): Flow<List<MortalityLogEntity>> = dao.getMortalityLogsByCycle(cycleId)
    fun getFeedStocks(cycleId: Long): Flow<List<FeedStockEntity>> = dao.getFeedStocksByCycle(cycleId)
    fun getWeightSamples(cycleId: Long): Flow<List<WeightSampleEntity>> = dao.getWeightSamplesByCycle(cycleId)
    fun getMedicines(cycleId: Long): Flow<List<MedicineEntity>> = dao.getMedicinesByCycle(cycleId)
    fun getExpenses(cycleId: Long): Flow<List<ExpenseEntity>> = dao.getExpensesByCycle(cycleId)
    fun getHarvests(cycleId: Long): Flow<List<HarvestEntity>> = dao.getHarvestsByCycle(cycleId)
    fun getPhotos(cycleId: Long): Flow<List<PhotoEvidenceEntity>> = dao.getPhotosByCycle(cycleId)

    suspend fun saveCoop(coop: CoopEntity): Long = if (coop.id == 0L) dao.insertCoop(coop) else { dao.updateCoop(coop); coop.id }
    suspend fun deleteCoop(coop: CoopEntity) = dao.deleteCoop(coop)
    suspend fun savePartner(partner: PartnerEntity): Long = if (partner.id == 0L) dao.insertPartner(partner) else { dao.updatePartner(partner); partner.id }
    suspend fun deletePartner(partner: PartnerEntity) = dao.deletePartner(partner)
    suspend fun saveCycle(cycle: CycleEntity): Long = if (cycle.id == 0L) dao.insertCycle(cycle) else { dao.updateCycle(cycle); cycle.id }
    suspend fun updateCycle(cycle: CycleEntity) = dao.updateCycle(cycle)
    suspend fun deleteCycle(cycle: CycleEntity) = dao.deleteCycle(cycle)
    suspend fun saveDailyLog(log: DailyLogEntity): Long = if (log.id == 0L) dao.insertDailyLog(log) else { dao.updateDailyLog(log); log.id }
    suspend fun getDailyLogByDate(cycleId: Long, date: String): DailyLogEntity? = dao.getDailyLogByDate(cycleId, date)
    suspend fun deleteDailyLog(log: DailyLogEntity) = dao.deleteDailyLog(log)

    suspend fun insertMortality(log: MortalityLogEntity): Long = dao.insertMortalityLog(log)
    suspend fun deleteMortality(log: MortalityLogEntity) = dao.deleteMortalityLog(log)
    suspend fun insertFeedStock(feed: FeedStockEntity): Long = dao.insertFeedStock(feed)
    suspend fun deleteFeedStock(feed: FeedStockEntity) = dao.deleteFeedStock(feed)
    suspend fun insertWeightSample(sample: WeightSampleEntity): Long = dao.insertWeightSample(sample)
    suspend fun deleteWeightSample(sample: WeightSampleEntity) = dao.deleteWeightSample(sample)
    suspend fun insertMedicine(med: MedicineEntity): Long = dao.insertMedicine(med)
    suspend fun deleteMedicine(med: MedicineEntity) = dao.deleteMedicine(med)
    suspend fun insertExpense(expense: ExpenseEntity): Long = dao.insertExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = dao.deleteExpense(expense)
    suspend fun insertHarvest(harvest: HarvestEntity): Long = dao.insertHarvest(harvest)
    suspend fun deleteHarvest(harvest: HarvestEntity) = dao.deleteHarvest(harvest)
    suspend fun saveFarmProfile(profile: FarmProfileEntity) = dao.saveFarmProfile(profile)

    suspend fun getAllDirectData(userId: Long): Map<String, Any> {
        val coops = dao.getAllCoopsDirect().filter { it.userId == userId }
        val partners = dao.getAllPartnersDirect().filter { it.userId == userId }
        val cycles = dao.getAllCyclesDirect().filter { it.userId == userId }
        val cycleIds = cycles.map { it.id }.toSet()
        return mapOf(
            "coops" to coops,
            "partners" to partners,
            "cycles" to cycles,
            "daily_logs" to dao.getAllDailyLogsDirect().filter { it.cycleId in cycleIds },
            "mortality_logs" to dao.getAllMortalityLogsDirect().filter { it.cycleId in cycleIds },
            "feed_stocks" to dao.getAllFeedStocksDirect().filter { it.cycleId in cycleIds },
            "weight_samples" to dao.getAllWeightSamplesDirect().filter { it.cycleId in cycleIds },
            "medicines" to dao.getAllMedicinesDirect().filter { it.cycleId in cycleIds },
            "expenses" to dao.getAllExpensesDirect().filter { it.cycleId in cycleIds },
            "harvests" to dao.getAllHarvestsDirect().filter { it.cycleId in cycleIds },
            "photos" to dao.getAllPhotosDirect().filter { it.userId == userId },
            "dispatch_history" to dao.getAllDispatchHistoryDirect().filter { it.userId == userId },
            "profile" to listOfNotNull(dao.getFarmProfileDirect(userId))
        )
    }

    suspend fun clearAllData(userId: Long) {
        val cycleIds = dao.getAllCyclesDirect().filter { it.userId == userId }.map { it.id }.toSet()
        dao.getAllCoopsDirect().filter { it.userId == userId }.forEach { dao.deleteCoop(it) }
        dao.getAllPartnersDirect().filter { it.userId == userId }.forEach { dao.deletePartner(it) }
        dao.getAllCyclesDirect().filter { it.userId == userId }.forEach { dao.deleteCycle(it) }
        dao.getAllDailyLogsDirect().filter { it.cycleId in cycleIds }.forEach { dao.deleteDailyLog(it) }
        dao.getAllMortalityLogsDirect().filter { it.cycleId in cycleIds }.forEach { dao.deleteMortalityLog(it) }
        dao.getAllFeedStocksDirect().filter { it.cycleId in cycleIds }.forEach { dao.deleteFeedStock(it) }
        dao.getAllWeightSamplesDirect().filter { it.cycleId in cycleIds }.forEach { dao.deleteWeightSample(it) }
        dao.getAllMedicinesDirect().filter { it.cycleId in cycleIds }.forEach { dao.deleteMedicine(it) }
        dao.getAllExpensesDirect().filter { it.cycleId in cycleIds }.forEach { dao.deleteExpense(it) }
        dao.getAllHarvestsDirect().filter { it.cycleId in cycleIds }.forEach { dao.deleteHarvest(it) }
        dao.getAllPhotosDirect().filter { it.userId == userId }.forEach { dao.deletePhoto(it) }
        dao.getAllDispatchHistoryDirect().filter { it.userId == userId }.forEach { dao.deleteDispatchHistory(it) }
        cycleIds.forEach { dao.clearFeedSchedulesByCycle(it) }
    }

    // Feed Schedules
    fun getFeedSchedulesByDate(cycleId: Long, date: String): Flow<List<FeedScheduleLogEntity>> =
        dao.getFeedSchedulesByCycleAndDate(cycleId, date)

    suspend fun getFeedSchedulesByDateDirect(cycleId: Long, date: String): List<FeedScheduleLogEntity> =
        dao.getFeedSchedulesByCycleAndDateDirect(cycleId, date)

    fun getAllFeedSchedules(cycleId: Long): Flow<List<FeedScheduleLogEntity>> =
        dao.getAllFeedSchedulesByCycle(cycleId)

    suspend fun insertFeedSchedule(schedule: FeedScheduleLogEntity): Long =
        dao.insertFeedSchedule(schedule)

    suspend fun insertFeedSchedules(schedules: List<FeedScheduleLogEntity>): List<Long> =
        dao.insertFeedSchedules(schedules)

    suspend fun updateFeedSchedule(schedule: FeedScheduleLogEntity) =
        dao.updateFeedSchedule(schedule)

    suspend fun deleteFeedSchedule(schedule: FeedScheduleLogEntity) =
        dao.deleteFeedSchedule(schedule)
}

