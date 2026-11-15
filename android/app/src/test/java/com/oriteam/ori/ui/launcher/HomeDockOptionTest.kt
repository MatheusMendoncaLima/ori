package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
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

private const val HOLD_MILLIS = 1_000L
private const val DRAG_STEPS = 10

/**
 * "Mostrar o dock": off, the row of apps at the bottom of ORI Home goes, the icons get its room and nothing is dropped
 * on it; its apps are kept for when it comes back. With "Mostrar botões", the button of all the apps stays alone.
 */
@RunWith(AndroidJUnit4::class)
class HomeDockOptionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(12) { launcherApp("App $it") }
    private val withDock = LauncherSettings(
        favorites = apps.take(8).map { it.key },
        dock = listOf(apps[10].key),
        isClockSetUp = true
    )
    private val settings = FakeLauncherSettingsRepository(withDock)
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

    private fun centerOf(description: String) =
        composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.center

    private fun holdAndCarry(start: Offset, end: Offset) {
        val step = (end - start) / DRAG_STEPS.toFloat()
        composeRule.onScreenRoot().performTouchInput { down(start) }
        composeRule.mainClock.advanceTimeBy(HOLD_MILLIS)
        repeat(DRAG_STEPS) {
            composeRule.onScreenRoot().performTouchInput { moveBy(step) }
            composeRule.waitForIdle()
        }
        composeRule.onScreenRoot().performTouchInput { up() }
        composeRule.waitForIdle()
    }

    @Test
    fun `without the dock its apps are not shown, the icons get its room, and the apps are kept`() {
        render(withDock)
        composeRule.onNodeWithContentDescription("App 10").assertIsDisplayed()
        val docked = composeRule.onNodeWithContentDescription("App 7").bounds

        settings.settings.value = withDock.copy(showsDock = false)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("App 10").assertDoesNotExist()
        // The same cells, taller: the icons further down sit lower than with the dock, in the same column.
        val bare = composeRule.onNodeWithContentDescription("App 7").bounds
        assertTrue(bare.top > docked.top)
        assertEquals(docked.left, bare.left)
        assertEquals(listOf(apps[10].key), settings.settings.value.dock)
    }

    @Test
    fun `without the dock an icon carried to the bottom is not dropped on it`() {
        render(withDock.copy(showsDock = false, dock = emptyList()))
        val bottom = composeRule.onScreenRoot().fetchSemanticsNode().boundsInRoot.bottom
        val whereTheDockWas = Offset(centerOf("App 0").x, bottom - with(composeRule.density) { 52.dp.toPx() })

        holdAndCarry(centerOf("App 0"), whereTheDockWas)

        assertTrue(settings.settings.value.dock.isEmpty())
        assertTrue(apps[0].key in settings.settings.value.favorites)
    }

    @Test
    fun `without the dock but with buttons, the button of all the apps stays alone in the middle`() {
        render(withDock.copy(showsDock = false, showsButtons = true))

        composeRule.onNodeWithContentDescription("App 10").assertDoesNotExist()
        val button = composeRule.onNodeWithContentDescription("Todos os apps").bounds
        assertEquals(180f, ((button.left + button.right) / 2).value, 1f)
        composeRule.onNodeWithContentDescription("Todos os apps").performClick()
        composeRule.waitForIdle()
        assertEquals(LauncherPane.DRAWER, viewModel.uiState.value.pane)
    }

    @Test
    fun `without the dock, editing still shows its tools whole, under the page`() {
        render(withDock.copy(showsDock = false))
        val bottom = composeRule.onScreenRoot().fetchSemanticsNode().boundsInRoot.bottom
        val last = composeRule.onNodeWithContentDescription("App 7").fetchSemanticsNode().boundsInRoot
        val empty = Offset(last.center.x, last.bottom + last.height / 2)

        composeRule.onScreenRoot().performTouchInput { longClick(empty) }
        composeRule.waitForIdle()

        assertTrue(viewModel.uiState.value.isEditingHome)
        val wallpaper = composeRule.onNodeWithText("Papel de parede")
        wallpaper.assertIsDisplayed()
        assertTrue(wallpaper.fetchSemanticsNode().boundsInRoot.bottom <= bottom)
    }

    @Test
    fun `the option turns the dock off and on again`() {
        render(withDock)
        viewModel.onOpenPane(LauncherPane.OPTIONS)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Mostrar o dock").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(settings.settings.value.showsDock)

        composeRule.onNodeWithText("Mostrar o dock").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(settings.settings.value.showsDock)
        assertEquals(listOf(apps[10].key), settings.settings.value.dock)
    }
}
