package com.oriteam.ori.domain.launcher

/*
 * Folders of the home screen, as on One UI: an app dropped on another makes a folder in its place; an app dropped on
 * a folder joins it; a folder left with one app becomes that app again. A folder takes one cell, as an app does, and
 * its apps take none. Folders live on the pages (not in the dock), and nothing changes on a locked home screen.
 */

private const val FOLDER_PREFIX = "folder:"

/** The name of a folder is the label under its icon: a longer one is cut. */
const val MAX_FOLDER_NAME = 40

private val NameBreaks = Regex("[\\t\\r\\n]+")

/**
 * A folder of the home screen: its name and its apps, in order. A folder made by a drop has no name ("") until the
 * person gives one, as on One UI; the screen shows "Pasta" in the language of the app meanwhile.
 */
data class HomeFolder(val name: String, val apps: List<String>)

/** Folders share the order of the home screen with the apps, under a key of their own. */
fun folderKey(id: Int): String = "$FOLDER_PREFIX$id"

fun folderIdOf(key: String): Int? = if (key.startsWith(
        FOLDER_PREFIX
    )
) {
    key.removePrefix(FOLDER_PREFIX).toIntOrNull()
} else {
    null
}

/** An app (not the clock, a widget or a folder): what a folder and the dock take. */
fun isAppKey(key: String): Boolean = key != CLOCK_KEY && widgetIdOf(key) == null && folderIdOf(key) == null

/** The folder [key] is in, if any. */
fun LauncherSettings.folderOf(key: String): Int? = folders.entries.firstOrNull { key in it.value.apps }?.key

/** Every app in a folder: they are on the home screen too, inside their folder. */
val LauncherSettings.appsInFolders: Set<String> get() = folders.values.flatMapTo(mutableSetOf()) { it.apps }

fun cleanFolderName(name: String): String = name.replace(NameBreaks, " ").trim().take(MAX_FOLDER_NAME)
