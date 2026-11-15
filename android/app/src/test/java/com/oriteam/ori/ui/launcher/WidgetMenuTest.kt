package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.CLOCK_KEY
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
import com.oriteam.ori.ui.components.text.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The menu of a widget or of the clock has its name on top, as the menu of an app has (it had none). */
@RunWith(AndroidJUnit4::class)
class WidgetMenuTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render() {
        val viewModel = LauncherViewModel(
            FakeInstalledApps(),
            FakeLauncherSettingsRepository(
                LauncherSettings(
                    favorites = listOf(CLOCK_KEY, widgetKey(5)),
                    widgetSpans = mapOf(5 to HomeSpan(2, 2)),
                    isClockSetUp = true
                )
            ),
            FakeDevicePreferencesRepository(),
            FakeWallpaperStore(),
            FakeHomeWidgetHost(),
            FakeLockScreenWallpaper(),
            FakeHomeSystem()
        )
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 800) {
                CompositionLocalProvider(LocalWidgetLabelLoader provides { "Clima" }) {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    LauncherScreen(
                        state,
                        viewModel,
                        LauncherActions(onOpenOri = {}, onLeaveHome = {}),
                        LauncherSlots(
                            panel = { Text("Painel") },
                            widget = { id, modifier -> Box(modifier.semantics { contentDescription = "Widget $id" }) }
                        )
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertMenuTitle(title: String) {
        composeRule.onNode(hasText(title) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
    }

    @Test
    fun `a widget held and let go has its own name on its menu`() {
        render()

        composeRule.onNodeWithContentDescription("Widget 5").performTouchInput { longClick() }
        composeRule.waitForIdle()

        assertMenuTitle("Clima")
    }
}
