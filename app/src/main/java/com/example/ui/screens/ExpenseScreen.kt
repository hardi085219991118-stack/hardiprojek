package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.components.DeletePinProtectedButton
import com.example.data.local.entity.ExpenseEntity
import com.example.ui.FarmViewModel
import com.example.ui.components.StatCard
import com.example.ui.theme.FarmGreenPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TYPE_OUT = "OUT"
private const val TYPE_IN = "IN"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val currentCycle by viewModel.currentCycle.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val transactions by viewModel.expenses.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var dialogType by remember { mutableStateOf(TYPE_OUT) }
    var deleteCandidate by remember { mutableStateOf<ExpenseEntity?>(null) }

    val idRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    val expenses = transactions.filter { it.transactionType != TYPE_IN }
    val incomes = transactions.filter { it.transactionType == TYPE_IN }
    val totalOut = expenses.sumOf { it.totalAmount }
    val totalIn = incomes.sumOf { it.totalAmount }
    val currentBalance = totalIn - totalOut

    LaunchedEffect(currentCycle, cycles) {
        if (currentCycle == null && cycles.isNotEmpty()) {
            val active = cycles.find { it.status == "ACTIVE" } ?: cycles.firstOrNull()
            active?.let { viewModel.selectCycle(it.id) }
        }
    }

    fun openAdd(type: String) {
        dialogType = type
        showAddDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keuangan Kandang", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_expenses")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { openAdd(if (selectedTab == 0) TYPE_OUT else TYPE_IN) },
                        modifier = Modifier.testTag("btn_top_add_expense")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Tambah", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmGreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (currentCycle != null) {
                FloatingActionButton(
                    onClick = { openAdd(if (selectedTab == 0) TYPE_OUT else TYPE_IN) },
                    containerColor = FarmGreenPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_expense")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_operasional), contentScale = ContentScale.Crop)
                .testTag("list_expenses"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BalanceCard(
                    balance = currentBalance,
                    income = totalIn,
                    expense = totalOut,
                    formatter = idRupiah
                )
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pengeluaran") },
                        icon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Uang Masuk") },
                        icon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
                    )
                }
            }

            item {
                val isIncome = selectedTab == 1
                StatCard(
                    title = if (isIncome) "Total Uang Masuk" else "Total Pengeluaran Kandang",
                    value = idRupiah.format(if (isIncome) totalIn else totalOut),
                    subtitle = if (isIncome) "Semua pemasukan yang dicatat pada siklus ini" else "Semua biaya/pembelian yang dicatat pada siklus ini",
                    icon = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            item {
                Text(
                    text = if (selectedTab == 0) "RINCIAN BIAYA / PEMBELIAN KANDANG" else "RINCIAN UANG MASUK",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                )
            }

            val visible = if (selectedTab == 0) expenses else incomes
            if (visible.isEmpty()) {
                item {
                    EmptyFinanceCard(isIncome = selectedTab == 1, onAdd = {
                        openAdd(if (selectedTab == 0) TYPE_OUT else TYPE_IN)
                    })
                }
            }

            items(visible, key = { it.id }) { item ->
                FinanceTransactionCard(item, idRupiah) { deleteCandidate = item }
            }
        }
    }

    if (showAddDialog && currentCycle != null) {
        FinanceEntryDialog(
            type = dialogType,
            cycleId = currentCycle!!.id,
            onDismiss = { showAddDialog = false },
            onSave = { entity ->
                viewModel.saveExpense(entity) { showAddDialog = false }
            }
        )
    }

    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Hapus Catatan?") },
            text = { Text("Hapus catatan '${candidate.expenseName}' sebesar ${idRupiah.format(candidate.totalAmount)}?") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    viewModel.deleteExpense(candidate)
                    deleteCandidate = null
                })
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    formatter: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Savings, contentDescription = null, tint = FarmGreenPrimary)
                Spacer(Modifier.width(8.dp))
                Text("JUMLAH UANG / SALDO SAAT INI", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            Text(
                formatter.format(balance),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMoneyCard("Uang Masuk", income, Color(0xFF2E7D32), Icons.Default.ArrowDownward, formatter, Modifier.weight(1f))
                MiniMoneyCard("Pengeluaran", expense, Color(0xFFC62828), Icons.Default.ArrowUpward, formatter, Modifier.weight(1f))
            }
            Text("Saldo = seluruh uang masuk − seluruh pengeluaran", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun MiniMoneyCard(
    title: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    formatter: NumberFormat,
    modifier: Modifier
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(3.dp))
            Text(formatter.format(amount), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
private fun EmptyFinanceCard(isIncome: Boolean, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (isIncome) Icons.Default.AttachMoney else Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = FarmGreenPrimary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                if (isIncome) "Belum Ada Catatan Uang Masuk" else "Belum Ada Catatan Pengeluaran",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                if (isIncome) "Tambahkan pemasukan, modal, pembayaran, atau penerimaan lainnya." else "Tambahkan biaya atau pembelian kandang beserta bukti foto.",
                fontSize = 12.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (isIncome) "+ TAMBAH UANG MASUK" else "+ TAMBAH PENGELUARAN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FinanceTransactionCard(item: ExpenseEntity, formatter: NumberFormat, onDelete: () -> Unit) {
    val isIncome = item.transactionType == TYPE_IN
    val accent = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.expenseName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (item.category.isNotBlank()) Text(item.category, fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold)
                Text("Tanggal: ${item.date}", fontSize = 12.sp, color = Color.DarkGray)
                if (item.notes.isNotBlank()) Text("Keterangan: ${item.notes}", fontSize = 11.sp, color = Color.Gray)
                if (item.photoUri.isNotBlank()) Text("📷 Bukti foto tersimpan", fontSize = 11.sp, color = FarmGreenPrimary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatter.format(item.totalAmount), fontWeight = FontWeight.ExtraBold, color = accent, fontSize = 14.sp)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.LightGray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceEntryDialog(
    type: String,
    cycleId: Long,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit
) {
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var category by remember { mutableStateOf(if (type == TYPE_IN) "Pemasukan" else "Perawatan Kandang") }
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf("") }

    val isIncome = type == TYPE_IN
    val title = if (isIncome) "Tambah Uang Masuk" else "Tambah Biaya / Pembelian Kandang"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (formError.isNotBlank()) Text(formError, color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Tanggal Pengeluaran / Pemasukan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_finance_date")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isIncome) "Nama Uang Masuk / Sumber" else "Biaya atau Pembelian") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(if (isIncome) "input_income_name" else "input_expense_name")
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Nilai Uang (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(if (isIncome) "input_income_amount" else "input_exp_amount")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_finance_category")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Keterangan") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_finance_notes")
                )

                Text("Bukti foto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                Text("Gunakan kamera atau pilih foto dari galeri.", fontSize = 11.sp, color = Color.Gray)
                PhotoProofPicker(initialPath = photoPath, onPathChanged = { photoPath = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (date.isBlank() || name.isBlank() || amount <= 0.0) {
                        formError = "Tanggal, nama biaya/pemasukan, dan nilai uang wajib diisi."
                        return@Button
                    }
                    onSave(
                        ExpenseEntity(
                            cycleId = cycleId,
                            transactionType = type,
                            date = date,
                            category = category,
                            expenseName = name,
                            quantity = 1.0,
                            unit = "",
                            unitPrice = amount,
                            totalAmount = amount,
                            proofNote = if (isIncome) "Bukti pemasukan" else "Bukti pengeluaran",
                            photoUri = photoPath,
                            notes = notes
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isIncome) Color(0xFF2E7D32) else FarmGreenPrimary),
                modifier = Modifier.testTag("btn_save_finance")
            ) { Text(if (isIncome) "Simpan Uang Masuk" else "Simpan Pengeluaran") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
