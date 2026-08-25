package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.paint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.FarmProfileEntity
import com.example.ui.FarmViewModel
import com.example.ui.theme.FarmGreenPrimary
import com.example.util.UserSessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmProfileScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val farmProfile by viewModel.farmProfile.collectAsState()
    val userSession = remember { UserSessionManager.getUserSession(context) }

    var farmName by remember(farmProfile) { mutableStateOf(farmProfile?.farmName ?: "SEJAHTERA BERSAMA") }
    var slogan by remember(farmProfile) { mutableStateOf(farmProfile?.slogan ?: "REZEKI LANCAR, USAHA MAKMUR") }
    var ownerName by remember(farmProfile) { mutableStateOf(farmProfile?.ownerName ?: "") }
    var address by remember(farmProfile) { mutableStateOf(farmProfile?.address ?: "") }
    var village by remember(farmProfile) { mutableStateOf(farmProfile?.village ?: "") }
    var district by remember(farmProfile) { mutableStateOf(farmProfile?.district ?: "") }
    var regency by remember(farmProfile) { mutableStateOf(farmProfile?.regency ?: "") }
    var province by remember(farmProfile) { mutableStateOf(farmProfile?.province ?: "") }
    var phone by remember(farmProfile) { mutableStateOf(farmProfile?.phoneNumber ?: userSession.phone.takeIf { it.isNotBlank() } ?: "") }
    var email by remember(farmProfile) { mutableStateOf(farmProfile?.email ?: userSession.email.takeIf { it.isNotBlank() } ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Usaha & Kop Laporan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_profile")) {
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
                .paint(painterResource(com.example.R.drawable.bg_dashboard), contentScale = ContentScale.Crop)
                .testTag("screen_farm_profile"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Official Logo with ContentScale.Fit
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_sejahtera_bersama),
                                contentDescription = "Logo Resmi SEJAHTERA BERSAMA",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("profile_logo_image")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(farmName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = FarmGreenPrimary)
                        Text(slogan, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFF57F17))
                        Text("Logo dan data ini akan tercetak otomatis pada Kop Surat PDF", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            item {
                Text(
                    text = "INFORMASI USAHA PETERNAKAN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = farmName,
                    onValueChange = { farmName = it },
                    label = { Text("Nama Peternakan / Usaha") },
                    modifier = Modifier.fillMaxWidth().testTag("input_profile_farm_name")
                )
            }

            item {
                OutlinedTextField(
                    value = slogan,
                    onValueChange = { slogan = it },
                    label = { Text("Slogan Peternakan") },
                    modifier = Modifier.fillMaxWidth().testTag("input_profile_slogan")
                )
            }

            item {
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Nama Pemilik / Kepala Kandang") },
                    modifier = Modifier.fillMaxWidth().testTag("input_profile_owner")
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("No. Telepon / WA") },
                        modifier = Modifier.weight(1f).testTag("input_profile_phone")
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
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
            }

            item {
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
            }

            item {
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        val profile = FarmProfileEntity(
                            id = farmProfile?.id ?: 1,
                            farmName = farmName,
                            slogan = slogan,
                            ownerName = ownerName,
                            address = address,
                            village = village,
                            district = district,
                            regency = regency,
                            province = province,
                            phoneNumber = phone,
                            email = email
                        )
                        viewModel.saveFarmProfile(
                            profile = profile,
                            onComplete = {
                                isSaving = false
                                Toast.makeText(context, "Profil peternakan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { message ->
                                isSaving = false
                                Toast.makeText(context, "Gagal menyimpan profil: $message", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_farm_profile")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaving) "MENYIMPAN..." else "SIMPAN PROFIL & KOP SURAT", fontWeight = FontWeight.Bold)
                }
            }

            // User Account & Session Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = FarmGreenPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Status Akun Peternak",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FarmGreenPrimary
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFC8E6C9)
                            ) {
                                Text(
                                    text = userSession.status + " ✓",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FarmGreenPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Login sebagai: ${userSession.userName} (${userSession.email})",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                        )
                        Text(
                            text = "WhatsApp: ${userSession.phone}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                        )

                        OutlinedButton(
                            onClick = {
                                UserSessionManager.clearSession(context)
                                Toast.makeText(context, "Anda telah keluar dari akun.", Toast.LENGTH_SHORT).show()
                                onLogout()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF5350)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("KELUAR / GANTI AKUN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
