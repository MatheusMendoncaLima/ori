package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.AppLocale
import com.oriteam.ori.domain.routine.startOfWeek
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

private const val DAYS_IN_WEEK = 7

private const val WEEK_PAGES = 5_200
private val FirstWeek: LocalDate = startOfWeek(LocalDate.of(2000, 1, 5))

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", AppLocale)

/** The days of the week of [date], from [com.oriteam.ori.domain.routine.FIRST_DAY_OF_WEEK]. */
internal fun weekOf(date: LocalDate): List<LocalDate> {
    val first = startOfWeek(date)
    return List(DAYS_IN_WEEK) { first.plusDays(it.toLong()) }
}

internal fun weekPageOf(date: LocalDate): Int =
    ChronoUnit.WEEKS.between(FirstWeek, date).coerceIn(0L, WEEK_PAGES - 1L).toInt()

/**
 * Month and the seven days of the week, one tap away, with the selected day in a circle. Each
 * day is announced in full, because "Seg 12" alone says little to a screen reader. Dragging sideways
 * changes the week, keeping the same day of the week, like the arrows, which stay for who cannot drag.
 */
@Composable
fun WeekStrip(date: LocalDate, today: LocalDate, onSelectDate: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        CalendarHeader(
            date = date,
            today = today,
            onSelectDate = onSelectDate,
            previous = { date.minusWeeks(1) },
            next = { date.plusWeeks(1) },
            previousLabel = stringResource(R.string.week_previous),
            nextLabel = stringResource(R.string.week_next)
        )
        SettlingPager(
            page = weekPageOf(date),
            pageCount = WEEK_PAGES,
            onDragSettled = { settled -> onSelectDate(date.plusWeeks((settled - weekPageOf(date)).toLong())) }
        ) { shown ->
            Row(modifier = Modifier.selectableGroup()) {
                weekOf(FirstWeek.plusWeeks(shown.toLong())).forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day.dayOfWeek.shortName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        DayCell(
                            day = day,
                            isSelected = day == date,
                            isToday = day == today,
                            isDimmed = YearMonth.from(day) != YearMonth.from(date),
                            onClick = { onSelectDate(day) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

internal fun DayOfWeek.shortName() = getDisplayName(TextStyle.SHORT, AppLocale)
    .removeSuffix(".")
    .replaceFirstChar { it.uppercase() }

/** The month, today and the arrows, the same above a week and above a month. */
@Composable
internal fun CalendarHeader(
    date: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    previous: () -> LocalDate,
    next: () -> LocalDate,
    previousLabel: String,
    nextLabel: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = date.format(monthFormat).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (date != today) {
            TextButton(onClick = { onSelectDate(today) }) {
                Text(text = stringResource(R.string.week_today), style = MaterialTheme.typography.labelLarge)
            }
        }
        IconButton(onClick = { onSelectDate(previous()) }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = previousLabel)
        }
        IconButton(onClick = { onSelectDate(next()) }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = nextLabel)
        }
    }
}

/**
 * Follows [page], and reports only where a drag ended: the pager also settles after following the
 * arrows, and that must not choose a date again.
 */
@Composable
internal fun SettlingPager(
    page: Int,
    pageCount: Int,
    onDragSettled: (Int) -> Unit,
    content: @Composable (Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = page) { pageCount }
    val animates = LocalOriAppearance.current.motion.animatesScrolling
    val currentPage by rememberUpdatedState(page)
    val currentOnSettled by rememberUpdatedState(onDragSettled)
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val movedByDrag = remember { booleanArrayOf(false) }
    if (isDragged) movedByDrag[0] = true

    LaunchedEffect(page) {
        if (pagerState.currentPage != page) {
            if (animates) pagerState.animateScrollToPage(page) else pagerState.scrollToPage(page)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settled ->
            if (movedByDrag[0] && settled != currentPage) currentOnSettled(settled)
            movedByDrag[0] = false
        }
    }
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { content(it) }
}
