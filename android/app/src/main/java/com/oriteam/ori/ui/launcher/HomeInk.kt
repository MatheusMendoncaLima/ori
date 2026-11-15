package com.oriteam.ori.ui.launcher

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.core.graphics.scale
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Over a picture brighter than this, the names and the clock are dark; otherwise light, as Android chooses. */
private const val LIGHT_PICTURE = 0.55f
private const val SAMPLE_SIDE = 8
private const val SHADOW_ALPHA = 0.45f
private const val SHADOW_BLUR = 6f
private val DarkInk = Color(0xFF14181C)

/**
 * The color of everything written on the home screen (the clock, the names, the dots), the same for all of it: light
 * on a dark wallpaper and dark on a light one, as on Android. On a picture, light writing gets a soft shadow so it
 * stays readable over any part of it.
 */
@Immutable
internal data class HomeInk(val color: Color, val shadow: Shadow?, val isOnPicture: Boolean)

/** The ink of the home screen, given to its texts; null outside it (the drawer and the panes use the theme). */
internal val LocalHomeInk = staticCompositionLocalOf<HomeInk?> { null }

@Composable
internal fun rememberHomeInk(wallpaper: LauncherWallpaper): HomeInk {
    val isLight = when (wallpaper) {
        LauncherWallpaper.System -> LocalSystemWallpaperLook.current.isLight
        is LauncherWallpaper.Photo -> rememberPictureIsLight(wallpaper.fileName)
        // The colors and gradients of ORI follow the theme: the theme's own writing color reads on them.
        else -> return HomeInk(MaterialTheme.colorScheme.onBackground, shadow = null, isOnPicture = false)
    }
    return if (isLight) {
        HomeInk(DarkInk, shadow = null, isOnPicture = true)
    } else {
        HomeInk(
            Color.White,
            Shadow(Color.Black.copy(alpha = SHADOW_ALPHA), Offset(0f, 1f), SHADOW_BLUR),
            isOnPicture = true
        )
    }
}

@Composable
private fun rememberPictureIsLight(fileName: String): Boolean {
    val load = LocalWallpaperLoader.current
    val isLight by produceState(initialValue = false, fileName) {
        val picture = load(fileName)
        value = picture != null && withContext(Dispatchers.Default) { averageLuminance(picture) > LIGHT_PICTURE }
    }
    return isLight
}

/** The mean brightness of a picture, from a tiny copy of it (64 points are enough for "light" or "dark"). */
internal fun averageLuminance(picture: Bitmap): Float {
    val small = picture.scale(SAMPLE_SIDE, SAMPLE_SIDE)
    val pixels = IntArray(SAMPLE_SIDE * SAMPLE_SIDE)
    small.getPixels(pixels, 0, SAMPLE_SIDE, 0, 0, SAMPLE_SIDE, SAMPLE_SIDE)
    return pixels.map { android.graphics.Color.luminance(it) }.average().toFloat()
}
