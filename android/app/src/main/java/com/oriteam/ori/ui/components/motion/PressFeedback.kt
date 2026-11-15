package com.oriteam.ori.ui.components.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import com.oriteam.ori.ui.theme.LocalOriAppearance
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** How much a row, a card or a button sinks while pressed. */
const val PRESSED_SCALE = 0.97f

/** A symbol of the boards sinks a little more: it is the main thing the person taps. */
const val PRESSED_SYMBOL_SCALE = 0.93f

private const val PRESS_RELEASE_MILLIS = 180

/**
 * Makes what is pressed sink a little and come back, so a tap feels answered even before anything else
 * changes. Pass the same [interactionSource] given to the `clickable`.
 *
 * It only changes how the content is drawn, inside its own node: no layout and no recomposition per frame,
 * which matters in a grid of 60 symbols. Nothing moves when the person reduced or turned off motion.
 */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = PRESSED_SCALE): Modifier =
    this then PressScaleElement(interactionSource, pressedScale)

private data class PressScaleElement(
    val interactionSource: InteractionSource,
    val pressedScale: Float
) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(interactionSource, pressedScale)

    override fun update(node: PressScaleNode) = node.update(interactionSource, pressedScale)
}

private class PressScaleNode(
    private var interactionSource: InteractionSource,
    private var pressedScale: Float
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val scale = Animatable(1f)
    private var listening: Job? = null

    override fun onAttach() = listen()

    fun update(source: InteractionSource, scaleWhilePressed: Float) {
        pressedScale = scaleWhilePressed
        if (source != interactionSource) {
            interactionSource = source
            listening?.cancel()
            listen()
        }
    }

    private fun listen() {
        listening = coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                }
                follow(isPressed = presses.isNotEmpty())
            }
        }
    }

    private fun follow(isPressed: Boolean) {
        val motion = currentValueOf(LocalOriAppearance).motion
        coroutineScope.launch {
            when {
                !motion.movesThings -> scale.snapTo(1f)
                // Sinking is immediate; coming back has a little life in the expressive level.
                isPressed -> scale.animateTo(pressedScale, spring(stiffness = Spring.StiffnessHigh))
                else -> scale.animateTo(1f, motion.playful(PRESS_RELEASE_MILLIS))
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val value = scale.value
        if (value == 1f) drawContent() else scale(value) { this@draw.drawContent() }
    }
}
