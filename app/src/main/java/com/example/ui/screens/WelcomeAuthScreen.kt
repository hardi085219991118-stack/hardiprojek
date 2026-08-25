package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun WelcomeAuthScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B5E20),
                        Color(0xFF2E7D32),
                        Color(0xFFF1F8E9)
                    ),
                    startY = 0f,
                    endY = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_sejahtera_bersama),
                        contentDescription = "Logo Sejahtera Bersama",
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SEJAHTERA BERSAMA",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = " « REZEKI LANCAR, USAHA MAKMUR » ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Aplikasi Manajemen Budidaya Ayam Broiler &\nLaporan Kemitraan Resmi",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFE8F5E9)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Value Cards Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Fitur Unggulan Peternakan Broiler",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    )

                    FeatureItem(
                        icon = Icons.Default.Security,
                        title = "Registrasi & Verifikasi Pengguna",
                        description = "Akses aman dengan akun peternak terverifikasi"
                    )

                    FeatureItem(
                        icon = Icons.Default.CameraAlt,
                        title = "Foto Bukti Otomatis Ber-Watermark",
                        description = "Dokumentasi kematian, pakan & kandang dengan GPS & waktu"
                    )

                    FeatureItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Laporan PDF Kemitraan & Harian",
                        description = "Cetak dan simpan 19 poin evaluasi panen standar kemitraan"
                    )

                    FeatureItem(
                        icon = Icons.Default.Share,
                        title = "Pengiriman Email & WhatsApp",
                        description = "Kirim laporan ke PIC kemitraan dan riwayat pengiriman"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DAFTAR AKUN BARU",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1B5E20)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1B5E20))
                ) {
                    Icon(imageVector = Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SUDAH PUNYA AKUN? MASUK",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sistem Offline-First • Data Tersimpan Aman di Perangkat",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFE8F5E9),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF555555)
                )
            )
        }
    }
}
