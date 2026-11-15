package com.oriteam.ori.domain.launcher

/*
 * The recent apps in the dock of a tablet, as One UI shows them after the dock's own apps: the last apps opened from
 * ORI Home. ORI Home counts only what is opened from it (it reads no usage history of Android, which would need the
 * person to allow "Acesso ao uso"), and keeps the list only in memory: it is gone when ORI Home closes.
 */

/** The dock shows at most this many recent apps, as One UI's tablet does. */
const val MAX_RECENT_APPS = 2

/** The list of recent keys with [key] opened now: it goes first, once; only apps count, not their shortcuts. */
fun List<String>.withOpened(key: String): List<String> =
    if (isShortcutKey(key)) this else (listOf(key) + (this - key)).take(MAX_RECENT_APPS + MAX_DOCK_ITEMS)

/**
 * The recent apps the dock shows now: the ones of [recent] still installed ([installed], by key), not in the dock,
 * not hidden, at most [MAX_RECENT_APPS]; none while the person does not want them or the dock is not shown.
 */
fun recentApps(
    recent: List<String>,
    installed: Map<String, LauncherApp>,
    settings: LauncherSettings
): List<LauncherApp> =
    if (!settings.showsRecentApps || !settings.showsDock) {
        emptyList()
    } else {
        recent.filter { it !in settings.dock && it !in settings.hidden }
            .mapNotNull(installed::get)
            .take(MAX_RECENT_APPS)
    }
