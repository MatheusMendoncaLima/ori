package com.oriteam.ori.ui.launcher

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.AppOrigin
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.PlacedHomeItem
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.ui.components.motion.entrance
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing

private const val PLACE_MILLIS = 260
private const val MAX_ENTRANCE_ORDER = 8

/** Everything a page of the home screen needs, so each piece takes one parameter instead of seven. */
internal class HomeContext(
    val state: LauncherUiState,
    val viewModel: LauncherViewModel,
    val metrics: HomeMetrics,
    val drag: HomeDragState,
    val slots: LauncherSlots,
    val clock: ClockActions
) {
    val entries: Map<String, HomeEntry> = state.home.associateBy { it.key }
    val isEditing: Boolean get() = state.isEditingHome

    /** The apps and folders of the pages: an app held over the middle of one of them makes a folder. */
    val folderTargets: Set<String> =
        state.home.filter { it is HomeEntry.App || it is HomeEntry.Folder }.mapTo(mutableSetOf()) { it.key }

    /** The app of [key] wherever it is shown: on the pages, in a folder, in the dock or in all the apps. */
    fun appOf(key: String): LauncherApp? = (entries[key] as? HomeEntry.App)?.app
        ?: state.home.filterIsInstance<HomeEntry.Folder>().firstNotNullOfOrNull { folder ->
            folder.apps.firstOrNull { it.key == key }
        }
        ?: (state.dock + state.drawer).firstOrNull { it.key == key }
}

/**
 * One page of cells, with its items where the layout put them (the clock widget is one of them). An item that
 * changes cell slides there with the person's motion level. Each item and the page tell the drag where they are.
 *
 * [allPlaced] is every item of every page, in reading order: the screen reader moves an item through all of them,
 * and the drop preview finds the size of the carried one. The items rise in only when the home screen is first shown
 * ([arrives]); a page turned to is already there, as on Android.
 */
@Composable
internal fun HomePageGrid(
    page: Int,
    items: List<PlacedHomeItem>,
    rows: Int,
    context: HomeContext,
    allPlaced: List<PlacedHomeItem>,
    arrives: Boolean,
    modifier: Modifier = Modifier
) {
    val metrics = context.metrics
    Box(
        modifier = modifier
            .width(metrics.gridWidth)
            .height(metrics.cellHeight * rows)
            .onGloballyPositioned { context.drag.places.placePage(page, it) }
    ) {
        DisposableEffect(page) { onDispose { context.drag.places.removePage(page) } }
        // Only on an empty home screen (the clock alone): never over widgets or icons.
        val isEmpty = !context.state.isLoading && context.state.home.all { it is HomeEntry.Clock }
        if (page == 0 && isEmpty) EmptyHint(items, context)
        DropPreview(page, allPlaced, context)
        // Changes only when the finger reaches or leaves the middle of an item, not at each move; let go there, the
        // item keeps the folder drawn around it until it shows the app inside.
        val folderTarget by remember(context.drag, context.folderTargets) {
            derivedStateOf { context.drag.folderTarget(context.folderTargets) ?: context.drag.landing?.into }
        }
        // The items there when the page is first shown (with the home screen read) rise in. One that joins it later (an
        // app taken out of a folder, a folder made or undone, an app added) is simply there: it rose in from nothing,
        // after the turn of the others before it, and blinked.
        var isShown by remember { mutableStateOf(false) }
        val isLoading = context.state.isLoading
        LaunchedEffect(isLoading) { if (!isLoading) isShown = true }
        items.forEachIndexed { index, item ->
            val order = ItemOrder(onPage = index, inReading = allPlaced.indexOf(item), last = allPlaced.lastIndex)
            // By key: an item that changes place in the order keeps its own state (its slide, its landing).
            key(item.key) {
                val risesIn = remember { arrives && !isShown }
                HomeItem(item, order, risesIn, context, isFolderTarget = folderTarget == item.key)
            }
        }
    }
}

/** Where an item is among the items of its page (for its entrance) and among all of them (for the screen reader). */
private data class ItemOrder(val onPage: Int, val inReading: Int, val last: Int)

@Composable
private fun HomeItem(
    item: PlacedHomeItem,
    order: ItemOrder,
    arrives: Boolean,
    context: HomeContext,
    isFolderTarget: Boolean
) {
    val entry = context.entries[item.key] ?: return
    val metrics = context.metrics
    val drag = context.drag
    val motion = LocalOriAppearance.current.motion
    val target = with(LocalDensity.current) {
        IntOffset(
            (metrics.cellWidth * item.cell.column).roundToPx(),
            (metrics.cellHeight * item.cell.row).roundToPx()
        )
    }
    // The item just dropped goes to its new cell at once: it travels there from where it was let go instead.
    val isLanding = drag.landing?.key == item.key
    val offset by animateIntOffsetAsState(
        targetValue = target,
        animationSpec = if (isLanding) snap() else motion.feedback(PLACE_MILLIS),
        label = "HomeItemPlace"
    )
    val actions = Modifier.itemActions(item.key, order.inReading, order.last, context)
    Box(
        modifier = Modifier
            .offset { offset }
            .size(metrics.cellWidth * item.span.columns, metrics.cellHeight * item.span.rows)
            .onGloballyPositioned { drag.places.placeItem(item.key, it) }
            .landsWhereDropped(item.key, drag)
            .graphicsLayer { alpha = if (drag.isCarriedOrLanding(item.key)) CARRIED_ITEM_ALPHA else 1f }
            .entrance(order = minOf(order.onPage, MAX_ENTRANCE_ORDER), isEnabled = arrives)
    ) {
        DisposableEffect(item.key) { onDispose { drag.places.removeItem(item.key) } }
        when (entry) {
            is HomeEntry.App -> AppCell(entry, context, isFolderTarget, actions)
            is HomeEntry.Folder -> FolderCell(entry, context, isFolderTarget, actions)
            is HomeEntry.Widget -> WidgetCell(entry, context, actions)
            HomeEntry.Clock -> HomeClock(
                actions = context.clock,
                // On a screen turned its row is lower than it needs, and it is drawn a little smaller.
                modifier = actions.scaledToFit(metrics.clockSize).padding(horizontal = OriSpacing.Small)
            )
        }
        if (entry is HomeEntry.Widget && drag.resizingKey == item.key) {
            WidgetResizeFrame(entry.widgetId, item.span, context)
        }
    }
}

@Composable
private fun AppCell(entry: HomeEntry.App, context: HomeContext, isFolderTarget: Boolean, modifier: Modifier) {
    val settings = context.state.settings
    val metrics = context.metrics
    val badge = LocalHomeBadges.current.countOf(entry.app)
    // "Máximo": a tile of the app's own color, with a smaller icon at its top and the name inside.
    val tile = if (metrics.isTiles) {
        rememberAppTile(entry.app, settings.isMonochrome, settings.showsLabels)
    } else {
        null
    }
    val icon = if (metrics.isTiles) tileIconSize(metrics) else metrics.iconSize
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        LauncherTile(
            label = entry.app.label,
            description = tileDescription(entry.app.label, badge),
            size = settings.iconSize,
            onOpen = { from: AppOrigin? -> context.viewModel.onOpenApp(entry.app, from) }
                .takeUnless { context.isEditing },
            isEditing = context.isEditing,
            labelLines = metrics.labelLines,
            tile = tile,
            modifier = modifier
        ) {
            DropTargetIcon(isFolderTarget) {
                BadgedIcon(badge, icon) {
                    AppIcon(app = entry.app, size = icon, isMonochrome = settings.isMonochrome)
                }
            }
        }
    }
}

/**
 * A widget, drawn by its app. While editing, a layer over it takes the touches, so holding moves the widget
 * instead of pressing what is inside it.
 */
@Composable
private fun WidgetCell(entry: HomeEntry.Widget, context: HomeContext, modifier: Modifier) {
    val label = stringResource(R.string.launcher_widget)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(OriSpacing.ExtraSmall)
            .clip(MaterialTheme.shapes.large)
            .then(modifier)
            .semantics { contentDescription = label }
    ) {
        context.slots.widget(entry.widgetId, Modifier.fillMaxSize())
        if (context.isEditing) {
            // Hit by the touch (so the widget below gets nothing), but takes nothing: the hold still reaches the drag.
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { awaitEachGesture { awaitFirstDown(false) } })
        }
    }
}

/**
 * For the screen reader, what the fingers do: holding opens the item's menu, and the actions move it one place
 * (before or after) or take it off the home screen, since dragging is not possible for everyone.
 */
@Composable
internal fun Modifier.itemActions(key: String, index: Int, lastIndex: Int, context: HomeContext): Modifier {
    val viewModel = context.viewModel
    val grid = context.metrics.grid
    val menu = stringResource(R.string.launcher_menu_open)
    val before = stringResource(R.string.launcher_move_earlier_item)
    val after = stringResource(R.string.launcher_move_later_item)
    val remove = stringResource(R.string.launcher_menu_remove)
    val isLocked = context.state.settings.isLayoutLocked
    return semantics {
        onLongClick(menu) {
            context.drag.menuKey = key
            true
        }
        // A locked home screen keeps every item where it is: the screen reader offers no move either.
        if (isLocked) return@semantics
        customActions = listOfNotNull(
            CustomAccessibilityAction(before) {
                viewModel.layout.onStepItem(key, -1, grid)
                true
            }.takeIf { index > 0 },
            CustomAccessibilityAction(after) {
                viewModel.layout.onStepItem(key, 1, grid)
                true
            }.takeIf { index < lastIndex },
            CustomAccessibilityAction(remove) {
                viewModel.layout.onRemove(key)
                true
            }
        )
    }
}

/** The hint goes under the rows the items of the page take (the clock, wherever it was put). */
@Composable
private fun EmptyHint(items: List<PlacedHomeItem>, context: HomeContext) {
    val usedRows = items.maxOfOrNull { it.cell.row + it.span.rows } ?: 0
    Text(
        text = stringResource(R.string.launcher_favorites_empty),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = context.metrics.cellHeight * usedRows)
            .padding(OriSpacing.Medium)
            .entrance(order = 2)
    )
}
