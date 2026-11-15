package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.DrawerStyle
import com.oriteam.ori.domain.launcher.HomeScreenMode
import com.oriteam.ori.domain.launcher.LauncherIconSize
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
 * How ORI Home can be shown: the tiles of "Máximo", all the apps on pages as the home screen, and only all the apps
 * (no home screen at all).
 */
@RunWith(AndroidJUnit4::class)
class HomeStylesTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(30) { launcherApp("App ${it.toString().padStart(2, '0')}") }
    private val settings = FakeLauncherSettingsRepository(
        LauncherSettings(favorites = apps.take(4).map { it.key }, dock = listOf(apps[29].key), isClockSetUp = true)
    )
    private lateinit var viewModel: LauncherViewModel

    private fun render(initial: LauncherSettings) {
        settings.settings.value = initial
        viewModel = LauncherViewModel(
            FakeInstalledApps(apps),
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

    private fun isShown(description: String) =
        composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `at Maximum the apps are wide tiles, two in a row on a phone`() {
        render(settings.settings.value.copy(iconSize = LauncherIconSize.MAXIMUM))

        val first = composeRule.onNodeWithContentDescription("App 00").bounds
        val second = composeRule.onNodeWithContentDescription("App 01").bounds
        val third = composeRule.onNodeWithContentDescription("App 02").bounds
        // A tile takes about half the width of the phone, and the third goes to the next row.
        assertTrue("tile ${first.width}", first.width > 140.dp)
        assertEquals(first.top, second.top)
        assertTrue(third.top > first.top)
    }

    /** All the apps opened at "Máximo", shown as [style]: the apps there are tiles, two in a row on a phone. */
    private fun assertTilesInAllTheApps(style: DrawerStyle) {
        render(settings.settings.value.copy(iconSize = LauncherIconSize.MAXIMUM, drawerStyle = style))
        composeRule.onScreenRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        // Not on the home screen (its first four) nor in the dock: only in all the apps.
        val first = composeRule.onNodeWithContentDescription("App 04").bounds
        val second = composeRule.onNodeWithContentDescription("App 05").bounds
        val third = composeRule.onNodeWithContentDescription("App 06").bounds
        assertTrue("$style tile ${first.width} x ${first.height}", first.width > 140.dp && first.height > 100.dp)
        // A tile is not taller than wide; a huge icon with its name under it is (as all the apps were before).
        assertTrue("$style tile ${first.width} x ${first.height}", first.height <= first.width)
        assertEquals(first.top, second.top)
        assertTrue(third.top > first.top)
    }

    @Test
    fun `at Maximum all the apps in a list are tiles too, as on the home screen`() =
        assertTilesInAllTheApps(DrawerStyle.SCROLL)

    @Test
    fun `at Maximum all the apps on pages are tiles too, as on the home screen`() =
        assertTilesInAllTheApps(DrawerStyle.PAGES)

    @Test
    fun `all the apps on pages show a page at a time, and a swipe sideways shows the next`() {
        render(settings.settings.value.copy(drawerStyle = DrawerStyle.PAGES))
        composeRule.onScreenRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertTrue(isShown("App 00"))
        assertFalse(isShown("App 28"))
        val firstElsewhere = apps.map { it.label }.first { !isShown(it) }

        composeRule.onScreenRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(firstElsewhere).assertIsDisplayed()
    }

    @Test
    fun `with only all the apps, they are the whole screen, with the options at hand and nothing to add to a home`() {
        render(settings.settings.value.copy(screenMode = HomeScreenMode.APPS_ONLY))

        composeRule.onNodeWithText("Nome do app").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("App 05").assertIsDisplayed()

        // An app held opens its menu, without "Adicionar à tela inicial": there is no home screen.
        composeRule.onNodeWithContentDescription("App 05").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Desinstalar").assertExists()
        composeRule.onNodeWithText("Adicionar à tela inicial").assertDoesNotExist()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Opções da tela inicial").performClick()
        composeRule.waitForIdle()
        assertEquals(LauncherPane.OPTIONS, viewModel.uiState.value.pane)
    }

    @Test
    fun `with only all the apps there is no editing to fall into`() {
        render(settings.settings.value.copy(screenMode = HomeScreenMode.APPS_ONLY))

        viewModel.onEditingChange(true)
        assertFalse(viewModel.uiState.value.isEditingHome)

        // The options opened while editing go back to editing, but not where there is no home screen.
        viewModel.onOpenPane(LauncherPane.OPTIONS)
        viewModel.onBack()
        assertFalse(viewModel.uiState.value.isEditingHome)
    }
}
