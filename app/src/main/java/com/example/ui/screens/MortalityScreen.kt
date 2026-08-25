package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.paint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.data.local.entity.MortalityLogEntity
import com.example.ui.FarmViewModel
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatCard
import com.example.ui.theme.FarmGreenPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MortalityScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val currentCycle by viewModel.currentCycle.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val mortalityLogs by viewModel.mortalityLogs.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<MortalityLogEntity?>(null) }

    val numFmt = NumberFormat.getNumberInstance(Locale("id", "ID")).apply { maximumFractionDigits = 2 }

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
                title = { Text("Pencatatan Kematian & Mortalitas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_mortality")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("btn_top_add_mortality")) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Catat Kematian", tint = Color.White)
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
                    onClick = { showAddDialog = true },
                    containerColor = FarmGreenPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_mortality")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Catat Kematian")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                .testTag("list_mortality"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Total Kematian",
                        value = "${summary.totalDead} Ekor",
                        subtitle = "Dari ${summary.initialPop} Ekor DOC",
                        icon = Icons.Default.HeartBroken,
                        contentColor = if (summary.mortalityPercent > 4.0) Color(0xFFC62828) else FarmGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Mortalitas Kumulatif",
                        value = "${numFmt.format(summary.mortalityPercent)} %",
                        subtitle = "Standar toleransi: < 4.0%",
                        icon = Icons.Default.Percent,
                        contentColor = if (summary.mortalityPercent > 4.0) Color(0xFFC62828) else FarmGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Chart
            item {
                val mortBarPoints = mortalityLogs.takeLast(10).map { "H-${it.ageDays}" to it.count.toDouble() }
                SimpleBarChart(
                    title = "Kematian Harian (Ekor)",
                    dataPoints = mortBarPoints,
                    unit = "Ekor",
                    barColor = Color(0xFFD32F2F)
                )
            }

            item {
                Text(
                    text = "RINCIAN KEMATIAN AYAM",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary
                    )
                )
            }

            if (mortalityLogs.isEmpty()) {
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
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Belum Ada Catatan Kematian Ayam",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tekan tombol di bawah untuk mencatat kejadian kematian atau afkir ayam harian.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                modifier = Modifier.testTag("btn_empty_add_mortality")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ CATAT KEMATIAN AYAM", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(mortalityLogs.reversed()) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(containerColor = Color(0xFFD32F2F), contentColor = Color.White) {
                                    Text("Hari ke-${log.ageDays}")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(log.date, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Penyebab: ${log.cause} | Blok: ${log.locationBlock.ifEmpty { "Kandang Utama" }}", fontSize = 12.sp, color = Color.DarkGray)
                            if (log.notes.isNotEmpty()) {
                                Text("Catatan: ${log.notes}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${log.count} Ekor",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD32F2F),
                                fontSize = 16.sp
                            )
                            IconButton(onClick = { deleteCandidate = log }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && currentCycle != null) {
        val cycle = currentCycle!!
        var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var ageStr by remember { mutableStateOf(summary.ageDays.toString()) }
        var countStr by remember { mutableStateOf("1") }
        var cause by remember { mutableStateOf("Lemah / Kerdil") }
        var blockLocation by remember { mutableStateOf("Blok Depan") }
        var notes by remember { mutableStateOf("") }
        var photoPath by remember { mutableStateOf("") }
        var formError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Catat Kematian Ayam", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (formError.isNotEmpty()) {
                        Text(formError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Tanggal") },
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it },
                            label = { Text("Umur (Hari)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f).testTag("input_mortality_age")
                        )
                    }

                    OutlinedTextField(
                        value = countStr,
                        onValueChange = { countStr = it },
                        label = { Text("Jumlah Ayam Mati (Ekor)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_mortality_count")
                    )

                    OutlinedTextField(
                        value = cause,
                        onValueChange = { cause = it },
                        label = { Text("Penyebab Kematian") },
                        modifier = Modifier.fillMaxWidth().testTag("input_mortality_cause")
                    )

                    OutlinedTextField(
                        value = blockLocation,
                        onValueChange = { blockLocation = it },
                        label = { Text("Lokasi / Blok Kandang") },
                        modifier = Modifier.fillMaxWidth()
                    )

                                        PhotoProofPicker(
                        initialPath = photoPath,
                        onPathChanged = { photoPath = it }
                    )

OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Keterangan Tambahan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = countStr.toIntOrNull() ?: 0
                        val age = ageStr.toIntOrNull() ?: 0
                        if (count <= 0 || age <= 0) {
                            formError = "Isi umur dan jumlah ayam mati!"
                            return@Button
                        }

                        val entity = MortalityLogEntity(
                            cycleId = cycle.id,
                            date = date,
                            ageDays = age,
                            count = count,
                            cause = cause,
                            locationBlock = blockLocation,
                            notes = notes,
                            photoUri = photoPath
                        )
                        viewModel.saveMortality(entity) {
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("btn_save_mortality")
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

    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Hapus Catatan Kematian?") },
            text = { Text("Hapus data kematian ${deleteCandidate?.count} ekor pada tanggal ${deleteCandidate?.date}?") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    deleteCandidate?.let { viewModel.deleteMortality(it) }
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
