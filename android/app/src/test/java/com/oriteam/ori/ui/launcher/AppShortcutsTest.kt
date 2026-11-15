package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.HomeFolder
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.MAX_HOME_ITEMS
import com.oriteam.ori.domain.launcher.PinRefusal
import com.oriteam.ori.domain.launcher.folderKey
import com.oriteam.ori.domain.launcher.pinRefusal
import com.oriteam.ori.domain.launcher.shortcutKeys
import com.oriteam.ori.domain.launcher.widgetKey
import com.oriteam.ori.domain.launcher.withWidget
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.FakeHomeSystem
import com.oriteam.ori.testing.FakeHomeWidgetHost
import com.oriteam.ori.testing.FakeInstalledApps
import com.oriteam.ori.testing.FakeLauncherSettingsRepository
import com.oriteam.ori.testing.FakeLockScreenWallpaper
import com.oriteam.ori.testing.FakeWallpaperStore
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.launcherApp
import com.oriteam.ori.testing.shortcutOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Shortcuts of apps, as on One UI: the menu of an app offers them, one opens through Android, one goes to the home
 * screen (where it shows with its name of now and is kept pinned), and one taken off is let go. An app asking to pin
 * one is refused on a locked or full home screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppShortcutsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messages = launcherApp("Mensagens")
    private val newMessage = shortcutOf(messages, "Nova mensagem")
    private val apps = FakeInstalledApps(listOf(messages)).apply {
        shortcuts.value = mapOf(messages.key to listOf(newMessage))
    }
    private val settings = FakeLauncherSettingsRepository(LauncherSettings(isClockSetUp = true))

    private fun TestScope.viewModel() = LauncherViewModel(
        apps,
        settings,
        FakeDevicePreferencesRepository(),
        FakeWallpaperStore(),
        FakeHomeWidgetHost(emptyList()),
        FakeLockScreenWallpaper(),
        FakeHomeSystem()
    ).also { backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() } }

    @Test
    fun `the menu of an app reads its shortcuts, and one opens through Android`() = runTest {
        val viewModel = viewModel()

        assertEquals(listOf(newMessage), viewModel.appMenu.shortcutsOf(messages))
        viewModel.onOpenApp(newMessage)

        assertEquals(listOf(newMessage), apps.opened)
    }

    @Test
    fun `a shortcut put on the home screen shows there, is kept pinned, and is let go when taken off`() = runTest {
        val viewModel = viewModel()

        viewModel.onToggleFavorite(newMessage)
        assertEquals(listOf(newMessage.key), viewModel.uiState.value.home.map { it.key })
        assertTrue(newMessage.key in viewModel.uiState.value.favoriteKeys)
        assertEquals(setOf(newMessage.key) to emptySet<String>(), apps.pinCalls.last())
        // Not in all the apps: a shortcut is a part of its app.
        assertFalse(viewModel.uiState.value.drawer.any { it.key == newMessage.key })

        viewModel.onToggleFavorite(newMessage)
        assertTrue(viewModel.uiState.value.home.isEmpty())
        assertEquals(emptySet<String>() to setOf(newMessage.key), apps.pinCalls.last())
    }

    @Test
    fun `a shortcut its app removed is not shown, and one renamed shows its new name`() = runTest {
        settings.settings.value = settings.settings.value.copy(favorites = listOf(newMessage.key))
        val viewModel = viewModel()

        apps.shortcuts.value = mapOf(messages.key to listOf(newMessage.copy(label = "Escrever")))
        assertEquals("Escrever", (viewModel.uiState.value.home.single() as HomeEntry.App).app.label)

        apps.shortcuts.value = emptyMap()
        assertTrue(viewModel.uiState.value.home.isEmpty())
    }

    @Test
    fun `the shortcuts kept pinned are the ones on the pages, in the dock and in folders`() {
        val other = shortcutOf(messages, "Contato")
        val home = LauncherSettings(
            favorites = listOf(newMessage.key, folderKey(1), messages.key),
            dock = listOf(other.key),
            folders = mapOf(1 to HomeFolder("", listOf(messages.key, shortcutOf(messages, "Grupo").key)))
        )

        assertEquals(setOf(newMessage.key, other.key, shortcutOf(messages, "Grupo").key), home.shortcutKeys)
    }

    @Test
    fun `an app asking to pin is refused on a locked or full home screen, and a widget takes the cells it asks`() {
        assertNull(LauncherSettings().pinRefusal)
        assertEquals(PinRefusal.LOCKED, LauncherSettings(isLayoutLocked = true).pinRefusal)
        val full = LauncherSettings(favorites = List(MAX_HOME_ITEMS) { "app$it" })
        assertEquals(PinRefusal.FULL, full.pinRefusal)

        val withWidget = LauncherSettings().withWidget(7, HomeSpan(columns = 2, rows = 2))
        assertEquals(listOf(widgetKey(7)), withWidget.favorites)
        assertEquals(HomeSpan(columns = 2, rows = 2), withWidget.widgetSpans[7])
    }
}
