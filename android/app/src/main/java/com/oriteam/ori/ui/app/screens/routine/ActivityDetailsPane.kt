package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.adaptive.FillOrScrollColumn
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberTimeFormat
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Everything about one activity, with only the actions this viewer may take. Deleting asks
 * for confirmation (handled by the routine screen) because it cannot be undone.
 */
@Composable
fun ActivityDetailsPane(
    step: RoutineStepUiModel,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxSize()) {
        OriTopBar(title = stringResource(R.string.activity_details_title), onNavigateBack = onNavigateBack)
        FillOrScrollColumn(modifier = Modifier.weight(1f)) {
            CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                OriCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = step.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(OriSpacing.Small))
                    DetailLine(
                        label = stringResource(R.string.activity_time_label),
                        value = step.time?.format(rememberTimeFormat()) ?: stringResource(R.string.activity_no_time)
                    )
                    DetailLine(
                        label = stringResource(R.string.activity_status_label),
                        value = stringResource(step.progress.labelRes)
                    )
                    if (step.repeatRule != null && step.repeatStart != null) {
                        DetailLine(
                            label = stringResource(R.string.activity_repeat_label),
                            value = repeatSummary(step.repeatRule, step.repeatStart)
                        )
                    }
                    ActivityBadges(step.badges)
                    step.description?.let {
                        Spacer(modifier = Modifier.height(OriSpacing.Small))
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(modifier = Modifier.height(OriSpacing.Large))
                DetailActions(step = step, onToggleDone = onToggleDone, onEdit = onEdit, onDelete = onDelete)
                Spacer(modifier = Modifier.height(OriSpacing.Huge))
            }
        }
    }
}

@Composable
private fun DetailActions(
    step: RoutineStepUiModel,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)) {
        if (step.canToggle) {
            OriPrimaryButton(
                text = stringResource(
                    if (step.isDone) R.string.activity_mark_pending else R.string.activity_mark_done
                ),
                onClick = onToggleDone
            )
        }
        if (step.canEdit) OriSecondaryButton(text = stringResource(R.string.activity_edit), onClick = onEdit)
        if (step.canDelete) {
            OriSecondaryButton(
                text = stringResource(R.string.activity_delete),
                onClick = onDelete,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (!step.canEdit && !step.canDelete) {
            // Explains why there are no edit buttons instead of leaving the person guessing.
            Text(
                text = stringResource(R.string.activity_reserved_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Text(
        text = stringResource(R.string.activity_detail_line, label, value),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
