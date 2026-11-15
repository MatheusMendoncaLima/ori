package com.oriteam.ori.domain.routine

import com.oriteam.ori.domain.common.appLocaleFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * The week of the repetitions ("a cada 2 semanas"): fixed, and never to be changed. It decides on which weeks a
 * series falls, so it must be the same on every device and in every language: a series "every 2 weeks, Sunday
 * and Monday" falls on other days if the week starts on Monday. Changing it would move the days of the series
 * already saved.
 */
val REPEAT_WEEK_START: DayOfWeek = DayOfWeek.SUNDAY

private const val DAYS_IN_WEEK = 7

/** The first day of the week as [locale] writes calendars: Sunday in Brazil and the United States, Monday in Spain. */
fun firstDayOfWeekFor(locale: Locale): DayOfWeek = WeekFields.of(locale).firstDayOfWeek

/**
 * The first day of the week on screen (calendars, "Esta semana" of the reports), in the language of the app
 * ([appLocaleFor]). Read at each use, so a change of language shows at once. It never decides a repetition:
 * that is [REPEAT_WEEK_START].
 */
val firstDayOfWeek: DayOfWeek
    get() = firstDayOfWeekFor(appLocaleFor(listOf(Locale.getDefault())))

/** The first day of the week that contains [date]. */
fun startOfWeek(date: LocalDate, firstDay: DayOfWeek = firstDayOfWeek): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(firstDay))

/** The seven days in the order the calendars show them. */
val daysOfWeekInOrder: List<DayOfWeek>
    get() = firstDayOfWeek.let { first -> List(DAYS_IN_WEEK) { first.plus(it.toLong()) } }
