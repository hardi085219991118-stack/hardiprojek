package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
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
import com.example.data.local.entity.MedicineEntity
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
import com.example.util.FormatHelper
import com.example.util.PdfReportGenerator
import com.example.util.PhotoStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineScreen(
    viewModel: FarmViewModel,
    onBack: UnitCallback = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentCycle by viewModel.currentCycle.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val medicines by viewModel.medicines.collectAsState()
    val profile by viewModel.farmProfile.collectAsState()
    val coop by viewModel.currentCoop.collectAsState()
    val partner by viewModel.currentPartner.collectAsState()
    val photos by viewModel.photos.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<MedicineEntity?>(null) }
    var viewingPhotoMed by remember { mutableStateOf<Pair<Int, MedicineEntity>?>(null) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var processState by rememberProcessState()

    // Auto-select cycle if currentCycle is null but cycles exist
    LaunchedEffect(currentCycle, cycles) {
        if (currentCycle == null && cycles.isNotEmpty()) {
            val active = cycles.find { it.status == "ACTIVE" } ?: cycles.firstOrNull()
            active?.let { viewModel.selectCycle(it.id) }
        }
    }

    // Urutkan data secara deterministik (sesuai urutan tabel PDF)
    val sortedMedicines = remember(medicines) {
        medicines.sortedWith(
            compareBy<MedicineEntity> { it.date }
                .thenBy { it.ageDays }
                .thenBy { it.id }
        )
    }

    val exportMedicinePdf = {
        val cycle = currentCycle
        if (cycle == null) {
            processState = ProcessState.Error(
                title = "Siklus Belum Dipilih",
                message = "Silakan pilih atau mulai siklus pemeliharaan aktif terlebih dahulu."
            )
        } else {
            val validation = PdfReportGenerator.validateReportData(
                reportType = 8,
                profile = profile,
                coop = coop,
                cycle = cycle,
                dailyLogs = emptyList(),
                mortalities = emptyList(),
                feedStocks = emptyList(),
                expenses = emptyList(),
                harvests = emptyList(),
                weights = emptyList(),
                medicines = sortedMedicines
            )

            if (!validation.isValid) {
                processState = ProcessState.Error(
                    title = validation.message,
                    message = validation.detail
                )
            } else {
                processState = ProcessState.Processing(
                    title = "MEMBUAT LAPORAN PDF OVK",
                    message = "Menyusun tabel log rincian dan menyinkronkan lampiran bukti foto...",
                    step = "Render Dokumen Standar A4"
                )

                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val file = PdfReportGenerator.generateMedicinePdf(
                            context = context,
                            profile = profile!!,
                            coop = coop,
                            cycle = cycle,
                            medicines = sortedMedicines,
                            photos = photos
                        )
                        withContext(Dispatchers.Main) {
                            if (file.exists()) {
                                generatedPdfFile = file
                                processState = ProcessState.Success(
                                    title = "PDF OVK BERHASIL DIBUAT",
                                    message = "Laporan Penggunaan Obat, Vaksin & Vitamin siap dibuka atau dibagikan.",
                                    detail = "Nama Dokumen: ${file.name}",
                                    onDismiss = {
                                        PdfReportGenerator.openPdf(context, file)
                                    }
                                )
                            } else {
                                processState = ProcessState.Error(
                                    title = "Gagal Membuat PDF",
                                    message = "File dokumen tidak dapat disimpan ke penyimpanan perangkat."
                                )
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            processState = ProcessState.Error(
                                title = "Terjadi Kesalahan PDF",
                                message = "Gagal mengekspor dokumen: ${e.localizedMessage ?: "Kesalahan tak terduga"}"
                            )
                        }
                    }
                }
            }
        }
    }

    ProcessNotificationDialog(
        state = processState,
        onDismissRequest = { processState = ProcessState.Idle }
    )

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
                    IconButton(
                        onClick = { exportMedicinePdf() },
                        modifier = Modifier.testTag("btn_export_pdf_medicine")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF OVK", tint = Color.White)
                    }
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
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Medication, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pencatatan Medis & Biosekuriti", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                                Text("Dokumentasi pemberian vaksin, multivitamin, desinfektan dan pengobatan ayam.", fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }

                        val countVac = sortedMedicines.count { it.category.equals("Vaksin", true) }
                        val countVit = sortedMedicines.count { it.category.equals("Vitamin", true) }
                        val countMed = sortedMedicines.count { it.category.equals("Obat", true) }
                        val countPhotos = sortedMedicines.count { it.photoUri.isNotBlank() }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total: ${sortedMedicines.size} Tindakan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            Text("Vaksin: $countVac | Vit: $countVit | Obat: $countMed", fontSize = 11.sp, color = Color.DarkGray)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { exportMedicinePdf() },
                                modifier = Modifier.weight(1f).testTag("btn_export_pdf_banner"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmGreenPrimary)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cetak PDF OVK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.weight(1f).testTag("btn_add_medicine_banner"),
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Catat OVK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (sortedMedicines.isEmpty()) {
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

            itemsIndexed(sortedMedicines) { index, med ->
                val tableNumber = index + 1
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("item_medicine_$tableNumber"),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Header Bar Kartu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = FarmGreenPrimary
                                ) {
                                    Text(
                                        text = "#$tableNumber",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Badge(
                                    containerColor = when (med.category) {
                                        "Vaksin" -> Color(0xFF1565C0)
                                        "Vitamin" -> Color(0xFFF57F17)
                                        "Obat" -> Color(0xFFC62828)
                                        "Desinfektan" -> Color(0xFF6A1B9A)
                                        else -> FarmGreenPrimary
                                    },
                                    contentColor = Color.White
                                ) {
                                    Text(med.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = med.productName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1B5E20)
                                )
                            }

                            IconButton(
                                onClick = { deleteCandidate = med },
                                modifier = Modifier.size(28.dp).testTag("btn_delete_med_$tableNumber")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.8.dp)

                        // Info Rincian
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("• Dosis: ${med.dose.ifBlank { "-" }}", fontSize = 12.sp, color = Color.DarkGray)
                                Text("• Aplikasi: ${med.method.ifBlank { "-" }}", fontSize = 12.sp, color = Color.DarkGray)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("• Tanggal: ${med.date}", fontSize = 12.sp, color = Color.DarkGray)
                                Text("• Umur Ayam: Hari ke-${med.ageDays}", fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }

                        if (med.purpose.isNotBlank()) {
                            Text("• Indikasi/Tujuan: ${med.purpose}", fontSize = 12.sp, color = Color(0xFF37474F), fontWeight = FontWeight.Medium)
                        }
                        if (med.notes.isNotBlank()) {
                            Text("• Catatan: ${med.notes}", fontSize = 11.sp, color = Color.Gray)
                        }

                        // Bagian Status & Bukti Foto
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (med.photoUri.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .clickable { viewingPhotoMed = tableNumber to med }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("btn_view_photo_$tableNumber"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Lihat Bukti Foto (#$tableNumber)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF5F5F5))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.HideImage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Belum Ada Foto", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Text("ID Rekam: #${med.id}", fontSize = 10.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog Pratinjau Bukti Foto dengan Rincian Terstruktur
    if (viewingPhotoMed != null) {
        val (tableNum, med) = viewingPhotoMed!!
        val bitmap = remember(med.photoUri) { PhotoStorageHelper.loadBitmapSafe(context, med.photoUri, maxDim = 1000) }

        AlertDialog(
            onDismissRequest = { viewingPhotoMed = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = FarmGreenPrimary) {
                        Text(
                            "#$tableNum",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text("Bukti Foto: ${med.productName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Bukti Foto ${med.productName}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("File foto tidak dapat dimuat atau telah dipindahkan", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• Kategori: ${med.category} | Umur: Hari ke-${med.ageDays}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("• Tanggal: ${med.date}", fontSize = 12.sp)
                            Text("• Dosis: ${med.dose.ifBlank { "-" }} | Aplikasi: ${med.method.ifBlank { "-" }}", fontSize = 12.sp)
                            if (med.purpose.isNotBlank()) {
                                Text("• Indikasi/Tujuan: ${med.purpose}", fontSize = 12.sp)
                            }
                            Text("• Relasi PDF: Log Baris #${tableNum} (ID: #${med.id})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingPhotoMed = null }) {
                    Text("Tutup", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                }
            }
        )
    }

    if (showAddDialog && currentCycle != null) {
        val cycle = currentCycle!!
        var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var category by remember { mutableStateOf("Vitamin") }
        var name by remember { mutableStateOf("") }
        var dose by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("Air Minum") }
        val calculatedAge = remember(date, cycle.chickInDate) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dLog = sdf.parse(date)?.time ?: 0L
                val dChick = sdf.parse(cycle.chickInDate)?.time ?: 0L
                val diffDays = ((dLog - dChick) / (1000 * 60 * 60 * 24)).toInt() + 1
                diffDays.coerceAtLeast(1)
            } catch (e: Exception) {
                1
            }
        }
        var ageStr by remember { mutableStateOf(calculatedAge.toString()) }
        var purpose by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var photoPath by remember { mutableStateOf("") }
        var formError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Input Obat / Vitamin / Vaksin", fontWeight = FontWeight.Bold) },
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
                            onValueChange = {
                                date = it
                                ageStr = calculatedAge.toString()
                            },
                            label = { Text("Tanggal (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1.3f).testTag("input_med_date")
                        )
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it },
                            label = { Text("Umur (Hari)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f).testTag("input_med_age")
                        )
                    }

                    var categoryExpanded by remember { mutableStateOf(false) }
                    val categories = listOf("Vaksin", "Vitamin", "Obat", "Elektrolit", "Desinfektan", "Suplemen", "Lainnya")
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            readOnly = true,
                            label = { Text("Kategori Tindakan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("input_med_category")
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Produk / Merk (Wajib)") },
                        placeholder = { Text("Contoh: Nopstress, Zagro Amilyte, PRO BALAC") },
                        modifier = Modifier.fillMaxWidth().testTag("input_med_name")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dose,
                            onValueChange = { dose = it },
                            label = { Text("Dosis") },
                            placeholder = { Text("1 gr/2L air") },
                            modifier = Modifier.weight(1f).testTag("input_med_dose")
                        )
                        OutlinedTextField(
                            value = method,
                            onValueChange = { method = it },
                            label = { Text("Aplikasi/Rute") },
                            placeholder = { Text("Air Minum / Spray") },
                            modifier = Modifier.weight(1f).testTag("input_med_method")
                        )
                    }

                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("Indikasi / Tujuan Pemberian") },
                        placeholder = { Text("Contoh: Anti-stres setelah chick in") },
                        modifier = Modifier.fillMaxWidth().testTag("input_med_purpose")
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Tambahan") },
                        modifier = Modifier.fillMaxWidth().testTag("input_med_notes")
                    )

                    PhotoProofPicker(
                        initialPath = photoPath,
                        onPathChanged = { photoPath = it },
                        feature = "obat",
                        title = "Bukti Foto Kemasan / Aplikasi OVK"
                    )
                }
            },
            confirmButton = {
                val isProcessing = processState is ProcessState.Processing
                Button(
                    onClick = {
                        if (isProcessing) return@Button
                        if (name.isBlank()) {
                            formError = "Nama produk wajib diisi!"
                            return@Button
                        }

                        processState = ProcessState.Processing(
                            title = "MENYIMPAN DATA OVK",
                            message = "Sedang menyimpan catatan obat/vaksin/vitamin...",
                            step = if (photoPath.isNotBlank()) "Menyimpan foto bukti kemasan/aplikasi" else "Mencatat perlakuan medis"
                        )

                        val entity = MedicineEntity(
                            cycleId = cycle.id,
                            date = date,
                            category = category,
                            productName = name,
                            dose = dose,
                            quantity = 1.0,
                            unit = "Aplikasi",
                            method = method,
                            ageDays = ageStr.toIntOrNull() ?: 1,
                            purpose = purpose,
                            notes = notes,
                            photoUri = photoPath
                        )
                        coroutineScope.launch {
                            delay(300)
                            viewModel.saveMedicine(entity) {
                                showAddDialog = false
                                processState = ProcessState.Success(
                                    title = "DATA OVK BERHASIL DISIMPAN",
                                    message = "Pemberian $category ($name) telah dicatat ke rekam medis.",
                                    detail = "Dosis: $dose | Metode: $method"
                                )
                            }
                        }
                    },
                    enabled = !isProcessing,
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
            text = { Text("Hapus pemberian '${deleteCandidate?.productName}' (${deleteCandidate?.category})?") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    val candidate = deleteCandidate
                    deleteCandidate = null
                    if (candidate != null) {
                        processState = ProcessState.Processing(
                            title = "MENGHAPUS CATATAN MEDIS",
                            message = "Sedang menghapus riwayat OVK..."
                        )
                        coroutineScope.launch {
                            delay(200)
                            viewModel.deleteMedicine(candidate)
                            processState = ProcessState.Success(
                                title = "DATA BERHASIL DIHAPUS",
                                message = "Catatan OVK '${candidate.productName}' telah dihapus."
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

typealias UnitCallback = () -> Unit
