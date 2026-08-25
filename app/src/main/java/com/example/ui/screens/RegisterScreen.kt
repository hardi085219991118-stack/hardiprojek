package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.entity.UserEntity
import com.example.util.SecurityHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVerify: (userId: Long) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).farmDao() }

    var fullName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isAgreedToTerms by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Daftar Akun Peternak",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AppRegistration,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Formulir Registrasi Pengguna",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        )
                        Text(
                            text = "Lengkapi data untuk mengamankan data kandang & kemitraan Anda.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF333333))
                        )
                    }
                }
            }

            // Error banner
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

            // Inputs
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; errorMessage = null },
                label = { Text("Nama Lengkap Peternak *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2E7D32)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it; errorMessage = null },
                label = { Text("Nama Usaha / Peternakan *") },
                placeholder = { Text("Contoh: Peternakan Barokah Broiler") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF2E7D32)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = whatsappNumber,
                onValueChange = { whatsappNumber = it; errorMessage = null },
                label = { Text("Nomor WhatsApp *") },
                placeholder = { Text("081234567890") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2E7D32)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("Alamat Email *") },
                placeholder = { Text("peternak@gmail.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF2E7D32)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password (Min. 6 Karakter Huruf & Angka) *") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2E7D32)) },
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = { Text("Konfirmasi Password *") },
                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFF2E7D32)) },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Terms Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isAgreedToTerms,
                    onCheckedChange = { isAgreedToTerms = it; errorMessage = null },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1B5E20))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Saya menyetujui syarat, ketentuan & kebijakan privasi penggunaan aplikasi Sejahtera Bersama.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF333333))
                )
            }

            // Submit Button
            Button(
                onClick = {
                    val cleanFullName = fullName.trim()
                    val cleanBusiness = businessName.trim()
                    val cleanPhone = whatsappNumber.trim()
                    val cleanEmail = email.trim()

                    if (cleanFullName.isBlank()) {
                        errorMessage = "Nama lengkap wajib diisi."
                        return@Button
                    }
                    if (cleanBusiness.isBlank()) {
                        errorMessage = "Nama usaha/peternakan wajib diisi."
                        return@Button
                    }
                    if (!SecurityHelper.isValidPhone(cleanPhone)) {
                        errorMessage = "Format nomor WhatsApp tidak valid (contoh: 081234567890)."
                        return@Button
                    }
                    if (!SecurityHelper.isValidEmail(cleanEmail)) {
                        errorMessage = "Format email tidak valid (contoh: user@gmail.com)."
                        return@Button
                    }
                    val (isPassValid, passMsg) = SecurityHelper.isStrongPassword(password)
                    if (!isPassValid) {
                        errorMessage = passMsg
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Konfirmasi password tidak cocok."
                        return@Button
                    }
                    if (!isAgreedToTerms) {
                        errorMessage = "Harap setujui syarat & ketentuan aplikasi."
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val existingByEmail = withContext(Dispatchers.IO) { dao.getUserByEmail(cleanEmail) }
                            if (existingByEmail != null) {
                                errorMessage = "Email $cleanEmail sudah terdaftar. Silakan gunakan email lain atau login."
                                isLoading = false
                                return@launch
                            }

                            val existingByPhone = withContext(Dispatchers.IO) { dao.getUserByWhatsapp(cleanPhone) }
                            if (existingByPhone != null) {
                                errorMessage = "Nomor WhatsApp $cleanPhone sudah terdaftar. Silakan gunakan nomor lain."
                                isLoading = false
                                return@launch
                            }

                            val salt = SecurityHelper.generateSalt()
                            val hash = SecurityHelper.hashPassword(password, salt)
                            // Satu kode aktivasi OTO untuk dua kanal tujuan resmi.
                            val activationOtp = SecurityHelper.generateOtpCode()
                            val otpEmail = activationOtp
                            val otpWa = activationOtp
                            val expiry = System.currentTimeMillis() + (10 * 60 * 1000) // 10 menit

                            val newUser = UserEntity(
                                fullName = cleanFullName,
                                businessName = cleanBusiness,
                                whatsappNumber = cleanPhone,
                                email = cleanEmail,
                                passwordHash = hash,
                                passwordSalt = salt,
                                status = "MENUNGGU_VERIFIKASI",
                                isEmailVerified = false,
                                isWhatsappVerified = false,
                                emailVerificationCode = otpEmail,
                                whatsappVerificationCode = otpWa,
                                verificationCodeExpiry = expiry
                            )

                            val newUserId = withContext(Dispatchers.IO) { dao.insertUser(newUser) }
                            isLoading = false
                            Toast.makeText(context, "Pendaftaran berhasil! Silakan verifikasi akun Anda.", Toast.LENGTH_LONG).show()
                            onNavigateToVerify(newUserId)
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Gagal mendaftar: ${e.localizedMessage}"
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
                    Icon(imageVector = Icons.Default.HowToReg, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DAFTAR SEKARANG", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            // Already have account
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sudah punya akun?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Masuk di sini",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    )
                }
            }
        }
    }
}
