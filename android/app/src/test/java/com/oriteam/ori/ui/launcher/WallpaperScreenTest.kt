package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.domain.launcher.LockScreenChoice
import com.oriteam.ori.domain.launcher.WallpaperGradient
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.FakeHomeSystem
import com.oriteam.ori.testing.FakeHomeWidgetHost
import com.oriteam.ori.testing.FakeInstalledApps
import com.oriteam.ori.testing.FakeLauncherSettingsRepository
import com.oriteam.ori.testing.FakeLockScreenWallpaper
import com.oriteam.ori.testing.FakeWallpaperStore
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.testing.launcherApp
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The wallpaper screen: how the lock and home screens look now, where the Android wallpaper goes, the photos used
 * before (each with a name of its own) and thumbnails in the shape of the screen.
 */
@RunWith(AndroidJUnit4::class)
class WallpaperScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var viewModel: LauncherViewModel

    private fun render(
        settings: LauncherSettings = LauncherSettings(isClockSetUp = true),
        apps: List<LauncherApp> = emptyList(),
        window: DpSize = DpSize(360.dp, 800.dp),
        lockScreen: FakeLockScreenWallpaper = FakeLockScreenWallpaper()
    ) {
        viewModel = LauncherViewModel(
            FakeInstalledApps(apps),
            FakeLauncherSettingsRepository(settings),
            FakeDevicePreferencesRepository(),
            FakeWallpaperStore(),
            FakeHomeWidgetHost(),
            lockScreen,
            FakeHomeSystem()
        )
        composeRule.setContent {
            OriTestWindow(widthDp = window.width.value.toInt(), heightDp = window.height.value.toInt()) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LauncherScreen(
                    state,
                    viewModel,
                    LauncherActions(onOpenOri = {}, onLeaveHome = {}),
                    LauncherSlots(panel = { Text("Painel") }, widget = { _, modifier -> Box(modifier) })
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun showWallpapers(
        settings: LauncherSettings = LauncherSettings(isClockSetUp = true),
        window: DpSize = DpSize(360.dp, 800.dp)
    ) {
        render(settings, window = window)
        openWallpapers()
    }

    private fun openWallpapers() {
        viewModel.onOpenPane(LauncherPane.WALLPAPER)
        composeRule.waitForIdle()
    }

    @Test
    fun `the wallpaper screen starts with how the lock screen and the home screen look now`() {
        showWallpapers()

        composeRule.onNodeWithText("Como está agora").assertIsDisplayed()
        composeRule.onNodeWithText("Tela de bloqueio").assertIsDisplayed()
        composeRule.onNodeWithText("Tela inicial").assertIsDisplayed()
    }

    @Test
    fun `applying the Android wallpaper asks for the home screen, the lock screen or both`() {
        showWallpapers()

        composeRule.onNodeWithContentDescription("Papel de parede do Android").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Aplicar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Usar o papel de parede em").assertIsDisplayed()
        composeRule.onNodeWithText("Tela inicial e de bloqueio").assertIsDisplayed()
    }

    @Test
    fun `the preview shows the wallpaper on the home screen first, and on the lock screen with its tab`() {
        showWallpapers()
        composeRule.onNodeWithContentDescription("Mar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Tela inicial").assertIsSelected()
        composeRule.onNodeWithTag(LOCK_PREVIEW_TAG, useUnmergedTree = true).assertDoesNotExist()

        composeRule.onNodeWithText("Tela de bloqueio").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tela de bloqueio").assertIsSelected()
        composeRule.onNodeWithTag(LOCK_PREVIEW_TAG, useUnmergedTree = true).assertExists()
        // Cancelar and Aplicar stay over it.
        composeRule.onNodeWithText("Aplicar").assertIsDisplayed()

        composeRule.onNodeWithText("Tela inicial").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(LOCK_PREVIEW_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `where Android does not let ORI set the lock screen, there are no tabs and Aplicar is for the home screen`() {
        render(lockScreen = FakeLockScreenWallpaper(allows = false))
        openWallpapers()
        composeRule.onNodeWithContentDescription("Mar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Tela de bloqueio").assertDoesNotExist()
        composeRule.onNodeWithText("Aplicar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Usar o papel de parede em").assertDoesNotExist()
        assertEquals(
            LauncherWallpaper.Gradient(WallpaperGradient.SEA),
            viewModel.uiState.value.settings.wallpaper
        )
    }

    @Test
    fun `a photo the lock screen shows cannot be taken out of the history`() {
        val lockPhoto = LockScreenChoice(LauncherWallpaper.Photo("lock.jpg"), androidId = 5)
        showWallpapers(
            LauncherSettings(
                isClockSetUp = true,
                wallpaperHistory = listOf("home.jpg", "lock.jpg"),
                lockWallpaper = lockPhoto
            )
        )

        composeRule.onNodeWithContentDescription("Foto 1 usada antes").assert(hasLongClickAction())
        composeRule.onNodeWithContentDescription("Foto 2 usada antes").assert(hasLongClickAction().not())
    }

    @Test
    fun `while a wallpaper is tried, Cancelar and Aplicar stay over the dock, never on it`() {
        val camera = launcherApp("Câmera")
        render(LauncherSettings(isClockSetUp = true, dock = listOf(camera.key)), listOf(camera))
        // Under a pane the home screen is only a picture for the screen reader: where the dock is is read before.
        val dock = composeRule.onNodeWithContentDescription("Câmera").bounds
        openWallpapers()

        composeRule.onNodeWithContentDescription("Papel de parede do Android").performClick()
        composeRule.waitForIdle()

        val apply = composeRule.onNodeWithText("Aplicar").bounds
        assertTrue("Aplicar ends at ${apply.bottom}, the dock starts at ${dock.top}", apply.bottom <= dock.top)
    }

    @Test
    fun `each photo used before has a name of its own, newest first`() {
        showWallpapers(LauncherSettings(isClockSetUp = true, wallpaperHistory = listOf("new.jpg", "old.jpg")))

        composeRule.onNodeWithContentDescription("Foto 1 usada antes").assertExists()
        composeRule.onNodeWithContentDescription("Foto 2 usada antes").assertExists()
    }

    private fun hasLongClickAction() = SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)

    @Test
    @Config(qualifiers = "+w1280dp-h800dp")
    fun `on a screen turned, the thumbnails are turned too, in the shape of the screen`() {
        showWallpapers(window = DpSize(1280.dp, 800.dp))

        // A choice is its small screen and its name under it: turned, the long side of the small screen is across.
        val choice = composeRule.onNodeWithContentDescription("Papel de parede do Android").bounds
        assertTrue("choice ${choice.width} x ${choice.height}", choice.width >= 150.dp)
    }
}
