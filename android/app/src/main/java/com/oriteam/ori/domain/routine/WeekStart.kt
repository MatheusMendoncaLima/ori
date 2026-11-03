package com.oriteam.ori.domain.routine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The first day of the week in every calendar of the app, in "esta semana" of the reports and in
 * "a cada 2 semanas". Brazilian calendars start on Sunday. To start on Monday, as calendars in
 * English often do, change only this value: everything else follows it.
 */
val FIRST_DAY_OF_WEEK: DayOfWeek = DayOfWeek.SUNDAY

private const val DAYS_IN_WEEK = 7

/** The first day of the week that contains [date]. */
fun startOfWeek(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(FIRST_DAY_OF_WEEK))

/** The seven days in the order the calendars show them. */
val daysOfWeekInOrder: List<DayOfWeek> = List(DAYS_IN_WEEK) { FIRST_DAY_OF_WEEK.plus(it.toLong()) }
