package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.FakeHomeSystem
import com.oriteam.ori.testing.FakeHomeWidgetHost
import com.oriteam.ori.testing.FakeInstalledApps
import com.oriteam.ori.testing.FakeLauncherSettingsRepository
import com.oriteam.ori.testing.FakeLockScreenWallpaper
import com.oriteam.ori.testing.FakeWallpaperStore
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.launcherApp
import com.oriteam.ori.testing.onScreenRoot
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PANEL_TEXT = "Painel de teste"

/**
 * Back on ORI Home, as on any home screen: on the pages it does nothing (the home screen is never closed, so it is
 * never built again), on the panel it goes to the first page, and from a pane it comes back to the page it left.
 */
@RunWith(AndroidJUnit4::class)
class HomeBackTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(20) { launcherApp("App $it") }
    private lateinit var viewModel: LauncherViewModel

    private fun render() {
        viewModel = LauncherViewModel(
            FakeInstalledApps(apps),
            FakeLauncherSettingsRepository(LauncherSettings(favorites = apps.map { it.key }, isClockSetUp = true)),
            FakeDevicePreferencesRepository(),
            FakeWallpaperStore(),
            FakeHomeWidgetHost(),
            FakeLockScreenWallpaper(),
            FakeHomeSystem()
        )
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 800) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LauncherScreen(
                    state,
                    viewModel,
                    LauncherActions(onOpenOri = {}, onLeaveHome = {}),
                    LauncherSlots(panel = { Text(PANEL_TEXT) }, widget = { _, modifier -> Box(modifier) })
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun `back on the home screen does nothing, the screen is not closed and built again`() {
        render()

        pressBack()

        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithContentDescription("App 0").assertIsDisplayed()
    }

    @Test
    fun `coming back from the options finds the page that was left, not the first one`() {
        render()
        composeRule.onScreenRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("App 19").assertIsDisplayed()

        viewModel.onOpenPane(LauncherPane.OPTIONS)
        composeRule.waitForIdle()
        pressBack()

        composeRule.onNodeWithContentDescription("App 19").assertIsDisplayed()
    }

    @Test
    fun `back on the panel goes to the first page`() {
        render()
        composeRule.onScreenRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(PANEL_TEXT).assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithText(PANEL_TEXT).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("App 0").assertIsDisplayed()
    }
}
