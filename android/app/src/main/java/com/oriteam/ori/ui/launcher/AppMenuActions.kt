package com.oriteam.ori.ui.launcher

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.InstalledAppsRepository
import com.oriteam.ori.domain.launcher.LauncherApp

/**
 * What the menu of an app asks Android, on the home screen and in all the apps alike: the app's page, or to uninstall
 * it (Android asks the person). When Android cannot (the app is gone), [onUnavailable] says so.
 */
class AppMenuActions internal constructor(
    private val apps: InstalledAppsRepository,
    private val onUnavailable: () -> Unit
) {
    /** The shortcuts of the app for its menu (none for a shortcut, or when Android does not give them). */
    suspend fun shortcutsOf(app: LauncherApp): List<LauncherApp> = apps.shortcutsOf(app)

    /** Android's page about the app, where it can also be uninstalled. */
    fun onOpenDetails(app: LauncherApp) {
        if (!apps.openDetails(app)) onUnavailable()
    }

    /** "Desinstalar": Android asks the person, and the app leaves the lists if they agree. */
    fun onUninstall(app: LauncherApp) {
        if (!apps.uninstall(app)) onUnavailable()
    }

    /**
     * The menu of an app, as on One UI: "Informações do app" first, then what the place offers ([middle]: put it on
     * the home screen or take it off, edit), and "Desinstalar" last, for an app that can be removed: what takes
     * something away is never the first thing under the finger.
     */
    internal fun entriesFor(app: LauncherApp, middle: List<MenuEntry> = emptyList()): List<MenuEntry> =
        listOf(MenuEntry(R.string.launcher_menu_app_info, Icons.Filled.Info) { onOpenDetails(app) }) +
            middle +
            listOfNotNull(
                MenuEntry(R.string.launcher_menu_uninstall, Icons.Filled.Delete) { onUninstall(app) }
                    .takeIf { app.isRemovable }
            )
}
