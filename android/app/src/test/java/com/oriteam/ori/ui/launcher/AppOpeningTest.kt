package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.LauncherIconSize
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.preferences.MotionLevel
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
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriAppearance
import com.oriteam.ori.ui.theme.OriMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An app opened from the home screen grows out of its icon, as on One UI: the tap says where the icon is on the screen.
 * With animations off (or reduced) nothing grows, so the app opens from nowhere.
 */
@RunWith(AndroidJUnit4::class)
class AppOpeningTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app = launcherApp("Câmera")
    private val installed = FakeInstalledApps(listOf(app))

    private fun render(motion: MotionLevel) {
        val viewModel = LauncherViewModel(
            installed,
            FakeLauncherSettingsRepository(LauncherSettings(favorites = listOf(app.key), isClockSetUp = true)),
            FakeDevicePreferencesRepository(),
            FakeWallpaperStore(),
            FakeHomeWidgetHost(),
            FakeLockScreenWallpaper(),
            FakeHomeSystem()
        )
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 800) {
                CompositionLocalProvider(LocalOriAppearance provides OriAppearance(motion = OriMotion(motion))) {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    LauncherScreen(
                        state,
                        viewModel,
                        LauncherActions(onOpenOri = {}, onLeaveHome = {}),
                        LauncherSlots(panel = { Text("Painel") }, widget = { _, modifier -> Box(modifier) })
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `an app opened from the home screen grows out of its icon`() {
        render(MotionLevel.EXPRESSIVE)

        composeRule.onNodeWithContentDescription("Câmera").performClick()
        composeRule.waitForIdle()

        val origin = installed.origins.single()
        val iconSide = with(composeRule.density) { LauncherIconSize.MEDIUM.iconDp.roundToPx() }
        assertEquals(iconSide, origin?.width)
        assertEquals(iconSide, origin?.height)
    }

    @Test
    fun `with animations off, an app opens without growing out of anything`() {
        render(MotionLevel.OFF)

        composeRule.onNodeWithContentDescription("Câmera").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(app), installed.opened)
        assertNull(installed.origins.single())
    }
}
