package com.oriteam.ori.ui.app.screens.routine

import com.oriteam.ori.domain.routine.firstDayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TasksAndWeekTest {
    private val steps = listOf(
        RoutineStepUiModel("a", "Banho", isDone = true),
        RoutineStepUiModel("b", "Remédio")
    )

    @Test
    fun `each filter shows its activities`() {
        assertEquals(listOf("b"), steps.filteredBy(TaskFilter.PENDING).map { it.id })
        assertEquals(listOf("a"), steps.filteredBy(TaskFilter.DONE).map { it.id })
        assertEquals(listOf("a", "b"), steps.filteredBy(TaskFilter.ALL).map { it.id })
    }

    @Test
    fun `an activity marked a moment ago stays among the pending ones`() {
        assertEquals(listOf("a", "b"), steps.filteredBy(TaskFilter.PENDING, justDone = setOf("a")).map { it.id })
    }

    @Test
    fun `a week has seven days from its first one, also when the day is the last one`() {
        val saturday = LocalDate.of(2026, 11, 7)

        val week = weekOf(saturday)

        assertEquals(7, week.size)
        assertEquals(firstDayOfWeek, week.first().dayOfWeek)
        assertEquals(saturday, week.last())
    }
}
