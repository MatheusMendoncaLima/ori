package com.oriteam.ori.ui.launcher

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.luminance

private const val TAG = "OriWallpaperColors"
private const val LIGHT = 0.5f
private const val DARK_MAX_LIGHTNESS = 0.2f
private const val LIGHT_MIN_LIGHTNESS = 0.75f
private const val HSL_PARTS = 3
private const val LIGHTNESS = 2

/**
 * What Android says about its wallpapers, without the pictures (Android does not let an app read them without a
 * special permission): whether the one of the home screen is light, and the main colors of the home and lock screen
 * ones, to draw a preview of them. Empty colors: unknown.
 */
@Immutable
data class SystemWallpaperLook(
    val isLight: Boolean = false,
    val homeColors: List<Color> = emptyList(),
    val lockColors: List<Color> = emptyList()
)

/** Given by the activity, which reads it off the main thread when ORI Home comes back and when the wallpaper opens. */
val LocalSystemWallpaperLook = staticCompositionLocalOf { SystemWallpaperLook() }

/** How bright Android says a wallpaper is: its main colors alone may not show it (a dark picture, a light accent). */
internal enum class WallpaperBrightness { LIGHT, DARK, UNKNOWN }

/**
 * Reads the look of the Android wallpapers (a call to Android: not on the main thread). Android 12 says directly
 * whether the wallpaper is light or dark; before, the main color tells. Unknown counts as dark for the writing. A lock
 * screen without its own wallpaper shows the home one, so it has its colors. The colors are brought to the brightness
 * of the picture ([toned]): a dark blue wallpaper whose main color is a light lilac is drawn dark, not light.
 */
internal fun readSystemWallpaperLook(context: Context): SystemWallpaperLook {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return SystemWallpaperLook()
    val manager = WallpaperManager.getInstance(context)
    // Some phones (One UI) answer only for one of the two when the same picture is on both: either serves both.
    val system = colorsOf(manager, WallpaperManager.FLAG_SYSTEM)
    val lockOwn = colorsOf(manager, WallpaperManager.FLAG_LOCK)
    val home = system ?: lockOwn ?: return SystemWallpaperLook()
    val lock = lockOwn ?: home
    val homeBrightness = brightnessOf(home)
    return SystemWallpaperLook(
        isLight = homeBrightness == WallpaperBrightness.LIGHT,
        homeColors = home.mainColors().toned(homeBrightness),
        lockColors = lock.mainColors().toned(brightnessOf(lock))
    )
}

/**
 * The colors with their own hue and the lightness of the picture: no lighter than [DARK_MAX_LIGHTNESS] for a dark
 * wallpaper, no darker than [LIGHT_MIN_LIGHTNESS] for a light one, as they are when Android does not say.
 */
internal fun List<Color>.toned(brightness: WallpaperBrightness): List<Color> = when (brightness) {
    WallpaperBrightness.UNKNOWN -> this
    WallpaperBrightness.DARK -> map { color -> color.withLightness { minOf(it, DARK_MAX_LIGHTNESS) } }
    WallpaperBrightness.LIGHT -> map { color -> color.withLightness { maxOf(it, LIGHT_MIN_LIGHTNESS) } }
}

private fun Color.withLightness(change: (Float) -> Float): Color {
    val hsl = FloatArray(HSL_PARTS)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[LIGHTNESS] = change(hsl[LIGHTNESS])
    return Color(ColorUtils.HSLToColor(hsl))
}

@RequiresApi(Build.VERSION_CODES.O_MR1)
private fun brightnessOf(colors: WallpaperColors): WallpaperBrightness {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        val isLight = colors.primaryColor.toArgb().luminance > LIGHT
        return if (isLight) WallpaperBrightness.LIGHT else WallpaperBrightness.DARK
    }
    val hints = colors.colorHints
    return when {
        hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0 -> WallpaperBrightness.LIGHT
        hints and WallpaperColors.HINT_SUPPORTS_DARK_THEME != 0 -> WallpaperBrightness.DARK
        else -> WallpaperBrightness.UNKNOWN
    }
}

@RequiresApi(Build.VERSION_CODES.O_MR1)
private fun colorsOf(manager: WallpaperManager, which: Int): WallpaperColors? = try {
    manager.getWallpaperColors(which)
} catch (exception: SecurityException) {
    Log.w(TAG, "Wallpaper colors are not available: ${exception.javaClass.simpleName}")
    null
}

@RequiresApi(Build.VERSION_CODES.O_MR1)
private fun WallpaperColors.mainColors(): List<Color> =
    listOfNotNull(primaryColor, secondaryColor, tertiaryColor).map { Color(it.toArgb()) }
