package com.oriteam.ori.domain.routine

import com.oriteam.ori.testing.DAY
import com.oriteam.ori.testing.OWNER
import com.oriteam.ori.testing.activity
import com.oriteam.ori.testing.manager
import com.oriteam.ori.testing.viewer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class RoutineDomainTest {

    private fun draft(
        title: String = "Tomar café",
        time: LocalTime? = LocalTime.of(
            7,
            30
        ),
        date: java.time.LocalDate = DAY
    ) =
        ActivityDraft(title, date, time, description = null, ownerCanComplete = true, isHiddenFromCaregiver = false)

    @Test
    fun `day order puts timed activities first, then untimed alphabetically, and skips archived`() {
        val day = listOf(
            activity("late", time = LocalTime.of(20, 0)),
            activity("untimedB", title = "b", time = null),
            activity("early", time = LocalTime.of(7, 0)),
            activity("untimedA", title = "A", time = null),
            activity("old", status = TaskStatus.ARCHIVED)
        ).forDay()

        assertEquals(listOf("early", "late", "untimedA", "untimedB"), day.map { it.id })
    }

    @Test
    fun `time input accepts 24-hour times and an empty field`() {
        assertEquals(TimeInput.Valid(LocalTime.of(8, 5)), parseScheduledTime(" 8:05 "))
        assertEquals(TimeInput.Valid(LocalTime.of(23, 59)), parseScheduledTime("23:59"))
        assertEquals(TimeInput.Empty, parseScheduledTime("  "))
        for (invalid in listOf("24:00", "12:60", "8h", "830a", "2460", "08", "ab:cd", "13:00 PM", "0:30 am")) {
            assertEquals(invalid, TimeInput.Invalid, parseScheduledTime(invalid))
        }
    }

    @Test
    fun `time input also takes 7h30 and the 12-hour clock of American English`() {
        assertEquals(TimeInput.Valid(LocalTime.of(7, 30)), parseScheduledTime("7h30"))
        assertEquals(TimeInput.Valid(LocalTime.of(20, 30)), parseScheduledTime("8:30 PM"))
        assertEquals(TimeInput.Valid(LocalTime.of(20, 30)), parseScheduledTime("8:30pm"))
        assertEquals(TimeInput.Valid(LocalTime.of(0, 15)), parseScheduledTime("12:15 a.m."))
        assertEquals(TimeInput.Valid(LocalTime.of(12, 15)), parseScheduledTime("12:15 PM"))
    }

    @Test
    fun `time input takes the numbers of a number keyboard and the time a picker writes`() {
        // G4: with 24 hours the keyboard is of numbers, which may have no ":".
        assertEquals(TimeInput.Valid(LocalTime.of(8, 30)), parseScheduledTime("0830"))
        assertEquals(TimeInput.Valid(LocalTime.of(8, 30)), parseScheduledTime("830"))
        assertEquals(TimeInput.Valid(LocalTime.of(21, 5)), parseScheduledTime("2105"))
        // Android writes a no-break space before AM/PM, and Spanish writes "p. m.".
        assertEquals(TimeInput.Valid(LocalTime.of(20, 30)), parseScheduledTime("8:30 PM"))
        assertEquals(TimeInput.Valid(LocalTime.of(20, 30)), parseScheduledTime("8:30 p. m."))
    }

    @Test
    fun `duplicates match title ignoring case and spaces, on the same date and time`() {
        val existing = listOf(activity("a", title = "Tomar café", time = LocalTime.of(7, 30)))

        assertEquals(ActivityValidationError.DUPLICATE, validateActivity(draft("  tomar   CAFÉ "), existing))
        assertNull(validateActivity(draft(time = LocalTime.of(8, 0)), existing))
        assertNull(validateActivity(draft(date = DAY.plusDays(1)), existing))
        assertNull(validateActivity(draft(), existing, editingId = "a"))
    }

    @Test
    fun `required title and size limits are checked before duplicates`() {
        assertEquals(ActivityValidationError.MISSING_TITLE, validateActivity(draft(title = "  "), emptyList()))
        assertEquals(
            ActivityValidationError.TITLE_TOO_LONG,
            validateActivity(draft(title = "x".repeat(MAX_ACTIVITY_TITLE_LENGTH + 1)), emptyList())
        )
        assertEquals(
            ActivityValidationError.DESCRIPTION_TOO_LONG,
            validateActivity(draft().copy(description = "x".repeat(MAX_ACTIVITY_DESCRIPTION_LENGTH + 1)), emptyList())
        )
    }

    @Test
    fun `primary user manages own activities and only completes those reserved to the caregiver`() {
        val own = activity("own", createdBy = OWNER, ownerCanComplete = true)
        val reserved = activity("reserved", ownerCanComplete = false)
        val completable = activity("completable", ownerCanComplete = true)

        assertTrue(canCreateActivity(RoutineViewer.Owner))
        assertTrue(canEditActivity(own, RoutineViewer.Owner))
        assertTrue(canDeleteActivity(own, RoutineViewer.Owner))
        assertFalse(canEditActivity(reserved, RoutineViewer.Owner))
        assertFalse(canDeleteActivity(completable, RoutineViewer.Owner))
        assertFalse(canToggleCompletion(reserved, RoutineViewer.Owner))
        assertTrue(canToggleCompletion(completable, RoutineViewer.Owner))
    }

    @Test
    fun `caregiver needs manage permission and never touches hidden activities`() {
        val visible = activity("visible")
        val hidden = activity("hidden", createdBy = OWNER, status = TaskStatus.HIDDEN_FROM_CAREGIVER)

        assertTrue(canCreateActivity(manager))
        assertTrue(canEditActivity(visible, manager))
        assertTrue(canToggleCompletion(visible, manager))
        assertFalse(canEditActivity(hidden, manager))
        assertFalse(canCreateActivity(viewer))
        assertFalse(canEditActivity(visible, viewer))
        assertFalse(canToggleCompletion(visible, viewer))
    }
}
