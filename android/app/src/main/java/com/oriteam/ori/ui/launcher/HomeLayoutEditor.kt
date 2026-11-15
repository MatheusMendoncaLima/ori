package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.CLOCK_KEY
import com.oriteam.ori.domain.launcher.HomeCell
import com.oriteam.ori.domain.launcher.HomeGrid
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.PlacedHomeItem
import com.oriteam.ori.domain.launcher.dropped
import com.oriteam.ori.domain.launcher.droppedOnDock
import com.oriteam.ori.domain.launcher.removedFromHome
import com.oriteam.ori.domain.launcher.stepped
import com.oriteam.ori.domain.launcher.widgetIdOf
import com.oriteam.ori.domain.launcher.withAddedToHome
import com.oriteam.ori.domain.launcher.withMainPage
import com.oriteam.ori.domain.launcher.withPageAdded
import com.oriteam.ori.domain.launcher.withPageMoved
import com.oriteam.ori.domain.launcher.withoutPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Where things are on the home screen: an item dropped on a cell or on the dock, taken out (on "Remover" or from its
 * menu), moved by the screen reader, and the clock widget put back. The rules are in the domain (HomeEditing.kt);
 * this only applies them to the stored choices.
 */
class HomeLayoutEditor internal constructor(
    private val settings: LauncherSettingsRepository,
    private val scope: CoroutineScope,
    private val widgets: HomeWidgetEditor,
    /** The keys shown on the pages now (installed apps and widgets), in order. */
    private val shownKeys: () -> List<String>,
    /** The home screen is locked now ([com.oriteam.ori.domain.launcher.LauncherSettings.isLayoutLocked]). */
    private val isLocked: () -> Boolean,
    /** Says that the home screen is locked, when a change was stopped by it. */
    private val onLocked: () -> Unit
) {
    /** An item dropped on a cell of the home screen laid out on [grid]. */
    fun onDropItem(key: String, target: HomeCell, grid: HomeGrid) {
        val keys = shownKeys()
        update { it.dropped(keys, key, target, grid) }
    }

    /** An app dropped on the dock, before the app at [index]. */
    fun onDropOnDock(key: String, index: Int, capacity: Int) = update { it.droppedOnDock(key, index, capacity) }

    /** "Move before/after" of the screen reader. */
    fun onStepItem(key: String, offset: Int, grid: HomeGrid) {
        val keys = shownKeys()
        update { it.stepped(keys, key, offset, grid) }
    }

    /** Off the home screen; a widget also gives its id back to Android. The app stays in all the apps. */
    fun onRemove(key: String) {
        if (isLocked()) return onLocked()
        val widgetId = widgetIdOf(key)
        if (widgetId != null) widgets.onRemove(widgetId) else update { it.removedFromHome(key) }
    }

    /** An item held and moved on a locked home screen: it stays, and the person is told why. */
    fun onMoveWhileLocked() = onLocked()

    /** "+" while editing: an empty page at the end. */
    fun onAddPage(placed: List<PlacedHomeItem>) = update { it.withPageAdded(placed) }

    /** A page changes places with the one before ([step] -1) or after (1), with its items (free icons only). */
    fun onMovePage(page: Int, step: Int, placed: List<PlacedHomeItem>, grid: HomeGrid) =
        update { it.withPageMoved(page, step, placed, grid.orientation) }

    /** The page the Home button goes back to. */
    fun onMainPage(page: Int, placed: List<PlacedHomeItem>) = update { it.withMainPage(page, placed) }

    /** A page taken away (as shown on [grid]), with its items; the widgets on it give their ids back to Android. */
    fun onRemovePage(page: Int, placed: List<PlacedHomeItem>, grid: HomeGrid) {
        scope.launch {
            var widgetIds = emptyList<Int>()
            settings.update { current ->
                val removal = current.withoutPage(page, placed, grid.orientation)
                widgetIds = removal.widgetIds
                removal.settings
            }
            widgets.release(widgetIds)
        }
    }

    /** The clock widget of ORI back on the home screen, at the end (from the list of widgets). */
    fun onAddClock() = update { it.withAddedToHome(CLOCK_KEY) }

    private fun update(transform: (LauncherSettings) -> LauncherSettings) {
        scope.launch { settings.update(transform) }
    }
}
