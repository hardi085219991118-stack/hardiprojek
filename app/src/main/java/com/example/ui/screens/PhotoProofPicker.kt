package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ui.components.DeletePinProtectedTextButton
import com.example.util.WatermarkHelper
import java.io.File
import java.io.FileOutputStream

@Composable
fun PhotoProofPicker(
    initialPath: String = "",
    onPathChanged: (String) -> Unit,
    title: String = "Bukti Foto (Kamera / Galeri)"
) {
    val context = LocalContext.current
    var cameraFile by remember { mutableStateOf<File?>(null) }

    fun copyToEvidence(uri: Uri) {
        try {
            val target = File(context.filesDir, "evidence_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw IllegalStateException("Gambar tidak dapat dibaca")
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            onPathChanged(target.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal menyimpan foto: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { copyToEvidence(it) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = cameraFile
        if (ok && file != null && file.exists()) onPathChanged(file.absolutePath)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = WatermarkHelper.createTempImageFile(context)
            cameraFile = file
            cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        } else Toast.makeText(context, "Izin kamera diperlukan.", Toast.LENGTH_SHORT).show()
    }

    val path = initialPath
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color(0xFF1B5E20))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Text(
                if (path.isBlank()) "Belum ada bukti foto. Foto dapat diambil dari kamera atau dipilih dari galeri." else "Bukti foto sudah dipilih.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
            if (path.isNotBlank()) {
                val bitmap = remember(path) { android.graphics.BitmapFactory.decodeFile(path) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Pratinjau bukti foto",
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            val file = WatermarkHelper.createTempImageFile(context)
                            cameraFile = file
                            cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                        } else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(5.dp)); Text("Kamera")
                }
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(5.dp)); Text("Galeri")
                }
            }
            if (path.isNotBlank()) {
                DeletePinProtectedTextButton(
                    onAuthorizedDelete = { onPathChanged("") },
                    label = "Hapus Foto"
                )
            }
        }
    }
}
