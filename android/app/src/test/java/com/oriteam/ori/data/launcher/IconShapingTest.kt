package com.oriteam.ori.data.launcher

import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.ui.launcher.tileColorOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val SIDE = 100

/**
 * Every icon in the shape the person chose, whatever the app brings: an adaptive icon shows the middle of its layers,
 * an older one sits on a white plate, and the corners outside the shape are clear.
 */
@RunWith(AndroidJUnit4::class)
class IconShapingTest {
    /** A point near the corner: outside a circle, inside a square with rounded corners. */
    private val nearCorner = 10

    @Test
    fun `round icons are clear near the corners, rounded ones only at the corner, square ones nowhere`() {
        val round = ColorDrawable(Color.RED).inShape(IconShape.ROUND, SIDE)
        val rounded = ColorDrawable(Color.RED).inShape(IconShape.ROUNDED, SIDE)
        val square = ColorDrawable(Color.RED).inShape(IconShape.SQUARE, SIDE)

        assertEquals(0, Color.alpha(round.getPixel(nearCorner, nearCorner)))
        assertEquals(255, Color.alpha(rounded.getPixel(nearCorner, nearCorner)))
        assertEquals(0, Color.alpha(rounded.getPixel(0, 0)))
        assertEquals(255, Color.alpha(square.getPixel(0, 0)))
        assertEquals(Color.RED, round.getPixel(SIDE / 2, SIDE / 2))
    }

    @Test
    fun `an older icon sits on a white plate, as Android draws it`() {
        val square = ColorDrawable(Color.RED).inShape(IconShape.SQUARE, SIDE)

        assertEquals(Color.WHITE, square.getPixel(nearCorner, SIDE / 2))
        assertEquals(Color.RED, square.getPixel(SIDE / 2, SIDE / 2))
    }

    @Test
    fun `an adaptive icon shows its own background up to the edge of the shape`() {
        val adaptive = AdaptiveIconDrawable(ColorDrawable(Color.BLUE), ColorDrawable(Color.TRANSPARENT))

        val square = adaptive.inShape(IconShape.SQUARE, SIDE)

        assertEquals(Color.BLUE, square.getPixel(nearCorner, SIDE / 2))
        assertEquals(Color.BLUE, square.getPixel(SIDE / 2, SIDE / 2))
    }

    @Test
    fun `a tile takes a calm, dark tone of the icon's color, and grey in low stimulation`() {
        val red = ColorDrawable(Color.RED).inShape(IconShape.SQUARE, SIDE)

        val tile = tileColorOf(red, isMonochrome = false)
        val grey = tileColorOf(red, isMonochrome = true)

        assertTrue("red ${tile.red} green ${tile.green}", tile.red > tile.green && tile.red > tile.blue)
        assertTrue("dark enough for white writing: ${tile.red}", tile.red < 0.7f)
        assertEquals(grey.red, grey.green, 0.01f)
        assertEquals(grey.green, grey.blue, 0.01f)
    }
}
