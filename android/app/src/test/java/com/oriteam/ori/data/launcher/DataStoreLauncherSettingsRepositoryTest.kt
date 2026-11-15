package com.oriteam.ori.data.launcher

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oriteam.ori.domain.launcher.BadgeStyle
import com.oriteam.ori.domain.launcher.DrawerStyle
import com.oriteam.ori.domain.launcher.GridChoice
import com.oriteam.ori.domain.launcher.HomeArrangement
import com.oriteam.ori.domain.launcher.HomeCell
import com.oriteam.ori.domain.launcher.HomeFolder
import com.oriteam.ori.domain.launcher.HomeScreenMode
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.domain.launcher.LauncherIconSize
import com.oriteam.ori.domain.launcher.LauncherPanel
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.domain.launcher.LockScreenChoice
import com.oriteam.ori.domain.launcher.StimulusChoices
import com.oriteam.ori.domain.launcher.StimulusLevel
import com.oriteam.ori.domain.launcher.StimulusMotion
import com.oriteam.ori.domain.launcher.WallpaperColor
import com.oriteam.ori.domain.launcher.WallpaperGradient
import com.oriteam.ori.domain.launcher.widgetKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The choices of ORI Home survive a restart, with the order of the favorites. Each test writes its file once. */
class DataStoreLauncherSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `the choices are kept with the order of the favorites`() = runTest {
        val file = File(folder.root, "launcher.preferences_pb")
        val repository = DataStoreLauncherSettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope
            ) {
                file
            }
        )
        assertEquals(LauncherSettings(), repository.settings.first())
        val chosen = LauncherSettings(
            favorites = listOf("b/.Main@0", "a/.Main@0"),
            hidden = setOf("c/.Main@10"),
            stimulus = StimulusChoices(motion = StimulusMotion.REDUCED, showsWallpaper = false),
            previousLauncher = "com.samsung.launcher",
            showsButtons = true,
            iconSize = LauncherIconSize.MAXIMUM,
            iconShape = IconShape.SQUARE,
            drawerStyle = DrawerStyle.PAGES,
            screenMode = HomeScreenMode.APPS_ONLY,
            wallpaper = LauncherWallpaper.Photo("wallpaper-1.jpg"),
            arrangement = HomeArrangement.FREE,
            positions = mapOf("b/.Main@0" to HomeCell(1, 2, 3), widgetKey(7) to HomeCell(0, 1, 0)),
            positionsTurned = mapOf("b/.Main@0" to HomeCell(0, 4, 1)),
            widgetSpans = mapOf(7 to HomeSpan(4, 2)),
            panel = LauncherPanel.PHRASES,
            dock = listOf("d/.Main@0", "e/.Main@0"),
            isClockSetUp = true,
            wallpaperHistory = listOf("wallpaper-1.jpg", "wallpaper-0.jpg"),
            lockWallpaper = LockScreenChoice(LauncherWallpaper.Gradient(WallpaperGradient.DUSK), androidId = 42),
            homePageCount = 3,
            mainPage = 1,
            isLayoutLocked = true,
            homeGrid = GridChoice(6, 8),
            swipesDownForNotifications = false,
            showsLabels = false,
            addsNewApps = true,
            turnsOnPhone = true,
            folders = mapOf(3 to HomeFolder("Jogos", listOf("g/.Main@0", "h/.Main@0"))),
            badges = BadgeStyle.NUMBER,
            showsDock = false
        )

        repository.update { chosen }

        assertEquals(chosen, repository.settings.first())
    }

    @Test
    fun `every kind of background is written and read back the same`() {
        val fixed =
            listOf(LauncherWallpaper.System, LauncherWallpaper.Theme, LauncherWallpaper.Photo("wallpaper-1.jpg"))
        val kinds = fixed +
            WallpaperColor.entries.map { LauncherWallpaper.Solid(it) } +
            WallpaperGradient.entries.map { LauncherWallpaper.Gradient(it) }

        kinds.forEach { assertEquals(it, wallpaperOf(it.toStored())) }
    }

    @Test
    fun `a background this version does not know is the Android wallpaper, the usual one`() {
        assertEquals(LauncherWallpaper.System, wallpaperOf("color:ORANGE"))
        assertEquals(LauncherWallpaper.System, wallpaperOf("video:x"))
        assertEquals(LauncherWallpaper.System, wallpaperOf(null))
        assertEquals(LauncherWallpaper.Solid(WallpaperColor.MINT), wallpaperOf("color:MINT"))
    }

    @Test
    fun `the wallpaper put on the lock screen is read back only whole, and one this version does not know is none`() {
        val dusk = LauncherWallpaper.Gradient(WallpaperGradient.DUSK)
        assertEquals(LockScreenChoice(dusk, 42), lockChoiceOf(dusk.toStored(), 42))
        assertNull(lockChoiceOf(dusk.toStored(), null))
        assertNull(lockChoiceOf(null, 42))
        assertNull(lockChoiceOf("video:x", 42))
    }

    @Test
    fun `broken cells and sizes are skipped, never the whole home screen`() {
        val cells = cellsOf("a\t0\t1\t2\nbroken\nb\tx\t1\t2\nc\t0\t-1\t0")
        assertEquals(mapOf("a" to HomeCell(0, 1, 2)), cells)
        assertEquals(mapOf(3 to HomeSpan(2, 1)), spansOf("3\t2\t1\n4\t0\t1\nx\t1\t1"))
        assertEquals(emptyMap<String, HomeCell>(), cellsOf(null))
        // A grid that is broken or not one ORI offers reads as automatic (null).
        assertEquals(GridChoice(6, 8), gridOf("6x8"))
        listOf(null, "", "6", "6x", "x8", "6x8x2", "0x8", "6x40", "seis").forEach { assertNull(it, gridOf(it)) }
        // A broken folder or one without apps is skipped; the others stay.
        assertEquals(
            mapOf(2 to HomeFolder("Jogos", listOf("a", "b"))),
            foldersOf("2\tJogos\ta,b\nx\tQuebrada\ta\n4\tVazia\t\nsem campos")
        )
    }

    @Test
    fun `the choice of the scrolling list, taken out, is erased at the next change`() = runTest {
        val retired = stringPreferencesKey("launcher_paging")
        val store = MemoryPreferences(mutablePreferencesOf(retired to "SCROLL"))
        val repository = DataStoreLauncherSettingsRepository(store)

        repository.update { it.copy(stimulus = StimulusLevel.LOW.choices) }

        assertNull(store.data.first()[retired])
        assertEquals(StimulusLevel.LOW.choices, repository.settings.first().stimulus)
    }

    @Test
    fun `a shape stored before the third one keeps its look, and the old key is erased at the next change`() = runTest {
        val retired = stringPreferencesKey("launcher_icon_shape")
        val rounded = MemoryPreferences(mutablePreferencesOf(retired to "SQUARE"))
        val round = MemoryPreferences(mutablePreferencesOf(retired to "ROUND"))
        val repository = DataStoreLauncherSettingsRepository(rounded)

        assertEquals(IconShape.ROUNDED, repository.settings.first().iconShape)
        assertEquals(IconShape.ROUND, DataStoreLauncherSettingsRepository(round).settings.first().iconShape)

        repository.update { it.copy(showsButtons = true) }
        assertNull(rounded.data.first()[retired])
        assertEquals(IconShape.ROUNDED, repository.settings.first().iconShape)
    }

    @Test
    fun `the old switch of low stimulation becomes its choices, and is erased at the next change`() = runTest {
        val retired = booleanPreferencesKey("launcher_low_stimulation")
        val store = MemoryPreferences(mutablePreferencesOf(retired to true))
        val repository = DataStoreLauncherSettingsRepository(store)
        val calm = StimulusChoices(motion = StimulusMotion.REDUCED, colorfulIcons = false, isSimplified = true)

        assertEquals(calm, repository.settings.first().stimulus)

        repository.update { it.copy(showsButtons = true) }
        assertNull(store.data.first()[retired])
        assertEquals(calm, repository.settings.first().stimulus)
    }
}

/** A store in memory, for a test that writes twice: on Windows the file of DataStore can be written once per test. */
private class MemoryPreferences(initial: Preferences) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}
