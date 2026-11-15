package com.oriteam.ori.ui.launcher

import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.HomeFolder
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.MAX_HOME_ITEMS
import com.oriteam.ori.domain.launcher.folderKey
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.FakeHomeSystem
import com.oriteam.ori.testing.FakeHomeWidgetHost
import com.oriteam.ori.testing.FakeInstalledApps
import com.oriteam.ori.testing.FakeLauncherSettingsRepository
import com.oriteam.ori.testing.FakeLockScreenWallpaper
import com.oriteam.ori.testing.FakeWallpaperStore
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.launcherApp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The folders of ORI Home through the view model, as the screen uses them: made by a drop or from the menu, opened,
 * tidied however they are closed, filled from the list, renamed, and kept as they are on a locked or full home screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeFolderEditorTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val music = launcherApp("Música")
    private val notes = launcherApp("Notas")
    private val clock = launcherApp("Relógio")
    private val apps = FakeInstalledApps(listOf(music, notes, clock))
    private val settings = FakeLauncherSettingsRepository(
        LauncherSettings(isClockSetUp = true, favorites = listOf(music.key, clock.key, notes.key))
    )

    private fun TestScope.viewModel() = LauncherViewModel(
        apps,
        settings,
        FakeDevicePreferencesRepository(),
        FakeWallpaperStore(),
        FakeHomeWidgetHost(emptyList()),
        FakeLockScreenWallpaper(),
        FakeHomeSystem()
    ).also { backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() } }

    private val LauncherViewModel.homeKeys get() = uiState.value.home.map { it.key }

    @Test
    fun `an app dropped on another makes a folder with both, in its place, and both count as on the home screen`() =
        runTest {
            val viewModel = viewModel()

            viewModel.folders.onDropOnItem(clock.key, music.key)

            val folder = viewModel.uiState.value.home.first() as HomeEntry.Folder
            assertEquals(listOf(music, clock), folder.apps)
            // Unnamed until the person names it: the screen shows "Pasta" in the language of the app.
            assertEquals("", folder.name)
            assertEquals(listOf(folder.key, notes.key), viewModel.homeKeys)
            assertTrue(clock.key in viewModel.uiState.value.favoriteKeys)
        }

    @Test
    fun `made from the menu, a folder opens at once, and closed with that app alone it is the app again`() = runTest {
        val viewModel = viewModel()

        viewModel.folders.onCreate(notes.key)

        assertEquals(listOf(notes), viewModel.uiState.value.openFolder?.apps)
        viewModel.folders.onClose()
        assertNull(viewModel.uiState.value.openFolderId)
        assertEquals(listOf(music.key, clock.key, notes.key), viewModel.homeKeys)
        assertTrue(settings.settings.value.folders.isEmpty())
    }

    @Test
    fun `Home pressed or an app opened close the folder and tidy it too`() = runTest {
        val viewModel = viewModel()

        viewModel.folders.onCreate(notes.key)
        viewModel.onHomePressed()
        assertNull(viewModel.uiState.value.openFolderId)
        assertTrue(settings.settings.value.folders.isEmpty())

        viewModel.folders.onCreate(notes.key)
        viewModel.onOpenApp(notes)
        assertNull(viewModel.uiState.value.openFolderId)
        assertEquals(listOf(music.key, clock.key, notes.key), viewModel.homeKeys)
    }

    @Test
    fun `the apps chosen in the list join the folder, one taken out goes to the home screen, and it takes a name`() =
        runTest {
            val viewModel = viewModel()
            viewModel.folders.onDropOnItem(clock.key, music.key)
            val id = (viewModel.uiState.value.home.first() as HomeEntry.Folder).id

            viewModel.folders.onChooseApps(id, listOf(music.key, clock.key, notes.key))
            assertEquals(listOf(music, clock, notes), viewModel.uiState.value.openFolderOrFirst().apps)
            assertEquals(listOf(folderKey(id)), viewModel.homeKeys)

            viewModel.folders.onTakeOut(clock.key)
            assertEquals(listOf(folderKey(id), clock.key), viewModel.homeKeys)

            viewModel.folders.onRename(id, "  Jogos ")
            assertEquals("Jogos", viewModel.uiState.value.openFolderOrFirst().name)
        }

    @Test
    fun `a folder shows only its apps still installed, and one with none left is not shown`() = runTest {
        settings.settings.value = LauncherSettings(
            isClockSetUp = true,
            favorites = listOf(folderKey(1), folderKey(2)),
            folders = mapOf(1 to HomeFolder("Jogos", listOf(music.key, "gone")), 2 to HomeFolder("", listOf("gone")))
        )
        val viewModel = viewModel()

        assertEquals(listOf(folderKey(1)), viewModel.homeKeys)
        assertEquals(listOf(music), viewModel.uiState.value.openFolderOrFirst().apps)
    }

    @Test
    fun `a locked home screen makes no folder and says why, and a full one keeps the app in its folder`() = runTest {
        settings.settings.value = settings.settings.value.copy(isLayoutLocked = true)
        val viewModel = viewModel()

        viewModel.folders.onDropOnItem(clock.key, music.key)
        viewModel.folders.onCreate(notes.key)
        assertEquals(listOf(music.key, clock.key, notes.key), viewModel.homeKeys)
        assertEquals(R.string.launcher_layout_locked, viewModel.uiState.value.infoRes)

        val others = List(MAX_HOME_ITEMS - 1) { "other$it" }
        settings.settings.value = LauncherSettings(
            isClockSetUp = true,
            favorites = listOf(folderKey(1)) + others,
            folders = mapOf(1 to HomeFolder("Jogos", listOf(music.key, clock.key)))
        )
        viewModel.folders.onTakeOut(clock.key)
        assertEquals(listOf(music.key, clock.key), settings.settings.value.folders.getValue(1).apps)
        assertEquals(R.string.launcher_favorites_full, viewModel.uiState.value.messageRes)
    }

    @Test
    fun `the list of a folder offers every app not hidden, whatever the search of all the apps`() = runTest {
        val viewModel = viewModel()

        viewModel.onQueryChange("not")

        assertEquals(listOf(notes), viewModel.uiState.value.drawer)
        assertEquals(listOf(music, notes, clock), viewModel.uiState.value.apps)
    }

    private fun LauncherUiState.openFolderOrFirst(): HomeEntry.Folder =
        openFolder ?: home.filterIsInstance<HomeEntry.Folder>().first()
}
