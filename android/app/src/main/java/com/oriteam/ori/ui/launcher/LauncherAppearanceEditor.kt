package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.GridChoice
import com.oriteam.ori.domain.launcher.HomeArrangement
import com.oriteam.ori.domain.launcher.LauncherIconSize
import com.oriteam.ori.domain.launcher.LauncherPanel
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.StimulusChoices
import com.oriteam.ori.domain.launcher.StimulusLevel
import com.oriteam.ori.domain.launcher.withArrangement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The choices of how ORI Home looks and is laid out (the level of stimulus, buttons, icon size, packed or free icons,
 * the grid, the panel on the left), apart from the [LauncherViewModel], which runs the screen.
 */
class LauncherAppearanceEditor internal constructor(
    private val settings: LauncherSettingsRepository,
    private val scope: CoroutineScope
) {
    /** A level of stimulus: every choice of it at once, as a preset of a game's graphics. */
    fun onStimulusLevelChange(level: StimulusLevel) = update { it.copy(stimulus = level.choices) }

    /** One choice of stimulus changed by hand: the level is then the one these choices make, or "Personalizado". */
    fun onStimulusChange(transform: (StimulusChoices) -> StimulusChoices) =
        update { it.copy(stimulus = transform(it.stimulus)) }

    fun onShowsButtonsChange(value: Boolean) = update { it.copy(showsButtons = value) }

    /** The names under the icons of the home screen. */
    fun onShowsLabelsChange(value: Boolean) = update { it.copy(showsLabels = value) }

    /** "Mostrar o dock": the row of apps at the bottom; its apps are kept while it is off. */
    fun onShowsDockChange(value: Boolean) = update { it.copy(showsDock = value) }

    fun onIconSizeChange(size: LauncherIconSize) = update { it.copy(iconSize = size) }

    /** Packed (with the clock first) or free, whose cells are kept: going back to it finds everything in place. */
    fun onArrangementChange(arrangement: HomeArrangement) = update { it.withArrangement(arrangement) }

    /** "Grade da tela inicial": a grid of its own (the icons fit its cells), or null to follow the icon size. */
    fun onGridChange(grid: GridChoice?) = update { it.copy(homeGrid = grid) }

    /** "Travar a tela inicial": nothing on the home screen moves or leaves it until it is unlocked. */
    fun onLayoutLockedChange(value: Boolean) = update { it.copy(isLayoutLocked = value) }

    fun onPanelChange(panel: LauncherPanel) = update { it.copy(panel = panel) }

    private fun update(transform: (LauncherSettings) -> LauncherSettings) {
        scope.launch { settings.update(transform) }
    }
}
