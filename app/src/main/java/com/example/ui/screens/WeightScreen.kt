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
import com.example.data.local.entity.WeightSampleEntity
import com.example.ui.FarmViewModel
import com.example.ui.components.SimpleLineChart
import com.example.ui.components.StatCard
import com.example.ui.theme.FarmGreenPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val currentCycle by viewModel.currentCycle.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val weightSamples by viewModel.weightSamples.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<WeightSampleEntity?>(null) }

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
                title = { Text("Penimbangan Bobot & ADG", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_weight")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("btn_top_add_weight")) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Tambah Penimbangan", tint = Color.White)
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
                    modifier = Modifier.testTag("fab_add_weight")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Timbang Bobot")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                .testTag("list_weights"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Bobot Rata-rata Terkini",
                        value = "${summary.latestAvgWeightGram.toInt()} Gram",
                        subtitle = "(${numFmt.format(summary.latestAvgWeightKg)} Kg / ekor)",
                        icon = Icons.Default.Scale,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "ADG Rata-rata",
                        value = "${numFmt.format(summary.adgGram)} gr/hari",
                        subtitle = "Pertambahan bobot harian",
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Line Chart
            item {
                val chartPoints = weightSamples.map { it.ageDays to it.averageWeightGram }
                SimpleLineChart(
                    title = "Kurva Pertumbuhan Bobot Ayam (Gram)",
                    dataPoints = chartPoints,
                    unit = "Gram",
                    lineColor = FarmGreenPrimary
                )
            }

            item {
                Text(
                    text = "RIWAYAT PENIMBANGAN SAMPEL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary
                    )
                )
            }

            if (weightSamples.isEmpty()) {
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
                                imageVector = Icons.Default.Scale,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Belum ada data penimbangan bobot sampel.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tekan tombol di bawah untuk mencatat hasil timbang sampel ayam hari ini.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                modifier = Modifier.testTag("btn_empty_add_weight")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ TAMBAH DATA PENIMBANGAN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(weightSamples.reversed()) { sample ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(containerColor = FarmGreenPrimary, contentColor = Color.White) {
                                    Text("Hari ke-${sample.ageDays}")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(sample.date, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sampel: ${sample.sampleCount} Ekor | Total: ${numFmt.format(sample.totalWeightKg)} Kg",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                            if (sample.notes.isNotEmpty()) {
                                Text("Catatan: ${sample.notes}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${sample.averageWeightGram.toInt()} Gram",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FarmGreenPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${numFmt.format(sample.averageWeightKg)} Kg",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            IconButton(onClick = { deleteCandidate = sample }) {
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
        var ageStr by remember { mutableStateOf("") }
        var sampleCountStr by remember { mutableStateOf("") }
        var totalWeightStr by remember { mutableStateOf("") }
        var avgWeightGramStr by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var photoPath by remember { mutableStateOf("") }
        var formError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Input Penimbangan Bobot", fontWeight = FontWeight.Bold) },
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
                            modifier = Modifier.weight(0.8f).testTag("input_weight_age")
                        )
                    }

                    OutlinedTextField(
                        value = sampleCountStr,
                        onValueChange = {
                            sampleCountStr = it
                            val count = it.toIntOrNull() ?: 1
                            val total = totalWeightStr.toDoubleOrNull() ?: 0.0
                            if (total > 0 && count > 0) {
                                avgWeightGramStr = String.format(Locale.US, "%.0f", (total / count) * 1000.0)
                            }
                        },
                        label = { Text("Jumlah Ayam Sampel (Ekor)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_sample_count")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = totalWeightStr,
                            onValueChange = {
                                totalWeightStr = it
                                val total = it.toDoubleOrNull() ?: 0.0
                                val count = sampleCountStr.toIntOrNull() ?: 1
                                if (count > 0) {
                                    avgWeightGramStr = String.format(Locale.US, "%.0f", (total / count) * 1000.0)
                                }
                            },
                            label = { Text("Total Bobot (Kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("input_total_weight_kg")
                        )
                        OutlinedTextField(
                            value = avgWeightGramStr,
                            onValueChange = {
                                avgWeightGramStr = it
                                val avgGram = it.toDoubleOrNull() ?: 0.0
                                val count = sampleCountStr.toIntOrNull() ?: 1
                                totalWeightStr = String.format(Locale.US, "%.2f", (avgGram * count) / 1000.0)
                            },
                            label = { Text("Rata-rata (Gram)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_avg_gram")
                        )
                    }

                                        PhotoProofPicker(
                        initialPath = photoPath,
                        onPathChanged = { photoPath = it }
                    )

OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Keterangan (Lokasi sudut penimbangan dll)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val age = ageStr.toIntOrNull() ?: 0
                        val count = sampleCountStr.toIntOrNull() ?: 0
                        val totalKg = totalWeightStr.toDoubleOrNull() ?: 0.0
                        val avgGram = avgWeightGramStr.toDoubleOrNull() ?: 0.0

                        if (age <= 0 || count <= 0 || avgGram <= 0) {
                            formError = "Lengkapi data penimbangan dengan benar!"
                            return@Button
                        }

                        val sample = WeightSampleEntity(
                            cycleId = cycle.id,
                            date = date,
                            ageDays = age,
                            sampleCount = count,
                            totalWeightKg = totalKg,
                            averageWeightGram = avgGram,
                            averageWeightKg = avgGram / 1000.0,
                            notes = notes,
                            photoUri = photoPath
                        )
                        viewModel.saveWeightSample(sample) {
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("btn_save_weight")
                ) {
                    Text("Simpan Bobot")
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
            title = { Text("Hapus Penimbangan?") },
            text = { Text("Hapus data penimbangan umur ${deleteCandidate?.ageDays} hari?") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    deleteCandidate?.let { viewModel.deleteWeightSample(it) }
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
