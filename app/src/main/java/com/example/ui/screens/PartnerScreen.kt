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
import com.example.data.local.entity.PartnerEntity
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val partners by viewModel.partners.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPartner by remember { mutableStateOf<PartnerEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<PartnerEntity?>(null) }

    val idRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Perusahaan Mitra", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_partners")) {
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
                            editingPartner = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("btn_top_add_partner")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Tambah Mitra", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPartner = null
                    showAddDialog = true
                },
                containerColor = FarmGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_partner")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Mitra")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                .testTag("list_partners"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Handshake, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Data Integrator / Kemitraan Broiler", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            Text("Pencatatan kontrak, harga DOC, pakan, harga livebird & bonus performa.", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            if (partners.isEmpty()) {
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
                                imageVector = Icons.Default.Handshake,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Belum Ada Data Perusahaan Mitra",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Daftarkan perusahaan integrator kemitraan (misal: PT Japfa, PT Charoen Pokphand, dll) beserta kontrak harga dan target FCR.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    editingPartner = null
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                modifier = Modifier.testTag("btn_empty_add_partner")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ TAMBAH PERUSAHAAN MITRA", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(partners) { partner ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("item_partner_${partner.id}"),
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
                                Text(partner.companyName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = FarmGreenPrimary)
                                Text("No. Kontrak: ${partner.contractNumber.ifEmpty { "-" }}", fontSize = 12.sp, color = Color.DarkGray)
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingPartner = partner
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { deleteCandidate = partner },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Harga DOC", fontSize = 11.sp, color = Color.Gray)
                                Text(idRupiah.format(partner.chickPrice), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Harga Pakan", fontSize = 11.sp, color = Color.Gray)
                                Text("${idRupiah.format(partner.feedPrice)} /kg", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Harga Ayam Panen", fontSize = 11.sp, color = Color.Gray)
                                Text("${idRupiah.format(partner.liveBirdPrice)} /kg", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PIC Lapangan / TS: ${partner.picName} (${partner.picPhone})", fontSize = 12.sp, color = Color.DarkGray)
                        if (partner.bonusTerms.isNotEmpty()) {
                            Text("Ketentuan Bonus: ${partner.bonusTerms}", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var companyName by remember { mutableStateOf(editingPartner?.companyName ?: "") }
        var address by remember { mutableStateOf(editingPartner?.address ?: "") }
        var picName by remember { mutableStateOf(editingPartner?.picName ?: "") }
        var picPhone by remember { mutableStateOf(editingPartner?.picPhone ?: "") }
        var contractNumber by remember { mutableStateOf(editingPartner?.contractNumber ?: "") }
        var partnershipNumber by remember { mutableStateOf(editingPartner?.partnershipNumber ?: "") }
        var contractDate by remember { mutableStateOf(editingPartner?.contractDate ?: "") }
        var chickPriceStr by remember { mutableStateOf(editingPartner?.chickPrice?.takeIf { it > 0 }?.toString() ?: "") }
        var feedPriceStr by remember { mutableStateOf(editingPartner?.feedPrice?.takeIf { it > 0 }?.toString() ?: "") }
        var liveBirdPriceStr by remember { mutableStateOf(editingPartner?.liveBirdPrice?.takeIf { it > 0 }?.toString() ?: "") }
        var bonusTerms by remember { mutableStateOf(editingPartner?.bonusTerms ?: "") }
        var penaltyTerms by remember { mutableStateOf(editingPartner?.penaltyTerms ?: "") }
        var notes by remember { mutableStateOf(editingPartner?.notes ?: "") }
        var formError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingPartner == null) "Tambah Mitra" else "Edit Mitra", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (formError.isNotEmpty()) {
                        Text(formError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Nama Perusahaan Mitra") },
                        modifier = Modifier.fillMaxWidth().testTag("input_partner_name")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = picName,
                            onValueChange = { picName = it },
                            label = { Text("Nama PIC / TS") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = picPhone,
                            onValueChange = { picPhone = it },
                            label = { Text("No. HP PIC") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = contractNumber,
                            onValueChange = { contractNumber = it },
                            label = { Text("No. Kontrak") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = contractDate,
                            onValueChange = { contractDate = it },
                            label = { Text("Tgl Kontrak") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = chickPriceStr,
                            onValueChange = { chickPriceStr = it },
                            label = { Text("Harga DOC (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = feedPriceStr,
                            onValueChange = { feedPriceStr = it },
                            label = { Text("Harga Pakan/kg (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = liveBirdPriceStr,
                        onValueChange = { liveBirdPriceStr = it },
                        label = { Text("Harga Beli Ayam Hidup / kg (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bonusTerms,
                        onValueChange = { bonusTerms = it },
                        label = { Text("Ketentuan Bonus FCR / Mortalitas") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = penaltyTerms,
                        onValueChange = { penaltyTerms = it },
                        label = { Text("Ketentuan Potongan") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat Kantor Mitra") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (companyName.isBlank()) {
                            formError = "Nama perusahaan mitra wajib diisi!"
                            return@Button
                        }
                        val partnerToSave = PartnerEntity(
                            id = editingPartner?.id ?: 0,
                            companyName = companyName,
                            address = address,
                            picName = picName,
                            picPhone = picPhone,
                            contractNumber = contractNumber,
                            partnershipNumber = partnershipNumber,
                            contractDate = contractDate,
                            contractPrice = 0.0,
                            chickPrice = chickPriceStr.toDoubleOrNull() ?: 0.0,
                            feedPrice = feedPriceStr.toDoubleOrNull() ?: 0.0,
                            liveBirdPrice = liveBirdPriceStr.toDoubleOrNull() ?: 0.0,
                            bonusTerms = bonusTerms,
                            penaltyTerms = penaltyTerms,
                            notes = notes
                        )
                        viewModel.savePartner(partnerToSave) {
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("btn_save_partner")
                ) {
                    Text("Simpan Mitra")
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
            title = { Text("Hapus Mitra?") },
            text = { Text("Hapus data perusahaan '${deleteCandidate?.companyName}'?") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    deleteCandidate?.let { viewModel.deletePartner(it) }
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
