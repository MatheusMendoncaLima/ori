package com.oriteam.ori.domain.launcher

/*
 * Editing the home screen with the settings of the person: where each item is, where it goes when dropped (on a
 * cell or on the dock) or moved by the screen reader. The layout itself is in HomeLayout.kt.
 */

/** The cells [key] takes: one for an app, the stored size for a widget, the size of the screen for the clock. */
fun LauncherSettings.spanOf(key: String, grid: HomeGrid): HomeSpan = when (key) {
    CLOCK_KEY -> grid.clockSpan
    else -> widgetIdOf(key)?.let { widgetSpans[it] } ?: HomeSpan()
}

/** The free cells kept for the way the screen is held. */
fun LauncherSettings.positionsFor(orientation: HomeOrientation): Map<String, HomeCell> =
    if (orientation == HomeOrientation.TURNED) positionsTurned else positions

/** [cells] as the free cells of the way the screen is held; the other way keeps its own. */
fun LauncherSettings.withPositions(orientation: HomeOrientation, cells: Map<String, HomeCell>): LauncherSettings =
    if (orientation == HomeOrientation.TURNED) copy(positionsTurned = cells) else copy(positions = cells)

/**
 * The home screen of these settings, for the items in [keys] (the ones still installed), on [grid]. With a tablet
 * turned, an item that has no cell there yet goes to the same page it has upright, in the upright reading order (the
 * transposed grid has as many cells as the upright one, so a page still fits in its page).
 */
fun LauncherSettings.arranged(keys: List<String>, grid: HomeGrid): List<PlacedHomeItem> {
    val spans = { key: String -> spanOf(key, grid) }
    if (grid.orientation == HomeOrientation.UPRIGHT) return arrangeHome(keys, spans, grid, arrangement, positions)
    val inUprightOrder = if (arrangement == HomeArrangement.FREE) {
        keys.sortedWith(compareBy(nullsLast()) { positions[it] })
    } else {
        keys
    }
    return arrangeHome(inUprightOrder, spans, grid, arrangement, positionsTurned) { positions[it]?.page ?: 0 }
}

/**
 * Packed or free. Packed puts the clock first, at the top of the first page: in the order it would sit among the icons,
 * in the middle of a row. The cells of the free layout are kept, so going back to it finds everything in place.
 */
fun LauncherSettings.withArrangement(arrangement: HomeArrangement): LauncherSettings {
    val clockFirst = arrangement == HomeArrangement.PACKED && CLOCK_KEY in favorites
    val order = if (clockFirst) listOf(CLOCK_KEY) + (favorites - CLOCK_KEY) else favorites
    return copy(arrangement = arrangement, favorites = order)
}

/** Only apps go in the dock: a widget or the clock would not fit in its row, and folders stay on the pages. */
fun isDockable(key: String): Boolean = isAppKey(key)

/**
 * [key] dropped on [target]. Packed: it takes that place in the order and the others close up. Free: it stays in
 * that cell (or changes places with an item of its size); when it does not fit, nothing changes. An app that comes
 * from outside the pages (the dock, or all the apps) joins them there, or at the first free cell when that one is
 * taken; a full home screen takes no new app. A locked home screen ([LauncherSettings.isLayoutLocked]) changes nothing.
 */
fun LauncherSettings.dropped(keys: List<String>, key: String, target: HomeCell, grid: HomeGrid): LauncherSettings {
    if (isLayoutLocked) return this
    val isOutside = key !in keys
    if (isOutside && key !in favorites && isHomeFull) return this
    // From outside the pages (the dock, a folder, all the apps): it leaves where it was and joins the pages.
    val settings = if (isOutside) {
        withoutInFolders(key).let { it.copy(dock = it.dock - key, favorites = it.favorites - key + key) }
    } else {
        this
    }
    // The folder it left may have become its last app, in its place: that app is shown where the folder was.
    val shown = if (isOutside) shownAfterLeavingFolder(keys, key, settings) + key else keys
    val placed = settings.arranged(shown, grid)
    return when (arrangement) {
        HomeArrangement.PACKED ->
            settings.moveFavorite(placed.map { it.key }, key, packedIndexFor(placed, key, target))
        HomeArrangement.FREE -> freeMove(placed, key, target, grid)
            ?.let { moved ->
                val cells = settings.keepingStoredCells(placed, moved, grid.orientation)
                settings.withPositions(grid.orientation, cells)
            }
            ?: if (isOutside) settings else this
    }
}

/**
 * An app dropped on the dock at [index]: it leaves the pages (or changes place in the dock). A full dock takes no
 * new app, and widgets never go there.
 */
fun LauncherSettings.droppedOnDock(key: String, index: Int, capacity: Int): LauncherSettings {
    val isNew = key !in dock
    val isFull = isNew && dock.size >= minOf(capacity, MAX_DOCK_ITEMS)
    if (!isDockable(key) || isFull || isLayoutLocked) return this
    val rest = dock - key
    val docked = rest.toMutableList().apply { add(index.coerceIn(0, rest.size), key) }
    return withoutInFolders(key).let {
        it.copy(
            dock = docked,
            favorites = it.favorites - key,
            positions = it.positions - key,
            positionsTurned = it.positionsTurned - key
        )
    }
}

/**
 * [key] one place earlier ([offset] -1) or later (+1) in reading order, as the screen reader asks. Free: it changes
 * cells with that neighbor when they have the same size. In the dock, it moves within the dock.
 */
fun LauncherSettings.stepped(keys: List<String>, key: String, offset: Int, grid: HomeGrid): LauncherSettings {
    if (isLayoutLocked) return this
    if (key in dock) return moveInDock(key, offset)
    val placed = arranged(keys, grid)
    val order = placed.map { it.key }
    val to = order.indexOf(key) + offset
    if (key !in order || to !in order.indices) return this
    return when (arrangement) {
        HomeArrangement.PACKED -> moveFavorite(order, key, to)
        HomeArrangement.FREE -> freeMove(placed, key, placed[to].cell, grid)
            ?.let { withPositions(grid.orientation, keepingStoredCells(placed, it, grid.orientation)) } ?: this
    }
}

private fun LauncherSettings.moveInDock(key: String, offset: Int): LauncherSettings {
    val from = dock.indexOf(key)
    val to = from + offset
    if (to !in dock.indices) return this
    return copy(dock = dock.toMutableList().apply { add(to, removeAt(from)) })
}

/**
 * Whether [key] dropped on [target] stays there, the same rule as [dropped]: packed always takes it (in that place
 * of the order); free takes it when its cells are free, or when it changes places with an item of its size. A full
 * home screen takes no new app.
 */
fun LauncherSettings.canDrop(keys: List<String>, key: String, target: HomeCell, grid: HomeGrid): Boolean {
    val isOutside = key !in keys
    val isFull = isOutside && key !in favorites && isHomeFull
    return when {
        isFull -> false
        arrangement == HomeArrangement.PACKED -> true
        else -> freeMove(arranged(if (isOutside) keys + key else keys, grid), key, target, grid) != null
    }
}

/**
 * The free cells after a move ([moved]: every item of [placed] as [freeMove] leaves it). The items that changed cell
 * get the new one, an item without a stored cell gets the one it is shown in (so nothing moves on its own later), and
 * every other item keeps its stored cell, even one this grid cannot show: with larger icons there are fewer columns
 * and rows, and an item of the last ones, shown elsewhere meanwhile, goes back to its cell with the smaller icons.
 */
private fun LauncherSettings.keepingStoredCells(
    placed: List<PlacedHomeItem>,
    moved: Map<String, HomeCell>,
    orientation: HomeOrientation
): Map<String, HomeCell> {
    val shown = placed.associate { it.key to it.cell }
    val changed = moved.filter { (key, cell) -> shown[key] != cell }
    return shown + positionsFor(orientation).filterKeys { it in shown } + changed
}
