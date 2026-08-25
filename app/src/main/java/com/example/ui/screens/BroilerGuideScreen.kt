package com.example.ui.screens

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroilerGuideScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit,
    onNavigateToAlarm: () -> Unit = {}
) {
    val context = LocalContext.current
    val cycle by viewModel.currentCycle.collectAsState()
    val coop by viewModel.currentCoop.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()
    val logs by viewModel.dailyLogs.collectAsState()
    val weights by viewModel.weightSamples.collectAsState()
    val todaySchedules by viewModel.todayFeedSchedules.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Target Hari Ini, 1: 15 Bab Panduan Lengkap

    var selectedChapterId by remember { mutableIntStateOf(4) } // Default to Bab 4: Manajemen Pakan
    var searchQuery by remember { mutableStateOf("") }
    var savingPdf by remember { mutableStateOf(false) }

    val date = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val age = cycle?.let { BroilerGuideData.ageDays(it.chickInDate) } ?: 0
    val guide = BroilerGuideData.forDay(age)
    val todayLog = logs.firstOrNull { it.date == date } ?: logs.maxByOrNull { it.ageDays }
    val rupiah = NumberFormat.getNumberInstance(Locale("id", "ID"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panduan Budidaya Broiler", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Standar Profesional IP > 400", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_guide")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            savingPdf = true
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    generateGuidePdf(context, cycle?.cycleNumber ?: "Siklus Aktif", coop?.name ?: "Kandang", date, age, guide, summary, todayLog)
                                }
                                savingPdf = false
                                openGuidePdf(context, file)
                            }
                        },
                        enabled = !savingPdf,
                        modifier = Modifier.testTag("btn_export_guide_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Cetak PDF Panduan")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Hari Ke-$age & Checklist", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Today, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("15 Bab Buku Panduan", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // TAB 0: TARGET HARI INI & CHECKLIST
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = FarmGreenPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "SEJAHTERA BERSAMA",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "Target IP > 400",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "PANDUAN HARI KE-$age",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "Kandang: ${coop?.name ?: "Belum dipilih"} • ${cycle?.cycleNumber ?: "Siklus Belum Ada"}",
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Quick Action: Alarm Cerdas Pakan (Bab 4)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToAlarm() }
                                .testTag("card_guide_alarm_quick_access"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color(0xFFE65100), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessAlarm,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "⏰ ALARM CERDAS PAKAN",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = Color(0xFFBF360C)
                                        )
                                        Text(
                                            text = "Jadwal 4x sehari: 06.00, 11.00, 16.00, 20.00 (Bab 4)",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                                Button(
                                    onClick = onNavigateToAlarm,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_open_alarm_from_guide")
                                ) {
                                    Text("Buka Alarm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Target Spesifik Hari Ini
                    item {
                        Text("🎯 TARGET HARIAN (STANDAR RESMI PDF)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }


                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TargetBadge("Bobot Target", "${guide.targetWeightG} gram", Icons.Default.Scale)
                                    TargetBadge("Pakan Harian", "${guide.feedG} g/ekor", Icons.Default.Restaurant)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TargetBadge("Air Minum", "± ${guide.waterMl} ml/ekor", Icons.Default.WaterDrop)
                                    TargetBadge("Target Suhu", guide.temp, Icons.Default.Thermostat)
                                }
                                Divider(Modifier.padding(vertical = 4.dp))
                                Text("💡 Pencahayaan: ${guide.light}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("🌬️ Ventilasi: ${guide.ventilation}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("💧 Kelembapan: ${guide.humidity}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Analisis Aktual vs Target
                    item {
                        Text("📊 KONDISI AKTUAL & EVALUASI", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val actualWeight = weights.maxByOrNull { it.ageDays }?.averageWeightGram ?: 0.0
                                val actualFeed = todayLog?.let { if (summary.currentPop > 0) it.feedGivenKg * 1000.0 / summary.currentPop else 0.0 } ?: 0.0
                                val actualWater = todayLog?.let { if (summary.currentPop > 0) it.waterIntakeLiters * 1000.0 / summary.currentPop else 0.0 } ?: 0.0

                                EvaluasiRow("Bobot Badan", if (actualWeight > 0) "${rupiah.format(actualWeight)} g" else "Belum ada data", actualWeight, guide.targetWeightG.toDouble(), higherIsGood = true)
                                EvaluasiRow("Konsumsi Pakan", if (actualFeed > 0) "${rupiah.format(actualFeed)} g/ekor" else "Belum ada data", actualFeed, guide.feedG.toDouble(), higherIsGood = true)
                                EvaluasiRow("Konsumsi Air", if (actualWater > 0) "${rupiah.format(actualWater)} ml/ekor" else "Belum ada data", actualWater, guide.waterMl.toDouble(), higherIsGood = true)

                                Divider(Modifier.padding(vertical = 4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Populasi Saat Ini:", fontSize = 13.sp)
                                    Text("${summary.currentPop} ekor", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Mortalitas Kumulatif:", fontSize = 13.sp)
                                    Text("${String.format(Locale.US, "%.2f", summary.mortalityPercent)}% (Target: ≤ 2,50%)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Livability (Daya Hidup):", fontSize = 13.sp)
                                    Text("${String.format(Locale.US, "%.2f", summary.survivalRatePercent)}% (Target: ≥ 97,50%)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                if (summary.mortalityPercent >= 2.5) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "⚠️ Peringatan Mortalitas: Angka kematian mendekati atau melebihi ambang batas 2,5%. Segera lakukan audit biosekuriti, periksa air, pakan, suhu brooding, dan konsultasikan ke Technical Service (TS).",
                                            modifier = Modifier.padding(10.dp),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Checklist Harian
                    item {
                        Text("📋 CHECKLIST STANDAR OPERASIONAL HARIAN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    items(guide.tasks) { task ->
                        val cycleId = cycle?.id ?: 0L
                        var checked by remember(cycleId, date, task) {
                            mutableStateOf(BroilerGuideStore.isChecked(context, cycleId, date, task))
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    checked = !checked
                                    BroilerGuideStore.setChecked(context, cycleId, date, task, checked)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        checked = it
                                        BroilerGuideStore.setChecked(context, cycleId, date, task, it)
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(task, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                savingPdf = true
                                scope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        generateGuidePdf(context, cycle?.cycleNumber ?: "Siklus", coop?.name ?: "-", date, age, guide, summary, todayLog)
                                    }
                                    savingPdf = false
                                    openGuidePdf(context, file)
                                }
                            },
                            enabled = !savingPdf,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (savingPdf) "Sedang Membuat Dokumen PDF..." else "CETAK / BAGIKAN PANDUAN HARIAN (PDF)")
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            } else {
                // TAB 1: 15 BAB BUKU PANDUAN LENGKAP
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_guide"),
                        placeholder = { Text("Cari materi (misal: pakan, FCR, vaksin, suhu)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true
                    )

                    Spacer(Modifier.height(10.dp))

                    // Horizontal Chapter Selector Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(BroilerGuideData.chapters) { ch ->
                            FilterChip(
                                selected = selectedChapterId == ch.id && searchQuery.isBlank(),
                                onClick = {
                                    selectedChapterId = ch.id
                                    searchQuery = ""
                                },
                                label = { Text("${ch.tag}: ${ch.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = if (selectedChapterId == ch.id && searchQuery.isBlank()) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Chapter Content
                    val displayedChapters = remember(searchQuery, selectedChapterId) {
                        if (searchQuery.isBlank()) {
                            BroilerGuideData.chapters.filter { it.id == selectedChapterId }
                        } else {
                            BroilerGuideData.chapters.filter { ch ->
                                ch.title.contains(searchQuery, ignoreCase = true) ||
                                        ch.subtitle.contains(searchQuery, ignoreCase = true) ||
                                        ch.sections.any { sec ->
                                            sec.heading.contains(searchQuery, ignoreCase = true) ||
                                                    sec.items.any { it.contains(searchQuery, ignoreCase = true) } ||
                                                    sec.tableRows.any { row -> row.any { it.contains(searchQuery, ignoreCase = true) } }
                                        }
                            }
                        }
                    }

                    if (displayedChapters.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("Tidak ditemukan materi dengan kata kunci '$searchQuery'")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedChapters) { chapter ->
                                ChapterFullCard(chapter)
                            }
                            item {
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterFullCard(chapter: GuideChapter) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_chapter_${chapter.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Chapter Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = FarmGreenPrimary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        chapter.tag.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    chapter.targetStandard,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                chapter.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                chapter.subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Divider(Modifier.padding(vertical = 12.dp))

            // Sections
            chapter.sections.forEachIndexed { index, section ->
                Text(
                    section.heading,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))

                // Items list
                if (section.items.isNotEmpty()) {
                    section.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(item, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }

                // Table if present
                if (section.tableHeaders.isNotEmpty() && section.tableRows.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    GuideTableView(section.tableHeaders, section.tableRows)
                }

                if (section.note != null) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            section.note,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (index < chapter.sections.size - 1) {
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun GuideTableView(headers: List<String>, rows: List<List<String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Table Rows
        rows.forEachIndexed { idx, row ->
            val bg = if (idx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (idx < rows.size - 1) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun TargetBadge(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(160.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun EvaluasiRow(label: String, actualText: String, actual: Double, target: Double, higherIsGood: Boolean) {
    val status = if (actual <= 0 || target <= 0) {
        "⚪ Belum Ada Data" to Color.Gray
    } else if (higherIsGood && actual >= target * 0.95) {
        "🟢 Normal" to Color(0xFF2E7D32)
    } else {
        "🟡 Perlu Evaluasi" to Color(0xFFE65100)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(actualText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(status.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = status.second)
    }
}

private fun generateGuidePdf(
    context: Context,
    cycle: String,
    coop: String,
    date: String,
    age: Int,
    guide: BroilerGuide,
    summary: com.example.ui.DashboardSummary,
    log: com.example.data.local.entity.DailyLogEntity?
): File {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val c = page.canvas
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.BLACK; textSize = 12f }
    var y = 48f

    fun line(t: String, bold: Boolean = false, size: Float = 12f, color: Int = AndroidColor.BLACK) {
        p.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        p.textSize = size
        p.color = color
        c.drawText(t, 40f, y, p)
        y += size + 8f
    }

    line("SEJAHTERA BERSAMA - PANDUAN RESMI BUDIDAYA BROILER", bold = true, size = 15f, color = AndroidColor.rgb(27, 94, 32))
    line("Standar Manajemen Kemitraan Menuju IP > 400 & FCR ≤ 1,40", size = 10f, color = AndroidColor.DKGRAY)
    y += 6f
    line("Siklus: $cycle  |  Kandang: $coop  |  Tanggal: $date  |  Umur: Hari Ke-$age", bold = true)
    y += 8f

    line("TARGET PRODUKSI HARIAN (UMUR HARI KE-$age)", bold = true, size = 12f)
    line("• Target Bobot Badan: ${guide.targetWeightG} gram")
    line("• Target Konsumsi Pakan: ${guide.feedG} gram / ekor / hari")
    line("• Target Konsumsi Air: ± ${guide.waterMl} ml / ekor / hari")
    line("• Suhu Brooding / Kandang: ${guide.temp}  |  Kelembapan: ${guide.humidity}")
    line("• Program Pencahayaan: ${guide.light}")
    line("• Kebutuhan Ventilasi: ${guide.ventilation}")
    y += 10f

    line("DATA AKTUAL OPERASIONAL", bold = true, size = 12f)
    line("• Populasi Aktif: ${summary.currentPop} ekor")
    line("• Mortalitas Kumulatif: ${String.format(Locale.US, "%.2f", summary.mortalityPercent)}% (Target: ≤ 2,50%)")
    line("• Livability: ${String.format(Locale.US, "%.2f", summary.survivalRatePercent)}% (Target: ≥ 97,50%)")
    line("• Pakan Terdistribusi Hari Ini: ${log?.feedGivenKg ?: 0.0} kg")
    line("• Air Minum Hari Ini: ${log?.waterIntakeLiters ?: 0.0} Liter")
    line("• Suhu Tercatat: ${log?.tempCelsius ?: 0.0} °C")
    y += 10f

    line("CHECKLIST STANDAR OPERASIONAL HARI INI", bold = true, size = 12f)
    guide.tasks.forEach {
        line("  [  ] $it", size = 10f)
    }
    y += 12f

    line("CATATAN MANAJEMEN PAKAN & AIR (BAB 4 & BAB 5 RESMI)", bold = true, size = 11f)
    line("• Rasio Air : Pakan wajib dijaga 1,8 - 2,0 : 1. Jangan biarkan tempat pakan penuh (isi 1/4 - 1/3).", size = 9f)
    line("• Jadwal feeding standar: Pagi (06.00), Siang (11.00), Sore (16.00), Malam (20.00).", size = 9f)
    line("• Dokumen ini dihasilkan resmi oleh Sistem SEJAHTERA BERSAMA.", size = 9f, color = AndroidColor.GRAY)

    doc.finishPage(page)
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, "Panduan_Budidaya_Hari_${age}_${date}.pdf")
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()
    return file
}

private fun openGuidePdf(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            android.content.Intent.createChooser(
                android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                "Buka Dokumen Panduan PDF"
            )
        )
    } catch (_: Exception) {
    }
}
