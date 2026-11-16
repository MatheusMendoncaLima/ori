package com.oriteam.ori.ui.components.form

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Below this height, with the keyboard open, the action bar would leave the fields only a strip (the tablet lying
 * down, G3 and P6 of the general test): "Cancelar" ended up where the field being typed had been.
 */
private val RoomForActionBar = 440.dp

/**
 * A form with its title bar on top, the fields in [content] (give the scrolling part `Modifier.weight(1f)`) and its
 * [actionBar] at the bottom, above the keyboard. When the keyboard leaves little room, the bar waits for it to
 * close: the fields get the space, and a tap meant for a field never lands on "Cancelar". The system "back" closes
 * the keyboard first, so the bar is always one step away.
 */
@Composable
fun FormLayout(
    topBar: @Composable () -> Unit,
    actionBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime)) {
        val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val showsBar = showsActionBar(isKeyboardOpen, maxHeight)
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            content()
            if (showsBar) actionBar()
        }
    }
}

/** The bar shows unless the keyboard is open and the form has less than [RoomForActionBar] left. */
internal fun showsActionBar(isKeyboardOpen: Boolean, formHeight: Dp): Boolean =
    !isKeyboardOpen || formHeight >= RoomForActionBar
