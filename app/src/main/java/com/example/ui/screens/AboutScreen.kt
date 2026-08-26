package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val officialPhone = "085219991118"

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
                .paint(painterResource(R.drawable.bg_dashboard), contentScale = ContentScale.Crop)
                .testTag("screen_about"),
            contentPadding = PaddingValues(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo & Header Card
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp)
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
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = FarmGreenPrimary,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Aplikasi Laporan Berbasis Android",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.DarkGray,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FarmGreenPrimary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Dibuat untuk Anggota SEJAHTERA BERSAMA",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FarmGreenPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Tujuan Aplikasi Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = FarmGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tujuan Aplikasi", fontWeight = FontWeight.Bold, color = FarmGreenPrimary, fontSize = 15.sp)
                        }
                        Text(
                            text = "Aplikasi ini dibangun untuk anggota SEJAHTERA BERSAMA untuk mempermudah pencatatan dan pelaporan berbasis Android.",
                            fontSize = 13.5.sp,
                            color = Color.DarkGray,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Informasi Pembuat Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = FarmGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Informasi Pembuat Aplikasi", fontWeight = FontWeight.Bold, color = FarmGreenPrimary, fontSize = 15.sp)
                        }

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Pembuat:", fontSize = 12.sp, color = Color.Gray)
                                Text("Hardi Mantangai", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tanggal Pembangunan:", fontSize = 12.sp, color = Color.Gray)
                                Text("Dibangun pada 20 Agustus 2026", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }

            // Kontak WhatsApp Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kontak Resmi", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 15.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("WhatsApp:", fontSize = 12.sp, color = Color.DarkGray)
                                Text(officialPhone, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1B5E20))
                            }
                        }

                        Button(
                            onClick = {
                                try {
                                    val formattedPhone = if (officialPhone.startsWith("0")) {
                                        "62" + officialPhone.substring(1)
                                    } else {
                                        officialPhone
                                    }
                                    val message = "Halo Bapak Hardi Mantangai, saya anggota SEJAHTERA BERSAMA ingin berkonsultasi mengenai aplikasi."
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp tidak tersedia di perangkat ini.", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_contact_whatsapp")
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HUBUNGI VIA WHATSAPP", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Action Tutorial
            item {
                OutlinedButton(
                    onClick = onOpenTutorial,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_open_tutorial"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmGreenPrimary)
                ) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = FarmGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TUTORIAL & PANDUAN PENGGUNAAN", fontWeight = FontWeight.Bold)
                }
            }

            // Footer Copyright
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "© 2026 SEJAHTERA BERSAMA\nSeluruh hak cipta dilindungi.",
                    fontSize = 11.5.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
