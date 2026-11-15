package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.LauncherApp

/** Where an app of all the apps is on the screen, read when it is held (to carry it from there). */
private class Placement {
    var coordinates: LayoutCoordinates? = null
}

/**
 * An app of all the apps or of an open folder, the same tile as on the home screen. A tap opens it. Held, it
 * vibrates: moved, what was open goes away and it is carried to the home screen (as on Android); let go without
 * moving, its menu opens, with [menuEntries] (put on the home screen, take out of the folder) between its page and
 * "Desinstalar". The menu is only built when it opens: forty apps on the screen do not each make one they never show.
 */
@Composable
internal fun DrawerApp(
    app: LauncherApp,
    tile: DrawerTile,
    viewModel: LauncherViewModel,
    carry: AppCarry?,
    menuEntries: () -> List<MenuEntry>
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val menuLabel = stringResource(R.string.launcher_more_named, app.label)
    val placement = remember { Placement() }
    val badge = LocalHomeBadges.current.countOf(app)
    // "Máximo": the same tiles as on the home screen, the name always inside (all the apps are found by it).
    val look = tile.tileHeight?.let { rememberAppTile(app, tile.isMonochrome, showsName = true) }
    Box {
        LauncherTile(
            label = app.label,
            description = tileDescription(app.label, badge),
            labelLines = tile.labelLines,
            size = tile.size,
            onOpen = { from -> viewModel.onOpenApp(app, from) },
            tile = look,
            modifier = Modifier
                .then(tile.tileHeight?.let { Modifier.height(it) } ?: Modifier)
                .onGloballyPositioned { placement.coordinates = it }
                .holdToCarry(app.key, placement, onMenu = { isMenuOpen = it }, onHold = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }, carry = carry.takeUnless { tile.isLayoutLocked || !tile.hasHome })
                .semantics {
                    onLongClick(menuLabel) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        isMenuOpen = true
                        true
                    }
                }
        ) { BadgedIcon(badge, tile.icon) { AppIcon(app = app, size = tile.icon, isMonochrome = tile.isMonochrome) } }
        if (isMenuOpen) {
            AppMenu(
                isOpen = true,
                onDismiss = { isMenuOpen = false },
                title = app.label,
                entries = viewModel.appMenu.entriesFor(app, menuEntries()),
                shortcuts = viewModel.menuShortcuts(app, tile.isLayoutLocked, tile.isMonochrome)
            )
        }
    }
}

/**
 * Hold to open the menu (at once, with the finger still down, as on One UI), then move to carry [key] out of all the
 * apps (the menu closes). The moves are taken before the list sees them, so it does not scroll under the finger while
 * the app is carried. Without [carry] (a locked home screen), a hold only opens the menu.
 */
private fun Modifier.holdToCarry(
    key: String,
    placement: Placement,
    onMenu: (isOpen: Boolean) -> Unit,
    onHold: () -> Unit,
    carry: AppCarry?
): Modifier = pointerInput(key, carry) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val held = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        val coordinates = placement.coordinates ?: return@awaitEachGesture
        onHold()
        onMenu(true)
        var isCarrying = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == held.id } ?: break
            val hasMoved = (change.position - held.position).getDistance() > viewConfiguration.touchSlop
            if (carry != null && !isCarrying && hasMoved) {
                isCarrying = true
                onMenu(false)
                carry.onStart(key, coordinates.boundsInRoot(), coordinates.localToRoot(held.position))
            }
            if (isCarrying) carry?.onMove(coordinates.localToRoot(change.position))
            change.consume()
        } while (!change.changedToUpIgnoreConsumed())
        if (isCarrying) carry?.onDrop()
    }
}
