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
import com.example.data.local.entity.HarvestEntity
import com.example.ui.FarmViewModel
import com.example.ui.components.StatCard
import com.example.ui.theme.FarmGreenPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val currentCycle by viewModel.currentCycle.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val currentPartner by viewModel.currentPartner.collectAsState()
    val harvests by viewModel.harvests.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<HarvestEntity?>(null) }

    val idRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
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
                title = { Text("Panen & Penjualan Ayam", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_harvest")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("btn_top_add_harvest")) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Catat Panen", tint = Color.White)
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
                    modifier = Modifier.testTag("fab_add_harvest")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Catat Panen")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                .testTag("list_harvests"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Total Ayam Terpanen",
                        value = "${summary.totalHarvestBirds} Ekor",
                        subtitle = "Total Tonase: ${numFmt.format(summary.totalHarvestKg)} Kg",
                        icon = Icons.Default.LocalShipping,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Hasil Penjualan",
                        value = idRupiah.format(summary.totalHarvestRevenue),
                        subtitle = "Hasil bruto panen",
                        icon = Icons.Default.AttachMoney,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                StatCard(
                    title = "Laba Bersih Siklus (Realisasi Panen)",
                    value = idRupiah.format(summary.totalHarvestRevenue - summary.totalExpenses),
                    subtitle = "Hasil Panen (${idRupiah.format(summary.totalHarvestRevenue)}) - Biaya Operasional (${idRupiah.format(summary.totalExpenses)})",
                    contentColor = if (summary.totalHarvestRevenue - summary.totalExpenses >= 0) FarmGreenPrimary else Color.Red,
                    icon = Icons.Default.AccountBalanceWallet
                )
            }

            item {
                Text(
                    text = "SURAT JALAN & RIT PANEN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary
                    )
                )
            }

            if (harvests.isEmpty()) {
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
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Belum Ada Pencatatan Panen Ayam",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tekan tombol di bawah untuk mencatat Surat Jalan (DO), jumlah ekor, timbangan tonase, dan harga panen.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                modifier = Modifier.testTag("btn_empty_add_harvest")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ CATAT PENJUALAN PANEN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(harvests.reversed()) { harv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Panen: ${harv.harvestDate} (Hari ke-${harv.ageDays})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("DO: ${harv.doNumber.ifEmpty { "-" }} | Pembeli: ${harv.buyerName}", fontSize = 12.sp, color = Color.DarkGray)
                            }

                            IconButton(onClick = { deleteCandidate = harv }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.LightGray)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.LightGray.copy(alpha = 0.5f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Jumlah", fontSize = 11.sp, color = Color.Gray)
                                Text("${harv.birdCount} Ekor", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Total Bobot", fontSize = 11.sp, color = Color.Gray)
                                Text("${numFmt.format(harv.totalWeightKg)} Kg", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Rata-rata", fontSize = 11.sp, color = Color.Gray)
                                Text("${numFmt.format(harv.averageWeightKg)} Kg", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            }
                            Column {
                                Text("Total Nilai", fontSize = 11.sp, color = Color.Gray)
                                Text(idRupiah.format(harv.totalRevenue), fontWeight = FontWeight.ExtraBold, color = FarmGreenPrimary)
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
        var countStr by remember { mutableStateOf("") }
        var totalWeightStr by remember { mutableStateOf("") }
        var pricePerKgStr by remember { mutableStateOf(currentPartner?.liveBirdPrice?.takeIf { it > 0 }?.toString() ?: "") }
        var doNumber by remember { mutableStateOf("SJ-PANEN-00${harvests.size + 1}") }
        var buyerName by remember { mutableStateOf(currentPartner?.companyName ?: "") }
        var notes by remember { mutableStateOf("") }
        var photoPath by remember { mutableStateOf("") }
        var formError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Pencatatan Panen Ayam", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (formError.isNotEmpty()) {
                        Text(formError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Tgl Panen") },
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it },
                            label = { Text("Umur (Hari)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f).testTag("input_harvest_age")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = countStr,
                            onValueChange = { countStr = it },
                            label = { Text("Jumlah Ayam (Ekor)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_harvest_count")
                        )
                        OutlinedTextField(
                            value = totalWeightStr,
                            onValueChange = { totalWeightStr = it },
                            label = { Text("Total Timbang (Kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("input_harvest_total_kg")
                        )
                    }

                    OutlinedTextField(
                        value = pricePerKgStr,
                        onValueChange = { pricePerKgStr = it },
                        label = { Text("Harga Beli / Kg (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_harvest_price")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = doNumber,
                            onValueChange = { doNumber = it },
                            label = { Text("No. Surat Jalan") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = buyerName,
                            onValueChange = { buyerName = it },
                            label = { Text("Pembeli / Bakul / Mitra") },
                            modifier = Modifier.weight(1f)
                        )
                    }

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
                        val totalKg = totalWeightStr.toDoubleOrNull() ?: 0.0
                        val pricePerKg = pricePerKgStr.toDoubleOrNull() ?: 0.0

                        if (count <= 0 || totalKg <= 0) {
                            formError = "Isi jumlah ayam dan total timbangan!"
                            return@Button
                        }

                        val avgKg = totalKg / count
                        val totalRev = totalKg * pricePerKg

                        val entity = HarvestEntity(
                            cycleId = cycle.id,
                            harvestDate = date,
                            ageDays = ageStr.toIntOrNull() ?: summary.ageDays,
                            populationBeforeHarvest = summary.currentPop,
                            birdCount = count,
                            totalWeightKg = totalKg,
                            averageWeightKg = avgKg,
                            pricePerKg = pricePerKg,
                            totalRevenue = totalRev,
                            doNumber = doNumber,
                            buyerName = buyerName,
                            notes = notes,
                            photoUri = photoPath
                        )
                        viewModel.saveHarvest(entity) {
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("btn_save_harvest")
                ) {
                    Text("Simpan Data Panen")
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
            title = { Text("Hapus Data Panen?") },
            text = { Text("Hapus data panen tanggal ${deleteCandidate?.harvestDate} (${deleteCandidate?.birdCount} ekor)?") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    deleteCandidate?.let { viewModel.deleteHarvest(it) }
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
