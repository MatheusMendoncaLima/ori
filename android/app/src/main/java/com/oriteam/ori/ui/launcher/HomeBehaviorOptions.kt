package com.oriteam.ori.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.settings.SettingsSwitchItem

/**
 * How the home screen behaves, as in the settings of One UI's home screen (in "Opções avançadas"): the swipe down for
 * the notifications, the frame where a carried app will go, new apps on the home screen, and on a phone whether the
 * home screen turns with it (on a tablet, the recent apps in the dock). The badges are with the icons, on the first
 * screen of the options.
 */
@Composable
internal fun HomeBehaviorOptions(settings: LauncherSettings, editor: HomeBehaviorEditor) {
    SectionHeader(text = stringResource(R.string.launcher_behavior))
    SettingsSwitchItem(
        title = stringResource(R.string.launcher_swipe_down),
        description = stringResource(R.string.launcher_swipe_down_description),
        checked = settings.swipesDownForNotifications,
        onCheckedChange = editor::onSwipeDownChange
    )
    SettingsSwitchItem(
        title = stringResource(R.string.launcher_drop_preview),
        description = stringResource(R.string.launcher_drop_preview_description),
        checked = settings.showsDropPreview,
        onCheckedChange = editor::onDropPreviewChange
    )
    SettingsSwitchItem(
        title = stringResource(R.string.launcher_adds_new_apps),
        description = stringResource(R.string.launcher_adds_new_apps_description),
        checked = settings.addsNewApps,
        onCheckedChange = editor::onAddsNewAppsChange
    )
    // A tablet always turns: the choice is only a phone's. The recent apps in the dock are only a tablet's, as on
    // One UI: a phone's dock has no room for them.
    if (isPhone(LocalConfiguration.current.smallestScreenWidthDp)) {
        SettingsSwitchItem(
            title = stringResource(R.string.launcher_turns_on_phone),
            description = stringResource(R.string.launcher_turns_on_phone_description),
            checked = settings.turnsOnPhone,
            onCheckedChange = editor::onTurnsOnPhoneChange
        )
    } else {
        // They are part of the dock: without it ("Mostrar o dock" off) there is nowhere to show them.
        SettingsSwitchItem(
            title = stringResource(R.string.launcher_recent_apps),
            description = stringResource(R.string.launcher_recent_apps_description),
            checked = settings.showsRecentApps,
            onCheckedChange = editor::onRecentAppsChange,
            enabled = settings.showsDock
        )
    }
}
