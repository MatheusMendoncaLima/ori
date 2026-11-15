package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

/** The two small screens of "Como está agora" are at most this tall, in the shape of this screen. */
private val PreviewMaxHeight = 300.dp
private const val UPRIGHT_RATIO = 0.5f
private val ScreenPreviewShape = RoundedCornerShape(16.dp)

/**
 * How the wallpapers are now, as One UI shows them at the top of its wallpaper screen: the lock screen and the home
 * screen side by side, each with what it really shows ([lockScreen]: see [LockScreenPicture]; the home screen as the
 * person made it: [home]). The Android wallpaper is the real picture, seen through the window. Only a picture: the
 * names under them say what each one is.
 */
@Composable
internal fun WallpaperNow(
    wallpaper: LauncherWallpaper,
    lockScreen: LauncherWallpaper?,
    home: MiniHome,
    isMonochrome: Boolean
) {
    val ratio = screenRatio()
    Column {
        SectionHeader(text = stringResource(R.string.launcher_wallpaper_now))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val height = minOf(PreviewMaxHeight, (maxWidth - OriSpacing.Medium) / 2 / ratio)
            Row(
                horizontalArrangement = Arrangement.spacedBy(OriSpacing.Medium, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                ScreenPreview(stringResource(R.string.launcher_wallpaper_target_lock), height * ratio, height) {
                    LockScreenPicture(lockScreen)
                }
                ScreenPreview(stringResource(R.string.launcher_wallpaper_target_home), height * ratio, height) {
                    WallpaperPicture(wallpaper)
                    MiniHomePicture(home, rememberHomeInk(wallpaper).color, isMonochrome)
                }
            }
        }
    }
}

@Composable
private fun ScreenPreview(label: String, width: Dp, height: Dp, picture: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(width)) {
        Box(
            modifier = Modifier
                .size(width, height)
                .clip(ScreenPreviewShape)
                .clearAndSetSemantics { }
        ) { picture() }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = OriSpacing.ExtraSmall)
        )
    }
}

/**
 * A wallpaper drawn small: the ORI ones as they are, a photo from its small copy, the Android one as itself, through
 * the window ([AndroidWallpaperWindow]).
 */
@Composable
internal fun WallpaperPicture(wallpaper: LauncherWallpaper) {
    when (wallpaper) {
        LauncherWallpaper.System -> AndroidWallpaperWindow()
        is LauncherWallpaper.Photo -> PhotoThumbnail(wallpaper.fileName)
        else -> WallpaperBackground(wallpaper)
    }
}

/** The shape of this screen, its width over its height: the small screens of the wallpaper have it. */
@Composable
internal fun screenRatio(): Float {
    val window = LocalWindowInfo.current.containerSize
    return if (window.height > 0) window.width.toFloat() / window.height else UPRIGHT_RATIO
}

/** An Android wallpaper as the colors Android tells of it, top to bottom ([readSystemWallpaperLook]). */
@Composable
internal fun SystemWallpaperPicture(colors: List<Color>) {
    val modifier = Modifier.fillMaxSize()
    if (colors.size == 1) {
        Box(modifier = modifier.background(colors.first()))
    } else {
        Box(modifier = modifier.background(Brush.verticalGradient(colors)))
    }
}
