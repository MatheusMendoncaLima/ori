package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.GridChoice
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.list.OriMenuRow
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.navigation.SubscreenHost
import com.oriteam.ori.ui.components.navigation.rememberOpenSubscreen
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriAreaAccents
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.areaAccent

/** The screen opened inside the options. */
private enum class OptionsSubscreen { ADVANCED }

/**
 * The options of ORI Home, the everyday ones first: the level of stimulus, how big the icons are and their names, the
 * badges, locking the home screen, the panel, the wallpaper, the widgets and the hidden apps. What changes how the
 * home screen works (the grid, packed or free icons, the gestures, the buttons) is in "Opções avançadas", one step
 * in, so the first screen stays short. Leaving is always here and goes back to the launcher used before: ORI Home
 * never keeps anyone in.
 */
@Composable
fun LauncherOptions(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    actions: LauncherActions,
    gridChoices: List<GridChoice> = emptyList()
) {
    val subscreen = rememberOpenSubscreen<OptionsSubscreen>()
    SubscreenHost(subscreen) { shown ->
        when (shown) {
            null -> MainOptions(state, viewModel, actions) { subscreen.open(OptionsSubscreen.ADVANCED) }
            OptionsSubscreen.ADVANCED -> LauncherPaneScaffold(
                title = stringResource(R.string.launcher_advanced_options),
                onBack = subscreen::close
            ) { AdvancedOptions(state, viewModel, gridChoices) }
        }
    }
}

@Composable
private fun MainOptions(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    actions: LauncherActions,
    onAdvanced: () -> Unit
) {
    LauncherPaneScaffold(title = stringResource(R.string.launcher_options), onBack = viewModel::onBack) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            StimulusOptions(state.settings.stimulus, viewModel.appearance)
            LauncherLayoutOptions(state, viewModel)
            HiddenAppsOption(state, viewModel.behavior)
            OriMenuRow(
                title = stringResource(R.string.launcher_advanced_options),
                description = stringResource(R.string.launcher_advanced_options_description),
                icon = Icons.Filled.Settings,
                accent = areaAccent(OriAreaAccents.TASKS),
                onClick = onAdvanced
            )
            SectionHeader(text = stringResource(R.string.launcher_options_ori))
            OriMenuRow(
                title = stringResource(R.string.launcher_open_ori),
                icon = Icons.Filled.Home,
                accent = areaAccent(OriAreaAccents.ROUTINE),
                onClick = { actions.onOpenOri(null) }
            )
            Spacer(modifier = Modifier.height(OriSpacing.Large))
            Text(
                text = stringResource(R.string.launcher_leave_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            OriTextButton(text = stringResource(R.string.launcher_leave), onClick = actions.onLeaveHome)
            Spacer(modifier = Modifier.height(OriSpacing.Large))
        }
    }
}
