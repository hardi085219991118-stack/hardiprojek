package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class UserSessionInfo(
    val userId: Long,
    val userName: String,
    val email: String,
    val phone: String,
    val businessName: String,
    val status: String,
    val isLoggedIn: Boolean
)

object SecurityHelper {

    fun generateSalt(length: Int = 16): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(length)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val saltBytes = ByteArray(salt.length / 2) { index ->
            salt.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, 120_000, 256)
        val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return derived.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val calculatedHash = hashPassword(password, salt)
        if (calculatedHash.equals(expectedHash, ignoreCase = true)) return true
        // Kompatibilitas satu kali untuk akun lama yang memakai SHA-256 sebelum pembaruan keamanan.
        if (expectedHash.length == 64) {
            val legacyInput = "$password:$salt"
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(legacyInput.toByteArray(Charsets.UTF_8))
            val legacyHash = digest.joinToString("") { "%02x".format(it) }
            return legacyHash.equals(expectedHash, ignoreCase = true)
        }
        return false
    }

    fun generateOtpCode(): String {
        val code = 100000 + SecureRandom().nextInt(900000)
        return code.toString()
    }


    fun openEmailVerification(context: Context, email: String, code: String) {
        val subject = Uri.encode("Kode Verifikasi SEJAHTERA BERSAMA")
        val body = Uri.encode("Kode verifikasi akun Anda: $code\n\nJangan berikan kode ini kepada orang lain.")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email?subject=$subject&body=$body")
        }
        context.startActivity(Intent.createChooser(intent, "Kirim kode melalui Email"))
    }

    fun openWhatsAppVerification(context: Context, phone: String, code: String) {
        val clean = phone.replace(Regex("[^0-9]"), "")
        val international = when {
            clean.startsWith("0") -> "62${clean.drop(1)}"
            clean.startsWith("62") -> clean
            clean.startsWith("8") -> "62$clean"
            else -> clean
        }
        val message = Uri.encode("Kode verifikasi SEJAHTERA BERSAMA: $code\n\nJangan berikan kode ini kepada orang lain.")
        val uri = Uri.parse("https://wa.me/$international?text=$message")
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Kirim kode melalui WhatsApp"))
    }

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    fun isValidPhone(phone: String): Boolean {
        val clean = phone.replace(Regex("[^0-9]"), "")
        return clean.length in 10..15 && (clean.startsWith("08") || clean.startsWith("628") || clean.startsWith("8"))
    }

    fun isStrongPassword(password: String): Pair<Boolean, String> {
        if (password.length < 6) {
            return false to "Password minimal 6 karakter"
        }
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            return false to "Password wajib mengandung kombinasi huruf dan angka"
        }
        return true to ""
    }
}

object VerificationDeliveryConfig {
    // Tujuan resmi pengiriman kode aktivasi OTO.
    const val ACTIVATION_WHATSAPP = "085219991118"
    const val ACTIVATION_EMAIL = "hardi.085219991118@gmail.com"
}

object UserSessionManager {
    private const val PREF_NAME = "sb_user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "current_user_id"
    private const val KEY_USER_NAME = "current_user_name"
    private const val KEY_USER_EMAIL = "current_user_email"
    private const val KEY_USER_PHONE = "current_user_phone"
    private const val KEY_BUSINESS_NAME = "current_business_name"
    private const val KEY_USER_STATUS = "current_user_status"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(
        context: Context,
        userId: Long,
        userName: String,
        email: String,
        phone: String,
        businessName: String,
        status: String
    ) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PHONE, phone)
            putString(KEY_BUSINESS_NAME, businessName)
            putString(KEY_USER_STATUS, status)
            apply()
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserSession(context: Context): UserSessionInfo {
        val prefs = getPrefs(context)
        return UserSessionInfo(
            userId = prefs.getLong(KEY_USER_ID, 0L),
            userName = prefs.getString(KEY_USER_NAME, "") ?: "",
            email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
            phone = prefs.getString(KEY_USER_PHONE, "") ?: "",
            businessName = prefs.getString(KEY_BUSINESS_NAME, "SEJAHTERA BERSAMA") ?: "SEJAHTERA BERSAMA",
            status = prefs.getString(KEY_USER_STATUS, "") ?: "",
            isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        )
    }

    fun getCurrentUserId(context: Context): Long = getUserSession(context).userId
    fun getCurrentUserName(context: Context): String = getUserSession(context).userName
    fun getCurrentUserEmail(context: Context): String = getUserSession(context).email
    fun getCurrentUserPhone(context: Context): String = getUserSession(context).phone
    fun getCurrentBusinessName(context: Context): String = getUserSession(context).businessName
    fun getCurrentUserStatus(context: Context): String = getUserSession(context).status

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
