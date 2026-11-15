package com.oriteam.ori.ui.components.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.theme.EnteringEasing
import com.oriteam.ori.ui.theme.LocalOriAppearance
import kotlinx.coroutines.launch

private const val ENTRANCE_MILLIS = 320

/** The wait between two items that enter together, so they arrive one after the other. */
private const val STAGGER_MILLIS = 45

/** Items after this many enter together: a long list must not keep the person waiting. */
private const val MAX_STAGGERED = 6

private const val POP_INITIAL_SCALE = 0.7f

private val RiseDistance = 12.dp

/** How an item arrives the first time it is shown. */
enum class EntranceStyle {
    /** Fades in while rising a little: cards of a screen. */
    RISE,

    /** Fades in while growing from a little smaller: a word added to the message. */
    POP
}

/**
 * Makes an item arrive when it is first attached to the screen. [order] staggers items that arrive together.
 * The layout is final from the first frame and only the drawing changes, inside the node's own layer, so
 * nothing recomposes while it plays. Reduced motion keeps only a short fade; with animation off, or when
 * [isEnabled] is false, the item is simply there.
 */
fun Modifier.entrance(order: Int = 0, style: EntranceStyle = EntranceStyle.RISE, isEnabled: Boolean = true): Modifier =
    if (isEnabled) this then EntranceElement(order, style) else this

private data class EntranceElement(val order: Int, val style: EntranceStyle) : ModifierNodeElement<EntranceNode>() {
    override fun create() = EntranceNode(order, style)

    // An item arrives once: a new order or style later does not replay it.
    override fun update(node: EntranceNode) = Unit
}

private class EntranceNode(
    private val order: Int,
    private val style: EntranceStyle
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private val progress = Animatable(0f)
    private var movesThings = false
    private var hasStarted = false

    /** With animation off the item is complete from its first frame, not one frame later. */
    private var isImmediate = false

    /** Decided once, the first time the item is measured: an entrance is not replayed if the level changes. */
    private fun start() {
        hasStarted = true
        val motion = currentValueOf(LocalOriAppearance).motion
        movesThings = motion.movesThings
        val duration = motion.millis(ENTRANCE_MILLIS)
        isImmediate = duration == 0
        if (isImmediate) return
        coroutineScope.launch {
            val delay = motion.millis(STAGGER_MILLIS) * order.coerceAtMost(MAX_STAGGERED)
            progress.animateTo(1f, tween(duration, delay, EnteringEasing))
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        if (!hasStarted) start()
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val value = if (isImmediate) 1f else progress.value
                alpha = value
                if (movesThings) {
                    when (style) {
                        EntranceStyle.RISE -> translationY = (1f - value) * RiseDistance.toPx()
                        EntranceStyle.POP -> {
                            val scale = POP_INITIAL_SCALE + (1f - POP_INITIAL_SCALE) * value
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                }
            }
        }
    }
}
