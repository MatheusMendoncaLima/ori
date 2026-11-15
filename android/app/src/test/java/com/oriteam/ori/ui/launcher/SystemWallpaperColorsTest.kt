package com.oriteam.ori.ui.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The preview of an Android wallpaper is drawn from the colors Android gives about it, brought to the brightness
 * Android says the picture has: on the tablet, a dark blue wallpaper whose main color was a light lilac came out light.
 */
@RunWith(AndroidJUnit4::class)
class SystemWallpaperColorsTest {
    private val lilac = Color(0xFFD0C0F0)
    private val navy = Color(0xFF1A2340)

    private fun Color.hsl() = FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }

    @Test
    fun `a dark wallpaper with a light main color is drawn dark, with the same hue`() {
        val toned = listOf(lilac).toned(WallpaperBrightness.DARK).single()

        assertTrue("lightness ${toned.hsl()[2]}", toned.hsl()[2] <= 0.21f)
        assertEquals(lilac.hsl()[0], toned.hsl()[0], 2f)
    }

    @Test
    fun `a light wallpaper with a dark main color is drawn light, and one Android says nothing about as it is`() {
        val toned = listOf(navy).toned(WallpaperBrightness.LIGHT).single()

        assertTrue("lightness ${toned.hsl()[2]}", toned.hsl()[2] >= 0.74f)
        assertEquals(navy.hsl()[0], toned.hsl()[0], 2f)
        assertEquals(listOf(lilac, navy), listOf(lilac, navy).toned(WallpaperBrightness.UNKNOWN))
    }
}
