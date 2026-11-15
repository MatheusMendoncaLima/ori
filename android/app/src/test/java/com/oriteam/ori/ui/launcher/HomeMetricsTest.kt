package com.oriteam.ori.ui.launcher

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.launcher.GridChoice
import com.oriteam.ori.domain.launcher.HomeOrientation
import com.oriteam.ori.domain.launcher.LauncherIconSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The grid of the home screen on windows of many shapes: a phone, a foldable, a tablet upright and turned (the
 * upright grid transposed, as on One UI), short screens and large text. A cell is never shorter than what it shows.
 */
class HomeMetricsTest {
    /** A room with the name's line of medium icons (titleSmall), the line of "Remover" and a clock of a common size. */
    private fun room(width: Dp, height: Dp, labelLine: Dp = 20.dp, stripLine: Dp = 20.dp, hasDock: Boolean = true) =
        HomeRoom(width, height, labelLine, stripLine, DpSize(240.dp, 104.dp), hasDock)

    private fun metrics(
        width: Dp,
        height: Dp,
        size: LauncherIconSize = LauncherIconSize.MEDIUM,
        labelLine: Dp = 20.dp,
        stripLine: Dp = 20.dp
    ) = homeMetricsFor(room(width, height, labelLine, stripLine), size)

    /** What a cell shows: the icon, its name and the room around it. */
    private fun HomeMetrics.contentHeight(labelLine: Dp = 20.dp) =
        if (showsLabels) iconSize + labelLine * labelLines + 24.dp else iconSize + 16.dp

    /** The cells, the strip over them and the dots and the dock under them, together. */
    private fun HomeMetrics.usedHeight() = topStripHeight + cellHeight * grid.rows + bottomHeight

    private fun assertWholeCells(metrics: HomeMetrics, labelLine: Dp = 20.dp) {
        val needed = metrics.contentHeight(labelLine)
        assertTrue("cell ${metrics.cellHeight} < $needed", metrics.cellHeight >= needed)
    }

    @Test
    fun `a phone fills its height with whole rows of icons and two-line names, with One UI's icon size`() {
        val phone = metrics(360.dp, 800.dp)

        // One UI's phone grid, with the icons of One UI's size (58 dp, measured on the test tablet).
        assertEquals(4, phone.grid.columns)
        assertEquals(5, phone.grid.rows)
        assertEquals(2, phone.labelLines)
        assertEquals(58.dp, phone.iconSize)
        assertWholeCells(phone)
    }

    @Test
    fun `huge icons are twice the size of medium ones and stay so, upright and turned, in fewer cells`() {
        val upright = room(820.dp, 1235.dp, labelLine = 24.dp)
        val phone = metrics(360.dp, 800.dp, LauncherIconSize.EXTRA_LARGE, labelLine = 24.dp)
        val tablet = homeMetricsFor(upright, LauncherIconSize.EXTRA_LARGE)
        val turned = homeMetricsFor(
            room(1313.dp, 742.dp, labelLine = 24.dp),
            LauncherIconSize.EXTRA_LARGE,
            upright = upright
        )

        assertTrue(LauncherIconSize.EXTRA_LARGE.iconDp >= LauncherIconSize.MEDIUM.iconDp * 2)
        listOf(phone, tablet, turned).forEach { huge ->
            assertEquals(LauncherIconSize.EXTRA_LARGE.iconDp, huge.iconSize)
            assertTrue(huge.showsLabels)
            assertWholeCells(huge, labelLine = 24.dp)
        }
        assertEquals(2, phone.grid.columns)
        // Turned, fewer rows than the upright columns rather than icons made small again.
        assertTrue("turned rows ${turned.grid.rows}", turned.grid.rows < tablet.grid.columns)
        assertTrue(turned.usedHeight() <= 742.dp)
    }

    @Test
    fun `at Maximum the apps are tiles about square, two by three on a phone and three columns on a tablet`() {
        val phone = metrics(360.dp, 800.dp, LauncherIconSize.MAXIMUM)
        val tablet = metrics(820.dp, 1235.dp, LauncherIconSize.MAXIMUM)

        assertTrue(phone.isTiles)
        assertEquals(2, phone.grid.columns)
        // Three rows, four on a window without system bars.
        assertTrue("rows ${phone.grid.rows}", phone.grid.rows in 3..4)
        assertEquals(3, tablet.grid.columns)
        listOf(phone, tablet).forEach { tiles ->
            assertTrue("cell ${tiles.cellWidth} x ${tiles.cellHeight}", tiles.cellHeight >= tiles.cellWidth * 0.8f)
            assertTrue(tiles.usedHeight() <= if (tiles == phone) 800.dp else 1235.dp)
        }
        assertFalse(metrics(360.dp, 800.dp).isTiles)
    }

    @Test
    fun `a tablet turned has the upright rows as its columns, the icons whole and one-line names`() {
        val upright = room(820.dp, 1235.dp)
        val turned = homeMetricsFor(room(1313.dp, 742.dp), LauncherIconSize.MEDIUM, upright = upright)
        val standing = homeMetricsFor(upright, LauncherIconSize.MEDIUM)

        assertEquals(6, standing.grid.columns)
        assertEquals(8, standing.grid.rows)
        assertEquals(standing.grid.rows, turned.grid.columns)
        // The upright columns as rows, as on One UI, as many as the icons fit whole.
        assertTrue("turned rows ${turned.grid.rows}", turned.grid.rows in 4..standing.grid.columns)
        assertEquals(HomeOrientation.UPRIGHT, standing.grid.orientation)
        assertEquals(HomeOrientation.TURNED, turned.grid.orientation)
        assertEquals(standing.dockCapacity, turned.dockCapacity)
        assertEquals(58.dp, standing.iconSize)
        assertEquals(58.dp, turned.iconSize)
        assertEquals(1, turned.labelLines)
        assertWholeCells(turned)
        assertTrue(turned.usedHeight() <= 742.dp)
    }

    @Test
    fun `turned, the clock takes one row and is drawn at most a little smaller instead of taking two`() {
        val turned = homeMetricsFor(room(1313.dp, 742.dp), LauncherIconSize.MEDIUM, upright = room(820.dp, 1235.dp))

        assertEquals(1, turned.grid.clockSpan.rows)
        val room = turned.cellHeight * turned.grid.clockSpan.rows
        assertTrue("room $room", room >= turned.clockSize.height * 0.85f)
    }

    @Test
    fun `a screen too short for the rows of upright shows fewer rows, with the icons whole`() {
        val short = homeMetricsFor(room(800.dp, 300.dp), LauncherIconSize.MEDIUM, upright = room(360.dp, 800.dp))

        assertEquals(58.dp, short.iconSize)
        assertTrue("rows ${short.grid.rows}", short.grid.rows in 1..3)
        assertWholeCells(short)
        assertTrue(short.usedHeight() <= 300.dp)
    }

    @Test
    fun `a window too short for one row with names makes the icon smaller before taking the names out`() {
        val huge = metrics(800.dp, 340.dp, LauncherIconSize.EXTRA_LARGE, labelLine = 24.dp)

        assertEquals(1, huge.grid.rows)
        assertTrue(huge.showsLabels)
        assertTrue("icon ${huge.iconSize}", huge.iconSize < 104.dp)
        assertEquals(huge.iconSize + 32.dp, huge.dockHeight)
        assertWholeCells(huge, labelLine = 24.dp)
        assertTrue(huge.usedHeight() <= 340.dp)
    }

    @Test
    fun `a tablet upright keeps six columns of medium icons and up to eight rows, as One UI has`() {
        val tablet = metrics(800.dp, 1280.dp)

        assertEquals(6, tablet.grid.columns)
        assertEquals(8, tablet.grid.rows)
        assertWholeCells(tablet)
    }

    @Test
    fun `a foldable and huge icons on a tablet get whole cells too`() {
        assertWholeCells(metrics(673.dp, 841.dp))
        val huge = metrics(800.dp, 1280.dp, LauncherIconSize.EXTRA_LARGE, labelLine = 24.dp)
        assertEquals(4, huge.grid.columns)
        assertWholeCells(huge, labelLine = 24.dp)
    }

    @Test
    fun `the clock takes the columns and rows its size needs`() {
        val tablet = metrics(1280.dp, 800.dp)
        assertEquals(2, tablet.grid.clockSpan.columns)
        assertEquals(1, tablet.grid.clockSpan.rows)

        val phone = metrics(360.dp, 800.dp)
        assertEquals(3, phone.grid.clockSpan.columns)
    }

    @Test
    fun `with text at twice its size the top strip grows, so Remover never covers the first row`() {
        val normal = metrics(360.dp, 800.dp)
        val big = metrics(360.dp, 800.dp, labelLine = 40.dp, stripLine = 40.dp)

        assertEquals(64.dp, normal.topStripHeight)
        // "Remover": its line and padding (16 dp over and under), 8 dp from the top, never over the cells.
        assertTrue("strip ${big.topStripHeight}", big.topStripHeight >= 8.dp + 40.dp + 32.dp)
        assertTrue(big.usedHeight() <= 800.dp)
    }

    @Test
    fun `a phone stays upright and a tablet turns, as One UI does`() {
        assertEquals(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, homeOrientationFor(411))
        assertEquals(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, homeOrientationFor(800))
        // "Girar a tela inicial no celular".
        assertEquals(
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            homeOrientationFor(411, turnsOnPhone = true)
        )
    }

    @Test
    fun `with a grid chosen, a larger icon keeps the cells and fits in them, upright and turned`() {
        val chosen = GridChoice(6, 8)
        val upright = room(800.dp, 1280.dp, labelLine = 24.dp)

        val huge = homeMetricsFor(upright, LauncherIconSize.EXTRA_LARGE, chosen = chosen)
        val turned = homeMetricsFor(
            room(1280.dp, 800.dp, labelLine = 24.dp),
            LauncherIconSize.EXTRA_LARGE,
            upright = upright,
            chosen = chosen
        )

        // Automatic, huge icons give 4 columns on this tablet; with the grid chosen, the cells stay.
        assertEquals(4, homeMetricsFor(upright, LauncherIconSize.EXTRA_LARGE).grid.columns)
        assertEquals(6, huge.grid.columns)
        assertEquals(8, huge.grid.rows)
        assertTrue("icon ${huge.iconSize} in a cell of ${huge.cellWidth}", huge.iconSize <= huge.cellWidth - 16.dp)
        assertWholeCells(huge, labelLine = 24.dp)
        assertEquals(8, turned.grid.columns)
        assertEquals(6, turned.grid.rows)
        assertEquals(6, turned.dockCapacity)
    }

    @Test
    fun `a grid chosen on a wider screen keeps to what this one holds, and the choices come from the icon sizes`() {
        val phone = homeMetricsFor(room(360.dp, 800.dp), LauncherIconSize.MEDIUM, chosen = GridChoice(9, 8))
        // 328 dp of cells: at most 5 cells of 56 dp (the smallest icon and its sides).
        assertTrue("columns ${phone.grid.columns}", phone.grid.columns <= 5)
        assertTrue("icon ${phone.iconSize}", phone.iconSize >= 40.dp)

        val choices = gridChoicesFor(room(800.dp, 1280.dp))
        assertTrue("choices $choices", GridChoice(6, 8) in choices)
        assertEquals(choices.sortedByDescending { it.columns * it.rows }, choices)
        assertEquals(choices.distinct(), choices)
    }

    @Test
    fun `without names the icons take their room, in as many rows or more`() {
        val named = metrics(360.dp, 800.dp)
        val bare = homeMetricsFor(room(360.dp, 800.dp), LauncherIconSize.MEDIUM, showsLabels = false)

        assertEquals(0, bare.labelLines)
        assertFalse(bare.showsLabels)
        assertTrue("rows ${bare.grid.rows}", bare.grid.rows >= named.grid.rows)
        assertWholeCells(bare)
    }

    @Test
    fun `without the dock the same cells get taller, so nothing moves, and only the dots stay under them`() {
        val upright = room(820.dp, 1235.dp)
        val ways = listOf(
            Triple(room(360.dp, 800.dp), null, null),
            Triple(room(820.dp, 1313.dp), null, null),
            Triple(room(1313.dp, 742.dp), upright, null),
            Triple(room(360.dp, 800.dp), null, GridChoice(4, 5))
        )
        ways.forEach { (room, standing, chosen) ->
            LauncherIconSize.entries.forEach { size ->
                val docked = homeMetricsFor(room, size, upright = standing, chosen = chosen)
                val bare = homeMetricsFor(
                    room.copy(hasDock = false),
                    size,
                    upright = standing?.copy(hasDock = false),
                    chosen = chosen
                )

                assertEquals(DotsHeight, bare.bottomHeight)
                assertEquals("$size ${room.width}", docked.grid, bare.grid)
                assertEquals(docked.gridChoices, bare.gridChoices)
                assertTrue("$size cell ${bare.cellHeight} <= ${docked.cellHeight}", bare.cellHeight > docked.cellHeight)
                assertTrue("$size used ${bare.usedHeight()}", bare.usedHeight() <= room.height + 0.5.dp)
                assertWholeCells(bare)
            }
        }
    }
}
