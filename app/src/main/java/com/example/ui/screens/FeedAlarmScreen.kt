package com.example.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.FeedAlarmPreferences
import com.example.alarm.FeedAlarmScheduler
import com.example.alarm.FeedGuideRules
import com.example.data.local.entity.FeedScheduleLogEntity
import com.example.ui.FarmViewModel
import com.example.ui.components.StatCard
import com.example.ui.theme.FarmGreenDark
import com.example.ui.theme.FarmGreenPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
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

    val prefs = remember { FeedAlarmPreferences(context) }
    var alarmMasterEnabled by remember { mutableStateOf(prefs.isAlarmMasterEnabled) }
    var soundEnabled by remember { mutableStateOf(prefs.isSoundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(prefs.isVibrationEnabled) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRecordDialogForSchedule by remember { mutableStateOf<FeedScheduleLogEntity?>(null) }
    var showSnoozeDialogForSchedule by remember { mutableStateOf<FeedScheduleLogEntity?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Inisialisasi 4 slot jadwal hari ini ketika siklus aktif terdeteksi
    LaunchedEffect(currentCycle) {
        if (currentCycle != null) {
            viewModel.ensureTodayFeedSchedules()
        }
    }

    val ageDays = remember(currentCycle) {
        FeedGuideRules.calculateAgeDays(currentCycle?.chickInDate)
    }
    val phaseDetail = remember(ageDays) {
        FeedGuideRules.getPhaseDetailForAge(ageDays)
    }

    val numFmt = NumberFormat.getNumberInstance(Locale("id", "ID")).apply { maximumFractionDigits = 1 }

    // Hitung kepatuhan jadwal
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
                            text = "Panduan Resmi Bab 4 (4x Sehari)",
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
                // Header Ringkasan Siklus & Fase Panduan
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

                // Kartu Ringkasan Kepatuhan Hari Ini
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

                // Section Judul Jadwal Hari Ini
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

    // Dialog Pengaturan Alarm Pakan
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
                        text = "Pengaturan jadwal alarm pemberian pakan berdasarkan Panduan Sejahtera Bersama (Bab 4).",
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

                    HorizontalDivider()

                    Text("Jadwal Slot Aktif (Bab 4 PDF):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    FeedGuideRules.STANDARD_SLOTS.forEach { slot ->
                        var isSlotActive by remember { mutableStateOf(prefs.isSlotEnabled(slot.time)) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${slot.time} - ${slot.slotName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(slot.taskInstruction, fontSize = 10.sp, color = Color.Gray)
                            }
                            Checkbox(
                                checked = isSlotActive,
                                onCheckedChange = {
                                    isSlotActive = it
                                    prefs.setSlotEnabled(slot.time, it)
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
                        text = "Pilih durasi tunda untuk jadwal ${schedule.slotName} (${schedule.scheduledTime}):",
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateFeedScheduleStatus(schedule, "DITUNDA", snoozeMinutes = 5)
                                Toast.makeText(context, "Alarm ditunda 5 menit", Toast.LENGTH_SHORT).show()
                                showSnoozeDialogForSchedule = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("5 Mnt")
                        }

                        Button(
                            onClick = {
                                viewModel.updateFeedScheduleStatus(schedule, "DITUNDA", snoozeMinutes = 10)
                                Toast.makeText(context, "Alarm ditunda 10 menit", Toast.LENGTH_SHORT).show()
                                showSnoozeDialogForSchedule = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("10 Mnt")
                        }

                        Button(
                            onClick = {
                                viewModel.updateFeedScheduleStatus(schedule, "DITUNDA", snoozeMinutes = 15)
                                Toast.makeText(context, "Alarm ditunda 15 menit", Toast.LENGTH_SHORT).show()
                                showSnoozeDialogForSchedule = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("15 Mnt")
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

    // Dialog Catat Pemberian Pakan Lengkap Terintegrasi
    if (showRecordDialogForSchedule != null) {
        val schedule = showRecordDialogForSchedule!!
        var bagsStr by remember { mutableStateOf("") }
        var kgPerBagStr by remember { mutableStateOf("50") }
        var feedTypeInput by remember { mutableStateOf(schedule.feedType.ifBlank { phaseDetail.feedType }) }
        var notesInput by remember { mutableStateOf("") }
        var photoPath by remember { mutableStateOf("") }
        var formError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRecordDialogForSchedule = null },
            title = {
                Column {
                    Text("Catat Pemberian Pakan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Jadwal: ${schedule.slotName} (${schedule.scheduledTime}) • Hari ke-${schedule.ageDays}", fontSize = 11.sp, color = Color.Gray)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (formError.isNotBlank()) {
                        Text(formError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("📋 Rekomendasi Panduan:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FarmGreenDark)
                            Text(schedule.instruction, fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }

                    OutlinedTextField(
                        value = feedTypeInput,
                        onValueChange = { feedTypeInput = it },
                        label = { Text("Jenis Pakan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_record_feed_type")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bagsStr,
                            onValueChange = { bagsStr = it },
                            label = { Text("Jumlah Sak") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_record_feed_bags")
                        )
                        OutlinedTextField(
                            value = kgPerBagStr,
                            onValueChange = { kgPerBagStr = it },
                            label = { Text("Kg / Sak") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PhotoProofPicker(
                        initialPath = photoPath,
                        onPathChanged = { photoPath = it }
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Catatan / Keterangan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bags = bagsStr.toDoubleOrNull() ?: 0.0
                        val kgPerBag = kgPerBagStr.toDoubleOrNull() ?: 50.0

                        if (bags <= 0) {
                            formError = "Masukkan jumlah sak pakan yang diberikan!"
                            return@Button
                        }

                        viewModel.recordFeedingAndCompleteSchedule(
                            schedule = schedule,
                            bags = bags,
                            kgPerBag = kgPerBag,
                            feedType = feedTypeInput,
                            notes = notesInput,
                            photoPath = photoPath
                        ) {
                            Toast.makeText(context, "Pemberian pakan berhasil dicatat & jadwal selesai!", Toast.LENGTH_SHORT).show()
                            showRecordDialogForSchedule = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("btn_confirm_record_feed")
                ) {
                    Text("Simpan & Selesai")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordDialogForSchedule = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Info Panduan Resmi Bab 4
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = FarmGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aturan Pakan Panduan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        text = "Sumber: PANDUAN SEJAHTERA BERSAMA.pdf (Bab 2, 3, dan 4)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = FarmGreenPrimary
                    )

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("1. Fase Brooding (0–7 Hari):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("• Pakan Prestarter berkualitas baik, butiran halus.\n• 1 tray pakan / 50 DOC.\n• Isi 1/4 - 1/3 tray, sering tetapi sedikit (5-6x/hari).", fontSize = 11.sp)
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("2. Fase Pembesaran / Starter (8–21 Hari):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("• Pakan Starter.\n• Transisi bertahap 4 hari (75/25, 50/50, 25/75, 100%).\n• Feeder gantung setinggi punggung ayam (4-5x/hari).", fontSize = 11.sp)
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("3. Fase Finisher (22–35 Hari):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("• Pakan Finisher.\n• Hindari pemberian pakan saat suhu puncak (11.00–15.00) untuk cegah heat stress.\n• Target feed waste <1% (3-4x/hari).", fontSize = 11.sp)
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("4. Jadwal Standar Feeding (Bab 4 Bagian 4):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("• 06.00: Isi feeder, bersihkan pakan basah, cek feed waste.\n• 11.00: Tambah pakan, ratakan pakan, cek ayam makan.\n• 16.00: Isi feeder, pastikan cukup sampai malam.\n• 20.00: Cek sisa pakan, tambah jika perlu.", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Mengerti")
                }
            }
        )
    }
}

@Composable
fun FeedingScheduleCard(
    schedule: FeedScheduleLogEntity,
    onMarkDone: () -> Unit,
    onOpenSnooze: () -> Unit,
    onSkip: () -> Unit,
    onOpenRecordFeed: () -> Unit
) {
    val isDone = schedule.status == "SELESAI"
    val isSnooze = schedule.status == "DITUNDA"
    val isSkip = schedule.status == "DILEWATI"

    val numFmt = NumberFormat.getNumberInstance(Locale("id", "ID")).apply { maximumFractionDigits = 1 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_feed_slot_${schedule.scheduledTime.replace(":", "")}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Slot & Jam
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = when {
                                    isDone -> FarmGreenPrimary
                                    isSnooze -> Color(0xFFFFA000)
                                    isSkip -> Color(0xFFE53935)
                                    else -> Color(0xFF1976D2)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isDone -> Icons.Default.Check
                                isSnooze -> Icons.Default.Snooze
                                isSkip -> Icons.Default.Close
                                else -> Icons.Default.AccessTime
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "${schedule.scheduledTime} - ${schedule.slotName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (schedule.actualTime.isNotBlank()) {
                            Text(
                                text = "Aktual: ${schedule.actualTime}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                StatusBadge(status = schedule.status)
            }

            // Tugas Sesuai Bab 4 PDF
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = null,
                        tint = FarmGreenPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = schedule.instruction,
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }

            if (schedule.feedAmountKg > 0.0) {
                Text(
                    text = "📦 Pakan Terpakai: ${numFmt.format(schedule.feedAmountKg)} Kg ${if (schedule.notes.isNotBlank()) "(${schedule.notes})" else ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FarmGreenPrimary
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isDone) {
                    Button(
                        onClick = onMarkDone,
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_done_${schedule.scheduledTime.replace(":", "")}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Selesai", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenSnooze,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Tunda", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onSkip,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text("Lewati", fontSize = 11.sp, color = Color(0xFFC62828))
                    }
                }

                FilledTonalButton(
                    onClick = onOpenRecordFeed,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.4f)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat Pakan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, compact: Boolean = false) {
    val (bgColor, textColor, text) = when (status) {
        "SELESAI" -> Triple(Color(0xFFE8F5E9), FarmGreenDark, "✓ SUDAH DIBERIKAN")
        "DITUNDA" -> Triple(Color(0xFFFFF8E1), Color(0xFFE65100), "⏰ DITUNDA")
        "DILEWATI" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "⚠️ DILEWATI")
        else -> Triple(Color(0xFFEDE7F6), Color(0xFF4527A0), "❌ BELUM DILAKUKAN")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = if (compact) 5.dp else 8.dp, vertical = if (compact) 2.dp else 4.dp)
        )
    }
}
