package com.oriteam.ori.domain.routine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private val businessDays = DayOfWeek.entries.toSet() - setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
private val weekendDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
const val MAX_REPEAT_INTERVAL_WEEKS = 4

/** When an activity comes back. No rule (null) means that it happens only on its date. */
sealed interface RepeatRule {
    /** Whether it happens on [date], for a series that starts on [start]. */
    fun occursOn(date: LocalDate, start: LocalDate): Boolean

    /** The circles of the Samsung Clock: the chosen days, every week or every few weeks. */
    data class Weekly(val days: Set<DayOfWeek>, val intervalWeeks: Int = 1) : RepeatRule {
        override fun occursOn(date: LocalDate, start: LocalDate): Boolean {
            val weeks = ChronoUnit.WEEKS.between(startOfWeek(start), startOfWeek(date))
            return date.dayOfWeek in days && weeks % intervalWeeks == 0L
        }

        val isEveryDay: Boolean get() = intervalWeeks == 1 && days.size == DayOfWeek.entries.size
        val isBusinessDays: Boolean get() = intervalWeeks == 1 && days == businessDays
        val isWeekend: Boolean get() = intervalWeeks == 1 && days == weekendDays
    }

    data object LastDayOfMonth : RepeatRule {
        override fun occursOn(date: LocalDate, start: LocalDate) = date == lastDayOf(date)
    }

    /** Monday to Friday: national and local holidays are not known to the app. */
    data object LastBusinessDayOfMonth : RepeatRule {
        override fun occursOn(date: LocalDate, start: LocalDate) =
            date == lastDayOf(date).with(TemporalAdjusters.previousOrSame(lastWorkingDayOfWeek(lastDayOf(date))))
    }

    companion object {
        val EveryDay = Weekly(DayOfWeek.entries.toSet())
        val BusinessDays = Weekly(businessDays)
        val Weekend = Weekly(weekendDays)
    }
}

private fun lastDayOf(date: LocalDate) = date.with(TemporalAdjusters.lastDayOfMonth())

/** The month ends on a Saturday or a Sunday: the last business day is the Friday before. */
private fun lastWorkingDayOfWeek(lastDay: LocalDate) =
    if (lastDay.dayOfWeek in weekendDays) DayOfWeek.FRIDAY else lastDay.dayOfWeek

/** Tapping a circle: adds or removes the day; removing the last one means that it no longer repeats. */
fun RepeatRule?.togglingDay(day: DayOfWeek): RepeatRule? {
    val weekly = this as? RepeatRule.Weekly ?: return RepeatRule.Weekly(setOf(day))
    val days = if (day in weekly.days) weekly.days - day else weekly.days + day
    return if (days.isEmpty()) null else weekly.copy(days = days)
}

private const val WEEKLY_PREFIX = "weekly"
private const val WEEKLY_PARTS = 3
private const val LAST_DAY = "last_day"
private const val LAST_BUSINESS_DAY = "last_business_day"

/**
 * One short text, the same on the device, in the saved draft and in Firestore, where the rules
 * check its format: "weekly:1,3:2" is Monday and Wednesday, every 2 weeks.
 */
fun RepeatRule.toStoredText(): String = when (this) {
    is RepeatRule.Weekly ->
        "$WEEKLY_PREFIX:${days.map { it.value }.sorted().joinToString(",")}:$intervalWeeks"
    RepeatRule.LastDayOfMonth -> LAST_DAY
    RepeatRule.LastBusinessDayOfMonth -> LAST_BUSINESS_DAY
}

/** Null for a text this version does not understand, so a newer rule is never misread. */
fun repeatRuleFromStoredText(text: String?): RepeatRule? = when (text) {
    LAST_DAY -> RepeatRule.LastDayOfMonth
    LAST_BUSINESS_DAY -> RepeatRule.LastBusinessDayOfMonth
    null -> null
    else -> weeklyFromStoredText(text)
}

private fun weeklyFromStoredText(text: String): RepeatRule? {
    val parts = text.split(":")
    val days = parts.getOrNull(1)?.split(",")?.mapNotNull { it.toIntOrNull() }.orEmpty()
    val interval = parts.getOrNull(2)?.toIntOrNull()
    val isValid = parts.size == WEEKLY_PARTS && parts[0] == WEEKLY_PREFIX && interval in 1..MAX_REPEAT_INTERVAL_WEEKS &&
        days.isNotEmpty() && days.all { it in DayOfWeek.MONDAY.value..DayOfWeek.SUNDAY.value }
    return if (isValid) RepeatRule.Weekly(days.map(DayOfWeek::of).toSet(), interval!!) else null
}
