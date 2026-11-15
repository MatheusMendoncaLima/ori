package com.oriteam.ori.ui.launcher

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.oriteam.ori.data.launcher.AndroidWidgetHost
import com.oriteam.ori.data.launcher.startSetup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val EXTRA_STEP = "com.oriteam.ori.widget.STEP"
private const val EXTRA_WIDGET_ID = "com.oriteam.ori.widget.ID"
private const val EXTRA_PROVIDER_ID = "com.oriteam.ori.widget.PROVIDER"
private const val SETUP_REQUEST = 43
private const val NO_WIDGET = -1

/**
 * What Android asks the person about a widget, in its own window: to allow ORI Home to show it, the app's setup before
 * it goes on the home screen, or the setup again of a widget already there.
 */
enum class WidgetStep { BIND, SET_UP, SET_UP_AGAIN }

/** The person's answer to a [WidgetStep] for [widgetId]. */
data class WidgetStepResult(val step: WidgetStep, val widgetId: Int, val isDone: Boolean)

/**
 * The answers of the widget windows, from [WidgetStepActivity] to the home screen. Kept until the home screen reads
 * them (it is stopped while the window shows), and read once.
 */
@Singleton
class WidgetStepResults @Inject constructor() {
    private val channel = Channel<WidgetStepResult>(Channel.BUFFERED)

    val results: Flow<WidgetStepResult> = channel.receiveAsFlow()

    fun send(result: WidgetStepResult) {
        channel.trySend(result)
    }
}

/**
 * Opens Android's window for a widget (the "allow" window, or the setup screen of the widget's app) and passes the
 * answer on ([WidgetStepResults]). A window opened from the home screen itself goes to a task of its own, as Android
 * keeps the home task for the home screen, and its answer never comes back: it arrives at once as "canceled" and the
 * widget was lost (the analogue clock of One UI, a weather city). From this plain, see-through screen it comes back.
 */
@AndroidEntryPoint
class WidgetStepActivity : ComponentActivity() {
    @Inject lateinit var widgetHost: AndroidWidgetHost

    @Inject lateinit var results: WidgetStepResults

    private val step by lazy { WidgetStep.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_STEP) } }
    private val widgetId by lazy { intent.getIntExtra(EXTRA_WIDGET_ID, NO_WIDGET) }

    private val askToBind = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        answer(it.resultCode == RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opened again after a turn of the screen: the window is already asking, and its answer will come.
        if (savedInstanceState != null) return
        when (step) {
            WidgetStep.BIND -> askToBind()
            WidgetStep.SET_UP, WidgetStep.SET_UP_AGAIN ->
                runCatching {
                    widgetHost.startSetup(
                        this,
                        widgetId,
                        SETUP_REQUEST
                    )
                }.onFailure { answer(isDone = false) }
            null -> finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETUP_REQUEST) answer(resultCode == RESULT_OK)
    }

    private fun askToBind() {
        val provider = intent.getStringExtra(EXTRA_PROVIDER_ID)?.let(widgetHost::providerOf)
        if (provider == null) {
            answer(isDone = false)
            return
        }
        askToBind.launch(
            Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
        )
    }

    private fun answer(isDone: Boolean) {
        step?.let { results.send(WidgetStepResult(it, widgetId, isDone)) }
        finish()
    }

    companion object {
        fun bind(context: Context, widgetId: Int, providerId: String): Intent =
            stepIntent(context, WidgetStep.BIND, widgetId).putExtra(EXTRA_PROVIDER_ID, providerId)

        fun setUp(context: Context, widgetId: Int): Intent = stepIntent(context, WidgetStep.SET_UP, widgetId)

        fun setUpAgain(context: Context, widgetId: Int): Intent =
            stepIntent(context, WidgetStep.SET_UP_AGAIN, widgetId)

        private fun stepIntent(context: Context, step: WidgetStep, widgetId: Int) =
            Intent(context, WidgetStepActivity::class.java)
                .putExtra(EXTRA_STEP, step.name)
                .putExtra(EXTRA_WIDGET_ID, widgetId)
    }
}
