package com.oriteam.ori.data.launcher

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import android.util.LruCache
import android.view.View
import com.oriteam.ori.domain.launcher.AppOrigin
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.domain.launcher.InstalledAppsRepository
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.isShortcutKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InstalledApps"
private const val ICON_CACHE_SIZE = 120
private const val ICON_SIZE_PX = 144

/**
 * The apps of every profile (the work profile included), read with [LauncherApps], which a launcher may use without
 * QUERY_ALL_PACKAGES. The list is read again on each change Android reports (installed, updated, removed,
 * profile locked), off the main thread. Nothing of it leaves the device.
 */
@Singleton
class AndroidInstalledApps @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shortcuts: AndroidShortcuts
) : InstalledAppsRepository {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val users = context.getSystemService(UserManager::class.java)
    private val icons = LruCache<String, Bitmap>(ICON_CACHE_SIZE)

    /**
     * Android reports changes on a thread with a Looper (here the main one, which only sends a signal); the list is
     * then read again off the main thread. Registering from a background thread without a Handler crashed ORI Home
     * on every opening (see AndroidInstalledAppsTest).
     */
    override fun observeApps(): Flow<List<LauncherApp>> =
        changes(withShortcuts = false).map { readApps() }.flowOn(Dispatchers.IO)

    /** Read again when an app changes its shortcuts (a renamed contact) or is removed; none for no keys. */
    override fun observeShortcuts(keys: Set<String>): Flow<List<LauncherApp>> = if (keys.isEmpty()) {
        flowOf(emptyList())
    } else {
        changes(withShortcuts = true).map { shortcuts.shortcutsOf(keys) }.flowOn(Dispatchers.IO)
    }

    /** A signal now and at each change Android reports (with [withShortcuts], also of the shortcuts), conflated. */
    private fun changes(withShortcuts: Boolean): Flow<Unit> = callbackFlow {
        val callback = AppsChanged(withShortcuts) { trySend(Unit) }
        trySend(Unit)
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }.conflate()

    override suspend fun shortcutsOf(app: LauncherApp): List<LauncherApp> = withContext(Dispatchers.IO) {
        activityOf(app.key)?.takeUnless { isShortcutKey(app.key) }
            ?.let { shortcuts.menuShortcutsOf(it.componentName, it.user) }
            .orEmpty()
    }

    override suspend fun keepPinned(kept: Set<String>, released: Set<String>) =
        withContext(Dispatchers.IO) { shortcuts.keepPinned(kept, released) }

    /** Each package Android reports as installed (an update is reported as changed, not added). */
    override fun observeAddedPackages(): Flow<String> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(packageName)
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) = Unit
            override fun onPackageChanged(packageName: String, user: UserHandle) = Unit
            override fun onPackagesAvailable(packages: Array<out String>, user: UserHandle, replacing: Boolean) = Unit
            override fun onPackagesUnavailable(packages: Array<out String>, user: UserHandle, replacing: Boolean) =
                Unit
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    override suspend fun open(app: LauncherApp, from: AppOrigin?): Boolean {
        if (isShortcutKey(app.key)) {
            val options = from?.let(::growingOutOf)
            return withContext(Dispatchers.IO) { shortcuts.start(app.key, from?.toRect(), options) }
        }
        // Asking Android where the app is takes a call to the system: never on the main thread.
        val info = withContext(Dispatchers.IO) { activityOf(app.key) } ?: return false
        return try {
            launcherApps.startMainActivity(info.componentName, info.user, from?.toRect(), from?.let(::growingOutOf))
            true
        } catch (exception: SecurityException) {
            Log.w(TAG, "Could not open an app: ${exception.javaClass.simpleName}")
            false
        } catch (exception: android.content.ActivityNotFoundException) {
            Log.w(TAG, "Could not open an app: ${exception.javaClass.simpleName}")
            false
        }
    }

    override fun openDetails(app: LauncherApp): Boolean {
        val info = activityOf(app.key) ?: return false
        return try {
            launcherApps.startAppDetailsActivity(info.componentName, info.user, null, null)
            true
        } catch (exception: SecurityException) {
            Log.w(TAG, "Could not open the app details: ${exception.javaClass.simpleName}")
            false
        } catch (exception: android.content.ActivityNotFoundException) {
            Log.w(TAG, "Could not open the app details: ${exception.javaClass.simpleName}")
            false
        }
    }

    override fun uninstall(app: LauncherApp): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (exception: android.content.ActivityNotFoundException) {
        Log.w(TAG, "Could not ask to uninstall: ${exception.javaClass.simpleName}")
        false
    }

    /**
     * The app grows out of its icon, as on One UI. Android reads where the source view is on the screen and adds the
     * bounds to it; a view in no window is at 0,0, so the bounds given are the icon's on the screen (ORI Home keeps no
     * view of the screen here).
     */
    private fun growingOutOf(from: AppOrigin): Bundle =
        ActivityOptions.makeScaleUpAnimation(View(context), from.left, from.top, from.width, from.height).toBundle()

    private fun AppOrigin.toRect() = Rect(left, top, left + width, top + height)

    /** The icon already in memory, or null: read at once, so an icon seen before shows from the first frame. */
    fun cachedIconOf(key: String, shape: IconShape): Bitmap? = icons.get(cacheKeyOf(key, shape))

    /**
     * The icon in [shape] (the same for every app: IconShaping.kt), with the badge of its profile, reduced and kept in
     * memory; read off the main thread. Null when the app is gone.
     */
    suspend fun iconOf(key: String, shape: IconShape): Bitmap? =
        icons.get(cacheKeyOf(key, shape)) ?: withContext(Dispatchers.IO) {
            val icon = if (isShortcutKey(key)) {
                shortcuts.iconOf(key, shape, ICON_SIZE_PX)
            } else {
                activityOf(key)?.let { info ->
                    context.badgedFor(info.user, info.getIcon(0).inShape(shape, ICON_SIZE_PX))
                }
            }
            icon?.also { icons.put(cacheKeyOf(key, shape), it) }
        }

    private fun cacheKeyOf(key: String, shape: IconShape) = "${shape.name}|$key"

    private fun readApps(): List<LauncherApp> = launcherApps.profiles.flatMap { user ->
        launcherApps.getActivityList(null, user)
            .filter { it.componentName.packageName != context.packageName }
            .map { info ->
                LauncherApp(keyOf(info), info.componentName.packageName, info.label.toString(), isRemovable(info))
            }
    }

    /** An app installed by the person, or an update of a system app (which can be taken back). */
    private fun isRemovable(info: LauncherActivityInfo): Boolean {
        val flags = info.applicationInfo.flags
        return flags and ApplicationInfo.FLAG_SYSTEM == 0 || flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    }

    private fun keyOf(info: LauncherActivityInfo) =
        "${info.componentName.flattenToShortString()}@${users.getSerialNumberForUser(info.user)}"

    /** The activity of an app key; for a shortcut, the main activity of its app (for "Informações do app"). */
    private fun activityOf(key: String): LauncherActivityInfo? = shortcuts.ownerOf(key)
        ?.let { (packageName, user) -> launcherApps.getActivityList(packageName, user).firstOrNull() }
        ?: appActivityOf(key)

    private fun appActivityOf(key: String): LauncherActivityInfo? {
        val component = ComponentName.unflattenFromString(key.substringBefore('@')) ?: return null
        val serial = key.substringAfter('@', "").toLongOrNull() ?: return null
        val user = users.getUserForSerialNumber(serial) ?: return null
        return launcherApps.getActivityList(component.packageName, user).firstOrNull { it.componentName == component }
    }
}

/**
 * Every change Android reports about the apps (installed, updated, removed, a profile locked) as a signal; with
 * [withShortcuts], also the changes of their shortcuts, which only the shortcuts on the home screen need (they change
 * often, as a messaging app updates its recent chats).
 */
private class AppsChanged(
    private val withShortcuts: Boolean,
    private val signal: () -> Unit
) : LauncherApps.Callback() {
    override fun onPackageRemoved(packageName: String, user: UserHandle) = signal()
    override fun onPackageAdded(packageName: String, user: UserHandle) = signal()
    override fun onPackageChanged(packageName: String, user: UserHandle) = signal()
    override fun onPackagesAvailable(packages: Array<out String>, user: UserHandle, replacing: Boolean) = signal()
    override fun onPackagesUnavailable(packages: Array<out String>, user: UserHandle, replacing: Boolean) = signal()
    override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<ShortcutInfo>, user: UserHandle) {
        if (withShortcuts) signal()
    }
}
