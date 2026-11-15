package com.oriteam.ori.domain.launcher

/*
 * Shortcuts of apps, as on One UI: holding an app shows its shortcuts ("Nova mensagem", "Escanear") at the top of its
 * menu; a tap opens one, and one can go to the home screen, where it is an icon like an app's. An app can also ask to
 * put one there (Android's "Adicionar à tela inicial"), and ORI Home asks the person first.
 *
 * A shortcut is a [LauncherApp] whose key starts with [SHORTCUT_PREFIX]: it goes on the pages, in the dock and in
 * folders as an app does, and opens through the same calls. Only the home app in use may read shortcuts, so without
 * that they are simply not shown. What a shortcut is called and its picture are read from Android each time; the home
 * screen keeps only its key.
 */

const val SHORTCUT_PREFIX = "shortcut:"

/** A menu shows at most this many shortcuts of an app, as Android's own home screen does. */
const val MAX_MENU_SHORTCUTS = 4

fun isShortcutKey(key: String): Boolean = key.startsWith(SHORTCUT_PREFIX)

/** The shortcuts on the home screen (the pages, the dock, the folders): Android keeps them while they are there. */
val LauncherSettings.shortcutKeys: Set<String>
    get() = (favorites + dock + appsInFolders).filterTo(mutableSetOf(), ::isShortcutKey)
