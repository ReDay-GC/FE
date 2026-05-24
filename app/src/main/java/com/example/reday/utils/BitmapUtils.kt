package com.example.reday.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

fun loadBitmapWithCorrectOrientation(path: String): Bitmap? {
    val bitmap: Bitmap
    val exif: ExifInterface?

    if (path.startsWith("http")) {
        return try {
            val bytes = java.net.URL(path).openStream().use { it.readBytes() }
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            exif = try { ExifInterface(bytes.inputStream()) } catch (e: Exception) { null }
            applyExifRotation(bitmap, exif)
        } catch (e: Exception) { null }
    }

    bitmap = BitmapFactory.decodeFile(path) ?: return null
    exif = try { ExifInterface(path) } catch (e: Exception) { null }
    return applyExifRotation(bitmap, exif)
}

private fun applyExifRotation(bitmap: Bitmap, exif: ExifInterface?): Bitmap {
    val orientation = exif?.getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
    ) ?: ExifInterface.ORIENTATION_NORMAL
    val rotation = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (rotation == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
