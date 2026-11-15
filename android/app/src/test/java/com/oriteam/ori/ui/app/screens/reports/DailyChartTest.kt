package com.oriteam.ori.ui.app.screens.reports

import com.oriteam.ori.domain.reports.DayResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DailyChartTest {

    private val sunday = LocalDate.of(2026, 11, 1)

    @Test
    fun `days sit on the chart by date, so days without activities keep their place`() {
        // Activities on Sunday and on Saturday only: the five days between are on the chart, empty.
        val week = (0L..6L).map { index ->
            val hasActivities = index == 0L || index == 6L
            DayResult(sunday.plusDays(index), done = if (hasActivities) 1 else 0, total = if (hasActivities) 2 else 0)
        }

        val points = chartPoints(week)

        assertEquals(0f to 0.5f, points.first())
        assertEquals(1f to 0.5f, points.last())
        (1..5).forEach { assertNull(points[it]) }
    }

    @Test
    fun `a single day sits in the middle`() {
        assertEquals(0.5f to 1f, chartPoints(listOf(DayResult(sunday, done = 2, total = 2))).single())
    }
}
