package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.GridChoice
import com.oriteam.ori.domain.launcher.HomeArrangement
import com.oriteam.ori.domain.launcher.HomeScreenMode
import com.oriteam.ori.ui.components.form.OriChoiceChip
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.settings.SettingsSwitchItem
import com.oriteam.ori.ui.theme.OriSpacing

private val screenModeLabels = mapOf(
    HomeScreenMode.HOME_AND_APPS to R.string.launcher_screen_mode_home,
    HomeScreenMode.APPS_ONLY to R.string.launcher_screen_mode_apps_only
)

private val screenModeDescriptions = mapOf(
    HomeScreenMode.HOME_AND_APPS to R.string.launcher_screen_mode_home_description,
    HomeScreenMode.APPS_ONLY to R.string.launcher_screen_mode_apps_only_description
)

private val arrangementLabels = mapOf(
    HomeArrangement.PACKED to R.string.launcher_arrangement_packed,
    HomeArrangement.FREE to R.string.launcher_arrangement_free
)

private val arrangementDescriptions = mapOf(
    HomeArrangement.PACKED to R.string.launcher_arrangement_packed_description,
    HomeArrangement.FREE to R.string.launcher_arrangement_free_description
)

/**
 * "Opções avançadas": what changes how the home screen works, kept one step in so the first screen of the options
 * stays short. Whether there is a home screen at all or only all the apps, how it is organized (packed or free icons,
 * the grid of [gridChoices]), the gestures and what happens on their own (notifications by swiping down, the frame
 * where an app will go, new apps, turning or the recent apps) and the buttons.
 */
@Composable
internal fun AdvancedOptions(state: LauncherUiState, viewModel: LauncherViewModel, gridChoices: List<GridChoice>) {
    val settings = state.settings
    val editor = viewModel.appearance
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader(text = stringResource(R.string.launcher_organization))
        ChoiceTitle(R.string.launcher_screen_mode)
        Choices(HomeScreenMode.entries, settings.screenMode, screenModeLabels, viewModel.style::onScreenModeChange)
        OptionDescription(screenModeDescriptions.getValue(settings.screenMode))
        ChoiceTitle(R.string.launcher_arrangement)
        Choices(HomeArrangement.entries, settings.arrangement, arrangementLabels, editor::onArrangementChange)
        OptionDescription(arrangementDescriptions.getValue(settings.arrangement))
        ChoiceTitle(R.string.launcher_grid)
        GridChoices(gridChoices, settings.homeGrid, editor::onGridChange)
        val gridDescription = if (settings.homeGrid == null) {
            R.string.launcher_grid_auto_description
        } else {
            R.string.launcher_grid_fixed_description
        }
        OptionDescription(gridDescription)

        HomeBehaviorOptions(settings, viewModel.behavior)
        SettingsSwitchItem(
            title = stringResource(R.string.launcher_shows_buttons),
            description = stringResource(R.string.launcher_shows_buttons_description),
            checked = settings.showsButtons,
            onCheckedChange = editor::onShowsButtonsChange
        )
        Spacer(modifier = Modifier.height(OriSpacing.Large))
    }
}

/**
 * "Automática" and the grids this screen offers ([choices]); a grid chosen on another screen (a window that changed)
 * stays offered while it is the one in use.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GridChoices(choices: List<GridChoice>, selected: GridChoice?, onSelect: (GridChoice?) -> Unit) {
    val options = listOf(null) + (choices + listOfNotNull(selected)).distinct()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = Modifier.fillMaxWidth().selectableGroup()
    ) {
        options.forEach { option ->
            OriChoiceChip(
                label = if (option == null) {
                    stringResource(R.string.launcher_grid_auto)
                } else {
                    stringResource(R.string.launcher_grid_choice, option.columns, option.rows)
                },
                isSelected = option == selected,
                onClick = { onSelect(option) }
            )
        }
    }
}
