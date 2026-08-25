package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogExportHelper {

    fun getDiagnosticsDir(context: Context): File {
        val externalDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val dir = if (externalDocs != null) {
            File(externalDocs, "SEJAHTERA_BERSAMA_DIAGNOSTICS")
        } else {
            File(context.filesDir, "diagnostics")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createLogFile(context: Context, logContent: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "LOG_DIAGNOSTIK_$timeStamp.txt"
        val dir = getDiagnosticsDir(context)
        val file = File(dir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(logContent.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    fun getUriForFileSafely(context: Context, file: File): Uri? {
        return try {
            if (!file.exists()) return null
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: IllegalArgumentException) {
            // Fallback: Copy file to internal cache directory which is always covered by FileProvider
            try {
                val cacheFallback = File(context.cacheDir, file.name)
                file.copyTo(cacheFallback, overwrite = true)
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheFallback
                )
            } catch (ex: Exception) {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun shareLogFile(context: Context, file: File, title: String = "Bagikan Log Diagnostik") {
        if (!file.exists()) {
            Toast.makeText(context, "File log tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = getUriForFileSafely(context, file)
        if (uri == null) {
            Toast.makeText(context, "Gagal menyiapkan berkas untuk dibagikan", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Log Diagnostik - SEJAHTERA BERSAMA")
                putExtra(Intent.EXTRA_TEXT, "Terlampir file log diagnostik aplikasi SEJAHTERA BERSAMA.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan log: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
