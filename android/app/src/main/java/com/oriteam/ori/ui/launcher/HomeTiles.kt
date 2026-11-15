package com.oriteam.ori.ui.launcher

import android.graphics.Bitmap
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.ui.theme.OnOriTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The icon takes this part of the shorter side of its tile, at the top, with the name under it. */
private const val TILE_ICON_SHARE = 0.42f

/** The color of an app's tile: its icon's own hue, never louder than this, as dark as this (white writing reads). */
private const val TILE_MAX_SATURATION = 0.55f
private const val TILE_LIGHTNESS = 0.36f

/** A folder's tile is the writing color of the home screen, this see-through, as its icon is. */
private const val FOLDER_TILE_ALPHA = 0.22f

/** The icon is read this small to find its color: enough for the average, cheap to go through. */
private const val COLOR_SAMPLE_PX = 12
private const val OPAQUE = 128
private const val HSL_PARTS = 3
private const val SATURATION = 1
private const val LIGHTNESS = 2

/**
 * How an item of the largest size ("Máximo") is drawn: a tile filling its cell, of [color], with the icon at the top
 * and the name inside it in [nameColor] (shown when [showsName]), as the tiles of Windows Phone, made for today
 * (rounded, of a calm tone of the app's own color, grey in low stimulation).
 */
@Immutable
internal data class TileLook(val color: Color, val nameColor: Color, val showsName: Boolean)

/** The icon in a tile: a part of the shorter side of the cell, so the name fits under it. */
internal fun tileIconSize(metrics: HomeMetrics): Dp = minOf(metrics.cellWidth, metrics.cellHeight) * TILE_ICON_SHARE

/**
 * The tile of [app]: a calm tone of its icon's color, read off the main thread (grey until then, and in low
 * stimulation, [isMonochrome]).
 */
@Composable
internal fun rememberAppTile(app: LauncherApp, isMonochrome: Boolean, showsName: Boolean): TileLook {
    val load = LocalAppIconLoader.current
    val color by produceState(initialValue = Color.Gray, app.key, load, isMonochrome) {
        val icon = load(app.key)
        value = if (icon == null) Color.Gray else withContext(Dispatchers.Default) { tileColorOf(icon, isMonochrome) }
    }
    return TileLook(color, OnOriTile, showsName)
}

/** The tile of a folder: see-through, of the writing color of the home screen, as the folder's own icon. */
@Composable
internal fun folderTile(showsName: Boolean): TileLook {
    val ink = LocalContentColor.current
    return TileLook(ink.copy(alpha = FOLDER_TILE_ALPHA), ink, showsName)
}

/**
 * The average color of the parts of [icon] that show, with its hue, at most [TILE_MAX_SATURATION] and at
 * [TILE_LIGHTNESS] (no saturation when [isMonochrome]).
 */
internal fun tileColorOf(icon: Bitmap, isMonochrome: Boolean): Color {
    val small = icon.scale(COLOR_SAMPLE_PX, COLOR_SAMPLE_PX)
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0
    for (y in 0 until small.height) {
        for (x in 0 until small.width) {
            val pixel = small[x, y]
            if (android.graphics.Color.alpha(pixel) < OPAQUE) continue
            red += android.graphics.Color.red(pixel)
            green += android.graphics.Color.green(pixel)
            blue += android.graphics.Color.blue(pixel)
            count++
        }
    }
    if (count == 0) return Color.Gray
    val average = Color(red = (red / count).toInt(), green = (green / count).toInt(), blue = (blue / count).toInt())
    val hsl = FloatArray(HSL_PARTS)
    ColorUtils.colorToHSL(average.toArgb(), hsl)
    hsl[SATURATION] = if (isMonochrome) 0f else minOf(hsl[SATURATION], TILE_MAX_SATURATION)
    hsl[LIGHTNESS] = TILE_LIGHTNESS
    return Color(ColorUtils.HSLToColor(hsl))
}
