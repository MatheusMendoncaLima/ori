package com.oriteam.ori.domain.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A widget resized keeps its corner and grows only into free cells, within what its app allows; a page moved changes
 * places with its neighbor, with its items (upright and turned) and the house. A locked home screen keeps all of it.
 */
class WidgetResizeAndPagesTest {
    private val grid = HomeGrid(columns = 4, rows = 4)
    private val widget = widgetKey(7)
    private val free = LauncherSettings(
        favorites = listOf(widget, "a", "b"),
        arrangement = HomeArrangement.FREE,
        widgetSpans = mapOf(7 to HomeSpan(2, 2)),
        positions = mapOf(widget to HomeCell(0, 0, 0), "a" to HomeCell(0, 0, 3), "b" to HomeCell(1, 0, 0))
    )
    private val keys = free.favorites

    @Test
    fun `a widget grows into free cells, keeping its corner and every other item in its cell`() {
        val taller = free.withWidgetResized(keys, 7, HomeSpan(2, 3), grid)

        assertEquals(HomeSpan(2, 3), taller.widgetSpans[7])
        assertEquals(HomeCell(0, 0, 0), taller.positions[widget])
        assertEquals(HomeCell(0, 0, 3), taller.positions["a"])
        assertEquals(HomeCell(1, 0, 0), taller.positions["b"])
    }

    @Test
    fun `a widget does not grow over another item, past the page, or on a locked home screen`() {
        assertFalse(free.canResizeWidget(keys, 7, HomeSpan(4, 2), grid))
        assertEquals(free, free.withWidgetResized(keys, 7, HomeSpan(4, 2), grid))
        assertTrue(free.canResizeWidget(keys, 7, HomeSpan(3, 2), grid))

        val locked = free.copy(isLayoutLocked = true)
        assertEquals(locked, locked.withWidgetResized(keys, 7, HomeSpan(3, 2), grid))
        // Packed, the items after it make room.
        val packed = free.copy(arrangement = HomeArrangement.PACKED)
        assertEquals(HomeSpan(4, 2), packed.withWidgetResized(keys, 7, HomeSpan(4, 2), grid).widgetSpans[7])
    }

    @Test
    fun `the sizes offered are the app's limits within the page, and only the ways it allows`() {
        val resize = WidgetResize(HomeSpan(2, 1), HomeSpan(3, 9), isHorizontal = true, isVertical = false)
        val current = HomeSpan(2, 2)

        assertEquals(2..3, resize.columnsFor(current, grid))
        assertEquals(2..2, resize.rowsFor(current, grid))
        // A size outside what the app asks stays offered while it is the one in use.
        assertEquals(1..3, resize.copy(min = HomeSpan(3, 1)).columnsFor(HomeSpan(1, 1), grid))
    }

    @Test
    fun `a page moved changes places with its neighbor, its items and the house going with it`() {
        val pages = free.copy(mainPage = 0, positionsTurned = mapOf("b" to HomeCell(1, 2, 2)))
        val placed = pages.arranged(keys, grid)

        val moved = pages.withPageMoved(0, 1, placed)

        assertEquals(HomeCell(1, 0, 0), moved.positions[widget])
        assertEquals(HomeCell(1, 0, 3), moved.positions["a"])
        assertEquals(HomeCell(0, 0, 0), moved.positions["b"])
        assertEquals(HomeCell(0, 2, 2), moved.positionsTurned["b"])
        assertEquals(1, moved.mainPage)
    }

    @Test
    fun `the first page goes no earlier, the last no later, and packed or locked pages do not move`() {
        val placed = free.arranged(keys, grid)

        assertEquals(free, free.withPageMoved(0, -1, placed))
        assertEquals(free, free.withPageMoved(1, 1, placed))
        val packed = free.copy(arrangement = HomeArrangement.PACKED)
        assertEquals(packed, packed.withPageMoved(0, 1, packed.arranged(keys, grid)))
        val locked = free.copy(isLayoutLocked = true)
        assertEquals(locked, locked.withPageMoved(0, 1, placed))
    }
}
