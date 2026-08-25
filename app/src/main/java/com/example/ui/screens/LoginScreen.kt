package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entity.UserEntity
import com.example.util.SecurityHelper
import com.example.util.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToVerify: (userId: Long) -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).farmDao() }

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Forgot Password Dialog State
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotEmailOrPhone by remember { mutableStateOf("") }
    var forgotStep by remember { mutableIntStateOf(1) } // 1: identify, 2: otp + new pass
    var targetForgotUser by remember { mutableStateOf<UserEntity?>(null) }
    var resetOtpInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmNewPasswordInput by remember { mutableStateOf("") }
    var forgotError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Masuk ke Akun",
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
            // Header Logo & Branding
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_sejahtera_bersama),
                    contentDescription = "Logo",
                    modifier = Modifier.padding(10.dp)
                )
            }

            Text(
                text = "Selamat Datang Kembali",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            )

            Text(
                text = "Masuk untuk mengelola siklus panen & laporan kemitraan",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                textAlign = TextAlign.Center
            )

            // Error Display
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEBEE),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it; errorMessage = null },
                label = { Text("Email atau Nomor WhatsApp") },
                placeholder = { Text("contoh: peternak@gmail.com / 0812...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Sembunyikan" else "Tampilkan"
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Forgot Password Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    forgotEmailOrPhone = identifier
                    forgotStep = 1
                    forgotError = null
                    showForgotPasswordDialog = true
                }) {
                    Text(
                        text = "Lupa Password?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    )
                }
            }

            // Login Button
            Button(
                onClick = {
                    val cleanId = identifier.trim()
                    val cleanPass = password.trim()

                    if (cleanId.isBlank()) {
                        errorMessage = "Masukkan email atau nomor WhatsApp."
                        return@Button
                    }
                    if (cleanPass.isBlank()) {
                        errorMessage = "Masukkan password."
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val user = withContext(Dispatchers.IO) {
                                dao.getUserByIdentifier(cleanId)
                            }

                            if (user == null) {
                                errorMessage = "Akun tidak ditemukan. Periksa email/WhatsApp Anda atau daftar baru."
                                isLoading = false
                                return@launch
                            }

                            // Verify password hash
                            val isPasswordCorrect = SecurityHelper.verifyPassword(
                                password = cleanPass,
                                salt = user.passwordSalt,
                                expectedHash = user.passwordHash
                            )

                            if (!isPasswordCorrect) {
                                errorMessage = "Password salah. Silakan coba lagi."
                                isLoading = false
                                return@launch
                            }

                            // Check status
                            if (user.status == "MENUNGGU_VERIFIKASI" || (!user.isEmailVerified && !user.isWhatsappVerified)) {
                                isLoading = false
                                Toast.makeText(context, "Akun Anda belum aktif. Mengalihkan ke halaman verifikasi...", Toast.LENGTH_LONG).show()
                                onNavigateToVerify(user.id)
                                return@launch
                            }

                            if (user.status == "DITANGGUHKAN" || user.status == "NONAKTIF") {
                                errorMessage = "Akun Anda berstatus '${user.status}'. Silakan hubungi admin kemitraan."
                                isLoading = false
                                return@launch
                            }

                            // Save session
                            UserSessionManager.saveSession(
                                context = context,
                                userId = user.id,
                                userName = user.fullName,
                                email = user.email,
                                phone = user.whatsappNumber,
                                businessName = user.businessName,
                                status = user.status
                            )

                            // Update last active
                            withContext(Dispatchers.IO) {
                                dao.updateUser(user.copy(updatedAt = System.currentTimeMillis()))
                            }

                            isLoading = false
                            Toast.makeText(context, "Selamat datang, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Terjadi kesalahan saat masuk: ${e.localizedMessage}"
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(imageVector = Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MASUK", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Register Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Belum memiliki akun?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = "Daftar Sekarang",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    )
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text(
                    text = if (forgotStep == 1) "Reset Password" else "Buat Password Baru",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (forgotError != null) {
                        Text(
                            text = forgotError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Red, fontWeight = FontWeight.Bold)
                        )
                    }

                    if (forgotStep == 1) {
                        Text(
                            text = "Masukkan alamat email atau nomor WhatsApp terdaftar Anda untuk menerima kode verifikasi OTP reset.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = forgotEmailOrPhone,
                            onValueChange = { forgotEmailOrPhone = it; forgotError = null },
                            label = { Text("Email atau Nomor WhatsApp") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        targetForgotUser?.let { u ->
                            Text(
                                "Kode reset tidak ditampilkan. Kirim kode melalui salah satu pilihan berikut:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = {
                                    try { SecurityHelper.openEmailVerification(context, u.email, u.emailVerificationCode) }
                                    catch (_: Exception) { forgotError = "Aplikasi Email tidak tersedia." }
                                }, modifier = Modifier.weight(1f)) { Text("Kirim Email") }
                                OutlinedButton(onClick = {
                                    try { SecurityHelper.openWhatsAppVerification(context, u.whatsappNumber, u.whatsappVerificationCode) }
                                    catch (_: Exception) { forgotError = "WhatsApp tidak tersedia." }
                                }, modifier = Modifier.weight(1f)) { Text("Kirim WA") }
                            }
                        }

                        OutlinedTextField(
                            value = resetOtpInput,
                            onValueChange = { resetOtpInput = it; forgotError = null },
                            label = { Text("Kode OTP 6-Digit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newPasswordInput,
                            onValueChange = { newPasswordInput = it; forgotError = null },
                            label = { Text("Password Baru (Min 6 Karakter)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = confirmNewPasswordInput,
                            onValueChange = { confirmNewPasswordInput = it; forgotError = null },
                            label = { Text("Konfirmasi Password Baru") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotStep == 1) {
                            val cleanInput = forgotEmailOrPhone.trim()
                            if (cleanInput.isBlank()) {
                                forgotError = "Masukkan email atau nomor WhatsApp."
                                return@Button
                            }
                            coroutineScope.launch {
                                val userFound = withContext(Dispatchers.IO) {
                                    dao.getUserByIdentifier(cleanInput)
                                }
                                if (userFound == null) {
                                    forgotError = "Akun dengan email/nomor tersebut tidak ditemukan."
                                } else {
                                    val newOtp = SecurityHelper.generateOtpCode()
                                    val updated = userFound.copy(
                                        emailVerificationCode = newOtp,
                                        whatsappVerificationCode = newOtp,
                                        verificationCodeExpiry = System.currentTimeMillis() + (10 * 60 * 1000)
                                    )
                                    withContext(Dispatchers.IO) { dao.updateUser(updated) }
                                    targetForgotUser = updated
                                    forgotStep = 2
                                    forgotError = null
                                }
                            }
                        } else {
                            val cleanOtp = resetOtpInput.trim()
                            val u = targetForgotUser ?: return@Button
                            if (cleanOtp != u.emailVerificationCode && cleanOtp != u.whatsappVerificationCode) {
                                forgotError = "Kode OTP tidak valid."
                                return@Button
                            }
                            val (isValid, msg) = SecurityHelper.isStrongPassword(newPasswordInput)
                            if (!isValid) {
                                forgotError = msg
                                return@Button
                            }
                            if (newPasswordInput != confirmNewPasswordInput) {
                                forgotError = "Konfirmasi password baru tidak cocok."
                                return@Button
                            }

                            coroutineScope.launch {
                                val salt = SecurityHelper.generateSalt()
                                val hash = SecurityHelper.hashPassword(newPasswordInput, salt)
                                val updated = u.copy(
                                    passwordHash = hash,
                                    passwordSalt = salt,
                                    updatedAt = System.currentTimeMillis()
                                )
                                withContext(Dispatchers.IO) { dao.updateUser(updated) }
                                showForgotPasswordDialog = false
                                Toast.makeText(context, "Password berhasil diubah! Silakan login kembali.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text(if (forgotStep == 1) "Lanjutkan" else "Simpan Password Baru")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
