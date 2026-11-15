package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
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
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.testing.launcherApp
import com.oriteam.ori.testing.onScreenRoot
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The menu of an icon is drawn in the window of the home screen (a window of its own cost about 90 ms at each hold on
 * the test tablet), and still behaves as that window did: next to the icon, closed by a touch outside or by back.
 */
@RunWith(AndroidJUnit4::class)
class HomeMenuLayerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(12) { launcherApp("App $it") }
    private val installed = FakeInstalledApps(apps)
    private val settings = FakeLauncherSettingsRepository(
        LauncherSettings(favorites = apps.take(8).map { it.key }, isClockSetUp = true)
    )
    private lateinit var viewModel: LauncherViewModel

    private fun render() {
        viewModel = LauncherViewModel(
            installed,
            settings,
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
                    LauncherSlots(panel = { Text("Painel") }, widget = { _, modifier -> Box(modifier) })
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun openMenuOf(description: String) {
        composeRule.onNodeWithContentDescription(description).performTouchInput { longClick() }
        composeRule.waitForIdle()
    }

    @Test
    fun `the menu of an icon opens in the window of the home screen, next to the icon, not over it`() {
        render()

        openMenuOf("App 0")

        composeRule.onNodeWithText("Informações do app").assertIsDisplayed()
        composeRule.onAllNodes(isPopup()).assertCountEquals(0)
        val icon = composeRule.onNodeWithContentDescription("App 0").bounds
        val choice = composeRule.onNodeWithText("Informações do app").bounds
        assertTrue("$choice over $icon", choice.top >= icon.bottom || choice.bottom <= icon.top)
    }

    @Test
    fun `a touch outside closes the menu and does not open the app under it`() {
        render()
        openMenuOf("App 0")
        val other = composeRule.onNodeWithContentDescription("App 7").fetchSemanticsNode().boundsInRoot.center

        composeRule.onScreenRoot().performTouchInput { click(other) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Informações do app").assertDoesNotExist()
        assertEquals(emptyList<String>(), installed.opened.map { it.label })
    }

    @Test
    fun `back closes the menu and stays on the home screen`() {
        render()
        openMenuOf("App 0")

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Informações do app").assertDoesNotExist()
        assertEquals(LauncherPane.HOME, viewModel.uiState.value.pane)
        assertFalse(viewModel.uiState.value.isEditingHome)
    }

    @Test
    fun `a choice of the menu still works`() {
        render()
        openMenuOf("App 0")

        composeRule.onNodeWithText("Informações do app").performTouchInput { click() }
        composeRule.waitForIdle()

        assertEquals(listOf("App 0"), installed.detailsOpened.map { it.label })
        composeRule.onNodeWithText("Informações do app").assertDoesNotExist()
    }
}
