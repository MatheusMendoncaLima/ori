package com.oriteam.ori.ui.components.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.paneTransition

/**
 * Shows [content] for [target] and moves between them as a step forward ([isForward]) or back, with the
 * level of "Animações" the person chose. Used wherever an area opens something over itself: the
 * sub-screens of each area and the detail of a list on a phone.
 *
 * During the change both contents are on screen for a moment; the new one is complete from the start.
 */
@Composable
fun <T> OriPaneTransition(
    target: T,
    isForward: (from: T, to: T) -> Boolean,
    modifier: Modifier = Modifier,
    label: String = "Pane",
    content: @Composable (T) -> Unit
) {
    val motion = LocalOriAppearance.current.motion
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = { paneTransition(motion, isForward(initialState, targetState)).using(null) },
        label = label
    ) { shown -> content(shown) }
}
