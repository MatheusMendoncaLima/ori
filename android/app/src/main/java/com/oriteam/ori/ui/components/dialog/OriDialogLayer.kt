package com.oriteam.ori.ui.components.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing

/** How dark the screen gets under a dialog: the same as under the dialog windows of Android (60% black). */
private const val SCRIM_ALPHA = 0.6f
private const val SCRIM_MILLIS = 150

/** Where a dialog sits: a card in the middle of the screen, or the whole screen (the message in large letters). */
enum class DialogPlacement { CENTERED, FULL_SCREEN }

/**
 * The dialogs open over one screen of ORI. [OriDialogHost] keeps it; [OriDialog] adds to it while it is composed.
 *
 * A dialog of Android (`AlertDialog`, `Dialog`) is a window of its own, and Android created that window at every
 * opening: 50 to 90 ms with the main thread waiting for the system, on the test tablet (M17 in
 * docs/revisao-de-codigo.md). Drawn here, in the window of the screen, a dialog costs what its content costs.
 */
@Stable
class DialogLayer internal constructor() {
    internal val shown = mutableStateListOf<ShownDialog>()
}

/** A dialog asked for: what the screen that asked passed last, read where the dialog is drawn. */
@Stable
internal class ShownDialog(val placement: DialogPlacement) {
    var paneTitle by mutableStateOf("")
    var onDismissRequest by mutableStateOf({})

    /** The values of the screen that asked (theme, colors, view models): the dialog is drawn with them. */
    var locals by mutableStateOf<CompositionLocalContext?>(null)
    var content by mutableStateOf<@Composable () -> Unit>({})
}

val LocalDialogLayer = staticCompositionLocalOf<DialogLayer> {
    error("A dialog of ORI needs OriDialogHost around the screen")
}

/**
 * Shows [content] over the screen while this is composed, as `Dialog` does, in the window of the screen itself.
 * [paneTitle] is what the screen reader says when it opens. Back and a touch outside (with [DialogPlacement.CENTERED])
 * ask to close it ([onDismissRequest]); what is under it can be neither touched nor read until it closes.
 */
@Composable
fun OriDialog(
    onDismissRequest: () -> Unit,
    paneTitle: String,
    placement: DialogPlacement = DialogPlacement.CENTERED,
    content: @Composable () -> Unit
) {
    val layer = LocalDialogLayer.current
    val locals = currentCompositionLocalContext
    val dialog = remember(placement) { ShownDialog(placement) }
    // After each composition: the dialog shows what the screen that asked for it has now.
    SideEffect {
        dialog.paneTitle = paneTitle
        dialog.onDismissRequest = onDismissRequest
        dialog.locals = locals
        dialog.content = content
    }
    val focus = LocalFocusManager.current
    DisposableEffect(layer, dialog) {
        // A field under the dialog lets go of the keyboard, as when a dialog window takes the focus.
        focus.clearFocus()
        layer.shown += dialog
        onDispose { layer.shown -= dialog }
    }
}

/**
 * The screen ([content]) with its dialogs over it. One per window of ORI (MainActivity, LauncherActivity), around
 * everything, so a dialog covers the whole window. While a dialog is open, the screen under it is hidden from the
 * screen reader, as under a dialog window.
 */
@Composable
fun OriDialogHost(content: @Composable () -> Unit) {
    val layer = remember { DialogLayer() }
    CompositionLocalProvider(LocalDialogLayer provides layer) {
        Box(propagateMinConstraints = true) {
            val isCovered = layer.shown.isNotEmpty()
            Box(
                propagateMinConstraints = true,
                modifier = if (isCovered) Modifier.clearAndSetSemantics { } else Modifier
            ) { content() }
            layer.shown.forEach { dialog -> key(dialog) { DialogFrame(dialog) } }
        }
    }
}

@Composable
private fun DialogFrame(dialog: ShownDialog) {
    // Composed after the screen, so it is the last one registered: back closes the dialog first.
    BackHandler { dialog.onDismissRequest() }
    val motion = LocalOriAppearance.current.motion
    val shade = remember { Animatable(0f) }
    LaunchedEffect(shade) { shade.animateTo(1f, motion.feedback(SCRIM_MILLIS)) }
    val scrim = MaterialTheme.colorScheme.scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            // The shade fades in a layer of its own, its opacity put on the color itself (no picture of the whole
            // screen made aside at every frame of the fade, which kept the GPU 10 to 14 ms per frame on the tablet).
            .graphicsLayer {
                alpha = shade.value
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .background(scrim.copy(alpha = SCRIM_ALPHA))
            .pointerInput(dialog) { closeOnTouch(dialog) }
    )
    val locals = dialog.locals ?: return
    val placement = when (dialog.placement) {
        DialogPlacement.CENTERED ->
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(OriSpacing.Large)
        DialogPlacement.FULL_SCREEN -> Modifier.fillMaxSize()
    }
    CompositionLocalProvider(locals) {
        Box(contentAlignment = Alignment.Center, modifier = placement) {
            Box(
                modifier = Modifier
                    .semantics {
                        dialog()
                        paneTitle = dialog.paneTitle
                    }
                    // Hit here, a touch stays in the dialog and never reaches the shade, which would close it.
                    .pointerInput(Unit) { }
            ) { dialog.content() }
        }
    }
}

/**
 * A touch on the shade around the dialog closes it when the finger leaves the screen, as Android does, and goes
 * nowhere else: nothing under the dialog is touched.
 */
private suspend fun PointerInputScope.closeOnTouch(dialog: ShownDialog) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false).consume()
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
    if (dialog.placement == DialogPlacement.CENTERED) dialog.onDismissRequest()
}
