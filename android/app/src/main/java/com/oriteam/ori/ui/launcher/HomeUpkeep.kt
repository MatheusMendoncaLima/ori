package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.InstalledAppsRepository
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.shortcutKeys
import com.oriteam.ori.domain.launcher.tidied
import com.oriteam.ori.domain.launcher.withAddedToHome
import com.oriteam.ori.domain.launcher.withClockSetUp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * What ORI Home keeps in order on its own while it runs, apart from what the person does: the clock widget set up
 * once, a new app put on the home screen (when chosen), a folder closed with one app tidied, and the shortcuts on the
 * home screen kept pinned with Android. Started once by LauncherViewModel, in its scope.
 */
internal class HomeUpkeep(
    private val apps: InstalledAppsRepository,
    private val settings: LauncherSettingsRepository,
    private val scope: CoroutineScope
) {
    /** [installed]: the apps of the device as the home screen follows them; [openFolder]: the folder open now. */
    fun start(installed: Flow<List<LauncherApp>>, openFolder: Flow<Int?>) {
        // The time and the date became a widget: it goes to the start of the home screen once.
        scope.launch { settings.update { it.withClockSetUp() } }
        scope.launch { apps.observeAddedPackages().collect { onAppInstalled(it, installed) } }
        scope.launch { tidyClosedFolders(openFolder) }
        scope.launch { keepShortcutsPinned() }
    }

    /**
     * An app just installed goes to the home screen when the person chose so (and it is not hidden), once Android
     * lists it; an app already there stays where it is.
     */
    private suspend fun onAppInstalled(packageName: String, installed: Flow<List<LauncherApp>>) {
        if (!settings.settings.first().addsNewApps) return
        val keys = installed.first { list -> list.any { it.packageName == packageName } }
            .filter { it.packageName == packageName }
            .map { it.key }
        settings.update { current ->
            keys.filterNot { it in current.hidden }.fold(current) { added, key -> added.withAddedToHome(key) }
        }
    }

    /**
     * A folder closed (however: back, a tap outside, Home, an app opened from it) with one app left becomes that app
     * again, and an empty one goes, as on One UI. Watched here, so no way of closing it forgets this.
     */
    private suspend fun tidyClosedFolders(openFolder: Flow<Int?>) {
        var shown: Int? = null
        openFolder.distinctUntilChanged().collect { id ->
            val closed = shown
            shown = id
            if (closed != null) scope.launch { settings.update { it.tidied(closed) } }
        }
    }

    /**
     * Android keeps a shortcut for the home screen while it is pinned: the ones on it are pinned (again, at each
     * opening, as an app updated may have dropped them), and one taken off is let go, so its app may remove it.
     */
    private suspend fun keepShortcutsPinned() {
        var before = emptySet<String>()
        settings.settings.map { it.shortcutKeys }.distinctUntilChanged().collect { keys ->
            apps.keepPinned(kept = keys, released = before - keys)
            before = keys
        }
    }
}
