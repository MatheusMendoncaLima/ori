package com.oriteam.ori.domain.launcher

import kotlinx.coroutines.flow.Flow
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Most apps and widgets on the home screen, over all its pages: more would turn it into a second drawer. */
const val MAX_HOME_ITEMS = 60

/** The dock (the row at the bottom, on every page) never takes more apps than this, even on a wide tablet. */
const val MAX_DOCK_ITEMS = 7

/** The clock widget of ORI (the time and the date), an item of the home screen like any widget. */
const val CLOCK_KEY = "ori:clock"

/**
 * An app that can be opened from the launcher. [key] names the activity and the profile ("package/activity@user"),
 * so the same app of a work profile is another entry. A shortcut of an app (AppShortcuts.kt) is one too, under a key
 * of its own, so it goes wherever an app goes.
 */
data class LauncherApp(
    val key: String,
    val packageName: String,
    val label: String,
    /** False for an app that came with the device: Android does not let it be uninstalled. */
    val isRemovable: Boolean = true
)

/**
 * What the person chose for ORI Home, kept only on this device (it depends on the apps installed here):
 * the favorites in their order, the apps hidden from the drawer, how much it stimulates, the look of the home screen
 * and the launcher used before.
 */
data class LauncherSettings(
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    /** How much ORI Home stimulates: the choices of a level ("Padrão" to "Nenhum"), or chosen one by one. */
    val stimulus: StimulusChoices = StimulusChoices(),
    /** The package of the home app used before ORI Home, to go back to it. */
    val previousLauncher: String? = null,
    /**
     * The home screen is made of gestures (drag up for all the apps, hold to organize), with no buttons on it.
     * True shows the options and "Todos os apps" as buttons too, for whoever prefers to see them.
     */
    val showsButtons: Boolean = false,
    val iconSize: LauncherIconSize = LauncherIconSize.MEDIUM,
    /** The same shape for every icon (apps bring their own, and they mixed). */
    val iconShape: IconShape = IconShape.ROUNDED,
    val drawerStyle: DrawerStyle = DrawerStyle.SCROLL,
    val screenMode: HomeScreenMode = HomeScreenMode.HOME_AND_APPS,
    val wallpaper: LauncherWallpaper = LauncherWallpaper.System,
    val arrangement: HomeArrangement = HomeArrangement.PACKED,
    /** Where each item is, for [HomeArrangement.FREE]; an item without a cell goes to the first free one. */
    val positions: Map<String, HomeCell> = emptyMap(),
    /**
     * Where each item is with a tablet turned ([HomeOrientation.TURNED]), on the transposed grid. An item without a
     * cell here goes to its upright page, in the upright reading order.
     */
    val positionsTurned: Map<String, HomeCell> = emptyMap(),
    /** The cells each widget takes, by its id. */
    val widgetSpans: Map<Int, HomeSpan> = emptyMap(),
    val panel: LauncherPanel = LauncherPanel.ROUTINE,
    /** The apps of the dock, the row at the bottom shown on every page, in order. */
    val dock: List<String> = emptyList(),
    /**
     * The clock widget was put on the home screen once. After that, taking it out is the person's choice and it does
     * not come back on its own.
     */
    val isClockSetUp: Boolean = false,
    /** The photos used as wallpaper, the newest first, to choose again ([MAX_WALLPAPER_HISTORY] at most). */
    val wallpaperHistory: List<String> = emptyList(),
    /** The wallpaper ORI put on the lock screen, to draw it in the previews while Android still shows it. */
    val lockWallpaper: LockScreenChoice? = null,
    /** The pages the person made (with "+"); there are more when the items need them. */
    val homePageCount: Int = 1,
    /** The page the Home button goes back to (the house on One UI), counted from the first page after the panel. */
    val mainPage: Int = 0,
    /**
     * "Travar a tela inicial", as on One UI: the items of the pages and the dock stay where they are and on the home
     * screen (none is carried, moved or taken out, and no page goes), so a hold by mistake changes nothing. Adding an
     * app or a widget still works.
     */
    val isLayoutLocked: Boolean = false,
    /**
     * The grid the person chose upright ([GridChoice]); null: "Automática", the columns and rows follow the icon size,
     * so a larger icon gives fewer of them.
     */
    val homeGrid: GridChoice? = null,
    /** A swipe down on the home screen opens Android's notifications, as on One UI (on by default, as there). */
    val swipesDownForNotifications: Boolean = true,
    /** The names under the icons of the home screen; without them the icons get the room (One UI's "nomes"). */
    val showsLabels: Boolean = true,
    /** An app installed goes to the home screen by itself, at the first free cell (off, as on One UI tablets). */
    val addsNewApps: Boolean = false,
    /** The home screen of a phone turns with it (One UI's "Girar para o modo paisagem"); otherwise it stays upright. */
    val turnsOnPhone: Boolean = false,
    /** The folders of the pages, by id ([HomeFolder]); a folder is in [favorites] as `folder:<id>`. */
    val folders: Map<Int, HomeFolder> = emptyMap(),
    /**
     * The badges on the icons (BadgeStyle.kt): the number, as on One UI, shown once the person allows Android's
     * "Acesso a notificações"; a dot or none is the person's choice.
     */
    val badges: BadgeStyle = BadgeStyle.NUMBER,
    /** The dock of a tablet shows the last apps opened from ORI Home, after its own (RecentApps.kt). */
    val showsRecentApps: Boolean = false,
    /**
     * While an app or a folder is carried, a soft frame shows the cell where it will land (red when it does not fit).
     * Off until the person turns it on; a widget and the clock always show theirs, as they take several cells.
     */
    val showsDropPreview: Boolean = false,
    /**
     * "Mostrar o dock": off, the row of apps at the bottom is not shown and the pages get its room. Its apps ([dock])
     * are kept for when it comes back, and are still in all the apps.
     */
    val showsDock: Boolean = true
)

/**
 * The drawer: every app not [hidden], in alphabetical order of the language ("Árvore" next to "Arroz"), filtered by
 * [query] without caring about case or accents ("musica" finds "Música").
 */
fun drawerApps(apps: List<LauncherApp>, hidden: Set<String>, query: String, locale: Locale): List<LauncherApp> {
    val wanted = query.searchKey()
    val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
    return apps.filter { it.key !in hidden && (wanted.isEmpty() || wanted in it.label.searchKey()) }
        .sortedWith { first, second -> collator.compare(first.label, second.label) }
}

private val accents = Regex("\\p{Mn}+")

/** Whether [query] is in this text, without caring about case or accents ("musica" finds "Música"). */
fun String.matchesSearch(query: String): Boolean {
    val wanted = query.searchKey()
    return wanted.isEmpty() || wanted in searchKey()
}

private fun String.searchKey(): String =
    Normalizer.normalize(trim().lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(accents, "")

/** The home screen takes no new item: [MAX_HOME_ITEMS] at most, over all its pages. */
val LauncherSettings.isHomeFull: Boolean get() = favorites.size >= MAX_HOME_ITEMS

/** [key] at the end of the home screen, unless it is there already or the home screen is full. */
fun LauncherSettings.withAddedToHome(key: String): LauncherSettings =
    if (key in favorites || isHomeFull) this else copy(favorites = favorites + key)

/**
 * Adds an app (or a widget) to the end of the home screen, or takes it out (also out of its folder); a full home screen
 * takes no more.
 */
fun LauncherSettings.toggleFavorite(key: String): LauncherSettings =
    if (key in favorites || key in dock || folderOf(key) != null) removedFromHome(key) else withAddedToHome(key)

/**
 * Moves the favorite [key] to [toIndex] of the favorites shown ([shown], the ones still installed, in order), as a
 * drag or the screen reader does. An app that was removed keeps its place among the stored favorites, so it comes
 * back where it was if it is installed again. An index outside the list or an unknown key changes nothing.
 */
fun LauncherSettings.moveFavorite(shown: List<String>, key: String, toIndex: Int): LauncherSettings {
    val from = shown.indexOf(key)
    val visible = shown.toSet()
    // A list read before the favorites changed (an app pinned meanwhile) would put keys in the wrong slots.
    val isCurrent = favorites.filter { it in visible } == shown
    val canMove = from >= 0 && toIndex in shown.indices && from != toIndex
    if (!isCurrent || !canMove) return this
    val reordered = shown.toMutableList().apply { add(toIndex, removeAt(from)) }.iterator()
    return copy(favorites = favorites.map { if (it in visible) reordered.next() else it })
}

/** Hides an app from the drawer (and from the home screen), or shows it again. */
/**
 * The apps hidden as chosen in "Ocultar apps" ([chosen]): each newly hidden one leaves the home screen (as [toggleHidden]
 * does), and the ones left out come back to all the apps.
 */
fun LauncherSettings.withHiddenApps(chosen: Set<String>): LauncherSettings {
    val newlyHidden = chosen - hidden
    return newlyHidden.fold(copy(hidden = hidden.intersect(chosen))) { settings, key -> settings.toggleHidden(key) }
}

fun LauncherSettings.toggleHidden(key: String): LauncherSettings =
    if (key in hidden) copy(hidden = hidden - key) else removedFromHome(key).copy(hidden = hidden + key)

/**
 * Takes an item off the home screen, the dock and its folder, with its cells (upright and turned). A folder taken off
 * takes its apps off the home screen; they stay in all the apps.
 */
fun LauncherSettings.removedFromHome(key: String): LauncherSettings = if (isLayoutLocked) {
    this
} else {
    leavingHome(key).let { left -> folderIdOf(key)?.let { left.copy(folders = left.folders - it) } ?: left }
}

/** The clock widget goes to the start of the home screen once, the first time ORI Home opens with it. */
fun LauncherSettings.withClockSetUp(): LauncherSettings =
    if (isClockSetUp) this else copy(favorites = listOf(CLOCK_KEY) + (favorites - CLOCK_KEY), isClockSetUp = true)

/** The choices of ORI Home on this device. */
interface LauncherSettingsRepository {
    val settings: Flow<LauncherSettings>

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings)
}

/**
 * The apps with an icon in the drawer, followed as they are installed, updated or removed. Only apps that declare a
 * launcher entry are visible (Android package visibility), with no special permission.
 */
/** Where an app is opened from on the screen (its icon), in pixels: Android grows the app out of it, as on One UI. */
data class AppOrigin(val left: Int, val top: Int, val width: Int, val height: Int)

interface InstalledAppsRepository {
    fun observeApps(): Flow<List<LauncherApp>>

    /** The package of each app installed while ORI Home runs (not an update), to put it on the home screen. */
    fun observeAddedPackages(): Flow<String>

    /**
     * Opens the app, growing out of [from] when it is given; false when the app is gone or cannot be opened. It asks
     * Android where the app is, so it is called off the main thread's work (it is a suspend call).
     */
    suspend fun open(app: LauncherApp, from: AppOrigin? = null): Boolean

    /** Android's page about the app (storage, permissions, uninstall); false when it cannot be opened. */
    fun openDetails(app: LauncherApp): Boolean

    /** Asks Android to uninstall the app; Android asks the person first. False when it cannot be asked. */
    fun uninstall(app: LauncherApp): Boolean

    /**
     * The shortcuts of [app] for its menu, at most [MAX_MENU_SHORTCUTS], read off the main thread; none when Android
     * does not let ORI Home read them (it is not the home app in use) or the app has none.
     */
    suspend fun shortcutsOf(app: LauncherApp): List<LauncherApp>

    /** The shortcuts of [keys] that still exist, read again when an app changes them (renamed, removed). */
    fun observeShortcuts(keys: Set<String>): Flow<List<LauncherApp>>

    /**
     * Tells Android which shortcuts are on the home screen ([kept]) and which just left it ([released]), so their apps
     * keep the first and may drop the others.
     */
    suspend fun keepPinned(kept: Set<String>, released: Set<String>)
}

/**
 * Whether ORI is the home app, and the official way to change it. Android decides: the app only asks
 * (RoleManager, or the system "default apps" screen), never forces, and never blocks leaving.
 */
interface HomeActivation {
    /** True when ORI Home answers the Home button now. */
    fun isOriHome(): Boolean

    /** The package of the home app in use, to go back to it later; null when Android has no default. */
    fun currentHomePackage(): String?

    /** The home entry exists only while the person wants it, so ORI never shows up as a home app unasked. */
    fun setLauncherEnabled(isEnabled: Boolean)
}
