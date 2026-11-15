package com.oriteam.ori.data.launcher

import com.oriteam.ori.domain.launcher.HomeFolder
import com.oriteam.ori.domain.launcher.cleanFolderName

/* The folders of the home screen as the DataStore keeps them, in the same plain lines as the other choices. */

private const val FOLDER_FIELDS = 3
private const val FOLDER_APPS_SEPARATOR = ","

/** The folders as lines of "id, name, apps" (the name has no tabs or line breaks: [cleanFolderName]). */
internal fun Map<Int, HomeFolder>.toStoredFolders(): String = entries.joinToString(LINE_SEPARATOR) { (id, folder) ->
    listOf(id.toString(), cleanFolderName(folder.name), folder.apps.joinToString(FOLDER_APPS_SEPARATOR))
        .joinToString(FIELD_SEPARATOR)
}

/** The folders stored; a broken line, or one without apps, is skipped, never the whole home screen. */
internal fun foldersOf(stored: String?): Map<Int, HomeFolder> = linesOf(stored, FOLDER_FIELDS).mapNotNull { fields ->
    val id = fields[0].toIntOrNull()
    val apps = fields[2].split(FOLDER_APPS_SEPARATOR).filter { it.isNotBlank() }
    if (id == null || apps.isEmpty()) null else id to HomeFolder(fields[1], apps)
}.toMap()
