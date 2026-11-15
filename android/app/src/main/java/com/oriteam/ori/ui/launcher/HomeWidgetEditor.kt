package com.oriteam.ori.ui.launcher

import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.HomeGrid
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.HomeWidgetHost
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.WidgetProvider
import com.oriteam.ori.domain.launcher.WidgetResize
import com.oriteam.ori.domain.launcher.isHomeFull
import com.oriteam.ori.domain.launcher.widgetIdOf
import com.oriteam.ori.domain.launcher.widgetKey
import com.oriteam.ori.domain.launcher.withWidget
import com.oriteam.ori.domain.launcher.withWidgetResized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The widgets the installed apps offer, for the list of widgets. */
data class WidgetCatalog(
    val providers: List<WidgetProvider> = emptyList(),
    val isLoading: Boolean = false,
    /** Android could not give some widgets (or none): the list says so. */
    val isPartial: Boolean = false
)

/** What only the activity can do for a widget: ask Android's permission window, or open the app's setup. */
sealed interface WidgetRequest {
    data class AskToBind(val widgetId: Int, val providerId: String) : WidgetRequest

    data class SetUp(val widgetId: Int) : WidgetRequest

    /** The setup of a widget already on the home screen, opened again from its menu. */
    data class SetUpAgain(val widgetId: Int) : WidgetRequest
}

/**
 * Adding and removing widgets. A widget is added in steps, any of which the person may refuse: an id from Android,
 * the binding (Android may ask in its own window), the app's setup when it has one, and only then the home screen.
 * An id that does not reach the end is given back to Android, so none is left behind; one left by a step that never
 * answered (ORI closed meanwhile) is given back the next time the home screen starts, as Android's own home does.
 */
class HomeWidgetEditor internal constructor(
    private val host: HomeWidgetHost,
    private val settings: LauncherSettingsRepository,
    private val scope: CoroutineScope,
    private val onMessage: (Int) -> Unit
) {
    private val catalog = MutableStateFlow(WidgetCatalog())
    val state: StateFlow<WidgetCatalog> = catalog.asStateFlow()

    private val pending = mutableMapOf<Int, WidgetProvider>()
    private val outbox = Channel<WidgetRequest>(Channel.BUFFERED)

    /** Read by the activity, which answers with [onBindResult] and [onSetUpResult]. */
    val requests: Flow<WidgetRequest> = outbox.receiveAsFlow()

    init {
        scope.launch { releaseLeftBehind() }
    }

    /** The ids Android keeps for ORI Home that are on no page (and in no step now) go back to Android. */
    private suspend fun releaseLeftBehind() {
        val onHome = settings.settings.first().favorites.mapNotNull(::widgetIdOf).toSet()
        host.boundIds().filter { it !in onHome && it !in pending }.forEach(host::release)
    }

    /**
     * Reads the widgets of the device again. The list already read stays on screen meanwhile: "Carregando" only
     * shows the first time, when there is nothing to show.
     */
    fun loadProviders() {
        catalog.value = catalog.value.copy(isLoading = catalog.value.providers.isEmpty())
        scope.launch {
            val read = host.providers()
            catalog.value = WidgetCatalog(providers = read.providers, isPartial = !read.isComplete)
        }
    }

    fun onAdd(provider: WidgetProvider) {
        scope.launch {
            if (settings.settings.first().isHomeFull) {
                onMessage(R.string.launcher_favorites_full)
                return@launch
            }
            val widgetId = host.allocate()
            pending[widgetId] = provider
            if (host.bindIfAllowed(widgetId, provider.id)) {
                afterBind(widgetId)
            } else {
                outbox.send(
                    WidgetRequest.AskToBind(widgetId, provider.id)
                )
            }
        }
    }

    fun onBindResult(widgetId: Int, isAllowed: Boolean) {
        if (isAllowed) afterBind(widgetId) else cancel(widgetId)
    }

    fun onSetUpResult(widgetId: Int, isDone: Boolean) {
        if (isDone) finish(widgetId) else cancel(widgetId)
    }

    /** Whether the app of the widget has a setup screen to open again, for its menu. */
    suspend fun canSetUp(widgetId: Int): Boolean = host.canSetUp(widgetId)

    /** "Configurar o widget": the app's setup again; the widget stays on the home screen whatever the answer. */
    fun onSetUpAgain(widgetId: Int) {
        outbox.trySend(WidgetRequest.SetUpAgain(widgetId))
    }

    /** How the app of the widget lets it be resized ("Redimensionar"); null when the widget is gone. */
    suspend fun resizeOf(widgetId: Int): WidgetResize? = host.resizeOf(widgetId)

    /** The widget [widgetId] with [span] cells, on the home screen of [keys] laid out on [grid]. */
    fun onResize(widgetId: Int, span: HomeSpan, keys: List<String>, grid: HomeGrid) {
        scope.launch { settings.update { it.withWidgetResized(keys, widgetId, span, grid) } }
    }

    /** Widgets that already left the home screen (with their page): their ids go back to Android. */
    fun release(widgetIds: List<Int>) = widgetIds.forEach(host::release)

    /** A widget taken out ("Remover" or its menu): it leaves the home screen and its id goes back to Android. */
    fun onRemove(widgetId: Int) {
        host.release(widgetId)
        val key = widgetKey(widgetId)
        scope.launch {
            settings.update {
                it.copy(
                    favorites = it.favorites - key,
                    positions = it.positions - key,
                    positionsTurned = it.positionsTurned - key,
                    widgetSpans = it.widgetSpans - widgetId
                )
            }
        }
    }

    private fun afterBind(widgetId: Int) {
        val provider = pending[widgetId] ?: return cancel(widgetId)
        if (provider.needsSetup) outbox.trySend(WidgetRequest.SetUp(widgetId)) else finish(widgetId)
    }

    private fun finish(widgetId: Int) {
        val provider = pending.remove(widgetId) ?: return cancel(widgetId)
        scope.launch { settings.update { it.withWidget(widgetId, provider.span) } }
    }

    /** Also for a result that arrives after ORI was closed and opened again: the id is simply given back. */
    private fun cancel(widgetId: Int) {
        pending.remove(widgetId)
        host.release(widgetId)
    }
}
