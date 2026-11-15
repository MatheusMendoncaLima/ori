package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.DrawerStyle
import com.oriteam.ori.domain.launcher.HomeScreenMode
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.domain.launcher.LauncherIconSize
import com.oriteam.ori.domain.launcher.LauncherPanel
import com.oriteam.ori.ui.components.form.OriChoiceChip
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.list.OriMenuRow
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.settings.SettingsSwitchItem
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriAreaAccents
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.areaAccent

private val panelLabels = mapOf(
    LauncherPanel.ROUTINE to R.string.shortcut_routine,
    LauncherPanel.PHRASES to R.string.main_sentences,
    LauncherPanel.NONE to R.string.launcher_panel_none
)

private val iconSizeLabels = mapOf(
    LauncherIconSize.SMALL to R.string.launcher_icon_size_small,
    LauncherIconSize.MEDIUM to R.string.launcher_icon_size_medium,
    LauncherIconSize.LARGE to R.string.launcher_icon_size_large,
    LauncherIconSize.EXTRA_LARGE to R.string.launcher_icon_size_extra_large,
    LauncherIconSize.MAXIMUM to R.string.launcher_icon_size_maximum
)

private val iconShapeLabels = mapOf(
    IconShape.ROUND to R.string.launcher_icon_shape_round,
    IconShape.ROUNDED to R.string.launcher_icon_shape_rounded,
    IconShape.SQUARE to R.string.launcher_icon_shape_square
)

private val drawerStyleLabels = mapOf(
    DrawerStyle.SCROLL to R.string.launcher_drawer_style_scroll,
    DrawerStyle.PAGES to R.string.launcher_drawer_style_pages
)

private val drawerStyleDescriptions = mapOf(
    DrawerStyle.SCROLL to R.string.launcher_drawer_style_scroll_description,
    DrawerStyle.PAGES to R.string.launcher_drawer_style_pages_description
)

/**
 * The everyday options of how the home screen looks and behaves: the icons (their size and shape, their names, the
 * badges), the dock, how all the apps show, locking the home screen, the panel on the left, and the ways to the
 * wallpaper and the widgets. The rest is in "Opções avançadas" ([AdvancedOptions]). With only all the apps, what
 * belongs to the home screen (the dock, locking it, the panel, the widgets) is not offered.
 */
@Composable
internal fun LauncherLayoutOptions(state: LauncherUiState, viewModel: LauncherViewModel) {
    val settings = state.settings
    val editor = viewModel.appearance
    val hasHome = settings.screenMode == HomeScreenMode.HOME_AND_APPS
    SectionHeader(text = stringResource(R.string.launcher_appearance))
    ChoiceTitle(R.string.launcher_icon_size)
    Choices(LauncherIconSize.entries, settings.iconSize, iconSizeLabels, editor::onIconSizeChange)
    OptionDescription(R.string.launcher_icon_size_description)
    ChoiceTitle(R.string.launcher_icon_shape)
    Choices(IconShape.entries, settings.iconShape, iconShapeLabels, viewModel.style::onIconShapeChange)
    OptionDescription(R.string.launcher_icon_shape_description)
    SettingsSwitchItem(
        title = stringResource(R.string.launcher_shows_labels),
        description = stringResource(R.string.launcher_shows_labels_description),
        checked = settings.showsLabels,
        onCheckedChange = editor::onShowsLabelsChange
    )
    if (hasHome) {
        SettingsSwitchItem(
            title = stringResource(R.string.launcher_shows_dock),
            description = stringResource(R.string.launcher_shows_dock_description),
            checked = settings.showsDock,
            onCheckedChange = editor::onShowsDockChange
        )
    }
    BadgeOptions(settings, viewModel.behavior)

    SectionHeader(text = stringResource(R.string.launcher_all_apps))
    ChoiceTitle(R.string.launcher_drawer_style)
    Choices(DrawerStyle.entries, settings.drawerStyle, drawerStyleLabels, viewModel.style::onDrawerStyleChange)
    OptionDescription(drawerStyleDescriptions.getValue(settings.drawerStyle))

    if (hasHome) {
        // Nothing moves or leaves by a hold by mistake: an everyday choice for whoever has difficulty with the fingers.
        SettingsSwitchItem(
            title = stringResource(R.string.launcher_lock_layout),
            description = stringResource(R.string.launcher_lock_layout_description),
            checked = settings.isLayoutLocked,
            onCheckedChange = editor::onLayoutLockedChange
        )
        SectionHeader(text = stringResource(R.string.launcher_panel))
        OptionDescription(R.string.launcher_panel_description)
        Choices(LauncherPanel.entries, settings.panel, panelLabels, editor::onPanelChange)
    }
    OriMenuRow(
        title = stringResource(R.string.launcher_edit_wallpaper),
        icon = OriIcons.Palette,
        accent = areaAccent(OriAreaAccents.COMMUNICATION),
        onClick = { viewModel.onOpenPane(LauncherPane.WALLPAPER) }
    )
    if (hasHome) {
        OriMenuRow(
            title = stringResource(R.string.launcher_edit_widgets),
            icon = OriIcons.Widgets,
            accent = areaAccent(OriAreaAccents.ROUTINE),
            onClick = { viewModel.onOpenPane(LauncherPane.WIDGETS) }
        )
    }
}

/** One choice of [options] (the chosen one filled), wrapping to the next line when they do not fit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> Choices(options: List<T>, selected: T, labels: Map<T, Int>, onSelect: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = Modifier.fillMaxWidth().selectableGroup()
    ) {
        options.forEach { option ->
            OriChoiceChip(
                label = stringResource(labels.getValue(option)),
                isSelected = option == selected,
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
internal fun ChoiceTitle(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth().padding(top = OriSpacing.Medium, bottom = OriSpacing.Small)
    )
}

@Composable
internal fun OptionDescription(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = OriSpacing.ExtraSmall)
    )
}
