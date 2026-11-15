package com.oriteam.ori.data.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.core.net.toUri
import com.oriteam.ori.data.photos.readReducedPicture
import com.oriteam.ori.domain.launcher.MAX_WALLPAPER_HISTORY
import com.oriteam.ori.domain.launcher.WallpaperImport
import com.oriteam.ori.domain.launcher.WallpaperStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OriWallpaper"
private const val WALLPAPER_DIRECTORY = "launcher_wallpaper"

/** Sharp on a phone or a tablet screen, and a few hundred KB on disk instead of the original megabytes. */
internal const val MAX_WALLPAPER_SIDE_PX = 1920
private const val WALLPAPER_QUALITY = 85
private const val THUMBNAIL_SAMPLE = 8
private val SAFE_WALLPAPER_NAME = Regex("^wallpaper-[A-Za-z0-9-]{1,64}\\.jpg$")

/**
 * The background pictures of ORI Home, in the app's private storage. The original picture is never kept: a reduced,
 * upright copy is written, read with the system photo picker's one-time access (no storage permission). Nothing of
 * it leaves the device, and it is deleted when another background is chosen.
 */
@Singleton
class DeviceWallpaperStore @Inject constructor(
    @ApplicationContext private val context: Context
) : WallpaperStore {
    private val directory: File get() = File(context.filesDir, WALLPAPER_DIRECTORY)

    /** The background shown, kept so going back to the home screen does not decode it again. */
    @Volatile
    private var loaded: Pair<String, Bitmap>? = null

    override suspend fun importPhoto(sourceUri: String): WallpaperImport = withContext(Dispatchers.IO) {
        val bitmap = context.contentResolver.readReducedPicture(sourceUri.toUri(), MAX_WALLPAPER_SIDE_PX)
            ?: return@withContext WallpaperImport.Failed
        val fileName = "wallpaper-${UUID.randomUUID()}.jpg"
        if (write(bitmap, fileName)) WallpaperImport.Imported(fileName) else WallpaperImport.Failed
    }

    override suspend fun delete(fileName: String) {
        withContext(Dispatchers.IO) {
            if (loaded?.first == fileName) loaded = null
            thumbnails.remove(fileName)
            fileOf(fileName)?.delete()
        }
    }

    /** Small copies for the thumbnails of the history: a few hundred KB in all, instead of a full picture each. */
    private val thumbnails = LruCache<String, Bitmap>(MAX_WALLPAPER_HISTORY)

    /** A small copy (an eighth of each side) of a background photo, for its thumbnail; null when it is gone. */
    suspend fun thumbnail(fileName: String): Bitmap? = thumbnails.get(fileName) ?: withContext(Dispatchers.IO) {
        fileOf(fileName)?.takeIf { it.isFile }
            ?.let { file -> readOrNull(file) }
            ?.let { bytes ->
                val options = BitmapFactory.Options().apply { inSampleSize = THUMBNAIL_SAMPLE }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
            ?.also { thumbnails.put(fileName, it) }
    }

    /** The picture to draw, read off the main thread; null when the file is gone (the theme background shows). */
    suspend fun load(fileName: String): Bitmap? = loaded?.takeIf { it.first == fileName }?.second
        ?: withContext(Dispatchers.IO) {
            fileOf(fileName)?.takeIf { it.isFile }
                // Read whole, then decoded from memory: the file is never held open while the picture is used.
                ?.let { file -> readOrNull(file) }
                ?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                ?.also { loaded = fileName to it }
        }

    private fun readOrNull(file: File): ByteArray? = try {
        file.readBytes()
    } catch (exception: IOException) {
        Log.w(TAG, "Wallpaper could not be read: ${exception.javaClass.simpleName}")
        null
    }

    /** Writes to a temporary file first, so a failure never leaves a broken picture behind. */
    private fun write(bitmap: Bitmap, fileName: String): Boolean {
        val temporary = File(directory, "$fileName.tmp")
        val isWritten = try {
            (directory.isDirectory || directory.mkdirs()) &&
                temporary.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, WALLPAPER_QUALITY, it) } &&
                temporary.renameTo(File(directory, fileName))
        } catch (exception: IOException) {
            Log.w(TAG, "Wallpaper could not be stored: ${exception.javaClass.simpleName}")
            false
        }
        if (!isWritten) temporary.delete()
        return isWritten
    }

    /** Only names this store made are accepted, so a stored value can never point outside the folder. */
    private fun fileOf(fileName: String): File? =
        fileName.takeIf { SAFE_WALLPAPER_NAME.matches(it) }?.let { File(directory, it) }
}
