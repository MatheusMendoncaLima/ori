package com.oriteam.ori.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** A flick faster than this opens or closes all the apps, whatever the distance (as on Android). */
private val FlingSpeed = 500.dp

/** Let go without a flick, all the apps open when they came up more than this part of the screen. */
private const val OPEN_SHARE = 0.4f
private const val SETTLE_MILLIS = 300

/**
 * How far all the apps have come up, from 0 (closed) to 1 (open). The finger moves it directly, so the list follows
 * the finger up and down; let go, it settles open or closed by the speed of the flick, with a spring (or a fade when
 * the person reduced motion, or at once with motion off). [onSettled] tells the screen where it ended.
 */
@Stable
internal class DrawerSheet(
    private val scope: CoroutineScope,
    private val onSettled: (isOpen: Boolean) -> Unit
) {
    private val progress = Animatable(0f)
    internal var heightPx = 1f
    internal var flingPx = 1f
    internal var motion = OriMotion()

    /** Read while drawing, never in composition, so the screen does not recompose on every pixel. */
    val value: Float get() = progress.value

    /** Whether any part of the list is on the screen (read in composition, it changes only at the ends). */
    val isVisible: Boolean by derivedStateOf { progress.value > 0f }

    fun dragBy(deltaY: Float) {
        scope.launch { progress.snapTo((progress.value - deltaY / heightPx).coerceIn(0f, 1f)) }
    }

    /** The finger let go with [velocityY] (pixels per second, positive downward). */
    fun settle(velocityY: Float) {
        val isOpen = when {
            velocityY < -flingPx -> true
            velocityY > flingPx -> false
            else -> progress.value > OPEN_SHARE
        }
        onSettled(isOpen)
        animateTo(isOpen, initialVelocity = -velocityY / heightPx)
    }

    /** Opened or closed from elsewhere (the button, back, an app opened). */
    fun animateTo(isOpen: Boolean, initialVelocity: Float = 0f) {
        val target = if (isOpen) 1f else 0f
        if (progress.value == target && !progress.isRunning) return
        scope.launch { progress.animateTo(target, spec(), initialVelocity) }
    }

    private fun spec(): AnimationSpec<Float> = if (motion.movesThings) {
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    } else {
        motion.feedback(SETTLE_MILLIS)
    }
}

@Composable
internal fun rememberDrawerSheet(heightPx: Float, onSettled: (isOpen: Boolean) -> Unit): DrawerSheet {
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onSettled)
    val sheet = remember { DrawerSheet(scope) { latest.value(it) } }
    sheet.heightPx = heightPx.coerceAtLeast(1f)
    sheet.flingPx = with(LocalDensity.current) { FlingSpeed.toPx() }
    sheet.motion = LocalOriAppearance.current.motion
    return sheet
}

/**
 * Connects the list of all the apps to the sheet: a drag down at the top of the list takes the apps down with the
 * finger, and letting go settles the sheet.
 */
internal fun DrawerSheet.connection(): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || value >= 1f) return Offset.Zero
        dragBy(available.y)
        return Offset(0f, available.y)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
        dragBy(available.y)
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (value >= 1f) return Velocity.Zero
        settle(available.y)
        return available
    }
}

/**
 * [state] while all the apps are shown. Closed, they follow only what changes the list (the apps, the settings),
 * so the list made ahead is the one that will show; another change of the home screen (a badge, a message, the
 * pane) does not redo a list nobody sees, and comes up to date in the frame it starts to show.
 */
@Composable
internal fun rememberStateWhileShown(state: LauncherUiState, isShown: Boolean): LauncherUiState {
    val shown = remember { arrayOf(state) }
    val kept = shown[0]
    if (isShown || kept.drawer != state.drawer || kept.settings != state.settings) shown[0] = state
    return shown[0]
}
