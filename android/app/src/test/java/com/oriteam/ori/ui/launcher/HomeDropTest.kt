package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.HomeArrangement
import com.oriteam.ori.domain.launcher.HomeCell
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.LauncherSettings
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
private const val FRAME_MILLIS = 16L
private const val WIDGET_PICTURE_PX = 40

/**
 * Dropping on the home screen, as on One UI: the icon travels from where it was let go to its cell (not from where it
 * was before), the frame of the cell shows for apps only when chosen (a widget always shows its own), and a tap on the
 * empty screen leaves the editing.
 */
@RunWith(AndroidJUnit4::class)
class HomeDropTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(4) { launcherApp("App $it") }
    private val host = FakeHomeWidgetHost().apply { allocate() }
    private val widgetId = host.allocated.single()
    private val settings = FakeLauncherSettingsRepository(
        LauncherSettings(
            favorites = apps.map { it.key },
            arrangement = HomeArrangement.FREE,
            positions = apps.mapIndexed { index, app -> app.key to HomeCell(0, 0, index % 3) }.toMap(),
            isClockSetUp = true
        )
    )
    private lateinit var viewModel: LauncherViewModel

    /** The widgets lifted (held), to carry them as they look. */
    private val pictured = mutableListOf<Int>()

    private fun render(initial: LauncherSettings = settings.settings.value) {
        settings.settings.value = initial
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
                    LauncherSlots(
                        panel = { Text("Painel") },
                        widget = { _, modifier -> Box(modifier) },
                        liftWidget = { widgetId ->
                            pictured += widgetId
                            ImageBitmap(WIDGET_PICTURE_PX, WIDGET_PICTURE_PX)
                        }
                    )
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun centerOf(description: String) =
        composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.center

    private fun holdAndMove(start: Offset, end: Offset) {
        val step = (end - start) / DRAG_STEPS.toFloat()
        composeRule.onScreenRoot().performTouchInput { down(start) }
        composeRule.mainClock.advanceTimeBy(HOLD_MILLIS)
        repeat(DRAG_STEPS) {
            composeRule.onScreenRoot().performTouchInput { moveBy(step) }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun `a dropped icon travels from where it was let go to its cell, not from where it was`() {
        render()
        val start = centerOf("App 0")
        val cell = composeRule.onNodeWithContentDescription("App 0").fetchSemanticsNode().boundsInRoot
        // Two rows down, in the free third column: an empty cell far from where the icon was.
        val dropAt = Offset(centerOf("App 2").x, start.y + cell.height * 2.5f)
        holdAndMove(start, dropAt)

        composeRule.mainClock.autoAdvance = false
        composeRule.onScreenRoot().performTouchInput { up() }
        repeat(3) { composeRule.mainClock.advanceTimeBy(FRAME_MILLIS) }
        val justAfter = centerOf("App 0")
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        val settled = centerOf("App 0")

        // Right after the drop the icon is near where it was let go, never back where it started.
        assertTrue("near the drop: $justAfter, $dropAt", (justAfter - dropAt).getDistance() < cell.height)
        assertTrue("not at the start: $justAfter, $start", (justAfter - start).getDistance() > cell.height)
        assertEquals(HomeCell(0, 2, 2), settings.settings.value.positions[apps[0].key])
        assertTrue("settled in its cell: $settled", (settled - dropAt).getDistance() < cell.height)
    }

    @Test
    fun `an app carried shows no frame by default, and a widget always shows its own and is carried as it looks`() {
        render(
            settings.settings.value.copy(
                favorites = apps.map { it.key } + widgetKey(widgetId),
                widgetSpans = mapOf(widgetId to HomeSpan(2, 2)),
                positions = settings.settings.value.positions + (widgetKey(widgetId) to HomeCell(0, 2, 0))
            )
        )
        val start = centerOf("App 0")
        holdAndMove(start, Offset(start.x, start.y + 300f))
        composeRule.onNodeWithTag(DROP_PREVIEW_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onScreenRoot().performTouchInput { up() }
        composeRule.waitForIdle()

        assertTrue("an app needs no picture: $pictured", pictured.isEmpty())
        val widget = centerOf("Widget")
        holdAndMove(widget, Offset(widget.x, widget.y - 100f))
        composeRule.onNodeWithTag(DROP_PREVIEW_TAG, useUnmergedTree = true).assertExists()
        // Held, the widget is lifted once (its own touch ends) and carried as it looks, as on One UI.
        assertEquals(listOf(widgetId), pictured)
        composeRule.onScreenRoot().performTouchInput { up() }
        composeRule.waitForIdle()
    }

    @Test
    fun `while editing, a tap on the empty screen leaves the editing, and a tap on an icon does not`() {
        render()
        viewModel.onEditingChange(true)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("App 1").performTouchInput { click() }
        composeRule.waitForIdle()
        assertTrue(viewModel.uiState.value.isEditingHome)

        val below = centerOf("App 1")
        composeRule.onScreenRoot().performTouchInput { click(Offset(below.x, below.y + 400f)) }
        composeRule.waitForIdle()
        assertFalse(viewModel.uiState.value.isEditingHome)
    }
}
