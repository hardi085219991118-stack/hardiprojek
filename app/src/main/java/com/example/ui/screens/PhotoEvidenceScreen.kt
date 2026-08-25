package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.ui.components.DeletePinProtectedIconButton
import com.example.data.local.entity.FarmProfileEntity
import com.example.data.local.entity.PhotoEvidenceEntity
import com.example.util.WatermarkHelper
import com.example.util.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEvidenceScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).farmDao() }
    val currentUserId = remember { UserSessionManager.getCurrentUserId(context) }

    val allPhotos by dao.getAllPhotos().collectAsState(initial = emptyList())
    val photos = remember(allPhotos, currentUserId) { allPhotos.filter { it.userId == currentUserId } }
    val allCoops by dao.getAllCoops().collectAsState(initial = emptyList())
    val allCycles by dao.getAllCycles().collectAsState(initial = emptyList())
    val coops = remember(allCoops, currentUserId) { allCoops.filter { it.userId == currentUserId } }
    val cycles = remember(allCycles, currentUserId) { allCycles.filter { it.userId == currentUserId } }
    var farmProfile by remember { mutableStateOf<FarmProfileEntity?>(null) }

    var selectedCategoryFilter by remember { mutableStateOf("SEMUA") }
    var showCaptureDialog by remember { mutableStateOf(false) }
    var selectedPhotoDetail by remember { mutableStateOf<PhotoEvidenceEntity?>(null) }

    // New Photo Capture State
    var selectedCategory by remember { mutableStateOf("Mortalitas / Afkir") }
    var photoNotes by remember { mutableStateOf("") }
    var selectedCoopId by remember { mutableLongStateOf(0L) }
    var selectedCycleId by remember { mutableLongStateOf(0L) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val categories = listOf(
        "Mortalitas / Afkir",
        "Pakan & Silo",
        "Kondisi Kandang & Blower",
        "Sampling Bobot Ayam",
        "DOC / Chick In",
        "Vaksinasi & Obat",
        "Panen & Penimbangan",
        "Lainnya"
    )

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            farmProfile = dao.getFarmProfileDirect(currentUserId)
        }
    }

    LaunchedEffect(coops, cycles) {
        if (coops.isNotEmpty() && selectedCoopId == 0L) {
            selectedCoopId = coops.firstOrNull()?.id ?: 0L
        }
        if (cycles.isNotEmpty() && selectedCycleId == 0L) {
            selectedCycleId = cycles.firstOrNull()?.id ?: 0L
        }
    }

    // Function to process photo and apply watermark
    fun processAndSavePhoto(sourceFile: File) {
        isProcessing = true
        coroutineScope.launch {
            try {
                val currentCoop = coops.find { it.id == selectedCoopId }
                val currentCycle = cycles.find { it.id == selectedCycleId }
                val profile = farmProfile

                // Get GPS if available
                var gpsLat: Double? = null
                var gpsLng: Double? = null
                var gpsAcc: Float? = null

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val loc: Location? = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    loc?.let {
                        gpsLat = it.latitude
                        gpsLng = it.longitude
                        gpsAcc = it.accuracy
                    }
                }

                // Apply watermark using WatermarkHelper
                val watermarkedFile = withContext(Dispatchers.IO) {
                    WatermarkHelper.applyWatermark(
                        context = context,
                        sourceFile = sourceFile,
                        farmName = profile?.farmName ?: "SEJAHTERA BERSAMA",
                        coopName = currentCoop?.name ?: "Kandang Utama",
                        cycleNumber = currentCycle?.cycleNumber ?: "Siklus Broiler",
                        category = selectedCategory,
                        latitude = gpsLat,
                        longitude = gpsLng,
                        gpsAccuracy = gpsAcc,
                        customCaption = photoNotes
                    )
                }

                val now = Date()
                val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID")).format(now)
                val timeStr = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(now)

                withContext(Dispatchers.IO) {
                    val entity = PhotoEvidenceEntity(
                        userId = currentUserId,
                        cycleId = selectedCycleId,
                        coopId = selectedCoopId,
                        reportType = selectedCategory,
                        date = dateStr,
                        time = timeStr,
                        caption = photoNotes,
                        photoUri = sourceFile.absolutePath,
                        watermarkedUri = watermarkedFile.absolutePath,
                        latitude = gpsLat,
                        longitude = gpsLng,
                        gpsAccuracy = gpsAcc
                    )
                    dao.insertPhoto(entity)
                }

                isProcessing = false
                showCaptureDialog = false
                photoNotes = ""
                Toast.makeText(context, "Foto bukti ber-watermark otomatis berhasil disimpan!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isProcessing = false
                Toast.makeText(context, "Gagal memproses foto: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraFile != null && tempCameraFile!!.exists()) {
            processAndSavePhoto(tempCameraFile!!)
        }
    }

    // Gallery / Image Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val tempFile = WatermarkHelper.createTempImageFile(context)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val outputStream = FileOutputStream(tempFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    processAndSavePhoto(tempFile)
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal mengambil gambar dari galeri: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = WatermarkHelper.createTempImageFile(context)
            tempCameraFile = file
            val uri = WatermarkHelper.getImageUri(context, file)
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto bukti.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Foto Bukti & Watermark",
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
                actions = {
                    IconButton(onClick = { showCaptureDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Tambah Foto",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCaptureDialog = true },
                containerColor = Color(0xFF1B5E20),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                text = { Text("Ambil Bukti Foto", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAF7))
        ) {
            // Header Info & Watermark Notice
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Watermark Otomatis Terverifikasi",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        )
                        Text(
                            text = "Setiap foto dibubuhi Nama Usaha, Kandang, Siklus, Tanggal, Jam, dan Koordinat GPS real-time untuk audit kemitraan.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF333333))
                        )
                    }
                }
            }

            // Category Filter Chips
            ScrollableTabRow(
                selectedTabIndex = if (selectedCategoryFilter == "SEMUA") 0 else (categories.indexOf(selectedCategoryFilter) + 1).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = selectedCategoryFilter == "SEMUA",
                    onClick = { selectedCategoryFilter = "SEMUA" },
                    text = { Text("Semua (${photos.size})") }
                )
                categories.forEach { cat ->
                    val count = photos.count { it.reportType == cat }
                    Tab(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        text = { Text("$cat ($count)") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Photos Grid
            val filteredPhotos = if (selectedCategoryFilter == "SEMUA") {
                photos
            } else {
                photos.filter { it.reportType == selectedCategoryFilter }
            }

            if (filteredPhotos.isEmpty()) {
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
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Belum ada foto bukti untuk kategori ini",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                        )
                        Button(
                            onClick = { showCaptureDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ambil Foto Sekarang")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPhotos, key = { it.id }) { photo ->
                        PhotoEvidenceCard(
                            photo = photo,
                            onClick = { selectedPhotoDetail = photo }
                        )
                    }
                }
            }
        }
    }

    // Capture / Add Photo Dialog
    if (showCaptureDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showCaptureDialog = false },
            title = {
                Text(
                    "Ambil Foto Bukti Peternakan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pilih kategori dan data kandang untuk disematkan pada watermark:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )

                    // Category Selector
                    Text("Kategori Bukti:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    var expandedCategory by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = !expandedCategory }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }

                    // Coop Selector
                    if (coops.isNotEmpty()) {
                        Text("Pilih Kandang:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        var expandedCoop by remember { mutableStateOf(false) }
                        val activeCoop = coops.find { it.id == selectedCoopId }
                        ExposedDropdownMenuBox(
                            expanded = expandedCoop,
                            onExpandedChange = { expandedCoop = !expandedCoop }
                        ) {
                            OutlinedTextField(
                                value = activeCoop?.name ?: "Pilih Kandang",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCoop) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCoop,
                                onDismissRequest = { expandedCoop = false }
                            ) {
                                coops.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name) },
                                        onClick = {
                                            selectedCoopId = c.id
                                            expandedCoop = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Cycle Selector
                    if (cycles.isNotEmpty()) {
                        Text("Pilih Siklus:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        var expandedCycle by remember { mutableStateOf(false) }
                        val activeCycle = cycles.find { it.id == selectedCycleId }
                        ExposedDropdownMenuBox(
                            expanded = expandedCycle,
                            onExpandedChange = { expandedCycle = !expandedCycle }
                        ) {
                            OutlinedTextField(
                                value = activeCycle?.cycleNumber ?: "Pilih Siklus",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCycle) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCycle,
                                onDismissRequest = { expandedCycle = false }
                            ) {
                                cycles.forEach { cyc ->
                                    DropdownMenuItem(
                                        text = { Text(cyc.cycleNumber) },
                                        onClick = {
                                            selectedCycleId = cyc.id
                                            expandedCycle = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Notes / Caption
                    OutlinedTextField(
                        value = photoNotes,
                        onValueChange = { photoNotes = it },
                        label = { Text("Catatan / Keterangan Bukti") },
                        placeholder = { Text("Contoh: Kondisi blower fan 1-4 normal, sekam kering.") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    if (isProcessing) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF1B5E20),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Menerapkan watermark otomatis...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        // Options: Camera or Gallery
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        val file = WatermarkHelper.createTempImageFile(context)
                                        tempCameraFile = file
                                        val uri = WatermarkHelper.getImageUri(context, file)
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kamera", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galeri", fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showCaptureDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Photo Detail & Share Dialog
    selectedPhotoDetail?.let { photo ->
        AlertDialog(
            onDismissRequest = { selectedPhotoDetail = null },
            title = {
                Text(
                    photo.reportType,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val file = File(photo.watermarkedUri.ifBlank { photo.photoUri })
                    if (file.exists()) {
                        val bitmap = remember(photo.watermarkedUri, photo.photoUri) {
                            WatermarkHelper.loadThumbnail(file, maxDim = 800)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = photo.caption,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Waktu: ${photo.date}, ${photo.time} WIB", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            if (photo.caption.isNotBlank()) {
                                Text("Catatan: ${photo.caption}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (photo.latitude != null && photo.longitude != null) {
                                Text("GPS: ${photo.latitude}, ${photo.longitude} (±${photo.gpsAccuracy ?: 0f}m)", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1B5E20)))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = File(photo.watermarkedUri.ifBlank { photo.photoUri })
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_TEXT, "Bukti Foto Budidaya Broiler [${photo.reportType}] - ${farmProfile?.farmName ?: "Sejahtera Bersama"}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Foto Bukti"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan")
                }
            },
            dismissButton = {
                Row {
                    DeletePinProtectedIconButton(
                        onAuthorizedDelete = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    dao.deletePhoto(photo)
                                    val f = File(photo.watermarkedUri)
                                    if (f.exists()) f.delete()
                                }
                                selectedPhotoDetail = null
                                Toast.makeText(context, "Foto bukti dihapus.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    TextButton(onClick = { selectedPhotoDetail = null }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }
}

@Composable
private fun PhotoEvidenceCard(
    photo: PhotoEvidenceEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            val file = File(photo.watermarkedUri.ifBlank { photo.photoUri })
            if (file.exists()) {
                val bitmap = remember(photo.watermarkedUri, photo.photoUri) {
                    WatermarkHelper.loadThumbnail(file, maxDim = 400)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = photo.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.DarkGray)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF2E7D32))
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = photo.reportType,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${photo.date} • ${photo.time}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )

                if (photo.caption.isNotBlank()) {
                    Text(
                        text = photo.caption,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
