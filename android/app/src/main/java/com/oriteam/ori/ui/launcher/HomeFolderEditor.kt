package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.droppedOnItem
import com.oriteam.ori.domain.launcher.isHomeFull
import com.oriteam.ori.domain.launcher.withFolderAround
import com.oriteam.ori.domain.launcher.withFolderChoice
import com.oriteam.ori.domain.launcher.withFolderRenamed
import com.oriteam.ori.domain.launcher.withTakenOutOfFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The folders of the home screen, as on One UI: an app dropped on another makes one, a tap opens it over the home
 * screen, its name is changed there, and its apps are chosen from a list or taken out. The rules are in the domain
 * (HomeFolderEditing.kt); this applies them to the stored choices and says which folder is open ([onOpenChange]).
 *
 * A folder left with one app becomes that app again when it is closed, however it is closed (LauncherViewModel watches
 * the folder open, so back, Home and an app opened all tidy it).
 */
class HomeFolderEditor internal constructor(
    private val settings: LauncherSettingsRepository,
    private val scope: CoroutineScope,
    /** The choices as shown now: a locked or full home screen stops a change, and the person is told why. */
    private val current: () -> LauncherSettings,
    private val onLocked: () -> Unit,
    private val onFull: () -> Unit,
    /** Opens the folder of that id over the home screen, or closes it (null). */
    private val onOpenChange: (Int?) -> Unit
) {
    fun onOpen(id: Int) = onOpenChange(id)

    fun onClose() = onOpenChange(null)

    /** The app [key] dropped on the app or folder [target] of the pages: a new folder, or one more app in it. */
    fun onDropOnItem(key: String, target: String) = update { it.droppedOnItem(key, target, name = "") }

    /** "Criar pasta" from the menu of an app of the pages: a folder with it, opened at once for the others. */
    fun onCreate(key: String) {
        if (current().isLayoutLocked) return onLocked()
        scope.launch {
            var made: Int? = null
            settings.update { before ->
                val after = before.withFolderAround(key, name = "")
                made = after.folders.keys.firstOrNull { it !in before.folders }
                after
            }
            made?.let(onOpenChange)
        }
    }

    fun onRename(id: Int, name: String) = update { it.withFolderRenamed(id, name) }

    /** The apps chosen in the list of the folder [id]; the ones left out go to the home screen, while it has room. */
    fun onChooseApps(id: Int, chosen: List<String>) {
        val shown = current()
        val isLeavingAny = shown.folders[id]?.apps.orEmpty().any { it !in chosen }
        if (!shown.isLayoutLocked && isLeavingAny && shown.isHomeFull) onFull()
        update { it.withFolderChoice(id, chosen) }
    }

    /** "Tirar da pasta": the app goes to the first free place of the home screen, while it has room. */
    fun onTakeOut(key: String) = when {
        current().isLayoutLocked -> onLocked()
        current().isHomeFull -> onFull()
        else -> update { it.withTakenOutOfFolder(key) }
    }

    private fun update(transform: (LauncherSettings) -> LauncherSettings) {
        if (current().isLayoutLocked) return onLocked()
        scope.launch { settings.update(transform) }
    }
}
