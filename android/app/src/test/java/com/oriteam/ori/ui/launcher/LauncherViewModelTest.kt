package com.oriteam.ori.ui.launcher

import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.CLOCK_KEY
import com.oriteam.ori.domain.launcher.HomeArrangement
import com.oriteam.ori.domain.launcher.HomeCell
import com.oriteam.ori.domain.launcher.HomeGrid
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.LauncherPanel
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.domain.launcher.MAX_HOME_ITEMS
import com.oriteam.ori.domain.launcher.StimulusLevel
import com.oriteam.ori.domain.launcher.StimulusMotion
import com.oriteam.ori.domain.launcher.WallpaperColor
import com.oriteam.ori.domain.launcher.WallpaperGradient
import com.oriteam.ori.domain.launcher.WallpaperTarget
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.domain.launcher.level
import com.oriteam.ori.domain.launcher.widgetKey
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.FakeHomeSystem
import com.oriteam.ori.testing.FakeHomeWidgetHost
import com.oriteam.ori.testing.FakeInstalledApps
import com.oriteam.ori.testing.FakeLauncherSettingsRepository
import com.oriteam.ori.testing.FakeLockScreenWallpaper
import com.oriteam.ori.testing.FakeWallpaperStore
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.launcherApp
import com.oriteam.ori.testing.widgetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = launcherApp("Relógio")
    private val music = launcherApp("Música")
    private val apps = FakeInstalledApps(listOf(music, clock))

    // The clock was set up already, so these tests see only the apps they add.
    private val settings = FakeLauncherSettingsRepository(LauncherSettings(isClockSetUp = true))
    private val device = FakeDevicePreferencesRepository()
    private val wallpapers = FakeWallpaperStore()
    private val widgetHost = FakeHomeWidgetHost(listOf(widgetProvider("Clima")))
    private val lockScreen = FakeLockScreenWallpaper()
    private val homeSystem = FakeHomeSystem()
    private val grid = HomeGrid(columns = 3, rows = 4)

    private fun TestScope.viewModel() = LauncherViewModel(
        apps,
        settings,
        device,
        wallpapers,
        widgetHost,
        lockScreen,
        homeSystem
    ).also {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.appPreferences.collect() }
    }

    private val LauncherViewModel.homeKeys get() = uiState.value.home.map { it.key }

    @Test
    fun `the apps arrive in alphabetical order, and pinned ones are on the home screen in their order`() = runTest {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(music, clock), viewModel.uiState.value.drawer)

        viewModel.onToggleFavorite(clock)
        viewModel.onToggleFavorite(music)
        viewModel.layout.onDropItem(music.key, HomeCell(0, 0, 0), grid)

        assertEquals(listOf(music.key, clock.key), viewModel.homeKeys)
    }

    @Test
    fun `free icons stay in the cell where they are dropped`() = runTest {
        settings.settings.value = LauncherSettings(
            favorites = listOf(clock.key, music.key),
            arrangement = HomeArrangement.FREE,
            isClockSetUp = true
        )
        val viewModel = viewModel()

        viewModel.layout.onDropItem(clock.key, HomeCell(1, 3, 2), grid)

        assertEquals(HomeCell(1, 3, 2), settings.settings.value.positions[clock.key])
        assertEquals(HomeCell(0, 0, 1), settings.settings.value.positions[music.key])
    }

    @Test
    fun `after a restart the home screen and the choices are the same`() = runTest {
        viewModel().apply {
            onToggleFavorite(clock)
            appearance.onStimulusLevelChange(StimulusLevel.LOW)
            appearance.onPanelChange(LauncherPanel.PHRASES)
        }

        val restarted = viewModel()

        assertEquals(listOf(clock.key), restarted.homeKeys)
        assertEquals(StimulusLevel.LOW.choices, restarted.uiState.value.settings.stimulus)
        assertEquals(LauncherPanel.PHRASES, restarted.uiState.value.settings.panel)
    }

    @Test
    fun `an app removed from the device leaves the home screen and the drawer without an error`() = runTest {
        val viewModel = viewModel()
        viewModel.onToggleFavorite(music)

        apps.apps.value = listOf(clock)

        assertEquals(emptyList<String>(), viewModel.homeKeys)
        assertEquals(listOf(clock), viewModel.uiState.value.drawer)
    }

    @Test
    fun `opening an app goes back to the start, and one that cannot open is explained`() = runTest {
        val viewModel = viewModel()
        viewModel.onOpenPane(LauncherPane.DRAWER)
        viewModel.onQueryChange("mus")
        assertEquals(listOf(music), viewModel.uiState.value.drawer)

        viewModel.onOpenApp(music)
        assertEquals(listOf(music), apps.opened)
        assertEquals(LauncherPane.HOME, viewModel.uiState.value.pane)
        assertEquals("", viewModel.uiState.value.query)

        apps.canOpen = false
        viewModel.onOpenApp(clock)
        assertEquals(R.string.launcher_app_unavailable, viewModel.uiState.value.messageRes)

        viewModel.onMessageSeen()
        assertNull(viewModel.uiState.value.messageRes)
    }

    @Test
    fun `a full home screen says so instead of ignoring the tap`() = runTest {
        settings.settings.value = LauncherSettings(
            favorites = List(MAX_HOME_ITEMS) { "app$it/.Main@0" },
            isClockSetUp = true
        )
        val viewModel = viewModel()

        viewModel.onToggleFavorite(music)

        assertEquals(R.string.launcher_favorites_full, viewModel.uiState.value.messageRes)
    }

    @Test
    fun `each level of stimulus sets the motion and the decorations of ORI, and motion off stays off`() = runTest {
        device.accountCopy.value = AccountPreferences(motion = MotionLevel.EXPRESSIVE)
        val viewModel = viewModel()
        fun account() = viewModel.appPreferences.value!!.account

        assertEquals(MotionLevel.EXPRESSIVE, account().motion)
        viewModel.appearance.onStimulusLevelChange(StimulusLevel.MEDIUM)
        assertEquals(MotionLevel.REDUCED, account().motion)
        assertFalse(account().isSimplified)
        viewModel.appearance.onStimulusLevelChange(StimulusLevel.LOW)
        assertEquals(MotionLevel.OFF, account().motion)
        assertFalse(account().isSimplified)
        viewModel.appearance.onStimulusLevelChange(StimulusLevel.NONE)
        assertTrue(account().isSimplified)

        // "Médio" lowers the motion, never raises it: the motion the person turned off stays off.
        viewModel.appearance.onStimulusLevelChange(StimulusLevel.MEDIUM)
        device.accountCopy.value = AccountPreferences(motion = MotionLevel.OFF)
        assertEquals(MotionLevel.OFF, account().motion)
    }

    @Test
    fun `a choice changed by hand after a level is kept with the others, and the level is custom`() = runTest {
        val viewModel = viewModel()
        viewModel.appearance.onStimulusLevelChange(StimulusLevel.LOW)
        viewModel.appearance.onStimulusChange { it.copy(colorfulIcons = false) }

        val settings = viewModel.uiState.value.settings
        assertEquals(StimulusMotion.OFF, settings.stimulus.motion)
        assertTrue(settings.stimulus.showsWallpaper)
        assertTrue(settings.isMonochrome)
        assertNull(settings.stimulus.level)
    }

    @Test
    fun `home pressed closes everything and asks the pages to go back to the first one`() = runTest {
        val viewModel = viewModel()
        viewModel.onEditingChange(true)
        val before = viewModel.uiState.value.homeRequest

        viewModel.onHomePressed()

        assertEquals(LauncherPane.HOME, viewModel.uiState.value.pane)
        assertFalse(viewModel.uiState.value.isEditingHome)
        assertEquals(before + 1, viewModel.uiState.value.homeRequest)
    }

    @Test
    fun `a wallpaper is only previewed until it is applied, and a cancelled photo is deleted`() = runTest {
        val viewModel = viewModel()

        viewModel.wallpaper.onPreview(LauncherWallpaper.Solid(WallpaperColor.BLUE))
        assertEquals(LauncherWallpaper.System, settings.settings.value.wallpaper)
        viewModel.wallpaper.onApply()
        assertEquals(LauncherWallpaper.Solid(WallpaperColor.BLUE), settings.settings.value.wallpaper)

        viewModel.wallpaper.onPhotoPicked("content://picker/1")
        assertEquals(LauncherWallpaper.Photo("wallpaper-1.jpg"), viewModel.uiState.value.wallpaperDraft.preview)
        viewModel.wallpaper.onCancel()
        assertEquals(emptySet<String>(), wallpapers.files)
        assertNull(viewModel.uiState.value.wallpaperDraft.preview)
    }

    @Test
    fun `applied photos stay in the history, a removed one is deleted, and a bad photo is explained`() = runTest {
        val viewModel = viewModel()
        viewModel.wallpaper.onPhotoPicked("content://picker/1")
        viewModel.wallpaper.onApply()
        viewModel.wallpaper.onPhotoPicked("content://picker/2")
        viewModel.wallpaper.onApply()

        assertEquals(LauncherWallpaper.Photo("wallpaper-2.jpg"), settings.settings.value.wallpaper)
        assertEquals(listOf("wallpaper-2.jpg", "wallpaper-1.jpg"), settings.settings.value.wallpaperHistory)
        assertEquals(setOf("wallpaper-1.jpg", "wallpaper-2.jpg"), wallpapers.files)

        viewModel.wallpaper.onPreview(LauncherWallpaper.Photo("wallpaper-1.jpg"))
        viewModel.wallpaper.onCancel()
        assertTrue("a photo of the history stays", "wallpaper-1.jpg" in wallpapers.files)

        viewModel.wallpaper.onRemoveFromHistory("wallpaper-1.jpg")
        viewModel.wallpaper.onRemoveFromHistory("wallpaper-2.jpg")
        assertEquals(listOf("wallpaper-2.jpg"), settings.settings.value.wallpaperHistory)
        assertEquals(setOf("wallpaper-2.jpg"), wallpapers.files)

        wallpapers.failsNext = true
        viewModel.wallpaper.onPhotoPicked("content://picker/broken")
        assertEquals(R.string.launcher_wallpaper_failed, viewModel.uiState.value.messageRes)
        assertEquals(LauncherWallpaper.Photo("wallpaper-2.jpg"), settings.settings.value.wallpaper)
    }

    @Test
    fun `a widget goes to the home screen once Android allows it, with its size`() = runTest {
        val viewModel = viewModel()
        val provider = widgetHost.providers().providers.single()

        viewModel.widgets.onAdd(provider)

        val widgetId = widgetHost.bound.keys.single()
        assertEquals(listOf(widgetKey(widgetId)), viewModel.homeKeys)
        assertEquals(HomeSpan(2, 2), settings.settings.value.widgetSpans[widgetId])

        // Locked, it stays and keeps its id (given back, Android would leave an empty space), and the person is told.
        viewModel.appearance.onLayoutLockedChange(true)
        viewModel.layout.onRemove(widgetKey(widgetId))
        assertEquals(listOf(widgetKey(widgetId)), viewModel.homeKeys)
        assertTrue(widgetId in widgetHost.allocated)
        // A neutral word, not a failure.
        assertEquals(R.string.launcher_layout_locked, viewModel.uiState.value.infoRes)
        assertNull(viewModel.uiState.value.messageRes)
        viewModel.appearance.onLayoutLockedChange(false)

        viewModel.widgets.onRemove(widgetId)
        assertEquals(emptyList<String>(), viewModel.homeKeys)
        assertTrue(widgetHost.allocated.isEmpty())
    }

    @Test
    fun `when Android asks the person, a no gives the widget id back and adds nothing`() = runTest {
        widgetHost.allowsBinding = false
        val viewModel = viewModel()

        viewModel.widgets.onAdd(widgetHost.providers().providers.single())
        val request = viewModel.widgets.requests.first() as WidgetRequest.AskToBind
        viewModel.widgets.onBindResult(request.widgetId, isAllowed = false)

        assertTrue(widgetHost.allocated.isEmpty())
        assertEquals(emptyList<String>(), viewModel.homeKeys)
    }

    @Test
    fun `at start, a widget id left behind on no page goes back to Android, and the ones on the pages stay`() =
        runTest {
            val host = FakeHomeWidgetHost()
            val kept = host.allocate()
            val leftBehind = host.allocate()
            settings.settings.value = settings.settings.value.copy(favorites = listOf(widgetKey(kept)))

            LauncherViewModel(apps, settings, device, wallpapers, host, lockScreen, homeSystem)

            assertEquals(setOf(kept), host.allocated)
            assertFalse(leftBehind in host.allocated)
        }

    @Test
    fun `with recent apps on, the apps opened from ORI Home follow the dock, the last first`() = runTest {
        settings.settings.value = settings.settings.value.copy(showsRecentApps = true)
        val viewModel = viewModel()

        viewModel.onOpenApp(music)
        viewModel.onOpenApp(clock)
        assertEquals(listOf(clock, music), viewModel.uiState.value.recentApps)

        viewModel.behavior.onRecentAppsChange(false)
        assertTrue(viewModel.uiState.value.recentApps.isEmpty())
    }

    @Test
    fun `Ocultar apps hides the apps checked and brings back the others`() = runTest {
        settings.settings.value = settings.settings.value.copy(favorites = listOf(music.key), hidden = setOf(clock.key))
        val viewModel = viewModel()

        viewModel.behavior.onHiddenAppsChange(setOf(music.key))

        assertEquals(listOf(music), viewModel.uiState.value.hiddenApps)
        assertEquals(listOf(clock), viewModel.uiState.value.drawer)
        assertTrue(viewModel.uiState.value.home.isEmpty())
    }

    @Test
    fun `the setup of a widget opens again from its menu, and the widget stays whatever the answer`() = runTest {
        val viewModel = viewModel()

        viewModel.widgets.onSetUpAgain(5)

        assertEquals(WidgetRequest.SetUpAgain(5), viewModel.widgets.requests.first())
    }

    @Test
    fun `a widget with a setup screen is added only after the setup is done`() = runTest {
        val host = FakeHomeWidgetHost(listOf(widgetProvider("Nota", needsSetup = true)))
        val viewModel = LauncherViewModel(apps, settings, device, wallpapers, host, lockScreen, homeSystem).also {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() }
        }

        viewModel.widgets.onAdd(host.providers().providers.single())
        val request = viewModel.widgets.requests.first() as WidgetRequest.SetUp
        assertTrue(settings.settings.value.favorites.isEmpty())
        viewModel.widgets.onSetUpResult(request.widgetId, isDone = true)

        assertEquals(listOf(widgetKey(request.widgetId)), settings.settings.value.favorites)
    }

    @Test
    fun `the time and the date come as the clock widget at the start, only the first time`() = runTest {
        settings.settings.value = LauncherSettings(favorites = listOf(music.key))
        val viewModel = viewModel()
        assertEquals(listOf(CLOCK_KEY, music.key), viewModel.homeKeys)
        assertTrue(viewModel.uiState.value.hasClock)

        viewModel.layout.onRemove(CLOCK_KEY)
        viewModel()

        assertEquals(listOf(music.key), settings.settings.value.favorites)
        viewModel.layout.onAddClock()
        assertEquals(listOf(music.key, CLOCK_KEY), settings.settings.value.favorites)
    }

    @Test
    fun `an app dropped on the dock is in the dock, and still counts as on the home screen`() = runTest {
        val viewModel = viewModel()
        viewModel.onToggleFavorite(music)

        viewModel.layout.onDropOnDock(music.key, index = 0, capacity = 4)

        assertEquals(listOf(music), viewModel.uiState.value.dock)
        assertEquals(emptyList<String>(), viewModel.homeKeys)
        assertTrue(music.key in viewModel.uiState.value.favoriteKeys)
        viewModel.onToggleFavorite(music)
        assertEquals(emptyList<Any>(), viewModel.uiState.value.dock)
    }

    @Test
    fun `back closes what is open and stays on the page, the menu opens the app page`() = runTest {
        val viewModel = viewModel()
        viewModel.onOpenPane(LauncherPane.DRAWER)
        val request = viewModel.uiState.value.homeRequest

        viewModel.onBack()
        viewModel.appMenu.onOpenDetails(music)

        assertEquals(LauncherPane.HOME, viewModel.uiState.value.pane)
        assertEquals(request, viewModel.uiState.value.homeRequest)
        assertEquals(listOf(music), apps.detailsOpened)
    }

    @Test
    fun `back from a pane opened while editing goes back to editing, and from editing to the home screen`() = runTest {
        val viewModel = viewModel()
        viewModel.onEditingChange(true)
        viewModel.onOpenPane(LauncherPane.WALLPAPER)

        viewModel.onBack()
        assertTrue(viewModel.uiState.value.isEditingHome)

        viewModel.onBack()
        assertFalse(viewModel.uiState.value.isEditingHome)
        viewModel.onOpenPane(LauncherPane.OPTIONS)
        viewModel.onBack()
        assertFalse(viewModel.uiState.value.isEditingHome)
    }

    @Test
    fun `uninstall asks Android, which asks the person`() = runTest {
        val viewModel = viewModel()

        viewModel.appMenu.onUninstall(music)

        assertEquals(listOf(music), apps.uninstallAsked)
    }

    @Test
    fun `a photo only for the lock screen goes to Android and to the history, the home keeps its own`() = runTest {
        val viewModel = viewModel()
        viewModel.onOpenPane(LauncherPane.WALLPAPER)
        assertEquals(LauncherWallpaper.System, viewModel.uiState.value.lockScreenShown)
        viewModel.wallpaper.onPhotoPicked("content://picker/1")

        viewModel.wallpaper.onApply(WallpaperTarget.LOCK)

        val photo = LauncherWallpaper.Photo("wallpaper-1.jpg")
        assertEquals("wallpaper-1.jpg", lockScreen.photo)
        assertEquals(LauncherWallpaper.System, settings.settings.value.wallpaper)
        assertEquals(listOf("wallpaper-1.jpg"), settings.settings.value.wallpaperHistory)
        // No message says it worked: the preview of the lock screen shows the photo.
        assertNull(viewModel.uiState.value.messageRes)
        assertEquals(photo, viewModel.uiState.value.lockScreenShown)
    }

    @Test
    fun `the lock screen preview follows Android, and shows nothing of a picture put there by something else`() =
        runTest {
            val viewModel = viewModel()
            viewModel.wallpaper.onPreview(LauncherWallpaper.Gradient(WallpaperGradient.SEA))
            viewModel.wallpaper.onApply(WallpaperTarget.LOCK, lockColors = 1 to 2)
            assertEquals(LauncherWallpaper.Gradient(WallpaperGradient.SEA), viewModel.uiState.value.lockScreenShown)

            lockScreen.changedOutside()
            viewModel.onOpenPane(LauncherPane.WALLPAPER)
            assertNull(viewModel.uiState.value.lockScreenShown)

            viewModel.wallpaper.onPreview(LauncherWallpaper.System)
            viewModel.wallpaper.onApply(WallpaperTarget.LOCK)
            assertTrue(lockScreen.usesHomeWallpaper)
            assertEquals(LauncherWallpaper.System, viewModel.uiState.value.lockScreenShown)
            assertNull(settings.settings.value.lockWallpaper)
        }

    @Test
    fun `a color for both screens is drawn for the lock screen, and Android refusing is said`() = runTest {
        val viewModel = viewModel()
        viewModel.wallpaper.onPreview(LauncherWallpaper.Solid(WallpaperColor.MINT))
        viewModel.wallpaper.onApply(WallpaperTarget.BOTH, lockColors = 1 to 2)

        assertEquals(LauncherWallpaper.Solid(WallpaperColor.MINT), settings.settings.value.wallpaper)
        assertEquals(1 to 2, lockScreen.colors)

        lockScreen.allows = false
        viewModel.wallpaper.onPreview(LauncherWallpaper.Solid(WallpaperColor.BLUE))
        viewModel.wallpaper.onApply(WallpaperTarget.LOCK, lockColors = 3 to 3)
        assertEquals(R.string.launcher_wallpaper_lock_failed, viewModel.uiState.value.messageRes)
        assertEquals(LauncherWallpaper.Solid(WallpaperColor.MINT), settings.settings.value.wallpaper)
    }

    @Test
    fun `all the apps are not sorted again when only the home screen or the editing changes`() = runTest {
        val viewModel = viewModel()
        val sorted = viewModel.uiState.value.drawer

        viewModel.onEditingChange(true)
        viewModel.layout.onDropItem(music.key, HomeCell(0, 0, 0), grid)
        viewModel.onToggleFavorite(music)

        assertSame(sorted, viewModel.uiState.value.drawer)
        viewModel.onQueryChange("mus")
        assertEquals(listOf(music), viewModel.uiState.value.drawer)
    }

    @Test
    fun `the Android wallpaper on the lock screen makes Android show its own wallpaper there again`() = runTest {
        settings.settings.value = settings.settings.value.copy(wallpaper = LauncherWallpaper.Solid(WallpaperColor.MINT))
        val viewModel = viewModel()

        viewModel.wallpaper.onPreview(LauncherWallpaper.System)
        viewModel.wallpaper.onApply(WallpaperTarget.LOCK)

        assertTrue(lockScreen.usesHomeWallpaper)
        assertEquals(LauncherWallpaper.Solid(WallpaperColor.MINT), settings.settings.value.wallpaper)
    }

    @Test
    fun `a swipe down opens Android's notifications, unless the person turned it off`() = runTest {
        val viewModel = viewModel()

        viewModel.onSwipeDown()
        assertEquals(1, homeSystem.notificationsOpened)

        viewModel.behavior.onSwipeDownChange(false)
        viewModel.onSwipeDown()
        assertEquals(1, homeSystem.notificationsOpened)
    }

    @Test
    fun `an app installed goes to the home screen only when the person chose so`() = runTest {
        val viewModel = viewModel()
        val camera = launcherApp("Câmera")
        apps.apps.value = apps.apps.value + camera
        apps.added.emit(camera.packageName)
        assertFalse(camera.key in settings.settings.value.favorites)

        viewModel.behavior.onAddsNewAppsChange(true)
        val notes = launcherApp("Notas")
        apps.apps.value = apps.apps.value + notes
        apps.added.emit(notes.packageName)
        assertTrue(notes.key in settings.settings.value.favorites)
    }
}
