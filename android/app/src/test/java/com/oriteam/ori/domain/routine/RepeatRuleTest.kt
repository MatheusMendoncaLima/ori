package com.oriteam.ori.domain.routine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RepeatRuleTest {
    private val monday = LocalDate.of(2026, 11, 2)

    @Test
    fun `weekly rule happens on the chosen days only`() {
        val rule = RepeatRule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))

        assertTrue(rule.occursOn(monday, monday))
        assertTrue(rule.occursOn(monday.plusDays(2), monday))
        assertFalse(rule.occursOn(monday.plusDays(1), monday))
    }

    @Test
    fun `every two weeks counts from the week of the first day`() {
        val rule = RepeatRule.Weekly(setOf(DayOfWeek.MONDAY), intervalWeeks = 2)
        // The first day is a Friday: the start of its own week counts as week zero.
        val friday = monday.plusDays(4)

        assertFalse(rule.occursOn(monday.plusWeeks(1), friday))
        assertTrue(rule.occursOn(monday.plusWeeks(2), friday))
    }

    @Test
    fun `last day of the month follows the month, including leap years`() {
        val rule = RepeatRule.LastDayOfMonth

        assertTrue(rule.occursOn(LocalDate.of(2026, 11, 30), monday))
        assertFalse(rule.occursOn(LocalDate.of(2026, 11, 29), monday))
        assertTrue(rule.occursOn(LocalDate.of(2028, 2, 29), monday))
    }

    @Test
    fun `last business day falls back to the Friday when the month ends on a weekend`() {
        val rule = RepeatRule.LastBusinessDayOfMonth

        // Monday, November 30.
        assertTrue(rule.occursOn(LocalDate.of(2026, 11, 30), monday))
        // January 31, 2027 is a Sunday; the Friday before is the 29th.
        assertTrue(rule.occursOn(LocalDate.of(2027, 1, 29), monday))
        assertFalse(rule.occursOn(LocalDate.of(2027, 1, 31), monday))
        // October 31, 2026 is a Saturday; the Friday before is the 30th.
        assertTrue(rule.occursOn(LocalDate.of(2026, 10, 30), monday))
    }

    @Test
    fun `tapping circles adds days and clearing the last one stops repeating`() {
        val first = null.togglingDay(DayOfWeek.MONDAY)
        val second = first.togglingDay(DayOfWeek.FRIDAY)

        assertEquals(RepeatRule.Weekly(setOf(DayOfWeek.MONDAY)), first)
        assertEquals(RepeatRule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)), second)
        assertEquals(first, second.togglingDay(DayOfWeek.FRIDAY))
        assertNull(first.togglingDay(DayOfWeek.MONDAY))
        assertEquals(
            RepeatRule.Weekly(setOf(DayOfWeek.MONDAY)),
            RepeatRule.LastDayOfMonth.togglingDay(DayOfWeek.MONDAY)
        )
    }

    @Test
    fun `a rule survives being stored, and an unknown one is never misread`() {
        listOf(
            RepeatRule.EveryDay,
            RepeatRule.Weekly(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY), intervalWeeks = 3),
            RepeatRule.LastDayOfMonth,
            RepeatRule.LastBusinessDayOfMonth
        ).forEach { assertEquals(it, repeatRuleFromStoredText(it.toStoredText())) }

        assertEquals("weekly:1,7:3", RepeatRule.Weekly(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY), 3).toStoredText())
        listOf("yearly", "weekly::1", "weekly:9:1", "weekly:1:9", "weekly:1").forEach {
            assertNull(it, repeatRuleFromStoredText(it))
        }
    }
}
