package com.oriteam.ori.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/*
 * The transitions of the app, built from [OriMotion] so every screen moves the same way and every level of
 * "Animações" is respected: expressive and subtle move and fade, reduced only fades, off changes at once.
 * Durations follow Material motion: what leaves goes fast, what arrives takes a little longer.
 */

private const val PANE_MILLIS = 300
private const val PANE_FADE_IN_MILLIS = 200
private const val PANE_FADE_OUT_MILLIS = 100

/** A sub-screen travels a tenth of the width: enough to show the direction, not a full slide to follow. */
private const val PANE_SHIFT_DIVISOR = 10

/** Short: the new area enters at once, and two different screens should barely be seen together. */
private const val THROUGH_OUT_MILLIS = 50
private const val THROUGH_IN_MILLIS = 200
private const val THROUGH_INITIAL_SCALE = 0.98f

private const val SWAP_OUT_MILLIS = 90
private const val SWAP_IN_MILLIS = 200

private const val APPEAR_MILLIS = 220
private const val DISAPPEAR_MILLIS = 150

private val none: ContentTransform get() = EnterTransition.None togetherWith ExitTransition.None

/**
 * One step into a sub-screen ([isForward]) or back out of it, along the reading direction (shared axis).
 * Nothing on the screen is hidden while it moves: the new content is complete from the first frame.
 */
fun <S> AnimatedContentTransitionScope<S>.paneTransition(motion: OriMotion, isForward: Boolean): ContentTransform {
    if (motion.millis(PANE_MILLIS) == 0) return none
    val fadeIn = fadeIn(tween(motion.millis(PANE_FADE_IN_MILLIS), easing = EnteringEasing))
    val fadeOut = fadeOut(tween(motion.millis(PANE_FADE_OUT_MILLIS), easing = LeavingEasing))
    if (!motion.movesThings) return fadeIn togetherWith fadeOut
    val towards = if (isForward) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    val slide = tween<IntOffset>(motion.millis(PANE_MILLIS), easing = StandardEasing)
    return (fadeIn + slideIntoContainer(towards, slide) { it / PANE_SHIFT_DIVISOR }) togetherWith
        (fadeOut + slideOutOfContainer(towards, slide) { it / PANE_SHIFT_DIVISOR })
}

/**
 * Between places that are not one inside the other (the main areas): the old one fades quickly, the new one
 * fades and grows in at once, so the tap is answered on the next frame.
 */
fun <S> AnimatedContentTransitionScope<S>.fadeThrough(motion: OriMotion): ContentTransform {
    if (motion.millis(THROUGH_IN_MILLIS) == 0) return none
    val leaving = motion.millis(THROUGH_OUT_MILLIS)
    val enterSpec = tween<Float>(motion.millis(THROUGH_IN_MILLIS), easing = EnteringEasing)
    val enter = if (motion.movesThings) {
        fadeIn(enterSpec) + scaleIn(enterSpec, initialScale = THROUGH_INITIAL_SCALE)
    } else {
        fadeIn(enterSpec)
    }
    return enter togetherWith fadeOut(tween(leaving, easing = LeavingEasing))
}

/** The same place shows something else (loading, then the content): a quiet cross-fade. */
fun <S> AnimatedContentTransitionScope<S>.contentSwap(motion: OriMotion): ContentTransform {
    if (motion.millis(SWAP_IN_MILLIS) == 0) return none
    return fadeIn(tween(motion.millis(SWAP_IN_MILLIS), easing = EnteringEasing)) togetherWith
        fadeOut(tween(motion.millis(SWAP_OUT_MILLIS), easing = LeavingEasing))
}

/** Something joins the screen (a notice, a field): it fades in and, when movement is welcome, opens its space. */
fun OriMotion.appear(): EnterTransition {
    if (millis(APPEAR_MILLIS) == 0) return EnterTransition.None
    val fade = fadeIn(tween(millis(APPEAR_MILLIS), easing = EnteringEasing))
    return if (movesThings) fade + expandVertically(tween(millis(APPEAR_MILLIS), easing = StandardEasing)) else fade
}

fun OriMotion.disappear(): ExitTransition {
    if (millis(DISAPPEAR_MILLIS) == 0) return ExitTransition.None
    val fade = fadeOut(tween(millis(DISAPPEAR_MILLIS), easing = LeavingEasing))
    return if (movesThings) fade + shrinkVertically(tween(millis(DISAPPEAR_MILLIS), easing = StandardEasing)) else fade
}
