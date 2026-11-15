package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.motion.EntranceStyle
import com.oriteam.ori.ui.components.motion.entrance
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing
import kotlin.math.roundToInt

/** The menu is never narrower than this, so a short name does not make a thin card. */
private val MenuMinWidth = 200.dp

/**
 * The menu that opens on an item held and let go, as on One UI 8: a card **above** the item (under it when there is no
 * room above), with the name of what was held ([title]), the shortcuts of the app ([shortcuts]) and each choice with
 * its icon, in the order given (what takes something away last). Nothing else on the screen changes: the item stays
 * as it was. Back or a touch outside closes it. The choices are written out ("Informações do app"), not only icons,
 * so each one says what it does. It is drawn in the window of the home screen ([MenuLayer]), not in one of its own.
 */
@Composable
internal fun AppMenu(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    entries: List<MenuEntry>,
    title: String? = null,
    shortcuts: MenuShortcuts? = null
) {
    if (!isOpen) return
    MenuRequest(MenuContent(entries, title, shortcuts, onDismiss))
}

/** The card of a menu, drawn by [MenuLayerHost]. */
@Composable
internal fun MenuCard(menu: MenuContent) {
    val title = menu.title
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = MenuDefaults.ShadowElevation,
        modifier = Modifier
            .entrance(style = EntranceStyle.POP)
            // The screen reader says the menu appeared, as it did when the menu was a window.
            .semantics { title?.let { paneTitle = it } }
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max).widthIn(min = MenuMinWidth)) {
            if (title != null) MenuTitle(title)
            menu.shortcuts?.let { ShortcutRows(it, menu.onDismiss) }
            menu.entries.forEach { entry ->
                val label = stringResource(entry.label)
                DropdownMenuItem(
                    text = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = { Icon(entry.icon, contentDescription = null) },
                    onClick = {
                        menu.onDismiss()
                        entry.onClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(horizontal = OriSpacing.Medium, vertical = OriSpacing.Small)
            .semantics { heading() }
    )
    HorizontalDivider()
}

/** The name on the card: the app's, the folder's, "Relógio do ORI", or the name the app gives the widget. */
@Composable
private fun homeItemTitle(entry: HomeEntry?, app: LauncherApp?): String? = when (entry) {
    HomeEntry.Clock -> stringResource(R.string.launcher_widget_ori_clock)
    is HomeEntry.Widget -> widgetLabel(entry.widgetId)
    is HomeEntry.Folder -> folderLabel(entry)
    else -> app?.label
}

/**
 * What the place of the item offers, between the app's page and "Desinstalar": "Criar pasta" for an app of the pages,
 * "Redimensionar" and "Configurar o widget" for a widget, editing (when it is not on), and "Remover". A locked home
 * screen offers nothing that moves or takes away.
 */
@Composable
private fun homeItemChoices(key: String, entry: HomeEntry?, context: HomeContext): List<MenuEntry> {
    val viewModel = context.viewModel
    val isLocked = context.state.settings.isLayoutLocked
    val widget = entry as? HomeEntry.Widget
    val canSetUpWidget = widget != null && canSetUp(widget.widgetId, viewModel)
    return listOfNotNull(
        MenuEntry(R.string.launcher_menu_create_folder, OriIcons.CreateFolder) { viewModel.folders.onCreate(key) }
            .takeIf { entry is HomeEntry.App && !isLocked },
        MenuEntry(R.string.launcher_menu_resize, OriIcons.Resize) { context.drag.resizingKey = key }
            .takeIf { widget != null && !isLocked },
        widget?.let {
            MenuEntry(R.string.launcher_menu_widget_setup, Icons.Filled.Settings) {
                viewModel.widgets.onSetUpAgain(it.widgetId)
            }
        }?.takeIf { canSetUpWidget },
        // Already editing: the menu does not offer what is on.
        MenuEntry(R.string.launcher_menu_edit_home, Icons.Filled.Edit) { viewModel.onEditingChange(true) }
            .takeUnless { context.isEditing },
        MenuEntry(R.string.launcher_menu_remove, Icons.Filled.Close) { viewModel.layout.onRemove(key) }
            .takeUnless { isLocked }
    )
}

/** Whether the app of the widget [widgetId] has a setup screen, read off the main thread; false until then. */
@Composable
private fun canSetUp(widgetId: Int, viewModel: LauncherViewModel): Boolean {
    val canSetUp by produceState(initialValue = false, widgetId) { value = viewModel.widgets.canSetUp(widgetId) }
    return canSetUp
}

/** The name the app of the widget gives it ("Clima"), read off the main thread; "Widget" until then, or without one. */
@Composable
private fun widgetLabel(widgetId: Int): String {
    val load = LocalWidgetLabelLoader.current
    val label by produceState<String?>(initialValue = null, widgetId) { value = load(widgetId) }
    return label ?: stringResource(R.string.launcher_widget)
}

/**
 * Where the menu goes: centered over the item and above it, as on One UI; under it when the room above (below the
 * system bar, [top]) is too small; always inside the screen, clear of the bars ([top], [bottom]).
 */
internal class AboveFirst(private val top: Int, private val bottom: Int, private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width))
        val above = anchorBounds.top - gap - popupContentSize.height
        val lowest = maxOf(top, windowSize.height - bottom - popupContentSize.height)
        val y = if (above >= top) above else (anchorBounds.bottom + gap).coerceAtMost(lowest)
        return IntOffset(x, y)
    }
}

/**
 * The menu of the item held on the home screen, opened over it: for an app, its page and "Desinstalar"; for an app of
 * the pages, "Criar pasta" (a folder with it, opened to add the others: for whoever does not drag); edit the home
 * screen, and take the item off it (not on a locked home screen; a folder taken off takes its apps with it). Moving the
 * item is done by holding and dragging it. The card has the name of the app, the folder, the clock ("Relógio do ORI")
 * or "Widget".
 */
@Composable
internal fun HomeItemMenu(context: HomeContext) {
    val drag = context.drag
    val key = drag.menuKey ?: return
    val bounds = drag.places.visibleBoundsOf(key)?.translate(-drag.origin) ?: return
    val viewModel = context.viewModel
    val entry = context.entries[key]
    val app = (entry as? HomeEntry.App)?.app ?: context.state.dock.firstOrNull { it.key == key }
    val isLocked = context.state.settings.isLayoutLocked
    val middle = homeItemChoices(key, entry, context)
    val size = with(LocalDensity.current) { DpSize(bounds.width.toDp(), bounds.height.toDp()) }
    Box(
        modifier = Modifier
            .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
            .size(size)
    ) {
        val settings = context.state.settings
        AppMenu(
            isOpen = true,
            onDismiss = { drag.menuKey = null },
            entries = app?.let { viewModel.appMenu.entriesFor(it, middle) } ?: middle,
            title = homeItemTitle(entry, app),
            shortcuts = app?.let { viewModel.menuShortcuts(it, isLocked, settings.isMonochrome) }
        )
    }
}
