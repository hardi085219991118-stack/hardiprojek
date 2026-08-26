package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MemberEntity
import com.example.ui.FarmViewModel
import com.example.ui.components.ProcessNotificationDialog
import com.example.ui.components.ProcessState
import com.example.ui.components.rememberProcessState
import com.example.ui.theme.FarmGreenPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    viewModel: FarmViewModel,
    onNavigateBack: () -> Unit
) {
    val members by viewModel.members.collectAsState()
    val scope = rememberCoroutineScope()
    var processState by rememberProcessState()

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("SEMUA") } // SEMUA, AKTIF, NON_AKTIF
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedMemberForEdit by remember { mutableStateOf<MemberEntity?>(null) }
    var memberToDelete by remember { mutableStateOf<MemberEntity?>(null) }

    val filteredMembers = remember(members, searchQuery, statusFilter) {
        members.filter { m ->
            val matchesQuery = m.name.contains(searchQuery, ignoreCase = true) ||
                    m.memberNumber.contains(searchQuery, ignoreCase = true) ||
                    m.phone.contains(searchQuery, ignoreCase = true)
            val matchesStatus = when (statusFilter) {
                "AKTIF" -> m.isActive
                "NON_AKTIF" -> !m.isActive
                else -> true
            }
            matchesQuery && matchesStatus
        }
    }

    val totalMembers = members.size
    val activeMembers = members.count { it.isActive }
    val inactiveMembers = totalMembers - activeMembers

    Scaffold(
        modifier = Modifier.testTag("members_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Data Anggota", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Kelola Anggota & Kemitraan Hasil Usaha",
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
                onClick = {
                    selectedMemberForEdit = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Tambah Anggota", fontWeight = FontWeight.Bold) },
                containerColor = FarmGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_member_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Stat Cards Baris
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemberStatCard(
                    title = "Total Anggota",
                    value = "$totalMembers Orang",
                    color = FarmGreenPrimary,
                    modifier = Modifier.weight(1f)
                )
                MemberStatCard(
                    title = "Aktif",
                    value = "$activeMembers Orang",
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                MemberStatCard(
                    title = "Non-Aktif",
                    value = "$inactiveMembers Orang",
                    color = Color(0xFF757575),
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("search_member_input"),
                placeholder = { Text("Cari nama, ID, atau no telepon...") },
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

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == "SEMUA",
                    onClick = { statusFilter = "SEMUA" },
                    label = { Text("Semua ($totalMembers)") }
                )
                FilterChip(
                    selected = statusFilter == "AKTIF",
                    onClick = { statusFilter = "AKTIF" },
                    label = { Text("Aktif ($activeMembers)") }
                )
                FilterChip(
                    selected = statusFilter == "NON_AKTIF",
                    onClick = { statusFilter = "NON_AKTIF" },
                    label = { Text("Non-Aktif ($inactiveMembers)") }
                )
            }

            // List of Members
            if (filteredMembers.isEmpty()) {
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
                            Icons.Default.GroupOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isNotBlank() || statusFilter != "SEMUA")
                                "Tidak ada anggota yang cocok dengan filter."
                            else
                                "Belum ada data anggota.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Tambahkan nama anggota untuk mempermudah perhitungan dan cetak pembagian hasil usaha.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                selectedMemberForEdit = null
                                showAddEditDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah Anggota Sekarang")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("members_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredMembers, key = { it.id }) { member ->
                        MemberItemCard(
                            member = member,
                            onEdit = {
                                selectedMemberForEdit = member
                                showAddEditDialog = true
                            },
                            onDelete = {
                                memberToDelete = member
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Add / Edit Member
    if (showAddEditDialog) {
        AddEditMemberDialog(
            initialMember = selectedMemberForEdit,
            totalExistingMembers = members.size,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedMember ->
                showAddEditDialog = false
                processState = ProcessState.Processing(
                    title = "MENYIMPAN DATA ANGGOTA",
                    message = "Menyimpan data anggota ke database..."
                )
                viewModel.saveMember(
                    member = updatedMember,
                    onSuccess = {
                        scope.launch {
                            delay(300)
                            processState = ProcessState.Success(
                                title = "Berhasil Tersimpan",
                                message = "Data anggota '${updatedMember.name}' berhasil diperbarui.",
                                onDismiss = { processState = ProcessState.Idle }
                            )
                        }
                    },
                    onError = { error ->
                        scope.launch {
                            processState = ProcessState.Error(
                                title = "Gagal Menyimpan",
                                message = error,
                                onDismiss = { processState = ProcessState.Idle }
                            )
                        }
                    }
                )
            }
        )
    }

    // Konfirmasi Hapus Member Dialog
    memberToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Anggota?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus data anggota '${member.name}' (${member.memberNumber.ifBlank { "Tanpa ID" }})? Tindakan ini tidak dapat dibatalkan.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = member
                        memberToDelete = null
                        processState = ProcessState.Processing(
                            title = "MENGHAPUS ANGGOTA",
                            message = "Menghapus data anggota..."
                        )
                        viewModel.deleteMember(
                            member = target,
                            onSuccess = {
                                scope.launch {
                                    delay(200)
                                    processState = ProcessState.Success(
                                        title = "Anggota Terhapus",
                                        message = "Data anggota '${target.name}' telah dihapus.",
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
                    modifier = Modifier.testTag("confirm_delete_member_btn")
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Process State Notification
    ProcessNotificationDialog(state = processState)
}

@Composable
private fun MemberStatCard(
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
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun MemberItemCard(
    member: MemberEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_item_${member.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Initial
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (member.isActive) FarmGreenPrimary else Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                val initial = member.name.trim().take(1).uppercase(Locale.ROOT).ifBlank { "A" }
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Member Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (member.isActive) Color(0xFFE8F5E9) else Color(0xFFEEEEEE)
                    ) {
                        Text(
                            text = if (member.isActive) "Aktif" else "Non-Aktif",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (member.isActive) Color(0xFF2E7D32) else Color(0xFF757575),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (member.memberNumber.isNotBlank()) {
                    Text(
                        text = "ID: ${member.memberNumber}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (member.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = member.phone,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (member.notes.isNotBlank()) {
                    Text(
                        text = member.notes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Action Buttons
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_member_${member.id}")
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Anggota",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_member_${member.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Hapus Anggota",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddEditMemberDialog(
    initialMember: MemberEntity?,
    totalExistingMembers: Int,
    onDismiss: () -> Unit,
    onSave: (MemberEntity) -> Unit
) {
    val isEdit = initialMember != null
    var name by remember { mutableStateOf(initialMember?.name ?: "") }
    var memberNumber by remember {
        mutableStateOf(
            initialMember?.memberNumber ?: "SB-${String.format(Locale.ROOT, "%03d", totalExistingMembers + 1)}"
        )
    }
    var phone by remember { mutableStateOf(initialMember?.phone ?: "") }
    var notes by remember { mutableStateOf(initialMember?.notes ?: "") }
    var isActive by remember { mutableStateOf(initialMember?.isActive ?: true) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) "Edit Data Anggota" else "Tambah Anggota Baru",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Nama Lengkap Anggota *") },
                    placeholder = { Text("Contoh: Ahmad Subagyo") },
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text("Nama anggota wajib diisi", color = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = memberNumber,
                    onValueChange = { memberNumber = it },
                    label = { Text("Nomor / ID Anggota") },
                    placeholder = { Text("Contoh: SB-001") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_number_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. Telepon / WhatsApp") },
                    placeholder = { Text("Contoh: 08123456789") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_phone_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Keterangan Tambahan") },
                    placeholder = { Text("Contoh: Blok A / Peternak Mitra") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Status Anggota Aktif",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("member_active_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val target = initialMember?.copy(
                        name = name.trim(),
                        memberNumber = memberNumber.trim(),
                        phone = phone.trim(),
                        notes = notes.trim(),
                        isActive = isActive
                    ) ?: MemberEntity(
                        name = name.trim(),
                        memberNumber = memberNumber.trim(),
                        phone = phone.trim(),
                        notes = notes.trim(),
                        isActive = isActive
                    )
                    onSave(target)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                modifier = Modifier.testTag("save_member_btn")
            ) {
                Text(if (isEdit) "Simpan Perubahan" else "Tambah Anggota")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
