package com.oriteam.ori.domain.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Folders, as on One UI: an app dropped on another makes a folder in its place; an app dropped on a folder joins it;
 * one taken out goes to the home screen; a folder left with one app becomes that app; a locked home screen keeps them.
 */
class HomeFoldersTest {
    private val grid = HomeGrid(columns = 2, rows = 2)
    private val free = LauncherSettings(
        favorites = listOf("a", "b", "c"),
        dock = listOf("d"),
        arrangement = HomeArrangement.FREE,
        positions = mapOf("a" to HomeCell(0, 0, 0), "b" to HomeCell(0, 0, 1), "c" to HomeCell(0, 1, 0))
    )

    @Test
    fun `an app dropped on another makes a folder in its place, with both`() {
        val folded = free.droppedOnItem("b", "a", "Jogos")

        val key = folderKey(1)
        assertEquals(listOf(key, "c"), folded.favorites)
        assertEquals(HomeCell(0, 0, 0), folded.positions[key])
        assertNull(folded.positions["a"])
        assertNull(folded.positions["b"])
        assertEquals(HomeFolder("Jogos", listOf("a", "b")), folded.folders[1])
        assertEquals(setOf("a", "b"), folded.appsInFolders)
        // Packed too: the folder takes the place of the app in the order.
        val packed = LauncherSettings(favorites = listOf("a", "b", "c")).droppedOnItem("c", "a", "Pasta")
        assertEquals(listOf(key, "b"), packed.favorites)
    }

    @Test
    fun `an app dropped on a folder joins it, from the pages, the dock or all the apps`() {
        val folded = free.droppedOnItem("b", "a", "Jogos")

        val fromPage = folded.droppedOnItem("c", folderKey(1), "")
        assertEquals(listOf("a", "b", "c"), fromPage.folders.getValue(1).apps)
        assertEquals(listOf(folderKey(1)), fromPage.favorites)

        val fromDock = folded.droppedOnItem("d", folderKey(1), "")
        assertTrue("d" in fromDock.folders.getValue(1).apps)
        assertTrue(fromDock.dock.isEmpty())

        val fromAllApps = folded.withAddedToFolder(1, "e")
        assertEquals(listOf("a", "b", "e"), fromAllApps.folders.getValue(1).apps)
    }

    @Test
    fun `an app taken out goes to the home screen, and a folder left with one app becomes it`() {
        val folded = free.droppedOnItem("b", "a", "Jogos")

        val out = folded.withTakenOutOfFolder("b")

        assertNull(out.folders[1])
        assertEquals(listOf("a", "c", "b"), out.favorites)
        assertEquals(HomeCell(0, 0, 0), out.positions["a"])
    }

    @Test
    fun `an app dropped from a folder on a cell or on the dock leaves the folder`() {
        val three = free.droppedOnItem("b", "a", "Jogos").droppedOnItem("c", folderKey(1), "")
        val keys = three.favorites

        val onCell = three.dropped(keys, "c", HomeCell(0, 1, 1), grid)
        assertEquals(listOf("a", "b"), onCell.folders.getValue(1).apps)
        assertEquals(HomeCell(0, 1, 1), onCell.positions["c"])

        val onDock = three.droppedOnDock("c", 0, capacity = 4)
        assertEquals(listOf("c", "d"), onDock.dock)
        assertEquals(listOf("a", "b"), onDock.folders.getValue(1).apps)
    }

    @Test
    fun `an app dropped out of a folder of two leaves the other app in the cell of the folder`() {
        val folded = free.droppedOnItem("b", "a", "Jogos")

        val out = folded.dropped(folded.favorites, "b", HomeCell(0, 1, 1), grid)

        assertTrue(out.folders.isEmpty())
        assertEquals(HomeCell(0, 0, 0), out.positions["a"])
        assertEquals(HomeCell(0, 1, 1), out.positions["b"])
        assertEquals(HomeCell(0, 1, 0), out.positions["c"])
    }

    @Test
    fun `taking a folder off takes its apps off the home screen, and taking an app off takes it out of its folder`() {
        val three = free.droppedOnItem("b", "a", "Jogos").droppedOnItem("c", folderKey(1), "")

        val withoutFolder = three.removedFromHome(folderKey(1))
        assertTrue(withoutFolder.favorites.isEmpty())
        assertTrue(withoutFolder.folders.isEmpty())

        val withoutApp = three.toggleFavorite("c")
        assertEquals(listOf("a", "b"), withoutApp.folders.getValue(1).apps)
    }

    @Test
    fun `made from the menu, a folder starts with its app, and closed with that one alone it is the app again`() {
        val around = free.withFolderAround("c", "Pasta")
        assertEquals(listOf("a", "b", folderKey(1)), around.favorites)
        assertEquals(HomeCell(0, 1, 0), around.positions[folderKey(1)])

        val tidied = around.tidied(1)
        assertEquals(free.favorites, tidied.favorites)
        assertEquals(free.positions, tidied.positions)
        assertTrue(tidied.folders.isEmpty())
    }

    @Test
    fun `the apps chosen in the list of a folder join it, and the ones left out go to the home screen`() {
        val folded = free.droppedOnItem("b", "a", "Jogos")

        val chosen = folded.withFolderChoice(1, listOf("a", "d", "e"))

        assertEquals(listOf("a", "d", "e"), chosen.folders.getValue(1).apps)
        assertTrue(chosen.dock.isEmpty())
        assertTrue("b" in chosen.favorites)
        // Every app left out: the folder becomes its last app, and the others go to the home screen.
        val none = folded.withFolderChoice(1, emptyList())
        assertTrue(none.folders.isEmpty())
        assertTrue(none.favorites.containsAll(listOf("a", "b", "c")))
        assertEquals(folded, folded.withFolderChoice(9, listOf("c")))
    }

    @Test
    fun `a name is one line, not too long, and a blank one keeps the old name`() {
        val folded = free.droppedOnItem("b", "a", "Jogos")

        assertEquals("Meus jogos", folded.withFolderRenamed(1, "  Meus\tjogos\n ").folders.getValue(1).name)
        assertEquals("Jogos", folded.withFolderRenamed(1, "   ").folders.getValue(1).name)
        assertEquals(MAX_FOLDER_NAME, folded.withFolderRenamed(1, "x".repeat(90)).folders.getValue(1).name.length)
    }

    @Test
    fun `a locked home screen keeps its folders as they are, and widgets or the clock never go in one`() {
        val folded = free.droppedOnItem("b", "a", "Jogos")
        val locked = folded.copy(isLayoutLocked = true)

        assertEquals(locked, locked.droppedOnItem("c", folderKey(1), ""))
        assertEquals(locked, locked.withTakenOutOfFolder("a"))
        assertEquals(locked, locked.droppedOnItem("c", "c", ""))
        assertEquals(folded, folded.droppedOnItem(widgetKey(4), "c", ""))
        assertEquals(folded, folded.droppedOnItem(CLOCK_KEY, "c", ""))
        assertFalse(isDockable(folderKey(1)))
    }
}
