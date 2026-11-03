package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.routine.daysOfWeekInOrder
import com.oriteam.ori.domain.routine.startOfWeek
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriDimensions
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val MONTH_PAGES = 1_200
private const val WEEKS_SHOWN = 6
private const val DAYS_IN_WEEK = 7
private const val OPEN_ANIMATION_MILLIS = 200
private val FirstMonth: YearMonth = YearMonth.of(2000, 1)

private val HandleWidth = 36.dp
private val HandleHeight = 4.dp

/** How far a drag goes before it means something, so a slightly shaky tap changes nothing. */
private val ToggleDragDistance = 48.dp

internal fun monthPageOf(date: LocalDate): Int =
    ChronoUnit.MONTHS.between(FirstMonth, YearMonth.from(date)).coerceIn(0L, MONTH_PAGES - 1L).toInt()

/** Always six weeks, from the start of the week of the first day: the height does not jump between months. */
internal fun daysShownIn(month: YearMonth): List<LocalDate> {
    val start = startOfWeek(month.atDay(1))
    return List(WEEKS_SHOWN * DAYS_IN_WEEK) { start.plusDays(it.toLong()) }
}

/**
 * The routine's calendar, in the two forms of the Samsung calendar: a week, or the whole month.
 * Dragging down opens the month, dragging up goes back to the week of the chosen day. The small bar
 * below shows that it can be dragged; tapping it does the same, for who cannot drag, and it is a
 * button for screen readers.
 */
@Composable
fun RoutineCalendar(
    date: LocalDate,
    today: LocalDate,
    isMonthOpen: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onMonthOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    daysWithActivities: Set<LocalDate> = emptySet()
) {
    val motion = LocalOriAppearance.current.motion
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = motion.decorative(OPEN_ANIMATION_MILLIS))
    ) {
        if (isMonthOpen) {
            MonthCalendar(
                date = date,
                today = today,
                daysWithActivities = daysWithActivities,
                onSelectDate = onSelectDate
            )
        } else {
            WeekStrip(date = date, today = today, onSelectDate = onSelectDate)
        }
        DragHandle(isMonthOpen = isMonthOpen, onToggle = { onMonthOpenChange(!isMonthOpen) })
    }
}

@Composable
private fun DragHandle(isMonthOpen: Boolean, onToggle: () -> Unit) {
    val label = stringResource(if (isMonthOpen) R.string.calendar_show_week else R.string.calendar_show_month)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = OriDimensions.MinTouchTarget)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onToggle)
            .semantics { contentDescription = label }
    ) {
        Box(
            modifier = Modifier
                .size(width = HandleWidth, height = HandleHeight)
                .background(MaterialTheme.colorScheme.outline, CircleShape)
        )
    }
}

/** Dragging sideways changes the month, keeping the day (or the last one, in a shorter month). */
@Composable
private fun MonthCalendar(
    date: LocalDate,
    today: LocalDate,
    daysWithActivities: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CalendarHeader(
            date = date,
            today = today,
            onSelectDate = onSelectDate,
            previous = { date.minusMonths(1) },
            next = { date.plusMonths(1) },
            previousLabel = stringResource(R.string.month_previous),
            nextLabel = stringResource(R.string.month_next)
        )
        Row {
            daysOfWeekInOrder.forEach { day ->
                Text(
                    text = day.shortName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        SettlingPager(
            page = monthPageOf(date),
            pageCount = MONTH_PAGES,
            onDragSettled = { settled -> onSelectDate(date.plusMonths((settled - monthPageOf(date)).toLong())) }
        ) { shown ->
            val month = FirstMonth.plusMonths(shown.toLong())
            Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.Top) {
                daysShownIn(month).chunked(DAYS_IN_WEEK).forEach { week ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        week.forEach { day ->
                            DayCell(
                                day = day,
                                isSelected = day == date,
                                isToday = day == today,
                                isDimmed = YearMonth.from(day) != month,
                                hasActivities = day in daysWithActivities,
                                onClick = { onSelectDate(day) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Listens to vertical drags that nothing below used: over the calendar, or over a list that had
 * nowhere to go. Dragging down opens the month and up goes back to the week. A drag that moved the list
 * changes nothing when it reaches the end, and a fling never does: scrolling must not open a calendar.
 */
@Composable
fun rememberCalendarDragConnection(isMonthOpen: Boolean, onMonthOpenChange: (Boolean) -> Unit): NestedScrollConnection {
    val currentIsOpen by rememberUpdatedState(isMonthOpen)
    val currentOnChange by rememberUpdatedState(onMonthOpenChange)
    val distance = with(LocalDensity.current) { ToggleDragDistance.toPx() }
    return remember(distance) {
        object : NestedScrollConnection {
            private var dragged = 0f
            private var isSpent = false

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) onDrag(consumed.y, available.y)
                return Offset.Zero
            }

            private fun onDrag(moved: Float, unused: Float) {
                if (moved != 0f) isSpent = true
                if (isSpent) return
                dragged += unused
                if (abs(dragged) >= distance) {
                    isSpent = true
                    val wantsOpen = dragged > 0f
                    if (wantsOpen != currentIsOpen) currentOnChange(wantsOpen)
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                dragged = 0f
                isSpent = false
                return Velocity.Zero
            }
        }
    }
}
