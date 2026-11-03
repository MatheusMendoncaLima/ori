package com.oriteam.ori.ui.app.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.profile.UserProfile
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.app.main.HomeSubscreen
import com.oriteam.ori.ui.app.screens.routine.RoutineStepUiModel
import com.oriteam.ori.ui.app.screens.routine.RoutineViewModel
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.list.OriMenuRow
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriAreaAccents
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/** The day at a glance and the way to every part of the app. */
@Composable
fun TodayRoute(
    profile: UserProfile,
    onOpenRoutine: () -> Unit,
    onOpenCommunication: () -> Unit,
    onOpenSubscreen: (HomeSubscreen) -> Unit,
    viewModel: RoutineViewModel = hiltViewModel(key = "today-${profile.uid}")
) {
    LaunchedEffect(profile.uid) { viewModel.show(profile.uid, RoutineViewer.Owner) }
    // Coming back the next day, or after midnight with the screen open, shows the new day.
    LifecycleResumeEffect(viewModel) {
        viewModel.followToday()
        onPauseOrDispose { }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        name = profile.displayName,
        steps = state.content,
        onRetry = viewModel::onRetry,
        onOpenRoutine = onOpenRoutine,
        onOpenCommunication = onOpenCommunication,
        onOpenSubscreen = onOpenSubscreen
    )
}

@Composable
fun TodayScreen(
    name: String,
    steps: ContentState<List<RoutineStepUiModel>>,
    onRetry: () -> Unit,
    onOpenRoutine: () -> Unit,
    onOpenCommunication: () -> Unit,
    onOpenSubscreen: (HomeSubscreen) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(
            title = if (name.isBlank()) {
                stringResource(R.string.today_greeting_no_name)
            } else {
                stringResource(R.string.today_greeting, name)
            }
        )
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                TodaySummary(state = steps, onRetry = onRetry, onClick = onOpenRoutine)
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                Column(verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)) {
                    OriMenuRow(
                        title = stringResource(R.string.shortcut_routine),
                        description = stringResource(R.string.shortcut_routine_description),
                        icon = Icons.Filled.DateRange,
                        accentIndex = OriAreaAccents.ROUTINE,
                        onClick = onOpenRoutine
                    )
                    OriMenuRow(
                        title = stringResource(R.string.shortcut_tasks),
                        description = stringResource(R.string.shortcut_tasks_description),
                        icon = Icons.AutoMirrored.Filled.List,
                        accentIndex = OriAreaAccents.TASKS,
                        onClick = { onOpenSubscreen(HomeSubscreen.TASKS) }
                    )
                    OriMenuRow(
                        title = stringResource(R.string.main_form_sentence),
                        description = stringResource(R.string.shortcut_sentence_description),
                        icon = OriIcons.Chat,
                        accentIndex = OriAreaAccents.COMMUNICATION,
                        onClick = onOpenCommunication
                    )
                    OriMenuRow(
                        title = stringResource(R.string.shortcut_reports),
                        description = stringResource(R.string.shortcut_reports_description),
                        painter = painterResource(R.drawable.ic_chart),
                        accentIndex = OriAreaAccents.REPORTS,
                        onClick = { onOpenSubscreen(HomeSubscreen.REPORTS) }
                    )
                }
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
    }
}

/** Each state says what is true: only a successful load may say that the day has no activities. */
@Composable
private fun TodaySummary(state: ContentState<List<RoutineStepUiModel>>, onRetry: () -> Unit, onClick: () -> Unit) {
    when (state) {
        ContentState.Loading -> OriCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.state_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        ContentState.Empty -> OriCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.today_empty), style = MaterialTheme.typography.titleMedium)
        }
        is ContentState.Error -> OriCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(state.messageRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            if (state.canRetry) {
                OriTextButton(text = stringResource(R.string.action_retry), onClick = onRetry)
            }
        }
        is ContentState.Content -> OriCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            DayProgress(state.value)
        }
    }
}

@Composable
private fun DayProgress(steps: List<RoutineStepUiModel>) {
    val done = steps.count { it.isDone }
    val next = steps.firstOrNull { !it.isDone }
    Text(
        text = pluralStringResource(R.plurals.today_progress, steps.size, done, steps.size),
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(modifier = Modifier.height(OriSpacing.Small))
    LinearProgressIndicator(
        progress = { done.toFloat() / steps.size },
        color = LocalOriColors.current.strongOnSurface,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(OriSpacing.Small))
    if (next == null) {
        Text(text = stringResource(R.string.today_all_done), style = MaterialTheme.typography.bodyMedium)
    } else {
        Text(
            text = stringResource(R.string.today_next),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = listOfNotNull(next.timeLabel, next.title).joinToString(" · "),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
