package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.util.PdfReportGenerator
import com.example.util.UserSessionManager
import com.example.util.SecurityHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDispatchScreen(
    cycleId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).farmDao() }
    val currentUserId = remember { UserSessionManager.getCurrentUserId(context) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Kirim Laporan, 1: Riwayat Pengiriman

    // Data State
    val allCycles by dao.getAllCycles().collectAsState(initial = emptyList())
    val cycles = remember(allCycles, currentUserId) { allCycles.filter { it.userId == currentUserId } }
    val allDispatchHistory by dao.getAllDispatchHistory().collectAsState(initial = emptyList())
    val dispatchHistory = remember(allDispatchHistory, currentUserId) { allDispatchHistory.filter { it.userId == currentUserId } }
    var currentCycleId by remember { mutableLongStateOf(cycleId) }

    var farmProfile by remember { mutableStateOf<FarmProfileEntity?>(null) }
    var partner by remember { mutableStateOf<PartnerEntity?>(null) }
    var coop by remember { mutableStateOf<CoopEntity?>(null) }
    var dailyLogs by remember { mutableStateOf<List<DailyLogEntity>>(emptyList()) }
    var harvests by remember { mutableStateOf<List<HarvestEntity>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<ExpenseEntity>>(emptyList()) }
    var feedStocks by remember { mutableStateOf<List<FeedStockEntity>>(emptyList()) }

    // Form inputs for dispatch
    var selectedReportType by remember { mutableStateOf("Laporan Harian & Recording") }
    var selectedChannel by remember { mutableStateOf("WHATSAPP") } // WHATSAPP or EMAIL
    var recipientPhone by remember { mutableStateOf("") }
    var recipientEmail by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val reportTypes = listOf(
        "Laporan Harian & Recording",
        "Laporan RHPP / Evaluasi Panen Akhir Siklus",
        "Laporan Periodik Mingguan"
    )

    LaunchedEffect(currentCycleId, cycles) {
        if (currentCycleId == 0L && cycles.isNotEmpty()) {
            currentCycleId = cycles.first().id
        }
        withContext(Dispatchers.IO) {
            farmProfile = dao.getFarmProfileDirect(currentUserId)
            val cycle = cycles.find { it.id == currentCycleId }
            if (cycle != null) {
                partner = cycle.partnerId.let { dao.getPartnerById(it) }
                coop = dao.getCoopById(cycle.coopId)
            }
            dailyLogs = dao.getDailyLogsDirect(currentCycleId)
            harvests = dao.getHarvestsDirect(currentCycleId)
            expenses = dao.getExpensesDirect(currentCycleId)
            feedStocks = dao.getFeedStocksDirect(currentCycleId)

            // Pre-fill recipient defaults from Partner PIC
            partner?.let { p ->
                withContext(Dispatchers.Main) {
                    if (recipientName.isBlank()) recipientName = p.picName
                    if (recipientPhone.isBlank()) recipientPhone = p.picPhone
                    if (recipientEmail.isBlank()) recipientEmail = p!!.companyName.takeIf { it.contains("@") } ?: ""
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pengiriman Laporan Kemitraan",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAF7))
        ) {
            // Tab Selector: Form Kirim vs Riwayat
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF1B5E20)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kirim Laporan", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Riwayat (${dispatchHistory.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // Send Report Form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cycle Info Card
                    val activeCycle = cycles.find { it.id == currentCycleId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = activeCycle?.cycleNumber ?: "Siklus Broiler",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                )
                                Text(
                                    text = coop?.name ?: "Kandang A",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                )
                            }
                            Text(
                                text = "Mitra: ${partner?.companyName ?: "-"}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF333333))
                            )
                            Text(
                                text = "Peternak: ${farmProfile?.farmName ?: "SEJAHTERA BERSAMA"} (${farmProfile?.ownerName ?: "-"})",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
                            )
                        }
                    }

                    // Select Report Type
                    Text("Pilih Jenis Dokumen Laporan:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    var expandedReportType by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedReportType,
                        onExpandedChange = { expandedReportType = !expandedReportType }
                    ) {
                        OutlinedTextField(
                            value = selectedReportType,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFC62828)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReportType) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedReportType,
                            onDismissRequest = { expandedReportType = false }
                        ) {
                            reportTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedReportType = type
                                        expandedReportType = false
                                    }
                                )
                            }
                        }
                    }

                    // Select Channel (WhatsApp vs Email)
                    Text("Pilih Media Pengiriman Resmi:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedChannel = "WHATSAPP" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedChannel == "WHATSAPP") Color(0xFFE8F5E9) else Color.Transparent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (selectedChannel == "WHATSAPP") Color(0xFF2E7D32) else Color.LightGray
                            )
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = if (selectedChannel == "WHATSAPP") Color(0xFF2E7D32) else Color.Gray)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontWeight = if (selectedChannel == "WHATSAPP") FontWeight.Bold else FontWeight.Normal, color = if (selectedChannel == "WHATSAPP") Color(0xFF2E7D32) else Color.Gray)
                        }

                        OutlinedButton(
                            onClick = { selectedChannel = "EMAIL" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedChannel == "EMAIL") Color(0xFFE3F2FD) else Color.Transparent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (selectedChannel == "EMAIL") Color(0xFF1565C0) else Color.LightGray
                            )
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = if (selectedChannel == "EMAIL") Color(0xFF1565C0) else Color.Gray)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email", fontWeight = if (selectedChannel == "EMAIL") FontWeight.Bold else FontWeight.Normal, color = if (selectedChannel == "EMAIL") Color(0xFF1565C0) else Color.Gray)
                        }
                    }

                    // Recipient Fields
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("Nama Penerima / TS Kemitraan") },
                        placeholder = { Text("Contoh: Bpk. Budi Santoso (TS Broiler)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2E7D32)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedChannel == "WHATSAPP") {
                        OutlinedTextField(
                            value = recipientPhone,
                            onValueChange = { recipientPhone = it },
                            label = { Text("Nomor WhatsApp Penerima *") },
                            placeholder = { Text("Contoh: 081198765432") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2E7D32)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = recipientEmail,
                            onValueChange = { recipientEmail = it },
                            label = { Text("Alamat Email Penerima *") },
                            placeholder = { Text("Contoh: ts.kemitraan@perusahaan.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF1565C0)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = { Text("Catatan Tambahan untuk Penerima") },
                        placeholder = { Text("Contoh: Laporan harian recording sampai hari ke-21 kondisi prima.") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    // Dispatch Button
                    Button(
                        onClick = {
                            val cycle = activeCycle ?: return@Button
                            val profile = farmProfile
                            val p = partner
                            val c = coop
                            if (profile == null || p == null || c == null) {
                                Toast.makeText(context, "Data profil, mitra, atau kandang belum lengkap.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedChannel == "WHATSAPP" && !SecurityHelper.isValidPhone(recipientPhone)) {
                                Toast.makeText(context, "Nomor WhatsApp penerima belum valid.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedChannel == "EMAIL" && !SecurityHelper.isValidEmail(recipientEmail)) {
                                Toast.makeText(context, "Alamat email penerima belum valid.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isGeneratingPdf = true
                            coroutineScope.launch {
                                try {
                                    // 1. Generate PDF Report File
                                    val pdfFile = withContext(Dispatchers.IO) {
                                        if (selectedReportType.contains("RHPP") || selectedReportType.contains("Akhir")) {
                                            PdfReportGenerator.generateCycleEndReportPdf(
                                                context = context,
                                                profile = profile!!,
                                                coop = c!!,
                                                partner = p!!,
                                                cycle = cycle,
                                                dailyLogs = dailyLogs,
                                                harvests = harvests,
                                                expenses = expenses,
                                                feedStocks = feedStocks
                                            )
                                        } else if (selectedReportType.contains("Periodik")) {
                                            PdfReportGenerator.generatePeriodReportPdf(
                                                context = context,
                                                profile = profile!!,
                                                coop = c!!,
                                                cycle = cycle,
                                                logs = dailyLogs
                                            )
                                        } else {
                                            val latestLog = dailyLogs.lastOrNull()
                                                ?: throw IllegalStateException("Belum ada laporan harian yang dapat dibuat menjadi PDF.")
                                            PdfReportGenerator.generateDailyReportPdf(
                                                context = context,
                                                profile = profile!!,
                                                coop = c!!,
                                                partner = p!!,
                                                cycle = cycle,
                                                dailyLog = latestLog,
                                                mortalities = emptyList(),
                                                feedRecords = feedStocks,
                                                medicines = emptyList()
                                            )
                                        }
                                    }

                                    val pdfUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        pdfFile
                                    )

                                    // Build summary message
                                    val totalDead = dailyLogs.sumOf { it.deadCount }
                                    val totalFeedKg = dailyLogs.sumOf { it.feedGivenKg }
                                    val totalHarvestKg = harvests.sumOf { it.totalWeightKg }
                                    val popAwal = cycle.docCount
                                    val mortPct = if (popAwal > 0) (totalDead.toDouble() / popAwal) * 100 else 0.0

                                    val messageBuilder = StringBuilder().apply {
                                        append("📋 *LAPORAN RESMI KEMITRAAN BROILER*\n")
                                        append("🏛 *${profile!!.farmName}*\n")
                                        append("═══════════════════════\n")
                                        append("• *Siklus:* ${cycle.cycleNumber}\n")
                                        append("• *Kandang:* ${c!!.name}\n")
                                        append("• *Mitra:* ${p!!.companyName}\n")
                                        append("• *DOC Masuk:* ${String.format("%,d", cycle.docCount)} Ekor (${cycle.docStrain})\n")
                                        append("• *Total Kematian:* ${String.format("%,d", totalDead)} Ekor (${String.format(Locale.US, "%.2f", mortPct)}%)\n")
                                        append("• *Total Pakan:* ${String.format("%,.1f", totalFeedKg)} Kg\n")
                                        if (harvests.isNotEmpty()) {
                                            append("• *Total Panen:* ${String.format("%,.1f", totalHarvestKg)} Kg (${String.format("%,d", harvests.sumOf { it.birdCount })} Ekor)\n")
                                        }
                                        if (additionalNotes.isNotBlank()) {
                                            append("• *Catatan:* $additionalNotes\n")
                                        }
                                        append("═══════════════════════\n")
                                        append("📄 _Dokumen PDF Terlampir via Aplikasi Sejahtera Bersama_")
                                    }

                                    val finalMessage = messageBuilder.toString()
                                    val targetRecipient = if (selectedChannel == "WHATSAPP") recipientPhone else recipientEmail

                                    if (selectedChannel == "WHATSAPP") {
                                        val cleanPhone = recipientPhone.replace("-", "").replace(" ", "").replace("+", "")
                                        val formattedPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone

                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, pdfUri)
                                            putExtra(Intent.EXTRA_TEXT, finalMessage)
                                            putExtra("jid", "$formattedPhone@s.whatsapp.net")
                                            setPackage("com.whatsapp")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }

                                        try {
                                            context.startActivity(shareIntent)
                                        } catch (e: Exception) {
                                            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, pdfUri)
                                                putExtra(Intent.EXTRA_TEXT, finalMessage)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(fallbackIntent, "Kirim via WhatsApp / Aplikasi Lain"))
                                        }
                                    } else {
                                        val emailIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                                            putExtra(Intent.EXTRA_SUBJECT, "Laporan Resmi Kemitraan Broiler - ${profile!!.farmName} - ${cycle.cycleNumber}")
                                            putExtra(Intent.EXTRA_TEXT, finalMessage)
                                            putExtra(Intent.EXTRA_STREAM, pdfUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(emailIntent, "Kirim Email Laporan"))
                                    }

                                    // 3. Save to Dispatch History in Room DB
                                    val now = Date()
                                    val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID")).format(now)
                                    val timeStr = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(now)

                                    val historyItem = ReportDispatchHistoryEntity(
                                        userId = currentUserId,
                                        cycleId = currentCycleId,
                                        reportType = selectedReportType,
                                        method = selectedChannel,
                                        destination = targetRecipient.ifBlank { recipientName.ifBlank { "PIC Kemitraan" } },
                                        date = dateStr,
                                        time = timeStr,
                                        fileName = pdfFile.name,
                                        status = "BERHASIL DISIAPKAN",
                                        notes = additionalNotes.ifBlank { "Dokumen $selectedReportType dikirim melalui $selectedChannel." }
                                    )
                                    withContext(Dispatchers.IO) {
                                        dao.insertDispatchHistory(historyItem)
                                    }

                                    isGeneratingPdf = false
                                    Toast.makeText(context, "Laporan $selectedReportType berhasil diproses untuk dikirim!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    isGeneratingPdf = false
                                    Toast.makeText(context, "Gagal membuat/mengirim laporan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isGeneratingPdf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedChannel == "WHATSAPP") Color(0xFF2E7D32) else Color(0xFF1565C0)
                        )
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Menyusun Dokumen PDF...", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (selectedChannel == "WHATSAPP") "KIRIM VIA WHATSAPP (PDF + RINGKASAN)" else "KIRIM VIA EMAIL RESMI (PDF ATTACHMENT)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            } else {
                // Tab 1: Dispatch History View
                if (dispatchHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HistoryToggleOff,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Belum ada riwayat pengiriman laporan",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                            )
                            Button(
                                onClick = { selectedTab = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kirim Laporan Pertama")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dispatchHistory, key = { it.id }) { item ->
                            DispatchHistoryCard(
                                item = item,
                                onReshare = {
                                    val reportsDir = File(context.cacheDir, "reports")
                                    val file = File(reportsDir, item.fileName)
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_TEXT, "Laporan Kemitraan: ${item.reportType}")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Kirim Ulang Laporan"))
                                    } else {
                                        Toast.makeText(context, "Dokumen PDF tersimpan: ${item.fileName}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatchHistoryCard(
    item: ReportDispatchHistoryEntity,
    onReshare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (item.method == "WHATSAPP") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.method == "WHATSAPP") Icons.Default.Chat else Icons.Default.Email,
                                contentDescription = null,
                                tint = if (item.method == "WHATSAPP") Color(0xFF2E7D32) else Color(0xFF1565C0),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.reportType,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Channel: ${item.method} • ${item.date} ${item.time}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Divider(color = Color(0xFFEEEEEE))

            Text(
                text = "Tujuan / PIC: ${item.destination}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )

            if (item.notes.isNotBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReshare,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan Ulang", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
