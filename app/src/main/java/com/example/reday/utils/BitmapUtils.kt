package com.example.reday.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

fun loadBitmapWithCorrectOrientation(path: String): Bitmap? {
    if (path.startsWith("http")) {
        return try {
            val bytes = java.net.URL(path).openStream().use { it.readBytes() }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }
    val bitmap = BitmapFactory.decodeFile(path) ?: return null
    val exif = try { ExifInterface(path) } catch (e: Exception) { return bitmap }
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
    )
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
