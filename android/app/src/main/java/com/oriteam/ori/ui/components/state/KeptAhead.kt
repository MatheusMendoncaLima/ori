package com.oriteam.ori.ui.components.state

import android.os.Looper
import android.os.MessageQueue
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.oriteam.ori.ui.theme.LocalOriAppearance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/*
 * Parts of a screen made ahead and kept made while they are not shown, so that showing them is only placing them:
 * composing a list of forty items when it is asked for took 60 to 140 ms on the test tablet (all the apps of ORI Home,
 * M18; "Montar" of the communication, M16).
 */

/**
 * Keeps what this modifies made while [isShown] is false: measured, but not placed, so it is neither drawn nor
 * touched, and hidden from the screen reader. Shown again, nothing of it is composed again.
 */
fun Modifier.outOfSightUnless(isShown: Boolean): Modifier = if (isShown) {
    this
} else {
    this
        .clearAndSetSemantics { }
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { }
        }
}

/** Under a sub-screen, as the steps of a sub-screen in [com.oriteam.ori.ui.theme.paneTransition]. */
private const val COVER_FADE_OUT_MILLIS = 100
private const val COVER_FADE_IN_MILLIS = 200

/**
 * [content] stays made while something covers it ([isShown] false, a sub-screen opened over it): it fades out as the
 * cover comes in and then leaves the screen as with [outOfSightUnless]; it is placed again at once, and fades in,
 * when the cover goes. Only read by the screen reader while shown. A field of it lets go of the keyboard when
 * covered.
 */
@Composable
fun KeptUnder(isShown: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val motion = LocalOriAppearance.current.motion
    val visibility = remember { Animatable(if (isShown) 1f else 0f) }
    val focus = LocalFocusManager.current
    LaunchedEffect(isShown) {
        if (!isShown) focus.clearFocus()
        val millis = if (isShown) COVER_FADE_IN_MILLIS else COVER_FADE_OUT_MILLIS
        visibility.animateTo(if (isShown) 1f else 0f, motion.feedback(millis))
    }
    val isStillVisible by remember { derivedStateOf { visibility.value > 0f } }
    Box(
        modifier = modifier
            .graphicsLayer { alpha = visibility.value }
            .then(if (isShown) Modifier else Modifier.clearAndSetSemantics { })
            .outOfSightUnless(isShown || isStillVisible)
    ) { content() }
}

/**
 * False until the screen has rested once (nothing left to compose, no animation running, no message waiting on the
 * main thread), then true for good: the moment to make ahead what is not shown yet, without a long frame in the
 * middle of a movement.
 */
@Composable
fun rememberScreenRested(): Boolean {
    var hasRested by remember { mutableStateOf(false) }
    val view = LocalView.current
    LaunchedEffect(view) {
        (view.findViewTreeCompositionContext() as? Recomposer)
            ?.currentState
            ?.first { it == Recomposer.State.Idle }
        awaitMainThreadIdle()
        hasRested = true
    }
    return hasRested
}

private suspend fun awaitMainThreadIdle() = suspendCancellableCoroutine { continuation ->
    val queue = Looper.myQueue()
    val handler = MessageQueue.IdleHandler {
        continuation.resume(Unit)
        false
    }
    queue.addIdleHandler(handler)
    continuation.invokeOnCancellation { queue.removeIdleHandler(handler) }
}
