package com.oriteam.ori.domain.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Where items go on the home screen: packed or free, on pages side by side, with widgets, the clock widget and the
 * dock.
 */
class HomeLayoutTest {
    private val pages = HomeGrid(columns = 3, rows = 3, clockSpan = HomeSpan(3, 1))
    private val apps = List(8) { "app$it" }
    private val withClock = listOf(CLOCK_KEY) + apps

    private fun cells(placed: List<PlacedHomeItem>) = placed.associate { it.key to it.cell }

    private fun arrange(keys: List<String>, grid: HomeGrid, settings: LauncherSettings = LauncherSettings()) =
        settings.arranged(keys, grid)

    @Test
    fun `the clock is a widget as wide as the screen asks, and the apps fill the rows after it, then the next page`() {
        val placed = arrange(withClock, pages)

        assertEquals(HomeCell(0, 0, 0), cells(placed)[CLOCK_KEY])
        assertEquals(HomeSpan(3, 1), placed.first { it.key == CLOCK_KEY }.span)
        assertEquals(HomeCell(0, 1, 0), cells(placed)["app0"])
        assertEquals(HomeCell(0, 2, 2), cells(placed)["app5"])
        assertEquals(HomeCell(1, 0, 0), cells(placed)["app6"])
        assertEquals(2, pageCount(placed))
    }

    @Test
    fun `without the clock the apps start at the top`() {
        assertEquals(HomeCell(0, 0, 0), cells(arrange(apps, pages))["app0"])
    }

    @Test
    fun `a wide widget never jumps ahead, it leaves a gap and the next ones follow it`() {
        val keys = listOf(CLOCK_KEY, "app0", widgetKey(1), "app1")
        val settings = LauncherSettings(widgetSpans = mapOf(1 to HomeSpan(3, 1)))

        val placed = arrange(keys, pages, settings)

        assertEquals(HomeCell(0, 2, 0), cells(placed)[widgetKey(1)])
        assertEquals(HomeCell(1, 0, 0), cells(placed)["app1"])
    }

    @Test
    fun `a widget bigger than the screen is made to fit`() {
        val settings = LauncherSettings(widgetSpans = mapOf(2 to HomeSpan(9, 9)))

        val placed = arrange(listOf(CLOCK_KEY, widgetKey(2)), pages, settings)

        assertEquals(HomeSpan(3, 3), placed.last().span)
        assertEquals(HomeCell(1, 0, 0), placed.last().cell)
    }

    @Test
    fun `free items stay where they were put, and the rest take the first free cells`() {
        val positions = mapOf("app0" to HomeCell(1, 2, 2), "app1" to HomeCell(0, 1, 0))
        val settings = LauncherSettings(arrangement = HomeArrangement.FREE, positions = positions)

        val placed = arrange(listOf(CLOCK_KEY, "app0", "app1", "app2"), pages, settings)

        assertEquals(HomeCell(1, 2, 2), cells(placed)["app0"])
        assertEquals(HomeCell(0, 1, 0), cells(placed)["app1"])
        assertEquals(HomeCell(0, 1, 1), cells(placed)["app2"])
    }

    @Test
    fun `a stored cell under the clock, outside the grid or taken goes to the first free cell`() {
        val positions = mapOf(
            CLOCK_KEY to HomeCell(0, 0, 0),
            "app0" to HomeCell(0, 0, 0),
            "app1" to HomeCell(0, 1, 7),
            "app2" to HomeCell(0, 1, 1),
            "app3" to HomeCell(0, 1, 1)
        )
        val settings = LauncherSettings(arrangement = HomeArrangement.FREE, positions = positions)

        val placed = arrange(listOf(CLOCK_KEY, "app0", "app1", "app2", "app3"), pages, settings)

        val expected = setOf(HomeCell(0, 1, 0), HomeCell(0, 1, 1), HomeCell(0, 1, 2), HomeCell(0, 2, 0))
        assertEquals(expected, (cells(placed) - CLOCK_KEY).values.toSet())
        assertEquals(HomeCell(0, 1, 1), cells(placed)["app2"])
    }

    @Test
    fun `free move goes to an empty cell, swaps with an item of its size, and refuses what does not fit`() {
        val keys = listOf("app0", "app1", widgetKey(3))
        val settings = LauncherSettings(arrangement = HomeArrangement.FREE, widgetSpans = mapOf(3 to HomeSpan(2, 2)))
        val placed = arrange(keys, pages, settings)

        assertEquals(HomeCell(1, 2, 0), freeMove(placed, "app0", HomeCell(1, 2, 0), pages)!!["app0"])
        val swapped = freeMove(placed, "app0", cells(placed).getValue("app1"), pages)!!
        assertEquals(cells(placed)["app1"], swapped["app0"])
        assertEquals(cells(placed)["app0"], swapped["app1"])
        assertNull(freeMove(placed, widgetKey(3), HomeCell(0, 0, 2), pages))
        assertNull(freeMove(placed, "app0", cells(placed).getValue(widgetKey(3)), pages))
    }

    @Test
    fun `dropping in packed takes the place of the item there, in free stores every cell`() {
        val packed = LauncherSettings(favorites = listOf("app0", "app1", "app2"))
        val moved = packed.dropped(packed.favorites, "app0", HomeCell(0, 0, 2), pages)
        assertEquals(listOf("app1", "app2", "app0"), moved.favorites)

        val free = packed.copy(arrangement = HomeArrangement.FREE)
        val placedFree = free.dropped(free.favorites, "app0", HomeCell(1, 1, 1), pages)
        assertEquals(free.favorites, placedFree.favorites)
        assertEquals(HomeCell(1, 1, 1), placedFree.positions["app0"])
        assertEquals(HomeCell(0, 0, 1), placedFree.positions["app1"])
    }

    @Test
    fun `the screen reader moves an item one place in reading order, and in the dock within the dock`() {
        val settings = LauncherSettings(favorites = listOf("app0", "app1", "app2"), dock = listOf("app5", "app6"))

        assertEquals(listOf("app1", "app0", "app2"), settings.stepped(settings.favorites, "app0", 1, pages).favorites)
        assertEquals(settings, settings.stepped(settings.favorites, "app0", -1, pages))
        assertEquals(listOf("app6", "app5"), settings.stepped(settings.favorites, "app5", 1, pages).dock)
    }

    @Test
    fun `an app dropped on the dock leaves the pages, and one dropped on a page leaves the dock`() {
        val settings = LauncherSettings(favorites = listOf("app0", "app1"), dock = listOf("app5"))

        val docked = settings.droppedOnDock("app0", index = 0, capacity = 4)
        assertEquals(listOf("app0", "app5"), docked.dock)
        assertEquals(listOf("app1"), docked.favorites)

        val back = docked.dropped(docked.favorites, "app5", HomeCell(0, 0, 0), pages)
        assertEquals(listOf("app0"), back.dock)
        assertEquals(listOf("app5", "app1"), back.favorites)
    }

    @Test
    fun `a full dock takes no new app, and widgets and the clock never go there`() {
        val full = LauncherSettings(favorites = listOf("app0", CLOCK_KEY, widgetKey(1)), dock = listOf("app5", "app6"))

        assertEquals(full, full.droppedOnDock("app0", 0, capacity = 2))
        assertEquals(full, full.droppedOnDock(CLOCK_KEY, 0, capacity = 6))
        assertEquals(full, full.droppedOnDock(widgetKey(1), 0, capacity = 6))
        assertEquals(listOf("app6", "app5"), full.droppedOnDock("app5", 2, capacity = 2).dock)
        assertFalse(isDockable(CLOCK_KEY))
    }

    @Test
    fun `the clock goes to the start once, and taken out it does not come back`() {
        val first = LauncherSettings(favorites = listOf("app0")).withClockSetUp()
        assertEquals(listOf(CLOCK_KEY, "app0"), first.favorites)

        val removed = first.removedFromHome(CLOCK_KEY)
        assertEquals(removed, removed.withClockSetUp())
    }

    @Test
    fun `a wallpaper photo goes first in the history, the one before stays, and the oldest beyond eight is deleted`() {
        val full = LauncherSettings(
            wallpaper = LauncherWallpaper.Photo("p0"),
            wallpaperHistory = List(MAX_WALLPAPER_HISTORY) { "p$it" }
        )

        val change = full.withWallpaper(LauncherWallpaper.Photo("new"))

        assertEquals("new", change.settings.wallpaperHistory.first())
        assertEquals(MAX_WALLPAPER_HISTORY, change.settings.wallpaperHistory.size)
        assertEquals(listOf("p${MAX_WALLPAPER_HISTORY - 1}"), change.unusedFiles)
    }

    @Test
    fun `the photo in use is never taken out of the history`() {
        val settings = LauncherSettings(
            wallpaper = LauncherWallpaper.Photo("p0"),
            wallpaperHistory = listOf("p0", "p1")
        )

        assertEquals(emptyList<String>(), settings.withoutHistoryPhoto("p0").unusedFiles)
        assertEquals(listOf("p1"), settings.withoutHistoryPhoto("p1").unusedFiles)
        assertEquals(listOf("p0"), settings.withoutHistoryPhoto("p1").settings.wallpaperHistory)
    }

    @Test
    fun `a span is fitted to the grid, never wider than its columns or taller than its rows`() {
        val phone = HomeGrid(columns = 3, rows = 4)

        assertEquals(HomeSpan(3, 2), HomeSpan(4, 2).fittedTo(phone))
        assertEquals(HomeSpan(2, 4), HomeSpan(2, 9).fittedTo(phone))
    }

    @Test
    fun `a free icon keeps a cell a smaller grid cannot show when another one is moved there`() {
        val upright = HomeGrid(columns = 3, rows = 6)
        val turned = HomeGrid(columns = 3, rows = 3)
        val settings = LauncherSettings(
            favorites = listOf("app0", "app1"),
            arrangement = HomeArrangement.FREE,
            positions = mapOf("app0" to HomeCell(0, 0, 0), "app1" to HomeCell(0, 5, 2))
        )

        val moved = settings.dropped(settings.favorites, "app0", HomeCell(0, 1, 1), turned)

        assertEquals(HomeCell(0, 1, 1), moved.positions["app0"])
        assertEquals(HomeCell(0, 5, 2), moved.positions["app1"])
        assertEquals(HomeCell(0, 5, 2), cells(moved.arranged(moved.favorites, upright))["app1"])
    }

    @Test
    fun `turned, an item without a cell of its own goes to its upright page, in the upright reading order`() {
        val upright = HomeGrid(columns = 2, rows = 3)
        val turned = HomeGrid(columns = 3, rows = 2, orientation = HomeOrientation.TURNED)
        val settings = LauncherSettings(
            favorites = listOf("a", "b", "c"),
            arrangement = HomeArrangement.FREE,
            positions = mapOf("a" to HomeCell(1, 0, 0), "b" to HomeCell(0, 2, 1), "c" to HomeCell(0, 0, 0))
        )

        val lying = cells(settings.arranged(settings.favorites, turned))

        assertEquals(HomeCell(0, 0, 0), lying["c"])
        assertEquals(HomeCell(0, 0, 1), lying["b"])
        assertEquals(HomeCell(1, 0, 0), lying["a"])
        assertEquals(HomeCell(1, 0, 0), cells(settings.arranged(settings.favorites, upright))["a"])
    }

    @Test
    fun `an icon moved turned keeps its upright cell, and one moved upright keeps its turned cell`() {
        val upright = HomeGrid(columns = 2, rows = 3)
        val turned = HomeGrid(columns = 3, rows = 2, orientation = HomeOrientation.TURNED)
        val settings = LauncherSettings(
            favorites = listOf("a", "b"),
            arrangement = HomeArrangement.FREE,
            positions = mapOf("a" to HomeCell(0, 0, 0), "b" to HomeCell(0, 0, 1))
        )

        val movedTurned = settings.dropped(settings.favorites, "a", HomeCell(0, 1, 2), turned)
        assertEquals(HomeCell(0, 1, 2), movedTurned.positionsTurned["a"])
        assertEquals(HomeCell(0, 0, 0), movedTurned.positions["a"])

        val movedUpright = movedTurned.dropped(movedTurned.favorites, "a", HomeCell(0, 2, 0), upright)
        assertEquals(HomeCell(0, 2, 0), movedUpright.positions["a"])
        assertEquals(HomeCell(0, 1, 2), movedUpright.positionsTurned["a"])
    }

    @Test
    fun `an item taken out or put in the dock leaves both arrangements`() {
        val settings = LauncherSettings(
            favorites = listOf("a", "b"),
            arrangement = HomeArrangement.FREE,
            positions = mapOf("a" to HomeCell(0, 0, 0)),
            positionsTurned = mapOf("a" to HomeCell(0, 1, 1))
        )

        val removed = settings.removedFromHome("a")
        assertNull(removed.positions["a"])
        assertNull(removed.positionsTurned["a"])

        val docked = settings.droppedOnDock("a", 0, capacity = 6)
        assertNull(docked.positions["a"])
        assertNull(docked.positionsTurned["a"])
    }

    @Test
    fun `free, the items whose cell the grid cannot show keep their reading order in the first free cells`() {
        // Larger icons: two columns instead of four. "b" (first row) and "c" (second row) have no column here.
        val narrow = HomeGrid(columns = 2, rows = 3)
        val settings = LauncherSettings(
            favorites = listOf("c", "b", "a", "d"),
            arrangement = HomeArrangement.FREE,
            positions = mapOf(
                "a" to HomeCell(0, 0, 0),
                "b" to HomeCell(0, 0, 3),
                "c" to HomeCell(0, 1, 2),
                "d" to HomeCell(0, 2, 0)
            )
        )

        val shown = cells(settings.arranged(settings.favorites, narrow))

        assertEquals(HomeCell(0, 0, 1), shown["b"])
        assertEquals(HomeCell(0, 1, 0), shown["c"])
        assertEquals(HomeCell(0, 2, 0), shown["d"])
    }
}
