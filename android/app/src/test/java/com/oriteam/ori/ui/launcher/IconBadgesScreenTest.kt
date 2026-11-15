package com.oriteam.ori.ui.launcher

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.BadgeStyle
import com.oriteam.ori.domain.launcher.HomeFolder
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.badgeKeyOf
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
import com.oriteam.ori.ui.components.text.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The badges as the screen reader says them: an app with notifications says how many, a folder the sum of its apps,
 * and with the badges off nothing is added to the names.
 */
@RunWith(AndroidJUnit4::class)
class IconBadgesScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val apps = List(4) { launcherApp("App $it") }
    private val system = FakeHomeSystem().apply {
        notificationCounts.value = mapOf(
            badgeKeyOf(apps[0].packageName, 0) to 3,
            badgeKeyOf(apps[1].packageName, 0) to 1,
            badgeKeyOf(apps[2].packageName, 0) to 2
        )
    }
    private val settings = FakeLauncherSettingsRepository(
        LauncherSettings(
            favorites = listOf(apps[0].key, folderKey(1), apps[3].key),
            folders = mapOf(1 to HomeFolder("Jogos", listOf(apps[1].key, apps[2].key))),
            isClockSetUp = true
        )
    )

    private fun render(style: BadgeStyle) {
        settings.settings.value = settings.settings.value.copy(badges = style)
        val viewModel = LauncherViewModel(
            FakeInstalledApps(apps),
            settings,
            FakeDevicePreferencesRepository(),
            FakeWallpaperStore(),
            FakeHomeWidgetHost(),
            FakeLockScreenWallpaper(),
            system
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

    @Test
    fun `an app says how many notifications it has, and a folder the sum of its apps`() {
        render(BadgeStyle.NUMBER)

        composeRule.onNodeWithContentDescription("App 0, 3 notificações").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Jogos, pasta com 2 apps, 3 notificações").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("App 3").assertIsDisplayed()
    }

    @Test
    fun `a dot says the number too, and with the badges off the names are only the names`() {
        render(BadgeStyle.DOT)
        composeRule.onNodeWithContentDescription("App 0, 3 notificações").assertIsDisplayed()
    }

    @Test
    fun `with the badges off the names are only the names`() {
        render(BadgeStyle.OFF)
        composeRule.onNodeWithContentDescription("App 0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Jogos, pasta com 2 apps").assertIsDisplayed()
    }
}
