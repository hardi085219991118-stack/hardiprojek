package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.paint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CycleEntity
import com.example.ui.DashboardSummary
import com.example.ui.FarmViewModel
import com.example.ui.components.AppHeader
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.SimpleLineChart
import com.example.ui.components.StatCard
import com.example.ui.theme.FarmGold
import com.example.ui.theme.FarmGreenPrimary
import com.example.ui.theme.FarmGreenSecondary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FarmViewModel,
    summary: DashboardSummary,
    cycles: List<CycleEntity>,
    onNavigate: (String) -> Unit
) {
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    val mortalityLogs by viewModel.mortalityLogs.collectAsState()
    val weightSamples by viewModel.weightSamples.collectAsState()
    val profile by viewModel.farmProfile.collectAsState()

    var showCycleDropdown by remember { mutableStateOf(false) }

    val idRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    val numFmt = NumberFormat.getNumberInstance(Locale("id", "ID")).apply { maximumFractionDigits = 2 }

    Scaffold(
        topBar = {
            AppHeader(
                title = profile?.farmName ?: "SEJAHTERA BERSAMA",
                subtitle = profile?.slogan ?: "REZEKI LANCAR, USAHA MAKMUR",
                onAboutClick = { onNavigate("about") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_dashboard), contentScale = ContentScale.Crop)
                .testTag("screen_dashboard"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- CYCLE & COOP SELECTOR BAR ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SIKLUS AKTIF",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.DarkGray,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = summary.activeCycle?.cycleNumber ?: "Belum ada siklus aktif",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FarmGreenPrimary
                                    )
                                )
                                Text(
                                    text = "Kandang: ${summary.coop?.name ?: "-"} | Mitra: ${summary.partner?.companyName ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                )
                            }

                            Box {
                                OutlinedButton(
                                    onClick = { showCycleDropdown = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_switch_cycle")
                                ) {
                                    Text("Ganti", fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = showCycleDropdown,
                                    onDismissRequest = { showCycleDropdown = false }
                                ) {
                                    cycles.forEach { cyc ->
                                        DropdownMenuItem(
                                            text = { Text("${cyc.cycleNumber} (${cyc.status})") },
                                            onClick = {
                                                viewModel.selectCycle(cyc.id)
                                                showCycleDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- WARNINGS & ALERTS ---
            if (summary.warnings.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        summary.warnings.forEach { warning ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (warning.contains("Peringatan")) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (warning.contains("Peringatan")) Color(0xFFC62828) else Color(0xFFF57F17),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = warning,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- KPI CARDS GRID ---
            item {
                Text(
                    text = "RINGKASAN PERFORMA SIKLUS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Populasi",
                        value = "${summary.currentPop} Ekor",
                        subtitle = "Awal: ${summary.initialPop} Ekor (Hari ke-${summary.ageDays})",
                        icon = Icons.Default.Pets,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Mortalitas",
                        value = "${numFmt.format(summary.mortalityPercent)} %",
                        subtitle = "Mati: ${summary.totalDead} | Afkir: ${summary.totalCulls}",
                        icon = Icons.Default.HeartBroken,
                        contentColor = if (summary.mortalityPercent > 4.0) Color(0xFFC62828) else FarmGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Sisa Pakan",
                        value = "${numFmt.format(summary.remainingFeedKg)} Kg",
                        subtitle = "Terpakai: ${numFmt.format(summary.totalFeedUsedKg)} Kg",
                        icon = Icons.Default.Inventory2,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Rata-rata Bobot",
                        value = "${summary.latestAvgWeightGram.toInt()} Gram",
                        subtitle = "Target: ${summary.activeCycle?.targetWeightKg?.takeIf { it > 0 } ?: 0.0} Kg",
                        icon = Icons.Default.Scale,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "FCR Realisasi",
                        value = numFmt.format(summary.fcr),
                        subtitle = "Target: ${summary.activeCycle?.targetFcr?.takeIf { it > 0 } ?: 0.0}",
                        icon = Icons.Default.TrendingDown,
                        contentColor = if (summary.fcr > 1.60) Color(0xFFD84315) else FarmGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "ADG (gr/hari)",
                        value = "${numFmt.format(summary.adgGram)} gr",
                        subtitle = "Perkembangan bobot harian",
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Biaya",
                        value = idRupiah.format(summary.totalExpenses),
                        subtitle = "DOC + Pakan + Operasional",
                        icon = Icons.Default.Payments,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Estimasi Laba",
                        value = idRupiah.format(summary.estimatedProfit),
                        subtitle = if (summary.estimatedProfit >= 0) "Surplus" else "Defisit",
                        contentColor = if (summary.estimatedProfit >= 0) FarmGreenPrimary else Color.Red,
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- QUICK ACTION MENU GRID ---
            item {
                Text(
                    text = "MENU OPERASIONAL & KEMITRAAN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Urutan dibuat mengikuti alur pengisian wajib: Profil -> Kandang -> Mitra -> Siklus -> Operasional.
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionMenuItem(Icons.Default.AccountCircle, "Profil & Akun", "menu_farm_profile", modifier = Modifier.weight(1f)) { onNavigate("farm_profile") }
                        ActionMenuItem(Icons.Default.LocationOn, "Kandang & GPS", "menu_coops", modifier = Modifier.weight(1f)) { onNavigate("coops") }
                        ActionMenuItem(Icons.Default.Business, "Mitra Kontrak", "menu_partners", modifier = Modifier.weight(1f)) { onNavigate("partners") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionMenuItem(Icons.Default.Sync, "Siklus Pemeliharaan", "menu_cycles", modifier = Modifier.weight(1f)) { onNavigate("cycles") }
                        ActionMenuItem(Icons.Default.EditNote, "Laporan Harian", "menu_daily_log", modifier = Modifier.weight(1f)) { onNavigate("daily_log") }
                        ActionMenuItem(Icons.Default.Scale, "Bobot & ADG", "menu_weight", modifier = Modifier.weight(1f)) { onNavigate("weight") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionMenuItem(Icons.Default.Grass, "Stok Pakan", "menu_feed", modifier = Modifier.weight(1f)) { onNavigate("feed") }
                        ActionMenuItem(Icons.Default.HeartBroken, "Kematian & Afkir", "menu_mortality", modifier = Modifier.weight(1f)) { onNavigate("mortality") }
                        ActionMenuItem(Icons.Default.Medication, "Obat / Vaksin", "menu_medicine", modifier = Modifier.weight(1f)) { onNavigate("medicine") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionMenuItem(Icons.Default.Paid, "Keuangan Kandang", "menu_expenses", modifier = Modifier.weight(1f)) { onNavigate("expenses") }
                        ActionMenuItem(Icons.Default.LocalShipping, "Panen & Jual", "menu_harvest", modifier = Modifier.weight(1f)) { onNavigate("harvest") }
                        ActionMenuItem(Icons.Default.CameraAlt, "Foto Bukti Watermark", "menu_photo_evidence", containerColor = Color(0xFF1B5E20), contentColor = Color.White, modifier = Modifier.weight(1f)) { onNavigate("photo_evidence") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionMenuItem(Icons.Default.PictureAsPdf, "Laporan PDF Kemitraan", "menu_reports_pdf", containerColor = FarmGreenPrimary, contentColor = Color.White, modifier = Modifier.weight(1f)) { onNavigate("reports_pdf") }
                        ActionMenuItem(Icons.Default.Send, "Kirim Laporan WA/Email", "menu_report_dispatch", containerColor = Color(0xFF00796B), contentColor = Color.White, modifier = Modifier.weight(1f)) { onNavigate("report_dispatch") }
                        ActionMenuItem(Icons.Default.Backup, "Backup & Ekspor", "menu_backup", modifier = Modifier.weight(1f)) { onNavigate("backup") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionMenuItem(Icons.Default.MenuBook, "Panduan Budidaya", "menu_panduan", containerColor = Color(0xFF2E7D32), contentColor = Color.White, modifier = Modifier.weight(1f)) { onNavigate("broiler_guide") }
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // --- CHARTS SECTION ---
            item {
                Text(
                    text = "GRAFIK PERKEMBANGAN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            // Population Line Chart
            item {
                val popPoints = dailyLogs.map { it.ageDays to it.afternoonPopulation.toDouble() }
                SimpleLineChart(
                    title = "Perkembangan Populasi Ayam (Ekor)",
                    dataPoints = popPoints,
                    unit = "Ekor",
                    lineColor = FarmGreenPrimary
                )
            }

            // Mortality Bar Chart
            item {
                val mortPoints = dailyLogs.takeLast(10).map { "H-${it.ageDays}" to it.deadCount.toDouble() }
                SimpleBarChart(
                    title = "Kematian Harian 10 Hari Terakhir (Ekor)",
                    dataPoints = mortPoints,
                    unit = "Ekor",
                    barColor = Color(0xFFD32F2F)
                )
            }

            // Feed Consumption Line Chart
            item {
                val feedPoints = dailyLogs.map { it.ageDays to it.feedGivenKg }
                SimpleLineChart(
                    title = "Konsumsi Pakan Harian (Kg)",
                    dataPoints = feedPoints,
                    unit = "Kg",
                    lineColor = FarmGold
                )
            }

            // Weight Growth Line Chart
            item {
                val weightPoints = weightSamples.map { it.ageDays to it.averageWeightGram }
                SimpleLineChart(
                    title = "Perkembangan Bobot Rata-rata (Gram)",
                    dataPoints = weightPoints,
                    unit = "Gram",
                    lineColor = Color(0xFF00796B)
                )
            }
        }
    }
}

@Composable
fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    tag: String,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = FarmGreenPrimary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (containerColor == FarmGreenPrimary) Color.White else Color.Black,
                    fontSize = 10.5.sp
                ),
                maxLines = 1
            )
        }
    }
}
