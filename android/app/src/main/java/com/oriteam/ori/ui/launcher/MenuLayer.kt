package com.oriteam.ori.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Where the menus of ORI Home are drawn: over everything, in the window of the home screen itself.
 *
 * A menu in a window of its own (a `Popup`) made Android create that window at every hold: about 90 ms with the main
 * thread stopped, three frames late each time, measured on the test tablet (M15 in docs/revisao-de-codigo.md). Drawn
 * here, a menu costs what any other layer of the home screen costs. An item asks for its menu with [MenuRequest] (the
 * way [AppMenu] does); [MenuLayerHost], at the top of [LauncherScreen], draws it next to the item.
 */
@Stable
internal class MenuLayer {
    var shown: ShownMenu? by mutableStateOf(null)
}

/** A menu asked for: what it shows ([menu]), next to [anchor] (the item, in window coordinates). */
@Stable
internal class ShownMenu {
    var anchor: IntRect? by mutableStateOf(null)
    var menu: MenuContent? by mutableStateOf(null)
}

/**
 * What a menu shows: the name of what was held ([title]), the shortcuts of the app and the choices. Kept as data, not
 * as a piece of interface: a name that arrives later (a widget's "Clima") changes the data, and the card follows.
 */
internal data class MenuContent(
    val entries: List<MenuEntry>,
    val title: String?,
    val shortcuts: MenuShortcuts?,
    val onDismiss: () -> Unit
)

internal val LocalMenuLayer = staticCompositionLocalOf<MenuLayer> {
    error("A menu of ORI Home needs MenuLayerHost")
}

/**
 * Shows [content] next to the layout this is placed in (the item held), as a `Popup` takes its parent as its anchor,
 * until it leaves the composition (the menu closed, or the item gone). One menu at a time: a new one replaces it.
 */
@Composable
internal fun MenuRequest(content: MenuContent) {
    val layer = LocalMenuLayer.current
    val shown = remember { ShownMenu() }
    SideEffect { shown.menu = content }
    DisposableEffect(layer, shown) {
        layer.shown = shown
        onDispose { if (layer.shown === shown) layer.shown = null }
    }
    // An empty point in the item: its parent is the item, wherever it is (a page, all the apps, a folder).
    Spacer(
        modifier = Modifier.onGloballyPositioned { point ->
            shown.anchor = point.parentLayoutCoordinates?.boundsInRoot()?.roundToIntRect()
        }
    )
}

/**
 * Draws the menu asked for next to its item ([AboveFirst]: above it, or under it without room), over everything. A
 * touch anywhere else only closes it, and so does back, as with the menu window before.
 */
@Composable
internal fun MenuLayerHost(layer: MenuLayer) {
    val shown = layer.shown ?: return
    val menu = shown.menu ?: return
    val anchor = shown.anchor ?: return
    BackHandler { menu.onDismiss() }
    val density = LocalDensity.current
    val bars = WindowInsets.safeDrawing
    val top = bars.getTop(density)
    val bottom = bars.getBottom(density)
    val gap = with(density) { OriSpacing.Small.roundToPx() }
    val position = remember(top, bottom, gap) { AboveFirst(top = top, bottom = bottom, gap = gap) }
    var origin by remember { mutableStateOf(IntOffset.Zero) }
    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot().round() }) {
        // Under the card, so a touch on it reaches its choices.
        Box(modifier = Modifier.fillMaxSize().pointerInput(shown) { closeOnTouch(shown) })
        Layout(content = { MenuCard(menu) }) { measurables, constraints ->
            val card = measurables.firstOrNull()?.measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(constraints.maxWidth, constraints.maxHeight) {
                card ?: return@layout
                val place = position.calculatePosition(
                    anchorBounds = anchor.translate(-origin),
                    windowSize = IntSize(constraints.maxWidth, constraints.maxHeight),
                    layoutDirection = layoutDirection,
                    popupContentSize = IntSize(card.width, card.height)
                )
                card.place(place)
            }
        }
    }
}

/** A touch outside the card closes the menu and goes nowhere else: it does not open the app under it. */
private suspend fun PointerInputScope.closeOnTouch(shown: ShownMenu) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false).consume()
    shown.menu?.onDismiss?.invoke()
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
}
