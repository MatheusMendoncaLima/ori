package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.routine.DeleteScope
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.ui.adaptive.AreaListDetailLayout
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.LoadingState
import com.oriteam.ori.ui.components.text.Text

/** Removing a repeating activity always asks what is meant: one day or all of them. */
@Composable
private fun RepeatingDeleteDialog(title: String, onDelete: (DeleteScope) -> Unit, onDismiss: () -> Unit) {
    val destructive = MaterialTheme.colorScheme.error
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Text(
                text = stringResource(R.string.activity_delete_repeating_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            FlowRow(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.action_cancel)) }
                TextButton(onClick = { onDelete(DeleteScope.THIS_DAY) }) {
                    Text(text = stringResource(R.string.activity_delete_this_day), color = destructive)
                }
                TextButton(onClick = { onDelete(DeleteScope.ALL_DAYS) }) {
                    Text(text = stringResource(R.string.activity_delete_all_days), color = destructive)
                }
            }
        }
    )
}

/** What the side (or full-screen) pane shows. Encoded as text so it survives process death. */
internal sealed interface RoutinePane {
    val key: String

    data class Details(val id: String) : RoutinePane {
        override val key = "details:$id"
    }

    data object New : RoutinePane {
        override val key = "new"
    }

    data class Edit(val id: String) : RoutinePane {
        override val key = "edit:$id"
    }

    companion object {
        fun parse(key: String): RoutinePane = when {
            key == New.key -> New
            key.startsWith("edit:") -> Edit(key.removePrefix("edit:"))
            else -> Details(key.removePrefix("details:"))
        }
    }
}

/**
 * Saving goes back to where the person was, in Rotina and in Tarefas: the list after creating, the
 * details after editing. A repeating activity may not fall on the day shown, so after editing one the
 * list comes back.
 */
internal fun RoutinePane.afterSave(saved: ActivityEditorViewModel.Event.Saved): RoutinePane? =
    if (this is RoutinePane.Edit && saved.date != null) RoutinePane.Details(saved.activityId) else null

/**
 * Routine of [ownerUid] for one day, as seen by [viewer]. The layout depends on the space this
 * area receives, so it also adapts inside the caregiver's detail pane.
 */
@Composable
fun RoutineRoute(
    ownerUid: String,
    viewer: RoutineViewer,
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: RoutineViewModel = hiltViewModel(key = "routine-$ownerUid")
) {
    LaunchedEffect(ownerUid, viewer) { viewModel.show(ownerUid, viewer) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var paneKey by rememberSaveable(ownerUid) { mutableStateOf<String?>(null) }
    val pane = paneKey?.let(RoutinePane::parse)
    val today = state.date.minusDays(state.daysFromToday)

    val list: @Composable (Modifier) -> Unit = { listModifier ->
        RoutineDayList(
            state = state,
            title = title,
            selectedId = (pane as? RoutinePane.Details)?.id,
            emptyMessage = stringResource(
                if (viewer == RoutineViewer.Owner) R.string.routine_empty_owner else R.string.routine_empty_caregiver
            ),
            onNavigateBack = onNavigateBack,
            onSelectDate = viewModel::onSelectDate,
            onMonthShown = viewModel::onMonthShown,
            onToggleDone = viewModel::onToggleDone,
            onOpen = { paneKey = RoutinePane.Details(it).key },
            onCreate = { paneKey = RoutinePane.New.key },
            onRetry = viewModel::onRetry,
            modifier = listModifier
        )
    }
    val paneContent: @Composable (RoutinePane, (() -> Unit)?) -> Unit = { current, onBack ->
        when (current) {
            is RoutinePane.Details -> {
                val step = state.step(current.id)
                when {
                    step != null -> ActivityDetailsPane(
                        step = step,
                        onToggleDone = { viewModel.onToggleDone(step.id) },
                        onEdit = { paneKey = RoutinePane.Edit(step.id).key },
                        onDelete = { viewModel.onDeleteRequest(step.id) },
                        onNavigateBack = onBack
                    )
                    // An activity saved on another day opens with that day, which may still be loading.
                    state.content == ContentState.Loading -> LoadingState()
                    else -> EmptyState(
                        title = stringResource(R.string.activity_not_found_title),
                        message = stringResource(R.string.activity_not_found_message)
                    )
                }
            }
            RoutinePane.New, is RoutinePane.Edit -> ActivityEditorRoute(
                editorKey = "editor-$ownerUid-${current.key}",
                ownerUid = ownerUid,
                viewer = viewer,
                existing = (current as? RoutinePane.Edit)?.let { viewModel.activity(it.id) },
                defaultDate = state.date,
                today = today,
                onSaved = { saved ->
                    // The form may have moved the activity to another day; that day is shown, so it is in sight.
                    saved.date?.let(viewModel::onSelectDate)
                    paneKey = current.afterSave(saved)?.key
                },
                onClose = { paneKey = (current as? RoutinePane.Edit)?.let { RoutinePane.Details(it.id).key } }
            )
        }
    }

    AreaListDetailLayout(
        hasDetail = pane != null,
        onBack = { paneKey = null },
        list = list,
        detail = { onBack -> pane?.let { paneContent(it, onBack) } },
        emptyDetail = {
            EmptyState(
                title = stringResource(R.string.activity_choose_title),
                message = stringResource(R.string.activity_choose_message)
            )
        }
    )

    state.pendingDeletion?.let { step ->
        DeletionDialog(
            step = step,
            onConfirm = { scope ->
                viewModel.onDeleteConfirm(scope)
                paneKey = null
            },
            onDismiss = viewModel::onDeleteDismiss
        )
    }
}

@Composable
private fun DeletionDialog(step: RoutineStepUiModel, onConfirm: (DeleteScope) -> Unit, onDismiss: () -> Unit) {
    if (step.seriesId != null) {
        RepeatingDeleteDialog(
            title = stringResource(R.string.activity_delete_title, step.title),
            onDelete = onConfirm,
            onDismiss = onDismiss
        )
    } else {
        OriConfirmationDialog(
            title = stringResource(R.string.activity_delete_title, step.title),
            message = stringResource(R.string.activity_delete_message),
            confirmLabel = stringResource(R.string.activity_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = { onConfirm(DeleteScope.ALL_DAYS) },
            onDismiss = onDismiss,
            isDestructive = true
        )
    }
}
