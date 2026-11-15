package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.HomeArrangement
import com.oriteam.ori.domain.launcher.HomeCell
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.WidgetResize
import com.oriteam.ori.domain.launcher.widgetKey
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.FakeHomeSystem
import com.oriteam.ori.testing.FakeHomeWidgetHost
import com.oriteam.ori.testing.FakeInstalledApps
import com.oriteam.ori.testing.FakeLauncherSettingsRepository
import com.oriteam.ori.testing.FakeLockScreenWallpaper
import com.oriteam.ori.testing.FakeWallpaperStore
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.launcherApp
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "Redimensionar" in the menu of a widget shows its frame and a card with its width and height: a cell more or less
 * at a tap, never past what its app allows; and while editing, a page changes places with the next one.
 */
@RunWith(AndroidJUnit4::class)
class WidgetResizeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(3) { launcherApp("App $it") }
    private val host = FakeHomeWidgetHost().apply {
        allocate()
        resize = WidgetResize(HomeSpan(1, 1), HomeSpan(3, 2), isHorizontal = true, isVertical = true)
    }
    private val widgetId = host.allocated.single()
    private val settings = FakeLauncherSettingsRepository(
        LauncherSettings(
            favorites = listOf(widgetKey(widgetId), apps[0].key, apps[1].key),
            arrangement = HomeArrangement.FREE,
            widgetSpans = mapOf(widgetId to HomeSpan(2, 2)),
            positions = mapOf(
                widgetKey(widgetId) to HomeCell(0, 0, 0),
                apps[0].key to HomeCell(0, 3, 0),
                apps[1].key to HomeCell(1, 0, 0)
            ),
            isClockSetUp = true
        )
    )
    private lateinit var viewModel: LauncherViewModel

    private fun render() {
        viewModel = LauncherViewModel(
            FakeInstalledApps(apps),
            settings,
            FakeDevicePreferencesRepository(),
            FakeWallpaperStore(),
            host,
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

    @Test
    fun `Redimensionar grows a widget by a cell, and stops at what its app allows`() {
        render()

        composeRule.onNodeWithContentDescription("Widget").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Redimensionar").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Mais largo").performClick()
        composeRule.waitForIdle()

        assertEquals(HomeSpan(3, 2), settings.settings.value.widgetSpans[widgetId])
        // Three columns is the most the app allows, and two rows: neither grows more.
        composeRule.onNodeWithContentDescription("Mais largo").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Mais alto").assertIsNotEnabled()
        composeRule.onNodeWithText("Pronto").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Mais largo").assertDoesNotExist()
    }

    @Test
    fun `while editing, a page changes places with the next one, with its items`() {
        render()
        viewModel.onEditingChange(true)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Trocar de lugar com a seguinte: página 1").performClick()
        composeRule.waitForIdle()

        assertEquals(1, settings.settings.value.positions.getValue(widgetKey(widgetId)).page)
        assertEquals(0, settings.settings.value.positions.getValue(apps[1].key).page)
    }
}
