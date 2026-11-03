package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.text.Text
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

/**
 * The round button that marks an activity as done: its own 48 dp target, announced as a checkbox with
 * "Concluída" or "Pendente". Tapping the rest of the card opens the details, when there are details to open.
 */
@Composable
internal fun DoneToggle(step: RoutineStepUiModel, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.activity_toggle_done, step.title)
    val stateLabel = stringResource(step.progress.labelRes)
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
        Surface(
            shape = CircleShape,
            color = if (step.isDone) colors.tertiary else colors.surface,
            modifier = Modifier
                .size(28.dp)
                .border(width = 2.dp, color = if (step.isDone) colors.tertiary else colors.outline, shape = CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (step.isDone) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = colors.onTertiary)
                }
            }
        }
    }
}
