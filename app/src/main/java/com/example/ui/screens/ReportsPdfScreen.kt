package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.ui.draw.paint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
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

    var selectedReportType by remember { mutableStateOf(0) } // 0: Daily, 1: Periodic, 2: Partnership, 3: Cycle-End
    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var statusMessage by remember { mutableStateOf("") }

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
                            Text("Generator Dokumen PDF Standar Kemitraan", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            Text("Dilengkapi Kop Surat Resmi SEJAHTERA BERSAMA, Slogan, Format Standar, & Kolom Tanda Tangan.", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "PILIH JENIS LAPORAN PDF",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary
                    )
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportTypeCard(
                        title = "1. Laporan Harian Kandang",
                        subtitle = "Catatan rinci populasi, kematian, pakan harian, suhu & OVK hari ini.",
                        icon = Icons.Default.Today,
                        isSelected = selectedReportType == 0,
                        tag = "tab_report_daily"
                    ) { selectedReportType = 0 }

                    ReportTypeCard(
                        title = "2. Laporan Periodik Pemeliharaan",
                        subtitle = "Ringkasan kumulatif mingguan / 14 hari / 30 hari pemeliharaan.",
                        icon = Icons.Default.DateRange,
                        isSelected = selectedReportType == 1,
                        tag = "tab_report_periodic"
                    ) { selectedReportType = 1 }

                    ReportTypeCard(
                        title = "3. Laporan Kemitraan & Rekonsiliasi",
                        subtitle = "Laporan formal ditujukan ke Perusahaan Mitra / TS Lapangan.",
                        icon = Icons.Default.Business,
                        isSelected = selectedReportType == 2,
                        tag = "tab_report_partnership"
                    ) { selectedReportType = 2 }

                    ReportTypeCard(
                        title = "4. Laporan Akhir Siklus (Tutup Buku)",
                        subtitle = "19 Parameter audit lengkap, FCR, IP, Mortalitas, Panen & Laba Bersih.",
                        icon = Icons.Default.WorkspacePremium,
                        isSelected = selectedReportType == 3,
                        tag = "tab_report_cycle_end"
                    ) { selectedReportType = 3 }
                }
            }

            // Summary of active target
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Parameter Laporan yang Akan Digenerate:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Siklus: ${currentCycle?.cycleNumber ?: "-"}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("• Kandang: ${currentCoop?.name ?: "-"} (${currentCoop?.address ?: "-"})", fontSize = 12.sp)
                        Text("• Mitra: ${currentPartner?.companyName ?: "-"}", fontSize = 12.sp)
                        Text("• Populasi Terkini: ${summary.currentPop} Ekor | Umur: Hari ke-${summary.ageDays}", fontSize = 12.sp)
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
                            if (currentCycle == null || currentCoop == null || farmProfile == null) {
                                Toast.makeText(context, "Data siklus/kandang belum lengkap.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isGenerating = true
                            statusMessage = "Sedang membuat dokumen PDF..."

                            coroutineScope.launch {
                                val file = try {
                                    val cycle = currentCycle ?: throw IllegalStateException("Siklus aktif tidak tersedia.")
                                    val coop = currentCoop ?: throw IllegalStateException("Data kandang tidak tersedia.")
                                    val partner = currentPartner
                                    val profile = farmProfile ?: throw IllegalStateException("Profil usaha tidak tersedia.")

                                    withContext(Dispatchers.IO) {
                                        when (selectedReportType) {
                                        0 -> {
                                            val log = dailyLogs.lastOrNull() ?: return@withContext null
                                            PdfReportGenerator.generateDailyReportPdf(context, profile, coop, partner, cycle, log, mortalityLogs, feedStocks, medicines, photos)
                                        }
                                        1 -> {
                                            PdfReportGenerator.generatePeriodReportPdf(context, profile, coop, cycle, dailyLogs, photos, "Perkembangan Harian Lengkap")
                                        }
                                        2 -> {
                                            PdfReportGenerator.generatePartnershipReportPdf(context, profile, coop, partner, cycle, dailyLogs, harvests, expenses, feedStocks, photos)
                                        }
                                        else -> {
                                            PdfReportGenerator.generateCycleEndReportPdf(context, profile, coop, partner, cycle, dailyLogs, harvests, expenses, feedStocks, photos)
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
                                    statusMessage = "PDF Berhasil dibuat: ${file.name}"
                                    PdfReportGenerator.openPdf(context, file)
                                } else {
                                    statusMessage = "Gagal membuat PDF."
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
                            Text("Memproses PDF...")
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
                                if (currentCycle == null || currentCoop == null || farmProfile == null) return@OutlinedButton
                                coroutineScope.launch {
                                    isGenerating = true
                                    val file = try {
                                        val cycle = currentCycle ?: throw IllegalStateException("Siklus aktif tidak tersedia.")
                                        val coop = currentCoop ?: throw IllegalStateException("Data kandang tidak tersedia.")
                                        val partner = currentPartner
                                        val profile = farmProfile ?: throw IllegalStateException("Profil usaha tidak tersedia.")

                                        withContext(Dispatchers.IO) {
                                            when (selectedReportType) {
                                            0 -> {
                                                val log = dailyLogs.lastOrNull() ?: return@withContext null
                                                PdfReportGenerator.generateDailyReportPdf(context, profile, coop, partner, cycle, log, mortalityLogs, feedStocks, medicines, photos)
                                            }
                                            1 -> PdfReportGenerator.generatePeriodReportPdf(context, profile, coop, cycle, dailyLogs, photos, "Perkembangan Harian Lengkap")
                                            2 -> PdfReportGenerator.generatePartnershipReportPdf(context, profile, coop, partner, cycle, dailyLogs, harvests, expenses, feedStocks, photos)
                                            else -> PdfReportGenerator.generateCycleEndReportPdf(context, profile, coop, partner, cycle, dailyLogs, harvests, expenses, feedStocks, photos)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        statusMessage = "Gagal membuat PDF: ${e.localizedMessage ?: "kesalahan tidak diketahui"}"
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

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "FORMULIR INPUT DATA OPERASIONAL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary
                    )
                )
                Text(
                    text = "Laporan PDF terisi otomatis berdasarkan input data pada modul operasional berikut:",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportInputShortcutRow(
                            title = "Penimbangan Bobot Ayam (Sampling)",
                            subtitle = "${weightSamples.size} kali penimbangan tercatat",
                            icon = Icons.Default.Scale,
                            buttonText = "+ Input Bobot",
                            onClick = { onNavigate("weight") }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        ReportInputShortcutRow(
                            title = "Laporan Harian Kandang",
                            subtitle = "${dailyLogs.size} hari log operasional",
                            icon = Icons.Default.EditCalendar,
                            buttonText = "+ Input Harian",
                            onClick = { onNavigate("daily_log") }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        ReportInputShortcutRow(
                            title = "Pencatatan Stok & Mutasi Pakan",
                            subtitle = "${feedStocks.size} transaksi penerimaan/pengeluaran",
                            icon = Icons.Default.Inventory2,
                            buttonText = "+ Input Pakan",
                            onClick = { onNavigate("feed") }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        ReportInputShortcutRow(
                            title = "Mortalitas & Kematian Ayam",
                            subtitle = "${mortalityLogs.size} catatan kematian",
                            icon = Icons.Default.Favorite,
                            buttonText = "+ Catat Mati",
                            onClick = { onNavigate("mortality") }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        ReportInputShortcutRow(
                            title = "Obat, Vitamin, Vaksin (OVK)",
                            subtitle = "${medicines.size} dokumentasi medis",
                            icon = Icons.Default.Vaccines,
                            buttonText = "+ Input OVK",
                            onClick = { onNavigate("medicine") }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        ReportInputShortcutRow(
                            title = "Pencatatan Panen & Penjualan",
                            subtitle = "${harvests.size} surat jalan / rit panen",
                            icon = Icons.Default.LocalShipping,
                            buttonText = "+ Input Panen",
                            onClick = { onNavigate("harvest") }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        ReportInputShortcutRow(
                            title = "Biaya Operasional Kandang",
                            subtitle = "${expenses.size} pos pengeluaran",
                            icon = Icons.Default.ReceiptLong,
                            buttonText = "+ Input Biaya",
                            onClick = { onNavigate("expenses") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportInputShortcutRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    buttonText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FarmGreenPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.DarkGray)
            }
        }
        FilledTonalButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(buttonText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ReportTypeCard(
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
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, FarmGreenPrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = FarmGreenPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isSelected) FarmGreenPrimary else Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) FarmGreenPrimary else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
