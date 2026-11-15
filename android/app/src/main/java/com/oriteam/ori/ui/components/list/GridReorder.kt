package com.oriteam.ori.ui.components.list

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** A dragged item grows a little, so it is clear which one moves. */
private const val DRAGGED_SCALE = 1.08f

/** How long an item let go takes to slide from under the finger into its place. */
private const val SETTLE_MILLIS = 220

/** How long the order built by a drag stays on screen, at most, waiting for the new order to be stored. */
internal const val STORE_WAIT_MILLIS = 2_000L

/**
 * Holding an item of a lazy grid and dragging it changes its place: the other items make room as it passes, and
 * the new order is given when the finger lifts. Let go, the item slides from under the finger into its place, as
 * on ORI Home, and the grid keeps the new order until it is stored. Showing the stored order in the meantime (a
 * write takes a moment) put the item back where it was and then made it travel to where it was let go.
 * The grid's item keys must be [keyOf] of each item.
 *
 * Dragging is not possible for everyone: the screens also give "move before/after" actions to screen readers.
 */
@Stable
class GridReorderState<T> internal constructor(
    internal val gridState: LazyGridState,
    private val scope: CoroutineScope,
    private val keyOf: (T) -> Any
) {
    internal var motion = OriMotion()
    private var draggedKey by mutableStateOf<Any?>(null)

    /** The item let go, while it slides into its place: it stays above the others. */
    private var settlingKey by mutableStateOf<Any?>(null)
    internal var offset by mutableStateOf(Offset.Zero)
        private set
    internal var scale by mutableFloatStateOf(1f)
        private set

    /** The order built by the drag, shown until the stored order is the same. */
    private var order by mutableStateOf<List<T>?>(null)

    /** The order as stored: the last one given to [shown]. */
    private var stored by mutableStateOf<List<T>>(emptyList())
    private var settling: Job? = null
    private var waiting: Job? = null

    /** While an item is dragged, or its new place is being stored, the order built; otherwise [items] as they are. */
    @Composable
    fun shown(items: List<T>): List<T> {
        SideEffect { stored = items }
        return order ?: items
    }

    /** The item is carried or slides into its place: it is drawn above the others, and only this state moves it. */
    fun isMoving(item: T): Boolean = keyOf(item).let { it == draggedKey || it == settlingKey }

    internal fun start(item: T, items: List<T>) {
        settling?.cancel()
        waiting?.cancel()
        settlingKey = null
        draggedKey = keyOf(item)
        offset = Offset.Zero
        scale = DRAGGED_SCALE
        order = items
    }

    /** The finger lifted: the new index of [item], or null when it ended where it started. */
    internal fun finish(item: T, items: List<T>): Int? {
        val key = keyOf(item)
        val built = order
        val newIndex = built?.indexOfFirst { keyOf(it) == key } ?: -1
        val isMoved = built != null && newIndex >= 0 && newIndex != items.indexOfFirst { keyOf(it) == key }
        if (isMoved) keepUntilStored(built) else order = null
        settle(key)
        return newIndex.takeIf { isMoved }
    }

    /** The gesture was taken away before the finger lifted: nothing moves. */
    internal fun cancel(item: T) {
        order = null
        settle(keyOf(item))
    }

    /** The finger moved [amount] while carrying [item]. */
    internal fun drag(item: T, amount: Offset) {
        offset += amount
        swapUnderFinger(item)
    }

    /**
     * When the center of the dragged item is over another item, the two change places. The offset is corrected by
     * the distance between the two places, so the item stays under the finger while the grid moves around it.
     */
    private fun swapUnderFinger(item: T) {
        val current = order ?: return
        val key = keyOf(item)
        val visible = gridState.layoutInfo.visibleItemsInfo
        val dragged = visible.firstOrNull { it.key == key } ?: return
        val center = Offset(
            dragged.offset.x + dragged.size.width / 2f + offset.x,
            dragged.offset.y + dragged.size.height / 2f + offset.y
        )
        val target = visible.firstOrNull { info ->
            info.key != key && current.any { keyOf(it) == info.key } &&
                center.x in info.offset.x.toFloat()..(info.offset.x + info.size.width).toFloat() &&
                center.y in info.offset.y.toFloat()..(info.offset.y + info.size.height).toFloat()
        } ?: return
        val from = current.indexOfFirst { keyOf(it) == key }
        val to = current.indexOfFirst { keyOf(it) == target.key }
        order = current.toMutableList().apply { add(to, removeAt(from)) }
        offset += Offset(
            (dragged.offset.x - target.offset.x).toFloat(),
            (dragged.offset.y - target.offset.y).toFloat()
        )
    }

    /** From under the finger to its place, shrinking back; without motion that moves things, at once. */
    private fun settle(key: Any) {
        draggedKey = null
        settlingKey = key
        val from = offset
        settling = scope.launch {
            if (motion.movesThings) {
                animate(0f, 1f, animationSpec = motion.feedback(SETTLE_MILLIS)) { progress, _ ->
                    offset = from * (1f - progress)
                    scale = DRAGGED_SCALE + (1f - DRAGGED_SCALE) * progress
                }
            }
            offset = Offset.Zero
            scale = 1f
            settlingKey = null
        }
    }

    /** If the order is not stored in time (the write failed), the stored one comes back. */
    private fun keepUntilStored(built: List<T>) {
        val keys = built.map(keyOf)
        waiting = scope.launch {
            withTimeoutOrNull(STORE_WAIT_MILLIS) { snapshotFlow { stored.map(keyOf) }.first { it == keys } }
            order = null
        }
    }
}

@Composable
fun <T> rememberGridReorderState(gridState: LazyGridState, keyOf: (T) -> Any): GridReorderState<T> {
    val scope = rememberCoroutineScope()
    val motion = LocalOriAppearance.current.motion
    val state = remember(gridState) { GridReorderState(gridState, scope, keyOf) }
    SideEffect { state.motion = motion }
    return state
}

/**
 * Makes [item] draggable after a long press. [items] is the order as stored; [onMove] receives the item and its new
 * index when the finger lifts. [onDragStart] runs when the hold is recognized, even if the finger then stays still.
 */
fun <T> Modifier.reorderable(
    item: T,
    items: List<T>,
    state: GridReorderState<T>,
    onMove: (T, Int) -> Unit,
    onDragStart: () -> Unit = {}
): Modifier = this
    .zIndex(if (state.isMoving(item)) 1f else 0f)
    .graphicsLayer {
        if (state.isMoving(item)) {
            translationX = state.offset.x
            translationY = state.offset.y
            scaleX = state.scale
            scaleY = state.scale
        }
    }
    .pointerInput(item, items) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                state.start(item, items)
                onDragStart()
            },
            onDrag = { change, amount ->
                change.consume()
                state.drag(item, amount)
            },
            onDragEnd = { state.finish(item, items)?.let { onMove(item, it) } },
            onDragCancel = { state.cancel(item) }
        )
    }
