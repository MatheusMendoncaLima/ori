package com.oriteam.ori.ui.launcher

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Where the menu of an item goes, as on One UI 8: above the item and centered on it; under it when there is no room
 * above; always inside the screen and clear of the system bars (here 50 px at the top and 100 px at the bottom).
 */
class MenuPositionTest {
    private val window = IntSize(1000, 2000)
    private val menu = IntSize(400, 500)
    private val position = AboveFirst(top = 50, bottom = 100, gap = 10)

    private fun placeOver(item: IntRect) = position.calculatePosition(item, window, LayoutDirection.Ltr, menu)

    @Test
    fun `with room above, the menu is above the item, centered on it`() {
        assertEquals(IntOffset(300, 1090), placeOver(IntRect(400, 1600, 600, 1800)))
    }

    @Test
    fun `without room above, the menu goes under the item`() {
        assertEquals(IntOffset(300, 310), placeOver(IntRect(400, 100, 600, 300)))
    }

    @Test
    fun `at the sides and the bottom, the menu stays inside the screen and clear of the bars`() {
        assertEquals(0, placeOver(IntRect(0, 1600, 100, 1800)).x)
        assertEquals(600, placeOver(IntRect(900, 1600, 1000, 1800)).x)
        // A tall item near the top with a tall menu: under it, but never into the bottom bar.
        val tall = position.calculatePosition(
            IntRect(400, 100, 600, 1700),
            window,
            LayoutDirection.Ltr,
            IntSize(400, 900)
        )
        assertEquals(2000 - 100 - 900, tall.y)
    }
}
