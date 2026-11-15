package com.oriteam.ori.domain.routine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

class WeekStartTest {

    @Test
    fun `each language has its own first day of the week on screen`() {
        assertEquals(DayOfWeek.SUNDAY, firstDayOfWeekFor(Locale.forLanguageTag("pt-BR")))
        assertEquals(DayOfWeek.SUNDAY, firstDayOfWeekFor(Locale.US))
        assertEquals(DayOfWeek.MONDAY, firstDayOfWeekFor(Locale.forLanguageTag("es-ES")))
    }

    @Test
    fun `a series every two weeks keeps its days whatever week the screen shows`() {
        // Sunday 1 and Monday 2 of November are one week of the repetitions; with weeks starting on Monday
        // they would be two, and the Monday would move a week.
        val rule = RepeatRule.Weekly(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY), intervalWeeks = 2)
        val start = LocalDate.of(2026, 11, 1)

        assertTrue(rule.occursOn(LocalDate.of(2026, 11, 2), start))
        assertFalse(rule.occursOn(LocalDate.of(2026, 11, 9), start))
        assertTrue(rule.occursOn(LocalDate.of(2026, 11, 16), start))
        assertEquals(DayOfWeek.SUNDAY, REPEAT_WEEK_START)
    }
}
