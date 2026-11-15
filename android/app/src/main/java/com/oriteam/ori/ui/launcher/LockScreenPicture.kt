package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.ui.app.screens.today.rememberCurrentMinute
import com.oriteam.ori.ui.components.text.DateSkeletons
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberDateFormat
import com.oriteam.ori.ui.components.text.rememberTimeFormat
import java.time.LocalDate

/** Where the time starts and how big it and the date are, as parts of the height of the screen, as on a lock screen. */
private const val TIME_TOP_SHARE = 0.1f
private const val TIME_SHARE = 0.11f
private const val DATE_SHARE = 0.028f
private const val LIGHT = 0.5f

/**
 * A lock screen as it looks with [wallpaper]: the picture, and the time and the date over it, sized to the height of
 * the drawing, so the same drawing serves the small preview of "Como está agora" and the whole screen of the preview.
 * The Android wallpaper is the real one, seen through the window ([AndroidWallpaperWindow]); null is a picture Android
 * put there and does not let ORI see, drawn with the colors Android tells of it, or plain. Only a picture: the screen
 * reader reads the name beside it.
 */
@Composable
internal fun LockScreenPicture(wallpaper: LauncherWallpaper?, modifier: Modifier = Modifier) {
    val colors = LocalSystemWallpaperLook.current.lockColors
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val ink = if (wallpaper == null) {
            UnknownLockPicture(colors)
            inkOn(colors)
        } else {
            WallpaperPicture(wallpaper)
            rememberHomeInk(wallpaper).color
        }
        LockScreenTime(ink, maxHeight)
    }
}

/** A picture of Android that ORI cannot see: its colors when Android tells them, or a plain tint. */
@Composable
private fun UnknownLockPicture(colors: List<Color>) {
    if (colors.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
    } else {
        SystemWallpaperPicture(colors)
    }
}

/** Dark writing on a light picture, light on a dark one. */
@Composable
private fun inkOn(colors: List<Color>): Color = when {
    colors.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
    colors.first().luminance() > LIGHT -> Color.Black
    else -> Color.White
}

@Composable
private fun LockScreenTime(ink: Color, height: Dp) {
    val time = rememberCurrentMinute().format(rememberTimeFormat())
    val date = LocalDate.now().format(rememberDateFormat(DateSkeletons.WEEKDAY_DAY_MONTH))
    val (timeSize, dateSize) = with(
        LocalDensity.current
    ) { (height * TIME_SHARE).toSp() to (height * DATE_SHARE).toSp() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = height * TIME_TOP_SHARE)
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = timeSize,
                lineHeight = timeSize,
                fontWeight = ClockTimeStyleWeight
            ),
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = dateSize, lineHeight = dateSize),
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}
