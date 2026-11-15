package com.oriteam.ori.domain.launcher

/** Where a chosen wallpaper goes, as One UI asks: the home screen, the lock screen, or both. */
enum class WallpaperTarget { HOME, LOCK, BOTH }

/**
 * A wallpaper ORI put on the lock screen, and the number Android gave that picture ([androidId]). Android does not let
 * an app see the picture of the lock screen; while its number is still this one, ORI knows the lock screen shows
 * [wallpaper], and the previews draw it.
 */
data class LockScreenChoice(val wallpaper: LauncherWallpaper, val androidId: Int)

/** The number Android gives a lock screen with no picture of its own: it shows the Android home wallpaper. */
const val LOCK_SHOWS_HOME = -1

/**
 * The wallpaper of the lock screen belongs to Android: ORI only hands it a picture when the person asks, and keeps
 * which one it was (to draw it in the previews). Android may not allow it (a device policy, a device without
 * wallpapers).
 */
interface LockScreenWallpaper {
    fun isAvailable(): Boolean

    /** A photo of the history (its reduced copy): the number Android gave it; null when refused or the file is gone. */
    suspend fun setPhoto(fileName: String): Int?

    /** A gradient from [topArgb] to [bottomArgb] (the same color twice is a plain color): its number, or null. */
    suspend fun setColors(topArgb: Int, bottomArgb: Int): Int?

    /**
     * The lock screen shows the wallpaper of Android's home screen again (its own picture is taken out), as when the
     * Android wallpaper is chosen for the lock screen. False when Android refused.
     */
    suspend fun useHomeWallpaper(): Boolean

    /**
     * The number of the picture the lock screen shows now: negative when it has none of its own (it shows the home
     * one), null when Android does not say.
     */
    suspend fun shownId(): Int?
}

/**
 * What the lock screen shows, for the previews, when Android says its picture is number [androidId]: the Android home
 * wallpaper when it has no picture of its own, the one ORI put there while it is still there, or null: a picture put
 * there by something else (or a number Android did not give), which ORI cannot see.
 */
fun LauncherSettings.lockScreenShown(androidId: Int?): LauncherWallpaper? = when {
    androidId == null -> null
    androidId < 0 -> LauncherWallpaper.System
    lockWallpaper?.androidId == androidId -> lockWallpaper.wallpaper
    else -> null
}

/**
 * [wallpaper] went to the lock screen, where Android numbered it [androidId] ([LOCK_SHOWS_HOME] for the Android
 * wallpaper, which is Android's own and not kept). A photo goes to the start of the history, as on the home screen,
 * and its file stays while the lock screen shows it; the photo the lock screen showed before goes when nothing else
 * uses it.
 */
fun LauncherSettings.withLockScreen(wallpaper: LauncherWallpaper, androidId: Int): WallpaperChange {
    val choice = LockScreenChoice(wallpaper, androidId).takeUnless { wallpaper == LauncherWallpaper.System }
    val before = (lockWallpaper?.wallpaper as? LauncherWallpaper.Photo)?.fileName
    val photos = (listOfNotNull((wallpaper as? LauncherWallpaper.Photo)?.fileName) + wallpaperHistory).distinct()
    val history = photos.take(MAX_WALLPAPER_HISTORY)
    val next = copy(lockWallpaper = choice, wallpaperHistory = history)
    val known = (photos + listOfNotNull(before)).distinct()
    return WallpaperChange(next, unusedFiles = known - history.toSet() - next.photosInUse)
}
