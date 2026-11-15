package com.oriteam.ori.data.launcher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.BadParcelableException
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.util.Log
import android.util.LruCache
import android.util.SizeF
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.HomeWidgetHost
import com.oriteam.ori.domain.launcher.WidgetProvider
import com.oriteam.ori.domain.launcher.WidgetProviders
import com.oriteam.ori.domain.launcher.WidgetResize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OriWidgets"

/** The number ORI Home uses with Android for its widgets; it must never change, or the widgets are lost. */
private const val HOST_ID = 4_107

/** Android's rule of thumb for old widgets: n cells take 70 × n − 30 dp. */
private const val LEGACY_CELL_DP = 70
private const val LEGACY_CELL_GAP_DP = 30
private const val PREVIEW_SIDE_PX = 480

/** The pictures of the widgets kept in memory, at most this many bytes (a few dozen pictures). */
private const val PREVIEW_CACHE_BYTES = 8 * 1024 * 1024

/**
 * The widgets of ORI Home, through Android's [AppWidgetHost]. Listening starts and stops with the home screen
 * ([setListening]), so nothing updates while it is not shown. No permission is needed: Android
 * asks the person, in its own window, before a widget is bound to ORI.
 */
@Singleton
class AndroidWidgetHost @Inject constructor(
    @ApplicationContext private val context: Context
) : HomeWidgetHost {
    internal val manager = AppWidgetManager.getInstance(context)
    private val users = context.getSystemService(UserManager::class.java)
    internal val host = AppWidgetHost(context, HOST_ID)
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val known = KnownWidgets<AppWidgetProviderInfo> { idOf(it) }
    private val previews = object : LruCache<String, Bitmap>(PREVIEW_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /**
     * All the widgets at once; when Android cannot send them in one answer (with many widgets the list is too big for
     * the system's message buffer, and the call fails), app by app, skipping an app that fails. ORI Home never closes
     * for it: the list says what is missing.
     */
    override suspend fun providers(): WidgetProviders = withContext(Dispatchers.IO) {
        var isComplete = true
        val infos = users.userProfiles.flatMap { user ->
            readOrNull { manager.getInstalledProvidersForProfile(user) } ?: packagesWithIcons(user).flatMap { name ->
                readOrNull { manager.getInstalledProvidersForPackage(name, user) }
                    ?: emptyList<AppWidgetProviderInfo>().also { isComplete = false }
            }
        }
        known.remember(infos)
        val clock = clockPackage()
        val providers = infos.map { providerOf(it, clock) }
            .sortedWith(compareBy({ !it.isClock }, { it.appLabel.lowercase() }, { it.label.lowercase() }))
        WidgetProviders(providers, isComplete)
    }

    private fun providerOf(info: AppWidgetProviderInfo, clock: String?): WidgetProvider {
        val packages = context.packageManager
        val appLabel = runCatching {
            packages.getApplicationLabel(packages.getApplicationInfo(info.provider.packageName, 0)).toString()
        }.getOrDefault(info.provider.packageName)
        return WidgetProvider(
            id = idOf(info),
            packageName = info.provider.packageName,
            appLabel = appLabel,
            label = info.loadLabel(packages),
            span = spanOf(info, context.resources.displayMetrics.density),
            needsSetup = needsSetupOf(info),
            isClock = info.provider.packageName == clock,
            description = descriptionOf(info)
        )
    }

    private fun descriptionOf(info: AppWidgetProviderInfo): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { info.loadDescription(context)?.toString() }.getOrNull()
        } else {
            null
        }

    /** The apps with an icon of [user]: the ones whose widgets are asked one by one. */
    private fun packagesWithIcons(user: UserHandle): List<String> =
        readOrNull { launcherApps.getActivityList(null, user).map { it.componentName.packageName }.distinct() }
            .orEmpty()

    /** A list asked to Android, or null when Android could not send it (it is written in the log, without details). */
    private inline fun <T> readOrNull(read: () -> T): T? = try {
        read()
    } catch (exception: BadParcelableException) {
        Log.w(TAG, "Android could not send the widgets: ${exception.javaClass.simpleName}")
        null
    } catch (exception: SecurityException) {
        Log.w(TAG, "The widgets were refused: ${exception.javaClass.simpleName}")
        null
    } catch (exception: IllegalArgumentException) {
        Log.w(TAG, "An app of the widgets is gone: ${exception.javaClass.simpleName}")
        null
    }

    override fun allocate(): Int = host.allocateAppWidgetId()

    /** The clock app of the device: the one that shows the alarms (Samsung's, Google's or another). */
    private fun clockPackage(): String? = context.packageManager
        .resolveActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS), PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName

    override fun bindIfAllowed(widgetId: Int, providerId: String): Boolean {
        val info = providerOf(providerId) ?: return false
        return try {
            manager.bindAppWidgetIdIfAllowed(widgetId, info.profile, info.provider, null)
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "Widget could not be bound: ${exception.javaClass.simpleName}")
            false
        }
    }

    override fun release(widgetId: Int) = host.deleteAppWidgetId(widgetId)

    override fun isAvailable(widgetId: Int): Boolean = manager.getAppWidgetInfo(widgetId) != null

    override suspend fun resizeOf(widgetId: Int): WidgetResize? = withContext(Dispatchers.IO) {
        manager.getAppWidgetInfo(widgetId)?.let { resizeOf(it, context.resources.displayMetrics.density) }
    }

    override suspend fun canSetUp(widgetId: Int): Boolean =
        withContext(Dispatchers.IO) { manager.getAppWidgetInfo(widgetId)?.configure != null }

    override suspend fun boundIds(): List<Int> = withContext(Dispatchers.IO) { host.appWidgetIds.toList() }

    /**
     * The provider of [providerId], for its picture, the "allow" window of Android and the setup screen: from the list
     * read last, or else from the widgets of its own app. Never the whole list again (it failed with many widgets).
     */
    fun providerOf(providerId: String): AppWidgetProviderInfo? = known.find(providerId) {
        val app = providerId.substringBefore('/')
        val user = providerId.substringAfterLast('@').toLongOrNull()?.let(users::getUserForSerialNumber)
        if (user == null) null else readOrNull { manager.getInstalledProvidersForPackage(app, user) }
    }

    /**
     * The picture the app gives of the widget, or else its icon, off the main thread and kept in memory ([previews]),
     * so scrolling back does not read it again. A widget of Android 12+ with a preview layout is shown as that layout
     * instead (`previewViewOf`), as One UI does.
     */
    suspend fun previewOf(providerId: String): Bitmap? = previews.get(providerId) ?: withContext(Dispatchers.IO) {
        providerOf(providerId)?.let(::imagePreviewOf)
    }?.also { previews.put(providerId, it) }

    private fun imagePreviewOf(info: AppWidgetProviderInfo): Bitmap? {
        val density = context.resources.displayMetrics.densityDpi
        return runCatching {
            // With a generated preview drawn over it, only the icon goes under (a picture as well would show twice).
            val picture = if (hasGenerated(info)) null else info.loadPreviewImage(context, density)
            (picture ?: info.loadIcon(context, density))?.let { drawable ->
                val scale = PREVIEW_SIDE_PX.toFloat() / maxOf(drawable.intrinsicWidth, drawable.intrinsicHeight, 1)
                drawable.toBitmap(
                    (drawable.intrinsicWidth * minOf(scale, 1f)).toInt().coerceAtLeast(1),
                    (drawable.intrinsicHeight * minOf(scale, 1f)).toInt().coerceAtLeast(1)
                )
            }
        }.getOrNull()
    }

    private fun idOf(info: AppWidgetProviderInfo) = providerIdOf(info, users)

    private fun hasGenerated(info: AppWidgetProviderInfo): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && info.hasGeneratedPreview()
}

/** The view Android draws the widget in; the app of the widget fills it. */
internal fun AndroidWidgetHost.createView(context: Context, widgetId: Int): AppWidgetHostView? {
    val info = manager.getAppWidgetInfo(widgetId) ?: return null
    return host.createView(context, widgetId, info)
}

/** Widgets update only while the home screen is shown: on when it starts, off when it stops. */
internal fun AndroidWidgetHost.setListening(isListening: Boolean) {
    runCatching { if (isListening) host.startListening() else host.stopListening() }
        .onFailure { Log.w(TAG, "Widgets could not change listening: ${it.javaClass.simpleName}") }
}

/**
 * Whether a widget needs its app's setup before it goes on the home screen. One the app marks optional (Android 12+:
 * the clocks of One UI, among others) goes at once with its defaults, as One UI does, and its menu opens the setup
 * ("Configurar o widget"): those apps open their setup in a task of their own, whose answer never comes back.
 */
internal fun needsSetupOf(info: AppWidgetProviderInfo): Boolean {
    val isOptional = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL != 0
    return info.configure != null && !isOptional
}

/** Opens the setup screen of the widget's app (a city, a note) for [widgetId], from [activity]. */
internal fun AndroidWidgetHost.startSetup(activity: Activity, widgetId: Int, requestCode: Int) {
    host.startAppWidgetConfigureActivityForResult(activity, widgetId, 0, requestCode, null)
}

/** The name the app gives the widget on the home screen [widgetId], for its menu; off the main thread. */
internal suspend fun widgetLabelOf(context: Context, widgetId: Int): String? = withContext(Dispatchers.IO) {
    runCatching { AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId)?.loadLabel(context.packageManager) }
        .onFailure { Log.w(TAG, "The name of a widget could not be read: ${it.javaClass.simpleName}") }
        .getOrNull()
}

/**
 * Tells the widget's app the size it has, in dp, so it can choose the layout that fits. From Android 12 the app reads
 * the list of sizes: widgets made with Glance (the clock of Samsung, among others) fail with "can't show content" when
 * that list is empty, which is what the old call leaves.
 */
internal fun updateWidgetSize(view: View?, widthDp: Int, heightDp: Int) {
    val host = view as? AppWidgetHostView ?: return
    if (widthDp <= 0 || heightDp <= 0) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        host.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())))
    } else {
        @Suppress("DEPRECATION")
        host.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
    }
}

private fun providerIdOf(info: AppWidgetProviderInfo, users: UserManager) =
    "${info.provider.flattenToShortString()}@${users.getSerialNumberForUser(info.profile)}"

/**
 * The cells a widget asks for on a screen of [density] (also for one an app asks to pin): its own count on Android
 * 12+, or Android's rule of thumb from its size.
 */
internal fun spanOf(info: AppWidgetProviderInfo, density: Float): HomeSpan {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
        return HomeSpan(info.targetCellWidth, info.targetCellHeight)
    }
    return HomeSpan(cellsOf(info.minWidth, density), cellsOf(info.minHeight, density))
}

/**
 * How the app lets the widget be resized, in cells: down to its least size (or its usual one), up to its most (Android
 * 12+, or else as big as the page), only in the directions it allows.
 */
internal fun resizeOf(info: AppWidgetProviderInfo, density: Float): WidgetResize {
    fun least(resize: Int, usual: Int) = cellsOf(resize.takeIf { it > 0 } ?: usual, density)
    val hasMost = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    fun most(px: Int) = if (hasMost && px > 0) cellsOf(px, density) else Int.MAX_VALUE
    return WidgetResize(
        min = HomeSpan(least(info.minResizeWidth, info.minWidth), least(info.minResizeHeight, info.minHeight)),
        max = HomeSpan(most(maxResizeWidthOf(info)), most(maxResizeHeightOf(info))),
        isHorizontal = info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0,
        isVertical = info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0
    )
}

private fun maxResizeWidthOf(info: AppWidgetProviderInfo): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.maxResizeWidth else 0

private fun maxResizeHeightOf(info: AppWidgetProviderInfo): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.maxResizeHeight else 0

/** Android's rule of thumb for how many cells [px] take: 70 dp a cell, with 30 dp of gap. */
private fun cellsOf(px: Int, density: Float): Int =
    ((px / density + LEGACY_CELL_GAP_DP) / LEGACY_CELL_DP).toInt().coerceAtLeast(1)
