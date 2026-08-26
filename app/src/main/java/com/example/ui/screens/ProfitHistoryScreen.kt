package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DistributionMemberItem
import com.example.data.local.entity.ProfitDistributionEntity
import com.example.ui.FarmViewModel
import com.example.ui.components.ProcessNotificationDialog
import com.example.ui.components.ProcessState
import com.example.ui.components.rememberProcessState
import com.example.ui.theme.FarmGreenPrimary
import com.example.util.FormatHelper
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitHistoryScreen(
    viewModel: FarmViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNewSharing: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var processState by rememberProcessState()

    val profitDistributions by viewModel.profitDistributions.collectAsState()
    val farmProfile by viewModel.farmProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedItemForDetail by remember { mutableStateOf<ProfitDistributionEntity?>(null) }
    var selectedItemForEdit by remember { mutableStateOf<ProfitDistributionEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<ProfitDistributionEntity?>(null) }

    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfSuccessDialog by remember { mutableStateOf(false) }

    val filteredList = remember(profitDistributions, searchQuery) {
        profitDistributions.filter { item ->
            item.period.contains(searchQuery, ignoreCase = true) ||
                    item.date.contains(searchQuery, ignoreCase = true) ||
                    item.notes.contains(searchQuery, ignoreCase = true) ||
                    item.status.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalRecords = profitDistributions.size
    val totalFundsDistributed = profitDistributions.sumOf { it.totalDistributed }
    val totalNetProfit = profitDistributions.sumOf { it.netProfit }

    Scaffold(
        modifier = Modifier.testTag("profit_history_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Riwayat Hasil Pembagian", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Daftar Rekapitulasi & Cetak Ulang PDF",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewSharing,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Bagi Hasil Baru", fontWeight = FontWeight.Bold) },
                containerColor = FarmGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_sharing_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryStatCard(
                    title = "Total Riwayat",
                    value = "$totalRecords Periode",
                    color = FarmGreenPrimary,
                    modifier = Modifier.weight(1f)
                )
                HistoryStatCard(
                    title = "Total Dibagikan",
                    value = FormatHelper.formatRupiah(totalFundsDistributed),
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1.3f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("search_history_input"),
                placeholder = { Text("Cari tanggal, periode, catatan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus Pencarian")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // List of Records
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "Tidak ada riwayat yang sesuai kata kunci." else "Belum ada riwayat pembagian hasil.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Hasil pembagian yang disimpan akan tercatat rapi di sini untuk dicetak atau ditinjau kembali.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToNewSharing,
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buat Pembagian Sekarang")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("history_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        HistoryItemCard(
                            item = item,
                            onViewDetail = { selectedItemForDetail = item },
                            onEdit = { selectedItemForEdit = item },
                            onDelete = { itemToDelete = item },
                            onPrintPdf = {
                                processState = ProcessState.Processing(
                                    title = "MENCETAK PDF",
                                    message = "Memproses dokumen PDF hasil pembagian..."
                                )
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val defaultProfile = farmProfile ?: com.example.data.local.entity.FarmProfileEntity()
                                        val pdfFile = PdfReportGenerator.generateMemberProfitReportPdf(
                                            context = context,
                                            profile = defaultProfile,
                                            distribution = item
                                        )
                                        withContext(Dispatchers.Main) {
                                            processState = ProcessState.Idle
                                            generatedPdfFile = pdfFile
                                            showPdfSuccessDialog = true
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            processState = ProcessState.Error(
                                                title = "Gagal Mencetak",
                                                message = e.localizedMessage ?: "Terjadi kesalahan saat memproses PDF.",
                                                onDismiss = { processState = ProcessState.Idle }
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Detail Member List
    selectedItemForDetail?.let { item ->
        val members = remember(item.memberDetailsJson) {
            PdfReportGenerator.parseDistributionMembers(item.memberDetailsJson)
        }
        AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            title = {
                Column {
                    Text("Rincian Pembagian Anggota", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Periode: ${item.period} (${item.date})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    // Summary row
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = FarmGreenPrimary.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Hasil Bersih:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FormatHelper.formatRupiah(item.netProfit), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Hasil Per Anggota:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FormatHelper.formatRupiah(item.amountPerMember), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sisa Pembulatan:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FormatHelper.formatRupiah(item.roundingRemainder), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Text("Daftar Anggota (${members.size} Orang):", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(members) { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(m.memberName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    if (m.memberNumber.isNotBlank()) {
                                        Text("ID: ${m.memberNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(
                                    FormatHelper.formatRupiah(m.amount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = FarmGreenPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentTarget = item
                        selectedItemForDetail = null
                        processState = ProcessState.Processing(
                            title = "MENCETAK PDF",
                            message = "Memproses rendering PDF..."
                        )
                        scope.launch(Dispatchers.IO) {
                            try {
                                val defaultProfile = farmProfile ?: com.example.data.local.entity.FarmProfileEntity()
                                val pdfFile = PdfReportGenerator.generateMemberProfitReportPdf(
                                    context = context,
                                    profile = defaultProfile,
                                    distribution = currentTarget
                                )
                                withContext(Dispatchers.Main) {
                                    processState = ProcessState.Idle
                                    generatedPdfFile = pdfFile
                                    showPdfSuccessDialog = true
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    processState = ProcessState.Error(
                                        title = "Gagal Cetak PDF",
                                        message = e.localizedMessage ?: "Terjadi kesalahan.",
                                        onDismiss = { processState = ProcessState.Idle }
                                    )
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak Dokumen PDF")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedItemForDetail = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Modal Edit Profit Distribution
    selectedItemForEdit?.let { initialItem ->
        EditProfitDistributionDialog(
            initialItem = initialItem,
            onDismiss = { selectedItemForEdit = null },
            onSave = { updatedItem ->
                selectedItemForEdit = null
                processState = ProcessState.Processing(
                    title = "MEMPERBARUI RIWAYAT",
                    message = "Menyimpan perubahan riwayat pembagian..."
                )
                viewModel.saveProfitDistribution(
                    distribution = updatedItem,
                    onSuccess = {
                        scope.launch {
                            delay(200)
                            processState = ProcessState.Success(
                                title = "Perubahan Tersimpan",
                                message = "Data pembagian berhasil diperbarui.",
                                onDismiss = { processState = ProcessState.Idle }
                            )
                        }
                    },
                    onError = { err ->
                        scope.launch {
                            processState = ProcessState.Error(
                                title = "Gagal Menyimpan",
                                message = err,
                                onDismiss = { processState = ProcessState.Idle }
                            )
                        }
                    }
                )
            }
        )
    }

    // Konfirmasi Hapus Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Riwayat Pembagian?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus data riwayat pembagian periode '${item.period}' tanggal ${item.date} senilai ${FormatHelper.formatRupiah(item.totalDistributed)}? Tindakan ini tidak dapat dibatalkan.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = item
                        itemToDelete = null
                        processState = ProcessState.Processing(
                            title = "MENGHAPUS RIWAYAT",
                            message = "Menghapus data pembagian..."
                        )
                        viewModel.deleteProfitDistribution(
                            distribution = target,
                            onSuccess = {
                                scope.launch {
                                    delay(200)
                                    processState = ProcessState.Success(
                                        title = "Riwayat Terhapus",
                                        message = "Data pembagian telah berhasil dihapus dari database.",
                                        onDismiss = { processState = ProcessState.Idle }
                                    )
                                }
                            },
                            onError = { err ->
                                scope.launch {
                                    processState = ProcessState.Error(
                                        title = "Gagal Menghapus",
                                        message = err,
                                        onDismiss = { processState = ProcessState.Idle }
                                    )
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_history_btn")
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // PDF Success Dialog
    if (showPdfSuccessDialog && generatedPdfFile != null) {
        val file = generatedPdfFile!!
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(36.dp)) },
            title = { Text("Dokumen PDF Siap", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Laporan Hasil Pembagian Anggota format A4 telah berhasil digenerate.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("File: ${file.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FarmGreenPrimary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfSuccessDialog = false
                        PdfReportGenerator.openPdf(context, file)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            showPdfSuccessDialog = false
                            PdfReportGenerator.sharePdf(context, file)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan")
                    }
                    OutlinedButton(
                        onClick = {
                            showPdfSuccessDialog = false
                            PdfReportGenerator.printPdf(context, file)
                        }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak")
                    }
                }
            }
        )
    }

    // Process State Notification Dialog
    ProcessNotificationDialog(state = processState)
}

@Composable
private fun HistoryStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: ProfitDistributionEntity,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrintPdf: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Date, Period, Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.period.ifBlank { "Pembagian Hasil Usaha" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tanggal: ${item.date}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (item.roundingRemainder == 0L) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = if (item.roundingRemainder == 0L) "Sesuai" else "Sisa Pembulatan",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.roundingRemainder == 0L) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Divider(color = Color.Black.copy(alpha = 0.06f))

            // Key Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Hasil Bersih", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatHelper.formatRupiah(item.netProfit), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Anggota", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${item.memberCount} Org", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Per Anggota", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        FormatHelper.formatRupiah(item.amountPerMember),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary
                    )
                }
            }

            if (item.notes.isNotBlank()) {
                Text(
                    text = "Catatan: ${item.notes}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Divider(color = Color.Black.copy(alpha = 0.06f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onViewDetail,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rincian Anggota", fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onPrintPdf,
                        modifier = Modifier.testTag("print_pdf_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Cetak PDF",
                            tint = FarmGreenPrimary
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_history_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Riwayat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_history_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus Riwayat",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfitDistributionDialog(
    initialItem: ProfitDistributionEntity,
    onDismiss: () -> Unit,
    onSave: (ProfitDistributionEntity) -> Unit
) {
    var date by remember { mutableStateOf(initialItem.date) }
    var period by remember { mutableStateOf(initialItem.period) }
    var totalRevenueText by remember { mutableStateOf(initialItem.totalRevenue.toString()) }
    var totalExpenseText by remember { mutableStateOf(initialItem.totalExpense.toString()) }
    var totalDeductionText by remember { mutableStateOf(initialItem.totalDeduction.toString()) }
    var notes by remember { mutableStateOf(initialItem.notes) }

    val members = remember(initialItem.memberDetailsJson) {
        PdfReportGenerator.parseDistributionMembers(initialItem.memberDetailsJson)
    }

    val totalRevenue = totalRevenueText.replace(".", "").replace(",", "").toLongOrNull() ?: 0L
    val totalExpense = totalExpenseText.replace(".", "").replace(",", "").toLongOrNull() ?: 0L
    val totalDeduction = totalDeductionText.replace(".", "").replace(",", "").toLongOrNull() ?: 0L
    val netProfit = (totalRevenue - totalExpense - totalDeduction).coerceAtLeast(0L)
    val memberCount = if (initialItem.memberCount > 0) initialItem.memberCount else members.size.coerceAtLeast(1)

    val amountPerMember = if (memberCount > 0) netProfit / memberCount else 0L
    val totalDistributed = amountPerMember * memberCount
    val roundingRemainder = netProfit - totalDistributed
    val status = if (roundingRemainder == 0L) "PEMBAGIAN SESUAI" else "PEMBAGIAN MEMILIKI SISA PEMBULATAN"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Riwayat Pembagian", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Tanggal Pembagian") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = period,
                    onValueChange = { period = it },
                    label = { Text("Periode") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalRevenueText,
                    onValueChange = { totalRevenueText = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Hasil (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalExpenseText,
                    onValueChange = { totalExpenseText = it.filter { c -> c.isDigit() } },
                    label = { Text("Biaya Operasional (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalDeductionText,
                    onValueChange = { totalDeductionText = it.filter { c -> c.isDigit() } },
                    label = { Text("Potongan (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Keterangan Tambahan / Alasan Perubahan") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Summary preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenPrimary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Hasil Bersih Baru: ${FormatHelper.formatRupiah(netProfit)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Hasil Per Anggota (${memberCount} Org): ${FormatHelper.formatRupiah(amountPerMember)}", fontSize = 12.sp, color = FarmGreenPrimary, fontWeight = FontWeight.Bold)
                        Text("Sisa Pembulatan: ${FormatHelper.formatRupiah(roundingRemainder)}", fontSize = 11.5.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedMembers = members.map { it.copy(amount = amountPerMember) }
                    val updatedJson = PdfReportGenerator.serializeDistributionMembers(updatedMembers)
                    val updated = initialItem.copy(
                        date = date.trim(),
                        period = period.trim(),
                        totalRevenue = totalRevenue,
                        totalExpense = totalExpense,
                        totalDeduction = totalDeduction,
                        netProfit = netProfit,
                        memberCount = memberCount,
                        amountPerMember = amountPerMember,
                        totalDistributed = totalDistributed,
                        roundingRemainder = roundingRemainder,
                        status = status,
                        notes = notes.trim(),
                        memberDetailsJson = updatedJson
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
            ) {
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
