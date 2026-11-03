package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.AppLocale
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.OriActionPill
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.navigation.OriFilterTab
import com.oriteam.ori.ui.components.navigation.OriFilterTabs
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.navigation.rememberOpenSubscreen
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.StatefulContent
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.format.DateTimeFormatter

private enum class TasksSubscreen { NEW_ACTIVITY }

private val todayFormat = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", AppLocale)

/**
 * "Tarefas": the activities of today as a checklist. It shows the same data as the routine, so
 * marking one here marks it there; only the way of looking at the day is different.
 */
@Composable
fun TasksRoute(
    ownerUid: String,
    onNavigateBack: () -> Unit,
    viewModel: RoutineViewModel = hiltViewModel(key = "tasks-$ownerUid")
) {
    LaunchedEffect(ownerUid) { viewModel.show(ownerUid, RoutineViewer.Owner) }
    // Coming back the next day, or after midnight with the screen open, shows the new day.
    LifecycleResumeEffect(viewModel) {
        viewModel.followToday()
        onPauseOrDispose { }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subscreen = rememberOpenSubscreen<TasksSubscreen>()

    if (subscreen.current == TasksSubscreen.NEW_ACTIVITY) {
        ActivityEditorRoute(
            editorKey = "tasks-editor-$ownerUid",
            ownerUid = ownerUid,
            viewer = RoutineViewer.Owner,
            existing = null,
            defaultDate = state.date,
            today = state.today,
            // As in Rotina, creating goes back to the list (see RoutinePane.afterSave).
            onSaved = { subscreen.close() },
            onClose = subscreen::close
        )
    } else {
        TasksScreen(
            state = state,
            onToggleDone = viewModel::onToggleDone,
            onCreate = { subscreen.open(TasksSubscreen.NEW_ACTIVITY) },
            onRetry = viewModel::onRetry,
            onNavigateBack = onNavigateBack
        )
    }
}

@Composable
fun TasksScreen(
    state: RoutineUiState,
    onToggleDone: (String) -> Unit,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var filterName by rememberSaveable { mutableStateOf(TaskFilter.PENDING.name) }
    val filter = TaskFilter.valueOf(filterName)
    val visible: ContentState<List<RoutineStepUiModel>> = when (val content = state.content) {
        is ContentState.Content -> content.value.filteredBy(filter).let {
            if (it.isEmpty()) ContentState.Empty else ContentState.Content(it)
        }
        else -> content
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OriTopBar(title = stringResource(R.string.tasks_title), onNavigateBack = onNavigateBack)
            CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                // Tarefas is always today: saying the day keeps "Todas" from meaning every day.
                Text(
                    text = stringResource(R.string.tasks_today, state.date.format(todayFormat)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = OriSpacing.Small)
                )
                OriFilterTabs(
                    tabs = listOf(
                        OriFilterTab(TaskFilter.PENDING, stringResource(R.string.tasks_filter_pending)),
                        OriFilterTab(TaskFilter.DONE, stringResource(R.string.tasks_filter_done)),
                        OriFilterTab(TaskFilter.ALL, stringResource(R.string.tasks_filter_all))
                    ),
                    selected = filter,
                    onSelect = { filterName = it.name }
                )
                FormMessage(messageRes = state.actionErrorRes, type = FormMessageType.ERROR)
                state.syncNoticeRes?.let { OriNotice(stringResource(it), isLive = true) }
            }
            StatefulContent(
                state = visible,
                onRetry = onRetry,
                emptyContent = { TasksEmptyState(filter) },
                modifier = Modifier.weight(1f)
            ) { steps ->
                CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                    // The same cards as in Rotina; here they only mark the activity as done.
                    RoutineTimeline(
                        steps = steps,
                        onToggleDone = onToggleDone,
                        onOpen = null,
                        contentPadding = PaddingValues(top = OriSpacing.Medium, bottom = 88.dp)
                    )
                }
            }
        }
        OriActionPill(
            label = stringResource(R.string.activity_add),
            onClick = onCreate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = OriSpacing.Medium)
        )
    }
}

@Composable
private fun TasksEmptyState(filter: TaskFilter) {
    val (title, message) = when (filter) {
        TaskFilter.PENDING -> R.string.tasks_empty_pending_title to R.string.tasks_empty_pending_message
        TaskFilter.DONE -> R.string.tasks_empty_done_title to R.string.tasks_empty_done_message
        TaskFilter.ALL -> R.string.routine_empty_title to R.string.routine_empty_owner
    }
    EmptyState(title = stringResource(title), message = stringResource(message))
}
