package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
import com.example.util.BackupHelper
import com.example.util.BackupInspectionResult
import com.example.util.BackupPackageResult
import com.example.util.RestoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupExportScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentCycle by viewModel.currentCycle.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()

    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("Sedang memproses data...") }
    var lastBackupResult by remember { mutableStateOf<BackupPackageResult?>(null) }
    var lastExportedCsv by remember { mutableStateOf<File?>(null) }

    // Restore dialog state
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var inspectionResult by remember { mutableStateOf<BackupInspectionResult?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreSuccessResult by remember { mutableStateOf<RestoreResult?>(null) }

    // File picker launcher for Restore (.zip, .json)
    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedRestoreUri = uri
        isProcessing = true
        processingMessage = "Memeriksa paket cadangan..."
        coroutineScope.launch {
            val inspection = withContext(Dispatchers.IO) {
                BackupHelper.inspectBackup(context, uri)
            }
            isProcessing = false
            inspectionResult = inspection
            if (inspection.isValid) {
                showRestoreConfirmDialog = true
            } else {
                Toast.makeText(
                    context,
                    inspection.errorMessage ?: "File tidak valid atau rusak.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cadangan & Ekspor Data", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Paket Lengkap Database + Foto Fisik", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_backup")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmGreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                .testTag("screen_backup_export"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Information
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(FarmGreenPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Paket Cadangan Portabel (.ZIP)",
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Mencadangkan seluruh tabel database dan berkas foto fisik (Mortalitas, OVK, Pakan, Panen, dll). Dapat dikirim ke HP lain dan dipulihkan secara utuh.",
                                fontSize = 12.sp,
                                color = Color(0xFF333333),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Section 1: Buat Cadangan ZIP Lengkap
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = FarmGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1. Buat File Paket Cadangan (.ZIP)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Mengemas database.json, manifest.json, dan seluruh file foto asli ke dalam satu file ZIP terpadu.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                processingMessage = "Membaca database & mengemas seluruh file foto ke ZIP..."
                                coroutineScope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        BackupHelper.createFullBackupPackage(context, viewModel.repository)
                                    }
                                    isProcessing = false
                                    lastBackupResult = result
                                    if (result.success && result.file != null) {
                                        Toast.makeText(context, "Cadangan lengkap berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, result.errorMessage ?: "Gagal membuat cadangan", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_create_backup_zip")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BUAT FILE CADANGAN LENGKAP", fontWeight = FontWeight.Bold)
                        }

                        // Tampilkan hasil sukses backup
                        AnimatedVisibility(visible = lastBackupResult != null) {
                            lastBackupResult?.let { res ->
                                Spacer(modifier = Modifier.height(14.dp))
                                if (res.success && res.file != null) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "CADANGAN BERHASIL DIBUAT",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32),
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Berkas: ${res.file.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Ukuran: ${res.sizeMb} MB", fontSize = 12.sp)
                                            Text("Total Data: ${res.totalRecords} catatan", fontSize = 12.sp)
                                            Text("Total Foto: ${res.photosArchived} file tersimpan di ZIP", fontSize = 12.sp)
                                            if (res.photosFailed > 0) {
                                                Text(
                                                    "Perhatian: ${res.photosFailed} foto tidak ditemukan di HP.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFC62828),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Rincian kategori foto
                                            if (res.categoriesCount.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    "Rincian Foto: " + res.categoriesCount.entries.joinToString(", ") { "${it.key}: ${it.value}" },
                                                    fontSize = 11.sp,
                                                    color = Color.DarkGray
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    BackupHelper.shareFile(
                                                        context = context,
                                                        file = res.file,
                                                        mimeType = "application/zip",
                                                        title = "Bagikan Paket Cadangan SEJAHTERA BERSAMA"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("btn_share_backup_zip")
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("BAGIKAN CADANGAN (.ZIP)")
                                            }
                                        }
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFC62828))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                res.errorMessage ?: "Gagal membuat cadangan.",
                                                color = Color(0xFFC62828),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Pulihkan Cadangan (Restore)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RestorePage, contentDescription = null, tint = FarmGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("2. Pulihkan Data dari Cadangan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Pilih berkas ZIP cadangan dari HP lama atau perangkat lain. Database dan seluruh file foto bukti akan otomatis dipulihkan ke direktori aplikasi ini.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                openBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/json", "*/*"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_select_backup_file")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = FarmGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PILIH FILE CADANGAN (.ZIP / .JSON)", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                        }
                    }
                }
            }

            // Section 3: Ekspor Harian ke CSV (Excel)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF1565C0))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("3. Ekspor Catatan Harian ke CSV (Excel)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Ekspor seluruh log harian siklus aktif ke format file CSV yang dapat dibuka langsung di Microsoft Excel atau Google Sheets.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (currentCycle == null || dailyLogs.isEmpty()) {
                                    Toast.makeText(context, "Belum ada data catatan harian pada siklus aktif.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isProcessing = true
                                processingMessage = "Membuat file CSV..."
                                coroutineScope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        BackupHelper.exportCsv(context, currentCycle!!, dailyLogs)
                                    }
                                    isProcessing = false
                                    lastExportedCsv = file
                                    if (file != null) {
                                        BackupHelper.shareFile(context, file, "text/csv", "Bagikan Laporan CSV Siklus")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_export_csv")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EKSPOR CSV & BAGIKAN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog Loading / Processing
    if (isProcessing) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FarmGreenPrimary, strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Mohon Tunggu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = { Text(processingMessage, fontSize = 13.sp, color = Color.DarkGray) }
        )
    }

    // Dialog Konfirmasi Pemulihan Cadangan (dengan rincian isi paket)
    if (showRestoreConfirmDialog && inspectionResult != null) {
        val insp = inspectionResult!!
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                selectedRestoreUri = null
            },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(32.dp)) },
            title = { Text("Konfirmasi Pemulihan Data", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "File cadangan valid ditemukan. Pemulihan akan menggantikan data aktif pada akun ini dengan data dari cadangan.",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Divider()
                    Text("Peternakan: ${insp.farmName.ifBlank { "SEJAHTERA BERSAMA" }}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Tanggal Cadangan: ${insp.backupDate}", fontSize = 12.sp)
                    if (insp.isZip) {
                        Text("Format: Paket Lengkap (.ZIP)", fontSize = 12.sp, color = FarmGreenPrimary, fontWeight = FontWeight.Bold)
                        Text("Total Catatan: ${insp.totalRecords} baris data", fontSize = 12.sp)
                        Text("Total Foto: ${insp.totalPhotos} file gambar asli", fontSize = 12.sp)
                    } else {
                        Text("Format: Berkas Database (JSON)", fontSize = 12.sp, color = Color(0xFF1565C0))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedRestoreUri ?: return@Button
                        showRestoreConfirmDialog = false
                        isProcessing = true
                        processingMessage = "Mengekstrak seluruh file foto dan memulihkan basis data..."
                        coroutineScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                BackupHelper.restoreBackup(context, viewModel.repository, uri)
                            }
                            isProcessing = false
                            selectedRestoreUri = null
                            if (result.success) {
                                restoreSuccessResult = result
                            } else {
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Lanjutkan Pemulihan")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    selectedRestoreUri = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Berhasil Pemulihan
    if (restoreSuccessResult != null) {
        val res = restoreSuccessResult!!
        AlertDialog(
            onDismissRequest = { restoreSuccessResult = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(36.dp)) },
            title = { Text("Pemulihan Berhasil!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(res.message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (res.details.isNotBlank()) {
                        Text(res.details, fontSize = 12.sp, color = Color.DarkGray)
                    }
                    Text(
                        "Seluruh data kandang, mitra, siklus, catatan harian, mortalitas, pakan, panen, OVK, dan seluruh bukti foto telah siap dibuka kembali.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { restoreSuccessResult = null },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Selesai")
                }
            }
        )
    }
}
