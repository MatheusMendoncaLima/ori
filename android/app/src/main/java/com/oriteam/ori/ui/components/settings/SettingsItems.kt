package com.oriteam.ori.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * On/off preference. The whole row is the touch target, not only the small switch,
 * which helps users with motor difficulties.
 */
@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LocalOriAppearance.current.metrics.listItemMinHeight)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            // No side padding: the texts and the switch line up with the rest of the content.
            .padding(vertical = OriSpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingTexts(title = title, description = description, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(OriSpacing.Medium))
        Switch(
            checked = checked,
            // The row handles the click; a second handler would announce two controls.
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.surface,
                uncheckedTrackColor = LocalOriColors.current.switchTrackInactive,
                uncheckedBorderColor = LocalOriColors.current.switchTrackInactive
            )
        )
    }
}

/** One option of a [SettingsChoiceGroup]. */
@Immutable
data class SettingsChoice<T>(
    val value: T,
    val label: String,
    val description: String? = null
)

/** Single choice among a few options (for example the animation level), shown as radio buttons. */
@Composable
fun <T> SettingsChoiceGroup(
    title: String,
    choices: List<SettingsChoice<T>>,
    selectedValue: T,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(text = title)
        Column(modifier = Modifier.selectableGroup()) {
            choices.forEach { choice ->
                val isSelected = choice.value == selectedValue
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = LocalOriAppearance.current.metrics.listItemMinHeight)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onValueSelected(choice.value) }
                        )
                        .padding(vertical = OriSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(OriSpacing.Medium))
                    SettingTexts(title = choice.label, description = choice.description, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SettingTexts(title: String, description: String?, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
