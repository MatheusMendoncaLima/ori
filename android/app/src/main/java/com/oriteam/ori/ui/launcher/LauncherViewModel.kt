package com.oriteam.ori.ui.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.appLanguageTag
import com.oriteam.ori.domain.launcher.AppOrigin
import com.oriteam.ori.domain.launcher.BadgeStyle
import com.oriteam.ori.domain.launcher.HomeScreenMode
import com.oriteam.ori.domain.launcher.HomeSystem
import com.oriteam.ori.domain.launcher.HomeWidgetHost
import com.oriteam.ori.domain.launcher.InstalledAppsRepository
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.LockScreenWallpaper
import com.oriteam.ori.domain.launcher.StimulusMotion
import com.oriteam.ori.domain.launcher.WallpaperStore
import com.oriteam.ori.domain.launcher.badgesShown
import com.oriteam.ori.domain.launcher.drawerApps
import com.oriteam.ori.domain.launcher.isHomeFull
import com.oriteam.ori.domain.launcher.matchesSearch
import com.oriteam.ori.domain.launcher.recentApps
import com.oriteam.ori.domain.launcher.shortcutKeys
import com.oriteam.ori.domain.launcher.toggleFavorite
import com.oriteam.ori.domain.launcher.toggleHidden
import com.oriteam.ori.domain.launcher.withOpened
import com.oriteam.ori.domain.preferences.AppPreferences
import com.oriteam.ori.domain.preferences.DevicePreferencesRepository
import com.oriteam.ori.domain.preferences.MotionLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * ORI Home: the home screen, all the apps, the widgets, the wallpaper and the options. Everything comes from the
 * device (the apps, the widgets, the ORI Home choices and the device copy of the appearance), so the launcher opens
 * without the account, the network or Firebase. Only the panel on the left shows the routine or the phrases, when
 * the person opens it.
 */
// One injected boundary per part of Android the home screen uses (apps, widgets, wallpapers, the system around it).
@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val apps: InstalledAppsRepository,
    private val launcherSettings: LauncherSettingsRepository,
    devicePreferences: DevicePreferencesRepository,
    wallpapers: WallpaperStore,
    widgetHost: HomeWidgetHost,
    lockScreen: LockScreenWallpaper,
    private val homeSystem: HomeSystem
) : ViewModel() {
    private val interaction = MutableStateFlow(Interaction())
    private val locale: Locale get() = Locale.forLanguageTag(appLanguageTag)

    /** Low stimulation, buttons, icon size, packed or free, pages or list, and the panel. */
    val appearance = LauncherAppearanceEditor(launcherSettings, viewModelScope)

    /**
     * The swipe down for the notifications, new apps on the home screen, a phone that turns, the badges, the hidden
     * apps and the recent apps in the dock.
     */
    val behavior = HomeBehaviorEditor(launcherSettings, viewModelScope, homeSystem)

    /** The shape of the icons, all the apps as a list or pages, and whether there is a home screen at all. */
    val style = HomeStyleEditor(launcherSettings, viewModelScope)

    val wallpaper = WallpaperEditor(launcherSettings, wallpapers, lockScreen, viewModelScope) {
        interaction.update { it.copy(messageRes = R.string.launcher_wallpaper_lock_failed) }
    }

    val widgets = HomeWidgetEditor(widgetHost, launcherSettings, viewModelScope) { message ->
        interaction.update { it.copy(messageRes = message) }
    }

    /** The app's page and uninstalling, from the menu of an app. */
    val appMenu =
        AppMenuActions(apps) { interaction.update { it.copy(messageRes = R.string.launcher_app_unavailable) } }

    /** Dropping, removing and moving items of the pages and the dock. */
    val layout = HomeLayoutEditor(
        settings = launcherSettings,
        scope = viewModelScope,
        widgets = widgets,
        shownKeys = { uiState.value.home.map { it.key } },
        isLocked = { uiState.value.settings.isLayoutLocked },
        onLocked = { interaction.update { it.copy(infoRes = R.string.launcher_layout_locked) } }
    )

    /** Folders: made by dropping an app on another, opened, renamed, filled from a list. */
    val folders = HomeFolderEditor(
        settings = launcherSettings,
        scope = viewModelScope,
        current = { uiState.value.settings },
        onLocked = { interaction.update { it.copy(infoRes = R.string.launcher_layout_locked) } },
        onFull = { interaction.update { it.copy(messageRes = R.string.launcher_favorites_full) } },
        onOpenChange = { id -> interaction.update { it.copy(openFolderId = id, messageRes = null, infoRes = null) } }
    )

    /** The apps of the device, followed once for the home screen and all the apps together. */
    private val installedApps = apps.observeApps()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    /**
     * The apps and the shortcuts of apps the home screen can show: a shortcut on it is read from Android with its name
     * of now, and followed only while there is one.
     */
    private val homeApps = combine(
        installedApps,
        launcherSettings.settings.map { it.shortcutKeys }.distinctUntilChanged().flatMapLatest(apps::observeShortcuts),
        List<LauncherApp>::plus
    )

    /**
     * Every app not hidden, sorted in the language's order, and the ones the search finds. Sorted again only when the
     * apps or the hidden ones change, filtered again only when the search changes: not at each drop, page or editing
     * change, which would sort every app on the main thread for nothing.
     */
    private val visibleApps = combine(
        installedApps,
        launcherSettings.settings.map { it.hidden }.distinctUntilChanged()
    ) { installed, hidden -> drawerApps(installed, hidden, query = "", locale) }

    private val drawer = combine(visibleApps, interaction.map { it.query }.distinctUntilChanged()) { visible, query ->
        visible to visible.filter { it.label.matchesSearch(query) }
    }

    /** The apps opened from ORI Home, the last first: only in memory, gone when ORI Home closes. */
    private val recentKeys = MutableStateFlow<List<String>>(emptyList())

    /** The counts of the notifications, followed only while the person wants badges. */
    private val badgeCounts = launcherSettings.settings.map { it.badgesShown != BadgeStyle.OFF }.distinctUntilChanged()
        .flatMapLatest { isOn -> if (isOn) homeSystem.notificationCounts else flowOf(emptyMap()) }

    val uiState: StateFlow<LauncherUiState> = combine(
        homeApps,
        launcherSettings.settings,
        interaction,
        combine(wallpaper.state, wallpaper.lockScreenShown, widgets.state, badgeCounts, recentKeys, ::HomeExtras),
        drawer
    ) { available, settings, local, extras, (visible, found) ->
        val byKey = available.associateBy { it.key }
        val draft = extras.wallpaper
        LauncherUiState(
            isLoading = false,
            pane = local.pane,
            home = homeEntries(byKey, settings),
            dock = settings.dock.mapNotNull(byKey::get),
            drawer = found,
            apps = visible,
            hiddenApps = available.filter { it.key in settings.hidden }.sortedBy { it.label.lowercase(locale) },
            query = local.query,
            isEditingHome = local.isEditingHome,
            wallpaperDraft = draft,
            lockScreenShown = extras.lockScreen,
            widgetCatalog = extras.widgets,
            settings = settings,
            messageRes = local.messageRes ?: R.string.launcher_wallpaper_failed.takeIf { draft.hasFailed },
            infoRes = local.infoRes,
            homeRequest = local.homeRequest,
            openFolderId = local.openFolderId,
            badgeCounts = extras.badgeCounts,
            recentApps = recentApps(extras.recentKeys, byKey, settings)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LauncherUiState())

    /**
     * The appearance of ORI, read from the device only. The level of stimulus of ORI Home goes on top of the person's
     * own choice: fewer decorations, and less motion or none (a motion the person turned off stays off).
     */
    val appPreferences: StateFlow<AppPreferences?> = combine(
        devicePreferences.preferences,
        devicePreferences.accountCopy,
        launcherSettings.settings
    ) { device, account, settings ->
        val stimulus = settings.stimulus
        val motion = when (stimulus.motion) {
            StimulusMotion.NORMAL -> account.motion
            StimulusMotion.REDUCED -> if (account.motion == MotionLevel.OFF) MotionLevel.OFF else MotionLevel.REDUCED
            StimulusMotion.OFF -> MotionLevel.OFF
        }
        val calm = account.copy(isSimplified = account.isSimplified || stimulus.isSimplified, motion = motion)
        AppPreferences(account = calm, device = device)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        HomeUpkeep(apps, launcherSettings, viewModelScope).start(installedApps, interaction.map { it.openFolderId })
    }

    /** A pull down on the home screen: Android's notifications, as on One UI, while the person keeps that gesture. */
    fun onSwipeDown() {
        if (uiState.value.settings.swipesDownForNotifications) homeSystem.openNotifications()
    }

    fun onOpenPane(pane: LauncherPane) {
        interaction.update {
            val fromEditing = it.isEditingHome || (it.returnsToEditing && it.pane != LauncherPane.HOME)
            it.copy(
                pane = pane,
                isEditingHome = false,
                returnsToEditing = fromEditing,
                messageRes = null,
                infoRes = null,
                openFolderId = null
            )
        }
        if (pane == LauncherPane.WIDGETS) widgets.loadProviders()
        if (pane == LauncherPane.WALLPAPER) wallpaper.onRefreshLockScreen()
    }

    /** Home pressed while ORI Home is shown: everything closes and the pages go back to the first one. */
    fun onHomePressed() = interaction.update { Interaction(homeRequest = it.homeRequest + 1) }

    /**
     * Back: closes what is open and stays on the same page, as on Android. A pane opened while editing (the wallpaper,
     * the widgets, the options, all the apps) goes back to editing; editing goes back to the home screen.
     */
    fun onBack() = interaction.update {
        Interaction(
            homeRequest = it.homeRequest,
            isEditingHome = it.pane != LauncherPane.HOME && it.returnsToEditing && hasHomeScreen
        )
    }

    /** With only all the apps there is no home screen, so nothing to edit or to go back to editing. */
    private val hasHomeScreen: Boolean get() = uiState.value.settings.screenMode == HomeScreenMode.HOME_AND_APPS

    fun onQueryChange(value: String) = interaction.update { it.copy(query = value) }

    /** A message of the home screen was shown long enough: it goes away. */
    fun onMessageSeen() = interaction.update { it.copy(messageRes = null, infoRes = null) }

    /** Holding the screen or an icon starts editing; "Pronto" or a tap on the empty screen ends it. */
    fun onEditingChange(isEditing: Boolean) {
        interaction.update {
            it.copy(
                pane = LauncherPane.HOME,
                isEditingHome = isEditing && hasHomeScreen,
                messageRes = null,
                infoRes = null,
                openFolderId = null
            )
        }
        // The widgets are read ahead, the first time: "Widgets" is one tap away, and reading every widget of the
        // device took about 200 ms on the test tablet, with "Carregando" on screen.
        val catalog = widgets.state.value
        if (isEditing && catalog.providers.isEmpty() && !catalog.isLoading) widgets.loadProviders()
    }

    /** Opens [app], growing out of its icon ([from]) when the motion level lets things move. */
    fun onOpenApp(app: LauncherApp, from: AppOrigin? = null) {
        viewModelScope.launch {
            if (apps.open(app, from)) {
                recentKeys.update { it.withOpened(app.key) }
                interaction.update { Interaction(homeRequest = it.homeRequest) }
            } else {
                interaction.update { it.copy(messageRes = R.string.launcher_app_unavailable) }
            }
        }
    }

    fun onToggleFavorite(app: LauncherApp) {
        if (app.key !in uiState.value.favoriteKeys && uiState.value.settings.isHomeFull) {
            interaction.update { it.copy(messageRes = R.string.launcher_favorites_full) }
            return
        }
        update { it.toggleFavorite(app.key) }
    }

    fun onToggleHidden(app: LauncherApp) = update { it.toggleHidden(app.key) }

    private fun update(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { launcherSettings.update(transform) }
    }
}
