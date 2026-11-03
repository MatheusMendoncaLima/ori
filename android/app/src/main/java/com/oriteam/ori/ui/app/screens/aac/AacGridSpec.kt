package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.oriteam.ori.domain.preferences.SymbolLabelMode
import com.oriteam.ori.domain.preferences.SymbolSize

/** How the symbol grid is drawn in the space it has. */
data class AacGridSpec(
    val columns: Int,
    /** Side of the picture inside a tile; zero when the person chose text only. */
    val imageSize: Dp,
    /** Narrow tiles use smaller labels, so common words still fit without breaking in the middle. */
    val usesSmallLabels: Boolean
)

/** Gap between tiles at the comfortable density. */
val AacGridSpacing: Dp = 12.dp

/** More columns than this makes symbols hard to find, even on a large screen. */
internal const val MAX_GRID_COLUMNS = 8

/** On a common phone these give four, three and two columns, so each choice makes a real difference. */
private val symbolSizeFactors = mapOf(
    SymbolSize.SMALL to 0.7f,
    SymbolSize.MEDIUM to 1f,
    SymbolSize.LARGE to 1.3f
)

private val MinImageSize = 40.dp
private val MaxImageSize = 160.dp
private val SmallLabelsBelowTileWidth = 80.dp
private const val IMAGE_SHARE_OF_TILE = 0.6f

/** Width a tile needs for a common word, multiplied by the system text scale. */
private val LabelRoomWidth = 67.dp

/**
 * Columns, picture size and label size for a grid [availableWidth] wide. This is the width the
 * grid really receives, after the screen margins, not the width of the window.
 *
 * Each tile is at least [baseTileWidth] (from the window size class) times the chosen symbol
 * size. Enlarged system text widens tiles only when the label needs it: a person who needs big
 * text gets fewer, wider symbols instead of broken words, but not one giant symbol per screen.
 */
fun aacGridSpec(
    availableWidth: Dp,
    baseTileWidth: Dp,
    symbolSize: SymbolSize,
    labelMode: SymbolLabelMode,
    fontScale: Float,
    spacing: Dp = AacGridSpacing
): AacGridSpec {
    val minTileWidth = max(baseTileWidth * symbolSizeFactors.getValue(symbolSize), LabelRoomWidth * fontScale)
    val fitting = ((availableWidth + spacing) / (minTileWidth + spacing)).toInt()
    val columns = fitting.coerceIn(1, MAX_GRID_COLUMNS)
    val tileWidth = max((availableWidth - spacing * (columns - 1)) / columns, 0.dp)
    val imageSize = when (labelMode) {
        SymbolLabelMode.TEXT_ONLY -> 0.dp
        else -> (tileWidth * IMAGE_SHARE_OF_TILE).coerceIn(MinImageSize, MaxImageSize)
    }
    return AacGridSpec(
        columns = columns,
        imageSize = imageSize,
        usesSmallLabels = tileWidth < SmallLabelsBelowTileWidth
    )
}
