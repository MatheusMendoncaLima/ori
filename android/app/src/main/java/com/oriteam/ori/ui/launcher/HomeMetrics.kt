package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.launcher.GridChoice
import com.oriteam.ori.domain.launcher.HomeGrid
import com.oriteam.ori.domain.launcher.HomeOrientation
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.LauncherIconSize
import com.oriteam.ori.ui.theme.OriSpacing
import kotlin.math.ceil

/**
 * Never more rows than this upright (One UI has 8 on a tablet upright): on a taller screen the cells grow instead. The
 * grid of a tablet turned has these rows as its columns.
 */
private const val MAX_ROWS = 8

/**
 * The clock may be drawn up to this much smaller rather than take one more row: a row just short of the clock (a
 * tablet turned) would otherwise leave a whole empty row under it.
 */
private const val CLOCK_MIN_SCALE = 0.85f

/** A tile of "Máximo" is at least this part of its width high, upright. */
private const val TILE_MIN_HEIGHT_SHARE = 0.8f

/** The name under an icon takes up to two lines; a screen turned may leave room for one, or none. */
private const val MAX_LABEL_LINES = 2

/** A tile with its name has three small gaps: over the icon, between the icon and the name, and under the name. */
private const val NAMED_TILE_GAPS = 3

/** A tile without a name has two: over and under the icon. */
private const val ICON_TILE_GAPS = 2

/** The smallest an icon gets, on a screen too short for the chosen size. */
private val MinIconSize = 40.dp

/** The narrowest a cell gets with a chosen grid: the smallest icon and a little room at its sides. */
private val MinCellWidth = MinIconSize + OriSpacing.Small * 2

/** The page dots over the dock. */
internal val DotsHeight = OriSpacing.Large

/**
 * The strip over the grid, as on Android: "Remover" while an item is carried, "Pronto" while editing. It is kept
 * empty otherwise, so neither ever covers an icon. At least this tall; taller with large text (see [topStripFor]).
 */
private val MinTopStripHeight = 64.dp

/** The space over and under the word "Remover" in its pill (RemoveZone). */
internal val RemovePillPadding = OriSpacing.Medium

/**
 * The cells of one home page: how many columns and rows, and their size.
 *
 * As on One UI, a tablet turned has the upright grid transposed (6 × 8 upright, 8 × 6 turned): the upright rows
 * become its columns and the upright columns its rows, and each way keeps its own cells (`positionsTurned`). With the
 * large icons of ORI the turned rows are low, so there the icons get a little smaller and their names one line.
 * Columns have a ceiling by icon size (6 for medium icons), as Android has on tablets: a tablet gets wider cells, not
 * a crowd of small ones.
 */
internal data class HomeMetrics(
    val grid: HomeGrid,
    val cellWidth: Dp,
    val cellHeight: Dp,
    /** The width of the cells together. */
    val gridWidth: Dp,
    /** The icon as drawn: the chosen size, smaller when the screen turned (or a short one) needs it. */
    val iconSize: Dp,
    /** Lines of the name under an icon: two, one on a screen turned, none when even one does not fit. */
    val labelLines: Int,
    /** The dock: one row of icons without names, as on Android. */
    val dockHeight: Dp,
    /** The strip at the bottom of the pages: the dots and the dock (or the tools while editing), or only the dots. */
    val bottomHeight: Dp,
    /** The strip over the grid where "Remover" and "Pronto" appear: it grows with large text. */
    val topStripHeight: Dp,
    /** The size the clock widget needs; when its cells are a little lower, it is drawn smaller to fit them. */
    val clockSize: DpSize,
    /** How many apps the dock takes: the upright columns either way, so the dock never overflows when turned. */
    val dockCapacity: Int,
    /** The grids offered on this screen ("Grade da tela inicial"): the one each icon size gives upright. */
    val gridChoices: List<GridChoice> = emptyList(),
    /** "Máximo": the items are tiles filling their cells, with the name inside (HomeTiles.kt). */
    val isTiles: Boolean = false
) {
    val showsLabels: Boolean get() = labelLines > 0
}

/**
 * What the screen gives the grid: the room inside the system bars, a line of an icon's name, a line of "Remover"
 * (both grow with the person's text size), the clock, and whether a row at the bottom is kept for the dock ("Mostrar o
 * dock" off and no button of all the apps: the same cells, taller, take its room).
 */
internal data class HomeRoom(
    val width: Dp,
    val height: Dp,
    val labelLine: Dp,
    val stripLine: Dp,
    val clock: DpSize,
    val hasDock: Boolean = true
)

/**
 * The grid of the home screen on a window of [windowWidth] × [windowHeight], inside the system bars on every side.
 * Made once for all of ORI Home, so the pages, the drop, the preview of a wallpaper, all the apps and the list of
 * widgets agree on the cells. A window wider than tall is a tablet turned: its grid is the upright one transposed (the
 * bars stay at the top and the bottom of a tablet either way). Phones stay upright (LauncherActivity), as One UI does
 * by default.
 */
@Composable
internal fun rememberHomeMetrics(
    windowWidth: Dp,
    windowHeight: Dp,
    size: LauncherIconSize,
    chosen: GridChoice? = null,
    showsLabels: Boolean = true,
    hasDock: Boolean = true
): HomeMetrics {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val direction = LocalLayoutDirection.current
    val sides = insets.calculateStartPadding(direction) + insets.calculateEndPadding(direction)
    val bars = insets.calculateTopPadding() + insets.calculateBottomPadding()
    val density = LocalDensity.current
    val labelLine = with(density) { size.labelStyle().lineHeight.toDp() }
    val stripLine = with(density) { MaterialTheme.typography.labelLarge.lineHeight.toDp() }
    // The clock widget takes only the cells its time and date need, as a clock widget of Android: icons sit beside it.
    val clock = rememberClockSize()
    val room = HomeRoom(windowWidth - sides, windowHeight - bars, labelLine, stripLine, clock, hasDock)
    val upright = if (windowWidth <= windowHeight) null else room.copy(windowHeight - sides, windowWidth - bars)
    return remember(room, upright, size, chosen, showsLabels) {
        homeMetricsFor(room, size, upright = upright, chosen = chosen, showsLabels = showsLabels)
    }
}

/**
 * The grid on [room]. Upright ([upright] null), the columns come from the width and the icon size and the rows from
 * the height, or from the grid the person [chosen] (kept to what the screen holds); then the icon fits its cell, so
 * the icon size no longer changes the cells, as on One UI. Turned ([upright] is the same screen held upright), the
 * upright grid is transposed. Rows are never squeezed below an icon and its name: when they do not fit, the icon gets
 * smaller, then its name one line, then none (as Android does), and as a last resort fewer rows are shown.
 */
internal fun homeMetricsFor(
    room: HomeRoom,
    size: LauncherIconSize,
    upright: HomeRoom? = null,
    chosen: GridChoice? = null,
    showsLabels: Boolean = true
): HomeMetrics {
    // The cells are counted as with the dock: without it they are only taller, so turning it off moves nothing to
    // another cell or page.
    val counted = room.copy(hasDock = true)
    val standing = (upright ?: room).copy(hasDock = true)
    val isTiles = size == LauncherIconSize.MAXIMUM
    // A tile has its name inside it: no line of name under it to make room for.
    val lines = if (showsLabels && !isTiles) MAX_LABEL_LINES else 0
    val uprightWidth = standing.width - OriSpacing.Medium * 2
    val uprightColumns = chosen?.columns?.coerceIn(1, maxOf(1, (uprightWidth / MinCellWidth).toInt()))
        ?: size.columnsFor(uprightWidth)
    val uprightRows = chosen?.rows?.coerceIn(1, MAX_ROWS) ?: rowsFor(standing, size.iconDp, lines).let { rows ->
        if (isTiles) minOf(rows, tileRowsFor(standing, uprightWidth / uprightColumns, size.iconDp)) else rows
    }
    val columns = if (upright == null) uprightColumns else uprightRows
    // Turned, the upright columns are the rows, as on One UI, but never more than the icons of the chosen size fit
    // with one line of name: large icons stay large (a person chose them to touch them), with fewer rows.
    val rows = when {
        upright == null -> uprightRows
        chosen != null -> uprightColumns
        else -> minOf(uprightColumns, rowsFor(counted, size.iconDp, minOf(lines, 1)))
    }
    val gridWidth = room.width - OriSpacing.Medium * 2
    // With a chosen grid the cells come first: the icon is the chosen size when it fits its cell, smaller otherwise.
    val wish = if (chosen == null) size.iconDp else minOf(size.iconDp, gridWidth / columns - OriSpacing.Small * 2)
    val tile = tileFor(room, rows, maxOf(wish, MinIconSize), lines)
    val gridHeight = gridHeightFor(room, tile.icon).coerceAtLeast(0.dp)
    val cellHeight = maxOf(gridHeight / tile.rows, tile.minHeight(room.labelLine))
    val clockSpan = HomeSpan(
        columns = ceil(room.clock.width / (gridWidth / columns)).toInt().coerceIn(1, columns),
        rows = ceil(room.clock.height * CLOCK_MIN_SCALE / cellHeight).toInt().coerceIn(1, tile.rows)
    )
    val orientation = if (upright == null) HomeOrientation.UPRIGHT else HomeOrientation.TURNED
    val dockHeight = dockHeightFor(tile.icon)
    return HomeMetrics(
        grid = HomeGrid(columns, tile.rows, clockSpan = clockSpan, orientation = orientation),
        cellWidth = gridWidth / columns,
        cellHeight = cellHeight,
        gridWidth = gridWidth,
        iconSize = tile.icon,
        labelLines = tile.labelLines,
        dockHeight = dockHeight,
        bottomHeight = bottomHeightFor(room, dockHeight),
        topStripHeight = topStripFor(room),
        clockSize = room.clock,
        dockCapacity = uprightColumns,
        gridChoices = gridChoicesFor(standing),
        isTiles = isTiles
    )
}

/**
 * Tiles stay about square: no more rows than cells at least [TILE_MIN_HEIGHT_SHARE] of [cellWidth] high fit, with
 * icons of [icon] in the dock (a tablet upright would otherwise get long, low tiles).
 */
private fun tileRowsFor(room: HomeRoom, cellWidth: Dp, icon: Dp): Int =
    (gridHeightFor(room, icon) / (cellWidth * TILE_MIN_HEIGHT_SHARE)).toInt().coerceAtLeast(1)

/**
 * The grids offered on a screen held upright as [standing]: the one each icon size gives on it, from the most cells to
 * the fewest (on the test tablet, 7 × 8 for small icons down to 4 × 6 for huge ones).
 */
internal fun gridChoicesFor(standing: HomeRoom): List<GridChoice> = LauncherIconSize.entries
    .map { GridChoice(it.columnsFor(standing.width - OriSpacing.Medium * 2), rowsFor(standing, it.iconDp)) }
    .distinct()
    .sortedByDescending { it.columns * it.rows }

/** An icon with its name in [labelLines] lines, on [rows] rows. */
private data class Tile(val icon: Dp, val labelLines: Int, val rows: Int) {
    fun minHeight(labelLine: Dp): Dp = if (labelLines > 0) {
        icon + labelLine * labelLines + OriSpacing.Small * NAMED_TILE_GAPS
    } else {
        icon + OriSpacing.Small * ICON_TILE_GAPS
    }
}

/** How many rows of icons of [icon] with [lines] lines of name fit [room] (one at least, [MAX_ROWS] at most). */
private fun rowsFor(room: HomeRoom, icon: Dp, lines: Int = MAX_LABEL_LINES): Int {
    val named = Tile(icon, lines, rows = 1).minHeight(room.labelLine)
    val gridHeight = gridHeightFor(room, icon)
    return (gridHeight / named).toInt().coerceIn(1, MAX_ROWS)
}

/**
 * The icon and the most lines of its name with which [rows] rows and the dock fit [room]: the [chosen] icon with as
 * many lines as fit it (two, one); when not even one line fits it, the largest icon with two lines, one or none, as
 * long as it is at least [MinIconSize]. When even icons of [MinIconSize] without names do not fit, fewer rows show.
 */
private fun tileFor(room: HomeRoom, rows: Int, chosen: Dp, maxLines: Int): Tile {
    // The cells and the dock share this height; the dock is an icon with its padding.
    val dockIcons = if (room.hasDock) 1 else 0
    val free = room.height - topStripFor(room) - DotsHeight - OriSpacing.Medium * 2 * dockIcons
    fun iconWith(lines: Int) = (free - Tile(0.dp, lines, rows).minHeight(room.labelLine) * rows) / (rows + dockIcons)
    val whole = (maxLines downTo minOf(maxLines, 1)).firstOrNull { iconWith(it) >= chosen }
    val shrunk = (maxLines downTo 0).firstOrNull { iconWith(it) >= MinIconSize }
    return when {
        whole != null -> Tile(chosen, whole, rows)
        shrunk != null -> Tile(minOf(iconWith(shrunk), chosen), shrunk, rows)
        else -> {
            val bare = Tile(MinIconSize, 0, 1).minHeight(room.labelLine)
            Tile(MinIconSize, 0, ((free - MinIconSize * dockIcons) / bare).toInt().coerceIn(1, rows))
        }
    }
}

private fun dockHeightFor(icon: Dp) = icon + OriSpacing.Medium * 2

/** Under the cells: the dots and the dock, or only the dots when there is no dock. */
private fun bottomHeightFor(room: HomeRoom, dockHeight: Dp) =
    if (room.hasDock) dockHeight + DotsHeight else DotsHeight

/** The strip holds "Remover" (8 dp from the top, its line and its padding) with a little room under it. */
private fun topStripFor(room: HomeRoom): Dp = maxOf(
    MinTopStripHeight,
    OriSpacing.Small + room.stripLine + RemovePillPadding * 2 + OriSpacing.ExtraSmall
)

/** The height left for the cells with icons of [icon]: the window less the top strip, the dots and any dock. */
private fun gridHeightFor(room: HomeRoom, icon: Dp) =
    room.height - topStripFor(room) - DotsHeight - if (room.hasDock) dockHeightFor(icon) else 0.dp

/** A page of the home screen sits inside the system bars, under the top strip and above the dots and the dock. */
internal fun Modifier.insidePageArea(metrics: HomeMetrics): Modifier = this
    .safeDrawingPadding()
    .padding(top = metrics.topStripHeight, bottom = metrics.bottomHeight)
