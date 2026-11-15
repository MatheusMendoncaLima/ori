package com.oriteam.ori.ui.app.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.list.OriMenuRow
import com.oriteam.ori.ui.components.motion.entrance
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.theme.OriAreaAccents
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.areaAccent
import java.time.LocalTime

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
        firstName = profile.firstName,
        steps = state.content,
        now = rememberCurrentMinute(),
        onRetry = viewModel::onRetry,
        onOpenRoutine = onOpenRoutine,
        onOpenCommunication = onOpenCommunication,
        onOpenSubscreen = onOpenSubscreen
    )
}

@Composable
fun TodayScreen(
    firstName: String,
    steps: ContentState<List<RoutineStepUiModel>>,
    now: LocalTime,
    onRetry: () -> Unit,
    onOpenRoutine: () -> Unit,
    onOpenCommunication: () -> Unit,
    onOpenSubscreen: (HomeSubscreen) -> Unit
) {
    // The cards arrive the first time Início opens; coming back to it, they are simply there.
    var hasArrived by rememberSaveable { mutableStateOf(false) }
    val arrives = remember { !hasArrived }
    LaunchedEffect(Unit) { hasArrived = true }
    Column(modifier = Modifier.fillMaxSize()) {
        // Only the first name: "Olá, Ana!" is a greeting, the full name is for the profile.
        OriTopBar(
            title = if (firstName.isBlank()) {
                stringResource(R.string.today_greeting_no_name)
            } else {
                stringResource(R.string.today_greeting, firstName)
            }
        )
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                Box(modifier = Modifier.entrance(order = 0, isEnabled = arrives)) {
                    TodaySummary(state = steps, now = now, onRetry = onRetry, onClick = onOpenRoutine)
                }
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                Column(verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)) {
                    OriMenuRow(
                        modifier = Modifier.entrance(order = 1, isEnabled = arrives),
                        title = stringResource(R.string.shortcut_routine),
                        description = stringResource(R.string.shortcut_routine_description),
                        icon = Icons.Filled.DateRange,
                        accent = areaAccent(OriAreaAccents.ROUTINE),
                        onClick = onOpenRoutine
                    )
                    OriMenuRow(
                        modifier = Modifier.entrance(order = 2, isEnabled = arrives),
                        title = stringResource(R.string.shortcut_tasks),
                        description = stringResource(R.string.shortcut_tasks_description),
                        icon = Icons.AutoMirrored.Filled.List,
                        accent = areaAccent(OriAreaAccents.TASKS),
                        onClick = { onOpenSubscreen(HomeSubscreen.TASKS) }
                    )
                    OriMenuRow(
                        modifier = Modifier.entrance(order = 3, isEnabled = arrives),
                        title = stringResource(R.string.main_sentences),
                        description = stringResource(R.string.shortcut_sentence_description),
                        icon = OriIcons.Chat,
                        accent = areaAccent(OriAreaAccents.COMMUNICATION),
                        onClick = onOpenCommunication
                    )
                    OriMenuRow(
                        modifier = Modifier.entrance(order = 4, isEnabled = arrives),
                        title = stringResource(R.string.shortcut_reports),
                        description = stringResource(R.string.shortcut_reports_description),
                        painter = painterResource(R.drawable.ic_chart),
                        accent = areaAccent(OriAreaAccents.REPORTS),
                        onClick = { onOpenSubscreen(HomeSubscreen.REPORTS) }
                    )
                }
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
    }
}
