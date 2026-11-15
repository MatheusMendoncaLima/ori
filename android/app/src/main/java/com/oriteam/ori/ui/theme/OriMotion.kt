package com.oriteam.ori.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset
import com.oriteam.ori.domain.preferences.MotionLevel
import kotlin.math.roundToInt

private const val SUBTLE_DURATION_SHARE = 0.6f
private const val REDUCED_DURATION_SHARE = 0.4f

/**
 * The curves of Material motion. A change starts quickly and settles slowly, so it feels light and never
 * ends with a jolt; leaving accelerates, because what goes away needs no attention.
 */
internal val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
internal val EnteringEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
internal val LeavingEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/**
 * The animation choices of the app, so screens ask for a kind of motion instead of a fixed one.
 * A snap still changes the value at once: turning animation off never hides information.
 *
 * [level] is already resolved: [MotionLevel.SYSTEM] does not reach this class.
 *
 * | Level | Ornament | Change notices | Movement (slide, grow, press) | Bounce |
 * |---|---|---|---|---|
 * | Expressive | full | full | yes | yes |
 * | Subtle | 60% | 60% | yes | no |
 * | Reduced | none | 40%, fades only | no | no |
 * | Off | none | none | no | no |
 */
@Immutable
data class OriMotion(val level: MotionLevel = MotionLevel.EXPRESSIVE) {

    /** Ornament: only present while motion is welcome. */
    fun <T> decorative(durationMillis: Int): FiniteAnimationSpec<T> = when (level) {
        MotionLevel.EXPRESSIVE, MotionLevel.SYSTEM -> tween(durationMillis, easing = StandardEasing)
        MotionLevel.SUBTLE -> tween((durationMillis * SUBTLE_DURATION_SHARE).roundToInt(), easing = StandardEasing)
        MotionLevel.REDUCED, MotionLevel.OFF -> snap()
    }

    /** Helps to notice a change (a message appears, a field gets focus): the last one to go. */
    fun <T> feedback(durationMillis: Int): FiniteAnimationSpec<T> =
        if (level == MotionLevel.OFF) snap() else tween(millis(durationMillis), easing = StandardEasing)

    /** The bouncy version exists only in the expressive level. */
    fun <T> playful(durationMillis: Int): FiniteAnimationSpec<T> = when (level) {
        // Fast and lively: a small thing settles in about a quarter of a second, with one small bounce.
        MotionLevel.EXPRESSIVE, MotionLevel.SYSTEM -> spring(dampingRatio = FAST_DAMPING, stiffness = FAST_STIFFNESS)
        else -> decorative(durationMillis)
    }

    /** Whether a jump to another position travels there or goes straight to it. */
    val animatesScrolling: Boolean
        get() = level != MotionLevel.REDUCED && level != MotionLevel.OFF

    /**
     * Whether things may travel, grow or shrink on screen: what people who reduce motion ask to remove.
     * Without it, changes still fade (reduced) or happen at once (off), so nothing is lost.
     */
    val movesThings: Boolean
        get() = level == MotionLevel.EXPRESSIVE || level == MotionLevel.SUBTLE || level == MotionLevel.SYSTEM

    /** [durationMillis] at this level: shorter when subtle or reduced, zero when off. */
    fun millis(durationMillis: Int): Int = when (level) {
        MotionLevel.EXPRESSIVE, MotionLevel.SYSTEM -> durationMillis
        MotionLevel.SUBTLE -> (durationMillis * SUBTLE_DURATION_SHARE).roundToInt()
        MotionLevel.REDUCED -> (durationMillis * REDUCED_DURATION_SHARE).roundToInt()
        MotionLevel.OFF -> 0
    }

    /** How an item of a list fades in or out; null (no fade) when animation is off. */
    fun itemFade(): FiniteAnimationSpec<Float>? =
        if (level == MotionLevel.OFF) null else tween(millis(LIST_FADE_MILLIS), easing = StandardEasing)

    /** How the other items of a list slide to their new places; null when nothing should move. */
    fun itemPlacement(): FiniteAnimationSpec<IntOffset>? = if (movesThings) {
        spring(PLACEMENT_DAMPING, PLACEMENT_STIFFNESS, IntOffset.VisibilityThreshold)
    } else {
        null
    }

    companion object {
        private const val LIST_FADE_MILLIS = 220

        // Material motion springs: "fast spatial" for small elements, "default spatial" for items of a list.
        private const val FAST_DAMPING = 0.6f
        private const val FAST_STIFFNESS = 800f
        private const val PLACEMENT_DAMPING = 0.9f
        private const val PLACEMENT_STIFFNESS = 700f

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
