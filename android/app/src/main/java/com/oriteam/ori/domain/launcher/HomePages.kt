package com.oriteam.ori.domain.launcher

/** The most pages the home screen takes, as on Android: more would only be scrolled past. */
const val MAX_HOME_PAGES = 12

/** How many pages there are: the ones the person made, or more when the items need them. */
fun LauncherSettings.pagesShown(placed: List<PlacedHomeItem>): Int = maxOf(homePageCount, pageCount(placed))

/** An empty page at the end ("+" while editing, as on One UI). */
fun LauncherSettings.withPageAdded(placed: List<PlacedHomeItem>): LauncherSettings =
    copy(homePageCount = (pagesShown(placed) + 1).coerceAtMost(MAX_HOME_PAGES))

/** The page the Home button goes back to, and where ORI Home opens (the house on One UI). */
fun LauncherSettings.withMainPage(page: Int, placed: List<PlacedHomeItem>): LauncherSettings =
    copy(mainPage = page.coerceIn(0, pagesShown(placed) - 1))

/**
 * Page [page] changes places with the one [step] away (-1 the one before, 1 the one after), as moving a page on One
 * UI: its items go with it, upright and turned alike, and the house goes with its page. Only free icons have pages of
 * their own (packed, the order makes them), and nothing changes on a locked home screen.
 */
fun LauncherSettings.withPageMoved(
    page: Int,
    step: Int,
    placed: List<PlacedHomeItem>,
    orientation: HomeOrientation = HomeOrientation.UPRIGHT
): LauncherSettings {
    val other = page + step
    val pages = 0 until pagesShown(placed)
    val hasOwnPages = !isLayoutLocked && arrangement == HomeArrangement.FREE
    if (!hasOwnPages || page !in pages || other !in pages) return this
    fun swapped(cell: HomeCell) = when (cell.page) {
        page -> cell.copy(page = other)
        other -> cell.copy(page = page)
        else -> cell
    }
    val otherOrientation =
        if (orientation == HomeOrientation.TURNED) HomeOrientation.UPRIGHT else HomeOrientation.TURNED
    return withPositions(orientation, placed.associate { it.key to swapped(it.cell) })
        .withPositions(otherOrientation, positionsFor(otherOrientation).mapValues { (_, cell) -> swapped(cell) })
        .copy(
            mainPage = when (mainPage) {
                page -> other
                other -> page
                else -> mainPage
            }
        )
}

/**
 * Page [page] taken away, as it is shown ([placed], with the screen held as [orientation]). Its items leave the home
 * screen (the apps stay in all the apps; the widgets are the ids to give back to Android), and the pages after it move
 * one back, upright and turned alike. The last page is never taken away.
 */
fun LauncherSettings.withoutPage(
    page: Int,
    placed: List<PlacedHomeItem>,
    orientation: HomeOrientation = HomeOrientation.UPRIGHT
): PageRemoval {
    val shown = pagesShown(placed)
    if (isLayoutLocked || shown <= 1 || page !in 0 until shown) return PageRemoval(this, emptyList())
    val removed = placed.filter { it.cell.page == page }.map { it.key }.toSet()
    fun shift(cell: HomeCell) = if (cell.page > page) cell.copy(page = cell.page - 1) else cell
    // Free: every cell is kept, moved back a page after the one removed. Packed: the order does it on its own.
    val cells = if (arrangement == HomeArrangement.FREE) {
        placed.filter { it.key !in removed }.associate { it.key to shift(it.cell) }
    } else {
        (positionsFor(orientation) - removed).mapValues { (_, cell) -> shift(cell) }
    }
    val other = if (orientation == HomeOrientation.TURNED) HomeOrientation.UPRIGHT else HomeOrientation.TURNED
    val otherCells = (positionsFor(other) - removed).mapValues { (_, cell) -> shift(cell) }
    val widgetIds = removed.mapNotNull(::widgetIdOf)
    val settings = withPositions(orientation, cells).withPositions(other, otherCells).copy(
        favorites = favorites - removed,
        widgetSpans = widgetSpans - widgetIds.toSet(),
        homePageCount = shown - 1,
        mainPage = when {
            mainPage > page -> mainPage - 1
            mainPage == page -> 0
            else -> mainPage
        }
    )
    return PageRemoval(settings, widgetIds)
}

/** The settings after a page was taken away, and the widgets on it, whose ids go back to Android. */
data class PageRemoval(val settings: LauncherSettings, val widgetIds: List<Int>)
