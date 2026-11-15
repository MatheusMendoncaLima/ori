package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.CLOCK_KEY
import com.oriteam.ori.domain.launcher.HomeArrangement
import com.oriteam.ori.domain.launcher.HomeCell
import com.oriteam.ori.domain.launcher.HomeGrid
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.arranged
import com.oriteam.ori.domain.launcher.folderKey
import com.oriteam.ori.domain.launcher.widgetKey
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The small preview of the home screen on the wallpaper screen is the person's own: the columns and rows of this
 * screen, the items of the main page where they are, each as it looks (an app's icon, a folder with its apps, the
 * clock, a widget's block), and the apps of the dock. It used to be gray squares for everything.
 */
class MiniHomeTest {
    @Test
    fun `the preview has the grid, the items of the main page in their cells, and the dock`() {
        val grid = HomeGrid(columns = 6, rows = 7, clockSpan = HomeSpan(2, 1))
        val settings = LauncherSettings(
            favorites = listOf(CLOCK_KEY, "a", widgetKey(3), "b"),
            arrangement = HomeArrangement.FREE,
            widgetSpans = mapOf(3 to HomeSpan(2, 2)),
            positions = mapOf(
                CLOCK_KEY to HomeCell(0, 0, 0),
                "a" to HomeCell(0, 0, 4),
                widgetKey(3) to HomeCell(0, 3, 1),
                "b" to HomeCell(1, 0, 0)
            ),
            mainPage = 0
        )

        val a = LauncherApp("a", "a", "A")
        val entries = listOf(HomeEntry.Clock, HomeEntry.App(a), HomeEntry.Widget(3)).associateBy { it.key }
        val dock = listOf(LauncherApp("d", "d", "D"))

        val home = miniHomeOf(settings.arranged(settings.favorites, grid), entries, grid, mainPage = 0, dock = dock)

        assertEquals(6, home.columns)
        assertEquals(7, home.rows)
        assertEquals(dock, home.dock)
        assertEquals(
            setOf(
                MiniItem(column = 0, row = 0, columns = 2, rows = 1, look = MiniLook.Clock),
                MiniItem(column = 4, row = 0, columns = 1, rows = 1, look = MiniLook.App(a)),
                MiniItem(column = 1, row = 3, columns = 2, rows = 2, look = MiniLook.Widget)
            ),
            home.items.toSet()
        )
    }

    @Test
    fun `a folder shows its apps, and an app no longer installed is left out`() {
        val grid = HomeGrid(columns = 4, rows = 4)
        val apps = listOf(LauncherApp("x", "x", "X"), LauncherApp("y", "y", "Y"))
        val settings = LauncherSettings(favorites = listOf(folderKey(1), "gone"))
        val entries = listOf(HomeEntry.Folder(1, "Jogos", apps)).associateBy { it.key }

        val home = miniHomeOf(
            settings.arranged(settings.favorites, grid),
            entries,
            grid,
            mainPage = 0,
            dock = emptyList()
        )

        assertEquals(listOf(MiniItem(0, 0, 1, 1, MiniLook.Folder(apps))), home.items)
    }
}
