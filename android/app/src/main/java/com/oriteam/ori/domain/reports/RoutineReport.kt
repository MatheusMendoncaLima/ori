package com.oriteam.ori.domain.reports

import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.forDay
import com.oriteam.ori.domain.routine.startOfWeek
import java.time.LocalDate

private const val PERCENT = 100

/** Which days the report covers. */
enum class ReportPeriod {
    WEEK,
    MONTH;

    /** From the first day of the period until [today]: the future has nothing to report yet. */
    fun daysUntil(today: LocalDate): List<LocalDate> {
        val start = when (this) {
            WEEK -> startOfWeek(today)
            MONTH -> today.withDayOfMonth(1)
        }
        return generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
    }
}

/** One day of the report. */
data class DayResult(val date: LocalDate, val done: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else done * PERCENT / total
}

/**
 * The numbers of the "Relatórios" screen. Days without activities stay out of the percentage:
 * a day with nothing planned is not a day that failed.
 */
data class RoutineReport(val days: List<DayResult>, val speechCount: Int) {
    val done: Int get() = days.sumOf { it.done }
    val total: Int get() = days.sumOf { it.total }
    val completionPercent: Int get() = if (total == 0) 0 else done * PERCENT / total
    val daysWithActivities: List<DayResult> get() = days.filter { it.total > 0 }
    val isEmpty: Boolean get() = total == 0 && speechCount == 0
}

fun dayResultOf(date: LocalDate, tasks: List<RoutineTask>): DayResult {
    val ofDay = tasks.forDay()
    return DayResult(date, done = ofDay.count { it.isDone }, total = ofDay.size)
}
