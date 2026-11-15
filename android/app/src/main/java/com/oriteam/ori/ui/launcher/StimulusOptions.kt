package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.StimulusChoices
import com.oriteam.ori.domain.launcher.StimulusLevel
import com.oriteam.ori.domain.launcher.StimulusMotion
import com.oriteam.ori.domain.launcher.level
import com.oriteam.ori.ui.components.form.OriChoiceChip
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.settings.SettingsSwitchItem
import com.oriteam.ori.ui.theme.OriSpacing

private val levelLabels = mapOf(
    StimulusLevel.STANDARD to R.string.launcher_stimulus_standard,
    StimulusLevel.MEDIUM to R.string.launcher_stimulus_medium,
    StimulusLevel.LOW to R.string.launcher_stimulus_low,
    StimulusLevel.NONE to R.string.launcher_stimulus_none
)

private val levelDescriptions = mapOf(
    StimulusLevel.STANDARD to R.string.launcher_stimulus_standard_description,
    StimulusLevel.MEDIUM to R.string.launcher_stimulus_medium_description,
    StimulusLevel.LOW to R.string.launcher_stimulus_low_description,
    StimulusLevel.NONE to R.string.launcher_stimulus_none_description
)

private val motionLabels = mapOf(
    StimulusMotion.NORMAL to R.string.launcher_stimulus_motion_normal,
    StimulusMotion.REDUCED to R.string.launcher_stimulus_motion_reduced,
    StimulusMotion.OFF to R.string.launcher_stimulus_motion_off
)

/**
 * "Nível de estímulo", as the presets of a game's graphics: a level sets every choice at once ("Padrão", "Médio",
 * "Baixo", "Nenhum"), and each choice below can still be changed by hand, which makes the level "Personalizado".
 */
@Composable
internal fun StimulusOptions(stimulus: StimulusChoices, editor: LauncherAppearanceEditor) {
    val level = stimulus.level
    SectionHeader(text = stringResource(R.string.launcher_stimulus))
    OptionDescription(R.string.launcher_stimulus_description)
    LevelChoices(level, editor::onStimulusLevelChange)
    OptionDescription(level?.let(levelDescriptions::getValue) ?: R.string.launcher_stimulus_custom_description)

    ChoiceTitle(R.string.launcher_stimulus_motion)
    Choices(StimulusMotion.entries, stimulus.motion, motionLabels) { motion ->
        editor.onStimulusChange { it.copy(motion = motion) }
    }
    SettingsSwitchItem(
        title = stringResource(R.string.launcher_stimulus_wallpaper),
        description = stringResource(R.string.launcher_stimulus_wallpaper_description),
        checked = stimulus.showsWallpaper,
        onCheckedChange = { value -> editor.onStimulusChange { it.copy(showsWallpaper = value) } }
    )
    SettingsSwitchItem(
        title = stringResource(R.string.launcher_stimulus_colors),
        description = stringResource(R.string.launcher_stimulus_colors_description),
        checked = stimulus.colorfulIcons,
        onCheckedChange = { value -> editor.onStimulusChange { it.copy(colorfulIcons = value) } }
    )
    SettingsSwitchItem(
        title = stringResource(R.string.launcher_stimulus_simplified),
        description = stringResource(R.string.launcher_stimulus_simplified_description),
        checked = stimulus.isSimplified,
        onCheckedChange = { value -> editor.onStimulusChange { it.copy(isSimplified = value) } }
    )
}

/** The levels; "Personalizado" shows chosen, after them, only while the choices are no level's. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LevelChoices(level: StimulusLevel?, onSelect: (StimulusLevel) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = Modifier.fillMaxWidth().selectableGroup()
    ) {
        StimulusLevel.entries.forEach { option ->
            OriChoiceChip(
                label = stringResource(levelLabels.getValue(option)),
                isSelected = option == level,
                onClick = { onSelect(option) }
            )
        }
        if (level == null) {
            OriChoiceChip(label = stringResource(R.string.launcher_stimulus_custom), isSelected = true, onClick = {})
        }
    }
}
