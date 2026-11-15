package com.oriteam.ori.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.util.Base64
import android.util.Log
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.MAX_MENU_SHORTCUTS
import com.oriteam.ori.domain.launcher.SHORTCUT_PREFIX
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppShortcuts"
private const val KEY_PARTS = 3
private const val KEY_SEPARATOR = "/"
private const val ID_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

/** The shortcuts a menu offers: the ones the app declares and the ones it added while used (Android's own choice). */
private const val MENU_FLAGS = ShortcutQuery.FLAG_MATCH_MANIFEST or ShortcutQuery.FLAG_MATCH_DYNAMIC

/** A shortcut on the home screen may be pinned, declared or dynamic: whichever it is, it is found. */
private const val ANY_FLAGS = MENU_FLAGS or ShortcutQuery.FLAG_MATCH_PINNED

/** Where a shortcut is: its profile, its app and its id (the id an app gives it, any text). */
private data class ShortcutPlace(val user: UserHandle, val packageName: String, val id: String)

/**
 * The shortcuts of the apps, read with [LauncherApps] (which only the home app in use may do: otherwise every call
 * answers nothing). Its key is "shortcut:<profile>/<package>/<id>", the id encoded so any text an app uses stays one
 * plain word in the stored lines. Nothing of it leaves the device; the home screen keeps only the key.
 */
@Singleton
class AndroidShortcuts @Inject constructor(@ApplicationContext private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val users = context.getSystemService(UserManager::class.java)

    /** The shortcuts of the app of [activity] in [user], for its menu, in the order the app gives them. */
    fun menuShortcutsOf(activity: ComponentName, user: UserHandle): List<LauncherApp> {
        val query = ShortcutQuery().setPackage(activity.packageName).setActivity(activity).setQueryFlags(MENU_FLAGS)
        return read(query, user)
            .filter { it.isEnabled }
            // As Android's own home screen: the declared ones first, each kind in the app's order.
            .sortedWith(compareBy<ShortcutInfo>({ !it.isDeclaredInManifest }, { it.rank }))
            .take(MAX_MENU_SHORTCUTS)
            .map(::appOf)
    }

    /** The shortcuts of [keys] that still exist, with their names of now. */
    fun shortcutsOf(keys: Set<String>): List<LauncherApp> = keys.mapNotNull(::placeOf)
        .groupBy { it.user to it.packageName }
        .flatMap { (owner, places) ->
            val query = ShortcutQuery().setPackage(owner.second).setShortcutIds(places.map { it.id })
                .setQueryFlags(ANY_FLAGS)
            read(query, owner.first).filter { it.isEnabled }.map(::appOf)
        }

    /**
     * Pins, for each app with a shortcut in [kept] or [released], exactly its shortcuts in [kept]: Android replaces the
     * pinned list of an app at each call, so the ones taken off the home screen are let go.
     */
    fun keepPinned(kept: Set<String>, released: Set<String>) {
        val keptPlaces = kept.mapNotNull(::placeOf)
        (keptPlaces + released.mapNotNull(::placeOf)).map { it.user to it.packageName }.toSet().forEach { owner ->
            val ids = keptPlaces.filter { it.user == owner.first && it.packageName == owner.second }.map { it.id }
            guarded { launcherApps.pinShortcuts(owner.second, ids, owner.first) }
        }
    }

    /** Opens the shortcut [key], growing out of [bounds] when given; false when it is gone or disabled. */
    fun start(key: String, bounds: Rect?, options: Bundle?): Boolean {
        val place = placeOf(key) ?: return false
        return guarded {
            launcherApps.startShortcut(place.packageName, place.id, bounds, options, place.user)
            true
        } ?: false
    }

    /**
     * The picture of the shortcut [key] in [shape], as every icon of the home screen, with the badge of its profile, at
     * [sizePx]; null when it has none.
     */
    fun iconOf(key: String, shape: IconShape, sizePx: Int): Bitmap? {
        val info = infoOf(key) ?: return null
        val density = context.resources.displayMetrics.densityDpi
        val picture = guarded { launcherApps.getShortcutIconDrawable(info, density) } ?: return null
        return context.badgedFor(info.userHandle, picture.inShape(shape, sizePx))
    }

    /**
     * Whether Android pinned the shortcut [key] for ORI Home: after a request accepted, it is, unless the request was
     * not Android's own (another app imitating one), which then adds nothing.
     */
    fun isPinned(key: String): Boolean {
        val place = placeOf(key) ?: return false
        val query = ShortcutQuery().setPackage(place.packageName).setShortcutIds(listOf(place.id))
            .setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED)
        return read(query, place.user).isNotEmpty()
    }

    /** The app a shortcut [key] belongs to, and its profile: for "Informações do app". */
    fun ownerOf(key: String): Pair<String, UserHandle>? = placeOf(key)?.let { it.packageName to it.user }

    fun keyOf(info: ShortcutInfo): String {
        val serial = users.getSerialNumberForUser(info.userHandle)
        val id = Base64.encodeToString(info.id.toByteArray(Charsets.UTF_8), ID_FLAGS)
        return SHORTCUT_PREFIX + listOf(serial, info.`package`, id).joinToString(KEY_SEPARATOR)
    }

    private fun appOf(info: ShortcutInfo): LauncherApp = LauncherApp(
        key = keyOf(info),
        packageName = info.`package`,
        label = (info.shortLabel ?: info.longLabel ?: "").toString(),
        // Taken off the home screen, not uninstalled: it is a part of its app.
        isRemovable = false
    )

    private fun infoOf(key: String): ShortcutInfo? {
        val place = placeOf(key) ?: return null
        val query = ShortcutQuery().setPackage(place.packageName).setShortcutIds(listOf(place.id))
            .setQueryFlags(ANY_FLAGS)
        return read(query, place.user).firstOrNull()
    }

    private fun placeOf(key: String): ShortcutPlace? {
        val parts = key.takeIf { it.startsWith(SHORTCUT_PREFIX) }
            ?.removePrefix(SHORTCUT_PREFIX)
            ?.split(KEY_SEPARATOR)
            ?.takeIf { it.size == KEY_PARTS }
            ?: return null
        val user = parts[0].toLongOrNull()?.let(users::getUserForSerialNumber)
        // A key written by hand or broken: Base64 refuses it, and it is simply not a shortcut.
        val id = runCatching { String(Base64.decode(parts[2], ID_FLAGS), Charsets.UTF_8) }.getOrNull()
        return if (user == null || id == null) null else ShortcutPlace(user, parts[1], id)
    }

    private fun read(query: ShortcutQuery, user: UserHandle): List<ShortcutInfo> =
        guarded { launcherApps.getShortcuts(query, user) }.orEmpty()

    /**
     * Android refuses shortcuts to an app that is not the home app in use (SecurityException), and to a profile that
     * is locked (IllegalStateException): then there are simply none. Logged without names or ids.
     */
    private fun <T> guarded(call: () -> T?): T? = try {
        if (launcherApps.hasShortcutHostPermission()) call() else null
    } catch (exception: SecurityException) {
        Log.w(TAG, "Shortcuts not available: ${exception.javaClass.simpleName}")
        null
    } catch (exception: IllegalStateException) {
        Log.w(TAG, "Shortcuts not available: ${exception.javaClass.simpleName}")
        null
    } catch (exception: android.content.ActivityNotFoundException) {
        Log.w(TAG, "Shortcut could not open: ${exception.javaClass.simpleName}")
        null
    }
}
