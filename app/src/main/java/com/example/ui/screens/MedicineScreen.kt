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
import com.example.data.local.entity.MedicineEntity
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val currentCycle by viewModel.currentCycle.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val medicines by viewModel.medicines.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<MedicineEntity?>(null) }

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
                title = { Text("Pemberian Obat, Vitamin & Vaksin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_medicine")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("btn_top_add_medicine")) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Catat Obat", tint = Color.White)
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
                    modifier = Modifier.testTag("fab_add_medicine")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Catat Obat")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                .testTag("list_medicines"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Medication, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Pencatatan Medis & Biosekuriti", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            Text("Dokumentasi pemberian vaksin, multivitamin, desinfektan dan pengobatan ayam.", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            if (medicines.isEmpty()) {
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
                                imageVector = Icons.Default.Vaccines,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Belum Ada Catatan Pemberian Obat/Vaksin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tekan tombol di bawah untuk mendokumentasikan pemberian vaksin, vitamin, antibiotik, atau desinfektan.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                modifier = Modifier.testTag("btn_empty_add_medicine")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ CATAT OBAT & VAKSIN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(medicines.reversed()) { med ->
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
                                Badge(
                                    containerColor = when (med.category) {
                                        "Vaksin" -> Color(0xFF1565C0)
                                        "Vitamin" -> Color(0xFFF57F17)
                                        "Obat" -> Color(0xFFC62828)
                                        else -> FarmGreenPrimary
                                    },
                                    contentColor = Color.White
                                ) {
                                    Text(med.category, modifier = Modifier.padding(horizontal = 4.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(med.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Dosis: ${med.dose} | Metode: ${med.method}", fontSize = 12.sp, color = Color.DarkGray)
                            Text("Tanggal: ${med.date} (Hari ke-${med.ageDays}) | Tujuan: ${med.purpose}", fontSize = 11.sp, color = Color.Gray)
                        }

                        IconButton(onClick = { deleteCandidate = med }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && currentCycle != null) {
        val cycle = currentCycle!!
        var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var category by remember { mutableStateOf("Vitamin") }
        var name by remember { mutableStateOf("") }
        var dose by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("") }
        var ageStr by remember { mutableStateOf("") }
        var purpose by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var photoPath by remember { mutableStateOf("") }
        var formError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Input Obat / Vitamin / Vaksin", fontWeight = FontWeight.Bold) },
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
                            modifier = Modifier.weight(0.8f).testTag("input_med_age")
                        )
                    }

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori (Vitamin/Vaksin/Obat/Desinfektan)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_med_category")
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Produk / Merk") },
                        modifier = Modifier.fillMaxWidth().testTag("input_med_name")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dose,
                            onValueChange = { dose = it },
                            label = { Text("Dosis") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = method,
                            onValueChange = { method = it },
                            label = { Text("Aplikasi (Minum/Spray/dll)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PhotoProofPicker(
                        initialPath = photoPath,
                        onPathChanged = { photoPath = it }
                    )

                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("Indikasi / Tujuan Pemberian") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            formError = "Nama produk wajib diisi!"
                            return@Button
                        }
                        val entity = MedicineEntity(
                            cycleId = cycle.id,
                            date = date,
                            category = category,
                            productName = name,
                            dose = dose,
                            quantity = 1.0,
                            unit = "Bungkus",
                            method = method,
                            ageDays = ageStr.toIntOrNull() ?: 0,
                            purpose = purpose,
                            notes = notes,
                            photoUri = photoPath
                        )
                        viewModel.saveMedicine(entity) {
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("btn_save_medicine")
                ) {
                    Text("Simpan Medis")
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
            title = { Text("Hapus Catatan Medis?") },
            text = { Text("Hapus pemberian '${deleteCandidate?.productName}'?") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    deleteCandidate?.let { viewModel.deleteMedicine(it) }
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
