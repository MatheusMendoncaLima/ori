package com.oriteam.ori.domain.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What ORI knows of the lock screen: the wallpaper it put there, while Android still shows that picture, and the
 * photo files kept for it (never deleted while a screen shows them).
 */
class LockScreenTest {
    private val sea = LauncherWallpaper.Gradient(WallpaperGradient.SEA)

    @Test
    fun `the lock screen shows what ORI put there only while Android still shows that picture`() {
        val settings = LauncherSettings().withLockScreen(sea, androidId = 7).settings

        assertEquals(sea, settings.lockScreenShown(7))
        // Another number: a picture put there by the settings of Android, which ORI cannot see.
        assertNull(settings.lockScreenShown(8))
        // No picture of its own: the lock screen shows the Android home wallpaper.
        assertEquals(LauncherWallpaper.System, settings.lockScreenShown(LOCK_SHOWS_HOME))
        // Android did not say.
        assertNull(settings.lockScreenShown(null))
    }

    @Test
    fun `the Android wallpaper on the lock screen keeps nothing of ORI`() {
        val withSea = LauncherSettings().withLockScreen(sea, androidId = 7).settings

        val change = withSea.withLockScreen(LauncherWallpaper.System, LOCK_SHOWS_HOME)

        assertNull(change.settings.lockWallpaper)
        assertEquals(emptyList<String>(), change.unusedFiles)
    }

    @Test
    fun `a photo on the lock screen goes to the history and its file stays while the lock screen shows it`() {
        val start = LauncherSettings(wallpaperHistory = List(MAX_WALLPAPER_HISTORY) { "p$it" })

        val onLock = start.withLockScreen(LauncherWallpaper.Photo("lock"), androidId = 3)
        assertEquals("lock", onLock.settings.wallpaperHistory.first())
        assertEquals(listOf("p${MAX_WALLPAPER_HISTORY - 1}"), onLock.unusedFiles)

        // Eight new home photos push it out of the history, but the lock screen still shows it: its file stays.
        var settings = onLock.settings
        repeat(MAX_WALLPAPER_HISTORY) { index ->
            val change = settings.withWallpaper(LauncherWallpaper.Photo("home$index"))
            assertTrue("lock deleted at $index", "lock" !in change.unusedFiles)
            settings = change.settings
        }
        assertTrue("lock" !in settings.wallpaperHistory)
        assertTrue("lock" in settings.photosInUse)

        // Another picture on the lock screen: now nothing uses it, and it goes.
        assertEquals(listOf("lock"), settings.withLockScreen(sea, androidId = 4).unusedFiles)
    }

    @Test
    fun `a photo the lock screen shows is not taken out of the history`() {
        val settings = LauncherSettings(wallpaperHistory = listOf("home", "lock"))
            .withLockScreen(LauncherWallpaper.Photo("lock"), androidId = 3).settings

        assertEquals(settings, settings.withoutHistoryPhoto("lock").settings)
        assertEquals(emptyList<String>(), settings.withoutHistoryPhoto("lock").unusedFiles)
        assertEquals(listOf("home"), settings.withoutHistoryPhoto("home").unusedFiles)
    }
}
