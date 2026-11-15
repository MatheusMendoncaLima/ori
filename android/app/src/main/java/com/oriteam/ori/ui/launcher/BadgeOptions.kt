package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.BadgeStyle
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.settings.SettingsChoice
import com.oriteam.ori.ui.components.settings.SettingsChoiceGroup
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

private val badgeLabels = mapOf(
    BadgeStyle.OFF to R.string.launcher_badges_off,
    BadgeStyle.DOT to R.string.launcher_badges_dot,
    BadgeStyle.NUMBER to R.string.launcher_badges_number
)

/**
 * "Selos dos ícones", as One UI's "Selos dos ícones de apps": off, a dot or the number. With a style chosen, it says
 * whether Android lets ORI Home count the notifications and opens Android's page to allow it (or take it back): the
 * person decides there, ORI never turns it on. Read again each time the options come back into view.
 */
@Composable
internal fun BadgeOptions(settings: LauncherSettings, editor: HomeBehaviorEditor) {
    var canCount by remember { mutableStateOf(editor.canCountNotifications()) }
    LifecycleResumeEffect(editor) {
        canCount = editor.canCountNotifications()
        onPauseOrDispose { }
    }
    SettingsChoiceGroup(
        title = stringResource(R.string.launcher_badges),
        choices = BadgeStyle.entries.map { SettingsChoice(it, stringResource(badgeLabels.getValue(it))) },
        selectedValue = settings.badges,
        onValueSelected = editor::onBadgesChange
    )
    Column(modifier = Modifier.padding(bottom = OriSpacing.Medium)) {
        Text(
            text = stringResource(R.string.launcher_badges_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (settings.badges != BadgeStyle.OFF) {
            val access = if (canCount) R.string.launcher_badges_allowed else R.string.launcher_badges_access
            val button = if (canCount) R.string.launcher_badges_manage else R.string.launcher_badges_allow
            Text(
                text = stringResource(access),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = OriSpacing.Small)
            )
            OriSecondaryButton(
                text = stringResource(button),
                onClick = editor::onOpenNotificationAccess
            )
        }
    }
}
