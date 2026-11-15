package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import com.oriteam.ori.domain.launcher.HomeCell
import com.oriteam.ori.domain.launcher.HomeGrid
import com.oriteam.ori.domain.launcher.isAppKey

/** The item being carried stays in its place, faint, until it is dropped (on the pages and in the dock). */
internal const val CARRIED_ITEM_ALPHA = 0.3f

/** Where a carried item was dropped. */
internal sealed interface HomeDrop {
    data class OnCell(val cell: HomeCell) : HomeDrop

    /** Before the app at [index] of the dock. */
    data class OnDock(val index: Int) : HomeDrop

    /** On "Remover", at the top. */
    data object OnRemove : HomeDrop

    /** On the middle of an app or a folder of the pages ([target]): they make a folder, or the app joins it. */
    data class OnItem(val target: String) : HomeDrop
}

/**
 * An item held and carried over the home screen. [places] says where everything is on the screen (in root
 * coordinates), so the finger finds what it holds and where it is dropped, even on another page.
 */
@Stable
internal class HomeDragState {
    /** The key of the item being carried, or null. */
    var draggedKey by mutableStateOf<String?>(null)
        private set

    /** The item carried comes from all the apps (not yet on the home screen). */
    var isFromOutside by mutableStateOf(false)
        private set

    /** The picture of the item held, taken when it was lifted (a widget, as the screen showed it). */
    var lifted by mutableStateOf<LiftedPicture?>(null)

    /** The item whose menu is open (held and let go without moving), or null. */
    var menuKey by mutableStateOf<String?>(null)

    /** The widget being resized ("Redimensionar" in its menu), or null; carrying anything ends it. */
    var resizingKey by mutableStateOf<String?>(null)

    /** The item just let go, until it is in its place ([landsWhereDropped]); its copy stays where it was let go. */
    var landing by mutableStateOf<Landing?>(null)
        private set

    /** Where the top left of the carried item is, relative to the home screen. */
    var itemOffset by mutableStateOf(Offset.Zero)
        private set

    var itemSize by mutableStateOf(Size.Zero)
        private set

    /** The finger, relative to the home screen: it turns the pages at the edges and finds "Remover" and the dock. */
    var finger by mutableStateOf(Offset.Zero)
        private set

    /**
     * The page an item was just dropped on, until the drop is written. The pages are counted from what is stored, and
     * a drop on the new page made at the edge is stored a moment after the finger lets go: without this, that page
     * would be gone for a moment and the pages would slide back to the one before.
     */
    var landingPage by mutableStateOf<Int?>(null)

    internal val places = HomeScreenPlaces()
    internal var origin = Offset.Zero
    private var grab = Offset.Zero

    internal fun start(key: String, heldAt: Offset) {
        val bounds = places.wholeBoundsOf(key) ?: return
        menuKey = null
        resizingKey = null
        landing = null
        draggedKey = key
        grab = origin + heldAt - bounds.topLeft
        itemSize = bounds.size
        follow(heldAt)
    }

    internal fun follow(position: Offset) {
        itemOffset = position - grab
        finger = position
    }

    /**
     * An app carried from all the apps: it is not on the home screen, so where it is and where the finger is come in
     * root coordinates, from the list of all the apps.
     */
    internal fun startFromOutside(key: String, bounds: Rect, fingerInRoot: Offset) {
        menuKey = null
        resizingKey = null
        landing = null
        isFromOutside = true
        draggedKey = key
        grab = fingerInRoot - bounds.topLeft
        itemSize = bounds.size
        followInRoot(fingerInRoot)
    }

    internal fun followInRoot(fingerInRoot: Offset) = follow(fingerInRoot - origin)

    internal fun end() {
        draggedKey = null
        isFromOutside = false
    }

    /**
     * The item [key] was let go where its copy is now: it travels from there to its place, which the drop changes
     * ([expectsMove]) or not (it goes back), or it goes into the app or folder [into].
     */
    internal fun land(key: String, expectsMove: Boolean, into: String? = null) {
        landing = Landing(key, origin + itemOffset, places.wholeBoundsOf(key)?.topLeft, expectsMove, into)
    }

    /** The item of [done] took over from its copy (or waited too long): the copy goes. */
    internal fun finishLanding(done: Landing) {
        if (landing == done) landing = null
    }

    /** Is the finger over "Remover" now (it lights up). */
    val isOverRemove: Boolean get() = places.remove?.contains(origin + finger) == true

    /**
     * Where the carried item lands: "Remover", the dock (only for apps, before the dock app to the right of the
     * finger), the middle of an app or folder of [folderTargets] (a folder), or the cell under its first cell on the
     * page where it is. Null over nothing (the panel, the bars).
     */
    internal fun dropTarget(metrics: HomeMetrics, dockKeys: List<String>, folderTargets: Set<String>): HomeDrop? {
        val point = origin + finger
        val onItem = folderTarget(folderTargets)
        return when {
            places.remove?.contains(point) == true -> HomeDrop.OnRemove
            places.dock?.contains(point) == true -> HomeDrop.OnDock(
                dockKeys.filter { it != draggedKey }
                    .count { (places.visibleBoundsOf(it)?.center?.x ?: Float.MAX_VALUE) < point.x }
            )
            onItem != null -> HomeDrop.OnItem(onItem)
            else -> targetCell(metrics.grid)?.let(HomeDrop::OnCell)
        }
    }

    /**
     * The cell the carried item is over now (not over "Remover", the dock or the middle of an item it would make a
     * folder with), to show where it will go. Read while carrying; it changes as the item moves.
     */
    internal fun hoveredCell(metrics: HomeMetrics, folderTargets: Set<String>): HomeCell? {
        val point = origin + finger
        val isElsewhere = places.remove?.contains(point) == true || places.dock?.contains(point) == true
        return if (draggedKey == null || isElsewhere || folderTarget(folderTargets) != null) {
            null
        } else {
            targetCell(metrics.grid)
        }
    }

    /**
     * The app or folder of [targets] (the ones of the pages) whose middle the carried app is over, as on One UI: let go
     * there, they make a folder. Only an app goes in a folder, never onto itself; the middle of the carried copy
     * counts, as that is where the person sees it.
     */
    internal fun folderTarget(targets: Set<String>): String? {
        val key = draggedKey ?: return null
        if (!isAppKey(key)) return null
        val center = origin + itemOffset + Offset(itemSize.width / 2, itemSize.height / 2)
        return places.itemWithMiddleAt(center) { it != key && it in targets }
    }

    /**
     * The cells are measured on the page as drawn: while editing the pages are shown smaller (as on One UI), so a cell
     * is the page's width over the columns, not the usual size. The page is found by the part on the screen; the rows
     * are counted from the top of the whole grid, so a list scrolled down gives the row under the finger.
     */
    private fun targetCell(grid: HomeGrid): HomeCell? {
        val center = origin + itemOffset + Offset(itemSize.width / 2, itemSize.height / 2)
        val (page, bounds) = places.pageAt(center) ?: return null
        val width = bounds.width / grid.columns
        val height = bounds.height / grid.rows
        val probe = origin + itemOffset + Offset(width / 2, height / 2)
        val column = ((probe.x - bounds.left) / width).toInt().coerceIn(0, grid.columns - 1)
        val row = ((probe.y - bounds.top) / height).toInt().coerceIn(0, grid.rows - 1)
        return HomeCell(page, row, column)
    }
}

/** When an item held on the home screen starts being carried. */
internal enum class CarryStart {
    /** When the finger moves (the menu, open since the hold, closes), as on One UI. */
    ON_MOVE,

    /** At once, while editing (no menu). */
    AT_HOLD,

    /** Never, on a locked home screen: the menu opens at the hold, and moving closes it and says it is locked. */
    NEVER
}

/** How an empty place of the home screen was touched. */
internal enum class EmptyTouch { HOLD, TAP }

/** What a hold on the home screen does, as on Android. */
internal class HoldActions(
    val isEnabled: () -> Boolean,
    val carryStart: () -> CarryStart,
    /**
     * The hold was felt, on an item ([key]) or on an empty place (null): a short vibration, as on Android, and a
     * widget is copied as the screen shows it, before anything changes.
     */
    val onHold: (key: String?) -> Unit,
    /** An empty place held (editing starts) or tapped (while editing, the editing ends), as on Android. */
    val onEmpty: (EmptyTouch) -> Unit,
    /** An item held and moved on a locked home screen ([CarryStart.NEVER]). */
    val onMoveWhileLocked: () -> Unit,
    val onDrop: (key: String) -> Unit
)

/**
 * Hold anywhere on the home screen. On an empty place, editing starts. On an item, its menu opens at once, with the
 * finger still down, as on One UI, and moving the finger closes it and carries the item (the moves are taken before
 * the pages see them, so they do not slide under the finger; they turn only at the edges); see [CarryStart].
 *
 * The handler starts at the first touch and lives on, so [actions] must read the screen as it is at each call (see
 * rememberHoldActions), never values taken when it was made: the grid, the editing, the dock change meanwhile.
 */
internal fun Modifier.homeHoldGestures(state: HomeDragState, actions: HoldActions): Modifier = pointerInput(
    state,
    actions
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val held = awaitLongPressOrCancellation(down.id)
        if (held == null) {
            if (actions.isEnabled() && isTapOnEmpty(down, state)) actions.onEmpty(EmptyTouch.TAP)
            return@awaitEachGesture
        }
        if (!actions.isEnabled()) return@awaitEachGesture
        val key = state.places.itemAt(state.origin + held.position)
        actions.onHold(key)
        if (key == null) {
            actions.onEmpty(EmptyTouch.HOLD)
            return@awaitEachGesture
        }
        holdItem(key, held, state, actions)
    }
}

/**
 * The item [key] is held. As on One UI, its menu opens at the hold, with the finger still down (editing carries it at
 * once instead); moving closes the menu and carries the item, or, on a locked home screen, says why it stays.
 */
private suspend fun AwaitPointerEventScope.holdItem(
    key: String,
    held: PointerInputChange,
    state: HomeDragState,
    actions: HoldActions
) {
    val start = actions.carryStart()
    var isCarrying = start == CarryStart.AT_HOLD
    if (isCarrying) state.start(key, held.position) else state.menuKey = key
    val hasMoved = followUntilUp(held) { position, isPastSlop ->
        // Moved: the person wants the item, not the menu.
        if (isPastSlop && state.menuKey == key) state.menuKey = null
        if (isPastSlop && !isCarrying && start == CarryStart.ON_MOVE) {
            isCarrying = true
            state.start(key, held.position)
        }
        if (isCarrying) state.follow(position)
    }
    when {
        isCarrying -> {
            actions.onDrop(key)
            state.end()
        }
        hasMoved -> actions.onMoveWhileLocked()
    }
}

/**
 * The gesture of [down] ended as a tap on an empty place: the finger let go near where it touched, nothing took the
 * tap (a button, an app, a folder) and no item is under it.
 */
private fun AwaitPointerEventScope.isTapOnEmpty(down: PointerInputChange, state: HomeDragState): Boolean {
    val up = currentEvent.changes.firstOrNull { it.id == down.id } ?: return false
    val isTap = up.changedToUpIgnoreConsumed() && !up.isConsumed &&
        (up.position - down.position).getDistance() < viewConfiguration.touchSlop
    return isTap && state.places.itemAt(state.origin + up.position) == null
}

/**
 * Takes every move of the finger of [held] until it lets go, before the pages see it, telling [onMove] where it is and
 * whether it has gone past the touch slop. Returns whether it did.
 */
private suspend fun AwaitPointerEventScope.followUntilUp(
    held: PointerInputChange,
    onMove: (position: Offset, isPastSlop: Boolean) -> Unit
): Boolean {
    var hasMoved = false
    do {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == held.id } ?: break
        hasMoved = hasMoved || (change.position - held.position).getDistance() > viewConfiguration.touchSlop
        onMove(change.position, hasMoved)
        change.consume()
    } while (!change.changedToUpIgnoreConsumed())
    return hasMoved
}
