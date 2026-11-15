package com.oriteam.ori.ui.launcher

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.HomeFolder
import com.oriteam.ori.domain.launcher.LauncherIconSize
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.folderKey
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

private const val HOLD_MILLIS = 1_000L
private const val DRAG_STEPS = 10
private const val TILE_TOLERANCE = 0.1f

/** How long the settings take to be written, as on a device. */
private const val WRITE_MILLIS = 300L

/** Enough frames for a change to reach the screen, and enough time for any entrance to end. */
private const val MAX_FRAMES = 10
private const val SETTLE_MILLIS = 2_000L

/**
 * Folders on the home screen, as on One UI: a tap opens one over the pages and back closes it; an app carried onto
 * the middle of another makes one; "Criar pasta" makes one from the menu, filled from its list; the name is changed
 * right there; an app carried out of an open folder lands on the pages.
 */
@RunWith(AndroidJUnit4::class)
class HomeFoldersScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(8) { launcherApp("App $it") }
    private val settings = FakeLauncherSettingsRepository(
        LauncherSettings(favorites = apps.take(4).map { it.key }, isClockSetUp = true)
    )
    private lateinit var viewModel: LauncherViewModel

    private val games = LauncherSettings(
        favorites = listOf(folderKey(1), apps[2].key),
        folders = mapOf(1 to HomeFolder("Jogos", listOf(apps[0].key, apps[1].key))),
        isClockSetUp = true
    )

    private fun render(initial: LauncherSettings = settings.settings.value) {
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

    private fun isShown(description: String) =
        composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()

    private fun boundsOf(description: String) =
        composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot

    private fun heightOf(description: String) =
        composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.height

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun `a tap opens a folder with its apps over the home screen, and back closes it`() {
        render(games)

        composeRule.onNodeWithContentDescription("Jogos, pasta com 2 apps").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("App 1").assertIsDisplayed()

        pressBack()
        composeRule.onNodeWithContentDescription("App 1").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Jogos, pasta com 2 apps").assertIsDisplayed()
        assertTrue(!composeRule.activity.isFinishing)
    }

    @Test
    fun `with Maximo the apps of an open folder are the same tiles as on the home screen`() {
        render(games.copy(iconSize = LauncherIconSize.MAXIMUM))
        val onHome = heightOf("App 2")

        composeRule.onNodeWithContentDescription("Jogos, pasta com 2 apps").performClick()
        composeRule.waitForIdle()

        // An icon with its name under it is less than half as tall as a tile.
        assertEquals(onHome, heightOf("App 1"), onHome * TILE_TOLERANCE)
    }

    @Test
    fun `an app carried onto the middle of another makes a folder with both, in its place`() {
        render()

        holdAndCarry(centerOf("App 1"), centerOf("App 0"))

        assertEquals(listOf(apps[0].key, apps[1].key), settings.settings.value.folders.values.single().apps)
        composeRule.onNodeWithContentDescription("Pasta, pasta com 2 apps").assertIsDisplayed()
        assertEquals(folderKey(1), settings.settings.value.favorites.first())
    }

    @Test
    fun `an app let go on another stays over it until the folder is written, instead of blinking back`() {
        render()
        settings.writeDelayMillis = WRITE_MILLIS

        holdAndCarry(centerOf("App 1"), centerOf("App 0"))
        // Not written yet: the copy stays over the other app (gone, the app showed again in its old place).
        composeRule.onNodeWithTag(CARRIED_COPY_TAG).assertExists()
        assertTrue(settings.settings.value.folders.isEmpty())

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(WRITE_MILLIS * 2))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CARRIED_COPY_TAG).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Pasta, pasta com 2 apps").assertIsDisplayed()
    }

    @Test
    fun `apps taken out of a folder are there at once, instead of rising in late`() {
        render(games)
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnUiThread { viewModel.folders.onTakeOut(apps[0].key) }
        repeat(MAX_FRAMES) { if (!isShown("App 0")) composeRule.mainClock.advanceTimeByFrame() }
        // The folder left with one app became it: both are new on the page, and neither rises in.
        val first = listOf(boundsOf("App 0"), boundsOf("App 1"))
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)

        assertEquals(first, listOf(boundsOf("App 0"), boundsOf("App 1")))
    }

    @Test
    fun `Criar pasta opens a new folder, and its list adds the apps checked`() {
        render()

        composeRule.onNodeWithContentDescription("App 2").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Criar pasta").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Adicionar apps").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("App 6").performClick()
        composeRule.onNodeWithText("Pronto").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(apps[2].key, apps[6].key), settings.settings.value.folders.values.single().apps)
        composeRule.onNodeWithContentDescription("App 6").assertIsDisplayed()
    }

    @Test
    fun `the name is changed at the top of the open folder`() {
        render(games)
        composeRule.onNodeWithContentDescription("Jogos, pasta com 2 apps").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Nome da pasta").performTextReplacement("Meus jogos")
        composeRule.onNodeWithContentDescription("Nome da pasta").performImeAction()
        composeRule.waitForIdle()

        assertEquals("Meus jogos", settings.settings.value.folders.getValue(1).name)
    }

    @Test
    fun `an app carried out of an open folder lands on the pages, and the folder left with one app becomes it`() {
        render(games)
        composeRule.onNodeWithContentDescription("Jogos, pasta com 2 apps").performClick()
        composeRule.waitForIdle()
        val app1 = centerOf("App 1")
        val below = composeRule.onNodeWithContentDescription("App 2").fetchSemanticsNode().boundsInRoot
        // A free cell of the page, a column to the right and rows under App 2: far from the folder and from where App 1
        // shows in the open folder, so the finger really carries it.
        val freeCell = Offset(below.center.x + below.width, below.bottom + below.height * 2.5f)

        holdAndCarry(app1, freeCell)

        assertTrue(settings.settings.value.folders.isEmpty())
        assertTrue(apps[1].key in settings.settings.value.favorites)
        assertEquals(apps[0].key, settings.settings.value.favorites.first())
    }
}
