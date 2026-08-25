package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.paint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.FarmGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tentang Aplikasi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_about")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmGreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .paint(painterResource(com.example.R.drawable.bg_panduan), contentScale = ContentScale.Crop)
                .testTag("screen_about"),
            contentPadding = PaddingValues(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Official Logo
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_sejahtera_bersama),
                        contentDescription = "Logo Resmi SEJAHTERA BERSAMA",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("about_app_logo")
                    )
                }
            }

            item {
                Text(
                    text = "SEJAHTERA BERSAMA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = FarmGreenPrimary,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Aplikasi Manajemen Budidaya Ayam Broiler & Laporan Kemitraan",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "« REZEKI LANCAR, USAHA MAKMUR »",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFFF57F17),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Versi 1.0.0 (Production Release)",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            }

            item {
                Button(
                    onClick = onOpenTutorial,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_open_tutorial"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("TUTORIAL & PANDUAN PENGGUNAAN", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Fitur Utama Aplikasi:", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)

                        FeatureItem("🐔 Manajemen Multi-Kandang & Multi-Siklus", "Mendukung pengelolaan banyak kandang dengan kapasitas, tipe closed/open house, dan riwayat siklus terpisah.")
                        FeatureItem("📍 Integrasi GPS & Lokasi Kandang", "Pengambilan koordinat akurat GPS secara otomatis dan integrasi langsung ke Google Maps.")
                        FeatureItem("📝 Laporan Harian & Pencatatan Operasional", "Catat populasi, mortalitas, konsumsi pakan, air minum, suhu, kelembaban, serta obat/vaksin harian.")
                        FeatureItem("🌾 Manajemen Pakan & Gudang", "Tracking penerimaan DO pakan, pencatatan sak masuk/keluar, dan monitoring stok minimum.")
                        FeatureItem("⚖️ Monitoring Bobot & ADG", "Perhitungan Average Daily Gain (ADG) otomatis dan grafik perkembangan bobot harian.")
                        FeatureItem("📄 Generator Dokumen PDF Resmi Standar Kemitraan", "Cetak & bagikan Laporan Harian, Laporan Kemitraan, dan Laporan Akhir Siklus lengkap dengan kop surat resmi dan tanda tangan.")
                        FeatureItem("💾 Backup & Ekspor CSV/JSON", "Penyimpanan data lokal Room DB yang aman dengan fitur ekspor ke Excel CSV & JSON.")
                    }
                }
            }

            item {
                Text(
                    text = "© 2026 SEJAHTERA BERSAMA Farm Management. Seluruh hak cipta dilindungi.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
fun FeatureItem(title: String, desc: String) {
    Column {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black)
        Text(desc, fontSize = 11.5.sp, color = Color.DarkGray)
    }
}
