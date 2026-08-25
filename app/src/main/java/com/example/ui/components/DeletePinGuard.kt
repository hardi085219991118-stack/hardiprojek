package com.example.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Guard untuk semua aksi penghapusan data.
 * PIN 8888 diminta setiap kali sebelum aksi destruktif dijalankan agar data
 * tidak terhapus karena salah sentuh. PIN ini adalah perlindungan operasional,
 * bukan pengganti autentikasi akun.
 */
private const val DELETE_PIN = "8888"

@Composable
fun DeletePinProtectedButton(
    onAuthorizedDelete: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    label: String = "Hapus"
) {
    var showPinDialog by remember { mutableStateOf(false) }
    Button(
        onClick = { showPinDialog = true },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
    ) { Text(label) }

    if (showPinDialog) {
        DeletePinDialog(
            onDismiss = { showPinDialog = false },
            onAuthorized = {
                showPinDialog = false
                onAuthorizedDelete()
            }
        )
    }
}

@Composable
fun DeletePinProtectedTextButton(
    onAuthorizedDelete: () -> Unit,
    label: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    var showPinDialog by remember { mutableStateOf(false) }
    TextButton(onClick = { showPinDialog = true }, modifier = modifier) {
        Icon(Icons.Default.Delete, contentDescription = null)
        Spacer(androidx.compose.ui.Modifier.width(4.dp))
        Text(label)
    }
    if (showPinDialog) {
        DeletePinDialog(
            onDismiss = { showPinDialog = false },
            onAuthorized = {
                showPinDialog = false
                onAuthorizedDelete()
            }
        )
    }
}

@Composable
fun DeletePinProtectedIconButton(
    onAuthorizedDelete: () -> Unit,
    contentDescription: String = "Hapus",
    tint: Color = Color.Red,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    var showPinDialog by remember { mutableStateOf(false) }
    IconButton(onClick = { showPinDialog = true }, modifier = modifier) {
        Icon(Icons.Default.Delete, contentDescription = contentDescription, tint = tint)
    }
    if (showPinDialog) {
        DeletePinDialog(
            onDismiss = { showPinDialog = false },
            onAuthorized = {
                showPinDialog = false
                onAuthorizedDelete()
            }
        )
    }
}

@Composable
fun DeletePinDialog(
    onDismiss: () -> Unit,
    onAuthorized: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var invalidPin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kode Keamanan Penghapusan") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text("Untuk mencegah data terhapus tidak sengaja, masukkan kode 8888 untuk melanjutkan.")
                Spacer(androidx.compose.ui.Modifier.width(1.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value.filter(Char::isDigit).take(4)
                        invalidPin = false
                    },
                    label = { Text("Kode Penghapusan") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = invalidPin
                )
                if (invalidPin) Text("Kode salah. Data tidak dihapus.", color = Color(0xFFC62828))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin == DELETE_PIN) onAuthorized() else invalidPin = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) { Text("Konfirmasi Hapus") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
