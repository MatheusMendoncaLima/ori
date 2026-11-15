package com.oriteam.ori.data.launcher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import androidx.core.graphics.createBitmap
import com.oriteam.ori.domain.launcher.LockScreenWallpaper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OriLockWallpaper"

/** A gradient needs no detail: half the screen in each side is enough, and Android stretches it smoothly. */
private const val GRADIENT_SCALE = 2

/**
 * Sets the wallpaper of the lock screen through Android ([WallpaperManager.FLAG_LOCK], permission SET_WALLPAPER,
 * granted with the app). The picture is not marked for backup, so it does not go to the cloud with Android's backup.
 * Android numbers each picture it is given; asking the number of the lock screen's now needs no permission and shows
 * nothing of the picture, only whether it is still the one ORI gave.
 */
@Singleton
class AndroidLockScreenWallpaper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photos: DeviceWallpaperStore
) : LockScreenWallpaper {
    private val manager = WallpaperManager.getInstance(context)

    override fun isAvailable(): Boolean = manager.isWallpaperSupported && manager.isSetWallpaperAllowed

    override suspend fun setPhoto(fileName: String): Int? = photos.load(fileName)?.let { set(it) }

    override suspend fun setColors(topArgb: Int, bottomArgb: Int): Int? =
        set(withContext(Dispatchers.Default) { gradient(topArgb, bottomArgb) })

    override suspend fun shownId(): Int? = withContext(Dispatchers.IO) {
        try {
            manager.getWallpaperId(WallpaperManager.FLAG_LOCK)
        } catch (exception: SecurityException) {
            Log.w(TAG, "The lock screen wallpaper number was not given: ${exception.javaClass.simpleName}")
            null
        }
    }

    override suspend fun useHomeWallpaper(): Boolean = withContext(Dispatchers.IO) {
        try {
            manager.clear(WallpaperManager.FLAG_LOCK)
            true
        } catch (exception: IOException) {
            Log.w(TAG, "The lock screen wallpaper could not be cleared: ${exception.javaClass.simpleName}")
            false
        } catch (exception: SecurityException) {
            Log.w(TAG, "Clearing the lock screen wallpaper was refused: ${exception.javaClass.simpleName}")
            false
        }
    }

    /** The number Android gave the picture (it answers 0 when it could not set it); null when it failed. */
    private suspend fun set(picture: Bitmap): Int? = withContext(Dispatchers.IO) {
        try {
            manager.setBitmap(picture, null, false, WallpaperManager.FLAG_LOCK).takeIf { it > 0 }
        } catch (exception: IOException) {
            Log.w(TAG, "The lock screen wallpaper could not be set: ${exception.javaClass.simpleName}")
            null
        } catch (exception: SecurityException) {
            Log.w(TAG, "The lock screen wallpaper was refused: ${exception.javaClass.simpleName}")
            null
        }
    }

    private fun gradient(topArgb: Int, bottomArgb: Int): Bitmap {
        val metrics = context.resources.displayMetrics
        val width = (metrics.widthPixels / GRADIENT_SCALE).coerceAtLeast(1)
        val height = (metrics.heightPixels / GRADIENT_SCALE).coerceAtLeast(1)
        return createBitmap(width, height).apply {
            val paint = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, height.toFloat(), topArgb, bottomArgb, Shader.TileMode.CLAMP)
            }
            Canvas(this).drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }
}
