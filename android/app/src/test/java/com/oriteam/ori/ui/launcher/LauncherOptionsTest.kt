package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.GridChoice
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.StimulusLevel
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.FakeHomeSystem
import com.oriteam.ori.testing.FakeHomeWidgetHost
import com.oriteam.ori.testing.FakeInstalledApps
import com.oriteam.ori.testing.FakeLauncherSettingsRepository
import com.oriteam.ori.testing.FakeLockScreenWallpaper
import com.oriteam.ori.testing.FakeWallpaperStore
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The options of ORI Home: the everyday ones on the first screen (the icon size, its names, the badges, locking), and
 * what changes how the home screen works (the grid, the placement of the icons, the gestures, the buttons) one step
 * in, in "Opções avançadas", which back closes before the options.
 */
@RunWith(AndroidJUnit4::class)
class LauncherOptionsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val settings = FakeLauncherSettingsRepository(LauncherSettings(isClockSetUp = true))
    private lateinit var viewModel: LauncherViewModel

    private fun showOptions() {
        viewModel = LauncherViewModel(
            FakeInstalledApps(),
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
        viewModel.onOpenPane(LauncherPane.OPTIONS)
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun `the first screen has the everyday options, and the grid and the gestures are in the advanced ones`() {
        showOptions()

        listOf("Tamanho dos ícones", "Selos dos ícones", "Travar a tela inicial", "Opções avançadas").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
        listOf("Grade da tela inicial", "Posição dos ícones", "Mostrar botões").forEach {
            composeRule.onNodeWithText(it).assertDoesNotExist()
        }

        composeRule.onNodeWithText("Opções avançadas").performScrollTo().performClick()
        composeRule.waitForIdle()

        listOf("Grade da tela inicial", "Posição dos ícones", "Mostrar botões").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
        composeRule.onNodeWithText("Tamanho dos ícones").assertDoesNotExist()
    }

    @Test
    fun `a level of stimulus sets every choice, and a choice changed by hand makes the level custom`() {
        showOptions()

        // The first "Nenhum" is the level; the badges have one of their own further down.
        composeRule.onAllNodesWithText("Nenhum")[0].performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(StimulusLevel.NONE.choices, settings.settings.value.stimulus)
        composeRule.onNodeWithText("Personalizado").assertDoesNotExist()

        composeRule.onNodeWithText("Ícones coloridos").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(settings.settings.value.stimulus.colorfulIcons)
        composeRule.onNodeWithText("Personalizado").assertExists()

        composeRule.onNodeWithText("Baixo").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(StimulusLevel.LOW.choices, settings.settings.value.stimulus)
        composeRule.onNodeWithText("Personalizado").assertDoesNotExist()
    }

    @Test
    fun `a grid chosen in the advanced options is kept, and back goes to the options before the home screen`() {
        showOptions()
        composeRule.onNodeWithText("Opções avançadas").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("4 × 5").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(GridChoice(4, 5), settings.settings.value.homeGrid)

        pressBack()
        composeRule.onNodeWithText("Tamanho dos ícones").assertExists()
        assertEquals(LauncherPane.OPTIONS, viewModel.uiState.value.pane)
        pressBack()
        assertEquals(LauncherPane.HOME, viewModel.uiState.value.pane)
    }
}
