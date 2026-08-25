package com.example.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object WatermarkHelper {

    fun getPhotosDir(context: Context): File {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getPhotosDir(context)
        return File.createTempFile("CAPTURED_${timeStamp}_", ".jpg", storageDir)
    }

    fun getImageUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun applyWatermark(
        context: Context,
        sourceFile: File,
        farmName: String = "SEJAHTERA BERSAMA",
        coopName: String = "Kandang 01",
        cycleNumber: String = "Siklus 001",
        category: String = "BUKTI DOKUMENTASI",
        latitude: Double? = null,
        longitude: Double? = null,
        gpsAccuracy: Float? = null,
        customCaption: String = ""
    ): File {
        try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) return sourceFile

            // Check dimensions first to avoid OOM
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, boundsOptions)
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return sourceFile

            var inSampleSize = 1
            val maxDim = 1920
            while (origW / inSampleSize > maxDim || origH / inSampleSize > maxDim) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }

            var bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return sourceFile
            val mutableBitmap = if (bitmap.isMutable) bitmap else {
                val copy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                bitmap.recycle()
                copy
            }
            val canvas = Canvas(mutableBitmap)

            val width = mutableBitmap.width.toFloat()
            val height = mutableBitmap.height.toFloat()

            // Scale font sizes based on image resolution
            val scaleFactor = (width / 1080f).coerceIn(0.5f, 3.0f)

            val bannerHeight = 140f * scaleFactor
            val bannerRect = RectF(0f, height - bannerHeight, width, height)

            // Draw semi-transparent dark green banner
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E60D3814") // 90% opacity deep farm green
                style = Paint.Style.FILL
            }
            canvas.drawRect(bannerRect, bgPaint)

            // Top accent bar of banner
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4CAF50")
                strokeWidth = 3f * scaleFactor
            }
            canvas.drawLine(0f, height - bannerHeight, width, height - bannerHeight, linePaint)

            // Text formatting
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFD54F") // Amber gold
                textSize = 20f * scaleFactor
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 14f * scaleFactor
                typeface = Typeface.DEFAULT
            }

            val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E0E0E0")
                textSize = 12f * scaleFactor
                typeface = Typeface.DEFAULT
            }

            val curDate = SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale("id", "ID")).format(Date())
            val paddingLeft = 20f * scaleFactor
            var curY = height - bannerHeight + (28f * scaleFactor)

            // Line 1: Header + Category
            canvas.drawText("$farmName • $category", paddingLeft, curY, titlePaint)
            curY += 24f * scaleFactor

            // Line 2: Date, Time, Coop, Cycle
            canvas.drawText("Waktu: $curDate WIB | Kandang: $coopName ($cycleNumber)", paddingLeft, curY, textPaint)
            curY += 22f * scaleFactor

            // Line 3: GPS Info
            val gpsStr = if (latitude != null && longitude != null) {
                val acc = if (gpsAccuracy != null) " (Akurasi: ${String.format(Locale.US, "%.1f", gpsAccuracy)}m)" else ""
                "GPS: ${String.format(Locale.US, "%.5f", latitude)}, ${String.format(Locale.US, "%.5f", longitude)}$acc"
            } else {
                "GPS: Terverifikasi Lokasi Kandang"
            }
            canvas.drawText(gpsStr, paddingLeft, curY, subTextPaint)

            // Optional custom caption
            if (customCaption.isNotBlank()) {
                curY += 20f * scaleFactor
                canvas.drawText("Ket: ${customCaption.take(50)}", paddingLeft, curY, subTextPaint)
            }

            // Save watermarked image to a new file safely
            val watermarkedFile = File(getPhotosDir(context), "WM_${sourceFile.name}")
            FileOutputStream(watermarkedFile).use { out ->
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.flush()
            }
            mutableBitmap.recycle()

            return watermarkedFile
        } catch (e: Exception) {
            e.printStackTrace()
            return sourceFile
        }
    }

    fun loadThumbnail(file: File, maxDim: Int = 400): Bitmap? {
        return try {
            if (!file.exists() || file.length() == 0L) return null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return null

            var inSampleSize = 1
            while (origW / inSampleSize > maxDim || origH / inSampleSize > maxDim) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }
}
