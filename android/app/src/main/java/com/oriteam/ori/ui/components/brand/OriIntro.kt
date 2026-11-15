package com.oriteam.ori.ui.components.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oriteam.ori.R
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriMotion
import com.oriteam.ori.ui.theme.OriSpacing
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

// The steps of the opening, in milliseconds at the expressive level; the other levels shorten them (OriMotion).
private const val REVEAL_MILLIS = 900
private const val HALO_DELAY_MILLIS = 450
private const val HALO_MILLIS = 1100
private const val LETTERS_DELAY_MILLIS = 800
private const val LETTER_STAGGER_MILLIS = 140
private const val SLOGAN_DELAY_MILLIS = 1350
private const val SLOGAN_STAGGER_MILLIS = 220
private const val SLOGAN_PIECE_MILLIS = 420
private const val BREATH_DELAY_MILLIS = 2300
private const val BREATH_MILLIS = 380
private const val END_PAUSE_MILLIS = 350

private const val START_SCALE = 0.72f
private const val START_TILT = -10f
private const val BREATH_SCALE = 1.05f
private const val HALO_START_ALPHA = 0.55f
private const val HALO_GROWTH = 0.45f
private const val LETTER_RISE_DP = 28f
private const val SLOGAN_RISE_DP = 12f

/**
 * The opening of the first launch, played once (see `HomeViewModel`): the logo appears from its center, turning a
 * little into place with a soft bounce, while a halo spreads from it; the letters of "ori" rise one after the
 * other; the slogan comes word by word; and the logo breathes once before the options appear.
 *
 * It follows "Animações": subtle is shorter, reduced only fades (nothing travels), and off shows everything at
 * once. Every animated value is read while drawing (graphicsLayer, drawBehind), not in composition. Screen readers
 * hear the logo, "ori" and the slogan as they are, never letter by letter. [onFinished] is called at the end.
 */
@Composable
fun OriIntroBrand(onFinished: () -> Unit, modifier: Modifier = Modifier, logoHeight: Dp = 200.dp) {
    val motion = LocalOriAppearance.current.motion
    val moves = motion.movesThings
    val reveal = remember { Animatable(0f) }
    val halo = remember { Animatable(0f) }
    val breath = remember { Animatable(1f) }
    val title = stringResource(R.string.app_name)
    val letters = remember(title) { title.map { Animatable(0f) } }
    val slogan = stringResource(R.string.onboarding_slogan)
    val sloganPieces = remember(slogan) { slogan.split(" ").filter { it.isNotBlank() } }
    val pieces = remember(sloganPieces) { sloganPieces.map { Animatable(0f) } }
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        IntroValues(reveal, halo, breath, letters, pieces).play(motion)
        delay(motion.millis(END_PAUSE_MILLIS).toLong())
        finish()
    }

    val haloColor = LocalOriColors.current.arcThick
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth(0.65f).height(logoHeight)) {
            Image(
                painter = painterResource(R.drawable.ori),
                contentDescription = stringResource(R.string.onboarding_logo_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(logoHeight)
                    .drawBehind {
                        // The halo: a ring that leaves the logo and fades as it grows.
                        val progress = halo.value
                        if (progress in 0.001f..0.999f) {
                            val radius = size.minDimension / 2 * (1f + HALO_GROWTH * progress)
                            drawCircle(
                                color = haloColor,
                                radius = radius,
                                alpha = HALO_START_ALPHA * (1f - progress),
                                style = Stroke(width = 6.dp.toPx() * (1f - progress) + 1f)
                            )
                        }
                    }
                    .graphicsLayer {
                        val shown = reveal.value
                        alpha = if (moves) 1f.coerceAtMost(shown * 2f) else shown
                        if (moves) {
                            val scale = (START_SCALE + (1f - START_SCALE) * shown) * breath.value
                            scaleX = scale
                            scaleY = scale
                            rotationZ = START_TILT * (1f - shown)
                        }
                    }
                    .then(if (moves) Modifier.circleReveal { reveal.value } else Modifier)
            )
        }
        Row(
            modifier = Modifier.clearAndSetSemantics { contentDescription = title }
        ) {
            title.forEachIndexed { index, letter ->
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.graphicsLayer {
                        val shown = letters[index].value
                        alpha = shown.coerceIn(0f, 1f)
                        if (moves) {
                            translationY = LETTER_RISE_DP.dp.toPx() * (1f - shown)
                            scaleX = START_SCALE + (1f - START_SCALE) * shown
                            scaleY = scaleX
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clearAndSetSemantics { contentDescription = slogan }
        ) {
            sloganPieces.forEachIndexed { index, piece ->
                Text(
                    text = piece,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.2.sp,
                    modifier = Modifier.graphicsLayer {
                        val shown = pieces[index].value
                        alpha = shown
                        if (moves) translationY = SLOGAN_RISE_DP.dp.toPx() * (1f - shown)
                    }
                )
            }
        }
    }
}

/** Shows only a circle around the center, growing with [progress] until the whole image is seen. */
private fun Modifier.circleReveal(progress: () -> Float): Modifier = drawWithContent {
    val value = progress()
    if (value >= 1f) {
        drawContent()
    } else {
        val radius = hypot(size.width / 2, size.height / 2) * value
        val circle = Path().apply { addOval(Rect(center = Offset(size.width / 2, size.height / 2), radius = radius)) }
        clipPath(circle) { this@drawWithContent.drawContent() }
    }
}

/** The values the opening moves, from 0 (not shown) to 1 (in place); [breath] is the logo's scale. */
private class IntroValues(
    val reveal: Animatable<Float, *>,
    val halo: Animatable<Float, *>,
    val breath: Animatable<Float, *>,
    val letters: List<Animatable<Float, *>>,
    val pieces: List<Animatable<Float, *>>
)

/** Runs the steps; with animations off every value goes straight to its end. */
private suspend fun IntroValues.play(motion: OriMotion) = coroutineScope {
    // The bounce is for the expressive level only; subtle glides, reduced fades.
    val bouncy = if (motion.level == MotionLevel.EXPRESSIVE || motion.level == MotionLevel.SYSTEM) {
        spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    } else {
        tween(motion.millis(REVEAL_MILLIS))
    }
    val moves = motion.movesThings
    fun at(millis: Int) = motion.millis(millis).toLong()
    if (motion.millis(REVEAL_MILLIS) == 0) {
        (listOf(reveal) + letters + pieces).forEach { it.snapTo(1f) }
        halo.snapTo(1f)
        return@coroutineScope
    }
    launch { reveal.animateTo(1f, if (moves) bouncy else tween(motion.millis(REVEAL_MILLIS))) }
    if (moves) {
        launch {
            delay(at(HALO_DELAY_MILLIS))
            halo.animateTo(1f, tween(motion.millis(HALO_MILLIS)))
        }
    }
    letters.forEachIndexed { index, letter ->
        launch {
            delay(at(LETTERS_DELAY_MILLIS + index * LETTER_STAGGER_MILLIS))
            letter.animateTo(1f, if (moves) bouncy else tween(motion.millis(SLOGAN_PIECE_MILLIS)))
        }
    }
    pieces.forEachIndexed { index, piece ->
        launch {
            delay(at(SLOGAN_DELAY_MILLIS + index * SLOGAN_STAGGER_MILLIS))
            piece.animateTo(1f, tween(motion.millis(SLOGAN_PIECE_MILLIS)))
        }
    }
    if (moves) {
        launch {
            delay(at(BREATH_DELAY_MILLIS))
            breath.animateTo(BREATH_SCALE, tween(motion.millis(BREATH_MILLIS)))
            breath.animateTo(1f, tween(motion.millis(BREATH_MILLIS)))
        }
    }
}
