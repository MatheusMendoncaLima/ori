package com.oriteam.ori.data.launcher

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.oriteam.ori.domain.launcher.BadgeStyle
import com.oriteam.ori.domain.launcher.DrawerStyle
import com.oriteam.ori.domain.launcher.HomeArrangement
import com.oriteam.ori.domain.launcher.HomeScreenMode
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.domain.launcher.LauncherIconSize
import com.oriteam.ori.domain.launcher.LauncherPanel
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.domain.launcher.LockScreenChoice
import com.oriteam.ori.domain.launcher.MAX_HOME_PAGES
import com.oriteam.ori.domain.launcher.StimulusChoices
import com.oriteam.ori.domain.launcher.StimulusMotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val FAVORITES_KEY = stringPreferencesKey("launcher_favorites")
private val HIDDEN_KEY = stringSetPreferencesKey("launcher_hidden")

/** The switch of low stimulation, before the levels: read once to keep what it did, then erased. */
private val RETIRED_LOW_STIMULATION_KEY = booleanPreferencesKey("launcher_low_stimulation")
private val STIMULUS_MOTION_KEY = stringPreferencesKey("launcher_stimulus_motion")
private val STIMULUS_WALLPAPER_KEY = booleanPreferencesKey("launcher_stimulus_wallpaper")
private val STIMULUS_COLORS_KEY = booleanPreferencesKey("launcher_stimulus_colors")
private val STIMULUS_SIMPLIFIED_KEY = booleanPreferencesKey("launcher_stimulus_simplified")
private val PREVIOUS_LAUNCHER_KEY = stringPreferencesKey("launcher_previous")
private val SHOWS_BUTTONS_KEY = booleanPreferencesKey("launcher_shows_buttons")
private val ICON_SIZE_KEY = stringPreferencesKey("launcher_icon_size")
private val WALLPAPER_KEY = stringPreferencesKey("launcher_wallpaper")
private val ARRANGEMENT_KEY = stringPreferencesKey("launcher_arrangement")

/** The home screen had a scrolling list as an option; it was taken out, and its stored choice is erased. */
private val RETIRED_PAGING_KEY = stringPreferencesKey("launcher_paging")
private val POSITIONS_KEY = stringPreferencesKey("launcher_positions")
private val POSITIONS_TURNED_KEY = stringPreferencesKey("launcher_positions_turned")
private val WIDGET_SPANS_KEY = stringPreferencesKey("launcher_widget_spans")
private val PANEL_KEY = stringPreferencesKey("launcher_panel")
private val DOCK_KEY = stringPreferencesKey("launcher_dock")
private val CLOCK_SET_UP_KEY = booleanPreferencesKey("launcher_clock_set_up")
private val WALLPAPER_HISTORY_KEY = stringPreferencesKey("launcher_wallpaper_history")
private val LOCK_WALLPAPER_KEY = stringPreferencesKey("launcher_lock_wallpaper")
private val ICON_SHAPE_KEY = stringPreferencesKey("launcher_icon_outline")

/** The shape had two choices, round and "SQUARE" (rounded corners); a third took that name, so the key changed. */
private val RETIRED_ICON_SHAPE_KEY = stringPreferencesKey("launcher_icon_shape")
private val DRAWER_STYLE_KEY = stringPreferencesKey("launcher_drawer_style")
private val SCREEN_MODE_KEY = stringPreferencesKey("launcher_screen_mode")
private val LOCK_WALLPAPER_ID_KEY = intPreferencesKey("launcher_lock_wallpaper_id")
private val PAGE_COUNT_KEY = intPreferencesKey("launcher_page_count")
private val MAIN_PAGE_KEY = intPreferencesKey("launcher_main_page")
private val LAYOUT_LOCKED_KEY = booleanPreferencesKey("launcher_layout_locked")
private val GRID_KEY = stringPreferencesKey("launcher_grid")
private val SWIPE_DOWN_KEY = booleanPreferencesKey("launcher_swipe_down_notifications")
private val SHOWS_LABELS_KEY = booleanPreferencesKey("launcher_shows_labels")
private val ADDS_NEW_APPS_KEY = booleanPreferencesKey("launcher_adds_new_apps")
private val TURNS_ON_PHONE_KEY = booleanPreferencesKey("launcher_turns_on_phone")
private val FOLDERS_KEY = stringPreferencesKey("launcher_folders")
private val BADGES_KEY = stringPreferencesKey("launcher_badges")
private val RECENT_APPS_KEY = booleanPreferencesKey("launcher_recent_apps")
private val DROP_PREVIEW_KEY = booleanPreferencesKey("launcher_drop_preview")
private val SHOWS_DOCK_KEY = booleanPreferencesKey("launcher_shows_dock")

/** ORI Home choices in the device DataStore; the home screen keeps its order (one key per line). */
class DataStoreLauncherSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : LauncherSettingsRepository {
    override val settings: Flow<LauncherSettings> = dataStore.data.map { it.toSettings() }.distinctUntilChanged()

    override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        dataStore.edit { stored -> stored.write(transform(stored.toSettings())) }
    }

    private fun MutablePreferences.write(settings: LauncherSettings) {
        this[FAVORITES_KEY] = settings.favorites.joinToString("\n")
        this[DOCK_KEY] = settings.dock.joinToString("\n")
        this[CLOCK_SET_UP_KEY] = settings.isClockSetUp
        this[WALLPAPER_HISTORY_KEY] = settings.wallpaperHistory.joinToString("\n")
        writeLockWallpaper(settings.lockWallpaper)
        this[PAGE_COUNT_KEY] = settings.homePageCount
        this[MAIN_PAGE_KEY] = settings.mainPage
        this[LAYOUT_LOCKED_KEY] = settings.isLayoutLocked
        putOrRemove(GRID_KEY, settings.homeGrid?.toStored())
        this[SWIPE_DOWN_KEY] = settings.swipesDownForNotifications
        this[SHOWS_LABELS_KEY] = settings.showsLabels
        this[ADDS_NEW_APPS_KEY] = settings.addsNewApps
        this[TURNS_ON_PHONE_KEY] = settings.turnsOnPhone
        this[FOLDERS_KEY] = settings.folders.toStoredFolders()
        this[BADGES_KEY] = settings.badges.name
        this[RECENT_APPS_KEY] = settings.showsRecentApps
        this[DROP_PREVIEW_KEY] = settings.showsDropPreview
        this[SHOWS_DOCK_KEY] = settings.showsDock
        this[HIDDEN_KEY] = settings.hidden
        writeStimulus(settings.stimulus)
        putOrRemove(PREVIOUS_LAUNCHER_KEY, settings.previousLauncher)
        this[SHOWS_BUTTONS_KEY] = settings.showsButtons
        this[ICON_SIZE_KEY] = settings.iconSize.name
        this[ICON_SHAPE_KEY] = settings.iconShape.name
        remove(RETIRED_ICON_SHAPE_KEY)
        this[DRAWER_STYLE_KEY] = settings.drawerStyle.name
        this[SCREEN_MODE_KEY] = settings.screenMode.name
        putOrRemove(WALLPAPER_KEY, settings.wallpaper.toStored())
        this[ARRANGEMENT_KEY] = settings.arrangement.name
        remove(RETIRED_PAGING_KEY)
        this[POSITIONS_KEY] = settings.positions.toStoredCells()
        this[POSITIONS_TURNED_KEY] = settings.positionsTurned.toStoredCells()
        this[WIDGET_SPANS_KEY] = settings.widgetSpans.toStoredSpans()
        this[PANEL_KEY] = settings.panel.name
    }

    private fun MutablePreferences.writeStimulus(stimulus: StimulusChoices) {
        this[STIMULUS_MOTION_KEY] = stimulus.motion.name
        this[STIMULUS_WALLPAPER_KEY] = stimulus.showsWallpaper
        this[STIMULUS_COLORS_KEY] = stimulus.colorfulIcons
        this[STIMULUS_SIMPLIFIED_KEY] = stimulus.isSimplified
        remove(RETIRED_LOW_STIMULATION_KEY)
    }

    /**
     * The choices of stimulus; before them, only the switch of low stimulation, which made the icons grey, the
     * motion gentler and the interface simplified (as a level chosen one by one).
     */
    private fun Preferences.stimulus(): StimulusChoices {
        if (this[STIMULUS_MOTION_KEY] == null && this[RETIRED_LOW_STIMULATION_KEY] == true) {
            return StimulusChoices(motion = StimulusMotion.REDUCED, colorfulIcons = false, isSimplified = true)
        }
        return StimulusChoices(
            motion = enumOf(this[STIMULUS_MOTION_KEY], StimulusMotion.NORMAL),
            showsWallpaper = this[STIMULUS_WALLPAPER_KEY] ?: true,
            colorfulIcons = this[STIMULUS_COLORS_KEY] ?: true,
            isSimplified = this[STIMULUS_SIMPLIFIED_KEY] ?: false
        )
    }

    /** The shape of the icons; one stored under the old key is round, or its "SQUARE" with rounded corners. */
    private fun Preferences.iconShape(): IconShape {
        val retired = this[RETIRED_ICON_SHAPE_KEY]
        return when {
            this[ICON_SHAPE_KEY] != null -> enumOf(this[ICON_SHAPE_KEY], IconShape.ROUNDED)
            retired == IconShape.ROUND.name -> IconShape.ROUND
            else -> IconShape.ROUNDED
        }
    }

    private fun MutablePreferences.writeLockWallpaper(choice: LockScreenChoice?) {
        putOrRemove(LOCK_WALLPAPER_KEY, choice?.wallpaper?.toStored())
        if (choice == null) remove(LOCK_WALLPAPER_ID_KEY) else this[LOCK_WALLPAPER_ID_KEY] = choice.androidId
    }

    private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private fun Preferences.toSettings() = LauncherSettings(
        favorites = keysOf(this[FAVORITES_KEY]),
        dock = keysOf(this[DOCK_KEY]),
        isClockSetUp = this[CLOCK_SET_UP_KEY] ?: false,
        wallpaperHistory = keysOf(this[WALLPAPER_HISTORY_KEY]),
        lockWallpaper = lockChoiceOf(this[LOCK_WALLPAPER_KEY], this[LOCK_WALLPAPER_ID_KEY]),
        homePageCount = (this[PAGE_COUNT_KEY] ?: 1).coerceIn(1, MAX_HOME_PAGES),
        mainPage = (this[MAIN_PAGE_KEY] ?: 0).coerceAtLeast(0),
        isLayoutLocked = this[LAYOUT_LOCKED_KEY] ?: false,
        homeGrid = gridOf(this[GRID_KEY]),
        swipesDownForNotifications = this[SWIPE_DOWN_KEY] ?: true,
        showsLabels = this[SHOWS_LABELS_KEY] ?: true,
        addsNewApps = this[ADDS_NEW_APPS_KEY] ?: false,
        turnsOnPhone = this[TURNS_ON_PHONE_KEY] ?: false,
        folders = foldersOf(this[FOLDERS_KEY]),
        badges = enumOf(this[BADGES_KEY], BadgeStyle.NUMBER),
        showsRecentApps = this[RECENT_APPS_KEY] ?: false,
        showsDropPreview = this[DROP_PREVIEW_KEY] ?: false,
        showsDock = this[SHOWS_DOCK_KEY] ?: true,
        hidden = this[HIDDEN_KEY].orEmpty(),
        stimulus = stimulus(),
        previousLauncher = this[PREVIOUS_LAUNCHER_KEY],
        showsButtons = this[SHOWS_BUTTONS_KEY] ?: false,
        iconSize = enumOf(this[ICON_SIZE_KEY], LauncherIconSize.MEDIUM),
        iconShape = iconShape(),
        drawerStyle = enumOf(this[DRAWER_STYLE_KEY], DrawerStyle.SCROLL),
        screenMode = enumOf(this[SCREEN_MODE_KEY], HomeScreenMode.HOME_AND_APPS),
        wallpaper = wallpaperOf(this[WALLPAPER_KEY]),
        arrangement = enumOf(this[ARRANGEMENT_KEY], HomeArrangement.PACKED),
        positions = cellsOf(this[POSITIONS_KEY]),
        positionsTurned = cellsOf(this[POSITIONS_TURNED_KEY]),
        widgetSpans = spansOf(this[WIDGET_SPANS_KEY]),
        panel = enumOf(this[PANEL_KEY], LauncherPanel.ROUTINE)
    )
}

/** One key per line; blank lines are skipped. */
/**
 * The wallpaper ORI put on the lock screen: both parts, or none. A value this version does not know is none (the
 * previews then draw the lock screen as one ORI cannot see).
 */
internal fun lockChoiceOf(stored: String?, androidId: Int?): LockScreenChoice? {
    if (stored == null || androidId == null) return null
    val wallpaper = wallpaperOf(stored).takeUnless { it == LauncherWallpaper.System } ?: return null
    return LockScreenChoice(wallpaper, androidId)
}

private fun keysOf(stored: String?): List<String> = stored?.split("\n")?.filter { it.isNotBlank() }.orEmpty()
