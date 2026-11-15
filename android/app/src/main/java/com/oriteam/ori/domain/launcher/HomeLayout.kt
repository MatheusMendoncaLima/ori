package com.oriteam.ori.domain.launcher

/**
 * How the icons sit on the home screen.
 * - [PACKED]: one after the other, with no gaps, in the order the person chose (as on older iPhones).
 * - [FREE]: each one in the cell the person put it, with gaps where they want (as on Android and newer iPhones).
 */
enum class HomeArrangement { PACKED, FREE }

/** A cell of the home screen: the page (side by side, as on One UI), the row and the column. */
data class HomeCell(val page: Int, val row: Int, val column: Int) : Comparable<HomeCell> {
    /** Reading order: page, then row, then column. */
    override fun compareTo(other: HomeCell): Int =
        compareValuesBy(this, other, HomeCell::page, HomeCell::row, HomeCell::column)
}

/** How many cells something takes: an app takes one, a widget may take more. */
data class HomeSpan(val columns: Int = 1, val rows: Int = 1)

/**
 * How the screen is held. As on One UI, a tablet turned has the grid transposed (the columns of upright become its
 * rows) and keeps cells of its own, so arranging it lying down does not move anything upright.
 */
enum class HomeOrientation { UPRIGHT, TURNED }

/**
 * The cells of a page. [clockSpan] is the size of the clock widget of ORI on this screen: it depends on how big the
 * cells are, which only the screen knows. [orientation] says which cells of the person the grid uses.
 */
data class HomeGrid(
    val columns: Int,
    val rows: Int,
    val clockSpan: HomeSpan = HomeSpan(columns, 1),
    val orientation: HomeOrientation = HomeOrientation.UPRIGHT
)

/**
 * A grid chosen for the home screen held upright, as "Grade da tela inicial" of One UI: [columns] × [rows]. Turned, a
 * tablet has it transposed. With it, the icon size no longer changes the cells: the icons fit in them.
 */
data class GridChoice(val columns: Int, val rows: Int)

data class PlacedHomeItem(val key: String, val cell: HomeCell, val span: HomeSpan)

/** The span as it is placed on [grid]: never wider than its columns, nor taller than its rows. */
fun HomeSpan.fittedTo(grid: HomeGrid): HomeSpan = HomeSpan(
    columns = columns.coerceIn(1, grid.columns),
    rows = rows.coerceIn(1, grid.rows)
)

private const val WIDGET_PREFIX = "widget:"

/** Pages looked at when finding room; far more than the 60 items a home screen takes. */
private const val MAX_PAGES = 64

/** Widgets share the order of the home screen with the apps, under a key of their own. */
fun widgetKey(widgetId: Int): String = "$WIDGET_PREFIX$widgetId"

/** The widget id in a key of the home screen, or null for an app. */
fun widgetIdOf(key: String): Int? {
    if (!key.startsWith(WIDGET_PREFIX)) return null
    return key.removePrefix(WIDGET_PREFIX).toIntOrNull()
}

/**
 * Where each item of [keys] goes. Packed: in order, each at the first room at or after the one before (so the order
 * never changes, and a wide widget may leave a gap before it). Free: at its stored cell when it fits and is free;
 * anything else at the first free room from the page [startPageOf] gives it: first the items whose stored cell this
 * grid cannot show (larger icons, fewer columns), in the reading order of those cells, so their order on the screen
 * stays; then the ones without a cell, in the order of [keys]. The result is in reading order.
 */
fun arrangeHome(
    keys: List<String>,
    spanOf: (String) -> HomeSpan,
    grid: HomeGrid,
    arrangement: HomeArrangement,
    positions: Map<String, HomeCell> = emptyMap(),
    startPageOf: (String) -> Int = { 0 }
): List<PlacedHomeItem> {
    val room = Room(grid)
    val placed = mutableListOf<PlacedHomeItem>()
    val spans = keys.associateWith { room.clamp(spanOf(it)) }
    val waiting = if (arrangement == HomeArrangement.FREE) {
        // An item whose stored cell is free stays there; the others wait for the first free room, in reading order.
        keys.filterNot { key ->
            val span = spans.getValue(key)
            val cell = positions[key]?.takeIf { room.fits(it, span) }
            cell?.also { placed += room.take(key, it, span) } != null
        }.sortedWith(compareBy(nullsLast()) { positions[it] })
    } else {
        keys
    }
    var cursor = HomeCell(0, 0, 0)
    waiting.forEach { key ->
        val span = spans.getValue(key)
        val from = if (arrangement == HomeArrangement.FREE) HomeCell(startPageOf(key), 0, 0) else cursor
        val cell = room.firstFree(from = from, span = span) ?: return@forEach
        placed += room.take(key, cell, span)
        if (arrangement == HomeArrangement.PACKED) cursor = cell
    }
    return placed.sortedBy { it.cell }
}

/** How many pages the arrangement uses (at least one). */
fun pageCount(placed: List<PlacedHomeItem>): Int = (placed.maxOfOrNull { it.cell.page } ?: 0) + 1

/**
 * Packed: the place in the order where an item dropped on [target] goes. It takes the place of the item there: moving
 * forward, that item steps back to fill the gap (so the dropped one goes after it); moving back, it steps forward.
 */
fun packedIndexFor(placed: List<PlacedHomeItem>, movingKey: String, target: HomeCell): Int {
    val moving = placed.firstOrNull { it.key == movingKey }
    val before = placed.count { it.key != movingKey && it.cell < target }
    val isForwardOntoItem = moving != null && moving.cell < target &&
        placed.any { it.key != movingKey && it.cell == target }
    return if (isForwardOntoItem) before + 1 else before
}

/**
 * Free: the positions after [key] is dropped on [target]. It goes there when the cells are free (or only its own);
 * on a single item of the same size, the two change places. Null when it does not fit: the item goes back.
 * Every item gets its current cell stored, so nothing moves on its own later.
 */
fun freeMove(placed: List<PlacedHomeItem>, key: String, target: HomeCell, grid: HomeGrid): Map<String, HomeCell>? {
    val moving = placed.firstOrNull { it.key == key } ?: return null
    val others = placed.filter { it.key != key }
    val room = Room(grid).apply { others.forEach { take(it.key, it.cell, it.span) } }
    val covered = others.filter { other -> overlaps(target, moving.span, other.cell, other.span) }
    val positions = placed.associate { it.key to it.cell }
    return if (covered.isEmpty()) {
        positions.takeIf { room.fits(target, moving.span) }?.plus(key to target)
    } else {
        covered.singleOrNull()?.takeIf { it.span == moving.span && it.cell == target }
            ?.let { swapped -> positions + (key to swapped.cell) + (swapped.key to moving.cell) }
    }
}

/** Whether [key] of [placed] fits at its cell with [span]: inside the grid, and on cells free of the others. */
fun fitsResized(placed: List<PlacedHomeItem>, key: String, span: HomeSpan, grid: HomeGrid): Boolean {
    val item = placed.firstOrNull { it.key == key } ?: return false
    val room = Room(grid).apply { placed.filter { it.key != key }.forEach { take(it.key, it.cell, it.span) } }
    return room.fits(item.cell, span)
}

private fun overlaps(first: HomeCell, firstSpan: HomeSpan, second: HomeCell, secondSpan: HomeSpan): Boolean =
    first.page == second.page &&
        first.row < second.row + secondSpan.rows && second.row < first.row + firstSpan.rows &&
        first.column < second.column + secondSpan.columns && second.column < first.column + firstSpan.columns

/** The cells already taken, page by page. */
private class Room(private val grid: HomeGrid) {
    private val taken = HashSet<HomeCell>()

    fun clamp(span: HomeSpan) = span.fittedTo(grid)

    fun fits(cell: HomeCell, span: HomeSpan): Boolean {
        val inside = cell.page >= 0 && cell.row >= 0 && cell.column >= 0 &&
            cell.column + span.columns <= grid.columns && cell.row + span.rows <= grid.rows
        return inside && cellsOf(cell, span).none { it in taken }
    }

    fun take(key: String, cell: HomeCell, span: HomeSpan): PlacedHomeItem {
        taken += cellsOf(cell, span)
        return PlacedHomeItem(key, cell, span)
    }

    /** The first cell at or after [from], in reading order, where [span] fits. */
    fun firstFree(from: HomeCell, span: HomeSpan): HomeCell? {
        val perPage = grid.rows * grid.columns
        val first = from.page * perPage + from.row * grid.columns + from.column
        return (first until MAX_PAGES * perPage).asSequence()
            .map { index -> HomeCell(index / perPage, index % perPage / grid.columns, index % grid.columns) }
            .firstOrNull { fits(it, span) }
    }

    private fun cellsOf(cell: HomeCell, span: HomeSpan) = buildList {
        for (row in cell.row until cell.row + span.rows) {
            for (column in cell.column until cell.column + span.columns) add(HomeCell(cell.page, row, column))
        }
    }
}
