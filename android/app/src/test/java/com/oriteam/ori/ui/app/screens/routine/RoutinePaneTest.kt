package com.oriteam.ori.ui.app.screens.routine

import com.oriteam.ori.testing.DAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Saving goes back to where the person was, the same way in Rotina and in Tarefas. */
class RoutinePaneTest {
    private fun saved(date: LocalDate?) = ActivityEditorViewModel.Event.Saved("a", date, isPendingSync = false)

    @Test
    fun `creating goes back to the list`() {
        assertNull(RoutinePane.New.afterSave(saved(DAY)))
        assertNull(RoutinePane.New.afterSave(saved(date = null)))
    }

    @Test
    fun `editing goes back to the details of the activity`() {
        assertEquals(RoutinePane.Details("a"), RoutinePane.Edit("a").afterSave(saved(DAY)))
    }

    @Test
    fun `editing a repeating activity goes back to the list, because it may not fall on the day shown`() {
        assertNull(RoutinePane.Edit("a").afterSave(saved(date = null)))
    }
}
