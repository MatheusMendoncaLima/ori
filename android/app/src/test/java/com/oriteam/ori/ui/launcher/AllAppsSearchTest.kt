package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.oriteam.ori.testing.launcherApp
import com.oriteam.ori.ui.components.text.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The search of all the apps: clearing it shows the whole list from the top again. On the tablet, the list kept the
 * first app found ("Calculadora") at the top after the search was cleared, and the apps before it were out of sight.
 */
@RunWith(AndroidJUnit4::class)
class AllAppsSearchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** More apps than a screen shows: "App 00" to "App 59", in that order. */
    private val apps = List(60) { launcherApp("App %02d".format(it)) }

    @Test
    fun `clearing the search shows the list from its first app again`() {
        val viewModel = LauncherViewModel(
            FakeInstalledApps(apps),
            FakeLauncherSettingsRepository(LauncherSettings(isClockSetUp = true)),
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
        viewModel.onOpenPane(LauncherPane.DRAWER)
        composeRule.waitForIdle()

        viewModel.onQueryChange("App 45")
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("App 45").assertIsDisplayed()
        viewModel.onQueryChange("")
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("App 00").assertIsDisplayed()
    }
}
