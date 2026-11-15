package com.oriteam.ori.ui.launcher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.oriteam.ori.R
import com.oriteam.ori.data.launcher.AndroidShortcuts
import com.oriteam.ori.data.launcher.AndroidWidgetHost
import com.oriteam.ori.data.launcher.needsSetupOf
import com.oriteam.ori.data.launcher.spanOf
import com.oriteam.ori.data.launcher.startSetup
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.pinRefusal
import com.oriteam.ori.domain.launcher.withAddedToHome
import com.oriteam.ori.domain.launcher.withWidget
import com.oriteam.ori.ui.theme.OriTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "PinRequest"
private const val PIN_SETUP_REQUEST = 42
private const val PICTURE_SIZE_PX = 192

/** A widget accepted and waiting for its app's setup screen before it goes on the home screen. */
private data class WidgetWaiting(val widgetId: Int, val span: HomeSpan)

/**
 * Android's "Adicionar à tela inicial": an app asks the home app in use to put one of its shortcuts or widgets there,
 * and Android opens this. ORI Home shows what it is and whose ([PinRequestDialog]) and adds it only on the person's
 * yes, at the first free place. Turned on and off with the home entry, so it never answers while ORI Home is not the
 * home app.
 *
 * Anyone can open an activity that is exported, so a request is trusted only when Android itself carries it out: the
 * shortcut is then pinned, and the widget bound; otherwise nothing is added.
 */
@AndroidEntryPoint
class PinRequestActivity : ComponentActivity() {
    @Inject lateinit var settings: LauncherSettingsRepository

    @Inject lateinit var shortcuts: AndroidShortcuts

    @Inject lateinit var widgetHost: AndroidWidgetHost

    private var waiting: WidgetWaiting? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = getSystemService(LauncherApps::class.java).getPinItemRequest(intent)
        if (request == null || !runCatching { request.isValid }.getOrDefault(false)) {
            finish()
            return
        }
        lifecycleScope.launch {
            val item = withContext(Dispatchers.IO) { itemOf(request) }
            if (item == null) {
                finish()
                return@launch
            }
            setContent {
                OriTheme {
                    val current by settings.settings.collectAsStateWithLifecycle(initialValue = null)
                    current?.let {
                        PinRequestDialog(item, it.pinRefusal, onAdd = { add(request, item) }, onDismiss = ::finish)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val widget = waiting ?: return
        if (requestCode != PIN_SETUP_REQUEST) return
        waiting = null
        if (resultCode == RESULT_OK) {
            lifecycleScope.launch {
                settings.update { it.withWidget(widget.widgetId, widget.span) }
                finish()
            }
        } else {
            widgetHost.release(widget.widgetId)
            finish()
        }
    }

    private fun add(request: PinItemRequest, item: PinnedItem) {
        lifecycleScope.launch {
            val isAdded = when (item) {
                is PinnedItem.Shortcut -> addShortcut(request, item.key)
                is PinnedItem.Widget -> addWidget(request, item.provider)
            }
            if (!isAdded) {
                Toast.makeText(this@PinRequestActivity, R.string.launcher_pin_failed, Toast.LENGTH_LONG).show()
            }
            if (waiting == null) finish()
        }
    }

    /** Android pins the shortcut on accepting; only a shortcut it really pinned goes on the home screen. */
    private suspend fun addShortcut(request: PinItemRequest, key: String): Boolean {
        val isPinned = accepted { request.accept() } && withContext(Dispatchers.IO) { shortcuts.isPinned(key) }
        if (isPinned) settings.update { it.withAddedToHome(key) }
        return isPinned
    }

    /**
     * A new widget id goes with the yes; Android binds it to the widget. One whose app needs a setup (a city, a note)
     * opens it first, and goes on the home screen only when it is done.
     */
    private suspend fun addWidget(request: PinItemRequest, provider: AppWidgetProviderInfo): Boolean {
        val widgetId = widgetHost.allocate()
        val isBound = accepted { request.accept(bundleOf(AppWidgetManager.EXTRA_APPWIDGET_ID to widgetId)) } &&
            widgetHost.isAvailable(widgetId)
        val span = spanOf(provider, resources.displayMetrics.density)
        return when {
            !isBound -> {
                widgetHost.release(widgetId)
                false
            }
            !needsSetupOf(provider) -> {
                settings.update { it.withWidget(widgetId, span) }
                true
            }
            else -> startSetup(WidgetWaiting(widgetId, span))
        }
    }

    /** The setup screen of the widget's app; the widget waits for its answer ([onActivityResult]). */
    private fun startSetup(widget: WidgetWaiting): Boolean {
        waiting = widget
        return runCatching { widgetHost.startSetup(this, widget.widgetId, PIN_SETUP_REQUEST) }.onFailure {
            waiting = null
            widgetHost.release(widget.widgetId)
        }.isSuccess
    }

    /** A request no longer valid (the app was removed meanwhile) throws: it is simply not accepted. */
    private fun accepted(accept: () -> Boolean): Boolean = try {
        accept()
    } catch (exception: IllegalStateException) {
        Log.w(TAG, "Request not accepted: ${exception.javaClass.simpleName}")
        false
    }

    /** What the request is, with its name, its app and its picture; null for a kind ORI Home does not take. */
    private fun itemOf(request: PinItemRequest): PinnedItem? {
        val density = resources.displayMetrics.densityDpi
        return when (request.requestType) {
            PinItemRequest.REQUEST_TYPE_SHORTCUT -> request.shortcutInfo?.let { info ->
                val icon = runCatching {
                    getSystemService(LauncherApps::class.java).getShortcutBadgedIconDrawable(info, density)
                }.getOrNull()
                PinnedItem.Shortcut(
                    label = (info.shortLabel ?: info.longLabel ?: "").toString(),
                    appLabel = appLabelOf(info.`package`, info.userHandle),
                    picture = icon?.toBitmap(PICTURE_SIZE_PX, PICTURE_SIZE_PX),
                    key = shortcuts.keyOf(info)
                )
            }
            PinItemRequest.REQUEST_TYPE_APPWIDGET -> request.getAppWidgetProviderInfo(this)?.let { provider ->
                PinnedItem.Widget(
                    label = provider.loadLabel(packageManager),
                    appLabel = appLabelOf(provider.provider.packageName, provider.profile),
                    picture = provider.loadIcon(this, density)?.toBitmap(PICTURE_SIZE_PX, PICTURE_SIZE_PX),
                    provider = provider
                )
            }
            else -> null
        }
    }

    private fun appLabelOf(packageName: String, user: UserHandle): String = try {
        getSystemService(LauncherApps::class.java).getApplicationInfo(packageName, 0, user).loadLabel(packageManager)
            .toString()
    } catch (exception: PackageManager.NameNotFoundException) {
        Log.w(TAG, "App of the request not found: ${exception.javaClass.simpleName}")
        packageName
    }
}
