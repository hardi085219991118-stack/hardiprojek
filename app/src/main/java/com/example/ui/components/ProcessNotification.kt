package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.FarmGreenPrimary

/**
 * Representasi State Notifikasi Proses Terpadu untuk Seluruh Fitur SEJAHTERA BERSAMA
 */
sealed interface ProcessState {
    data object Idle : ProcessState

    data class Processing(
        val title: String = "Sedang Memproses",
        val message: String = "Harap tunggu, proses sedang berlangsung...",
        val step: String? = null
    ) : ProcessState

    data class Success(
        val title: String = "Berhasil Disimpan",
        val message: String = "Data berhasil disimpan ke sistem.",
        val detail: String? = null,
        val onDismiss: () -> Unit = {}
    ) : ProcessState

    data class Error(
        val title: String = "Terjadi Kesalahan",
        val message: String = "Gagal memproses data. Silakan periksa kembali dan coba lagi.",
        val detail: String? = null,
        val onDismiss: () -> Unit = {}
    ) : ProcessState
}

@Composable
fun rememberProcessState(): MutableState<ProcessState> {
    return remember { mutableStateOf<ProcessState>(ProcessState.Idle) }
}

/**
 * Dialog Notifikasi Status Proses Reusable
 * Mencegah double click, menampilkan animasi status, dan memberikan feedback jelas
 */
@Composable
fun ProcessNotificationDialog(
    state: ProcessState,
    onDismissRequest: () -> Unit = {}
) {
    if (state is ProcessState.Idle) return

    Dialog(
        onDismissRequest = {
            when (state) {
                is ProcessState.Processing -> {
                    // Cegah dismiss saat proses sedang berjalan untuk menjaga integritas data & cegah double-action
                }
                is ProcessState.Success -> {
                    state.onDismiss()
                    onDismissRequest()
                }
                is ProcessState.Error -> {
                    state.onDismiss()
                    onDismissRequest()
                }
                else -> onDismissRequest()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = state !is ProcessState.Processing,
            dismissOnClickOutside = state !is ProcessState.Processing
        )
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .testTag("dialog_process_notification")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    is ProcessState.Processing -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(FarmGreenPrimary.copy(alpha = 0.12f))
                        ) {
                            CircularProgressIndicator(
                                color = FarmGreenPrimary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(44.dp).testTag("indicator_processing")
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = state.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("txt_process_title")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("txt_process_message")
                        )

                        if (!state.step.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FarmGreenPrimary.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = "Tahapan",
                                        tint = FarmGreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = state.step,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = FarmGreenPrimary
                                    )
                                }
                            }
                        }
                    }

                    is ProcessState.Success -> {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + scaleIn()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Sukses",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(48.dp).testTag("icon_process_success")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = state.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("txt_success_title")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("txt_success_message")
                        )

                        if (!state.detail.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.detail,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = {
                                state.onDismiss()
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_process_success_ok")
                        ) {
                            Text("Selesai", fontWeight = FontWeight.Bold)
                        }
                    }

                    is ProcessState.Error -> {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + scaleIn()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Gagal",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp).testTag("icon_process_error")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = state.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("txt_error_title")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("txt_error_message")
                        )

                        if (!state.detail.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.detail,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = {
                                state.onDismiss()
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_process_error_ok")
                        ) {
                            Text("Tutup", fontWeight = FontWeight.Bold)
                        }
                    }

                    ProcessState.Idle -> {}
                }
            }
        }
    }
}
