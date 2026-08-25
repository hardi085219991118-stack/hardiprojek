package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object PhotoStorageHelper {

    private const val TAG = "PhotoStorageHelper"
    private const val MAX_DIMENSION = 1920
    private const val JPEG_QUALITY = 86

    /**
     * Directory inside app internal storage: context.filesDir/photos/[category]
     * Persistent across app restarts, reboots, and offline.
     */
    fun getPhotosDir(context: Context, category: String = "umum"): File {
        val sanitizedCat = category.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]"), "_")
            .trim('_')
            .ifBlank { "umum" }
        val baseDir = File(context.filesDir, "photos")
        val targetDir = File(baseDir, sanitizedCat)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        return targetDir
    }

    /**
     * Create a uniquely named persistent photo file.
     * Pattern: [category]_[yyyyMMdd_HHmmss]_[uuid6].jpg
     */
    fun createUniquePhotoFile(context: Context, category: String = "umum"): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(6)
        val sanitizedCat = category.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]"), "_")
            .trim('_')
            .ifBlank { "bukti" }
        val fileName = "${sanitizedCat}_${timeStamp}_$randomSuffix.jpg"
        return File(getPhotosDir(context, sanitizedCat), fileName)
    }

    /**
     * Temporary file for camera captures before final processing.
     */
    fun createCameraTempFile(context: Context, category: String = "cam"): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(4)
        val sanitizedCat = category.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]"), "_")
            .trim('_')
            .ifBlank { "cam" }
        val tempDir = getPhotosDir(context, "temp")
        return File(tempDir, "TEMP_${sanitizedCat}_${timeStamp}_$randomSuffix.jpg")
    }

    /**
     * Generates a FileProvider Uri for camera intent.
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Processes photo taken with camera:
     * 1. Reads EXIF orientation from temporary file.
     * 2. Downsamples and decodes if image is very large (prevents OOM).
     * 3. Rotates bitmap so it is right-side up.
     * 4. Compresses to high-quality JPEG into permanent storage.
     * 5. Cleans up temporary camera file.
     * @return absolute path of the permanently saved photo file, or null if failed.
     */
    fun processCameraCapture(
        context: Context,
        tempFile: File,
        category: String = "kamera"
    ): String? {
        if (!tempFile.exists() || tempFile.length() == 0L) {
            Log.e(TAG, "processCameraCapture: Temp file does not exist or is empty")
            return null
        }

        try {
            // Read EXIF orientation
            val orientation = try {
                val exif = ExifInterface(tempFile.absolutePath)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val targetFile = createUniquePhotoFile(context, category)
            val success = decodeRotateAndSave(
                openInputStream = { tempFile.inputStream() },
                orientation = orientation,
                destinationFile = targetFile
            )

            // Safely delete temp file
            try {
                if (tempFile.exists()) tempFile.delete()
            } catch (_: Exception) {}

            return if (success && targetFile.exists() && targetFile.length() > 0L) {
                targetFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "processCameraCapture error: ${e.message}", e)
            return null
        }
    }

    /**
     * Processes photo selected from Gallery:
     * 1. Opens InputStream from content:// or file:// Uri.
     * 2. Reads EXIF orientation.
     * 3. Downsamples and decodes if image is large.
     * 4. Rotates bitmap to correct orientation.
     * 5. Compresses into permanent application storage.
     * @return absolute path of the permanently saved photo file, or null if failed.
     */
    fun processGallerySelection(
        context: Context,
        sourceUri: Uri,
        category: String = "galeri"
    ): String? {
        try {
            // Read EXIF orientation from URI stream
            val orientation = try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val exif = ExifInterface(input)
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val targetFile = createUniquePhotoFile(context, category)
            val success = decodeRotateAndSave(
                openInputStream = {
                    context.contentResolver.openInputStream(sourceUri)
                        ?: throw IllegalStateException("Tidak dapat membaca data gambar dari URI: $sourceUri")
                },
                orientation = orientation,
                destinationFile = targetFile
            )

            return if (success && targetFile.exists() && targetFile.length() > 0L) {
                targetFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "processGallerySelection error: ${e.message}", e)
            return null
        }
    }

    /**
     * Core decoding, orientation correction, resizing, and saving pipeline.
     */
    private fun decodeRotateAndSave(
        openInputStream: () -> InputStream,
        orientation: Int,
        destinationFile: File
    ): Boolean {
        var inputStream: InputStream? = null
        try {
            // 1. Check dimensions
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = openInputStream()
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream.close()

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                Log.e(TAG, "decodeRotateAndSave: Invalid image dimensions ($origWidth x $origHeight)")
                return false
            }

            // 2. Calculate inSampleSize for large images
            var inSampleSize = 1
            while (origWidth / inSampleSize > MAX_DIMENSION || origHeight / inSampleSize > MAX_DIMENSION) {
                inSampleSize *= 2
            }

            // 3. Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            inputStream = openInputStream()
            val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream.close()

            if (decodedBitmap == null) {
                Log.e(TAG, "decodeRotateAndSave: Failed to decode bitmap stream")
                return false
            }

            // 4. Handle EXIF Rotation Matrix
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            val finalBitmap = if (!matrix.isIdentity) {
                try {
                    val rotated = Bitmap.createBitmap(
                        decodedBitmap, 0, 0,
                        decodedBitmap.width, decodedBitmap.height,
                        matrix, true
                    )
                    if (rotated != decodedBitmap) {
                        decodedBitmap.recycle()
                    }
                    rotated
                } catch (oom: OutOfMemoryError) {
                    decodedBitmap
                }
            } else {
                decodedBitmap
            }

            // 5. Compress and write to destination
            FileOutputStream(destinationFile).use { outStream ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outStream)
                outStream.flush()
            }
            finalBitmap.recycle()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "decodeRotateAndSave error: ${e.message}", e)
            return false
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Safely loads a bitmap for preview/thumbnails without causing OutOfMemory errors.
     * Supports absolute file path, file:// URI, or content:// URI.
     */
    fun loadBitmapSafe(
        context: Context,
        pathOrUri: String?,
        maxDim: Int = 600
    ): Bitmap? {
        if (pathOrUri.isNullOrBlank()) return null

        try {
            val trimmed = pathOrUri.trim()

            // If it's a direct file path
            val file = File(trimmed)
            if (file.exists() && file.length() > 0L) {
                return loadBitmapFromFile(file, maxDim)
            }

            // If it's a content:// or file:// URI
            if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
                val uri = Uri.parse(trimmed)
                return loadBitmapFromUri(context, uri, maxDim)
            }

            // If it's relative path in filesDir
            val relativeFile = File(context.filesDir, trimmed)
            if (relativeFile.exists() && relativeFile.length() > 0L) {
                return loadBitmapFromFile(relativeFile, maxDim)
            }

            // If it's in photos subdirectory
            val photosFile = File(File(context.filesDir, "photos"), trimmed)
            if (photosFile.exists() && photosFile.length() > 0L) {
                return loadBitmapFromFile(photosFile, maxDim)
            }

            return null
        } catch (e: Exception) {
            Log.w(TAG, "loadBitmapSafe failed for '$pathOrUri': ${e.message}")
            return null
        }
    }

    private fun loadBitmapFromFile(file: File, maxDim: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val origW = bounds.outWidth
            val origH = bounds.outHeight
            if (origW <= 0 || origH <= 0) return null

            var inSampleSize = 1
            while (origW / inSampleSize > maxDim || origH / inSampleSize > maxDim) {
                inSampleSize *= 2
            }

            val opts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Efficient memory usage for UI
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
            val origW = bounds.outWidth
            val origH = bounds.outHeight
            if (origW <= 0 || origH <= 0) return null

            var inSampleSize = 1
            while (origW / inSampleSize > maxDim || origH / inSampleSize > maxDim) {
                inSampleSize *= 2
            }

            val opts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, opts)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Safely deletes a photo file from internal storage if no longer referenced.
     */
    fun deletePhotoFile(context: Context, path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            val file = File(path.trim())
            // Only allow deleting files inside our app's internal filesDir
            if (file.exists() && file.absolutePath.startsWith(context.filesDir.absolutePath)) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
