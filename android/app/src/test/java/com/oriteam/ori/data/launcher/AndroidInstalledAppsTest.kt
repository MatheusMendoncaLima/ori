package com.oriteam.ori.data.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.SHORTCUT_PREFIX
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The real list of apps, as the launcher reads it. It once registered the change callback from a background thread
 * without a Looper, which crashed ORI Home on every opening on the phone (the home screen blinked).
 */
@RunWith(AndroidJUnit4::class)
class AndroidInstalledAppsTest {
    @Test
    fun `the list of apps is followed without crashing, from a background thread`() = runBlocking {
        val repository = repository()

        val apps = withTimeout(5_000) { repository.observeApps().first() }

        assertTrue(apps.none { it.packageName == "com.oriteam.ori" })
    }

    @Test
    fun `without being the home app in use, shortcuts are simply none, and nothing fails`() = runBlocking {
        val repository = repository()
        val app = LauncherApp("app.notes/.Main@0", "app.notes", "Notas")
        val kept = setOf("${SHORTCUT_PREFIX}0/app.notes/bm92YQ", "${SHORTCUT_PREFIX}broken")

        assertTrue(repository.shortcutsOf(app).isEmpty())
        assertTrue(withTimeout(5_000) { repository.observeShortcuts(kept).first() }.isEmpty())
        assertTrue(withTimeout(5_000) { repository.observeShortcuts(emptySet()).first() }.isEmpty())
        repository.keepPinned(kept = kept, released = emptySet())
        assertFalse(repository.open(LauncherApp(kept.first(), "app.notes", "Nova")))
    }

    private fun repository(): AndroidInstalledApps {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return AndroidInstalledApps(context, AndroidShortcuts(context))
    }
}
