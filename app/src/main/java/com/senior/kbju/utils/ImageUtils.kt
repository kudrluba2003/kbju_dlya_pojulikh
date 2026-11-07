package com.senior.kbju.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import kotlin.math.max

object ImageUtils {

    /**
     * Безопасно загружает изображение по Uri, сохраняя высокое разрешение для OCR
     * (до 2800px по длинной стороне) и корректирует поворот по EXIF.
     */
    fun loadRotatedScaledBitmap(context: Context, uri: Uri, maxDimension: Int = 2800): Bitmap? {
        return try {
            // 1. Читаем размеры без загрузки пикселей
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            // 2. Считаем inSampleSize
            var sampleSize = 1
            val largest = max(options.outWidth, options.outHeight)
            while (largest / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            // 3. Декодируем битмап
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 4. Проверяем EXIF ориентацию и поворачиваем
            val degrees = getRotationDegrees(context, uri)
            if (degrees != 0) {
                val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun getRotationDegrees(context: Context, uri: Uri): Int {
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream)
            } ?: return 0

            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Throwable) {
            0
        }
    }
}
