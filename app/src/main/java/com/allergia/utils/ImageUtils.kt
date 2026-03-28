package com.allergia.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {

    private const val MAX_DIMENSION = 1280    // px — ~720p, баланс качество/размер архива
    private const val JPEG_QUALITY   = 80     // % — выше для архивных снимков
    private const val MAX_BASE64_MB  = 3.5    // OpenRouter limit

    /** Создаёт временный файл для снимка камеры */
    fun createTempPhotoFile(context: Context): File {
        val dir = File(context.filesDir, "food_photos").also { it.mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "food_$stamp.jpg")
    }

    /**
     * Читает URI (камера / галерея), сжимает, исправляет ориентацию EXIF,
     * сохраняет в приватное хранилище и возвращает путь к сохранённому файлу.
     */
    fun compressAndSave(context: Context, sourceUri: Uri): File {
        val bitmap = decodeSampledBitmap(context, sourceUri)
        val rotated = correctOrientation(context, sourceUri, bitmap)

        val outDir = File(context.filesDir, "food_photos").also { it.mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outFile = File(outDir, "food_$stamp.jpg")

        FileOutputStream(outFile).use { fos ->
            rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
        }
        rotated.recycle()
        return outFile
    }

    /** Конвертирует файл в base64-строку для отправки в OpenRouter Vision API */
    fun fileToBase64(file: File): String {
        val bos = ByteArrayOutputStream()
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        // Дополнительное сжатие если файл всё равно большой
        var quality = JPEG_QUALITY
        do {
            bos.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            quality -= 10
        } while (bos.size() > (MAX_BASE64_MB * 1024 * 1024) && quality > 20)

        bitmap.recycle()
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    }

    /** Конвертирует URI напрямую в base64 (без промежуточного файла) */
    fun uriToBase64(context: Context, uri: Uri): String {
        val bitmap = decodeSampledBitmap(context, uri)
        val rotated = correctOrientation(context, uri, bitmap)

        val bos = ByteArrayOutputStream()
        var quality = JPEG_QUALITY
        do {
            bos.reset()
            rotated.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            quality -= 10
        } while (bos.size() > (MAX_BASE64_MB * 1024 * 1024) && quality > 20)

        rotated.recycle()
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Deletes food photos older than [keepDays] days from the photos directory.
     * Call this after saving a new photo to keep storage usage bounded.
     */
    fun pruneOldPhotos(context: Context, keepDays: Int = 7) {
        val dir = File(context.filesDir, "food_photos")
        if (!dir.exists()) return
        val cutoff = System.currentTimeMillis() - keepDays * 24L * 60 * 60 * 1000
        dir.listFiles()
            ?.filter { it.isFile && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    /** Returns all saved food photo files sorted newest-first. */
    fun listFoodPhotos(context: Context): List<File> {
        val dir = File(context.filesDir, "food_photos")
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".jpg") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }

        opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight)
        opts.inJustDecodeBounds = false

        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open URI: $uri")
        return stream.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalArgumentException("Failed to decode bitmap from URI: $uri")
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            val halfW = width / 2
            val halfH = height / 2
            while (halfW / sampleSize >= MAX_DIMENSION && halfH / sampleSize >= MAX_DIMENSION) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val exifOrientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val rotation = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it != bitmap) bitmap.recycle() }
    }
}
