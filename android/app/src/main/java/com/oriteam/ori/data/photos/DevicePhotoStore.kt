package com.oriteam.ori.data.photos

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.photos.CloudPhotoRepository
import com.oriteam.ori.domain.photos.MAX_PHOTO_BYTES
import com.oriteam.ori.domain.photos.PhotoImportError
import com.oriteam.ori.domain.photos.PhotoImportResult
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.domain.photos.StoredPhoto
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

private const val TAG = "OriPhotos"

/** The folder keeps the name it had when only symbols had photos: renaming it would lose the photos on devices. */
private const val PHOTO_DIRECTORY = "aac_photos"
private const val PHOTO_EXTENSION = ".jpg"

/** Largest side of a stored photo: sharp on a big tile, about 50 KB on disk. */
internal const val MAX_PHOTO_SIDE_PX = 512
private val JPEG_QUALITIES = listOf(85, 70, 55, 40)
private const val SHRINK_FACTOR = 0.75f

/**
 * Keeps photos (of symbols and of the profile) in the app's private storage. The original picture is
 * never kept: a reduced copy is written, so a 12 MP photo becomes a small file, within what the account
 * accepts.
 */
class DevicePhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloud: CloudPhotoRepository
) : PhotoStore {

    private val directory: File get() = File(context.filesDir, PHOTO_DIRECTORY)

    override suspend fun importPhoto(sourceUri: String): PhotoImportResult = withContext(Dispatchers.IO) {
        val uri = sourceUri.toUri()
        val bitmap = decodeReduced(uri) ?: return@withContext PhotoImportResult.Failed(PhotoImportError.UNREADABLE)
        val fileName = UUID.randomUUID().toString() + PHOTO_EXTENSION
        if (write(bitmap, fileName)) {
            PhotoImportResult.Imported(StoredPhoto(fileName))
        } else {
            PhotoImportResult.Failed(PhotoImportError.STORAGE_FAILED)
        }
    }

    override fun pathOf(photo: StoredPhoto): String? =
        fileOf(photo)?.takeIf { it.isFile }?.absolutePath

    override suspend fun delete(photo: StoredPhoto) {
        withContext(Dispatchers.IO) { fileOf(photo)?.delete() }
    }

    override suspend fun readBytes(photo: StoredPhoto): ByteArray? = withContext(Dispatchers.IO) {
        try {
            fileOf(photo)?.takeIf { it.isFile }?.readBytes()
        } catch (exception: IOException) {
            Log.w(TAG, "Photo could not be read: ${exception.javaClass.simpleName}")
            null
        }
    }

    override suspend fun saveBytes(photo: StoredPhoto, bytes: ByteArray): Boolean =
        withContext(Dispatchers.IO) { fileOf(photo)?.let { writeBytes(bytes, it.name) } == true }

    override suspend fun upload(ownerUid: String, photo: StoredPhoto): DataResult<WriteStatus> {
        if (ownerUid == LOCAL_OWNER_UID) return DataResult.Success(WriteStatus.SYNCED)
        val bytes = readBytes(photo) ?: return DataResult.Failure(DataError.NOT_FOUND)
        return cloud.upload(ownerUid, photo.fileName, bytes)
    }

    override suspend fun deleteEverywhere(ownerUid: String, photo: StoredPhoto) {
        delete(photo)
        if (ownerUid != LOCAL_OWNER_UID) cloud.delete(ownerUid, photo.fileName)
    }

    /** A photo that cannot be fetched keeps its placeholder, and is tried again the next time. */
    override suspend fun fetchMissing(ownerUid: String, photos: List<StoredPhoto>): Boolean {
        if (ownerUid == LOCAL_OWNER_UID) return false
        return photos.filter { pathOf(it) == null }.map { photo ->
            val bytes = (cloud.get(ownerUid, photo.fileName) as? DataResult.Success)?.value
            bytes != null && saveBytes(photo, bytes)
        }.any { it }
    }

    /** Names come from Firestore, so anything that is not a plain file name is refused. */
    private fun fileOf(photo: StoredPhoto): File? =
        photo.fileName.takeIf { SAFE_FILE_NAME.matches(it) }?.let { File(directory, it) }

    /**
     * Decodes at a reduced resolution, so a huge picture never fills the memory. A file that
     * cannot be opened or is not an image leaves the bounds empty and returns null.
     */
    private fun decodeReduced(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_PHOTO_SIDE_PX)
        }
        return open(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?.let { scaledToFit(it, MAX_PHOTO_SIDE_PX).rotated(rotationOf(uri)) }
    }

    private fun open(uri: Uri): InputStream? = try {
        context.contentResolver.openInputStream(uri)
    } catch (exception: IOException) {
        Log.w(TAG, "Photo could not be opened: ${exception.javaClass.simpleName}")
        null
    } catch (exception: SecurityException) {
        Log.w(TAG, "Photo access was refused: ${exception.javaClass.simpleName}")
        null
    }

    /** Camera photos are often stored sideways with a rotation tag; the copy is stored upright. */
    private fun rotationOf(uri: Uri): Int = try {
        open(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
    } catch (_: IOException) {
        0
    }

    private fun write(bitmap: Bitmap, fileName: String): Boolean = writeBytes(compressedWithinLimit(bitmap), fileName)

    /** Writes to a temporary file first, so a failure never leaves a broken photo behind. */
    private fun writeBytes(bytes: ByteArray, fileName: String): Boolean {
        val temporary = File(directory, "$fileName.tmp")
        val isWritten = try {
            (directory.isDirectory || directory.mkdirs()) &&
                temporary.outputStream().use { it.write(bytes) }.let { true } &&
                temporary.renameTo(File(directory, fileName))
        } catch (exception: IOException) {
            Log.w(TAG, "Photo could not be stored: ${exception.javaClass.simpleName}")
            false
        }
        if (!isWritten) temporary.delete()
        return isWritten
    }

    private companion object {
        val SAFE_FILE_NAME = Regex("^[A-Za-z0-9-]{1,64}\\.jpg$")
    }
}

/**
 * A busy picture may not fit at the first attempt: the quality goes down first, because it shows
 * less than the size, and then the picture gets smaller until it fits.
 */
internal fun compressedWithinLimit(bitmap: Bitmap, maxBytes: Int = MAX_PHOTO_BYTES): ByteArray {
    var current = bitmap
    while (true) {
        val smallest = JPEG_QUALITIES.asSequence().map { quality ->
            ByteArrayOutputStream().also { current.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
        }.firstOrNull { it.size <= maxBytes }
        if (smallest != null) return smallest
        current = current.scale(
            (current.width * SHRINK_FACTOR).toInt().coerceAtLeast(1),
            (current.height * SHRINK_FACTOR).toInt().coerceAtLeast(1)
        )
    }
}

/** Largest power of two that keeps both sides at least [maxSide] (what BitmapFactory supports). */
internal fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sampleSize = 1
    while (maxOf(width, height) / (sampleSize * 2) >= maxSide) sampleSize *= 2
    return sampleSize
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
