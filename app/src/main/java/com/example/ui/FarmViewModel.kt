package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.FarmRepository
import com.example.util.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ringkasan dashboard dihitung hanya dari data yang benar-benar tersimpan. */
data class DashboardSummary(
    val activeCycle: CycleEntity? = null,
    val coop: CoopEntity? = null,
    val partner: PartnerEntity? = null,
    val ageDays: Int = 0,
    val initialPop: Int = 0,
    val currentPop: Int = 0,
    val totalDead: Int = 0,
    val totalCulls: Int = 0,
    val mortalityPercent: Double = 0.0,
    val survivalRatePercent: Double = 0.0,
    val totalFeedInKg: Double = 0.0,
    val totalFeedUsedKg: Double = 0.0,
    val remainingFeedKg: Double = 0.0,
    val dailyFeedAvgGramPerHead: Double = 0.0,
    val latestAvgWeightGram: Double = 0.0,
    val latestAvgWeightKg: Double = 0.0,
    val fcr: Double = 0.0,
    val adgGram: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalHarvestKg: Double = 0.0,
    val totalHarvestRevenue: Double = 0.0,
    val totalHarvestBirds: Int = 0,
    val estimatedProfit: Double = 0.0,
    val warnings: List<String> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class FarmViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = FarmRepository(database.farmDao())

    private val userScopeId = MutableStateFlow<Long?>(
        UserSessionManager.getUserSession(application).userId.takeIf {
            UserSessionManager.isLoggedIn(application) && it > 0L
        }
    )

    /** Dipanggil setelah login/verifikasi/logout agar seluruh data berganti sesuai akun aktif. */
    fun refreshUserScope() {
        val context = getApplication<Application>()
        userScopeId.value = UserSessionManager.getUserSession(context).userId.takeIf {
            UserSessionManager.isLoggedIn(context) && it > 0L
        }
        selectedCycleId.value = null
    }

    val farmProfile: StateFlow<FarmProfileEntity?> = userScopeId.flatMapLatest { userId ->
        if (userId == null) flowOf(null) else repository.getFarmProfile(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val coops: StateFlow<List<CoopEntity>> = userScopeId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else repository.getCoops(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partners: StateFlow<List<PartnerEntity>> = userScopeId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else repository.getPartners(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cycles: StateFlow<List<CycleEntity>> = userScopeId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else repository.getCycles(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserId: StateFlow<Long?> = userScopeId.asStateFlow()
    val selectedCycleId = MutableStateFlow<Long?>(null)

    val currentCycle = combine(cycles, selectedCycleId) { cycleList, selectedId ->
        if (selectedId != null) cycleList.find { it.id == selectedId }
        else cycleList.find { it.status == "ACTIVE" } ?: cycleList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentCoop = combine(coops, currentCycle) { coopList, cycle ->
        if (cycle != null) coopList.find { it.id == cycle.coopId } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentPartner = combine(partners, currentCycle) { partnerList, cycle ->
        if (cycle != null) partnerList.find { it.id == cycle.partnerId } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyLogs: StateFlow<List<DailyLogEntity>> = currentCycle.flatMapLatest { cycle ->
        if (cycle != null) repository.getDailyLogs(cycle.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mortalityLogs: StateFlow<List<MortalityLogEntity>> = currentCycle.flatMapLatest { cycle ->
        if (cycle != null) repository.getMortalityLogs(cycle.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedStocks: StateFlow<List<FeedStockEntity>> = currentCycle.flatMapLatest { cycle ->
        if (cycle != null) repository.getFeedStocks(cycle.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightSamples: StateFlow<List<WeightSampleEntity>> = currentCycle.flatMapLatest { cycle ->
        if (cycle != null) repository.getWeightSamples(cycle.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicines: StateFlow<List<MedicineEntity>> = currentCycle.flatMapLatest { cycle ->
        if (cycle != null) repository.getMedicines(cycle.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = currentCycle.flatMapLatest { cycle ->
        if (cycle != null) repository.getExpenses(cycle.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val harvests: StateFlow<List<HarvestEntity>> = currentCycle.flatMapLatest { cycle ->
        if (cycle != null) repository.getHarvests(cycle.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photos: StateFlow<List<PhotoEvidenceEntity>> = currentCycle.flatMapLatest { cycle ->
        if (cycle != null) repository.getPhotos(cycle.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        currentCycle, currentCoop, currentPartner, dailyLogs, mortalityLogs,
        feedStocks, weightSamples, expenses, harvests
    ) { args ->
        val cycle = args[0] as? CycleEntity
        val coop = args[1] as? CoopEntity
        val partner = args[2] as? PartnerEntity
        @Suppress("UNCHECKED_CAST") val logs = args[3] as List<DailyLogEntity>
        @Suppress("UNCHECKED_CAST") val mortality = args[4] as List<MortalityLogEntity>
        @Suppress("UNCHECKED_CAST") val feed = args[5] as List<FeedStockEntity>
        @Suppress("UNCHECKED_CAST") val weights = args[6] as List<WeightSampleEntity>
        @Suppress("UNCHECKED_CAST") val exp = args[7] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST") val harvest = args[8] as List<HarvestEntity>

        if (cycle == null) return@combine DashboardSummary()

        val initialPop = cycle.docCount.coerceAtLeast(0)
        val totalDead = if (mortality.isNotEmpty()) mortality.sumOf { it.count } else logs.sumOf { it.deadCount }
        val totalCulls = logs.sumOf { it.cullCount }
        val totalOut = logs.sumOf { it.outCount }
        val totalHarvestBirds = harvest.sumOf { it.birdCount }
        val currentPop = (initialPop - totalDead - totalCulls - totalOut - totalHarvestBirds).coerceAtLeast(0)

        val mortalityPct = if (initialPop > 0) totalDead * 100.0 / initialPop else 0.0
        val survivalPct = if (initialPop > 0) currentPop * 100.0 / initialPop else 0.0

        val totalFeedIn = feed.filter { it.movementType == "IN" }.sumOf { it.totalKg }
        val totalFeedOut = feed.filter { it.movementType == "OUT" }.sumOf { it.totalKg }
        val totalFeedDaily = logs.sumOf { it.feedGivenKg }
        val totalFeedUsed = if (totalFeedOut > 0.0) totalFeedOut else totalFeedDaily
        val remainingFeed = (totalFeedIn - totalFeedOut).coerceAtLeast(0.0)

        val latestLog = logs.maxByOrNull { it.ageDays }
        val latestAge = latestLog?.ageDays ?: 0
        val latestWeight = weights.maxByOrNull { it.ageDays }
        val latestWeightGram = latestWeight?.averageWeightGram ?: 0.0
        val latestWeightKg = latestWeight?.averageWeightKg ?: 0.0
        val dailyFeedAvgGram = if (currentPop > 0 && latestLog != null) latestLog.feedGivenKg * 1000.0 / currentPop else 0.0

        val firstWeight = weights.minByOrNull { it.ageDays }
        val adg = if (weights.size >= 2 && firstWeight != null && latestWeight != null) {
            val first = firstWeight
            val last = latestWeight
            val days = last.ageDays - first.ageDays
            if (days > 0) (last.averageWeightGram - first.averageWeightGram) / days else 0.0
        } else if (weights.size == 1 && latestAge > 0) {
            latestWeightGram / latestAge
        } else 0.0

        // FCR hanya dihitung jika data pakan dan bobot cukup.
        val liveBiomass = currentPop * latestWeightKg
        val fcr = if (totalFeedUsed > 0.0 && liveBiomass > 0.0) totalFeedUsed / liveBiomass else 0.0

        val operatingExpenses = exp.filter { it.transactionType == "OUT" }.sumOf { it.totalAmount }
        val docCost = if (cycle.docPricePerHead > 0.0) initialPop * cycle.docPricePerHead else 0.0
        val totalExpenses = operatingExpenses + docCost
        val harvestKg = harvest.sumOf { it.totalWeightKg }
        val harvestRevenue = harvest.sumOf { it.totalRevenue }
        val estimatedRevenue = if (harvestRevenue > 0.0) harvestRevenue
        else if (latestWeightKg > 0.0 && currentPop > 0 && (partner?.liveBirdPrice ?: 0.0) > 0.0)
            currentPop * latestWeightKg * (partner?.liveBirdPrice ?: 0.0)
        else 0.0
        val estimatedProfit = if (estimatedRevenue > 0.0 || totalExpenses > 0.0) estimatedRevenue - totalExpenses else 0.0

        val warnings = mutableListOf<String>()
        if (totalFeedIn > 0.0 && remainingFeed <= 0.0 && currentPop > 0) warnings += "⚠️ Stok pakan tercatat habis. Periksa persediaan."
        if (mortalityPct >= 4.0 && initialPop > 0) warnings += "⚠️ Mortalitas kumulatif mencapai ${String.format(Locale.US, "%.2f", mortalityPct)}%. Periksa kesehatan dan kondisi kandang."
        if (fcr > 0.0 && cycle.targetFcr > 0.0 && fcr > cycle.targetFcr) warnings += "⚠️ FCR realisasi (${String.format(Locale.US, "%.2f", fcr)}) berada di atas target (${String.format(Locale.US, "%.2f", cycle.targetFcr)})."
        if (cycle.targetHarvestAgeDays > 0 && latestAge >= cycle.targetHarvestAgeDays - 3 && cycle.status == "ACTIVE") warnings += "ℹ️ Umur ayam mendekati target panen (${cycle.targetHarvestAgeDays} hari)."

        DashboardSummary(
            activeCycle = cycle,
            coop = coop,
            partner = partner,
            ageDays = latestAge,
            initialPop = initialPop,
            currentPop = currentPop,
            totalDead = totalDead,
            totalCulls = totalCulls,
            mortalityPercent = mortalityPct,
            survivalRatePercent = survivalPct,
            totalFeedInKg = totalFeedIn,
            totalFeedUsedKg = totalFeedUsed,
            remainingFeedKg = remainingFeed,
            dailyFeedAvgGramPerHead = dailyFeedAvgGram,
            latestAvgWeightGram = latestWeightGram,
            latestAvgWeightKg = latestWeightKg,
            fcr = fcr,
            adgGram = adg,
            totalExpenses = totalExpenses,
            totalHarvestKg = harvestKg,
            totalHarvestRevenue = harvestRevenue,
            totalHarvestBirds = totalHarvestBirds,
            estimatedProfit = estimatedProfit,
            warnings = warnings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    private fun scopedUserId(): Long = userScopeId.value ?: 0L

    fun selectCycle(cycleId: Long) { selectedCycleId.value = cycleId }

    fun saveCoop(coop: CoopEntity, onComplete: (Long) -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        val id = repository.saveCoop(coop.copy(userId = scopedUserId()))
        onComplete(id)
    }
    fun deleteCoop(coop: CoopEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteCoop(coop) }

    fun savePartner(partner: PartnerEntity, onComplete: (Long) -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        val id = repository.savePartner(partner.copy(userId = scopedUserId()))
        onComplete(id)
    }
    fun deletePartner(partner: PartnerEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deletePartner(partner) }

    fun saveCycle(cycle: CycleEntity, onComplete: (Long) -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        val id = repository.saveCycle(cycle.copy(userId = scopedUserId()))
        selectedCycleId.value = id
        onComplete(id)
    }
    fun deleteCycle(cycle: CycleEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteCycle(cycle) }

    fun saveDailyLog(log: DailyLogEntity, onComplete: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        repository.saveDailyLog(log)
        onComplete()
    }
    fun deleteDailyLog(log: DailyLogEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteDailyLog(log) }

    fun saveMortality(log: MortalityLogEntity, onComplete: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) { repository.insertMortality(log); onComplete() }
    fun deleteMortality(log: MortalityLogEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteMortality(log) }
    fun saveFeedStock(feed: FeedStockEntity, onComplete: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) { repository.insertFeedStock(feed); onComplete() }
    fun deleteFeedStock(feed: FeedStockEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteFeedStock(feed) }
    fun saveWeightSample(sample: WeightSampleEntity, onComplete: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) { repository.insertWeightSample(sample); onComplete() }
    fun deleteWeightSample(sample: WeightSampleEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteWeightSample(sample) }
    fun saveMedicine(med: MedicineEntity, onComplete: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) { repository.insertMedicine(med); onComplete() }
    fun deleteMedicine(med: MedicineEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteMedicine(med) }
    fun saveExpense(exp: ExpenseEntity, onComplete: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) { repository.insertExpense(exp); onComplete() }
    fun deleteExpense(exp: ExpenseEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteExpense(exp) }
    fun saveHarvest(harvest: HarvestEntity, onComplete: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) { repository.insertHarvest(harvest); onComplete() }
    fun deleteHarvest(harvest: HarvestEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteHarvest(harvest) }

    fun saveFarmProfile(
        profile: FarmProfileEntity,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) = viewModelScope.launch(Dispatchers.IO) {
        val uid = scopedUserId()
        if (uid <= 0L) {
            withContext(Dispatchers.Main) { onError("Sesi akun tidak valid. Silakan login kembali.") }
            return@launch
        }
        try {
            // ID profil dibuat sama dengan ID akun agar satu akun selalu memiliki satu profil.
            repository.saveFarmProfile(profile.copy(id = uid, userId = uid))
            withContext(Dispatchers.Main) { onComplete() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Profil gagal disimpan.")
            }
        }
    }
}
