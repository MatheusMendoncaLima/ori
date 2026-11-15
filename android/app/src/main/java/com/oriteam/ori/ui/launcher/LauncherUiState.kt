package com.oriteam.ori.ui.launcher

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.oriteam.ori.domain.launcher.CLOCK_KEY
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.LauncherPanel
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.domain.launcher.appsInFolders
import com.oriteam.ori.domain.launcher.folderKey
import com.oriteam.ori.domain.launcher.widgetKey
import com.oriteam.ori.domain.preferences.StartArea

/** Which part of ORI Home is open. All the apps open over the home screen, following the finger. */
enum class LauncherPane { HOME, DRAWER, OPTIONS, WALLPAPER, WIDGETS }

/** What ORI Home asks its activity to do: open ORI in an area (or at its start), or leave ORI Home. */
data class LauncherActions(
    val onOpenOri: (StartArea?) -> Unit,
    val onLeaveHome: () -> Unit
)

/** Something on the home screen: an app, a widget of another app, or the clock widget of ORI. */
sealed interface HomeEntry {
    val key: String

    data class App(val app: LauncherApp) : HomeEntry {
        override val key: String get() = app.key
    }

    data class Widget(val widgetId: Int) : HomeEntry {
        override val key: String get() = widgetKey(widgetId)
    }

    /** A folder of the pages, with its apps still installed (a folder whose apps are all gone is not shown). */
    data class Folder(val id: Int, val name: String, val apps: List<LauncherApp>) : HomeEntry {
        override val key: String get() = folderKey(id)
    }

    data object Clock : HomeEntry {
        override val key: String get() = CLOCK_KEY
    }
}

data class LauncherUiState(
    val isLoading: Boolean = true,
    val pane: LauncherPane = LauncherPane.HOME,
    /** The apps and widgets of the pages, in the person's order. */
    val home: List<HomeEntry> = emptyList(),
    /** The apps of the dock, the row at the bottom of every page. */
    val dock: List<LauncherApp> = emptyList(),
    /** All the apps the search finds. */
    val drawer: List<LauncherApp> = emptyList(),
    /** Every app not hidden, in alphabetical order, whatever the search: what the list of a folder offers. */
    val apps: List<LauncherApp> = emptyList(),
    /** The hidden apps that are still installed, to show them again in the options. */
    val hiddenApps: List<LauncherApp> = emptyList(),
    val query: String = "",
    val isEditingHome: Boolean = false,
    val wallpaperDraft: WallpaperDraft = WallpaperDraft(),
    /**
     * What the lock screen shows now, as far as ORI can tell: the Android wallpaper ([LauncherWallpaper.System]), the
     * one ORI put there, or null (a picture of Android that ORI cannot see).
     */
    val lockScreenShown: LauncherWallpaper? = null,
    val widgetCatalog: WidgetCatalog = WidgetCatalog(),
    val settings: LauncherSettings = LauncherSettings(),
    @StringRes val messageRes: Int? = null,
    /** A neutral word, neither a failure nor a success: the home screen is locked. */
    @StringRes val infoRes: Int? = null,
    val homeRequest: Int = 0,
    /** The folder open over the home screen, or null. */
    val openFolderId: Int? = null,
    /** How many notifications each app shows now, by "package@profile"; empty while the badges are off. */
    val badgeCounts: Map<String, Int> = emptyMap(),
    /** The last apps opened from ORI Home that are not in the dock, while the person wants them there. */
    val recentApps: List<LauncherApp> = emptyList()
) {
    /**
     * What is on the home screen, on the pages, in the dock or in a folder: made once per state, read by every app
     * listed.
     */
    val favoriteKeys: Set<String> = settings.favorites.toSet() + settings.dock + settings.appsInFolders

    /** The folder open now, as shown on the pages; null when none is open or it is gone. */
    val openFolder: HomeEntry.Folder?
        get() = home.filterIsInstance<HomeEntry.Folder>().firstOrNull { it.id == openFolderId }

    val hasClock: Boolean get() = CLOCK_KEY in settings.favorites
}

/**
 * What the activity puts inside ORI Home: the panel on the left (the routine or the phrases of ORI, which need the
 * session) and the widgets (drawn by Android). Passed in, so the home screen does not depend on them and tests can
 * give simple stand-ins.
 */
class LauncherSlots(
    val panel: @Composable (LauncherPanel) -> Unit,
    val widget: @Composable (widgetId: Int, modifier: Modifier) -> Unit,
    /**
     * A widget held by the home screen: its own touch ends (no pressed shade, nothing opens when let go, as on any
     * home screen of Android), and its picture as it looks, for the copy that follows the finger, as One UI carries
     * it. Null when it cannot be drawn (the copy is then a plain card).
     */
    val liftWidget: (widgetId: Int) -> ImageBitmap? = { null }
)

/** One choice of a menu of an app or a widget, with its icon (only a picture: the label says what it does). */
internal class MenuEntry(@StringRes val label: Int, val icon: ImageVector, val onClick: () -> Unit)

/** What the clock can open, also as actions of the screen reader (the first thing it reads on the home screen). */
internal data class ClockActions(
    val onAllApps: () -> Unit,
    val onCustomize: () -> Unit,
    val onEdit: () -> Unit,
    /** Null when there is no panel on the left. */
    val onPanel: (() -> Unit)?
)
