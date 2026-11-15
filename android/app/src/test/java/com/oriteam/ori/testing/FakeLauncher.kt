package com.oriteam.ori.testing

import com.oriteam.ori.domain.launcher.AppOrigin
import com.oriteam.ori.domain.launcher.HomeActivation
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.HomeSystem
import com.oriteam.ori.domain.launcher.HomeWidgetHost
import com.oriteam.ori.domain.launcher.InstalledAppsRepository
import com.oriteam.ori.domain.launcher.LOCK_SHOWS_HOME
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.LockScreenWallpaper
import com.oriteam.ori.domain.launcher.SHORTCUT_PREFIX
import com.oriteam.ori.domain.launcher.WallpaperImport
import com.oriteam.ori.domain.launcher.WallpaperStore
import com.oriteam.ori.domain.launcher.WidgetProvider
import com.oriteam.ori.domain.launcher.WidgetProviders
import com.oriteam.ori.domain.launcher.WidgetResize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

fun launcherApp(label: String, packageName: String = "app.${label.lowercase()}") =
    LauncherApp(key = "$packageName/.Main@0", packageName = packageName, label = label)

/** A shortcut of [app], as Android gives one to the home app in use. */
fun shortcutOf(app: LauncherApp, label: String) = LauncherApp(
    key = "${SHORTCUT_PREFIX}0/${app.packageName}/${label.lowercase().replace(' ', '-')}",
    packageName = app.packageName,
    label = label,
    isRemovable = false
)

class FakeInstalledApps(apps: List<LauncherApp> = emptyList()) : InstalledAppsRepository {
    val apps = MutableStateFlow(apps)

    /** Packages "installed" while the test runs; [apps] gets them first, as Android lists them. */
    val added = MutableSharedFlow<String>(extraBufferCapacity = 8)

    override fun observeAddedPackages() = added
    val opened = mutableListOf<LauncherApp>()

    /** Where each app opened grew out of (null: from nowhere, as with animations off). */
    val origins = mutableListOf<AppOrigin?>()
    var canOpen = true

    override fun observeApps() = apps

    override suspend fun open(app: LauncherApp, from: AppOrigin?): Boolean {
        if (canOpen) {
            opened += app
            origins += from
        }
        return canOpen
    }

    val detailsOpened = mutableListOf<LauncherApp>()
    val uninstallAsked = mutableListOf<LauncherApp>()

    override fun uninstall(app: LauncherApp): Boolean {
        if (canOpen) uninstallAsked += app
        return canOpen
    }

    override fun openDetails(app: LauncherApp): Boolean {
        if (canOpen) detailsOpened += app
        return canOpen
    }

    /** The shortcuts of each app, by its key; the ones that exist are found by key too. */
    val shortcuts = MutableStateFlow<Map<String, List<LauncherApp>>>(emptyMap())

    /** What ORI Home asked Android to keep pinned and to let go, call by call. */
    val pinCalls = mutableListOf<Pair<Set<String>, Set<String>>>()

    override suspend fun shortcutsOf(app: LauncherApp) = shortcuts.value[app.key].orEmpty()

    override fun observeShortcuts(keys: Set<String>) =
        shortcuts.map { all -> all.values.flatten().filter { it.key in keys } }

    override suspend fun keepPinned(kept: Set<String>, released: Set<String>) {
        pinCalls += kept to released
    }
}

/** [writeDelayMillis]: how long a write takes, as DataStore takes a moment on the device. */
class FakeLauncherSettingsRepository(
    initial: LauncherSettings = LauncherSettings(),
    var writeDelayMillis: Long = 0
) : LauncherSettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        if (writeDelayMillis > 0) delay(writeDelayMillis)
        settings.value = transform(settings.value)
    }
}

/** [approvesRequest] is what the person answers in the request of Android. */
class FakeHomeActivation(
    var isHome: Boolean = false,
    var currentHome: String? = "com.samsung.launcher",
    var approvesRequest: Boolean = true
) : HomeActivation {
    var isEnabled = false
    val enabledChanges = mutableListOf<Boolean>()

    override fun isOriHome() = isHome

    override fun currentHomePackage() = if (isHome) "com.oriteam.ori" else currentHome

    override fun setLauncherEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        enabledChanges += isEnabled
        if (!isEnabled) isHome = false
    }

    /** What happens while Android shows its request. */
    fun answerRequest() {
        isHome = isEnabled && approvesRequest
    }
}

/** Copies pictures into memory; [failsNext] makes the next copy fail, as an unreadable file would. */
class FakeWallpaperStore : WallpaperStore {
    val files = mutableSetOf<String>()
    var failsNext = false
    private var count = 0

    override suspend fun importPhoto(sourceUri: String): WallpaperImport {
        if (failsNext) {
            failsNext = false
            return WallpaperImport.Failed
        }
        count += 1
        val name = "wallpaper-$count.jpg"
        files += name
        return WallpaperImport.Imported(name)
    }

    override suspend fun delete(fileName: String) {
        files -= fileName
    }
}

fun widgetProvider(
    label: String,
    span: HomeSpan = HomeSpan(2, 2),
    needsSetup: Boolean = false,
    id: String = "app.$label/.Widget@0",
    description: String? = null
) = WidgetProvider(
    id = id,
    packageName = "app.$label",
    appLabel = label,
    label = label,
    span = span,
    needsSetup = needsSetup,
    description = description
)

/** [allowsBinding] is whether the person already allowed ORI Home to show widgets (no window of Android). */
/** [isComplete] false: Android could not give some widgets (as with many widgets on a tablet). */
class FakeHomeWidgetHost(
    private val offered: List<WidgetProvider> = emptyList(),
    var allowsBinding: Boolean = true,
    var isComplete: Boolean = true
) : HomeWidgetHost {
    val allocated = mutableSetOf<Int>()
    val bound = mutableMapOf<Int, String>()
    private var next = 100

    override suspend fun providers() = WidgetProviders(offered, isComplete)

    override fun allocate(): Int = next++.also { allocated += it }

    override fun bindIfAllowed(widgetId: Int, providerId: String): Boolean {
        if (allowsBinding) bound[widgetId] = providerId
        return allowsBinding
    }

    override fun release(widgetId: Int) {
        allocated -= widgetId
        bound -= widgetId
    }

    override fun isAvailable(widgetId: Int) = widgetId in allocated

    /** How each widget may be resized; by default from one cell to the whole page, both ways. */
    var resize = WidgetResize(HomeSpan(1, 1), HomeSpan(Int.MAX_VALUE, Int.MAX_VALUE), true, true)

    override suspend fun resizeOf(widgetId: Int) = resize.takeIf { widgetId in allocated }

    /** The widgets whose app has a setup screen to open again. */
    val withSetup = mutableSetOf<Int>()

    override suspend fun canSetUp(widgetId: Int) = widgetId in withSetup

    override suspend fun boundIds() = allocated.toList()
}

/**
 * [allows] is whether Android lets ORI set the lock screen; what was set is kept to look at. As Android, each picture
 * set gets a new number, and [shown] is the number of the one the lock screen shows (negative: none of its own).
 */
class FakeLockScreenWallpaper(var allows: Boolean = true) : LockScreenWallpaper {
    var photo: String? = null
    var colors: Pair<Int, Int>? = null
    var shown: Int? = LOCK_SHOWS_HOME
    private var lastId = 0

    override fun isAvailable() = allows

    override suspend fun setPhoto(fileName: String): Int? {
        if (allows) photo = fileName
        return newPicture()
    }

    override suspend fun setColors(topArgb: Int, bottomArgb: Int): Int? {
        if (allows) colors = topArgb to bottomArgb
        return newPicture()
    }

    /** The lock screen was set to show the Android wallpaper again. */
    var usesHomeWallpaper = false

    override suspend fun useHomeWallpaper(): Boolean {
        if (allows) {
            usesHomeWallpaper = true
            shown = LOCK_SHOWS_HOME
        }
        return allows
    }

    override suspend fun shownId(): Int? = shown

    /** A picture put there by something else (the settings of Android): a number ORI did not get. */
    fun changedOutside() {
        shown = ++lastId
    }

    private fun newPicture(): Int? {
        if (!allows) return null
        shown = ++lastId
        return shown
    }
}

/** Android's notification panel: [canOpen] false is an Android that does not let a home screen open it. */
class FakeHomeSystem(var canOpen: Boolean = true) : HomeSystem {
    var notificationsOpened = 0

    /** The counts of the notifications, as the listener would give them; [canCount] is the person's answer. */
    override val notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    var canCount = false
    var accessPagesOpened = 0

    override fun canCountNotifications() = canCount

    override fun openNotificationAccess(): Boolean {
        accessPagesOpened++
        return true
    }

    override fun openNotifications(): Boolean {
        if (canOpen) notificationsOpened++
        return canOpen
    }
}
