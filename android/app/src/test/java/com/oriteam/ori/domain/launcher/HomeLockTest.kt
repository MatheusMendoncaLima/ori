package com.oriteam.ori.domain.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Travar a tela inicial", as on One UI: nothing on a locked home screen moves or leaves it, whatever asks (a drop, the
 * dock, the screen reader, a menu, a page taken away); adding still works. And packed puts the clock first.
 */
class HomeLockTest {
    private val grid = HomeGrid(columns = 2, rows = 2)
    private val locked = LauncherSettings(
        favorites = listOf("a", "b", "c"),
        dock = listOf("d"),
        arrangement = HomeArrangement.FREE,
        positions = mapOf("a" to HomeCell(0, 0, 0), "b" to HomeCell(0, 0, 1), "c" to HomeCell(1, 0, 0)),
        homePageCount = 2,
        isLayoutLocked = true
    )
    private val keys = locked.favorites

    @Test
    fun `a locked home screen keeps every item where it is`() {
        assertEquals(locked, locked.dropped(keys, "a", HomeCell(0, 1, 1), grid))
        assertEquals(locked, locked.droppedOnDock("a", 0, capacity = 4))
        assertEquals(locked, locked.droppedOnDock("d", 1, capacity = 4))
        assertEquals(locked, locked.stepped(keys, "a", 1, grid))
        assertEquals(locked, locked.stepped(keys, "d", -1, grid))
    }

    @Test
    fun `a locked home screen keeps every item on it, and its pages`() {
        assertEquals(locked, locked.removedFromHome("a"))
        assertEquals(locked, locked.removedFromHome("d"))
        assertEquals(locked, locked.toggleFavorite("a"))
        val placed = locked.arranged(keys, grid)
        assertEquals(locked, locked.withoutPage(1, placed).settings)
        assertTrue(locked.withoutPage(1, placed).widgetIds.isEmpty())
    }

    @Test
    fun `an app is still added to a locked home screen, and unlocked everything moves again`() {
        assertTrue("e" in locked.toggleFavorite("e").favorites)

        val unlocked = locked.copy(isLayoutLocked = false)
        assertEquals(HomeCell(0, 1, 1), unlocked.dropped(keys, "a", HomeCell(0, 1, 1), grid).positions["a"])
        assertTrue("a" !in unlocked.removedFromHome("a").favorites)
    }

    @Test
    fun `packed puts the clock first, never between icons, and free keeps the cells`() {
        val free = LauncherSettings(
            favorites = listOf("a", CLOCK_KEY, "b"),
            arrangement = HomeArrangement.FREE,
            positions = mapOf(CLOCK_KEY to HomeCell(0, 0, 0))
        )

        val packed = free.withArrangement(HomeArrangement.PACKED)

        assertEquals(listOf(CLOCK_KEY, "a", "b"), packed.favorites)
        assertEquals(free.positions, packed.positions)
        assertEquals(packed.favorites, packed.withArrangement(HomeArrangement.FREE).favorites)
        val noClock = LauncherSettings(favorites = listOf("a", "b"), arrangement = HomeArrangement.FREE)
        assertEquals(listOf("a", "b"), noClock.withArrangement(HomeArrangement.PACKED).favorites)
    }
}
