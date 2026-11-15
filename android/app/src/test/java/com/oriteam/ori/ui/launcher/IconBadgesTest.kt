package com.oriteam.ori.ui.launcher

import com.oriteam.ori.data.launcher.NotificationBadges
import com.oriteam.ori.domain.launcher.BadgeStyle
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.badgeCountOf
import com.oriteam.ori.domain.launcher.badgeKeyOf
import com.oriteam.ori.domain.launcher.countsOnBadge
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.FakeHomeSystem
import com.oriteam.ori.testing.FakeHomeWidgetHost
import com.oriteam.ori.testing.FakeInstalledApps
import com.oriteam.ori.testing.FakeLauncherSettingsRepository
import com.oriteam.ori.testing.FakeLockScreenWallpaper
import com.oriteam.ori.testing.FakeWallpaperStore
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.launcherApp
import com.oriteam.ori.testing.shortcutOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The badges on the icons, as One UI's: each app shows how many notifications it has (the profile counts apart, a
 * shortcut has none of its own), only notifications that alert count, and nothing is followed while they are off.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IconBadgesTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messages = launcherApp("Mensagens")
    private val system = FakeHomeSystem()
    private val settings = FakeLauncherSettingsRepository(LauncherSettings(isClockSetUp = true))

    private fun TestScope.viewModel() = LauncherViewModel(
        FakeInstalledApps(listOf(messages)),
        settings,
        FakeDevicePreferencesRepository(),
        FakeWallpaperStore(),
        FakeHomeWidgetHost(emptyList()),
        FakeLockScreenWallpaper(),
        system
    ).also { backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() } }

    @Test
    fun `an app shows the notifications of its own profile, and a shortcut none`() {
        val counts = mapOf(badgeKeyOf(messages.packageName, 0) to 3, badgeKeyOf(messages.packageName, 10) to 5)
        val work = messages.copy(key = "${messages.packageName}/.Main@10")

        assertEquals(3, badgeCountOf(messages, counts))
        assertEquals(5, badgeCountOf(work, counts))
        assertEquals(0, badgeCountOf(shortcutOf(messages, "Nova"), counts))
        assertEquals(0, badgeCountOf(LauncherApp("broken", "x", "X"), counts))
    }

    @Test
    fun `only a notification that alerts counts, not one that stays, a group summary or a channel without badges`() {
        assertTrue(countsOnBadge(isOngoing = false, isGroupSummary = false, canShowBadge = true))
        assertFalse(countsOnBadge(isOngoing = true, isGroupSummary = false, canShowBadge = true))
        assertFalse(countsOnBadge(isOngoing = false, isGroupSummary = true, canShowBadge = true))
        assertFalse(countsOnBadge(isOngoing = false, isGroupSummary = false, canShowBadge = false))
    }

    @Test
    fun `the counts add up per app, follow each notification, and empty when the access is taken back`() {
        val badges = NotificationBadges()

        badges.put("n1", "a@0", 1)
        badges.put("n2", "a@0", 2)
        badges.put("n3", "b@0", 1)
        assertEquals(mapOf("a@0" to 3, "b@0" to 1), badges.counts.value)

        badges.remove("n2")
        badges.put("n3", "b@0", 0)
        assertEquals(mapOf("a@0" to 1), badges.counts.value)

        badges.clear()
        assertTrue(badges.counts.value.isEmpty())
    }

    @Test
    fun `the badges come with the number from the start, as on One UI, and show once Android allows the counting`() =
        runTest {
            assertEquals(BadgeStyle.NUMBER, LauncherSettings().badges)
            system.notificationCounts.value = mapOf(badgeKeyOf(messages.packageName, 0) to 3)

            val viewModel = viewModel()

            assertEquals(3, badgeCountOf(messages, viewModel.uiState.value.badgeCounts))
        }

    @Test
    fun `off, no count reaches the screen, and a style chosen shows them as they change`() = runTest {
        settings.settings.value = settings.settings.value.copy(badges = BadgeStyle.OFF)
        system.notificationCounts.value = mapOf(badgeKeyOf(messages.packageName, 0) to 2)
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.badgeCounts.isEmpty())

        viewModel.behavior.onBadgesChange(BadgeStyle.NUMBER)
        assertEquals(BadgeStyle.NUMBER, viewModel.uiState.value.settings.badges)
        assertEquals(2, badgeCountOf(messages, viewModel.uiState.value.badgeCounts))

        system.notificationCounts.value = mapOf(badgeKeyOf(messages.packageName, 0) to 4)
        assertEquals(4, badgeCountOf(messages, viewModel.uiState.value.badgeCounts))
    }

    @Test
    fun `the options read whether the person allows the counting, and open Android's page to decide`() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.behavior.canCountNotifications())
        system.canCount = true
        assertTrue(viewModel.behavior.canCountNotifications())
        viewModel.behavior.onOpenNotificationAccess()

        assertEquals(1, system.accessPagesOpened)
    }
}
