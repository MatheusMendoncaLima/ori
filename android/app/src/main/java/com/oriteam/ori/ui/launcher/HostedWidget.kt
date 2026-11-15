package com.oriteam.ori.ui.launcher

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import com.oriteam.ori.R
import com.oriteam.ori.data.launcher.AndroidWidgetHost
import com.oriteam.ori.data.launcher.createView
import com.oriteam.ori.data.launcher.updateWidgetSize
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing
import java.util.concurrent.atomic.AtomicReference

/**
 * A widget on the home screen: Android's view, which the widget's app fills. Its size in dp is told to the app on
 * every change, so the app can choose the layout that fits (as on other home screens). The view is kept in [shown]
 * while it is on the screen, to lift it when it is held.
 */
@Composable
internal fun HostedWidget(host: AndroidWidgetHost, widgetId: Int, modifier: Modifier, shown: ShownWidgetViews) {
    val isAvailable = remember(widgetId) { host.isAvailable(widgetId) }
    if (!isAvailable) {
        // The app of the widget was removed: there is nothing to draw, and "Remover" (or its menu) takes it out.
        Text(
            text = stringResource(R.string.launcher_widget_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(OriSpacing.Small)
        )
        return
    }
    val density = LocalDensity.current
    // The view is made once; the size changes later, so it is kept here to be told.
    val view = remember(widgetId) { AtomicReference<View?>(null) }
    AndroidView(
        factory = { context ->
            (host.createView(context, widgetId) ?: View(context)).also {
                view.set(it)
                shown.add(widgetId, it)
            }
        },
        onRelease = { shown.remove(widgetId, it) },
        modifier = modifier.onSizeChanged { size ->
            val width = with(density) { size.width.toDp().value.toInt() }
            val height = with(density) { size.height.toDp().value.toInt() }
            updateWidgetSize(view.get(), width, height)
        }
    )
}
