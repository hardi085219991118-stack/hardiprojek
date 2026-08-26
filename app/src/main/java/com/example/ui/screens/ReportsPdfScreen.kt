package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsPdfScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentCycle by viewModel.currentCycle.collectAsState()
    val currentCoop by viewModel.currentCoop.collectAsState()
    val currentPartner by viewModel.currentPartner.collectAsState()
    val farmProfile by viewModel.farmProfile.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    val mortalityLogs by viewModel.mortalityLogs.collectAsState()
    val feedStocks by viewModel.feedStocks.collectAsState()
    val weightSamples by viewModel.weightSamples.collectAsState()
    val medicines by viewModel.medicines.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val harvests by viewModel.harvests.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()

    var selectedReportType by remember { mutableIntStateOf(0) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var processState by rememberProcessState()

    val reportOptions = listOf(
        ReportOption(0, "Laporan Harian Kandang", "Catatan rinci populasi, kematian, pakan harian, suhu & OVK hari ini.", Icons.Default.Today, "tab_report_daily"),
        ReportOption(1, "Laporan Periodik Pemeliharaan", "Tabel ringkasan kumulatif perkembangan harian (Landscape A4).", Icons.Default.DateRange, "tab_report_periodic"),
        ReportOption(2, "Laporan Kemitraan & Rekonsiliasi", "Laporan formal ditujukan ke Perusahaan Mitra / TS Lapangan.", Icons.Default.Business, "tab_report_partnership"),
        ReportOption(3, "Laporan Akhir Siklus (Tutup Buku)", "19 Parameter audit lengkap, FCR, IP, Mortalitas, Panen & Laba Bersih.", Icons.Default.WorkspacePremium, "tab_report_cycle_end"),
        ReportOption(4, "Laporan Data & Profil Kandang", "Spesifikasi teknis kandang, kapasitas, dimensi & foto fisik kandang.", Icons.Default.HomeWork, "tab_report_coop"),
        ReportOption(5, "Laporan Keuangan & Kas Kandang", "Buku kas operasional, nota/kuitansi pembelian & bukti foto kas.", Icons.Default.ReceiptLong, "tab_report_expense"),
        ReportOption(6, "Laporan Mortalitas & Afkir", "Rekap kematian, afkir, gejala klinis & bukti foto bangkai/nekropsi.", Icons.Default.HeartBroken, "tab_report_mortality"),
        ReportOption(7, "Laporan Penerimaan & Penggunaan Pakan", "Mutasi sak & kg pakan, nomor DO surat jalan & foto pakan.", Icons.Default.Grain, "tab_report_feed"),
        ReportOption(8, "Laporan Obat, Vaksin & Vitamin", "Jadwal OVK, dosis, rute aplikasi & dokumentasi foto kemasan.", Icons.Default.Medication, "tab_report_medicine"),
        ReportOption(9, "Laporan Sampling Bobot Ayam", "Histori sampling gram bobot mingguan, keseragaman & foto timbangan.", Icons.Default.Scale, "tab_report_weight"),
        ReportOption(10, "Laporan Penjualan & Panen", "Realisasi tonase, ekor panen, harga jual, nomor DO & foto timbangan.", Icons.Default.LocalShipping, "tab_report_harvest"),
        ReportOption(11, "Dokumentasi Foto Bukti Lapangan", "Arsip lengkap seluruh foto bukti terwatermark GPS & tanggal.", Icons.Default.PhotoLibrary, "tab_report_photo_doc")
    )

    fun generateSelectedPdf(onComplete: (File?) -> Unit) {
        val profile = farmProfile
        val cycle = currentCycle
        val coop = currentCoop
        val partner = currentPartner

        // 1. Indikator Proses: Memeriksa data laporan...
        processState = ProcessState.Processing(
            title = "MEMVALIDASI DATA LAPORAN",
            message = "Memeriksa data laporan...",
            step = "1/3 Verifikasi integritas & konsistensi data"
        )

        coroutineScope.launch {
            try {
                delay(300)
                // 2. Validasi Integritas Data
                val validation = PdfReportGenerator.validateReportData(
                    reportType = selectedReportType,
                    profile = profile,
                    coop = coop,
                    cycle = cycle,
                    dailyLogs = dailyLogs,
                    mortalities = mortalityLogs,
                    feedStocks = feedStocks,
                    expenses = expenses,
                    harvests = harvests,
                    weights = weightSamples,
                    medicines = medicines
                )

                if (!validation.isValid) {
                    processState = ProcessState.Error(
                        title = "Data Belum Lengkap / Tidak Sesuai",
                        message = "Data belum dapat dibuat menjadi PDF karena terdapat ketidaksesuaian: ${validation.message}\n\n${validation.detail}"
                    )
                    return@launch
                }

                // 3. Indikator Proses: Data berhasil diverifikasi -> Membuat PDF...
                processState = ProcessState.Processing(
                    title = "MEMBUAT DOKUMEN PDF",
                    message = "Data berhasil diverifikasi. Membuat PDF...",
                    step = "2/3 Merender halaman standar A4 & watermark foto"
                )
                delay(350)

                val file = withContext(Dispatchers.IO) {
                    when (selectedReportType) {
                        0 -> {
                            val log = dailyLogs.lastOrNull() ?: return@withContext null
                            PdfReportGenerator.generateDailyReportPdf(context, profile!!, coop, partner, cycle!!, log, mortalityLogs, feedStocks, medicines, photos)
                        }
                        1 -> {
                            PdfReportGenerator.generatePeriodReportPdf(context, profile!!, coop, cycle!!, dailyLogs, photos, "Perkembangan Harian Lengkap")
                        }
                        2 -> {
                            PdfReportGenerator.generatePartnershipReportPdf(context, profile!!, coop, partner, cycle!!, dailyLogs, harvests, expenses, feedStocks, photos)
                        }
                        3 -> {
                            PdfReportGenerator.generateCycleEndReportPdf(context, profile!!, coop, partner, cycle!!, dailyLogs, harvests, expenses, feedStocks, photos)
                        }
                        4 -> {
                            val targetCoop = coop ?: return@withContext null
                            PdfReportGenerator.generateCoopPdf(context, profile!!, targetCoop, photos)
                        }
                        5 -> {
                            PdfReportGenerator.generateExpensePdf(context, profile!!, coop, cycle, expenses, photos)
                        }
                        6 -> {
                            PdfReportGenerator.generateMortalityPdf(context, profile!!, coop, cycle, mortalityLogs, photos)
                        }
                        7 -> {
                            PdfReportGenerator.generateFeedPdf(context, profile!!, coop, cycle, feedStocks, photos)
                        }
                        8 -> {
                            PdfReportGenerator.generateMedicinePdf(context, profile!!, coop, cycle, medicines, photos)
                        }
                        9 -> {
                            PdfReportGenerator.generateWeightPdf(context, profile!!, coop, cycle, weightSamples, photos)
                        }
                        10 -> {
                            PdfReportGenerator.generateHarvestPdf(context, profile!!, coop, partner, cycle, harvests, photos)
                        }
                        else -> {
                            PdfReportGenerator.generatePhotoEvidencePdf(context, profile!!, coop, cycle, photos)
                        }
                    }
                }

                if (file != null && file.exists()) {
                    generatedFile = file
                    processState = ProcessState.Success(
                        title = "PDF BERHASIL DIBUAT",
                        message = "PDF berhasil dibuat dan siap disimpan.",
                        detail = "Nama Dokumen: ${file.name}",
                        onDismiss = {
                            onComplete(file)
                        }
                    )
                } else {
                    processState = ProcessState.Error(
                        title = "Gagal Membuat PDF",
                        message = "Data belum dapat dibuat menjadi PDF karena terdapat ketidaksesuaian. Silakan periksa data terlebih dahulu."
                    )
                }
            } catch (e: Exception) {
                processState = ProcessState.Error(
                    title = "Terjadi Kesalahan",
                    message = "Gagal memproses dokumen: ${e.localizedMessage ?: "Kesalahan tak terduga"}"
                )
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
                title = { Text("Pusat Laporan PDF Resmi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_reports_pdf")) {
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenPrimary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("SEJAHTERA BERSAMA PDF ENGINE", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FarmGreenPrimary)
                                Text("Format Standar A4 Cetak Resmi, Subtotal Akurat & Bukti Foto GPS", fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("profit_sharing") }
                        .testTag("btn_goto_profit_sharing"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E7D32)))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF1B5E20), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Laporan Hasil Pembagian Anggota", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1B5E20))
                            Text("Kalkulasi sama rata, sisa pembulatan, rincian per anggota & tanda tangan lapang.", fontSize = 11.5.sp, color = Color.DarkGray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF1B5E20))
                    }
                }
            }

            item {
                Text("PILIH JENIS LAPORAN:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.DarkGray)
            }


            items(reportOptions) { opt ->
                ReportTypeCard(
                    title = opt.title,
                    subtitle = opt.subtitle,
                    icon = opt.icon,
                    isSelected = selectedReportType == opt.id,
                    tag = opt.tag,
                    onClick = { selectedReportType = opt.id }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Parameter Siklus & Kandang Aktif:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Siklus: ${currentCycle?.cycleNumber ?: "Semua Siklus"}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("• Kandang: ${currentCoop?.name ?: "Semua Kandang"} (${currentCoop?.address ?: "-"})", fontSize = 12.sp)
                        Text("• Mitra: ${currentPartner?.companyName ?: "-"}", fontSize = 12.sp)
                        Text("• Populasi Terkini: ${FormatHelper.formatEkor(summary.currentPop)} | Umur: ${FormatHelper.formatHari(summary.ageDays)}", fontSize = 12.sp)
                    }
                }
            }

            // Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val isProcessingNow = processState is ProcessState.Processing

                    Button(
                        onClick = {
                            if (isProcessingNow) return@Button
                            generateSelectedPdf { file ->
                                if (file != null) {
                                    PdfReportGenerator.openPdf(context, file)
                                }
                            }
                        },
                        enabled = !isProcessingNow,
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_generate_and_preview_pdf")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BUAT & BUKA PREVIEW PDF", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isProcessingNow) return@OutlinedButton
                                val readyFile = generatedFile
                                if (readyFile != null && readyFile.exists()) {
                                    PdfReportGenerator.sharePdf(context, readyFile)
                                } else {
                                    generateSelectedPdf { file ->
                                        if (file != null) {
                                            PdfReportGenerator.sharePdf(context, file)
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessingNow,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_share_pdf")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BAGIKAN", fontWeight = FontWeight.Bold, color = FarmGreenPrimary, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (isProcessingNow) return@OutlinedButton
                                val readyFile = generatedFile
                                if (readyFile != null && readyFile.exists()) {
                                    PdfReportGenerator.printPdf(context, readyFile)
                                } else {
                                    generateSelectedPdf { file ->
                                        if (file != null) {
                                            PdfReportGenerator.printPdf(context, file)
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessingNow,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_print_pdf")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CETAK (PRINT)", fontWeight = FontWeight.Bold, color = FarmGreenPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class ReportOption(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
private fun ReportTypeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FarmGreenPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FarmGreenPrimary), width = 2.dp) else null
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) FarmGreenPrimary else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isSelected) FarmGreenPrimary else Color.Unspecified
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = Color.DarkGray
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = FarmGreenPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
