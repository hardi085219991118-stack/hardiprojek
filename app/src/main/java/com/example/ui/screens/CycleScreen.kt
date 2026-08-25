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
import com.example.data.local.entity.CycleEntity
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val cycles by viewModel.cycles.collectAsState()
    val coops by viewModel.coops.collectAsState()
    val partners by viewModel.partners.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCycle by remember { mutableStateOf<CycleEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<CycleEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Siklus & Periode Pemeliharaan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_cycles")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmGreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = {
                            editingCycle = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("btn_top_add_cycle")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Buat Siklus", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCycle = null
                    showAddDialog = true
                },
                containerColor = FarmGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_cycle")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat Siklus Baru")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                .testTag("list_cycles"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Manajemen Multi-Siklus Pemeliharaan", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            Text("Data antar-siklus tersimpan terpisah dan dapat dibuka kembali kapan saja.", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            if (cycles.isEmpty()) {
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
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Belum Ada Siklus Pemeliharaan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Mulai siklus baru dengan menentukan kandang, mitra kemitraan, tanggal chick-in, jumlah DOC masuk, dan target FCR.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    editingCycle = null
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                modifier = Modifier.testTag("btn_empty_add_cycle")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ BUAT SIKLUS BARU", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(cycles) { cyc ->
                val coopName = coops.find { it.id == cyc.coopId }?.name ?: "Kandang #${cyc.coopId}"
                val partnerName = partners.find { it.id == cyc.partnerId }?.companyName ?: "Mitra #${cyc.partnerId}"

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("item_cycle_${cyc.id}"),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cyc.cycleNumber, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = FarmGreenPrimary)
                                Text("Kandang: $coopName | Mitra: $partnerName", fontSize = 12.sp, color = Color.DarkGray)
                            }
                            AssistChip(
                                onClick = {
                                    viewModel.selectCycle(cyc.id)
                                },
                                label = { Text(cyc.status, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (cyc.status == "ACTIVE") Color(0xFFE8F5E9) else Color(0xFFECEFF1),
                                    labelColor = if (cyc.status == "ACTIVE") FarmGreenPrimary else Color.DarkGray
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("DOC Masuk", fontSize = 11.sp, color = Color.Gray)
                                Text("${cyc.docCount} Ekor", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Strain", fontSize = 11.sp, color = Color.Gray)
                                Text(cyc.docStrain, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Target FCR / Bobot", fontSize = 11.sp, color = Color.Gray)
                                Text("${cyc.targetFcr} / ${cyc.targetWeightKg}kg", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            }
                            Column {
                                Text("Tgl Chick-In", fontSize = 11.sp, color = Color.Gray)
                                Text(cyc.chickInDate, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                    viewModel.selectCycle(cyc.id)
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Buka di Dashboard", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { deleteCandidate = cyc }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var cycleNum by remember { mutableStateOf(editingCycle?.cycleNumber ?: "Siklus 00${cycles.size + 1} – ${SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())}") }
        var selectedCoopId by remember { mutableStateOf(editingCycle?.coopId ?: coops.firstOrNull()?.id ?: 0L) }
        var selectedPartnerId by remember { mutableStateOf(editingCycle?.partnerId ?: partners.firstOrNull()?.id ?: 0L) }
        var chickInDate by remember { mutableStateOf(editingCycle?.chickInDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var targetHarvestAgeStr by remember { mutableStateOf(editingCycle?.targetHarvestAgeDays?.takeIf { it > 0 }?.toString() ?: "") }
        var docCountStr by remember { mutableStateOf(editingCycle?.docCount?.takeIf { it > 0 }?.toString() ?: "") }
        var docStrain by remember { mutableStateOf(editingCycle?.docStrain ?: "") }
        var docType by remember { mutableStateOf(editingCycle?.docType ?: "") }
        var docPriceStr by remember { mutableStateOf(editingCycle?.docPricePerHead?.takeIf { it > 0 }?.toString() ?: "") }
        var targetFcrStr by remember { mutableStateOf(editingCycle?.targetFcr?.takeIf { it > 0 }?.toString() ?: "") }
        var targetWeightStr by remember { mutableStateOf(editingCycle?.targetWeightKg?.takeIf { it > 0 }?.toString() ?: "") }
        var status by remember { mutableStateOf(editingCycle?.status ?: "ACTIVE") }
        var notes by remember { mutableStateOf(editingCycle?.notes ?: "") }
        var formError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Buat Siklus Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (formError.isNotEmpty()) {
                        Text(formError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = cycleNum,
                        onValueChange = { cycleNum = it },
                        label = { Text("Nomor / Nama Siklus") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cycle_name")
                    )

                    // Coop selector
                    Text("Pilih Kandang:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    coops.forEach { coop ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedCoopId == coop.id,
                                onClick = { selectedCoopId = coop.id }
                            )
                            Text("${coop.name} (${coop.capacity} Ekor)", fontSize = 13.sp)
                        }
                    }

                    // Partner selector
                    Text("Pilih Mitra:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    partners.forEach { part ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPartnerId == part.id,
                                onClick = { selectedPartnerId = part.id }
                            )
                            Text(part.companyName, fontSize = 13.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = chickInDate,
                            onValueChange = { chickInDate = it },
                            label = { Text("Tgl Chick-In") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetHarvestAgeStr,
                            onValueChange = { targetHarvestAgeStr = it },
                            label = { Text("Target Umur (Hari)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = docCountStr,
                            onValueChange = { docCountStr = it },
                            label = { Text("Jumlah DOC Masuk") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_cycle_doc_count")
                        )
                        OutlinedTextField(
                            value = docPriceStr,
                            onValueChange = { docPriceStr = it },
                            label = { Text("Harga DOC/ekor") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = docStrain,
                            onValueChange = { docStrain = it },
                            label = { Text("Strain (Cobb/Ross/dll)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetFcrStr,
                            onValueChange = { targetFcrStr = it },
                            label = { Text("Target FCR Mitra") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = targetWeightStr,
                        onValueChange = { targetWeightStr = it },
                        label = { Text("Target Bobot Panen (Kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Siklus") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val doc = docCountStr.toIntOrNull() ?: 0
                        if (doc <= 0) {
                            formError = "Jumlah DOC harus lebih dari 0!"
                            return@Button
                        }
                        if (selectedCoopId == 0L || selectedPartnerId == 0L) {
                            formError = "Pilih kandang dan perusahaan mitra!"
                            return@Button
                        }

                        val cycleToSave = CycleEntity(
                            id = editingCycle?.id ?: 0,
                            cycleNumber = cycleNum,
                            coopId = selectedCoopId,
                            partnerId = selectedPartnerId,
                            chickInDate = chickInDate,
                            targetHarvestDate = "",
                            targetHarvestAgeDays = targetHarvestAgeStr.toIntOrNull() ?: 35,
                            docCount = doc,
                            docStrain = docStrain,
                            docType = docType,
                            docPricePerHead = docPriceStr.toDoubleOrNull() ?: 7500.0,
                            targetFcr = targetFcrStr.toDoubleOrNull() ?: 0.0,
                            targetWeightKg = targetWeightStr.toDoubleOrNull() ?: 0.0,
                            status = status,
                            notes = notes
                        )
                        viewModel.saveCycle(cycleToSave) {
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("btn_save_cycle")
                ) {
                    Text("Simpan Siklus")
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
            title = { Text("Hapus Siklus?") },
            text = { Text("Apakah Anda yakin ingin menghapus '${deleteCandidate?.cycleNumber}'? Seluruh data operasional pada siklus ini akan terhapus.") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    deleteCandidate?.let { viewModel.deleteCycle(it) }
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
