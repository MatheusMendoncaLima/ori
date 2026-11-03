package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.OriActionPill
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.StatefulContent
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.LocalDate
import java.time.YearMonth

/** Room at the end of a list, so the floating button never covers the last item. */
private val FloatingActionClearance = 88.dp

/** The open month leaves the rest for the activities of the chosen day. */
private const val CALENDAR_MAX_HEIGHT_SHARE = 0.65f

/** "Minha rotina": the week or the month, notices and the day as a timeline, with "+ Adicionar atividade". */
@Composable
fun RoutineDayList(
    state: RoutineUiState,
    title: String,
    selectedId: String?,
    emptyMessage: String,
    onNavigateBack: (() -> Unit)?,
    onSelectDate: (LocalDate) -> Unit,
    onToggleDone: (String) -> Unit,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onMonthShown: (YearMonth?) -> Unit = {}
) {
    var isMonthOpen by rememberSaveable { mutableStateOf(false) }
    val month = YearMonth.from(state.date)
    // The marks of the month are read only while the month is open.
    LaunchedEffect(isMonthOpen, month) { onMonthShown(month.takeIf { isMonthOpen }) }
    val calendarDrag = rememberCalendarDragConnection(isMonthOpen, onMonthOpenChange = { isMonthOpen = it })
    BoxWithConstraints(modifier = modifier.nestedScroll(calendarDrag)) {
        val calendarMaxHeight = maxHeight * CALENDAR_MAX_HEIGHT_SHARE
        Column(modifier = Modifier.fillMaxSize()) {
            OriTopBar(title = title, onNavigateBack = onNavigateBack)
            // Scrolling is what also reports a drag over the calendar, and short windows need it anyway.
            CenteredContent(
                maxWidth = OriDimensions.ContentMaxWidth,
                modifier = Modifier
                    .heightIn(max = calendarMaxHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                RoutineCalendar(
                    date = state.date,
                    today = state.today,
                    isMonthOpen = isMonthOpen,
                    daysWithActivities = state.daysWithActivities,
                    onSelectDate = onSelectDate,
                    onMonthOpenChange = { isMonthOpen = it }
                )
            }
            CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                FormMessage(messageRes = state.actionErrorRes, type = FormMessageType.ERROR)
                state.syncNoticeRes?.let { OriNotice(stringResource(it), isLive = true) }
                if (state.isTruncated) OriNotice(stringResource(R.string.routine_truncated), isLive = true)
            }
            StatefulContent(
                state = state.content,
                onRetry = onRetry,
                emptyContent = {
                    EmptyState(title = stringResource(R.string.routine_empty_title), message = emptyMessage)
                },
                modifier = Modifier.weight(1f)
            ) { steps ->
                CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                    RoutineTimeline(
                        steps = steps.map { it.copy(isCurrent = it.id == selectedId) },
                        onToggleDone = onToggleDone,
                        onOpen = onOpen,
                        contentPadding = PaddingValues(
                            top = OriSpacing.Medium,
                            bottom = if (state.canCreate) FloatingActionClearance else OriSpacing.Medium
                        )
                    )
                }
            }
        }
        if (state.canCreate) {
            OriActionPill(
                label = stringResource(R.string.activity_add),
                onClick = onCreate,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = OriSpacing.Medium)
            )
        }
    }
}
