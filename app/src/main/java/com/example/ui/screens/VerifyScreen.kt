package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FarmProfileEntity
import com.example.data.local.entity.UserEntity
import com.example.util.SecurityHelper
import com.example.util.VerificationDeliveryConfig
import com.example.util.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(
    userId: Long,
    onNavigateBack: () -> Unit,
    onVerificationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).farmDao() }

    var user by remember { mutableStateOf<UserEntity?>(null) }
    var otpInput by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("EMAIL") } // EMAIL or WHATSAPP
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    // Load user data
    LaunchedEffect(userId) {
        withContext(Dispatchers.IO) {
            val u = dao.getUserById(userId)
            withContext(Dispatchers.Main) {
                user = u
            }
        }
    }

    // Countdown timer for resend
    LaunchedEffect(resendCooldown, canResend) {
        if (!canResend && resendCooldown > 0) {
            delay(1000L)
            resendCooldown--
            if (resendCooldown <= 0) {
                canResend = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Verifikasi Akun",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAF7))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Icon & Title
            Surface(
                shape = CircleShape,
                color = Color(0xFFE8F5E9),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                text = "Aktivasi & Verifikasi Akun",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            )

            Text(
                text = "Masukkan 6-digit kode verifikasi OTP yang telah dibuat untuk akun Anda.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF555555)),
                textAlign = TextAlign.Center
            )

            // User Info Card
            user?.let { currentUser ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status Akun:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentUser.status == "AKTIF") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = if (currentUser.status == "AKTIF") "AKTIF ✓" else "MENUNGGU VERIFIKASI",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentUser.status == "AKTIF") Color(0xFF2E7D32) else Color(0xFFE65100)
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Divider(color = Color(0xFFEEEEEE))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = currentUser.fullName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = currentUser.businessName, style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = currentUser.email, style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = currentUser.whatsappNumber, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Pengiriman kode verifikasi yang transparan. Aplikasi tidak menampilkan OTP di layar.
            user?.let { currentUser ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Kirim kode verifikasi",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                        )
                        Text(
                            text = "Kode aktivasi OTO disiapkan untuk tujuan resmi: WhatsApp ${VerificationDeliveryConfig.ACTIVATION_WHATSAPP} dan Email ${VerificationDeliveryConfig.ACTIVATION_EMAIL}. Android tetap meminta konfirmasi aplikasi pengirim; aplikasi tidak dapat mengirim pesan WhatsApp/Email secara diam-diam tanpa layanan server.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF444444))
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        SecurityHelper.openEmailVerification(context, VerificationDeliveryConfig.ACTIVATION_EMAIL, currentUser.emailVerificationCode)
                                    } catch (e: Exception) {
                                        errorMessage = "Aplikasi Email tidak tersedia di perangkat ini."
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Kirim Email")
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        SecurityHelper.openWhatsAppVerification(context, VerificationDeliveryConfig.ACTIVATION_WHATSAPP, currentUser.whatsappVerificationCode)
                                    } catch (e: Exception) {
                                        errorMessage = "WhatsApp tidak tersedia di perangkat ini."
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Kirim WA")
                            }
                        }
                    }
                }
            }

            // Verification Method Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { selectedMethod = "EMAIL"; errorMessage = null },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedMethod == "EMAIL") Color(0xFFE8F5E9) else Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (selectedMethod == "EMAIL") Color(0xFF1B5E20) else Color(0xFFCCCCCC)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = if (selectedMethod == "EMAIL") Color(0xFF1B5E20) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Via Email",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selectedMethod == "EMAIL") FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedMethod == "EMAIL") Color(0xFF1B5E20) else Color.Gray
                        )
                    )
                }

                OutlinedButton(
                    onClick = { selectedMethod = "WHATSAPP"; errorMessage = null },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedMethod == "WHATSAPP") Color(0xFFE8F5E9) else Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (selectedMethod == "WHATSAPP") Color(0xFF1B5E20) else Color(0xFFCCCCCC)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (selectedMethod == "WHATSAPP") Color(0xFF1B5E20) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Via WhatsApp",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selectedMethod == "WHATSAPP") FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedMethod == "WHATSAPP") Color(0xFF1B5E20) else Color.Gray
                        )
                    )
                }
            }

            // Error & Success feedback
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEBEE),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFC62828), fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (successMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = successMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // OTP Input Field
            OutlinedTextField(
                value = otpInput,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        otpInput = it
                        errorMessage = null
                    }
                },
                label = { Text("Kode OTP 6-Digit") },
                placeholder = { Text("123456") },
                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = Color(0xFF2E7D32)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Verify Button
            Button(
                onClick = {
                    val cleanOtp = otpInput.trim()
                    if (cleanOtp.length != 6) {
                        errorMessage = "Masukkan 6-digit kode OTP lengkap."
                        return@Button
                    }

                    val currentUser = user
                    if (currentUser == null) {
                        errorMessage = "Data pengguna tidak ditemukan."
                        return@Button
                    }

                    val targetOtp = if (selectedMethod == "EMAIL") currentUser.emailVerificationCode else currentUser.whatsappVerificationCode

                    if (cleanOtp != targetOtp && cleanOtp != currentUser.emailVerificationCode && cleanOtp != currentUser.whatsappVerificationCode) {
                        errorMessage = "Kode OTP tidak valid atau salah. Silakan periksa kembali."
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val updatedUser = currentUser.copy(
                                status = "AKTIF",
                                isEmailVerified = currentUser.isEmailVerified || selectedMethod == "EMAIL",
                                isWhatsappVerified = currentUser.isWhatsappVerified || selectedMethod == "WHATSAPP",
                                updatedAt = System.currentTimeMillis()
                            )
                            withContext(Dispatchers.IO) {
                                dao.updateUser(updatedUser)
                                // Also update farm profile owner & name to match user
                                val currentProfile = dao.getFarmProfileDirect(updatedUser.id)
                                val newProfile = (currentProfile ?: FarmProfileEntity(id = updatedUser.id, userId = updatedUser.id)).copy(
                                    userId = updatedUser.id,
                                    ownerName = updatedUser.fullName,
                                    farmName = updatedUser.businessName,
                                    email = updatedUser.email,
                                    phoneNumber = updatedUser.whatsappNumber
                                )
                                dao.saveFarmProfile(newProfile)
                            }

                            // Save active session
                            UserSessionManager.saveSession(
                                context = context,
                                userId = updatedUser.id,
                                userName = updatedUser.fullName,
                                email = updatedUser.email,
                                phone = updatedUser.whatsappNumber,
                                businessName = updatedUser.businessName,
                                status = "AKTIF"
                            )

                            isLoading = false
                            successMessage = "Verifikasi Berhasil! Akun Anda kini AKTIF."
                            Toast.makeText(context, "Akun Berhasil Diaktifkan!", Toast.LENGTH_LONG).show()
                            delay(800)
                            onVerificationSuccess()
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Gagal memverifikasi akun: ${e.localizedMessage}"
                        }
                    }
                },
                enabled = !isLoading && otpInput.length == 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VERIFIKASI & AKTIFKAN AKUN", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            // Resend OTP Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tidak menerima kode?",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
                TextButton(
                    onClick = {
                        val currentUser = user ?: return@TextButton
                        val newEmailOtp = SecurityHelper.generateOtpCode()
                        val newWaOtp = SecurityHelper.generateOtpCode()
                        val newExpiry = System.currentTimeMillis() + (10 * 60 * 1000)

                        coroutineScope.launch {
                            val refreshedUser = currentUser.copy(
                                emailVerificationCode = newEmailOtp,
                                whatsappVerificationCode = newWaOtp,
                                verificationCodeExpiry = newExpiry
                            )
                            withContext(Dispatchers.IO) {
                                dao.updateUser(refreshedUser)
                            }
                            user = refreshedUser
                            canResend = false
                            resendCooldown = 60
                            Toast.makeText(context, "Kode baru dibuat. Gunakan tombol Kirim Email atau Kirim WA untuk mengirimkannya.", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = canResend
                ) {
                    Text(
                        text = if (canResend) "Kirim Ulang Kode" else "Kirim Ulang ($resendCooldown s)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (canResend) Color(0xFF1B5E20) else Color.Gray
                        )
                    )
                }
            }
        }
    }
}
