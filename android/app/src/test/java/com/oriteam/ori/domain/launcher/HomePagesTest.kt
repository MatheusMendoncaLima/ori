package com.oriteam.ori.domain.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pages as on One UI: added with "+", taken away with their items, a main page; and apps coming from all the apps. */
class HomePagesTest {
    private val grid = HomeGrid(columns = 2, rows = 2)

    @Test
    fun `there are the pages made, or more when the items need them, and plus adds an empty one`() {
        val settings = LauncherSettings(favorites = List(5) { "app$it" }, homePageCount = 1)
        val placed = settings.arranged(settings.favorites, grid)

        assertEquals(2, settings.pagesShown(placed))
        assertEquals(3, settings.withPageAdded(placed).homePageCount)
        assertEquals(4, settings.copy(homePageCount = 4).pagesShown(placed))
    }

    @Test
    fun `a free page taken away takes its items, and the pages after it move back`() {
        val positions = mapOf("a" to HomeCell(0, 0, 0), "b" to HomeCell(1, 0, 0), "c" to HomeCell(2, 1, 1))
        val settings = LauncherSettings(
            favorites = listOf("a", "b", "c"),
            arrangement = HomeArrangement.FREE,
            positions = positions,
            homePageCount = 3,
            mainPage = 2
        )
        val placed = settings.arranged(settings.favorites, grid)

        val removal = settings.withoutPage(1, placed)

        assertEquals(listOf("a", "c"), removal.settings.favorites)
        assertEquals(HomeCell(1, 1, 1), removal.settings.positions["c"])
        assertEquals(2, removal.settings.homePageCount)
        assertEquals(1, removal.settings.mainPage)
    }

    @Test
    fun `a widget on a page taken away gives its id back, and the last page never goes`() {
        val settings = LauncherSettings(
            favorites = listOf("a", widgetKey(9)),
            widgetSpans = mapOf(9 to HomeSpan(2, 2)),
            homePageCount = 1
        )
        val placed = settings.arranged(settings.favorites, grid)
        assertEquals(2, settings.pagesShown(placed))

        val removal = settings.withoutPage(1, placed)

        assertEquals(listOf(9), removal.widgetIds)
        assertFalse(9 in removal.settings.widgetSpans)
        val alone = removal.settings
        assertEquals(alone, alone.withoutPage(0, alone.arranged(alone.favorites, grid)).settings)
    }

    @Test
    fun `the main page stays inside the pages`() {
        val settings = LauncherSettings(favorites = listOf("a"), homePageCount = 3)
        val placed = settings.arranged(settings.favorites, grid)

        assertEquals(2, settings.withMainPage(2, placed).mainPage)
        assertEquals(2, settings.withMainPage(7, placed).mainPage)
    }

    @Test
    fun `an app carried from all the apps joins the home screen where it is dropped`() {
        val packed = LauncherSettings(favorites = listOf("a", "b"))
        assertEquals(
            listOf("new", "a", "b"),
            packed.dropped(packed.favorites, "new", HomeCell(0, 0, 0), grid).favorites
        )

        val free = packed.copy(arrangement = HomeArrangement.FREE)
        val dropped = free.dropped(free.favorites, "new", HomeCell(1, 1, 0), grid)
        assertTrue("new" in dropped.favorites)
        assertEquals(HomeCell(1, 1, 0), dropped.positions["new"])
    }

    @Test
    fun `a full home screen takes no app from all the apps`() {
        val full = LauncherSettings(favorites = List(MAX_HOME_ITEMS) { "app$it" })

        assertEquals(full, full.dropped(full.favorites, "new", HomeCell(0, 0, 0), grid))
    }

    @Test
    fun `packed takes a drop anywhere, free only on free cells or a swap of the same size`() {
        val packed = LauncherSettings(favorites = listOf("a", "b"))
        assertTrue(packed.canDrop(packed.favorites, "a", HomeCell(0, 0, 1), grid))

        val free = LauncherSettings(
            favorites = listOf("a", "b", widgetKey(4)),
            arrangement = HomeArrangement.FREE,
            widgetSpans = mapOf(4 to HomeSpan(2, 1)),
            positions = mapOf("a" to HomeCell(0, 0, 0), "b" to HomeCell(0, 0, 1), widgetKey(4) to HomeCell(0, 1, 0))
        )
        val keys = free.favorites
        assertTrue("free cell", free.canDrop(keys, "a", HomeCell(1, 0, 0), grid))
        assertTrue("same size swap", free.canDrop(keys, "a", HomeCell(0, 0, 1), grid))
        assertFalse("onto a wider widget", free.canDrop(keys, "a", HomeCell(0, 1, 0), grid))
        assertFalse("widget wider than the room", free.canDrop(keys, widgetKey(4), HomeCell(0, 0, 1), grid))
        assertTrue("from all the apps to a free cell", free.canDrop(keys, "new", HomeCell(1, 1, 1), grid))

        val full = LauncherSettings(favorites = List(MAX_HOME_ITEMS) { "app$it" })
        assertFalse("a full home takes no new app", full.canDrop(full.favorites, "new", HomeCell(0, 0, 0), grid))
        assertTrue("but its own apps still move", full.canDrop(full.favorites, "app0", HomeCell(0, 0, 1), grid))
    }

    @Test
    fun `a page taken away turned moves the next pages back in both arrangements`() {
        val turned = HomeGrid(columns = 2, rows = 2, orientation = HomeOrientation.TURNED)
        val settings = LauncherSettings(
            favorites = listOf("a", "b"),
            arrangement = HomeArrangement.FREE,
            positions = mapOf("a" to HomeCell(0, 0, 0), "b" to HomeCell(2, 0, 0)),
            positionsTurned = mapOf("a" to HomeCell(1, 0, 0), "b" to HomeCell(2, 1, 1)),
            homePageCount = 3
        )
        val placed = settings.arranged(settings.favorites, turned)

        val removal = settings.withoutPage(1, placed, HomeOrientation.TURNED)

        assertEquals(listOf("b"), removal.settings.favorites)
        assertEquals(HomeCell(1, 1, 1), removal.settings.positionsTurned["b"])
        assertEquals(HomeCell(1, 0, 0), removal.settings.positions["b"])
    }
}
