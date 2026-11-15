package com.oriteam.ori.ui.components.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.oriteam.ori.ui.components.motion.OriPaneTransition

/**
 * The sub-screen an area has open on top of itself, the same way in every area: one at a time, kept
 * across rotation and the system closing the app in the background, and closed by the system back
 * gesture. A screen opened inside it registers its own "back" later, so the innermost one closes first.
 */
@Stable
class OpenSubscreen<T : Any> internal constructor(private val state: MutableState<T?>) {
    val current: T? get() = state.value

    fun open(subscreen: T) {
        state.value = subscreen
    }

    fun close() {
        state.value = null
    }
}

/**
 * [T] must fit in a Bundle, as an enum does. [keys] start over with nothing open, for example when the
 * area is shown for another person.
 */
@Composable
fun <T : Any> rememberOpenSubscreen(vararg keys: Any?): OpenSubscreen<T> {
    val state = rememberSaveable(*keys) { mutableStateOf<T?>(null) }
    val subscreen = remember(state) { OpenSubscreen(state) }
    BackHandler(enabled = subscreen.current != null) { subscreen.close() }
    return subscreen
}

/**
 * Shows the area itself ([content] with null) or its open sub-screen. Opening moves forward and closing
 * moves back, as every step in and out of something in the app.
 */
@Composable
fun <T : Any> SubscreenHost(
    subscreen: OpenSubscreen<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T?) -> Unit
) = OriPaneTransition(
    target = subscreen.current,
    isForward = { _, to -> to != null },
    modifier = modifier,
    label = "Subscreen",
    content = content
)
