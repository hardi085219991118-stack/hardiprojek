package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DeletePinProtectedButton
import com.example.ui.components.ProcessNotificationDialog
import com.example.ui.components.ProcessState
import com.example.ui.components.rememberProcessState
import com.example.data.local.entity.HarvestEntity
import com.example.ui.FarmViewModel
import com.example.ui.components.StatCard
import com.example.ui.theme.FarmGreenPrimary
import com.example.util.FormatHelper
import com.example.util.PhotoStorageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentCycle by viewModel.currentCycle.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val currentPartner by viewModel.currentPartner.collectAsState()
    val harvests by viewModel.harvests.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<HarvestEntity?>(null) }
    var processState by rememberProcessState()

    val idRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    val numFmt = NumberFormat.getNumberInstance(Locale("id", "ID")).apply { maximumFractionDigits = 2 }

    // Auto-select cycle if currentCycle is null but cycles exist
    LaunchedEffect(currentCycle, cycles) {
        if (currentCycle == null && cycles.isNotEmpty()) {
            val active = cycles.find { it.status == "ACTIVE" } ?: cycles.firstOrNull()
            active?.let { viewModel.selectCycle(it.id) }
        }
    }

    ProcessNotificationDialog(
        state = processState,
        onDismissRequest = { processState = ProcessState.Idle }
    )

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

                        if (harv.photoUri.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            var showFullPhoto by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .clickable { showFullPhoto = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Lihat Bukti Foto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            }

                            if (showFullPhoto) {
                                val bitmap = remember(harv.photoUri) { com.example.util.PhotoStorageHelper.loadBitmapSafe(context, harv.photoUri, maxDim = 800) }
                                AlertDialog(
                                    onDismissRequest = { showFullPhoto = false },
                                    title = { Text("Bukti Foto Panen", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Bukti Foto",
                                                    modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Text("Foto tersimpan di: ${harv.photoUri}", fontSize = 12.sp)
                                            }
                                            Text("${harv.harvestDate} | ${harv.birdCount} Ekor (${numFmt.format(harv.totalWeightKg)} Kg) - DO: ${harv.doNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showFullPhoto = false }) { Text("Tutup") }
                                    }
                                )
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
                        onPathChanged = { photoPath = it },
                        feature = "panen",
                        title = "Bukti Foto Timbangan / DO Panen"
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
                val isProcessing = processState is ProcessState.Processing
                Button(
                    onClick = {
                        if (isProcessing) return@Button
                        val count = countStr.toIntOrNull() ?: 0
                        val totalKg = totalWeightStr.toDoubleOrNull() ?: 0.0
                        val pricePerKg = pricePerKgStr.toDoubleOrNull() ?: 0.0

                        if (count <= 0 || totalKg <= 0) {
                            formError = "Isi jumlah ayam dan total timbangan!"
                            return@Button
                        }

                        val avgKg = totalKg / count
                        val totalRev = totalKg * pricePerKg

                        processState = ProcessState.Processing(
                            title = "MENYIMPAN DATA PANEN",
                            message = "Sedang memproses realisasi penjualan panen...",
                            step = if (photoPath.isNotBlank()) "Menyimpan foto timbangan & DO panen" else "Mencatat tonase dan pendapatan"
                        )

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
                        coroutineScope.launch {
                            delay(300)
                            viewModel.saveHarvest(entity) {
                                showAddDialog = false
                                processState = ProcessState.Success(
                                    title = "DATA PANEN BERHASIL DISIMPAN",
                                    message = "Panen ${FormatHelper.formatEkor(count)} (${FormatHelper.formatKg(totalKg)}) telah tercatat.",
                                    detail = "Total: ${FormatHelper.formatRupiah(totalRev)} | DO: $doNumber"
                                )
                            }
                        }
                    },
                    enabled = !isProcessing,
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
                    val candidate = deleteCandidate
                    deleteCandidate = null
                    if (candidate != null) {
                        processState = ProcessState.Processing(
                            title = "MENGHAPUS DATA PANEN",
                            message = "Sedang menghapus catatan panen..."
                        )
                        coroutineScope.launch {
                            delay(200)
                            viewModel.deleteHarvest(candidate)
                            processState = ProcessState.Success(
                                title = "DATA BERHASIL DIHAPUS",
                                message = "Data panen tanggal ${candidate.harvestDate} telah dihapus."
                            )
                        }
                    }
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
