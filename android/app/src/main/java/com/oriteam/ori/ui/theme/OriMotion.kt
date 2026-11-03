package com.oriteam.ori.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import com.oriteam.ori.domain.preferences.MotionLevel
import kotlin.math.roundToInt

private const val SUBTLE_DURATION_SHARE = 0.6f
private const val REDUCED_DURATION_SHARE = 0.4f

/**
 * The animation choices of the app, so screens ask for a kind of motion instead of a fixed one.
 * A snap still changes the value at once: turning animation off never hides information.
 *
 * [level] is already resolved: [MotionLevel.SYSTEM] does not reach this class.
 */
@Immutable
data class OriMotion(val level: MotionLevel = MotionLevel.EXPRESSIVE) {

    /** Ornament: only present while motion is welcome. */
    fun <T> decorative(durationMillis: Int): FiniteAnimationSpec<T> = when (level) {
        MotionLevel.EXPRESSIVE, MotionLevel.SYSTEM -> tween(durationMillis)
        MotionLevel.SUBTLE -> tween((durationMillis * SUBTLE_DURATION_SHARE).roundToInt())
        MotionLevel.REDUCED, MotionLevel.OFF -> snap()
    }

    /** Helps to notice a change (a message appears, a field gets focus): the last one to go. */
    fun <T> feedback(durationMillis: Int): FiniteAnimationSpec<T> = when (level) {
        MotionLevel.EXPRESSIVE, MotionLevel.SYSTEM -> tween(durationMillis)
        MotionLevel.SUBTLE -> tween((durationMillis * SUBTLE_DURATION_SHARE).roundToInt())
        MotionLevel.REDUCED -> tween((durationMillis * REDUCED_DURATION_SHARE).roundToInt())
        MotionLevel.OFF -> snap()
    }

    /** The bouncy version exists only in the expressive level. */
    fun <T> playful(durationMillis: Int): FiniteAnimationSpec<T> = when (level) {
        MotionLevel.EXPRESSIVE, MotionLevel.SYSTEM -> spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
        else -> decorative(durationMillis)
    }

    /** Whether a jump to another position travels there or goes straight to it. */
    val animatesScrolling: Boolean
        get() = level != MotionLevel.REDUCED && level != MotionLevel.OFF

    companion object {
        /** [MotionLevel.SYSTEM] becomes "off" when the system removed animations. */
        fun resolve(preference: MotionLevel, systemRemovedAnimations: Boolean) = OriMotion(
            when {
                preference != MotionLevel.SYSTEM -> preference
                systemRemovedAnimations -> MotionLevel.OFF
                else -> MotionLevel.EXPRESSIVE
            }
        )
    }
}
