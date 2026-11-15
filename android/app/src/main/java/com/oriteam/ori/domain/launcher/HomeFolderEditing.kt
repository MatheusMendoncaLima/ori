package com.oriteam.ori.domain.launcher

/*
 * Changing the folders of the home screen (HomeFolders.kt has what they are): making one, adding to it, taking out of
 * it, renaming it and tidying it when it is closed. Every change keeps the cells of the pages (upright and turned).
 */

/**
 * [added] dropped on [target] of the pages: on an app, both make a folder named [name] in the place of [target]; on a
 * folder, [added] joins it. Nothing changes on a locked home screen, for a widget or the clock, or onto itself.
 */
fun LauncherSettings.droppedOnItem(added: String, target: String, name: String): LauncherSettings {
    val folderId = folderIdOf(target)
    return when {
        isLayoutLocked || added == target || !isAppKey(added) -> this
        folderId != null -> withAddedToFolder(folderId, added)
        isAppKey(target) && target in favorites -> withNewFolder(listOf(target, added), name)
        else -> this
    }
}

/**
 * "Criar pasta" from the menu of the app [key] (for whoever does not drag): a folder with only it, in its place, where
 * the person then adds apps. A folder still with one app when it is closed goes back to being the app ([tidied]).
 */
fun LauncherSettings.withFolderAround(key: String, name: String): LauncherSettings =
    if (isLayoutLocked || !isAppKey(key) || key !in favorites) this else withNewFolder(listOf(key), name)

/** [key] into the folder [id], from wherever it was (the pages, the dock, another folder, all the apps). */
fun LauncherSettings.withAddedToFolder(id: Int, key: String): LauncherSettings {
    val folder = folders[id]
    if (folder == null || !canJoin(folder, key)) return this
    val rest = leavingHome(key)
    return rest.folders[id]?.let { rest.copy(folders = rest.folders + (id to it.copy(apps = it.apps + key))) } ?: this
}

/** Only an app joins a folder, once, and not on a locked home screen. */
private fun LauncherSettings.canJoin(folder: HomeFolder, key: String): Boolean =
    !isLayoutLocked && isAppKey(key) && key !in folder.apps

/**
 * The apps of the folder [id] as chosen in its list ([chosen], as One UI's "Adicionar apps"): the new ones join it,
 * from wherever they were; the ones left out go to the home screen. The new ones join first, so a folder that keeps
 * fewer than two of its apps still has the new ones instead of becoming an app on the way.
 */
fun LauncherSettings.withFolderChoice(id: Int, chosen: List<String>): LauncherSettings {
    val folder = folders[id] ?: return this
    val joined = chosen.filterNot { it in folder.apps }
        .fold(this) { settings, key -> settings.withAddedToFolder(id, key) }
    return folder.apps.filterNot { it in chosen }
        .fold(joined) { settings, key -> settings.withTakenOutOfFolder(key) }
}

/** [key] out of its folder, onto the home screen at the first free room, as dragging it out does. */
fun LauncherSettings.withTakenOutOfFolder(key: String): LauncherSettings =
    if (isLayoutLocked || folderOf(key) == null || isHomeFull) this else withoutInFolders(key).withAddedToHome(key)

/** The name of the folder [id]: one line, at most [MAX_FOLDER_NAME] letters; a blank one keeps the old name. */
fun LauncherSettings.withFolderRenamed(id: Int, name: String): LauncherSettings {
    val folder = folders[id] ?: return this
    val clean = cleanFolderName(name)
    return if (clean.isEmpty()) this else copy(folders = folders + (id to folder.copy(name = clean)))
}

/** The folder [id] as it is closed: with one app left, it becomes that app again; with none, it goes. */
fun LauncherSettings.tidied(id: Int): LauncherSettings = folders[id]?.let { withFolderApps(id, it.apps) } ?: this

/** [key] out of every folder (a folder left with one app becomes it, in its place; an empty one goes). */
fun LauncherSettings.withoutInFolders(key: String): LauncherSettings {
    val id = folderOf(key) ?: return this
    return withFolderApps(id, folders.getValue(id).apps - key)
}

/**
 * The keys shown on the pages ([keys], as they were) once [key] left its folder ([after]): a folder that became its
 * last app shows that app in its place, and a folder left empty is gone.
 */
internal fun LauncherSettings.shownAfterLeavingFolder(
    keys: List<String>,
    key: String,
    after: LauncherSettings
): List<String> = keys.mapNotNull { shown ->
    val id = folderIdOf(shown)
    if (id == null || shown in after.favorites) {
        shown
    } else {
        folders[id]?.apps?.singleOrNull { it != key }?.takeIf { it in after.favorites }
    }
}

/** [key] off the pages, the dock and the folders, with its cells: about to go somewhere else. */
internal fun LauncherSettings.leavingHome(key: String): LauncherSettings = withoutInFolders(key).let {
    it.copy(
        favorites = it.favorites - key,
        dock = it.dock - key,
        positions = it.positions - key,
        positionsTurned = it.positionsTurned - key
    )
}

private fun LauncherSettings.withNewFolder(apps: List<String>, name: String): LauncherSettings {
    val place = apps.first()
    val id = (folders.keys.maxOrNull() ?: 0) + 1
    val key = folderKey(id)
    val rest = apps.drop(1).fold(this) { settings, app -> settings.leavingHome(app) }
    return rest.copy(
        favorites = rest.favorites.map { if (it == place) key else it },
        positions = rest.positions.renamed(place, key),
        positionsTurned = rest.positionsTurned.renamed(place, key),
        folders = rest.folders + (id to HomeFolder(cleanFolderName(name), apps))
    )
}

/** The folder [id] with [apps]: one app left takes the place of the folder, none takes the folder away. */
private fun LauncherSettings.withFolderApps(id: Int, apps: List<String>): LauncherSettings {
    val key = folderKey(id)
    return when (apps.size) {
        0 -> copy(
            favorites = favorites - key,
            positions = positions - key,
            positionsTurned = positionsTurned - key,
            folders = folders - id
        )
        1 -> copy(
            favorites = favorites.map { if (it == key) apps.single() else it },
            positions = positions.renamed(key, apps.single()),
            positionsTurned = positionsTurned.renamed(key, apps.single()),
            folders = folders - id
        )
        else -> copy(folders = folders + (id to folders.getValue(id).copy(apps = apps)))
    }
}

private fun Map<String, HomeCell>.renamed(from: String, to: String): Map<String, HomeCell> {
    val cell = this[from] ?: return this
    return this - from + (to to cell)
}
