package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.form.ActionPillLayout
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.OriTransientNotice
import com.oriteam.ori.ui.components.navigation.OriFilterTab
import com.oriteam.ori.ui.components.navigation.OriFilterTabs
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.navigation.SubscreenHost
import com.oriteam.ori.ui.components.navigation.rememberOpenSubscreen
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.StatefulContent
import com.oriteam.ori.ui.components.text.DateSkeletons
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberDateFormat
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import kotlinx.coroutines.delay

private enum class TasksSubscreen { NEW_ACTIVITY }

/** Long enough to see the mark and tap again; short enough not to look like it was not marked. */
internal const val KEEP_JUST_DONE_MILLIS = 4_000L

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

    SubscreenHost(subscreen) { current ->
        if (current == TasksSubscreen.NEW_ACTIVITY) {
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
    // Marked under "Pendentes", an activity stays a few seconds, marked: a tap by mistake is undone right
    // there, instead of the card vanishing into "Feitas".
    var justDone by remember(filter) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(justDone) {
        if (justDone.isNotEmpty()) {
            delay(KEEP_JUST_DONE_MILLIS)
            justDone = emptySet()
        }
    }
    val toggle: (String) -> Unit = { id ->
        if (filter == TaskFilter.PENDING) justDone = justDone + id
        onToggleDone(id)
    }
    val visible: ContentState<List<RoutineStepUiModel>> = when (val content = state.content) {
        is ContentState.Content -> content.value.filteredBy(filter, justDone).let {
            if (it.isEmpty()) ContentState.Empty else ContentState.Content(it)
        }
        else -> content
    }

    ActionPillLayout(
        pillLabel = stringResource(R.string.activity_add),
        onPillClick = onCreate,
        modifier = Modifier.fillMaxSize()
    ) { clearance ->
        Column(modifier = Modifier.fillMaxSize()) {
            OriTopBar(title = stringResource(R.string.tasks_title), onNavigateBack = onNavigateBack)
            CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                // Tarefas is always today: saying the day keeps "Todas" from meaning every day.
                Text(
                    text = stringResource(
                        R.string.tasks_today,
                        state.date.format(rememberDateFormat(DateSkeletons.WEEKDAY_DAY_MONTH))
                    ),
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
                OriTransientNotice(state.syncNoticeRes?.let { stringResource(it) })
            }
            StatefulContent(
                state = visible,
                onRetry = onRetry,
                emptyContent = { TasksEmptyState(filter, Modifier.padding(bottom = clearance)) },
                modifier = Modifier.weight(1f)
            ) { steps ->
                CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                    // The same cards as in Rotina; here they only mark the activity as done.
                    RoutineTimeline(
                        steps = steps,
                        onToggleDone = toggle,
                        onOpen = null,
                        listKey = state.contentDate to filter,
                        contentPadding = PaddingValues(top = OriSpacing.Medium, bottom = clearance)
                    )
                }
            }
        }
    }
}

@Composable
private fun TasksEmptyState(filter: TaskFilter, modifier: Modifier) {
    val (title, message) = when (filter) {
        TaskFilter.PENDING -> R.string.tasks_empty_pending_title to R.string.tasks_empty_pending_message
        TaskFilter.DONE -> R.string.tasks_empty_done_title to R.string.tasks_empty_done_message
        TaskFilter.ALL -> R.string.routine_empty_title to R.string.routine_empty_owner
    }
    val add = stringResource(R.string.activity_add)
    // All done is a check; an empty day is the calendar.
    val icon = if (filter == TaskFilter.ALL) Icons.Filled.DateRange else Icons.Filled.CheckCircle
    EmptyState(
        icon = icon,
        title = stringResource(title),
        message = stringResource(message, add),
        modifier = modifier
    )
}
