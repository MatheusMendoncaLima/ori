package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.WidgetProvider
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.FakeHomeSystem
import com.oriteam.ori.testing.FakeHomeWidgetHost
import com.oriteam.ori.testing.FakeInstalledApps
import com.oriteam.ori.testing.FakeLauncherSettingsRepository
import com.oriteam.ori.testing.FakeLockScreenWallpaper
import com.oriteam.ori.testing.FakeWallpaperStore
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.widgetProvider
import com.oriteam.ori.ui.components.text.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The list of widgets slides in over the home screen; after this it is still. */
private const val PANE_SETTLE_MILLIS = 2_000L

/**
 * The list of widgets says the size a widget will take on this home screen, not only the size its app asks for, and
 * says when Android could not give some widgets (or any) instead of failing.
 */
@RunWith(AndroidJUnit4::class)
class WidgetListTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val clima = widgetProvider("Clima", span = HomeSpan(5, 2))

    private fun showWidgets(widthDp: Int, offered: List<WidgetProvider> = listOf(clima), isComplete: Boolean = true) {
        val viewModel = LauncherViewModel(
            FakeInstalledApps(),
            FakeLauncherSettingsRepository(LauncherSettings(isClockSetUp = true)),
            FakeDevicePreferencesRepository(),
            FakeWallpaperStore(),
            FakeHomeWidgetHost(offered, isComplete = isComplete),
            FakeLockScreenWallpaper(),
            FakeHomeSystem()
        )
        composeRule.setContent {
            OriTestWindow(widthDp = widthDp, heightDp = 800) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LauncherScreen(
                    state,
                    viewModel,
                    LauncherActions(onOpenOri = {}, onLeaveHome = {}),
                    LauncherSlots(panel = { Text("Painel") }, widget = { _, modifier -> Box(modifier) })
                )
            }
        }
        viewModel.onOpenPane(LauncherPane.WIDGETS)
        composeRule.mainClock.advanceTimeBy(PANE_SETTLE_MILLIS)
        composeRule.waitForIdle()
    }

    @Test
    fun `a widget wider than a phone's four columns is listed with the four it will take`() {
        showWidgets(widthDp = 360)

        composeRule.onNodeWithContentDescription("Adicionar Clima, 4 por 2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Adicionar Clima, 5 por 2").assertDoesNotExist()
    }

    @Test
    fun `on a tablet the widget keeps the size its app asks for`() {
        showWidgets(widthDp = 800)

        composeRule.onNodeWithContentDescription("Adicionar Clima, 5 por 2").assertIsDisplayed()
    }

    @Test
    fun `when Android gives only some widgets, the list shows them and says some are missing`() {
        showWidgets(widthDp = 360, isComplete = false)

        composeRule.onNodeWithContentDescription("Adicionar Clima, 4 por 2").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Alguns widgets não puderam ser lidos agora. Tente abrir de novo daqui a pouco."
        ).assertIsDisplayed()
    }

    @Test
    fun `when Android gives no widget at all, the list says it failed, not that there are none`() {
        showWidgets(widthDp = 360, offered = emptyList(), isComplete = false)

        composeRule.onNodeWithText("Não foi possível ler os widgets").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum widget").assertDoesNotExist()
    }

    @Test
    fun `widgets of one app with the same name and size are told apart by their description`() {
        val today = widgetProvider("Tempo", id = "app.tempo/.Today@0", description = "A previsão de hoje")
        val week = widgetProvider("Tempo", id = "app.tempo/.Week@0", description = "A previsão da semana")
        showWidgets(widthDp = 360, offered = listOf(today, week))

        // The row is read as one sentence: the name, the size and what tells it apart.
        composeRule.onNodeWithContentDescription("Adicionar Tempo, 2 por 2 · A previsão de hoje").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Adicionar Tempo, 2 por 2 · A previsão da semana").assertIsDisplayed()
    }

    @Test
    fun `the search finds a widget by its name or its app, without caring about accents`() {
        showWidgets(widthDp = 360, offered = listOf(clima, widgetProvider("Música")))

        composeRule.onNode(hasSetTextAction()).performTextInput("musica")
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Adicionar Música, 2 por 2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Adicionar Clima, 4 por 2").assertDoesNotExist()
    }
}
