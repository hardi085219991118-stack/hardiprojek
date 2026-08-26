package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.DeletePinProtectedButton
import com.example.ui.components.ProcessNotificationDialog
import com.example.ui.components.ProcessState
import com.example.ui.components.rememberProcessState
import com.example.data.local.entity.CoopEntity
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
import com.example.util.LocationHelper
import com.example.util.PhotoStorageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoopScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val coops by viewModel.coops.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCoop by remember { mutableStateOf<CoopEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<CoopEntity?>(null) }
    var isLoadingGps by remember { mutableStateOf(false) }
    var processState by rememberProcessState()

    ProcessNotificationDialog(
        state = processState,
        onDismissRequest = { processState = ProcessState.Idle }
    )
    var statusMessage by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            statusMessage = "Izin lokasi diberikan. Silakan tekan Ambil Lokasi."
        } else {
            statusMessage = "Izin lokasi diperlukan untuk mengambil koordinat GPS kandang."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Kandang & Lokasi GPS", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_coops")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmGreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = {
                            editingCoop = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("btn_top_add_coop")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Tambah Kandang", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCoop = null
                    showAddDialog = true
                },
                containerColor = FarmGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_coop")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kandang")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_kandang), contentScale = ContentScale.Crop)
                .testTag("list_coops"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Roofing, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Sistem Manajemen Multi-Kandang", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            Text("Setiap kandang memiliki kapasitas, koordinat GPS, dan siklus tersendiri.", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            if (coops.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Roofing,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Belum Ada Data Kandang",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Silakan daftarkan kandang Anda untuk mulai mencatat siklus pemeliharaan, kapasitas, dan titik GPS.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    editingCoop = null
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                modifier = Modifier.testTag("btn_empty_add_coop")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ TAMBAH KANDANG BARU", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(coops) { coop ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_coop_${coop.id}"),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = coop.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = FarmGreenPrimary
                                )
                                Text(
                                    text = "Kode: ${coop.code} | Tipe: ${coop.coopType}",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingCoop = coop
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { deleteCandidate = coop },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Kapasitas", fontSize = 11.sp, color = Color.Gray)
                                Text("${coop.capacity} Ekor", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Dimensi (P x L)", fontSize = 11.sp, color = Color.Gray)
                                Text("${coop.lengthM.toInt()}m x ${coop.widthM.toInt()}m (${coop.areaSqm.toInt()}m²)", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Pemilik", fontSize = 11.sp, color = Color.Gray)
                                Text(coop.ownerName.ifEmpty { "-" }, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Alamat: ${coop.address}, ${coop.district}, ${coop.regency}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        // GPS Coordinate Block & Google Maps button
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("KOORDINAT GPS KANDANG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                                    if (coop.latitude != null && coop.longitude != null) {
                                        Text(
                                            text = "Lat: ${String.format(Locale.US, "%.5f", coop.latitude)}, Lng: ${String.format(Locale.US, "%.5f", coop.longitude)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        if (coop.gpsAccuracy != null) {
                                            Text("Akurasi: ±${String.format(Locale.US, "%.1f", coop.gpsAccuracy)} meter", fontSize = 10.sp, color = Color.DarkGray)
                                        }
                                    } else {
                                        Text("Belum ada koordinat tersimpan", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }

                                if (coop.latitude != null && coop.longitude != null) {
                                    Button(
                                        onClick = {
                                            LocationHelper.openInGoogleMaps(context, coop.latitude, coop.longitude, coop.name)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("btn_open_maps_${coop.id}")
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Google Maps", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (coop.photoUri.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            var showFullPhoto by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .clickable { showFullPhoto = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Lihat Foto Kandang", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            }

                            if (showFullPhoto) {
                                val bitmap = remember(coop.photoUri) { com.example.util.PhotoStorageHelper.loadBitmapSafe(context, coop.photoUri, maxDim = 800) }
                                AlertDialog(
                                    onDismissRequest = { showFullPhoto = false },
                                    title = { Text("Foto Kandang - ${coop.name}", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Foto Kandang",
                                                    modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Text("Foto tersimpan di: ${coop.photoUri}", fontSize = 12.sp)
                                            }
                                            Text("${coop.name} (${coop.code}) - Kapasitas ${coop.capacity} Ekor", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showFullPhoto = false }) { Text("Tutup") }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf(editingCoop?.name ?: "") }
        var code by remember { mutableStateOf(editingCoop?.code ?: "KND-0${coops.size + 1}") }
        var address by remember { mutableStateOf(editingCoop?.address ?: "") }
        var village by remember { mutableStateOf(editingCoop?.village ?: "") }
        var district by remember { mutableStateOf(editingCoop?.district ?: "") }
        var regency by remember { mutableStateOf(editingCoop?.regency ?: "") }
        var province by remember { mutableStateOf(editingCoop?.province ?: "") }
        var capacityStr by remember { mutableStateOf(editingCoop?.capacity?.takeIf { it > 0 }?.toString() ?: "") }
        var lengthStr by remember { mutableStateOf(editingCoop?.lengthM?.takeIf { it > 0 }?.toString() ?: "") }
        var widthStr by remember { mutableStateOf(editingCoop?.widthM?.takeIf { it > 0 }?.toString() ?: "") }
        var coopType by remember { mutableStateOf(editingCoop?.coopType ?: "") }
        var owner by remember { mutableStateOf(editingCoop?.ownerName ?: "") }
        var phone by remember { mutableStateOf(editingCoop?.phoneNumber ?: "") }
        var notes by remember { mutableStateOf(editingCoop?.notes ?: "") }
        var photoPath by remember { mutableStateOf(editingCoop?.photoUri ?: "") }

        var lat by remember { mutableStateOf(editingCoop?.latitude) }
        var lng by remember { mutableStateOf(editingCoop?.longitude) }
        var acc by remember { mutableStateOf(editingCoop?.gpsAccuracy) }
        var gpsTime by remember { mutableStateOf(editingCoop?.gpsTimestamp) }
        var formError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingCoop == null) "Tambah Data Kandang" else "Edit Data Kandang", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (formError.isNotEmpty()) {
                        Text(formError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Kandang") },
                        modifier = Modifier.fillMaxWidth().testTag("input_coop_name")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Kode Kandang") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = capacityStr,
                            onValueChange = { capacityStr = it },
                            label = { Text("Kapasitas (Ekor)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_coop_capacity")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lengthStr,
                            onValueChange = { lengthStr = it },
                            label = { Text("Panjang (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = widthStr,
                            onValueChange = { widthStr = it },
                            label = { Text("Lebar (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = coopType,
                        onValueChange = { coopType = it },
                        label = { Text("Tipe Kandang (Closed House / Semi-Closed / Open)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat / Lokasi Kandang") },
                        modifier = Modifier.fillMaxWidth().testTag("input_coop_address")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = village,
                            onValueChange = { village = it },
                            label = { Text("Desa") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("Kecamatan") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = regency,
                            onValueChange = { regency = it },
                            label = { Text("Kabupaten") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = province,
                            onValueChange = { province = it },
                            label = { Text("Provinsi") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = owner,
                            onValueChange = { owner = it },
                            label = { Text("Nama Pemilik") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("No. Telepon") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // GPS Section inside form
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("PENGAMBILAN KOORDINAT GPS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FarmGreenPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (lat != null && lng != null) {
                                Text("Lat: $lat, Lng: $lng", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                if (acc != null) Text("Akurasi: ±$acc m", fontSize = 10.sp, color = Color.DarkGray)
                            } else {
                                Text("Koordinat belum diambil", fontSize = 11.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (!fineGranted && !coarseGranted) {
                                        locationPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                        )
                                    } else {
                                        isLoadingGps = true
                                        coroutineScope.launch {
                                            val loc = LocationHelper.getCurrentLocation(context)
                                            isLoadingGps = false
                                            if (loc != null) {
                                                lat = loc.latitude
                                                lng = loc.longitude
                                                acc = loc.accuracy
                                                gpsTime = loc.timestamp
                                            } else {
                                                formError = "Gagal mengambil lokasi GPS. Pastikan GPS aktif."
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("btn_fetch_gps")
                            ) {
                                if (isLoadingGps) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mengambil Koordinat...")
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ambil Lokasi Saya (GPS)")
                                }
                            }
                        }
                    }

                    PhotoProofPicker(
                        initialPath = photoPath,
                        onPathChanged = { photoPath = it },
                        feature = "kandang",
                        title = "Foto Kandang (Depan / Tampak Luar)"
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Keterangan Tambahan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val isProcessing = processState is ProcessState.Processing
                Button(
                    onClick = {
                        if (isProcessing) return@Button
                        if (name.isBlank()) {
                            formError = "Nama kandang wajib diisi!"
                            return@Button
                        }
                        val cap = capacityStr.toIntOrNull() ?: 0
                        val len = lengthStr.toDoubleOrNull() ?: 0.0
                        val wid = widthStr.toDoubleOrNull() ?: 0.0
                        val area = len * wid

                        val isEdit = editingCoop != null
                        processState = ProcessState.Processing(
                            title = if (isEdit) "MENYIMPAN PERUBAHAN KANDANG" else "MENYIMPAN DATA KANDANG",
                            message = "Sedang memvalidasi spesifikasi kandang...",
                            step = if (photoPath.isNotBlank()) "Menyimpan foto fisik & koordinat GPS" else "Menyimpan profil kandang"
                        )

                        val coopToSave = CoopEntity(
                            id = editingCoop?.id ?: 0,
                            photoUri = photoPath,
                            name = name,
                            code = code,
                            address = address,
                            village = village,
                            district = district,
                            regency = regency,
                            province = province,
                            areaSqm = area,
                            lengthM = len,
                            widthM = wid,
                            capacity = cap,
                            coopType = coopType,
                            ownerName = owner,
                            phoneNumber = phone,
                            notes = notes,
                            latitude = lat,
                            longitude = lng,
                            gpsAccuracy = acc,
                            gpsTimestamp = gpsTime ?: System.currentTimeMillis()
                        )

                        coroutineScope.launch {
                            delay(300)
                            viewModel.saveCoop(coopToSave) {
                                showAddDialog = false
                                processState = ProcessState.Success(
                                    title = "DATA KANDANG DISIMPAN",
                                    message = "Data kandang '$name' (Kapasitas $cap ekor) berhasil disimpan.",
                                    detail = "Tipe: $coopType | Luas: $area m²"
                                )
                            }
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("btn_save_coop")
                ) {
                    Text("Simpan Kandang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete confirmation
    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Hapus Kandang?") },
            text = { Text("Apakah Anda yakin ingin menghapus data '${deleteCandidate?.name}'?") },
            confirmButton = {
                DeletePinProtectedButton(onAuthorizedDelete = {
                    val candidate = deleteCandidate
                    deleteCandidate = null
                    if (candidate != null) {
                        processState = ProcessState.Processing(
                            title = "MENGHAPUS DATA KANDANG",
                            message = "Sedang menghapus master kandang..."
                        )
                        coroutineScope.launch {
                            delay(200)
                            viewModel.deleteCoop(candidate)
                            processState = ProcessState.Success(
                                title = "KANDANG BERHASIL DIHAPUS",
                                message = "Data kandang '${candidate.name}' telah dihapus."
                            )
                        }
                    }
                })
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
