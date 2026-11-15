package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.CLOCK_KEY
import com.oriteam.ori.domain.launcher.GridChoice
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
private const val PANEL_TEXT = "Painel de teste"

/** How far below the middle of an icon a drop moves it instead of making a folder. */
private const val OFF_MIDDLE_SHARE = 0.45f

/**
 * The home screen: columns follow the window and the icon size (six on a tablet upright, transposed turned), the
 * panel of ORI is to the left, the pages go to the right (or one list scrolls), all the apps come up with the finger,
 * and holding an item opens its menu or carries it (to a cell, the dock or "Remover"), as on Android.
 */
@RunWith(AndroidJUnit4::class)
class LauncherScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(20) { launcherApp("App $it") }
    private val settings = FakeLauncherSettingsRepository(
        LauncherSettings(favorites = apps.take(8).map { it.key }, isClockSetUp = true)
    )
    private lateinit var viewModel: LauncherViewModel

    private val slots = LauncherSlots(
        panel = { Text(PANEL_TEXT) },
        widget = { _, modifier -> Box(modifier) }
    )

    private fun render(widthDp: Int = 360, heightDp: Int = 800, initial: LauncherSettings = settings.settings.value) {
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
            OriTestWindow(widthDp = widthDp, heightDp = heightDp) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LauncherScreen(state, viewModel, LauncherActions(onOpenOri = {}, onLeaveHome = {}), slots)
            }
        }
    }

    private fun iconsInFirstRow(): Int {
        val top = composeRule.onNodeWithContentDescription("App 0").bounds.top
        // Only the apps on the page shown: with large icons some are on the next page.
        val shown = (0 until 8).filter { isShown("App $it") }
        return shown.count { composeRule.onNodeWithContentDescription("App $it").bounds.top == top }
    }

    private fun isShown(description: String): Boolean =
        composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()

    /** Holds the finger on [start], then moves it to [end] step by step, as a finger does, and lets go. */
    private fun holdAndCarry(start: Offset, end: Offset) {
        val step = (end - start) / DRAG_STEPS.toFloat()
        composeRule.onScreenRoot().performTouchInput { down(start) }
        // The hold is measured by the clock of the screen, not by the time of the touch events.
        composeRule.mainClock.advanceTimeBy(HOLD_MILLIS)
        repeat(DRAG_STEPS) {
            composeRule.onScreenRoot().performTouchInput { moveBy(step) }
            composeRule.waitForIdle()
        }
        composeRule.onScreenRoot().performTouchInput { up() }
        composeRule.waitForIdle()
    }

    /** An empty place of the first page: the cell under the last of the eight icons, in the row after it. */
    private fun emptyPlace(): Offset {
        val last = composeRule.onNodeWithContentDescription("App 7").fetchSemanticsNode().boundsInRoot
        return Offset(last.center.x, last.bottom + last.height / 2)
    }

    /** A point in the cell of the item, below the middle of its icon: a drop there moves, not makes a folder. */
    private fun offMiddleOf(description: String): Offset {
        val bounds = composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
        return Offset(bounds.center.x, bounds.center.y + bounds.height * OFF_MIDDLE_SHARE)
    }

    private fun centerOf(description: String) =
        composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.center

    @Test
    fun `a phone shows at least three icons in a row, and the largest icons two`() {
        render()
        assertTrue("phone", iconsInFirstRow() >= 3)

        composeRule.activityRule.scenario.recreate()
        render(initial = settings.settings.value.copy(iconSize = LauncherIconSize.EXTRA_LARGE))
        assertEquals(2, iconsInFirstRow())
    }

    @Test
    fun `a tablet shows six columns of medium icons upright, and turned the grid transposed to more columns`() {
        render(widthDp = 800, heightDp = 1280)
        assertEquals(6, iconsInFirstRow())

        composeRule.activityRule.scenario.recreate()
        render(widthDp = 1280, heightDp = 800)
        // The upright rows (7 or 8 here, by the height of a name) become the columns.
        assertTrue("turned columns ${iconsInFirstRow()}", iconsInFirstRow() in 7..8)
    }

    @Test
    fun `with a grid chosen, huge icons keep its six columns on a tablet instead of four`() {
        render(
            widthDp = 800,
            heightDp = 1280,
            initial = settings.settings.value.copy(
                iconSize = LauncherIconSize.EXTRA_LARGE,
                homeGrid = GridChoice(6, 8)
            )
        )

        assertEquals(6, iconsInFirstRow())
    }

    @Test
    fun `the clock is a widget on the home screen, and it can be taken out`() {
        render(initial = settings.settings.value.copy(isClockSetUp = false))
        composeRule.waitForIdle()
        assertEquals(CLOCK_KEY, settings.settings.value.favorites.first())

        viewModel.layout.onRemove(CLOCK_KEY)
        composeRule.waitForIdle()

        assertFalse(CLOCK_KEY in settings.settings.value.favorites)
    }

    @Test
    fun `the routine and the phrases are in the panel to the left`() {
        render()
        composeRule.onNodeWithText(PANEL_TEXT).assertDoesNotExist()

        composeRule.onScreenRoot().performTouchInput { swipeRight() }

        composeRule.onNodeWithText(PANEL_TEXT).assertIsDisplayed()
    }

    @Test
    fun `more icons than a page holds go to the next page, to the right`() {
        // Large icons: a page of the phone holds fewer than the twenty.
        render(
            initial = settings.settings.value.copy(favorites = apps.map { it.key }, iconSize = LauncherIconSize.LARGE)
        )
        val firstElsewhere = (0 until 20).first { !isShown("App $it") }

        composeRule.onScreenRoot().performTouchInput { swipeLeft() }

        composeRule.onNodeWithContentDescription("App $firstElsewhere").assertIsDisplayed()
    }

    @Test
    fun `a drag up brings all the apps up, and a drag down takes them down`() {
        render()
        composeRule.onNodeWithText("App 12").assertDoesNotExist()

        composeRule.onScreenRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        assertEquals(LauncherPane.DRAWER, viewModel.uiState.value.pane)
        composeRule.onNodeWithContentDescription("App 12").assertIsDisplayed()

        composeRule.onScreenRoot().performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        assertEquals(LauncherPane.HOME, viewModel.uiState.value.pane)
    }

    @Test
    fun `with buttons, editing is a button at the top and all the apps a button in the middle of the dock`() {
        render(initial = settings.settings.value.copy(showsButtons = true, dock = listOf(apps[10].key)))
        // With one app in the dock, the button stays in the very middle and the app goes to its side.
        val button = composeRule.onNodeWithContentDescription("Todos os apps").bounds
        assertEquals(180f, ((button.left + button.right) / 2).value, 1f)
        assertTrue(composeRule.onNodeWithContentDescription("App 10").bounds.left >= button.right)

        composeRule.onNodeWithContentDescription("Editar a tela inicial").performClick()
        composeRule.waitForIdle()
        assertTrue(viewModel.uiState.value.isEditingHome)
        // "Pronto" takes its place while editing.
        composeRule.onNodeWithContentDescription("Editar a tela inicial").assertDoesNotExist()
        composeRule.onNodeWithText("Pronto").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Todos os apps").performClick()
        assertEquals(LauncherPane.DRAWER, viewModel.uiState.value.pane)
    }

    @Test
    fun `without buttons, the home screen is edited by holding it, with no button for it`() {
        render()

        composeRule.onNodeWithContentDescription("Editar a tela inicial").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Todos os apps").assertDoesNotExist()
    }

    @Test
    fun `holding an icon and letting go opens its menu, as on One UI, with its name and what takes it away last`() {
        render()

        composeRule.onNodeWithContentDescription("App 0").performTouchInput { longClick() }

        composeRule.onNode(hasText("App 0") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
        val tops = listOf("Informações do app", "Editar a tela inicial", "Remover", "Desinstalar")
            .map { composeRule.onNodeWithText(it).bounds.top }
        assertEquals(tops.sorted(), tops)
        composeRule.onNodeWithText("Remover").performClick()
        composeRule.waitForIdle()
        assertFalse(apps[0].key in settings.settings.value.favorites)
    }

    @Test
    fun `holding an empty place edits, with the tools of Android`() {
        render()
        val empty = emptyPlace()

        composeRule.onScreenRoot().performTouchInput { longClick(empty) }

        assertTrue(viewModel.uiState.value.isEditingHome)
        composeRule.onNodeWithText("Papel de parede").assertIsDisplayed()
        composeRule.onNodeWithText("Widgets").assertIsDisplayed()
    }

    @Test
    fun `holding an icon and dragging it to the edge of another's cell moves it there`() {
        render()

        // Near the edge of the cell, not over the middle of the icon (that makes a folder, as on One UI).
        holdAndCarry(centerOf("App 0"), offMiddleOf("App 1"))

        assertEquals(apps[1].key, settings.settings.value.favorites[0])
        assertEquals(apps[0].key, settings.settings.value.favorites[1])
    }

    @Test
    fun `an icon carried to the dock goes to the dock`() {
        render()
        val bottom = composeRule.onScreenRoot().fetchSemanticsNode().boundsInRoot.bottom
        val dock = Offset(centerOf("App 0").x, bottom - with(composeRule.density) { 52.dp.toPx() })

        holdAndCarry(centerOf("App 0"), dock)

        assertEquals(listOf(apps[0].key), settings.settings.value.dock)
        assertFalse(apps[0].key in settings.settings.value.favorites)
    }

    @Test
    fun `the menu of an icon opens while the finger still holds it, and moving then closes it and carries the icon`() {
        render()
        val start = centerOf("App 0")
        val end = offMiddleOf("App 1")

        composeRule.onScreenRoot().performTouchInput { down(start) }
        composeRule.mainClock.advanceTimeBy(HOLD_MILLIS)
        composeRule.waitForIdle()
        // As on One UI: no need to let go to see the menu.
        composeRule.onNodeWithText("Informações do app").assertIsDisplayed()

        val step = (end - start) / DRAG_STEPS.toFloat()
        repeat(DRAG_STEPS) {
            composeRule.onScreenRoot().performTouchInput { moveBy(step) }
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithText("Informações do app").assertDoesNotExist()
        composeRule.onScreenRoot().performTouchInput { up() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Informações do app").assertDoesNotExist()
        assertEquals(apps[0].key, settings.settings.value.favorites[1])
    }

    @Test
    fun `in all the apps too, the menu opens while the finger still holds the app`() {
        render()
        composeRule.onScreenRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        val app = composeRule.onNodeWithContentDescription("App 12").fetchSemanticsNode().boundsInRoot.center

        composeRule.onScreenRoot().performTouchInput { down(app) }
        composeRule.mainClock.advanceTimeBy(HOLD_MILLIS)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Desinstalar").assertIsDisplayed()
        composeRule.onScreenRoot().performTouchInput { up() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Desinstalar").assertIsDisplayed()
    }

    @Test
    fun `holding an app in all the apps offers to uninstall it, not to hide it`() {
        render()
        composeRule.onScreenRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("App 12").performTouchInput { longClick() }

        composeRule.onNodeWithText("Desinstalar").assertIsDisplayed()
        composeRule.onNodeWithText("Ocultar da lista").assertDoesNotExist()
    }

    @Test
    fun `editing shows the pages smaller with the house and the bin, and plus adds a page`() {
        render()
        val whole = composeRule.onNodeWithContentDescription("App 0").fetchSemanticsNode().boundsInRoot
        val empty = emptyPlace()
        composeRule.onScreenRoot().performTouchInput { longClick(empty) }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Tela principal").assertIsDisplayed()
        // About two thirds, as on One UI, so the pages next to it show at its sides.
        val small = composeRule.onNodeWithContentDescription("App 0").fetchSemanticsNode().boundsInRoot
        assertEquals(0.7f, small.width / whole.width, 0.03f)
        composeRule.onScreenRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Adicionar uma tela").performClick()
        composeRule.waitForIdle()

        assertEquals(2, settings.settings.value.homePageCount)
        // The house and the bin of the new page are at its two corners, as on One UI (the screen is 360 dp wide).
        // The page before it is built too (its edge shows): the house of the new page is the one further right.
        val house = composeRule.onAllNodesWithContentDescription("Tela principal").fetchSemanticsNodes()
            .map { it.boundsInRoot }.maxBy { it.left }
        val bin = composeRule.onNodeWithContentDescription("Apagar a tela 2").bounds
        assertTrue("house ends at ${house.right} px", house.right < composeRule.density.run { 150.dp.toPx() })
        assertTrue("bin starts at ${bin.left}", bin.left > 210.dp)

        // The new page, empty, goes at once; the page before it is shown, not "+" that took its place.
        composeRule.onNodeWithContentDescription("Apagar a tela 2").performClick()
        composeRule.waitForIdle()
        assertEquals(1, settings.settings.value.homePageCount)
        composeRule.onNodeWithContentDescription("Tela principal").assertIsDisplayed()
    }

    @Test
    fun `an app held in all the apps and moved is carried to the home screen`() {
        render()
        val target = emptyPlace()
        composeRule.onScreenRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        holdAndCarry(centerOf("App 12"), target)

        assertTrue(apps[12].key in settings.settings.value.favorites)
        assertEquals(LauncherPane.HOME, viewModel.uiState.value.pane)
    }

    @Test
    fun `the clock widget takes only the columns its time and date need, with icons beside it on a tablet`() {
        render(widthDp = 1280, heightDp = 800, initial = settings.settings.value.copy(isClockSetUp = false))
        composeRule.waitForIdle()

        val clockTop = composeRule.onNodeWithContentDescription("App 0").bounds.top
        val clockRowApps = (0 until 8).count {
            composeRule.onNodeWithContentDescription("App $it").bounds.top == clockTop
        }
        assertTrue("apps beside the clock: $clockRowApps", clockRowApps in 1..7)
    }

    @Test
    fun `with the choice on, while an icon is carried the area it will take is shown, and it goes when dropped`() {
        render(initial = settings.settings.value.copy(showsDropPreview = true))
        val start = centerOf("App 0")
        val step = (offMiddleOf("App 4") - start) / DRAG_STEPS.toFloat()
        composeRule.onNodeWithTag(DROP_PREVIEW_TAG).assertDoesNotExist()

        composeRule.onScreenRoot().performTouchInput { down(start) }
        composeRule.mainClock.advanceTimeBy(HOLD_MILLIS)
        repeat(DRAG_STEPS) {
            composeRule.onScreenRoot().performTouchInput { moveBy(step) }
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag(DROP_PREVIEW_TAG, useUnmergedTree = true).assertExists()

        composeRule.onScreenRoot().performTouchInput { up() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(DROP_PREVIEW_TAG, useUnmergedTree = true).assertDoesNotExist()
    }
}
