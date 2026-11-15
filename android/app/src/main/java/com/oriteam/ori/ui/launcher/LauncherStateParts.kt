package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.CLOCK_KEY
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.domain.launcher.folderIdOf
import com.oriteam.ori.domain.launcher.widgetIdOf

/* The parts LauncherViewModel puts together into LauncherUiState. */

/** What the home screen shows besides the apps and the choices: kept apart, so each changes alone. */
internal data class HomeExtras(
    val wallpaper: WallpaperDraft,
    /** What the lock screen shows now, as far as ORI can tell (null: a picture of Android ORI cannot see). */
    val lockScreen: LauncherWallpaper?,
    val widgets: WidgetCatalog,
    val badgeCounts: Map<String, Int>,
    val recentKeys: List<String>
)

/** What the person is doing on the home screen now: kept only while ORI Home runs. */
internal data class Interaction(
    val pane: LauncherPane = LauncherPane.HOME,
    val query: String = "",
    val isEditingHome: Boolean = false,
    /** The pane open now was opened while editing: back returns to editing, as Android does. */
    val returnsToEditing: Boolean = false,
    val messageRes: Int? = null,
    /** A neutral word (the home screen is locked). */
    val infoRes: Int? = null,
    /** Goes up each time the Home button is pressed, so the pages go back to the first one. */
    val homeRequest: Int = 0,
    /** The folder open over the home screen; any new state without it closes it (Home, an app opened, back). */
    val openFolderId: Int? = null
)

/**
 * The apps still installed ([installed], by key), the widgets, the folders and the clock, in the order of the home
 * screen. A folder shows its apps still installed; one with none left is not shown.
 */
internal fun homeEntries(installed: Map<String, LauncherApp>, settings: LauncherSettings): List<HomeEntry> =
    settings.favorites.mapNotNull { key ->
        val widgetId = widgetIdOf(key)
        val folderId = folderIdOf(key)
        when {
            key == CLOCK_KEY -> HomeEntry.Clock
            widgetId != null -> HomeEntry.Widget(widgetId)
            folderId != null -> settings.folders[folderId]?.let { folder ->
                val apps = folder.apps.mapNotNull(installed::get)
                HomeEntry.Folder(folderId, folder.name, apps).takeIf { apps.isNotEmpty() }
            }
            else -> installed[key]?.let(HomeEntry::App)
        }
    }
