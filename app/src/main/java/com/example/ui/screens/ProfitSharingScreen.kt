package com.example.ui.screens

import android.widget.Toast
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
import com.example.data.local.entity.MemberEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitSharingScreen(
    viewModel: FarmViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var processState by rememberProcessState()

    val currentCycle by viewModel.currentCycle.collectAsState()
    val farmProfile by viewModel.farmProfile.collectAsState()
    val allMembers by viewModel.members.collectAsState()

    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var dateText by remember { mutableStateOf(todayDate) }
    var periodText by remember(currentCycle) {
        mutableStateOf(
            if (currentCycle != null) "Siklus ${currentCycle?.cycleNumber} (${currentCycle?.chickInDate})"
            else "Pembagian Usaha ${SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())}"
        )
    }
    var totalRevenueText by remember { mutableStateOf("") }
    var totalExpenseText by remember { mutableStateOf("") }
    var totalDeductionText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    // Selected member IDs
    var selectedMemberIds by remember { mutableStateOf(setOf<Long>()) }

    // Auto-select active members upon initial load
    LaunchedEffect(allMembers) {
        if (selectedMemberIds.isEmpty() && allMembers.isNotEmpty()) {
            selectedMemberIds = allMembers.filter { it.isActive }.map { it.id }.toSet()
        }
    }

    // Parse values safely
    val totalRevenue = totalRevenueText.replace(".", "").replace(",", "").toLongOrNull() ?: 0L
    val totalExpense = totalExpenseText.replace(".", "").replace(",", "").toLongOrNull() ?: 0L
    val totalDeduction = totalDeductionText.replace(".", "").replace(",", "").toLongOrNull() ?: 0L

    val netProfit = (totalRevenue - totalExpense - totalDeduction).coerceAtLeast(0L)
    val selectedMembersList = allMembers.filter { it.id in selectedMemberIds }
    val memberCount = selectedMembersList.size

    val amountPerMember = if (memberCount > 0) netProfit / memberCount else 0L
    val totalDistributed = amountPerMember * memberCount
    val roundingRemainder = netProfit - totalDistributed

    val distributionStatus = if (netProfit > 0 && memberCount > 0) {
        if (roundingRemainder == 0L) "PEMBAGIAN SESUAI" else "PEMBAGIAN MEMILIKI SISA PEMBULATAN"
    } else {
        "BELUM LENGKAP"
    }

    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag("profit_sharing_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hasil Pembagian", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Kalkulasi Sama Rata & Cetak Laporan PDF",
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
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("history_action_btn")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Riwayat Pembagian", tint = FarmGreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // HEADER BANNER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenPrimary.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = FarmGreenPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Perhitungan Hasil Usaha Bersama",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = FarmGreenPrimary
                            )
                            Text(
                                "Hasil bersih dibagikan sama rata ke seluruh anggota terdaftar secara otomatis dan akurat.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // INPUT SECTION CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Input Keuangan Hasil & Periode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = dateText,
                                onValueChange = { dateText = it },
                                label = { Text("Tanggal Pembagian") },
                                placeholder = { Text("YYYY-MM-DD") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("date_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = periodText,
                                onValueChange = { periodText = it },
                                label = { Text("Periode") },
                                placeholder = { Text("Contoh: Siklus 1") },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("period_input"),
                                singleLine = true
                            )
                        }

                        // Total Hasil
                        OutlinedTextField(
                            value = totalRevenueText,
                            onValueChange = { totalRevenueText = it.filter { c -> c.isDigit() } },
                            label = { Text("Total Hasil (Rp) *") },
                            placeholder = { Text("0") },
                            prefix = { Text("Rp ", fontWeight = FontWeight.SemiBold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("total_revenue_input"),
                            singleLine = true,
                            supportingText = {
                                if (totalRevenue > 0) Text("Format: ${FormatHelper.formatRupiah(totalRevenue)}")
                            }
                        )

                        // Biaya
                        OutlinedTextField(
                            value = totalExpenseText,
                            onValueChange = { totalExpenseText = it.filter { c -> c.isDigit() } },
                            label = { Text("Biaya Operasional (Rp)") },
                            placeholder = { Text("0") },
                            prefix = { Text("Rp ", fontWeight = FontWeight.SemiBold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("total_expense_input"),
                            singleLine = true,
                            supportingText = {
                                if (totalExpense > 0) Text("Format: ${FormatHelper.formatRupiah(totalExpense)}")
                            }
                        )

                        // Potongan
                        OutlinedTextField(
                            value = totalDeductionText,
                            onValueChange = { totalDeductionText = it.filter { c -> c.isDigit() } },
                            label = { Text("Potongan Lain-lain (Rp)") },
                            placeholder = { Text("0") },
                            prefix = { Text("Rp ", fontWeight = FontWeight.SemiBold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("total_deduction_input"),
                            singleLine = true,
                            supportingText = {
                                if (totalDeduction > 0) Text("Format: ${FormatHelper.formatRupiah(totalDeduction)}")
                            }
                        )

                        // Keterangan
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Keterangan Tambahan / Alasan") },
                            placeholder = { Text("Contoh: Pembagian hasil panen broiler kemitraan") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }
                }
            }

            // HASIL KALKULASI & STATUS EVALUASI
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (distributionStatus == "PEMBAGIAN SESUAI") Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Rincian Ringkasan Hasil",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (distributionStatus == "PEMBAGIAN SESUAI") Color(0xFF1B5E20) else Color(0xFF795548)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (distributionStatus == "PEMBAGIAN SESUAI") Color(0xFF2E7D32) else Color(0xFFE65100)
                            ) {
                                Text(
                                    text = distributionStatus,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Divider(color = Color.Black.copy(alpha = 0.08f))

                        CalcRow(label = "Total Hasil", value = FormatHelper.formatRupiah(totalRevenue))
                        CalcRow(label = "Biaya Operasional", value = "- ${FormatHelper.formatRupiah(totalExpense)}")
                        CalcRow(label = "Potongan", value = "- ${FormatHelper.formatRupiah(totalDeduction)}")
                        Divider(color = Color.Black.copy(alpha = 0.08f))
                        CalcRow(
                            label = "Hasil Bersih",
                            value = FormatHelper.formatRupiah(netProfit),
                            isHighlight = true,
                            highlightColor = Color(0xFF1B5E20)
                        )
                        CalcRow(label = "Jumlah Anggota", value = "$memberCount Orang")
                        CalcRow(
                            label = "Hasil Per Anggota",
                            value = FormatHelper.formatRupiah(amountPerMember),
                            isHighlight = true,
                            highlightColor = FarmGreenPrimary
                        )
                        Divider(color = Color.Black.copy(alpha = 0.08f))
                        CalcRow(label = "Total Pembagian", value = FormatHelper.formatRupiah(totalDistributed))
                        CalcRow(
                            label = "Sisa Pembulatan / Selisih",
                            value = FormatHelper.formatRupiah(roundingRemainder),
                            isHighlight = roundingRemainder > 0L,
                            highlightColor = Color(0xFFE65100)
                        )
                    }
                }
            }

            // MEMBER SELECTION HEADER & CONTROLS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Penerima Hasil Pembagian",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "$memberCount dari ${allMembers.size} anggota terpilih",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(
                            onClick = onNavigateToMembers,
                            modifier = Modifier.testTag("manage_members_btn")
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kelola Anggota", fontSize = 12.sp)
                        }
                    }

                    if (allMembers.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedMemberIds = allMembers.map { it.id }.toSet()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text("Pilih Semua", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedMemberIds = allMembers.filter { it.isActive }.map { it.id }.toSet()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text("Aktif Saja", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedMemberIds = emptySet()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text("Batal Semua", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // MEMBERS LIST OR EMPTY STATE
            if (allMembers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = FarmGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Belum ada data anggota terdaftar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Silakan tambahkan data anggota terlebih dahulu agar dapat dibagi sama rata.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToMembers,
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                            ) {
                                Text("Buka Data Anggota")
                            }
                        }
                    }
                }
            } else {
                items(allMembers, key = { it.id }) { member ->
                    val isChecked = member.id in selectedMemberIds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMemberIds = if (isChecked) {
                                    selectedMemberIds - member.id
                                } else {
                                    selectedMemberIds + member.id
                                }
                            }
                            .testTag("member_select_${member.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) FarmGreenPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isChecked) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FarmGreenPrimary)) else CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedMemberIds = if (checked) {
                                        selectedMemberIds + member.id
                                    } else {
                                        selectedMemberIds - member.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "ID: ${member.memberNumber.ifBlank { "SB-${member.id}" }}${if (!member.isActive) " (Non-Aktif)" else ""}",
                                    fontSize = 11.5.sp,
                                    color = if (member.isActive) MaterialTheme.colorScheme.onSurfaceVariant else Color.Red
                                )
                            }
                            if (isChecked && amountPerMember > 0) {
                                Text(
                                    text = FormatHelper.formatRupiah(amountPerMember),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = FarmGreenPrimary
                                )
                            }
                        }
                    }
                }
            }

            // ACTION BUTTONS
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Simpan Hasil Pembagian
                    Button(
                        onClick = {
                            if (totalRevenue <= 0L) {
                                Toast.makeText(context, "Silakan isi Total Hasil (Rp)", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedMembersList.isEmpty()) {
                                Toast.makeText(context, "Pilih minimal 1 anggota penerima hasil", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            processState = ProcessState.Processing(
                                title = "MENYIMPAN PEMBAGIAN HASIL",
                                message = "Menyimpan data hasil pembagian usaha..."
                            )

                            val memberItems = selectedMembersList.map { m ->
                                DistributionMemberItem(
                                    memberId = m.id,
                                    memberName = m.name,
                                    memberNumber = m.memberNumber,
                                    amount = amountPerMember
                                )
                            }
                            val memberDetailsJson = PdfReportGenerator.serializeDistributionMembers(memberItems)

                            val record = ProfitDistributionEntity(
                                cycleId = currentCycle?.id ?: 0L,
                                coopId = currentCycle?.coopId ?: 0L,
                                date = dateText.trim(),
                                period = periodText.trim(),
                                totalRevenue = totalRevenue,
                                totalExpense = totalExpense,
                                totalDeduction = totalDeduction,
                                netProfit = netProfit,
                                memberCount = memberCount,
                                amountPerMember = amountPerMember,
                                totalDistributed = totalDistributed,
                                roundingRemainder = roundingRemainder,
                                status = distributionStatus,
                                notes = notesText.trim(),
                                memberDetailsJson = memberDetailsJson
                            )

                            viewModel.saveProfitDistribution(
                                distribution = record,
                                onSuccess = { id ->
                                    scope.launch {
                                        delay(300)
                                        processState = ProcessState.Success(
                                            title = "Tersimpan",
                                            message = "Hasil pembagian usaha berhasil dicatat dalam riwayat.",
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_distribution_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Hasil Pembagian", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    // Cetak Laporan PDF
                    OutlinedButton(
                        onClick = {
                            if (totalRevenue <= 0L) {
                                Toast.makeText(context, "Silakan isi Total Hasil terlebih dahulu", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            if (selectedMembersList.isEmpty()) {
                                Toast.makeText(context, "Pilih minimal 1 anggota untuk dicetak ke PDF", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }

                            processState = ProcessState.Processing(
                                title = "MEMBUAT LAPORAN PDF",
                                message = "Menyusun dokumen PDF laporan hasil pembagian..."
                            )

                            scope.launch(Dispatchers.IO) {
                                val memberItems = selectedMembersList.map { m ->
                                    DistributionMemberItem(
                                        memberId = m.id,
                                        memberName = m.name,
                                        memberNumber = m.memberNumber,
                                        amount = amountPerMember
                                    )
                                }
                                val memberDetailsJson = PdfReportGenerator.serializeDistributionMembers(memberItems)

                                val record = ProfitDistributionEntity(
                                    cycleId = currentCycle?.id ?: 0L,
                                    coopId = currentCycle?.coopId ?: 0L,
                                    date = dateText.trim(),
                                    period = periodText.trim(),
                                    totalRevenue = totalRevenue,
                                    totalExpense = totalExpense,
                                    totalDeduction = totalDeduction,
                                    netProfit = netProfit,
                                    memberCount = memberCount,
                                    amountPerMember = amountPerMember,
                                    totalDistributed = totalDistributed,
                                    roundingRemainder = roundingRemainder,
                                    status = distributionStatus,
                                    notes = notesText.trim(),
                                    memberDetailsJson = memberDetailsJson
                                )

                                val defaultProfile = farmProfile ?: com.example.data.local.entity.FarmProfileEntity()

                                try {
                                    val pdfFile = PdfReportGenerator.generateMemberProfitReportPdf(
                                        context = context,
                                        profile = defaultProfile,
                                        distribution = record
                                    )

                                    withContext(Dispatchers.Main) {
                                        processState = ProcessState.Idle
                                        generatedPdfFile = pdfFile
                                        showPdfSuccessDialog = true
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        processState = ProcessState.Error(
                                            title = "Gagal Membuat PDF",
                                            message = e.localizedMessage ?: "Terjadi kesalahan saat rendering PDF.",
                                            onDismiss = { processState = ProcessState.Idle }
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("print_pdf_distribution_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = FarmGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cetak & Preview Laporan PDF Anggota",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FarmGreenPrimary
                        )
                    }
                }
            }
        }
    }

    // Dialog Sukses Cetak PDF
    if (showPdfSuccessDialog && generatedPdfFile != null) {
        val file = generatedPdfFile!!
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(36.dp)) },
            title = { Text("PDF Anggota Berhasil Dibuat", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Laporan Hasil Pembagian Anggota berstandar resmi A4 telah siap dengan ruang tanda tangan lapang.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "File: ${file.name}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmGreenPrimary
                    )
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
                    Text("Buka PDF")
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
private fun CalcRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    highlightColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isHighlight) 13.5.sp else 12.5.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 14.5.sp else 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) highlightColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
