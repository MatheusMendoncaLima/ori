package com.oriteam.ori.ui.launcher

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.HomeGrid
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.WidgetProvider
import com.oriteam.ori.domain.launcher.fittedTo
import com.oriteam.ori.domain.launcher.matchesSearch
import com.oriteam.ori.ui.app.screens.today.rememberCurrentMinute
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.LoadingState
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberTimeFormat
import com.oriteam.ori.ui.theme.OriSpacing

private val PreviewSize = 88.dp

/**
 * The preview layout of a widget of Android 12+ as a view, by provider id (made on the main thread, as One UI does in
 * its list); null when the widget has none. Provided by the activity.
 */
val LocalWidgetPreviewView = staticCompositionLocalOf<(Context, String) -> View?> { { _, _ -> null } }

/** Reads the picture an app gives of a widget, by provider id (off the main thread); provided by the activity. */
val LocalWidgetPreviewLoader = staticCompositionLocalOf<suspend (String) -> Bitmap?> { { null } }

/** Reads the name of a widget on the home screen by its id (off the main thread), for its menu; set by the activity. */
val LocalWidgetLabelLoader = staticCompositionLocalOf<suspend (Int) -> String?> { { null } }

/**
 * The widgets the installed apps offer, grouped by app as in Android's list, with the picture the app gives and the
 * size in cells it will take on this home screen ([grid]: a widget wider than a phone's columns takes all of them).
 * A tap adds it: Android may ask the person first, and the app may ask to set it up.
 */
@Composable
internal fun WidgetPicker(state: LauncherUiState, viewModel: LauncherViewModel, grid: HomeGrid) {
    val catalog = state.widgetCatalog
    val clockTitle = stringResource(R.string.launcher_widgets_clock)
    LauncherPaneScaffold(title = stringResource(R.string.launcher_edit_widgets), onBack = viewModel::onBack) {
        when {
            catalog.isLoading -> LoadingState()
            // Android gave nothing: said as a failure, never as "no widgets".
            catalog.providers.isEmpty() && catalog.isPartial -> EmptyState(
                title = stringResource(R.string.launcher_widgets_failed_title),
                message = stringResource(R.string.launcher_widgets_failed_message)
            )
            catalog.providers.isEmpty() -> EmptyState(
                title = stringResource(R.string.launcher_widgets_empty_title),
                message = stringResource(R.string.launcher_widgets_empty_message)
            )
            else -> WidgetList(state, viewModel, grid, clockTitle)
        }
    }
}

/**
 * The widgets, with a search at the top as One UI has ("Buscar widgets"): by the name of the widget or of its app,
 * without caring about case or accents.
 */
@Composable
private fun WidgetList(state: LauncherUiState, viewModel: LauncherViewModel, grid: HomeGrid, clockTitle: String) {
    val catalog = state.widgetCatalog
    var query by rememberSaveable { mutableStateOf("") }
    val found = remember(catalog.providers, query) {
        catalog.providers.filter { it.label.matchesSearch(query) || it.appLabel.matchesSearch(query) }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            shape = CircleShape,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(text = stringResource(R.string.launcher_widgets_search)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = OriSpacing.Small)
        )
        if (found.isEmpty() && query.isNotBlank()) {
            Text(
                text = stringResource(R.string.launcher_widgets_none_found),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(OriSpacing.Medium)
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (catalog.isPartial) {
                item(key = "partial") {
                    FormMessage(messageRes = R.string.launcher_widgets_partial, type = FormMessageType.ERROR)
                }
            }
            val showsOriClock = !state.hasClock && clockTitle.matchesSearch(query)
            if (showsOriClock || found.any { it.isClock }) item(key = "clock") { SectionHeader(text = clockTitle) }
            if (showsOriClock) {
                item(key = "ori-clock") {
                    OriClockRow {
                        viewModel.layout.onAddClock()
                        viewModel.onEditingChange(true)
                    }
                }
            }
            // The clock app of the device first, under "Relógio"; then every app, in alphabetical order.
            val groups = found.groupBy { if (it.isClock) clockTitle else it.appLabel }
            val variants = variantsOf(catalog.providers)
            groups.forEach { (group, providers) ->
                if (group != clockTitle) item(key = "app-$group") { SectionHeader(text = group) }
                items(providers, key = { it.id }) { provider ->
                    WidgetRow(provider, provider.span.fittedTo(grid), variants[provider.id]) {
                        viewModel.widgets.onAdd(provider)
                        viewModel.onEditingChange(true)
                    }
                }
            }
        }
    }
}

/**
 * A widget of the list: its picture, its name and the cells it takes; with [variant], what tells it apart from a widget
 * of its app with the same name and size (also said by the screen reader).
 */
@Composable
private fun WidgetRow(provider: WidgetProvider, span: HomeSpan, variant: WidgetVariant?, onAdd: () -> Unit) {
    val size = stringResource(R.string.launcher_widget_size, span.columns, span.rows)
    val add = stringResource(R.string.launcher_widget_add_named, provider.label, span.columns, span.rows)
    val note = when (variant) {
        is WidgetVariant.Described -> variant.text
        is WidgetVariant.Numbered -> stringResource(R.string.launcher_widget_variant, variant.number)
        null -> null
    }
    WidgetChoice(
        title = provider.label,
        detail = note?.let { stringResource(R.string.launcher_widget_with_note, size, it) } ?: size,
        description = note?.let { stringResource(R.string.launcher_widget_with_note, add, it) } ?: add,
        preview = { WidgetPreview(provider.id) },
        onAdd = onAdd
    )
}

/** The clock widget of ORI (the time and the date), when it is not on the home screen. */
@Composable
private fun OriClockRow(onAdd: () -> Unit) {
    val title = stringResource(R.string.launcher_widget_ori_clock)
    WidgetChoice(
        title = title,
        detail = stringResource(R.string.launcher_widget_ori_clock_detail),
        description = title,
        preview = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(PreviewSize)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Text(
                    text = rememberCurrentMinute().format(rememberTimeFormat()),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        onAdd = onAdd
    )
}

@Composable
private fun WidgetChoice(
    title: String,
    detail: String,
    description: String,
    preview: @Composable () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onAdd)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    onAdd()
                    true
                }
            }
            .padding(vertical = OriSpacing.Small)
    ) {
        preview()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The picture of a widget in its box: the preview layout the app draws (Android 12+), as One UI shows it; otherwise
 * the picture the app gives, or its icon. Only a picture: the row says what the widget is.
 */
@Composable
private fun WidgetPreview(providerId: String) {
    val context = LocalContext.current
    val viewOf = LocalWidgetPreviewView.current
    val view = remember(providerId) { viewOf(context, providerId) }
    val load = LocalWidgetPreviewLoader.current
    val picture by produceState<Bitmap?>(initialValue = null, providerId) { value = load(providerId) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PreviewSize)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        // The picture (or the icon) is always under the drawn preview: a layout that draws nothing outside the home
        // screen of its own maker (some clocks of Samsung) leaves the icon in sight, never an empty box.
        val image = picture
        if (image == null) {
            Icon(OriIcons.Widgets, contentDescription = null)
        } else {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(OriSpacing.ExtraSmall)
            )
        }
        if (view != null) {
            AndroidView(factory = { view }, modifier = Modifier.fillMaxSize().padding(OriSpacing.ExtraSmall))
        }
    }
}
