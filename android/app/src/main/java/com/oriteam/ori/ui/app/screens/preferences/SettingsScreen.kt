package com.oriteam.ori.ui.app.screens.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.BuildConfig
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.app.screens.legal.AboutScreen
import com.oriteam.ori.ui.app.screens.legal.PrivacyScreen
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.list.OriMenuRow
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.navigation.SubscreenHost
import com.oriteam.ori.ui.components.navigation.rememberOpenSubscreen
import com.oriteam.ori.ui.launcher.HomeModeScreen
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private enum class SettingsPane { ACCESSIBILITY, THEME, COMMUNICATION, SCREEN, HOME_SCREEN, PRIVACY, ABOUT }

/** "Configurações": a short menu, each row one small screen, so no option needs a long search. */
@Composable
fun SettingsRoute(ownerUid: String, isLocal: Boolean, onEraseLocalData: () -> Unit, onNavigateBack: () -> Unit) {
    val subscreen = rememberOpenSubscreen<SettingsPane>()
    val back = subscreen::close
    SubscreenHost(subscreen) { current ->
        when (current) {
            null -> SettingsMenu(onOpen = subscreen::open, onNavigateBack = onNavigateBack)
            SettingsPane.ACCESSIBILITY -> PreferenceSectionRoute(PreferenceSection.ACCESSIBILITY, onNavigateBack = back)
            SettingsPane.THEME -> PreferenceSectionRoute(PreferenceSection.THEME, onNavigateBack = back)
            SettingsPane.COMMUNICATION -> PreferenceSectionRoute(PreferenceSection.COMMUNICATION, onNavigateBack = back)
            SettingsPane.SCREEN -> PreferenceSectionRoute(PreferenceSection.SCREEN, onNavigateBack = back)
            SettingsPane.HOME_SCREEN -> HomeModeScreen(onNavigateBack = back)
            SettingsPane.PRIVACY -> PrivacyScreen(ownerUid, isLocal, onEraseLocalData, onNavigateBack = back)
            SettingsPane.ABOUT -> AboutScreen(onNavigateBack = back)
        }
    }
}

@Composable
private fun SettingsMenu(onOpen: (SettingsPane) -> Unit, onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(title = stringResource(R.string.settings_title), onNavigateBack = onNavigateBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                Column(verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)) {
                    OriMenuRow(
                        title = stringResource(R.string.settings_accessibility_title),
                        description = stringResource(R.string.settings_accessibility_description),
                        icon = OriIcons.Accessibility,
                        accent = LocalOriColors.current.settings.accessibility,
                        onClick = { onOpen(SettingsPane.ACCESSIBILITY) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.settings_theme),
                        description = stringResource(R.string.settings_theme_description),
                        icon = OriIcons.Palette,
                        accent = LocalOriColors.current.settings.theme,
                        onClick = { onOpen(SettingsPane.THEME) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.settings_communication_title),
                        description = stringResource(R.string.settings_communication_description),
                        icon = OriIcons.Chat,
                        accent = LocalOriColors.current.settings.communication,
                        onClick = { onOpen(SettingsPane.COMMUNICATION) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.settings_screen),
                        description = stringResource(R.string.settings_screen_description),
                        icon = OriIcons.Smartphone,
                        accent = LocalOriColors.current.settings.screen,
                        onClick = { onOpen(SettingsPane.SCREEN) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.home_mode_title),
                        description = stringResource(R.string.home_mode_menu_description),
                        icon = Icons.Filled.Home,
                        accent = LocalOriColors.current.settings.screen,
                        onClick = { onOpen(SettingsPane.HOME_SCREEN) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.settings_privacy),
                        description = stringResource(R.string.settings_privacy_description),
                        icon = Icons.Filled.Lock,
                        accent = LocalOriColors.current.settings.privacy,
                        onClick = { onOpen(SettingsPane.PRIVACY) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.settings_about),
                        description = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                        icon = Icons.Filled.Info,
                        accent = LocalOriColors.current.settings.about,
                        onClick = { onOpen(SettingsPane.ABOUT) }
                    )
                }
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
    }
}
