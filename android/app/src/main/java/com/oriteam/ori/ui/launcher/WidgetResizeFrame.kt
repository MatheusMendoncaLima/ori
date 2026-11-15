package com.oriteam.ori.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.WidgetResize
import com.oriteam.ori.domain.launcher.canResizeWidget
import com.oriteam.ori.ui.theme.OriDimensions
import kotlin.math.roundToInt

private val FrameWidth = 2.dp
private val HandleLength = 28.dp
private val HandleThickness = 6.dp

/**
 * A widget being resized ("Redimensionar" in its menu), as on One UI: an outline around it with a handle on its end
 * edge and one on its bottom edge, which grow or shrink it by whole cells as they are dragged, and a card with the
 * same changes as buttons ([WidgetResizeControls]) for whoever does not drag. Each size is offered only within what
 * its app allows and what the page has free. "Pronto", back or carrying something ends it.
 */
@Composable
internal fun WidgetResizeFrame(widgetId: Int, span: HomeSpan, context: HomeContext) {
    val widgets = context.viewModel.widgets
    val limits by produceState<WidgetResize?>(null, widgetId) { value = widgets.resizeOf(widgetId) }
    val resize = limits ?: return
    val grid = context.metrics.grid
    val keys = remember(context.state.home) { context.state.home.map { it.key } }
    val settings = context.state.settings
    val columns = resize.columnsFor(span, grid)
    val rows = resize.rowsFor(span, grid)
    val sizes = WidgetSizes(span) { next ->
        next.columns in columns && next.rows in rows && settings.canResizeWidget(keys, widgetId, next, grid)
    }
    val onSize = { next: HomeSpan -> if (sizes.canTake(next)) widgets.onResize(widgetId, next, keys, grid) }
    BackHandler { context.drag.resizingKey = null }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(FrameWidth, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
    ) {
        EdgeHandle(
            Orientation.Horizontal,
            context.metrics.cellWidth,
            span.columns,
            Modifier.align(Alignment.CenterEnd)
        ) {
            onSize(span.copy(columns = it))
        }
        EdgeHandle(
            Orientation.Vertical,
            context.metrics.cellHeight,
            span.rows,
            Modifier.align(Alignment.BottomCenter)
        ) {
            onSize(span.copy(rows = it))
        }
        WidgetResizeControls(sizes, onSize, onDone = { context.drag.resizingKey = null })
    }
}

/** The size of a widget now and whether it can take another one (within its app's limits and the free cells). */
internal class WidgetSizes(val current: HomeSpan, val canTake: (HomeSpan) -> Boolean)

/**
 * A handle on an edge of the frame: dragged, it counts the whole cells ([cell] long) the finger moved from where it
 * started and asks for [startCount] plus those ([onCount]), once per change. Only a picture for the screen reader:
 * the buttons of the card do the same.
 */
@Composable
private fun EdgeHandle(
    orientation: Orientation,
    cell: Dp,
    startCount: Int,
    modifier: Modifier,
    onCount: (Int) -> Unit
) {
    val cellPx = with(LocalDensity.current) { cell.toPx() }
    val count by rememberUpdatedState(startCount)
    val onChange by rememberUpdatedState(onCount)
    val drag = remember { EdgeDrag() }
    val state = rememberDraggableState { delta ->
        drag.distance += delta
        val wanted = drag.from + (drag.distance / cellPx).roundToInt()
        if (wanted != drag.asked && wanted >= 1) {
            drag.asked = wanted
            onChange(wanted)
        }
    }
    val isAcross = orientation == Orientation.Horizontal
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(OriDimensions.MinTouchTarget)
            .draggable(
                state = state,
                orientation = orientation,
                onDragStarted = {
                    drag.distance = 0f
                    drag.from = count
                    drag.asked = count
                }
            )
            .clearAndSetSemantics { }
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = if (isAcross) HandleThickness else HandleLength,
                    height = if (isAcross) HandleLength else HandleThickness
                )
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

/** Where a drag of a handle started and what it asked last; not a state (it draws nothing). */
private class EdgeDrag {
    var distance = 0f
    var from = 1
    var asked = 1
}
