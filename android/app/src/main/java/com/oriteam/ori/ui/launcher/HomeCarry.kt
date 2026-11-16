package com.oriteam.ori.ui.launcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.launcher.dropped
import com.oriteam.ori.domain.launcher.droppedOnDock
import com.oriteam.ori.domain.launcher.droppedOnItem
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.domain.launcher.widgetIdOf
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

/** The carried item grows a little, so it is clear what moves. */
private const val CARRIED_SCALE = 1.08f

/**
 * A tile ("Máximo") held over an app or folder it would go into gets small: the tile under it, as big as the carried
 * one, shows with the folder drawn around it, as an icon over another does on the other sizes.
 */
private const val INTO_FOLDER_SCALE = 0.55f
private const val INTO_FOLDER_MILLIS = 180
private const val CLOCK_CARD_ALPHA = 0.35f

/** The copy that follows the finger, found by tests (it is not read by the screen reader). */
internal const val CARRIED_COPY_TAG = "carried_copy"

/** How long the finger waits at an edge before the page turns, as on other home screens. */
private const val EDGE_WAIT_MILLIS = 650L
private val EdgeWidth = 40.dp

/**
 * Where a carried item lands: a cell, the dock, an app or folder (a folder), or "Remover" (for an app from all the
 * apps, "Remover" lets it go).
 */
internal fun dropCarried(key: String, context: HomeContext) {
    val layout = context.viewModel.layout
    val drag = context.drag
    val settings = context.state.settings
    val keys = context.state.home.map { it.key }
    val isOnHome = key in keys || context.state.dock.any { it.key == key }
    val capacity = context.metrics.dockCapacity
    when (val drop = drag.dropTarget(context.metrics, context.state.dock.map { it.key }, context.folderTargets)) {
        HomeDrop.OnRemove -> layout.onRemove(key)
        is HomeDrop.OnItem -> {
            // Into the folder: the copy waits over it until the folder shows the app. Refused, it goes back.
            if (settings.droppedOnItem(key, drop.target, name = "") != settings) {
                drag.land(key, expectsMove = true, into = drop.target)
            } else if (isOnHome) {
                drag.land(key, expectsMove = false)
            }
            context.viewModel.folders.onDropOnItem(key, drop.target)
        }
        is HomeDrop.OnDock -> {
            // The same rule the dock applies, read now: the item lands in it, or goes back from where it was let go.
            val moves = settings.droppedOnDock(key, drop.index, capacity) != settings
            if (moves || isOnHome) drag.land(key, expectsMove = moves)
            layout.onDropOnDock(key, drop.index, capacity)
        }
        is HomeDrop.OnCell -> {
            drag.landingPage = drop.cell.page
            val moves = settings.dropped(keys, key, drop.cell, context.metrics.grid) != settings
            if (moves || isOnHome) drag.land(key, expectsMove = moves)
            layout.onDropItem(key, drop.cell, context.metrics.grid)
        }
        // Over nothing (the bars, the panel): it goes back to its place from where it was let go.
        null -> if (isOnHome) drag.land(key, expectsMove = false)
    }
}

/**
 * What follows the finger while an item is carried: a copy of the icon without its name, as on One UI (over another
 * icon, two names would mix), the clock with its area, or the widget as it looks.
 */
@Composable
internal fun CarriedItem(context: HomeContext) {
    val drag = context.drag
    // After the drop, the copy stays where it was let go until the item takes over from it.
    val key = drag.draggedKey ?: drag.landing?.key ?: return
    val entry = context.entries[key] ?: context.appOf(key)?.let(HomeEntry::App) ?: return
    val settings = context.state.settings
    val ink = LocalContentColor.current
    val size = with(LocalDensity.current) { drag.itemSize.width.toDp() to drag.itemSize.height.toDp() }
    val isTiles = context.metrics.isTiles
    val isIntoFolder by remember(drag, context.folderTargets, isTiles) {
        derivedStateOf { isTiles && (drag.folderTarget(context.folderTargets) ?: drag.landing?.into) != null }
    }
    val scale = animateFloatAsState(
        targetValue = if (isIntoFolder) INTO_FOLDER_SCALE else CARRIED_SCALE,
        animationSpec = LocalOriAppearance.current.motion.feedback(INTO_FOLDER_MILLIS),
        label = "IntoFolder"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset { IntOffset(drag.itemOffset.x.roundToInt(), drag.itemOffset.y.roundToInt()) }
            .size(size.first, size.second)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .testTag(CARRIED_COPY_TAG)
            .clearAndSetSemantics { }
    ) {
        when (entry) {
            // A tile ("Máximo") is carried whole, with its name inside: over another tile, no names mix.
            is HomeEntry.App -> if (context.metrics.isTiles) {
                LauncherTile(
                    label = entry.app.label,
                    size = settings.iconSize,
                    onOpen = {},
                    tile = rememberAppTile(entry.app, settings.isMonochrome, settings.showsLabels)
                ) { AppIcon(entry.app, tileIconSize(context.metrics), isMonochrome = settings.isMonochrome) }
            } else {
                WithoutName {
                    LauncherTile(
                        label = entry.app.label,
                        size = settings.iconSize,
                        onOpen = {},
                        labelLines = context.metrics.labelLines
                    ) { AppIcon(entry.app, context.metrics.iconSize, isMonochrome = settings.isMonochrome) }
                }
            }
            is HomeEntry.Folder -> WithoutName {
                LauncherTile(
                    label = folderLabel(entry),
                    size = settings.iconSize,
                    onOpen = {},
                    labelLines = context.metrics.labelLines
                ) { FolderIcon(entry.apps, context.metrics.iconSize, settings.isMonochrome, ink) }
            }
            // The clock carries its area with it, so its size is seen while it moves.
            HomeEntry.Clock -> Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = CLOCK_CARD_ALPHA),
                contentColor = LocalContentColor.current,
                modifier = Modifier.fillMaxSize()
            ) {
                HomeClock(
                    actions = context.clock,
                    modifier = Modifier.scaledToFit(context.metrics.clockSize)
                )
            }
            is HomeEntry.Widget -> CarriedWidget(key, drag)
        }
    }
}

/** The picture of the widget held ([key]) as it looked when lifted; [image] null when it could not be drawn. */
internal data class LiftedPicture(val key: String, val image: ImageBitmap?)

/**
 * The item [key] was held (null: an empty place). A widget is lifted at once, before anything changes: its own touch
 * ends and it is pictured as it looks, for the copy that follows the finger.
 */
internal fun liftHeld(key: String?, context: HomeContext) {
    val widgetId = key?.let(::widgetIdOf)
    context.drag.lifted = widgetId?.let { LiftedPicture(key, context.slots.liftWidget(it)) }
}

/**
 * A widget carried: as it looked when it was lifted (in the same room and with the same corners as on its cells), as
 * One UI carries it. A widget brought from the list of widgets, or one that could not be drawn, is a plain card with
 * the widget sign.
 */
@Composable
private fun CarriedWidget(key: String, drag: HomeDragState) {
    val picture = drag.lifted?.takeIf { it.key == key }?.image
    if (picture == null) {
        WidgetCard()
        return
    }
    Image(
        bitmap = picture,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().padding(OriSpacing.ExtraSmall)
    )
}

@Composable
private fun WidgetCard() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(OriIcons.Widgets, contentDescription = null) }
    }
}

/**
 * The name under a carried icon is drawn in no color (and without its shadow): its room stays, so the icon is where
 * it was under the finger, but the name does not show.
 */
@Composable
private fun WithoutName(tile: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides Color.Transparent, LocalHomeInk provides null, content = tile)
}

/**
 * While an item is held at the left or right edge, the pages turn one by one (never into the panel or "+"). The edges
 * are those of the room inside the system bars: on a phone turned, the navigation bar or the camera cutout is at a
 * side, and an edge under it could never be reached.
 */
@Composable
internal fun TurnPagesAtEdges(drag: HomeDragState, pager: PagerState, homePages: IntRange, widthPx: Float) {
    val density = LocalDensity.current
    val insets = WindowInsets.homeBars.asPaddingValues()
    val direction = LocalLayoutDirection.current
    val edge = with(density) { EdgeWidth.toPx() }
    val left = with(density) { insets.calculateLeftPadding(direction).toPx() } + edge
    val right = widthPx - with(density) { insets.calculateRightPadding(direction).toPx() } - edge
    LaunchedEffect(drag, pager, homePages, left, right) {
        snapshotFlow {
            when {
                drag.draggedKey == null -> 0
                drag.finger.x < left -> -1
                drag.finger.x > right -> 1
                else -> 0
            }
        }.distinctUntilChanged().collectLatest { direction ->
            while (direction != 0) {
                delay(EDGE_WAIT_MILLIS)
                val target = pager.currentPage + direction
                if (target !in homePages) break
                pager.animateScrollToPage(target)
            }
        }
    }
}
