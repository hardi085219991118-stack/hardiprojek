package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.*
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FeedScheduleLogEntity
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenDark
import com.example.ui.theme.FarmGreenPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedAlarmScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit,
    onNavigateToCycles: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentCycle by viewModel.currentCycle.collectAsState()
    val currentCoop by viewModel.currentCoop.collectAsState()
    val todaySchedules by viewModel.todayFeedSchedules.collectAsState()
    val allSchedules by viewModel.allFeedSchedules.collectAsState()

    // Alarm & Audio State
    val prefs = remember { FeedAlarmPreferences(context) }
    var alarmMasterEnabled by remember { mutableStateOf(prefs.isAlarmMasterEnabled) }
    var soundEnabled by remember { mutableStateOf(prefs.isSoundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(prefs.isVibrationEnabled) }

    val activeAlarmState by FarmAudioManager.activeAlarmState.collectAsState()
    val currentPreviewId by FarmAudioManager.previewSoundId.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSoundCatalogDialog by remember { mutableStateOf(false) }
    var soundDialogSlotTime by remember { mutableStateOf<String?>(null) }
    var soundDialogSlotName by remember { mutableStateOf<String?>(null) }

    var showRecordDialogForSchedule by remember { mutableStateOf<FeedScheduleLogEntity?>(null) }
    var showSnoozeDialogForSchedule by remember { mutableStateOf<FeedScheduleLogEntity?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Inisialisasi audio files dan slot pakan saat siklus aktif terdeteksi
    LaunchedEffect(currentCycle) {
        FarmSoundSynthesizer.ensureAudioFilesExist(context)
        if (currentCycle != null) {
            viewModel.ensureTodayFeedSchedules()
        }
    }

    // Stop preview on exit
    DisposableEffect(Unit) {
        onDispose {
            FarmAudioManager.stopPreview()
        }
    }

    val ageDays = remember(currentCycle) {
        FeedGuideRules.calculateAgeDays(currentCycle?.chickInDate)
    }
    val phaseDetail = remember(ageDays) {
        FeedGuideRules.getPhaseDetailForAge(ageDays)
    }

    val numFmt = NumberFormat.getNumberInstance(Locale("id", "ID")).apply { maximumFractionDigits = 1 }

    // Kepatuhan pakan hari ini
    val totalTodaySlots = todaySchedules.size
    val doneCount = todaySchedules.count { it.status == "SELESAI" }
    val snoozeCount = todaySchedules.count { it.status == "DITUNDA" }
    val skipCount = todaySchedules.count { it.status == "DILEWATI" }
    val pendingCount = todaySchedules.count { it.status == "BELUM" }
    val compliancePct = if (totalTodaySlots > 0) (doneCount.toDouble() / totalTodaySlots) * 100.0 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Alarm Cerdas Pakan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Audio Beda Tiap Jadwal • Bab 4 Panduan",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_alarm")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showSoundCatalogDialog = true }, modifier = Modifier.testTag("btn_catalog_alarm")) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Katalog Suara", tint = Color.White)
                    }
                    IconButton(onClick = { showInfoDialog = true }, modifier = Modifier.testTag("btn_info_alarm")) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Panduan Pakan", tint = Color.White)
                    }
                    IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.testTag("btn_settings_alarm")) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan Alarm", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmGreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        if (currentCycle == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmOff,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Belum Ada Siklus Aktif",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Buat atau pilih siklus terlebih dahulu untuk mengaktifkan alarm dan jadwal pemberian pakan.",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToCycles,
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            modifier = Modifier.testTag("btn_go_to_cycles")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Manajemen Siklus")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("list_feed_alarm"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. BANNER ALARM AKTIF SEDANG BERBUNYI
                if (activeAlarmState.isActive) {
                    item {
                        ActiveAlarmPlayingCard(
                            state = activeAlarmState,
                            onMarkDone = {
                                FarmAlarmAudioHelper.handleDoneFromApp(
                                    context = context,
                                    state = activeAlarmState,
                                    onComplete = {
                                        Toast.makeText(context, "Jadwal ${activeAlarmState.slotName} selesai dicatat!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onStopAlarm = {
                                FeedAlarmAudioService.stop(context)
                                Toast.makeText(context, "Alarm pakan dimatikan.", Toast.LENGTH_SHORT).show()
                            },
                            onSnooze5Mins = {
                                FeedAlarmAudioService.stop(context)
                                currentCycle?.let { c ->
                                    FeedAlarmScheduler.scheduleSnooze(
                                        context = context,
                                        scheduleId = 0L,
                                        cycleId = c.id,
                                        coopId = c.coopId,
                                        coopName = currentCoop?.name ?: "Kandang",
                                        timeStr = activeAlarmState.timeStr,
                                        slotName = activeAlarmState.slotName,
                                        instruction = activeAlarmState.instruction,
                                        ageDays = ageDays,
                                        phase = phaseDetail.phaseName,
                                        feedType = phaseDetail.feedType,
                                        snoozeMinutes = 5
                                    )
                                }
                                Toast.makeText(context, "Alarm ditunda 5 menit.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // 2. Ringkasan Siklus & Panduan Umur Ayam
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = FarmGreenPrimary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessAlarm,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentCoop?.name ?: "Kandang Broiler",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Siklus ${currentCycle?.cycleNumber} • Hari ke-$ageDays",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.25f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fase Pertumbuhan:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                    Text(phaseDetail.phaseName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text(phaseDetail.ageRange, fontSize = 11.sp, color = Color(0xFFFFD54F))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Rekomendasi Pakan (PDF):", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                    Text(phaseDetail.feedType, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("Frekuensi: ${phaseDetail.frequency}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                                }
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${phaseDetail.containerType} • ${phaseDetail.instructions}",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. PENGATURAN SUARA ALARM PER JADWAL
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = FarmGreenPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PILIHAN SUARA ALARM TIAP JADWAL",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = FarmGreenPrimary
                                    )
                                }
                                TextButton(
                                    onClick = { showSoundCatalogDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Uji Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "Setiap jadwal memiliki file audio suara khas peternakan. Tekan 'Dengarkan' untuk mencoba suara atau 'Ubah' untuk memilih suara lain.",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )

                            // 4 Slot Card dengan Preview Audio
                            FeedGuideRules.STANDARD_SLOTS.forEach { slot ->
                                val soundId = prefs.getSlotSoundId(slot.time)
                                val soundItem = FarmAudioCatalog.getSoundById(soundId)
                                val isPlaying = currentPreviewId == soundId
                                val isSlotActive = prefs.isSlotEnabled(slot.time)

                                SlotSoundCard(
                                    slotTime = slot.time,
                                    slotName = slot.slotName,
                                    soundItem = soundItem,
                                    isSlotActive = isSlotActive,
                                    isPlaying = isPlaying,
                                    onPlayPreview = {
                                        if (isPlaying) {
                                            FarmAudioManager.stopPreview()
                                        } else {
                                            FarmAudioManager.playPreview(
                                                context = context,
                                                soundId = soundId,
                                                volume = prefs.getSlotVolume(slot.time)
                                            )
                                        }
                                    },
                                    onChangeSound = {
                                        soundDialogSlotTime = slot.time
                                        soundDialogSlotName = slot.slotName
                                    },
                                    onToggleSlot = { enabled ->
                                        prefs.setSlotEnabled(slot.time, enabled)
                                        currentCycle?.let { c ->
                                            FeedAlarmScheduler.scheduleAllDailySlots(
                                                context = context,
                                                cycleId = c.id,
                                                coopId = c.coopId,
                                                coopName = currentCoop?.name ?: "Kandang",
                                                ageDays = ageDays
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Ringkasan Kepatuhan Jadwal Hari Ini
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊 KEPATUHAN JADWAL HARI INI",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FarmGreenPrimary
                                    )
                                )
                                Text(
                                    text = "${numFmt.format(compliancePct)}%",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = if (compliancePct >= 75.0) FarmGreenPrimary else if (compliancePct >= 50.0) Color(0xFFE65100) else Color(0xFFC62828)
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (compliancePct / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (compliancePct >= 75.0) FarmGreenPrimary else if (compliancePct >= 50.0) Color(0xFFE65100) else Color(0xFFC62828),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("✓ Selesai: $doneCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                                Text("⏰ Tunda: $snoozeCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                Text("⚠️ Lewat: $skipCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                Text("❌ Belum: $pendingCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }
                    }
                }

                // 5. Section Judul Jadwal Hari Ini
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JADWAL PEMBERIAN PAKAN HARI INI",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenPrimary
                            )
                        )
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Bab 4 Bagian 4 (Resmi)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // List 4 Jadwal Hari Ini
                items(todaySchedules) { schedule ->
                    FeedingScheduleCard(
                        schedule = schedule,
                        onMarkDone = {
                            viewModel.updateFeedScheduleStatus(schedule, "SELESAI")
                            Toast.makeText(context, "Jadwal ${schedule.slotName} ditandai selesai!", Toast.LENGTH_SHORT).show()
                        },
                        onOpenSnooze = {
                            showSnoozeDialogForSchedule = schedule
                        },
                        onSkip = {
                            viewModel.updateFeedScheduleStatus(schedule, "DILEWATI")
                            Toast.makeText(context, "Jadwal ${schedule.slotName} dilewati.", Toast.LENGTH_SHORT).show()
                        },
                        onOpenRecordFeed = {
                            showRecordDialogForSchedule = schedule
                        }
                    )
                }

                // Riwayat Keseluruhan Jadwal
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "📋 RIWAYAT ALARM & PEMBERIAN PAKAN",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenPrimary
                        )
                    )
                }

                if (allSchedules.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada riwayat jadwal pakan sebelumnya.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    items(allSchedules.take(15)) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${log.date} | ${log.scheduledTime} (${log.slotName})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StatusBadge(status = log.status, compact = true)
                                    }
                                    Text(
                                        text = "Umur: Hari ke-${log.ageDays} (${log.phase}) • ${log.feedType}",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                    if (log.feedAmountKg > 0.0) {
                                        Text(
                                            text = "Pakan Dicatat: ${numFmt.format(log.feedAmountKg)} Kg ${if (log.notes.isNotBlank()) "(${log.notes})" else ""}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = FarmGreenPrimary
                                        )
                                    }
                                }
                                if (log.actualTime.isNotBlank()) {
                                    Text(
                                        text = "Jam: ${log.actualTime}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG PEMILIHAN SUARA ALARM PER JADWAL
    if (soundDialogSlotTime != null) {
        val slotTime = soundDialogSlotTime!!
        val slotName = soundDialogSlotName ?: "Jadwal"

        SoundSelectionDialog(
            slotTime = slotTime,
            slotName = slotName,
            currentSoundId = prefs.getSlotSoundId(slotTime),
            currentVolume = prefs.getSlotVolume(slotTime),
            currentSnooze = prefs.getSlotSnoozeMinutes(slotTime),
            onDismiss = {
                FarmAudioManager.stopPreview()
                soundDialogSlotTime = null
            },
            onSave = { selectedSoundId, volume, snoozeMinutes ->
                prefs.setSlotSoundId(slotTime, selectedSoundId)
                prefs.setSlotVolume(slotTime, volume)
                prefs.setSlotSnoozeMinutes(slotTime, snoozeMinutes)

                currentCycle?.let { c ->
                    FeedAlarmScheduler.scheduleAllDailySlots(
                        context = context,
                        cycleId = c.id,
                        coopId = c.coopId,
                        coopName = currentCoop?.name ?: "Kandang",
                        ageDays = ageDays
                    )
                }
                FarmAudioManager.stopPreview()
                soundDialogSlotTime = null
                Toast.makeText(context, "Suara alarm untuk $slotTime ($slotName) berhasil disimpan!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // DIALOG KATALOG UJI SEMUA SUARA PETERNAKAN
    if (showSoundCatalogDialog) {
        SoundCatalogTesterDialog(
            onDismiss = {
                FarmAudioManager.stopPreview()
                showSoundCatalogDialog = false
            }
        )
    }

    // Dialog Pengaturan Alarm Pakan Umum
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Pengaturan Alarm Pakan", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pengaturan master jadwal alarm pemberian pakan berdasarkan Panduan Sejahtera Bersama (Bab 4).",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alarm Pakan Master", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Aktifkan seluruh pengingat feeding", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = alarmMasterEnabled,
                            onCheckedChange = {
                                alarmMasterEnabled = it
                                prefs.isAlarmMasterEnabled = it
                                currentCycle?.let { c ->
                                    FeedAlarmScheduler.scheduleAllDailySlots(
                                        context = context,
                                        cycleId = c.id,
                                        coopId = c.coopId,
                                        coopName = currentCoop?.name ?: "Kandang",
                                        ageDays = ageDays
                                    )
                                }
                            },
                            modifier = Modifier.testTag("switch_alarm_master")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bunyi / Suara Alarm", fontSize = 13.sp)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = {
                                soundEnabled = it
                                prefs.isSoundEnabled = it
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Getaran Notifikasi", fontSize = 13.sp)
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = {
                                vibrationEnabled = it
                                prefs.isVibrationEnabled = it
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    // Dialog Snooze (Tunda 5, 10, 15 Menit)
    if (showSnoozeDialogForSchedule != null) {
        val schedule = showSnoozeDialogForSchedule!!
        AlertDialog(
            onDismissRequest = { showSnoozeDialogForSchedule = null },
            title = { Text("Tunda Alarm Pakan", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pilih durasi penundaan alarm untuk jadwal ${schedule.scheduledTime} (${schedule.slotName}):",
                        fontSize = 13.sp
                    )

                    listOf(5, 10, 15, 30).forEach { mins ->
                        OutlinedButton(
                            onClick = {
                                viewModel.updateFeedScheduleStatus(schedule, "DITUNDA", mins)
                                currentCycle?.let { c ->
                                    FeedAlarmScheduler.scheduleSnooze(
                                        context = context,
                                        scheduleId = schedule.id,
                                        cycleId = c.id,
                                        coopId = c.coopId,
                                        coopName = currentCoop?.name ?: "Kandang",
                                        timeStr = schedule.scheduledTime,
                                        slotName = schedule.slotName,
                                        instruction = schedule.instruction,
                                        ageDays = ageDays,
                                        phase = phaseDetail.phaseName,
                                        feedType = phaseDetail.feedType,
                                        snoozeMinutes = mins
                                    )
                                }
                                Toast.makeText(context, "Alarm ditunda $mins menit.", Toast.LENGTH_SHORT).show()
                                showSnoozeDialogForSchedule = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100))
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tunda $mins Menit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSnoozeDialogForSchedule = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Catat Pakan Selesai
    if (showRecordDialogForSchedule != null) {
        val schedule = showRecordDialogForSchedule!!
        RecordFeedDialog(
            schedule = schedule,
            defaultFeedType = phaseDetail.feedType,
            onDismiss = { showRecordDialogForSchedule = null },
            onSave = { bags, kgPerBag, type, notes ->
                viewModel.recordFeedingAndCompleteSchedule(
                    schedule = schedule,
                    bags = bags,
                    kgPerBag = kgPerBag,
                    feedType = type,
                    notes = notes,
                    photoPath = "",
                    onComplete = {
                        Toast.makeText(context, "Pakan jadwal ${schedule.slotName} berhasil dicatat!", Toast.LENGTH_SHORT).show()
                        showRecordDialogForSchedule = null
                    }
                )
            }
        )
    }

    // Dialog Panduan Pakan Resmi
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = FarmGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Panduan Jadwal Feeding", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Standar Operasional Pakan Broiler (Panduan Bab 4 Bagian 4):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = FarmGreenPrimary
                    )

                    FeedGuideRules.STANDARD_SLOTS.forEach { slot ->
                        val sound = FarmAudioCatalog.getSoundById(FarmAudioCatalog.getDefaultSoundForSlot(slot.time))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${slot.time} - Waktu ${slot.slotName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${sound.iconEmoji} ${sound.name}", fontSize = 11.sp, color = FarmGreenPrimary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Tugas: ${slot.taskInstruction}", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Prinsip Feeding:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text("1. Bersihkan tempat pakan dari kotoran sebelum menambah pakan baru.", fontSize = 11.sp)
                    Text("2. Ratakan pakan di seluruh talang/tray agar ayam tidak berebut.", fontSize = 11.sp)
                    Text("3. Cek sisa pakan setiap feeding untuk memantau nafsu makan ayam.", fontSize = 11.sp)
                    Text("4. Pada fase finisher, hindari feeding saat jam terpanas (11:00-15:00) untuk mencegah heat stress.", fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Paham")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// KOMPONEN CARD SUARA PER JADWAL DENGAN TOMBOL PREVIEW AUDIO
// -------------------------------------------------------------
@Composable
fun SlotSoundCard(
    slotTime: String,
    slotName: String,
    soundItem: FarmSoundItem,
    isSlotActive: Boolean,
    isPlaying: Boolean,
    onPlayPreview: () -> Unit,
    onChangeSound: () -> Unit,
    onToggleSlot: (Boolean) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB300)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isSlotActive) FarmGreenPrimary else Color.Gray,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = slotTime,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jadwal $slotName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Switch(
                    checked = isSlotActive,
                    onCheckedChange = onToggleSlot,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Info Suara yang Terpilih
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onChangeSound() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = soundItem.iconEmoji,
                        fontSize = 24.sp,
                        modifier = if (isPlaying) Modifier.graphicsLayer { scaleX = pulseScale; scaleY = pulseScale } else Modifier
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = soundItem.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = soundItem.subtitle,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onChangeSound,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Ubah", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Tombol Preview / Berhentikan Audio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPlaying) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sedang memutar...", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onPlayPreview,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color(0xFFD32F2F) else FarmGreenPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "BERHENTIKAN" else "DENGARKAN SUARA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CARD ALARM AKTIF SEDANG BERBUNYI (HEADS-UP ALERT IN-APP)
// -------------------------------------------------------------
@Composable
fun ActiveAlarmPlayingCard(
    state: ActiveAlarmState,
    onMarkDone: () -> Unit,
    onStopAlarm: () -> Unit,
    onSnooze5Mins: () -> Unit
) {
    val soundItem = FarmAudioCatalog.getSoundById(state.soundId)
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAlarm")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE65100).copy(alpha = pulseAlpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE65100),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(soundItem.iconEmoji, fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⏰ WAKTU PEMBERIAN PAKAN!",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFBF360C),
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${state.timeStr} - ${state.slotName} (${state.coopName})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Audio Berbunyi: ${soundItem.name} (Looping)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                    Text("🐥 Umur: Hari ke-${state.ageDays} (${state.phase})", fontSize = 11.sp, color = Color.DarkGray)
                    Text("🥣 Pakan: ${state.feedType}", fontSize = 11.sp, color = Color.DarkGray)
                    if (state.instruction.isNotBlank()) {
                        Text("📋 Tugas: ${state.instruction}", fontSize = 11.sp, color = FarmGreenDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 3 TOMBOL BESAR & JELAS UNTUK PETERNAK
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onMarkDone,
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("✓ SUDAH DIBERIKAN", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onStopAlarm,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🔕 MATIKAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onSnooze5Mins,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⏰ TUNDA 5 MNT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG PEMILIHAN SUARA ALARM DENGAN PREVIEW PER SUARA
// -------------------------------------------------------------
@Composable
fun SoundSelectionDialog(
    slotTime: String,
    slotName: String,
    currentSoundId: String,
    currentVolume: Float,
    currentSnooze: Int,
    onDismiss: () -> Unit,
    onSave: (soundId: String, volume: Float, snoozeMinutes: Int) -> Unit
) {
    val context = LocalContext.current
    var selectedSoundId by remember { mutableStateOf(currentSoundId) }
    var volume by remember { mutableFloatStateOf(currentVolume) }
    var snoozeMinutes by remember { mutableIntStateOf(currentSnooze) }
    val currentPreviewId by FarmAudioManager.previewSoundId.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Pilih Suara Alarm Pakan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Jadwal: $slotTime - $slotName",
                    fontSize = 12.sp,
                    color = FarmGreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pilih audio yang akan diputar saat alarm berbunyi. Tekan tombol play untuk mendengarkan pratinjau audio:",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                FarmAudioCatalog.ALL_SOUNDS.forEach { sound ->
                    val isSelected = selectedSoundId == sound.id
                    val isPlaying = currentPreviewId == sound.id

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, FarmGreenPrimary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSoundId = sound.id }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedSoundId = sound.id }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(sound.iconEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = sound.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) FarmGreenDark else Color.Unspecified
                                        )
                                        Text(
                                            text = sound.subtitle,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            FarmAudioManager.stopPreview()
                                        } else {
                                            FarmAudioManager.playPreview(context, sound.id, volume)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayCircle,
                                        contentDescription = "Preview",
                                        tint = if (isPlaying) Color(0xFFD32F2F) else FarmGreenPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Text(
                                text = sound.description,
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(start = 36.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Slider Volume
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Volume Suara:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${(volume * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                    }
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0.2f..1.0f
                    )
                }

                // Opsi Durasi Tunda (Snooze)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Durasi Tunda Bawaan:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = snoozeMinutes == mins,
                                onClick = { snoozeMinutes = mins },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedSoundId, volume, snoozeMinutes) },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
            ) {
                Text("Simpan Pilihan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// -------------------------------------------------------------
// DIALOG UJI SEMUA SUARA PETERNAKAN (SOUND CATALOG TESTER)
// -------------------------------------------------------------
@Composable
fun SoundCatalogTesterDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentPreviewId by FarmAudioManager.previewSoundId.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = FarmGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Katalog Suara Peternakan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Dengarkan seluruh variasi suara peternakan ayam broiler berkualitas jernih yang tersedia dalam aplikasi:",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                FarmAudioCatalog.ALL_SOUNDS.forEach { sound ->
                    val isPlaying = currentPreviewId == sound.id

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPlaying) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isPlaying) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sound.iconEmoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(sound.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(sound.subtitle, fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Button(
                                onClick = {
                                    if (isPlaying) {
                                        FarmAudioManager.stopPreview()
                                    } else {
                                        FarmAudioManager.playPreview(context, sound.id, 1.0f)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) Color(0xFFD32F2F) else FarmGreenPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlaying) "Stop" else "Putar", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
            ) {
                Text("Selesai")
            }
        }
    )
}

// -------------------------------------------------------------
// HELPER LOGIKA SELESAI ALARM DARI APLIKASI
// -------------------------------------------------------------
object FarmAlarmAudioHelper {
    fun handleDoneFromApp(
        context: android.content.Context,
        state: ActiveAlarmState,
        onComplete: () -> Unit = {}
    ) {
        FeedAlarmAudioService.stop(context)
        val today = FeedGuideRules.getTodayDateString()
        val actualTime = FeedGuideRules.getCurrentTimeString()
        val now = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val dao = db.farmDao()
            val existing = dao.getFeedScheduleBySlot(state.cycleId, today, state.timeStr)

            if (existing != null) {
                dao.updateFeedSchedule(
                    existing.copy(
                        status = "SELESAI",
                        actualTime = actualTime,
                        updatedAt = now
                    )
                )
            } else {
                dao.insertFeedSchedule(
                    FeedScheduleLogEntity(
                        cycleId = state.cycleId,
                        coopId = state.coopId,
                        date = today,
                        scheduledTime = state.timeStr,
                        slotName = state.slotName,
                        instruction = state.instruction,
                        actualTime = actualTime,
                        ageDays = state.ageDays,
                        phase = state.phase,
                        feedType = state.feedType,
                        status = "SELESAI",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}

// -------------------------------------------------------------
// KARTU JADWAL HARIAN SEDERHANA & EFISIEN
// -------------------------------------------------------------
@Composable
fun FeedingScheduleCard(
    schedule: FeedScheduleLogEntity,
    onMarkDone: () -> Unit,
    onOpenSnooze: () -> Unit,
    onSkip: () -> Unit,
    onOpenRecordFeed: () -> Unit
) {
    val isDone = schedule.status == "SELESAI"
    val sound = FarmAudioCatalog.getSoundById(FarmAudioCatalog.getDefaultSoundForSlot(schedule.scheduledTime))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 1.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDone) FarmGreenPrimary else Color(0xFF1976D2)
                    ) {
                        Text(
                            text = schedule.scheduledTime,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Waktu ${schedule.slotName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${sound.iconEmoji} ${sound.name}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                StatusBadge(status = schedule.status)
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("🥣 Pakan: ${schedule.feedType}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("📋 Tugas: ${schedule.instruction}", fontSize = 11.sp, color = Color.DarkGray)
                    if (schedule.actualTime.isNotBlank()) {
                        Text("🕒 Aktual Diberikan: Jam ${schedule.actualTime}", fontSize = 11.sp, color = FarmGreenDark, fontWeight = FontWeight.Bold)
                    }
                    if (schedule.feedAmountKg > 0.0) {
                        val bags = schedule.feedAmountKg / 50.0
                        Text("⚖️ Jumlah: ${schedule.feedAmountKg} Kg (%.1f Sak)".format(bags), fontSize = 11.sp, color = FarmGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tombol Aksi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isDone) {
                    Button(
                        onClick = onMarkDone,
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sudah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenSnooze,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100))
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tunda", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onSkip,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Lewati", tint = Color.Gray)
                    }
                }

                Button(
                    onClick = onOpenRecordFeed,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDone) FarmGreenPrimary else Color(0xFF37474F)),
                    modifier = if (isDone) Modifier.fillMaxWidth() else Modifier.weight(1.2f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isDone) "Ubah Catatan Pakan" else "Catat Detail", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG CATAT DETAIL PAKAN
// -------------------------------------------------------------
@Composable
fun RecordFeedDialog(
    schedule: FeedScheduleLogEntity,
    defaultFeedType: String,
    onDismiss: () -> Unit,
    onSave: (bags: Double, kgPerBag: Double, feedType: String, notes: String) -> Unit
) {
    val initialBags = if (schedule.feedAmountKg > 0.0) (schedule.feedAmountKg / 50.0).toString() else ""
    var bagsText by remember { mutableStateOf(initialBags) }
    var kgPerBagText by remember { mutableStateOf("50") }
    var feedType by remember { mutableStateOf(schedule.feedType.ifBlank { defaultFeedType }) }
    var notes by remember { mutableStateOf(schedule.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Catat Pakan: ${schedule.scheduledTime} (${schedule.slotName})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = bagsText,
                    onValueChange = { bagsText = it },
                    label = { Text("Jumlah Sak Pakan (Sak)") },
                    placeholder = { Text("Contoh: 2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = kgPerBagText,
                    onValueChange = { kgPerBagText = it },
                    label = { Text("Berat per Sak (Kg/sak)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                val bags = bagsText.toDoubleOrNull() ?: 0.0
                val kgPerBag = kgPerBagText.toDoubleOrNull() ?: 50.0
                val totalKg = bags * kgPerBag

                if (totalKg > 0.0) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Total Pakan: %.1f Kg".format(totalKg),
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = feedType,
                    onValueChange = { feedType = it },
                    label = { Text("Jenis / Merk Pakan") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    placeholder = { Text("Contoh: Feeder dibersihkan, pakan habis total") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bags = bagsText.toDoubleOrNull() ?: 0.0
                    val kgPerBag = kgPerBagText.toDoubleOrNull() ?: 50.0
                    onSave(bags, kgPerBag, feedType, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
            ) {
                Text("Simpan Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// -------------------------------------------------------------
// BADGE STATUS JADWAL
// -------------------------------------------------------------
@Composable
fun StatusBadge(status: String, compact: Boolean = false) {
    val (bg, fg, label) = when (status) {
        "SELESAI" -> Triple(Color(0xFFE8F5E9), FarmGreenPrimary, "✓ SELESAI")
        "DITUNDA" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "⏰ DITUNDA")
        "DILEWATI" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "⚠️ LEWAT")
        else -> Triple(Color(0xFFECEFF1), Color(0xFF546E7A), "❌ BELUM")
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            modifier = Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 2.dp)
        )
    }
}
