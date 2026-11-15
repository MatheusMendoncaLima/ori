package com.oriteam.ori.domain.launcher

import com.oriteam.ori.testing.launcherApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LauncherModelTest {
    private val music = launcherApp("Música")
    private val maps = launcherApp("Mapas")
    private val tree = launcherApp("Árvore")
    private val rice = launcherApp("arroz")
    private val apps = listOf(music, maps, tree, rice)
    private val portuguese = Locale.forLanguageTag("pt-BR")

    @Test
    fun `the drawer is in the language's alphabetical order, without the hidden apps`() {
        val settings = LauncherSettings(hidden = setOf(maps.key))

        assertEquals(listOf(rice, tree, music), drawerApps(apps, settings.hidden, "", portuguese))
    }

    @Test
    fun `search ignores case and accents`() {
        assertEquals(listOf(music), drawerApps(apps, emptySet(), " MUSICA ", portuguese))
        assertEquals(listOf(rice, tree), drawerApps(apps, emptySet(), "ar", portuguese))
        assertEquals(emptyList<LauncherApp>(), drawerApps(apps, emptySet(), "zz", portuguese))
    }

    @Test
    fun `pinning adds at the end, a full home screen takes no more, and pinning again removes`() {
        val full = LauncherSettings(favorites = List(MAX_HOME_ITEMS) { "app$it/.Main@0" })

        assertEquals(listOf(music.key), LauncherSettings().toggleFavorite(music.key).favorites)
        assertEquals(full, full.toggleFavorite(music.key))
        assertEquals(
            emptyList<String>(),
            LauncherSettings(favorites = listOf(music.key)).toggleFavorite(music.key).favorites
        )
    }

    @Test
    fun `a favorite moves one place, and the ends stay`() {
        val settings = LauncherSettings(favorites = listOf(music.key, maps.key, tree.key))

        val shown = settings.favorites

        assertEquals(listOf(maps.key, music.key, tree.key), settings.moveFavorite(shown, maps.key, 0).favorites)
        assertEquals(listOf(maps.key, tree.key, music.key), settings.moveFavorite(shown, music.key, 2).favorites)
        assertEquals(settings, settings.moveFavorite(shown, music.key, -1))
        assertEquals(settings, settings.moveFavorite(shown, tree.key, 3))
        assertEquals(settings, settings.moveFavorite(shown, "unknown", 1))
    }

    @Test
    fun `a removed app keeps its place while the others move around it`() {
        // "maps" is not installed: the screen shows music and tree only.
        val settings = LauncherSettings(favorites = listOf(music.key, maps.key, tree.key))

        val moved = settings.moveFavorite(listOf(music.key, tree.key), tree.key, 0)

        assertEquals(listOf(tree.key, maps.key, music.key), moved.favorites)
    }

    @Test
    fun `a list read before the favorites changed moves nothing`() {
        val settings = LauncherSettings(favorites = listOf(music.key, maps.key, tree.key))

        assertEquals(settings, settings.moveFavorite(listOf(maps.key, music.key), maps.key, 1))
    }

    @Test
    fun `hiding takes the app off the home screen too, and showing brings it back to the drawer only`() {
        val hidden = LauncherSettings(favorites = listOf(music.key)).toggleHidden(music.key)

        assertEquals(setOf(music.key), hidden.hidden)
        assertEquals(emptyList<String>(), hidden.favorites)
        assertEquals(LauncherSettings(), hidden.toggleHidden(music.key))
    }

    @Test
    fun `an item is added once at the end, and a full home screen takes none, the clock included`() {
        val full = LauncherSettings(favorites = List(MAX_HOME_ITEMS) { "app$it/.Main@0" })

        assertEquals(
            listOf(music.key, maps.key),
            LauncherSettings(favorites = listOf(music.key)).withAddedToHome(maps.key).favorites
        )
        assertEquals(
            listOf(music.key),
            LauncherSettings(favorites = listOf(music.key)).withAddedToHome(music.key).favorites
        )
        assertTrue(full.isHomeFull)
        assertEquals(full, full.withAddedToHome(CLOCK_KEY))
    }
}
