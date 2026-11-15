package com.oriteam.ori.domain.launcher

/**
 * How big the icons of the home screen are. The screen decides the size in dp and how many fit in a row; the
 * phone keeps at least two in a row even at the largest size. [MAXIMUM] turns the apps into large tiles that fill the
 * screen, with the name inside (as the tiles of Windows Phone, made for today), for whoever has much difficulty with
 * the hands.
 */
enum class LauncherIconSize { SMALL, MEDIUM, LARGE, EXTRA_LARGE, MAXIMUM }

/**
 * The shape every icon gets, whatever shape its app brings: round, a square with rounded corners (One UI's, the usual
 * one), or a square with straight corners.
 */
enum class IconShape { ROUND, ROUNDED, SQUARE }

/** How all the apps show: a list that scrolls down, or pages side by side, as the home screen. */
enum class DrawerStyle { SCROLL, PAGES }

/**
 * What ORI Home has: the home screen and all the apps (as any home screen), or only all the apps, opened at once, for
 * whoever finds the pages, the dock and the widgets too much. The items of the home screen are kept for when the
 * person goes back.
 */
enum class HomeScreenMode { HOME_AND_APPS, APPS_ONLY }

/** The calm colors a background may have, the same soft ones of the rest of ORI, in light and dark. */
enum class WallpaperColor { MINT, BLUE, LILAC, YELLOW, PINK }

/** Soft gradients made by ORI (drawn, not pictures), in light and dark. */
enum class WallpaperGradient { DAWN, SEA, MEADOW, DUSK, SAND }

/** What the panel to the left of the home screen shows; [NONE] leaves the home screen without it. */
enum class LauncherPanel { ROUTINE, PHRASES, NONE }

/** What is behind the icons of the home screen. */
sealed interface LauncherWallpaper {
    /** The wallpaper chosen in Android, as on any home screen; ORI does not read it, Android draws it. */
    data object System : LauncherWallpaper

    /** The background of the theme, as the rest of ORI. */
    data object Theme : LauncherWallpaper

    data class Solid(val color: WallpaperColor) : LauncherWallpaper

    data class Gradient(val gradient: WallpaperGradient) : LauncherWallpaper

    /** A reduced copy of a picture the person chose, kept in the app's private storage. */
    data class Photo(val fileName: String) : LauncherWallpaper
}

/** Result of copying a picture for the background. */
sealed interface WallpaperImport {
    data class Imported(val fileName: String) : WallpaperImport

    /** The picture could not be read (not an image, access lost) or could not be written. */
    data object Failed : WallpaperImport
}

/** The background pictures of ORI Home. They stay on this device and are never sent anywhere. */
interface WallpaperStore {
    /** Copies a reduced copy of the picture at [sourceUri] (from the system photo picker). */
    suspend fun importPhoto(sourceUri: String): WallpaperImport

    /** Deletes a picture that is no longer the background; a file already gone is fine. */
    suspend fun delete(fileName: String)
}

/** The photos kept to choose again as wallpaper: more would only fill the device. */
const val MAX_WALLPAPER_HISTORY = 8

/**
 * [chosen] becomes the wallpaper. A photo goes to the start of the history, and the photo it replaces stays in it;
 * the photos that fall off the end are the files to delete (never the one in use).
 */
fun LauncherSettings.withWallpaper(chosen: LauncherWallpaper): WallpaperChange {
    val photos = listOfNotNull(
        (chosen as? LauncherWallpaper.Photo)?.fileName,
        (wallpaper as? LauncherWallpaper.Photo)?.fileName
    ) + wallpaperHistory
    val history = photos.distinct().take(MAX_WALLPAPER_HISTORY)
    val next = copy(wallpaper = chosen, wallpaperHistory = history)
    return WallpaperChange(next, unusedFiles = photos.distinct() - history.toSet() - next.photosInUse)
}

/** The photos the screens show now (the home screen's and the lock screen's): their files are never deleted. */
val LauncherSettings.photosInUse: Set<String>
    get() = setOfNotNull(
        (wallpaper as? LauncherWallpaper.Photo)?.fileName,
        (lockWallpaper?.wallpaper as? LauncherWallpaper.Photo)?.fileName
    )

/** A photo taken out of the history: its file goes too. A photo in use (home or lock screen) is never taken out. */
fun LauncherSettings.withoutHistoryPhoto(fileName: String): WallpaperChange {
    if (fileName in photosInUse || fileName !in wallpaperHistory) return WallpaperChange(this, emptyList())
    return WallpaperChange(copy(wallpaperHistory = wallpaperHistory - fileName), unusedFiles = listOf(fileName))
}

/** The settings after a change of wallpaper, and the photo files nothing uses any more. */
data class WallpaperChange(val settings: LauncherSettings, val unusedFiles: List<String>)

/** A photo kept in the history without becoming the home wallpaper (it went only to the lock screen). */
fun LauncherSettings.withPhotoRemembered(fileName: String): WallpaperChange {
    val photos = (listOf(fileName) + wallpaperHistory).distinct()
    val history = photos.take(MAX_WALLPAPER_HISTORY)
    return WallpaperChange(copy(wallpaperHistory = history), unusedFiles = photos - history.toSet() - photosInUse)
}
