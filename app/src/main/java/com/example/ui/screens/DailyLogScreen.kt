package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.paint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DeletePinProtectedButton
import com.example.data.local.entity.DailyLogEntity
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val currentCycle by viewModel.currentCycle.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<DailyLogEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<DailyLogEntity?>(null) }

    // Auto-select cycle if currentCycle is null but cycles exist
    LaunchedEffect(currentCycle, cycles) {
        if (currentCycle == null && cycles.isNotEmpty()) {
            val active = cycles.find { it.status == "ACTIVE" } ?: cycles.firstOrNull()
            active?.let { viewModel.selectCycle(it.id) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Harian Kandang", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_daily_log")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingLog = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("btn_top_add_daily_log")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Input Data Harian", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmGreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (currentCycle != null) {
                FloatingActionButton(
                    onClick = {
                        editingLog = null
                        showAddDialog = true
                    },
                    containerColor = FarmGreenPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_daily_log")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Input Data Harian")
                }
            }
        }
    ) { innerPadding ->
        if (currentCycle == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Pilih atau buat siklus pemeliharaan terlebih dahulu.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                    .testTag("list_daily_logs"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Siklus: ${currentCycle?.cycleNumber}",
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenPrimary
                            )
                            Text(
                                text = "Total Catatan Harian: ${dailyLogs.size} Hari | DOC Masuk: ${currentCycle?.docCount} Ekor",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                if (dailyLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    tint = FarmGreenPrimary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "Belum Ada Pencatatan Harian",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Tekan tombol di bawah untuk mengisi rekaman harian ayam (populasi, pakan, suhu, OVK).",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        editingLog = null
                                        showAddDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                    modifier = Modifier.testTag("btn_empty_add_daily_log")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ INPUT LAPORAN HARIAN", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                items(dailyLogs.reversed()) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_daily_log_${log.ageDays}"),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge(
                                        containerColor = FarmGreenPrimary,
                                        contentColor = Color.White
                                    ) {
                                        Text("Hari ke-${log.ageDays}", modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = log.date,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingLog = log
                                            showAddDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { deleteCandidate = log },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Populasi Sore", fontSize = 11.sp, color = Color.Gray)
                                    Text("${log.afternoonPopulation} Ekor", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                                }
                                Column {
                                    Text("Mati / Afkir", fontSize = 11.sp, color = Color.Gray)
                                    Text("${log.deadCount} / ${log.cullCount} Ekor", fontWeight = FontWeight.Bold, color = if (log.deadCount > 10) Color.Red else Color.Black)
                                }
                                Column {
                                    Text("Pakan Diberikan", fontSize = 11.sp, color = Color.Gray)
                                    Text("${log.feedGivenKg} Kg", fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Air Minum", fontSize = 11.sp, color = Color.Gray)
                                    Text("${log.waterIntakeLiters} L", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (log.medicineGiven.isNotEmpty() || log.vitaminGiven.isNotEmpty() || log.vaccineGiven.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "OVK: " + listOfNotNull(
                                        log.medicineGiven.takeIf { it.isNotEmpty() },
                                        log.vitaminGiven.takeIf { it.isNotEmpty() },
                                        log.vaccineGiven.takeIf { it.isNotEmpty() }
                                    ).joinToString(", "),
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (log.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Catatan: ${log.notes}",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog && currentCycle != null) {
        val cycle = currentCycle!!
        val nextAge = if (dailyLogs.isNotEmpty()) dailyLogs.maxOf { it.ageDays } + 1 else 1
        val lastPop = dailyLogs.maxByOrNull { it.ageDays }?.afternoonPopulation ?: cycle.docCount

        var date by remember { mutableStateOf(editingLog?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var ageStr by remember { mutableStateOf(editingLog?.ageDays?.toString() ?: nextAge.toString()) }
        var morningPopStr by remember { mutableStateOf(editingLog?.morningPopulation?.toString() ?: lastPop.toString()) }
        var deadStr by remember { mutableStateOf(editingLog?.deadCount?.toString() ?: "0") }
        var cullStr by remember { mutableStateOf(editingLog?.cullCount?.toString() ?: "0") }
        var outStr by remember { mutableStateOf(editingLog?.outCount?.toString() ?: "0") }
        var feedKgStr by remember { mutableStateOf(editingLog?.feedGivenKg?.toString() ?: "") }
        var waterStr by remember { mutableStateOf(editingLog?.waterIntakeLiters?.toString() ?: "") }
        var medicine by remember { mutableStateOf(editingLog?.medicineGiven ?: "") }
        var vitamin by remember { mutableStateOf(editingLog?.vitaminGiven ?: "") }
        var vaccine by remember { mutableStateOf(editingLog?.vaccineGiven ?: "") }
        var chickenCond by remember { mutableStateOf(editingLog?.chickenCondition ?: "") }
        var tempStr by remember { mutableStateOf(editingLog?.tempCelsius?.takeIf { it > 0 }?.toString() ?: "") }
        var humidityStr by remember { mutableStateOf(editingLog?.humidityPercent?.takeIf { it > 0 }?.toString() ?: "") }
        var weather by remember { mutableStateOf(editingLog?.weather ?: "") }
        var litterCond by remember { mutableStateOf(editingLog?.litterCondition ?: "") }
        var notes by remember { mutableStateOf(editingLog?.notes ?: "") }
        var photoPath by remember { mutableStateOf(editingLog?.photoUri ?: "") }
        var errorMessage by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingLog == null) "Input Laporan Harian" else "Edit Laporan Harian", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Tanggal (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1.3f).testTag("input_daily_date")
                        )
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it },
                            label = { Text("Umur (Hari)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f).testTag("input_daily_age")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = morningPopStr,
                            onValueChange = { morningPopStr = it },
                            label = { Text("Populasi Pagi") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deadStr,
                            onValueChange = { deadStr = it },
                            label = { Text("Ayam Mati") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_daily_dead")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cullStr,
                            onValueChange = { cullStr = it },
                            label = { Text("Afkir (Cull)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = outStr,
                            onValueChange = { outStr = it },
                            label = { Text("Ayam Keluar") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = feedKgStr,
                            onValueChange = {
                                feedKgStr = it
                                // Auto estimate water intake if empty (approx 2.0x feed kg)
                                val kg = it.toDoubleOrNull() ?: 0.0
                                if (waterStr.isEmpty() && kg > 0) {
                                    waterStr = String.format(Locale.US, "%.1f", kg * 2.1)
                                }
                            },
                            label = { Text("Pakan (Kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("input_daily_feed")
                        )
                        OutlinedTextField(
                            value = waterStr,
                            onValueChange = { waterStr = it },
                            label = { Text("Air Minum (L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = chickenCond,
                        onValueChange = { chickenCond = it },
                        label = { Text("Kondisi Ayam & Nafsu Makan") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tempStr,
                            onValueChange = { tempStr = it },
                            label = { Text("Suhu (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = humidityStr,
                            onValueChange = { humidityStr = it },
                            label = { Text("Kelembapan (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = medicine,
                        onValueChange = { medicine = it },
                        label = { Text("Obat (Jika ada)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = vitamin,
                        onValueChange = { vitamin = it },
                        label = { Text("Vitamin (Jika ada)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = vaccine,
                        onValueChange = { vaccine = it },
                        label = { Text("Vaksin (Jika ada)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                                        PhotoProofPicker(
                        initialPath = photoPath,
                        onPathChanged = { photoPath = it }
                    )

OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Keterangan / Kejadian Penting") },
                        modifier = Modifier.fillMaxWidth().testTag("input_daily_notes")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val age = ageStr.toIntOrNull() ?: 0
                        val morningPop = morningPopStr.toIntOrNull() ?: 0
                        val dead = deadStr.toIntOrNull() ?: 0
                        val cull = cullStr.toIntOrNull() ?: 0
                        val out = outStr.toIntOrNull() ?: 0
                        val feedKg = feedKgStr.toDoubleOrNull() ?: 0.0
                        val water = waterStr.toDoubleOrNull() ?: 0.0

                        if (age <= 0) {
                            errorMessage = "Umur ayam harus lebih dari 0!"
                            return@Button
                        }
                        if (dead + cull + out > morningPop && morningPop > 0) {
                            errorMessage = "Jumlah ayam mati + afkir + keluar melebihi populasi!"
                            return@Button
                        }

                        val afternoonPop = (morningPop - dead - cull - out).coerceAtLeast(0)

                        val logToSave = DailyLogEntity(
                            id = editingLog?.id ?: 0,
                            photoUri = photoPath,
                            cycleId = cycle.id,
                            date = date,
                            ageDays = age,
                            morningPopulation = morningPop,
                            afternoonPopulation = afternoonPop,
                            deadCount = dead,
                            cullCount = cull,
                            outCount = out,
                            feedGivenBags = feedKg / 50.0,
                            feedGivenKg = feedKg,
                            feedRemainingKg = 0.0,
                            waterIntakeLiters = water,
                            medicineGiven = medicine,
                            vitaminGiven = vitamin,
                            vaccineGiven = vaccine,
                            chickenCondition = chickenCond,
                            tempCelsius = tempStr.toDoubleOrNull() ?: 29.5,
                            humidityPercent = humidityStr.toDoubleOrNull() ?: 65.0,
                            weather = weather,
                            litterCondition = litterCond,
                            notes = notes
                        )

                        viewModel.saveDailyLog(logToSave) {
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("btn_save_daily_log")
                ) {
                    Text("Simpan Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete confirmation
    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Hapus Catatan Harian?") },
            text = { Text("Apakah Anda yakin ingin menghapus data harian umur ${deleteCandidate?.ageDays} hari?") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    deleteCandidate?.let { viewModel.deleteDailyLog(it) }
                    deleteCandidate = null
                })
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
