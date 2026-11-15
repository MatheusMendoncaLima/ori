package com.oriteam.ori.data.photos

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import java.io.IOException
import java.io.InputStream

private const val TAG = "OriPictures"

/**
 * Reads a picture the person chose (a symbol, the profile, the ORI Home background) at a reduced resolution, upright,
 * with its largest side at most [maxSide]: a 12 MP photo never fills the memory. A file that cannot be opened or
 * is not an image returns null. Call it off the main thread.
 */
internal fun ContentResolver.readReducedPicture(uri: Uri, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)
    }
    return open(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?.let { scaledToFit(it, maxSide).rotated(rotationOf(uri)) }
}

/** Largest power of two that keeps both sides at least [maxSide] (what BitmapFactory supports). */
internal fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sampleSize = 1
    while (maxOf(width, height) / (sampleSize * 2) >= maxSide) sampleSize *= 2
    return sampleSize
}

private fun ContentResolver.open(uri: Uri): InputStream? = try {
    openInputStream(uri)
} catch (exception: IOException) {
    Log.w(TAG, "Picture could not be opened: ${exception.javaClass.simpleName}")
    null
} catch (exception: SecurityException) {
    Log.w(TAG, "Picture access was refused: ${exception.javaClass.simpleName}")
    null
}

/** Camera photos are often stored sideways with a rotation tag; the copy is stored upright. */
private fun ContentResolver.rotationOf(uri: Uri): Int = try {
    open(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
} catch (_: IOException) {
    0
}

private fun scaledToFit(bitmap: Bitmap, maxSide: Int): Bitmap {
    val largest = maxOf(bitmap.width, bitmap.height)
    if (largest <= maxSide) return bitmap
    val scale = maxSide.toFloat() / largest
    return bitmap.scale(
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1)
    )
}

private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

// The platform ExifInterface is reliable from API 25 and minSdk is 26, so the AndroidX copy
// (an extra dependency) is not needed.
@get:SuppressLint("ExifInterface")
private val ExifInterface.rotationDegrees: Int
    get() = when (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
