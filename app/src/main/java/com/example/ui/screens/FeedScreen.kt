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
import com.example.data.local.entity.FeedStockEntity
import com.example.ui.FarmViewModel
import com.example.ui.components.StatCard
import com.example.ui.theme.FarmGreenPrimary
import com.example.util.PhotoStorageHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentCycle by viewModel.currentCycle.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val feedStocks by viewModel.feedStocks.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("IN") } // "IN" (Masuk) or "OUT" (Keluar)
    var deleteCandidate by remember { mutableStateOf<FeedStockEntity?>(null) }

    val numFmt = NumberFormat.getNumberInstance(Locale("id", "ID")).apply { maximumFractionDigits = 1 }
    val idRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

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
                title = { Text("Stok & Penggunaan Pakan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_feed")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("btn_top_add_feed")) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Catat Pakan", tint = Color.White)
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
                    modifier = Modifier.testTag("fab_add_feed")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Catat Pakan")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                .testTag("list_feed"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stock Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Pakan Masuk",
                        value = "${numFmt.format(summary.totalFeedInKg)} Kg",
                        subtitle = "${(summary.totalFeedInKg / 50).toInt()} Sak (50kg)",
                        icon = Icons.Default.AddBox,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Pakan Terpakai",
                        value = "${numFmt.format(summary.totalFeedUsedKg)} Kg",
                        subtitle = "${(summary.totalFeedUsedKg / 50).toInt()} Sak",
                        icon = Icons.Default.IndeterminateCheckBox,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                StatCard(
                    title = "Sisa Stok Pakan di Gudang",
                    value = "${numFmt.format(summary.remainingFeedKg)} Kg (${(summary.remainingFeedKg / 50).toInt()} Sak)",
                    subtitle = if (summary.remainingFeedKg < 1000) "⚠️ Stok Rendah! Segera Re-order" else "Kondisi Stok Aman",
                    contentColor = if (summary.remainingFeedKg < 1000) Color(0xFFC62828) else FarmGreenPrimary,
                    icon = Icons.Default.Inventory
                )
            }

            item {
                Text(
                    text = "RIWAYAT TRANSAKSI PAKAN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary
                    )
                )
            }

            if (feedStocks.isEmpty()) {
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
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Belum Ada Riwayat Pakan Masuk / Keluar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tekan tombol di bawah untuk mencatat penerimaan pakan (DO) atau pengeluaran pakan kandang.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                modifier = Modifier.testTag("btn_empty_add_feed")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ CATAT TRANSAKSI PAKAN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(feedStocks) { feed ->
                val isIn = feed.movementType == "IN"
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(if (isIn) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIn) FarmGreenPrimary else Color(0xFFC62828)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isIn) "Pakan Masuk: ${feed.feedType}" else "Pakan Keluar: ${feed.feedType}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${feed.date} | DO: ${feed.doNumber.ifEmpty { "-" }} | Supplier: ${feed.supplier.ifEmpty { "-" }}",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isIn) "+" else "-"}${numFmt.format(feed.totalKg)} Kg",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isIn) FarmGreenPrimary else Color(0xFFC62828),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${numFmt.format(feed.bags)} Sak",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            IconButton(onClick = { deleteCandidate = feed }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (feed.photoUri.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        var showFullPhoto by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 4.dp)
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
                            val bitmap = remember(feed.photoUri) { com.example.util.PhotoStorageHelper.loadBitmapSafe(context, feed.photoUri, maxDim = 800) }
                            AlertDialog(
                                onDismissRequest = { showFullPhoto = false },
                                title = { Text("Bukti Foto Pakan (${feed.feedType})", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Bukti Foto Pakan",
                                                modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Text("Foto tersimpan di: ${feed.photoUri}", fontSize = 12.sp)
                                        }
                                        Text("${feed.date} | ${feed.feedType} - ${feed.bags} Sak (${feed.totalKg} Kg)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

    if (showAddDialog && currentCycle != null) {
        val cycle = currentCycle!!
        var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var feedType by remember { mutableStateOf("") }
        var feedCode by remember { mutableStateOf("") }
        var bagsStr by remember { mutableStateOf("") }
        var kgPerBagStr by remember { mutableStateOf("") }
        var doNumber by remember { mutableStateOf("") }
        var supplier by remember { mutableStateOf("") }
        var pricePerBagStr by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var photoPath by remember { mutableStateOf("") }
        var formError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Pencatatan Pakan", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (formError.isNotEmpty()) {
                        Text(formError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Type Toggle
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == "IN",
                            onClick = { selectedType = "IN" },
                            label = { Text("Pakan Masuk (DO)") },
                            modifier = Modifier.weight(1f).testTag("tab_feed_in")
                        )
                        FilterChip(
                            selected = selectedType == "OUT",
                            onClick = { selectedType = "OUT" },
                            label = { Text("Pakan Digunakan") },
                            modifier = Modifier.weight(1f).testTag("tab_feed_out")
                        )
                    }

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Tanggal") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = feedType,
                        onValueChange = { feedType = it },
                        label = { Text("Jenis Pakan (Pre-Starter / Starter / Finisher)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_feed_type")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bagsStr,
                            onValueChange = { bagsStr = it },
                            label = { Text("Jumlah Sak") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("input_feed_bags")
                        )
                        OutlinedTextField(
                            value = kgPerBagStr,
                            onValueChange = { kgPerBagStr = it },
                            label = { Text("Kg per Sak") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedType == "IN") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = doNumber,
                                onValueChange = { doNumber = it },
                                label = { Text("No. Surat Jalan") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = supplier,
                                onValueChange = { supplier = it },
                                label = { Text("Supplier / Mitra") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = pricePerBagStr,
                            onValueChange = { pricePerBagStr = it },
                            label = { Text("Harga per Sak (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    PhotoProofPicker(
                        initialPath = photoPath,
                        onPathChanged = { photoPath = it },
                        feature = "pakan",
                        title = "Bukti Foto Pakan / Surat Jalan (DO)"
                    )

OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Keterangan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bags = bagsStr.toDoubleOrNull() ?: 0.0
                        val kgPerBag = kgPerBagStr.toDoubleOrNull() ?: 50.0
                        val totalKg = bags * kgPerBag

                        if (bags <= 0) {
                            formError = "Jumlah sak pakan harus lebih dari 0!"
                            return@Button
                        }

                        val pricePerBag = pricePerBagStr.toDoubleOrNull() ?: 0.0
                        val totalPrice = pricePerBag * bags

                        val feedToSave = FeedStockEntity(
                            cycleId = cycle.id,
                            coopId = cycle.coopId,
                            date = date,
                            movementType = selectedType,
                            feedType = feedType,
                            feedCode = feedCode,
                            bags = bags,
                            kgPerBag = kgPerBag,
                            totalKg = totalKg,
                            doNumber = doNumber,
                            supplier = supplier,
                            pricePerBag = pricePerBag,
                            totalPrice = totalPrice,
                            notes = notes,
                            photoUri = photoPath
                        )

                        viewModel.saveFeedStock(feedToSave) {
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("btn_save_feed")
                ) {
                    Text("Simpan Transaksi")
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
            title = { Text("Hapus Catatan Pakan?") },
            text = { Text("Hapus transaksi pakan tanggal ${deleteCandidate?.date}?") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    deleteCandidate?.let { viewModel.deleteFeedStock(it) }
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
