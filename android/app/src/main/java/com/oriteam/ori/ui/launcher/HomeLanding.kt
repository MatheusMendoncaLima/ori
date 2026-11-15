package com.oriteam.ori.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.oriteam.ori.ui.theme.LocalOriAppearance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** How long a dropped item travels from where it was let go to its place. */
private const val LAND_MILLIS = 220

/** How long a drop waits for the home screen to write the new place, at most (a write takes a moment on a device). */
internal const val LANDING_WAIT_MILLIS = 2_000L

/**
 * An item let go: where the copy that followed the finger was ([topLeftInRoot]), where the item itself was before
 * ([from]; null for one that was not on the home screen, as from all the apps), whether the drop moves it
 * ([expectsMove]; otherwise it goes back to its place) and the app or folder it goes into ([into]; null for a cell).
 */
internal data class Landing(
    val key: String,
    val topLeftInRoot: Offset,
    val from: Offset?,
    val expectsMove: Boolean,
    val into: String? = null
)

/** The item [key] is carried, or its copy is still where it was let go: in its place it shows faint. */
internal fun HomeDragState.isCarriedOrLanding(key: String): Boolean = draggedKey == key || landing?.key == key

/** Where an item is laid out, before it is shifted on the screen: read by the landing (not drawn). */
private class LandingPlace {
    var coordinates: LayoutCoordinates? = null
    var topLeft by mutableStateOf<Offset?>(null)

    fun update(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        val position = coordinates.positionInRoot()
        if (topLeft != position) topLeft = position
    }
}

/**
 * A dropped item travels from where it was let go to its place, as on One UI: not from where it was before the drop.
 * The copy that followed the finger stays where it was let go ([HomeDragState.landing]) until the item is in its new
 * place (or right away, when the drop does not move it: it goes back from there); then the item takes over, shifted
 * to that spot, and slides home. Without motion that moves things, it is simply in its place.
 */
@Composable
internal fun Modifier.landsWhereDropped(key: String, drag: HomeDragState): Modifier {
    val motion = LocalOriAppearance.current.motion
    val place = remember { LandingPlace() }
    val shift = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    // An item that goes into a folder does not take over from its copy: IntoFolderLanding ends that landing.
    val landing = drag.landing?.takeIf { it.key == key && it.into == null }
    LaunchedEffect(landing) {
        if (landing == null) return@LaunchedEffect
        if (landing.expectsMove) {
            withTimeoutOrNull(LANDING_WAIT_MILLIS) {
                snapshotFlow { place.topLeft }.first { it != null && it != landing.from }
            }
        }
        val coordinates = place.coordinates?.takeIf { it.isAttached }
        if (coordinates != null && motion.movesThings) {
            shift.snapTo(coordinates.localPositionOf(coordinates.findRootCoordinates(), landing.topLeftInRoot))
        }
        drag.finishLanding(landing)
        // Apart from this effect, which starts again when the landing ends: the slide goes on to the end.
        scope.launch { shift.animateTo(Offset.Zero, motion.feedback(LAND_MILLIS)) }
    }
    return onGloballyPositioned(place::update).graphicsLayer {
        translationX = shift.value.x
        translationY = shift.value.y
    }
}

/**
 * A copy let go over an app or folder ([Landing.into]) stays over it, with the folder drawn around it, until the home
 * screen shows the app in the folder: the moment the change takes to be written (at most [LANDING_WAIT_MILLIS]).
 * Gone at once, the app showed again in its old place for that moment and the folder lost its outline: both blinked.
 */
@Composable
internal fun IntoFolderLanding(context: HomeContext) {
    val drag = context.drag
    val landing = drag.landing?.takeIf { it.into != null }
    val home by rememberUpdatedState(context.state.home)
    LaunchedEffect(landing) {
        val into = landing?.into ?: return@LaunchedEffect
        withTimeoutOrNull(LANDING_WAIT_MILLIS) {
            snapshotFlow { home.holdsTogether(landing.key, into) }.first { it }
        }
        drag.finishLanding(landing)
    }
}

/** A folder of the pages has [key], and is [target] or has it too (the app it was dropped on). */
private fun List<HomeEntry>.holdsTogether(key: String, target: String): Boolean =
    filterIsInstance<HomeEntry.Folder>().any { folder ->
        val keys = folder.apps.map { it.key }
        key in keys && (folder.key == target || target in keys)
    }
