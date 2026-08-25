package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.DeletePinProtectedTextButton
import com.example.ui.theme.FarmGreenPrimary
import com.example.util.PhotoStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PhotoProofPicker(
    initialPath: String = "",
    onPathChanged: (String) -> Unit,
    feature: String = "umum",
    title: String = "Bukti Foto (Kamera / Galeri)"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var cameraTempFile by remember { mutableStateOf<File?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showFullPreview by remember { mutableStateOf(false) }

    // Load bitmap asynchronously & safely
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(initialPath) {
        if (initialPath.isNotBlank()) {
            withContext(Dispatchers.IO) {
                loadedBitmap = PhotoStorageHelper.loadBitmapSafe(context, initialPath, maxDim = 800)
            }
        } else {
            loadedBitmap = null
        }
    }

    // Process gallery image selection
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isProcessing = true
        coroutineScope.launch {
            try {
                val savedPath = withContext(Dispatchers.IO) {
                    PhotoStorageHelper.processGallerySelection(
                        context = context,
                        sourceUri = uri,
                        category = feature
                    )
                }
                isProcessing = false
                if (!savedPath.isNullOrBlank()) {
                    onPathChanged(savedPath)
                    Toast.makeText(context, "Foto bukti berhasil disimpan di penyimpanan aman.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal memproses foto dari galeri.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                isProcessing = false
                Toast.makeText(context, "Terjadi kesalahan saat memproses galeri: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Process camera capture
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraTempFile
        if (success && file != null && file.exists() && file.length() > 0L) {
            isProcessing = true
            coroutineScope.launch {
                try {
                    val savedPath = withContext(Dispatchers.IO) {
                        PhotoStorageHelper.processCameraCapture(
                            context = context,
                            tempFile = file,
                            category = feature
                        )
                    }
                    isProcessing = false
                    if (!savedPath.isNullOrBlank()) {
                        onPathChanged(savedPath)
                        Toast.makeText(context, "Foto kamera berhasil disimpan secara permanen.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal memproses hasil foto kamera.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    isProcessing = false
                    Toast.makeText(context, "Terjadi kesalahan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // Camera was cancelled
            try {
                if (file != null && file.exists()) file.delete()
            } catch (_: Exception) {}
        }
    }

    // Camera permission request
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val tempFile = PhotoStorageHelper.createCameraTempFile(context, feature)
            cameraTempFile = tempFile
            val uri = PhotoStorageHelper.getUriForFile(context, tempFile)
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto bukti.", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = null,
                    tint = FarmGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = FarmGreenPrimary
                )
            }

            if (isProcessing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = FarmGreenPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Memproses & menyimpan foto ke memori permanen...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            } else {
                Text(
                    text = if (initialPath.isBlank()) {
                        "Belum ada bukti foto tersimpan. Ambil dari kamera atau pilih dari galeri."
                    } else {
                        "✓ Foto bukti tersimpan permanen di penyimpanan aplikasi."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (initialPath.isBlank()) Color.Gray else Color(0xFF2E7D32),
                    fontSize = 11.sp
                )

                // Preview Thumbnail
                if (initialPath.isNotBlank()) {
                    val bmp = loadedBitmap
                    if (bmp != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.05f))
                                .clickable { showFullPreview = true }
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Pratinjau bukti foto tersimpan",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(topStart = 6.dp),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ZoomIn,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Ketuk untuk perbesar",
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color(0xFFF57F17),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "File bukti tersimpan (${File(initialPath).name.take(24)}...)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }

                // Action Buttons: Kamera & Galeri
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val tempFile = PhotoStorageHelper.createCameraTempFile(context, feature)
                                cameraTempFile = tempFile
                                val uri = PhotoStorageHelper.getUriForFile(context, tempFile)
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_picker_camera"),
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kamera", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_picker_gallery"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Galeri", fontSize = 12.sp)
                    }
                }

                // Delete Photo Button (if photo exists)
                if (initialPath.isNotBlank()) {
                    DeletePinProtectedTextButton(
                        onAuthorizedDelete = {
                            onPathChanged("")
                            Toast.makeText(context, "Bukti foto dihapus dari form.", Toast.LENGTH_SHORT).show()
                        },
                        label = "Hapus Foto Bukti"
                    )
                }
            }
        }
    }

    // Full screen preview dialog
    if (showFullPreview && loadedBitmap != null) {
        AlertDialog(
            onDismissRequest = { showFullPreview = false },
            title = {
                Text(
                    "Pratinjau Bukti Foto",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        bitmap = loadedBitmap!!.asImageBitmap(),
                        contentDescription = "Bukti Foto Lengkap",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Lokasi file: ${File(initialPath).name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFullPreview = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}
