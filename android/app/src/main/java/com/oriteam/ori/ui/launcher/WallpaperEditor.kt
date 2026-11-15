package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.LOCK_SHOWS_HOME
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.domain.launcher.LockScreenWallpaper
import com.oriteam.ori.domain.launcher.WallpaperChange
import com.oriteam.ori.domain.launcher.WallpaperImport
import com.oriteam.ori.domain.launcher.WallpaperStore
import com.oriteam.ori.domain.launcher.WallpaperTarget
import com.oriteam.ori.domain.launcher.lockScreenShown
import com.oriteam.ori.domain.launcher.photosInUse
import com.oriteam.ori.domain.launcher.withLockScreen
import com.oriteam.ori.domain.launcher.withPhotoRemembered
import com.oriteam.ori.domain.launcher.withWallpaper
import com.oriteam.ori.domain.launcher.withoutHistoryPhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A wallpaper being chosen: the one shown in the preview, a photo being copied, or a photo that failed. */
data class WallpaperDraft(
    val preview: LauncherWallpaper? = null,
    val isImporting: Boolean = false,
    val hasFailed: Boolean = false
)

/**
 * Choosing the wallpaper as on One UI: a choice opens a preview of the lock and the home screen with it, and only
 * "Aplicar" changes the wallpaper, on the screens the person ticked. A photo is copied first (a reduced copy); a new
 * photo that is not applied is deleted. The photos applied are kept in a short history (the newest eight) to choose
 * again; one that falls off the end, or is taken out of it, is deleted (never one a screen shows).
 */
class WallpaperEditor internal constructor(
    private val settings: LauncherSettingsRepository,
    private val wallpapers: WallpaperStore,
    private val lockScreen: LockScreenWallpaper,
    private val scope: CoroutineScope,
    /** Android refused the lock screen. A success says nothing: the preview of the lock screen shows it. */
    private val onLockScreenFailed: () -> Unit
) {
    /** Whether Android lets ORI set the lock screen here, to offer "Tela de bloqueio" next to "Tela inicial". */
    val canSetLockScreen: Boolean by lazy { lockScreen.isAvailable() }

    private val draft = MutableStateFlow(WallpaperDraft())
    val state: StateFlow<WallpaperDraft> = draft.asStateFlow()

    /** The number of the picture Android's lock screen shows, read when the wallpaper opens and after a change. */
    private val lockId = MutableStateFlow<Int?>(null)

    /**
     * What the lock screen shows now, as far as ORI can tell: the Android wallpaper, the one ORI put there, or null
     * (a picture of Android that ORI cannot see).
     */
    val lockScreenShown: Flow<LauncherWallpaper?> =
        combine(settings.settings, lockId) { current, id -> current.lockScreenShown(id) }.distinctUntilChanged()

    /** The lock screen may have changed outside ORI (in Android's settings): its number is asked again. */
    fun onRefreshLockScreen() {
        scope.launch { lockId.value = lockScreen.shownId() }
    }

    fun onPreview(wallpaper: LauncherWallpaper) = draft.update { WallpaperDraft(preview = wallpaper) }

    /** A picture chosen in the system photo picker: a reduced copy is shown in the preview. */
    fun onPhotoPicked(sourceUri: String) {
        draft.update { it.copy(isImporting = true, hasFailed = false) }
        scope.launch {
            when (val result = wallpapers.importPhoto(sourceUri)) {
                is WallpaperImport.Imported -> {
                    discardPreviewPhoto()
                    draft.value = WallpaperDraft(preview = LauncherWallpaper.Photo(result.fileName))
                }
                WallpaperImport.Failed -> draft.update { it.copy(isImporting = false, hasFailed = true) }
            }
        }
    }

    /**
     * The wallpaper of the preview goes to [target]. The home screen keeps it in the ORI Home choices; the lock screen
     * gets a picture from Android (a photo, or [lockColors] drawn as a gradient for the colors of ORI), or, for the
     * Android wallpaper, shows Android's own wallpaper again. ORI keeps which one it gave, to draw it in the previews.
     * A new photo used only on the lock screen still goes to the history, so it can be chosen again and is deleted
     * with it, also when Android refuses it.
     */
    fun onApply(target: WallpaperTarget = WallpaperTarget.HOME, lockColors: Pair<Int, Int>? = null) {
        val chosen = draft.value.preview ?: return
        draft.value = WallpaperDraft()
        scope.launch {
            if (target != WallpaperTarget.LOCK) change { it.withWallpaper(chosen) }
            if (target != WallpaperTarget.HOME) applyToLockScreen(chosen, lockColors)
        }
    }

    private suspend fun applyToLockScreen(chosen: LauncherWallpaper, lockColors: Pair<Int, Int>?) {
        val androidId = handToAndroid(chosen, lockColors)
        val photo = (chosen as? LauncherWallpaper.Photo)?.fileName
        when {
            androidId != null -> change { it.withLockScreen(chosen, androidId) }
            photo != null -> change { it.withPhotoRemembered(photo) }
        }
        if (androidId == null) onLockScreenFailed()
        lockId.value = lockScreen.shownId()
    }

    /** The number Android gave the picture of the lock screen ([LOCK_SHOWS_HOME] for its own); null: refused. */
    private suspend fun handToAndroid(chosen: LauncherWallpaper, lockColors: Pair<Int, Int>?): Int? = when {
        chosen is LauncherWallpaper.Photo -> lockScreen.setPhoto(chosen.fileName)
        chosen == LauncherWallpaper.System -> LOCK_SHOWS_HOME.takeIf { lockScreen.useHomeWallpaper() }
        lockColors != null -> lockScreen.setColors(lockColors.first, lockColors.second)
        else -> null
    }

    /** A photo of the history taken out ("Remover do histórico"); its copy is deleted. */
    fun onRemoveFromHistory(fileName: String) {
        scope.launch { change { it.withoutHistoryPhoto(fileName) } }
    }

    /** Applies a change and deletes the photo files nothing uses any more. */
    private suspend fun change(transform: (LauncherSettings) -> WallpaperChange) {
        var unused = emptyList<String>()
        settings.update { current ->
            val result = transform(current)
            unused = result.unusedFiles
            result.settings
        }
        unused.forEach { wallpapers.delete(it) }
    }

    fun onCancel() {
        scope.launch {
            discardPreviewPhoto()
            draft.value = WallpaperDraft()
        }
    }

    /** A new photo in the preview that is not applied is not kept (a photo of the history stays). */
    private suspend fun discardPreviewPhoto() {
        val shown = draft.value.preview as? LauncherWallpaper.Photo ?: return
        val current = settings.settings.first()
        val isKept = shown.fileName in current.wallpaperHistory || shown.fileName in current.photosInUse
        if (!isKept) wallpapers.delete(shown.fileName)
    }
}
