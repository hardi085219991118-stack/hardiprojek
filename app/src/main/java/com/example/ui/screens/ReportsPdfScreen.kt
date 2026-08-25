package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
import com.example.util.FormatHelper
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
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
    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var statusMessage by remember { mutableStateOf("") }

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
                .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                .testTag("screen_reports_pdf"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Generator Dokumen PDF Standar Kemitraan A4", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            Text("Dilengkapi Kop Resmi SEJAHTERA BERSAMA, Format Rupiah Indonesia, Lampiran Foto Bukti & Tanda Tangan.", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "PILIH LAPORAN PDF SPESIFIK FITUR",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary
                    )
                )
            }

            items(reportOptions) { opt ->
                ReportTypeCard(
                    title = "${opt.id + 1}. ${opt.title}",
                    subtitle = opt.subtitle,
                    icon = opt.icon,
                    isSelected = selectedReportType == opt.id,
                    tag = opt.tag
                ) {
                    selectedReportType = opt.id
                }
            }

            // Summary Parameter Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Parameter Laporan yang Akan Digenerate:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Siklus: ${currentCycle?.cycleNumber ?: "Semua Siklus"}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("• Kandang: ${currentCoop?.name ?: "Semua Kandang"} (${currentCoop?.address ?: "-"})", fontSize = 12.sp)
                        Text("• Mitra: ${currentPartner?.companyName ?: "-"}", fontSize = 12.sp)
                        Text("• Populasi Terkini: ${FormatHelper.formatEkor(summary.currentPop)} | Umur: ${FormatHelper.formatHari(summary.ageDays)}", fontSize = 12.sp)
                    }
                }
            }

            if (statusMessage.isNotEmpty()) {
                item {
                    Text(
                        text = statusMessage,
                        color = FarmGreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val profile = farmProfile
                            if (profile == null) {
                                Toast.makeText(context, "Profil usaha belum lengkap. Buka menu Profil Farm.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isGenerating = true
                            statusMessage = "Sedang menyusun dokumen PDF & memproses foto bukti..."

                            coroutineScope.launch {
                                val file = try {
                                    val cycle = currentCycle
                                    val coop = currentCoop
                                    val partner = currentPartner

                                    withContext(Dispatchers.IO) {
                                        when (selectedReportType) {
                                            0 -> {
                                                val log = dailyLogs.lastOrNull() ?: return@withContext null
                                                if (cycle == null) return@withContext null
                                                PdfReportGenerator.generateDailyReportPdf(context, profile, coop, partner, cycle, log, mortalityLogs, feedStocks, medicines, photos)
                                            }
                                            1 -> {
                                                if (cycle == null) return@withContext null
                                                PdfReportGenerator.generatePeriodReportPdf(context, profile, coop, cycle, dailyLogs, photos, "Perkembangan Harian Lengkap")
                                            }
                                            2 -> {
                                                if (cycle == null) return@withContext null
                                                PdfReportGenerator.generatePartnershipReportPdf(context, profile, coop, partner, cycle, dailyLogs, harvests, expenses, feedStocks, photos)
                                            }
                                            3 -> {
                                                if (cycle == null) return@withContext null
                                                PdfReportGenerator.generateCycleEndReportPdf(context, profile, coop, partner, cycle, dailyLogs, harvests, expenses, feedStocks, photos)
                                            }
                                            4 -> {
                                                val targetCoop = coop ?: return@withContext null
                                                PdfReportGenerator.generateCoopPdf(context, profile, targetCoop, photos)
                                            }
                                            5 -> {
                                                PdfReportGenerator.generateExpensePdf(context, profile, coop, cycle, expenses, photos)
                                            }
                                            6 -> {
                                                PdfReportGenerator.generateMortalityPdf(context, profile, coop, cycle, mortalityLogs, photos)
                                            }
                                            7 -> {
                                                PdfReportGenerator.generateFeedPdf(context, profile, coop, cycle, feedStocks, photos)
                                            }
                                            8 -> {
                                                PdfReportGenerator.generateMedicinePdf(context, profile, coop, cycle, medicines, photos)
                                            }
                                            9 -> {
                                                PdfReportGenerator.generateWeightPdf(context, profile, coop, cycle, weightSamples, photos)
                                            }
                                            10 -> {
                                                PdfReportGenerator.generateHarvestPdf(context, profile, coop, partner, cycle, harvests, photos)
                                            }
                                            else -> {
                                                PdfReportGenerator.generatePhotoEvidencePdf(context, profile, coop, cycle, photos)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "Gagal membuat PDF: ${e.localizedMessage ?: "kesalahan tidak diketahui"}"
                                    null
                                }
                                isGenerating = false
                                generatedFile = file
                                if (file != null) {
                                    statusMessage = "PDF Berhasil disimpan: ${file.name}"
                                    PdfReportGenerator.openPdf(context, file)
                                } else {
                                    statusMessage = "Gagal membuat PDF. Pastikan data siklus / kandang aktif tersedia."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_generate_and_preview_pdf")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyusun PDF...")
                        } else {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BUAT & BUKA PREVIEW PDF", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val readyFile = generatedFile
                            if (readyFile != null && readyFile.exists()) {
                                PdfReportGenerator.sharePdf(context, readyFile)
                            } else {
                                val profile = farmProfile ?: return@OutlinedButton
                                coroutineScope.launch {
                                    isGenerating = true
                                    val file = try {
                                        val cycle = currentCycle
                                        val coop = currentCoop
                                        val partner = currentPartner

                                        withContext(Dispatchers.IO) {
                                            when (selectedReportType) {
                                                0 -> {
                                                    val log = dailyLogs.lastOrNull() ?: return@withContext null
                                                    if (cycle == null) return@withContext null
                                                    PdfReportGenerator.generateDailyReportPdf(context, profile, coop, partner, cycle, log, mortalityLogs, feedStocks, medicines, photos)
                                                }
                                                1 -> if (cycle != null) PdfReportGenerator.generatePeriodReportPdf(context, profile, coop, cycle, dailyLogs, photos) else null
                                                2 -> if (cycle != null) PdfReportGenerator.generatePartnershipReportPdf(context, profile, coop, partner, cycle, dailyLogs, harvests, expenses, feedStocks, photos) else null
                                                3 -> if (cycle != null) PdfReportGenerator.generateCycleEndReportPdf(context, profile, coop, partner, cycle, dailyLogs, harvests, expenses, feedStocks, photos) else null
                                                4 -> if (coop != null) PdfReportGenerator.generateCoopPdf(context, profile, coop, photos) else null
                                                5 -> PdfReportGenerator.generateExpensePdf(context, profile, coop, cycle, expenses, photos)
                                                6 -> PdfReportGenerator.generateMortalityPdf(context, profile, coop, cycle, mortalityLogs, photos)
                                                7 -> PdfReportGenerator.generateFeedPdf(context, profile, coop, cycle, feedStocks, photos)
                                                8 -> PdfReportGenerator.generateMedicinePdf(context, profile, coop, cycle, medicines, photos)
                                                9 -> PdfReportGenerator.generateWeightPdf(context, profile, coop, cycle, weightSamples, photos)
                                                10 -> PdfReportGenerator.generateHarvestPdf(context, profile, coop, partner, cycle, harvests, photos)
                                                else -> PdfReportGenerator.generatePhotoEvidencePdf(context, profile, coop, cycle, photos)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                    isGenerating = false
                                    generatedFile = file
                                    if (file != null) {
                                        PdfReportGenerator.sharePdf(context, file)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_share_pdf")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = FarmGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BAGIKAN PDF (WHATSAPP / EMAIL)", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
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
