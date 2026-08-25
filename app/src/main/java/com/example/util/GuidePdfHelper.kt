package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object GuidePdfHelper {
    private const val ASSET_NAME = "panduan_sejahtera_bersama.pdf"

    fun open(context: Context) {
        try {
            val dir = File(context.cacheDir, "guide")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, ASSET_NAME)
            if (!file.exists() || file.length() == 0L) {
                context.assets.open(ASSET_NAME).use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Buka Panduan SEJAHTERA BERSAMA"))
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Panduan PDF tidak dapat dibuka: ${e.localizedMessage ?: "periksa aplikasi pembaca PDF"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
