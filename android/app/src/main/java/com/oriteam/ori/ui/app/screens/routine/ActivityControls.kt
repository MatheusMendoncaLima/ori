package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing

/** The notes of an activity ("Só você vê", "Definida pelo responsável", "Repete"). */
@Composable
internal fun ActivityBadges(badges: List<ActivityBadge>) {
    if (badges.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small)) {
        badges.forEach { badge ->
            Text(
                text = stringResource(badge.labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private const val DONE_MILLIS = 300

/**
 * The round button that marks an activity as done: its own 48 dp target, announced as a checkbox with
 * "Concluída" or "Pendente". Tapping the rest of the card opens the details, when there are details to open.
 */
@Composable
internal fun DoneToggle(step: RoutineStepUiModel, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val motion = LocalOriAppearance.current.motion
    val description = stringResource(R.string.activity_toggle_done, step.title)
    val stateLabel = stringResource(step.progress.labelRes)
    val container = animateColorAsState(
        targetValue = if (step.isDone) colors.tertiary else colors.surface,
        animationSpec = motion.feedback(DONE_MILLIS),
        label = "DoneContainer"
    )
    val ring = animateColorAsState(
        targetValue = if (step.isDone) colors.tertiary else colors.outline,
        animationSpec = motion.feedback(DONE_MILLIS),
        label = "DoneRing"
    )
    // Finishing something deserves a small moment: the check pops in (with a bounce, when expressive).
    val check = animateFloatAsState(
        targetValue = if (step.isDone) 1f else 0f,
        animationSpec = if (step.isDone) motion.playful(DONE_MILLIS) else motion.feedback(DONE_MILLIS / 2),
        label = "DoneCheck"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .toggleable(
                value = step.isDone,
                enabled = step.canToggle,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .semantics {
                contentDescription = description
                stateDescription = stateLabel
            }
    ) {
        // Drawn, not composed: the colors and the check change every frame without rebuilding anything.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .drawBehind {
                    val ringWidth = 2.dp.toPx()
                    drawCircle(container.value)
                    drawCircle(ring.value, radius = (size.minDimension - ringWidth) / 2, style = Stroke(ringWidth))
                }
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.onTertiary,
                modifier = Modifier.graphicsLayer {
                    val shown = check.value
                    scaleX = shown
                    scaleY = shown
                    alpha = shown.coerceIn(0f, 1f)
                }
            )
        }
    }
}
