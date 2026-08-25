package com.example.ui.screens

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
fun BroilerGuideScreen(viewModel: FarmViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val cycle by viewModel.currentCycle.collectAsState()
    val coop by viewModel.currentCoop.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()
    val logs by viewModel.dailyLogs.collectAsState()
    val meds by viewModel.medicines.collectAsState()
    val weights by viewModel.weightSamples.collectAsState()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<String?>(null) }
    val date = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    Scaffold(topBar = { TopAppBar(title = { Text("Panduan Budidaya Ayam Broiler", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        if (cycle == null) {
            Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) { Text("Silakan buat atau pilih Siklus Aktif terlebih dahulu.") }
            return@Scaffold
        }
        val age = BroilerGuideData.ageDays(cycle!!.chickInDate)
        val guide = BroilerGuideData.forDay(age)
        val todayLog = logs.firstOrNull { it.date == date } ?: logs.maxByOrNull { it.ageDays }
        val rupiah = NumberFormat.getNumberInstance(Locale("id","ID"))

        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = FarmGreenPrimary)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("SEJAHTERA BERSAMA", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        Text("PANDUAN BUDIDAYA HARI INI", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("$date • Hari Ke-$age • ${coop?.name ?: "Kandang belum dipilih"}", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            item { Text("🎯 TARGET HARI INI", fontWeight = FontWeight.Bold) }
            item {
                Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Bobot target: ${guide.targetWeightG} gram")
                    Text("Pakan target: ${guide.feedG} gram/ekor/hari")
                    Text("Air target: ± ${guide.waterMl} ml/ekor/hari")
                    Text("Suhu: ${guide.temp} • Kelembaban: ${guide.humidity}")
                    Text("Pencahayaan: ${guide.light}")
                    Text("Ventilasi: ${guide.ventilation}")
                } }
            }
            item { Text("📊 KONDISI AKTUAL & ANALISIS", fontWeight = FontWeight.Bold) }
            item {
                Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val actualWeight = weights.maxByOrNull { it.ageDays }?.averageWeightGram ?: 0.0
                    val actualFeed = todayLog?.let { if (summary.currentPop > 0) it.feedGivenKg * 1000.0 / summary.currentPop else 0.0 } ?: 0.0
                    val actualWater = todayLog?.let { if (summary.currentPop > 0) it.waterIntakeLiters * 1000.0 / summary.currentPop else 0.0 } ?: 0.0
                    AnalysisLine("Bobot", if (actualWeight > 0) "${rupiah.format(actualWeight)} g" else "Belum ada data", actualWeight, guide.targetWeightG.toDouble(), higherIsGood = true)
                    AnalysisLine("Pakan", if (actualFeed > 0) "${rupiah.format(actualFeed)} g/ekor" else "Belum ada data", actualFeed, guide.feedG.toDouble(), higherIsGood = true)
                    AnalysisLine("Air", if (actualWater > 0) "${rupiah.format(actualWater)} ml/ekor" else "Belum ada data", actualWater, guide.waterMl.toDouble(), higherIsGood = true)
                    AnalysisLine("Suhu", todayLog?.let { "${it.tempCelsius} °C" } ?: "Belum ada data", todayLog?.tempCelsius ?: 0.0, 0.0, true, guide.temp)
                    Text("Mortalitas kumulatif: ${String.format(Locale.US,"%.2f", summary.mortalityPercent)}% • Livability: ${String.format(Locale.US,"%.2f", summary.survivalRatePercent)}%")
                    if (summary.warnings.isNotEmpty()) summary.warnings.forEach { Text(it) }
                    if (summary.mortalityPercent >= 4.0) Text("🔴 PERINGATAN: Mortalitas tinggi. Periksa kondisi ayam, suhu, air, pakan, ventilasi dan catat gejala. Pertimbangkan pemeriksaan oleh tenaga profesional yang kompeten.")
                } }
            }
            item { Text("📋 CHECKLIST HARIAN", fontWeight = FontWeight.Bold) }
            items(guide.tasks.size) { index ->
                val task = guide.tasks[index]
                var checked by remember(cycle!!.id, date, task) { mutableStateOf(BroilerGuideStore.isChecked(context, cycle!!.id, date, task)) }
                Card(modifier = Modifier.fillMaxWidth().clickable { checked = !checked; BroilerGuideStore.setChecked(context, cycle!!.id, date, task, checked) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = { checked = it; BroilerGuideStore.setChecked(context, cycle!!.id, date, task, it) })
                        Text(task)
                    }
                }
            }
            item { Text("📚 MENU PANDUAN PROFESIONAL", fontWeight = FontWeight.Bold) }
            val sections = listOf(
                "🏠 Persiapan Kandang" to "Sanitasi, desinfeksi, litter, brooder dan pemanasan sebelum DOC masuk.",
                "🐥 Manajemen DOC" to "Penerimaan DOC, seleksi, suhu brooder, air minum pertama dan pemerataan DOC.",
                "🌾 Manajemen Pakan" to "Target FCR, feeder, program pemberian, pencegahan pakan terbuang dan kualitas pakan.",
                "💧 Manajemen Air Minum" to "Air bersih, rasio konsumsi, nipple, tekanan air dan sanitasi jalur minum.",
                "🌬️ Manajemen Ventilasi" to "Ventilasi minimum, pertukaran udara, amonia, suhu dan kelembaban.",
                "💡 Manajemen Pencahayaan" to "Durasi lampu, intensitas, waktu hidup/mati dan pemerataan pertumbuhan.",
                "🛡️ Biosekuriti" to "Disinfeksi, footbath, kontrol orang/kendaraan, bangkai dan sanitasi kandang.",
                "💉 Program Vaksin" to vaccineText(age, meds),
                "💊 Program Vitamin" to vitaminText(age, meds),
                "📊 Target Mingguan" to "Bandingkan bobot, pakan, mortalitas, livability, FCR dan IP menggunakan data tersimpan.",
                "🚨 Penanganan Masalah" to "Heat stress, gangguan pernapasan, pencernaan, koksidiosis, amonia dan litter: gunakan sebagai panduan awal, bukan diagnosis pasti.",
                "🏆 Cara Mencapai IP >400" to "Kontrol FCR, bobot panen, livability, feed waste, suhu dan biosekuriti secara konsisten.",
                "📈 Analisis Produksi" to "Populasi awal/akhir, mortalitas, livability, pakan, bobot, FCR dan IP dihitung dari data aplikasi.",
                "🎯 Target Produksi Profesional" to "DOC awal, deplesi, livability ≥97,5%, bobot panen, FCR dan IP dibandingkan dengan target."
            )
            items(sections.size) { i ->
                val s = sections[i]
                Card(Modifier.fillMaxWidth().clickable { expanded = if (expanded == s.first) null else s.first }) {
                    Column(Modifier.padding(14.dp)) { Text(s.first, fontWeight = FontWeight.Bold); if (expanded == s.first) { Spacer(Modifier.height(6.dp)); Text(s.second) } }
                }
            }
            item {
                Button(enabled = !saving, onClick = {
                    saving = true
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { generateGuidePdf(context, cycle!!.cycleNumber, coop?.name ?: "-", date, age, guide, summary, todayLog) }
                        saving = false
                        openGuidePdf(context, file)
                    }
                }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text(if (saving) "Sedang Menyimpan.....\nTunggu Sampai Selesai...." else "CETAK / SIMPAN PDF PANDUAN")
                }
            }
        }
    }
}

@Composable private fun AnalysisLine(label: String, actualText: String, actual: Double, target: Double, higherIsGood: Boolean, suffixTarget: String = ""): Unit {
    val status = if (actual <= 0 || target <= 0) "⚪ BELUM ADA DATA" else if (higherIsGood && actual >= target * 0.95) "🟢 NORMAL" else "🟡 PERLU PERHATIAN"
    Text("$label: $actualText • $status${if (suffixTarget.isNotBlank()) " (target $suffixTarget)" else ""}")
}
private fun vaccineText(age: Int, meds: List<com.example.data.local.entity.MedicineEntity>) = "Umur hari ke-$age. Catatan vaksin tersimpan: ${meds.count { it.category.contains("Vaksin", true) }}. Jadwal harus disesuaikan program dokter hewan/mitra dan kondisi lokal."
private fun vitaminText(age: Int, meds: List<com.example.data.local.entity.MedicineEntity>) = "Umur hari ke-$age. Catatan vitamin tersimpan: ${meds.count { it.category.contains("Vitamin", true) }}. Pantau konsumsi air, kondisi ayam dan catatan pemberian."

private fun generateGuidePdf(context: Context, cycle: String, coop: String, date: String, age: Int, guide: BroilerGuide, summary: com.example.ui.DashboardSummary, log: com.example.data.local.entity.DailyLogEntity?): File {
    val doc = PdfDocument(); val page = doc.startPage(PdfDocument.PageInfo.Builder(595,842,1).create()); val c = page.canvas
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.BLACK; textSize = 13f }
    var y = 48f
    fun line(t:String, bold:Boolean=false) { p.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT; c.drawText(t,40f,y,p); y += 22f }
    line("SEJAHTERA BERSAMA", true); line("PANDUAN BUDIDAYA HARI INI", true); y += 8f
    line("Siklus: $cycle"); line("Kandang: $coop"); line("Tanggal: $date • Umur: Hari Ke-$age")
    y += 8f; line("TARGET HARI INI", true); line("Bobot ${guide.targetWeightG} g | Pakan ${guide.feedG} g/ekor | Air ±${guide.waterMl} ml/ekor")
    line("Suhu ${guide.temp} | Kelembaban ${guide.humidity} | Lampu ${guide.light}"); line("Ventilasi: ${guide.ventilation}")
    y += 8f; line("DATA AKTUAL", true); line("Populasi ${summary.currentPop} ekor | Mortalitas ${String.format(Locale.US,"%.2f",summary.mortalityPercent)}% | Livability ${String.format(Locale.US,"%.2f",summary.survivalRatePercent)}%")
    line("Pakan hari ini ${log?.feedGivenKg ?: 0.0} kg | Air ${log?.waterIntakeLiters ?: 0.0} L | Suhu ${log?.tempCelsius ?: 0.0} °C")
    y += 8f; line("CHECKLIST & REKOMENDASI", true); guide.tasks.forEach { line("☐ $it") }
    line("PDF dibuat otomatis oleh Aplikasi SEJAHTERA BERSAMA")
    doc.finishPage(page); val dir = File(context.cacheDir,"reports").apply{mkdirs()}; val file = File(dir,"Panduan_Budidaya_Hari_${age}_${date}.pdf"); FileOutputStream(file).use{doc.writeTo(it)}; doc.close()
    return file
}

private fun openGuidePdf(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
        context.startActivity(android.content.Intent.createChooser(
            android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri,"application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }, "Buka PDF Panduan"))
    } catch (_: Exception) { }
}
