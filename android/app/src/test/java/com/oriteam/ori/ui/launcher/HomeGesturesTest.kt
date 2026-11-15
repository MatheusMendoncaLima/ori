package com.oriteam.ori.ui.launcher

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.HomeArrangement
import com.oriteam.ori.domain.launcher.HomeCell
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

private const val HOLD_MILLIS = 1_000L
private const val DRAG_STEPS = 10

/**
 * The gestures of the home screen keep up with the screen: a drop is measured on the grid shown now (not the one of
 * the first touch), a hold while editing carries at once, the screen reader moves items in the order of all the
 * pages, a short window never squeezes the icons, and the system bars at a side never cover them.
 */
@RunWith(AndroidJUnit4::class)
class HomeGesturesTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(20) { launcherApp("App $it") }
    private val settings = FakeLauncherSettingsRepository(
        LauncherSettings(favorites = apps.take(8).map { it.key }, isClockSetUp = true)
    )
    private val installed = FakeInstalledApps(apps)
    private val homeSystem = FakeHomeSystem()
    private lateinit var viewModel: LauncherViewModel

    private fun render(widthDp: Int = 360, heightDp: Int = 800, initial: LauncherSettings = settings.settings.value) {
        settings.settings.value = initial
        viewModel = LauncherViewModel(
            installed,
            settings,
            FakeDevicePreferencesRepository(),
            FakeWallpaperStore(),
            FakeHomeWidgetHost(),
            FakeLockScreenWallpaper(),
            homeSystem
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

    private fun centerOf(description: String) =
        composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.center

    /** A tap on the empty place after the last icon: the home screen has been touched once. */
    private fun touchOnce() {
        val last = composeRule.onNodeWithContentDescription("App 7").fetchSemanticsNode().boundsInRoot
        composeRule.onScreenRoot().performTouchInput { click(Offset(last.right + last.width / 2, last.center.y)) }
        composeRule.waitForIdle()
    }

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

    private fun customActionsOf(description: String): List<String> =
        composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().config
            .getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }

    @Test
    fun `after the icons grow, a drop lands on the cell under the finger of the new grid`() {
        render(initial = settings.settings.value.copy(arrangement = HomeArrangement.FREE))
        touchOnce()

        settings.settings.value = settings.settings.value.copy(iconSize = LauncherIconSize.EXTRA_LARGE)
        composeRule.waitForIdle()
        // Below the middle of App 3's icon, still in its cell: a move, not a folder.
        val bounds = composeRule.onNodeWithContentDescription("App 3").fetchSemanticsNode().boundsInRoot
        holdAndCarry(centerOf("App 0"), Offset(bounds.center.x, bounds.center.y + bounds.height * 0.45f))

        // Two columns now: App 3 was at the second row, second column, and the two changed places.
        assertEquals(HomeCell(0, 1, 1), settings.settings.value.positions[apps[0].key])
        assertEquals(HomeCell(0, 0, 0), settings.settings.value.positions[apps[3].key])
    }

    @Test
    fun `once editing, a hold carries the icon at once and letting go opens no menu`() {
        render()
        touchOnce()

        viewModel.onEditingChange(true)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("App 0").performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Informações do app").assertDoesNotExist()
        assertEquals(apps[0].key, settings.settings.value.favorites.first())
    }

    @Test
    fun `the screen reader moves the first icon of the second page before, and the very last one not after`() {
        render(initial = settings.settings.value.copy(favorites = apps.map { it.key }))
        composeRule.onScreenRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertTrue(customActionsOf("App 12").contains("Mover para antes"))
        assertFalse(customActionsOf("App 19").contains("Mover para depois"))
    }

    @Test
    fun `a tablet turned keeps whole icons with their names, smaller instead of squeezed`() {
        render(widthDp = 1280, heightDp = 800)

        val tile = composeRule.onNodeWithContentDescription("App 0").bounds
        // An icon of at least 40 dp and a line of its name: squeezed rows would cut the name off.
        assertTrue("tile height ${tile.height}", tile.height >= 40.dp + 20.dp)
    }

    @Test
    fun `a message on the home screen does not squeeze the dock, and goes away after a while`() {
        render(initial = settings.settings.value.copy(dock = listOf(apps[10].key)))
        val before = composeRule.onNodeWithContentDescription("App 10").bounds.height.value

        installed.canOpen = false
        viewModel.onOpenApp(apps[0])
        composeRule.waitForIdle()

        assertEquals(before, composeRule.onNodeWithContentDescription("App 10").bounds.height.value, 0.5f)
        composeRule.onNodeWithText("Não foi possível abrir este app. Ele pode ter sido removido.").assertExists()

        composeRule.mainClock.advanceTimeBy(HOME_MESSAGE_MILLIS + 1_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Não foi possível abrir este app. Ele pode ter sido removido.").assertDoesNotExist()
    }

    @Test
    fun `with the navigation bar at the side, as on a phone turned, the icons stay clear of it`() {
        render(widthDp = 800, heightDp = 360)
        val sideBar = with(composeRule.density) { 48.dp.roundToPx() }

        composeRule.runOnUiThread {
            val content = composeRule.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, sideBar, 0))
                .build()
            ViewCompat.dispatchApplyWindowInsets(content, insets)
        }
        composeRule.waitForIdle()

        // The icon farthest to the right on the page shown.
        val shown = (0 until 8).map { "App $it" }
            .filter { composeRule.onAllNodesWithContentDescription(it).fetchSemanticsNodes().isNotEmpty() }
        val rightEdge = shown.maxOf { composeRule.onNodeWithContentDescription(it).bounds.right }
        assertTrue("right edge $rightEdge", rightEdge <= 800.dp - 48.dp)
    }

    @Test
    fun `on a locked home screen an icon held and moved stays, says why, and its menu has no Remover`() {
        render(initial = settings.settings.value.copy(isLayoutLocked = true))
        touchOnce()

        holdAndCarry(centerOf("App 0"), centerOf("App 3"))

        assertEquals(apps[0].key, settings.settings.value.favorites.first())
        composeRule.onNodeWithText("A tela inicial está travada: os itens não mudam de lugar.").assertExists()
        assertFalse("Remover" in customActionsOf("App 0"))

        composeRule.onNodeWithContentDescription("App 0").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Informações do app").assertExists()
        composeRule.onNodeWithText("Remover").assertDoesNotExist()
    }

    @Test
    fun `a swipe down on the home screen opens Android's notifications, as on One UI`() {
        render()

        composeRule.onScreenRoot().performTouchInput {
            swipeDown(startY = height * 0.3f, endY = height * 0.7f)
        }
        composeRule.waitForIdle()

        assertEquals(1, homeSystem.notificationsOpened)
        assertEquals(LauncherPane.HOME, viewModel.uiState.value.pane)
    }
}
