package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.ui.draw.paint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var statusText by remember { mutableStateOf("") }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                if (json.isNullOrBlank()) {
                    statusText = "File cadangan kosong atau tidak dapat dibaca."
                } else {
                    pendingRestoreJson = json
                    showRestoreConfirm = true
                }
            } catch (e: Exception) {
                statusText = "Gagal membaca file cadangan: ${e.localizedMessage}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadangan & Ekspor Data", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Keamanan Data & Ekspor Excel/CSV", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            Text("Seluruh data tersimpan secara lokal dan aman. Anda dapat mengekspor atau membuat cadangan berkas kapan saja.", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            // Export to CSV
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = FarmGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ekspor Data Harian ke CSV (Excel)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Ekspor seluruh catatan harian siklus aktif ke format file CSV yang kompatibel dengan Microsoft Excel dan Google Sheets.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (currentCycle == null || dailyLogs.isEmpty()) {
                                    Toast.makeText(context, "Tidak ada data harian untuk diekspor.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isProcessing = true
                                coroutineScope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        BackupHelper.exportCsv(context, currentCycle!!, dailyLogs)
                                    }
                                    isProcessing = false
                                    exportedFile = file
                                    if (file != null) {
                                        statusText = "Berhasil diekspor: ${file.name}"
                                        BackupHelper.shareFile(context, file, "text/csv", "Bagikan Laporan CSV")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("btn_export_csv")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EKSPOR CSV & BAGIKAN")
                        }
                    }
                }
            }

            // Backup JSON
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = FarmGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cadangkan Seluruh Basis Data (JSON)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Membuat salinan cadangan lengkap seluruh kandang, mitra, siklus, pakan, obat, dan panen dalam satu file cadangan aman.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        BackupHelper.createBackupJson(context, viewModel.repository)
                                    }
                                    isProcessing = false
                                    exportedFile = file
                                    if (file != null) {
                                        statusText = "Backup berhasil: ${file.name}"
                                        BackupHelper.shareFile(context, file, "application/json", "Bagikan Cadangan Database")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth().testTag("btn_backup_json")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BUAT FILE CADANGAN (JSON)")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = FarmGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pulihkan Data dari Cadangan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Pilih file JSON cadangan SEJAHTERA BERSAMA. Data akun aktif akan diganti dengan isi cadangan setelah konfirmasi.",
                            fontSize = 12.sp, color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { openBackupLauncher.launch(arrayOf("application/json", "text/json", "*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PILIH FILE CADANGAN")
                        }
                    }
                }
            }

            if (statusText.isNotEmpty()) {
                item {
                    Text(
                        text = statusText,
                        color = FarmGreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false; pendingRestoreJson = null },
            title = { Text("Konfirmasi Pemulihan Data", fontWeight = FontWeight.Bold) },
            text = { Text("Pemulihan akan mengganti data peternakan pada akun aktif. Pastikan Anda sudah membuat cadangan terbaru sebelum melanjutkan.") },
            confirmButton = {
                Button(onClick = {
                    val json = pendingRestoreJson ?: return@Button
                    showRestoreConfirm = false
                    isProcessing = true
                    coroutineScope.launch {
                        val ok = withContext(Dispatchers.IO) { BackupHelper.restoreFromJson(context, viewModel.repository, json) }
                        isProcessing = false
                        pendingRestoreJson = null
                        statusText = if (ok) "Pemulihan data berhasil." else "Pemulihan gagal. File tidak valid atau data tidak dapat dipulihkan."
                    }
                }) { Text("Pulihkan") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false; pendingRestoreJson = null }) { Text("Batal") }
            }
        )
    }
}
