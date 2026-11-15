package com.oriteam.ori.domain.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The levels of stimulus of ORI Home, as the presets of a game's graphics: each one is a set of choices. */
class StimulusTest {
    @Test
    fun `each level is told apart by its choices, and choices of no level are custom`() {
        StimulusLevel.entries.forEach { assertEquals(it, it.choices.level) }
        assertEquals(StimulusLevel.STANDARD, StimulusChoices().level)
        assertNull(StimulusLevel.LOW.choices.copy(colorfulIcons = false).level)
    }

    @Test
    fun `each level takes away more, animations first, then all of them, then the wallpaper and the colors`() {
        assertEquals(StimulusChoices(motion = StimulusMotion.REDUCED), StimulusLevel.MEDIUM.choices)
        assertEquals(StimulusChoices(motion = StimulusMotion.OFF), StimulusLevel.LOW.choices)
        val none = StimulusLevel.NONE.choices
        assertEquals(StimulusMotion.OFF, none.motion)
        assertFalse(none.showsWallpaper)
        assertFalse(none.colorfulIcons)
        assertTrue(none.isSimplified)
    }

    @Test
    fun `without the wallpaper the home screen has the theme, even over Android's, and the chosen one is kept`() {
        val settings = LauncherSettings(wallpaper = LauncherWallpaper.System)
        assertEquals(LauncherWallpaper.System, settings.wallpaperShown)
        assertFalse(settings.isMonochrome)

        val none = settings.copy(stimulus = StimulusLevel.NONE.choices)
        assertEquals(LauncherWallpaper.Theme, none.wallpaperShown)
        assertTrue(none.isMonochrome)
        assertEquals(LauncherWallpaper.System, none.wallpaper)
    }
}
