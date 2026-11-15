package com.oriteam.ori.ui.launcher

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.domain.launcher.WallpaperColor
import com.oriteam.ori.domain.launcher.WallpaperGradient
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriAccent
import com.oriteam.ori.ui.theme.OriAreaAccents
import com.oriteam.ori.ui.theme.areaAccent

/** Reads a background picture by its file name (off the main thread); provided by the activity. */
val LocalWallpaperLoader = staticCompositionLocalOf<suspend (String) -> Bitmap?> { { null } }

/** The soft color of a [WallpaperColor], the same family as the colors of ORI, in light and in dark. */
@Composable
internal fun wallpaperAccent(color: WallpaperColor): OriAccent {
    val words = LocalOriColors.current.words
    return when (color) {
        WallpaperColor.MINT -> words.action
        WallpaperColor.BLUE -> words.feeling
        WallpaperColor.LILAC -> areaAccent(OriAreaAccents.COMMUNICATION)
        WallpaperColor.YELLOW -> words.person
        WallpaperColor.PINK -> words.social
    }
}

/** The two soft colors of ORI a gradient goes between, top to bottom. */
@Composable
internal fun wallpaperGradientColors(gradient: WallpaperGradient): Pair<Color, Color> {
    val (top, bottom) = when (gradient) {
        WallpaperGradient.DAWN -> WallpaperColor.YELLOW to WallpaperColor.PINK
        WallpaperGradient.SEA -> WallpaperColor.BLUE to WallpaperColor.MINT
        WallpaperGradient.MEADOW -> WallpaperColor.MINT to WallpaperColor.YELLOW
        WallpaperGradient.DUSK -> WallpaperColor.LILAC to WallpaperColor.BLUE
        WallpaperGradient.SAND -> WallpaperColor.PINK to WallpaperColor.LILAC
    }
    return wallpaperAccent(top).container to wallpaperAccent(bottom).container
}

/** A gradient from one soft color of ORI to another, top to bottom. */
@Composable
internal fun wallpaperBrush(gradient: WallpaperGradient): Brush {
    val (top, bottom) = wallpaperGradientColors(gradient)
    return Brush.verticalGradient(listOf(top, bottom))
}

/**
 * The Android wallpaper itself, seen through the window: the window of ORI Home shows it behind everything, and this
 * clears a hole down to it, as a SurfaceView does. Android does not let an app read the picture (from Android 14, only
 * with access to all the files, which ORI does not ask), so this is the real picture, at its real size: the part of it
 * behind this place, not the whole picture made small. Only where nothing in between is drawn apart (a fading pane
 * shows what is under it until it is still).
 */
@Composable
internal fun AndroidWallpaperWindow(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().drawBehind { drawRect(Color.Black, blendMode = BlendMode.Clear) })
}

/**
 * The colors to draw [wallpaper] for the lock screen, as ARGB (top and bottom); null for a photo (Android gets the
 * photo itself) and for the Android wallpaper (which is Android's already).
 */
@Composable
internal fun lockScreenColors(wallpaper: LauncherWallpaper): Pair<Int, Int>? = when (wallpaper) {
    LauncherWallpaper.System, is LauncherWallpaper.Photo -> null
    LauncherWallpaper.Theme -> MaterialTheme.colorScheme.background.toArgb().let { it to it }
    is LauncherWallpaper.Solid -> wallpaperAccent(wallpaper.color).container.toArgb().let { it to it }
    is LauncherWallpaper.Gradient -> wallpaperGradientColors(wallpaper.gradient).let { (top, bottom) ->
        top.toArgb() to bottom.toArgb()
    }
}

/**
 * What is behind the icons, filling [modifier]. The Android wallpaper is drawn by Android behind the window (the
 * window of ORI Home shows it): here, nothing. To hide it ("Nenhum" of the levels of stimulus), the caller passes
 * the plain background of the theme ([wallpaperShown]), which covers it.
 */
@Composable
internal fun WallpaperBackground(wallpaper: LauncherWallpaper, modifier: Modifier = Modifier) {
    when (wallpaper) {
        LauncherWallpaper.System -> Box(modifier = modifier.fillMaxSize())
        LauncherWallpaper.Theme -> Box(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        )
        is LauncherWallpaper.Solid ->
            Box(modifier = modifier.fillMaxSize().background(wallpaperAccent(wallpaper.color).container))
        is LauncherWallpaper.Gradient -> Box(
            modifier = modifier.fillMaxSize().background(wallpaperBrush(wallpaper.gradient))
        )
        is LauncherWallpaper.Photo -> Box(modifier = modifier.fillMaxSize()) { PhotoWallpaper(wallpaper.fileName) }
    }
}

@Composable
private fun PhotoWallpaper(fileName: String) {
    val load = LocalWallpaperLoader.current
    val picture by produceState<Bitmap?>(initialValue = null, fileName) { value = load(fileName) }
    // A picture that is gone (deleted, not copied yet) leaves nothing: nothing breaks.
    val image = picture ?: return
    Image(
        bitmap = image.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}
