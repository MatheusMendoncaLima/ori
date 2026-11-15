package com.oriteam.ori.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.oriteam.ori.domain.launcher.HomeScreenMode
import com.oriteam.ori.domain.launcher.badgesShown
import com.oriteam.ori.ui.components.motion.OriPaneTransition

/**
 * ORI Home. The home screen ([HomePane]) is always there, under everything: all the apps open over it with the
 * finger, and the options, the wallpaper and the widgets open over it as panes, with the person's motion level. So
 * coming back from a pane finds the home screen as it was left (the same page, nothing built again).
 *
 * Back closes what is open and stays on the same page; on the home screen itself it does nothing, as on any home
 * screen: the home screen is never closed, so it is never opened again either.
 */
@Composable
fun LauncherScreen(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    actions: LauncherActions,
    slots: LauncherSlots
) {
    BackHandler {
        if (state.pane != LauncherPane.HOME || state.isEditingHome) viewModel.onBack()
    }
    val style = state.settings.badgesShown
    val badges = remember(state.badgeCounts, style) { HomeBadges(state.badgeCounts, style) }
    val menus = remember { MenuLayer() }
    CompositionLocalProvider(
        LocalHomeBadges provides badges,
        LocalIconShape provides state.settings.iconShape,
        LocalMenuLayer provides menus
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LauncherPanes(state, viewModel, actions, slots)
            // Over everything, in this same window: the menu of an icon, of an app in all the apps, of a wallpaper.
            MenuLayerHost(menus)
        }
    }
}

/** The home screen and the pane over it (the options, the wallpaper, the widgets), or the wallpaper being tried. */
@Composable
private fun LauncherPanes(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    actions: LauncherActions,
    slots: LauncherSlots
) {
    val pane = if (state.pane == LauncherPane.DRAWER) LauncherPane.HOME else state.pane
    // While a wallpaper is tried, the real home screen shows it: the pane over it has only "Cancelar" and "Aplicar".
    val preview = state.wallpaperDraft.preview.takeIf { pane == LauncherPane.WALLPAPER }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val settings = state.settings
        val metrics = rememberHomeMetrics(
            maxWidth,
            maxHeight,
            settings.iconSize,
            settings.homeGrid,
            settings.showsLabels,
            hasDock = settings.hasDockRow
        )
        val home = if (preview == null) {
            state
        } else {
            // The wallpaper tried shows, also where the level of stimulus hides the wallpaper.
            val shown = state.settings.stimulus.copy(showsWallpaper = true)
            state.copy(settings = state.settings.copy(wallpaper = preview, stimulus = shown), isEditingHome = false)
        }
        // Under a pane the home screen is only a picture: the screen reader does not go through it.
        val isCovered = pane != LauncherPane.HOME
        Box(modifier = if (isCovered) Modifier.clearAndSetSemantics { } else Modifier) {
            if (settings.screenMode == HomeScreenMode.APPS_ONLY) {
                AppsOnlyPane(home, viewModel, metrics)
            } else {
                HomePane(home, viewModel, slots, metrics)
            }
        }
        OriPaneTransition(
            target = if (preview != null) PaneShown.WALLPAPER_PREVIEW else PaneShown.of(pane),
            isForward = { _, to -> to != PaneShown.HOME },
            modifier = Modifier.fillMaxSize(),
            label = "LauncherPane"
        ) { shown ->
            when (shown) {
                PaneShown.HOME -> Unit
                PaneShown.OPTIONS -> LauncherOptions(state, viewModel, actions, metrics.gridChoices)
                PaneShown.WALLPAPER -> WallpaperPicker(state, viewModel, metrics.grid)
                PaneShown.WALLPAPER_PREVIEW -> WallpaperPreviewControls(state, viewModel, metrics)
                PaneShown.WIDGETS -> WidgetPicker(state, viewModel, metrics.grid)
            }
        }
    }
}

/** What is over the home screen: nothing, or a pane (the wallpaper either chosen or tried on the home screen). */
private enum class PaneShown {
    HOME, OPTIONS, WALLPAPER, WALLPAPER_PREVIEW, WIDGETS;

    companion object {
        fun of(pane: LauncherPane): PaneShown = when (pane) {
            LauncherPane.HOME, LauncherPane.DRAWER -> HOME
            LauncherPane.OPTIONS -> OPTIONS
            LauncherPane.WALLPAPER -> WALLPAPER
            LauncherPane.WIDGETS -> WIDGETS
        }
    }
}

/**
 * The options, the wallpaper and the widgets are read on the plain theme background, whatever the wallpaper, with
 * the theme's writing color (a [Surface] gives both, and it takes the touches, so none reaches the home screen).
 */
@Composable
internal fun PlainPane(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) { content() }
    }
}
