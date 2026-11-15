package com.oriteam.ori.ui.launcher

import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.HomeArrangement
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

private const val HOLD_MILLIS = 1_000L
private const val DRAG_STEPS = 10

/** Long enough at the edge of the screen for one page to turn, not two. */
private const val PAGE_TURN_MILLIS = 1_000L

/** How long the settings take to be written in the test of the new page. */
private const val WRITE_MILLIS = 300L

/**
 * The whole screen takes items, whatever its shape: a tablet upright down to the dock, a tablet turned with the grid
 * transposed and cells of its own (as on One UI), and the page turns at an edge clear of the system bars.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenSizeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(30) { launcherApp("App $it") }
    private val settings = FakeLauncherSettingsRepository()

    private fun render(widthDp: Int, heightDp: Int, initial: LauncherSettings) {
        settings.settings.value = initial
        val viewModel = LauncherViewModel(
            FakeInstalledApps(apps),
            settings,
            FakeDevicePreferencesRepository(),
            FakeWallpaperStore(),
            FakeHomeWidgetHost(),
            FakeLockScreenWallpaper(),
            FakeHomeSystem()
        )
        composeRule.setContent {
            OriTestWindow(widthDp = widthDp, heightDp = heightDp) {
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

    private fun pages(count: Int, arrangement: HomeArrangement) = LauncherSettings(
        favorites = apps.take(count).map { it.key },
        isClockSetUp = true,
        arrangement = arrangement
    )

    private fun centerOf(description: String) =
        composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.center

    private fun screenHeight() = composeRule.onScreenRoot().fetchSemanticsNode().boundsInRoot.height

    /** Holds [start], moves to [end] step by step, waits there [waitMillis] and lets go. */
    private fun holdAndCarry(start: Offset, end: Offset, waitMillis: Long = 0) {
        val step = (end - start) / DRAG_STEPS.toFloat()
        composeRule.onScreenRoot().performTouchInput { down(start) }
        composeRule.mainClock.advanceTimeBy(HOLD_MILLIS)
        repeat(DRAG_STEPS) {
            composeRule.onScreenRoot().performTouchInput { moveBy(step) }
            composeRule.waitForIdle()
        }
        composeRule.mainClock.advanceTimeBy(waitMillis)
        composeRule.waitForIdle()
        composeRule.onScreenRoot().performTouchInput { up() }
        composeRule.waitForIdle()
    }

    @Test
    fun `with a system bar at the side, an icon held at the right edge still turns the page`() {
        render(1280, 800, pages(20, HomeArrangement.PACKED))
        val sideBar = with(composeRule.density) { 48.dp.roundToPx() }
        composeRule.runOnUiThread {
            val content = composeRule.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, sideBar, 0))
                .build()
            ViewCompat.dispatchApplyWindowInsets(content, insets)
        }
        composeRule.waitForIdle()
        val start = centerOf("App 0")
        val beforeTheBar = with(composeRule.density) { (1280.dp - 48.dp - 20.dp).toPx() }

        holdAndCarry(start, Offset(beforeTheBar, start.y), waitMillis = PAGE_TURN_MILLIS)

        val index = settings.settings.value.favorites.indexOf(apps[0].key)
        assertTrue("index $index", index >= 6)
    }

    @Test
    fun `on a tablet upright the whole height takes icons, down to the dock`() {
        render(800, 1280, pages(8, HomeArrangement.FREE))
        val start = centerOf("App 0")

        holdAndCarry(start, Offset(start.x, screenHeight() * 0.7f))

        val row = settings.settings.value.positions[apps[0].key]?.row ?: 0
        assertTrue("row $row", row >= 4)
    }

    @Test
    fun `a tablet turned transposes the grid, so a full page stays one page`() {
        render(1280, 800, pages(30, HomeArrangement.PACKED))

        composeRule.onNodeWithContentDescription("App 29").assertIsDisplayed()
    }

    @Test
    fun `turned, an icon carried to another cell keeps its upright cell`() {
        render(1280, 800, pages(8, HomeArrangement.FREE))
        val start = centerOf("App 0")
        // The row under the lowest icon is free: a cell there, under App 0 (not the middle of an icon, where it would
        // make a folder).
        val cell = composeRule.onNodeWithContentDescription("App 0").fetchSemanticsNode().boundsInRoot
        val lowest = (0 until 8).maxOf { centerOf("App $it").y }
        val freeCell = Offset(start.x, lowest + cell.height * 1.2f)

        holdAndCarry(start, freeCell)

        val stored = settings.settings.value
        assertTrue("turned ${stored.positionsTurned[apps[0].key]}", stored.positionsTurned[apps[0].key] != null)
        assertNull(stored.positions[apps[0].key])
    }

    @Test
    fun `an icon dropped on the new page made at the edge stays in sight, on that page`() {
        render(1280, 800, pages(20, HomeArrangement.FREE))
        settings.writeDelayMillis = WRITE_MILLIS
        val start = centerOf("App 0")
        val rightEdge = with(composeRule.density) { (1280.dp - 20.dp).toPx() }

        holdAndCarry(start, Offset(rightEdge, start.y), waitMillis = PAGE_TURN_MILLIS)
        // The drop is written a moment after the finger lets go, as on the device.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(WRITE_MILLIS * 2))
        composeRule.mainClock.advanceTimeBy(PAGE_TURN_MILLIS)
        composeRule.waitForIdle()

        assertEquals(1, settings.settings.value.positionsTurned[apps[0].key]?.page)
        composeRule.onNodeWithContentDescription("App 0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("App 1").assertDoesNotExist()
    }
}
