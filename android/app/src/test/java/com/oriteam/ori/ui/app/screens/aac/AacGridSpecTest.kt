package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.preferences.SymbolLabelMode
import com.oriteam.ori.domain.preferences.SymbolSize
import com.oriteam.ori.ui.adaptive.windowLayoutFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The width the grid really receives on a 360 dp phone: 360 minus 24 dp margins on each side. */
private const val COMMON_PHONE_GRID_WIDTH = 312

/** The same on a larger, 411 dp phone. */
private const val LARGE_PHONE_GRID_WIDTH = 363

class AacGridSpecTest {

    private val phoneTileWidth = windowLayoutFor(widthDp = 360, heightDp = 780).aacSymbolMinWidth

    private fun spec(
        width: Int,
        size: SymbolSize = SymbolSize.MEDIUM,
        labelMode: SymbolLabelMode = SymbolLabelMode.BELOW_IMAGE,
        fontScale: Float = 1f
    ) = aacGridSpec(width.dp, phoneTileWidth, symbolSize = size, labelMode = labelMode, fontScale = fontScale)

    @Test
    fun `more space gives more columns, not giant symbols`() {
        val phone = spec(width = COMMON_PHONE_GRID_WIDTH)
        val tablet = spec(width = 1000)

        assertTrue(tablet.columns > phone.columns)
        assertTrue(tablet.imageSize <= 160.dp)
    }

    @Test
    fun `on phones each symbol size gives a different number of columns`() {
        listOf(COMMON_PHONE_GRID_WIDTH, LARGE_PHONE_GRID_WIDTH).forEach { width ->
            assertEquals(4, spec(width, SymbolSize.SMALL).columns)
            assertEquals(3, spec(width, SymbolSize.MEDIUM).columns)
            assertEquals(2, spec(width, SymbolSize.LARGE).columns)
        }
    }

    @Test
    fun `narrow tiles use smaller labels so words fit`() {
        assertTrue(spec(COMMON_PHONE_GRID_WIDTH, SymbolSize.SMALL).usesSmallLabels)
        assertFalse(spec(COMMON_PHONE_GRID_WIDTH, SymbolSize.MEDIUM).usesSmallLabels)
        // Enlarged system text widens the tile, so it never gets the smaller label.
        assertFalse(spec(COMMON_PHONE_GRID_WIDTH, SymbolSize.SMALL, fontScale = 2f).usesSmallLabels)
    }

    @Test
    fun `enlarged system text widens tiles only when labels need it`() {
        // The medium tile already has room for slightly larger text.
        assertEquals(3, spec(COMMON_PHONE_GRID_WIDTH, fontScale = 1.3f).columns)
        // Twice the text: fewer columns, but not a single giant symbol per screen.
        assertEquals(2, spec(COMMON_PHONE_GRID_WIDTH, fontScale = 2f).columns)
        // A small tile has no spare room, so it gives way as soon as the text grows.
        assertEquals(3, spec(COMMON_PHONE_GRID_WIDTH, SymbolSize.SMALL, fontScale = 1.15f).columns)
        // Smaller system text never makes tiles smaller than the chosen size.
        assertEquals(spec(COMMON_PHONE_GRID_WIDTH).columns, spec(COMMON_PHONE_GRID_WIDTH, fontScale = 0.85f).columns)
    }

    @Test
    fun `there is always one column and never too many`() {
        assertEquals(1, spec(width = 80, size = SymbolSize.LARGE, fontScale = 2f).columns)
        assertEquals(MAX_GRID_COLUMNS, spec(width = 3000, size = SymbolSize.SMALL).columns)
    }

    @Test
    fun `text only mode draws no picture`() {
        assertEquals(0.dp, spec(COMMON_PHONE_GRID_WIDTH, labelMode = SymbolLabelMode.TEXT_ONLY).imageSize)
        assertTrue(spec(COMMON_PHONE_GRID_WIDTH, labelMode = SymbolLabelMode.IMAGE_ONLY).imageSize >= 40.dp)
    }
}
