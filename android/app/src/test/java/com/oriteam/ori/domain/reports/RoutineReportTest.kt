package com.oriteam.ori.domain.reports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RoutineReportTest {
    private val wednesday = LocalDate.of(2026, 11, 4)

    @Test
    fun `a week starts on its first day and a month on the first, and neither goes past today`() {
        assertEquals(
            listOf(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), wednesday),
            ReportPeriod.WEEK.daysUntil(wednesday)
        )
        assertEquals(4, ReportPeriod.MONTH.daysUntil(wednesday).size)
        assertEquals(LocalDate.of(2026, 11, 1), ReportPeriod.MONTH.daysUntil(wednesday).first())
    }

    @Test
    fun `days without activities do not lower the percentage`() {
        val report = RoutineReport(
            days = listOf(
                DayResult(wednesday, done = 1, total = 2),
                DayResult(wednesday.plusDays(1), done = 0, total = 0)
            ),
            speechCount = 0
        )

        assertEquals(50, report.completionPercent)
        assertEquals(1, report.daysWithActivities.size)
    }

    @Test
    fun `no activities and no speech is an empty report, not zero percent of something`() {
        assertTrue(RoutineReport(listOf(DayResult(wednesday, 0, 0)), speechCount = 0).isEmpty)
        assertEquals(0, RoutineReport(emptyList(), speechCount = 3).completionPercent)
    }
}
