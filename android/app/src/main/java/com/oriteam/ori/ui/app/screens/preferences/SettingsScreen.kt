package com.oriteam.ori.ui.app.screens.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.BuildConfig
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.list.OriMenuRow
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.navigation.rememberOpenSubscreen
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private enum class SettingsPane { ACCESSIBILITY, THEME, COMMUNICATION, SCREEN, PRIVACY, ABOUT }

/** "Configurações": a short menu, each row one small screen, so no option needs a long search. */
@Composable
fun SettingsRoute(isLocal: Boolean, onEraseLocalData: () -> Unit, onNavigateBack: () -> Unit) {
    val subscreen = rememberOpenSubscreen<SettingsPane>()
    val back = subscreen::close
    when (subscreen.current) {
        null -> SettingsMenu(onOpen = subscreen::open, onNavigateBack = onNavigateBack)
        SettingsPane.ACCESSIBILITY -> PreferenceSectionRoute(PreferenceSection.ACCESSIBILITY, onNavigateBack = back)
        SettingsPane.THEME -> PreferenceSectionRoute(PreferenceSection.THEME, onNavigateBack = back)
        SettingsPane.COMMUNICATION -> PreferenceSectionRoute(PreferenceSection.COMMUNICATION, onNavigateBack = back)
        SettingsPane.SCREEN -> PreferenceSectionRoute(PreferenceSection.SCREEN, onNavigateBack = back)
        SettingsPane.PRIVACY -> PrivacyScreen(isLocal = isLocal, onErase = onEraseLocalData, onNavigateBack = back)
        SettingsPane.ABOUT -> AboutScreen(onNavigateBack = back)
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
                        title = stringResource(R.string.profile_accessibility),
                        description = stringResource(R.string.settings_accessibility_description),
                        icon = OriIcons.Accessibility,
                        accentIndex = 0,
                        onClick = { onOpen(SettingsPane.ACCESSIBILITY) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.settings_theme),
                        description = stringResource(R.string.settings_theme_description),
                        icon = OriIcons.Palette,
                        accentIndex = 3,
                        onClick = { onOpen(SettingsPane.THEME) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.profile_communication),
                        description = stringResource(R.string.settings_communication_description),
                        icon = OriIcons.Chat,
                        accentIndex = 4,
                        onClick = { onOpen(SettingsPane.COMMUNICATION) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.settings_screen),
                        description = stringResource(R.string.settings_screen_description),
                        icon = OriIcons.Smartphone,
                        accentIndex = 2,
                        onClick = { onOpen(SettingsPane.SCREEN) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.settings_privacy),
                        description = stringResource(R.string.settings_privacy_description),
                        icon = Icons.Filled.Lock,
                        accentIndex = 1,
                        onClick = { onOpen(SettingsPane.PRIVACY) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.settings_about),
                        description = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                        icon = Icons.Filled.Info,
                        accentIndex = 5,
                        onClick = { onOpen(SettingsPane.ABOUT) }
                    )
                }
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
    }
}

/** Says in plain words where the data is. Erasing exists only for data that lives only here. */
@Composable
private fun PrivacyScreen(isLocal: Boolean, onErase: () -> Unit, onNavigateBack: () -> Unit) {
    var isConfirming by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(title = stringResource(R.string.settings_privacy), onNavigateBack = onNavigateBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
                OriCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.privacy_where_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(OriSpacing.Small))
                    Text(
                        text = stringResource(
                            if (isLocal) R.string.privacy_where_local else R.string.privacy_where_account
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(OriSpacing.Small))
                    Text(
                        text = stringResource(R.string.privacy_device_only),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!isLocal) {
                        Spacer(modifier = Modifier.height(OriSpacing.Small))
                        Text(
                            text = stringResource(R.string.privacy_caregivers),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (isLocal) {
                    Spacer(modifier = Modifier.height(OriSpacing.Medium))
                    OriTextButton(
                        text = stringResource(R.string.privacy_erase),
                        onClick = { isConfirming = true },
                        color = LocalOriColors.current.errorText
                    )
                }
            }
        }
    }
    if (isConfirming) {
        OriConfirmationDialog(
            title = stringResource(R.string.privacy_erase_title),
            message = stringResource(R.string.privacy_erase_message),
            confirmLabel = stringResource(R.string.aac_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = onErase,
            onDismiss = { isConfirming = false },
            isDestructive = true
        )
    }
}

@Composable
private fun AboutScreen(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(title = stringResource(R.string.settings_about), onNavigateBack = onNavigateBack)
        CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
            OriCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.about_description), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                Text(
                    text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
