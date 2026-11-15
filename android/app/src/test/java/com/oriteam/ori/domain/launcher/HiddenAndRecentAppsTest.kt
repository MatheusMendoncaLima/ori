package com.oriteam.ori.domain.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Ocultar apps" hides the apps checked (they leave the home screen) and brings back the ones unchecked; the recent
 * apps of the dock are the last opened from ORI Home, once each, apps only, not in the dock nor hidden, two at most.
 */
class HiddenAndRecentAppsTest {
    private val home = LauncherSettings(favorites = listOf("a", "b"), dock = listOf("c"), hidden = setOf("x"))

    @Test
    fun `the apps checked are hidden and leave the home screen, and the unchecked come back`() {
        val chosen = home.withHiddenApps(setOf("a", "c"))

        assertEquals(setOf("a", "c"), chosen.hidden)
        assertEquals(listOf("b"), chosen.favorites)
        assertTrue(chosen.dock.isEmpty())
        assertEquals(home.copy(hidden = emptySet()), home.withHiddenApps(emptySet()))
    }

    @Test
    fun `an app opened goes first, once, and a shortcut does not count`() {
        val opened = emptyList<String>().withOpened("a").withOpened("b").withOpened("a")

        assertEquals(listOf("a", "b"), opened)
        assertEquals(opened, opened.withOpened("${SHORTCUT_PREFIX}0/app/x"))
    }

    @Test
    fun `the dock shows the recent apps not in it nor hidden, two at most, and none while off`() {
        val installed = listOf("a", "b", "c", "d", "x").associateWith { LauncherApp(it, it, it.uppercase()) }
        val on = home.copy(showsRecentApps = true)

        val shown = recentApps(listOf("c", "x", "d", "a", "b"), installed, on)

        assertEquals(listOf("d", "a"), shown.map { it.key })
        assertTrue(recentApps(listOf("d"), installed, home).isEmpty())
        // They are part of the dock: without the dock there are none.
        assertTrue(recentApps(listOf("d"), installed, on.copy(showsDock = false)).isEmpty())
    }
}
