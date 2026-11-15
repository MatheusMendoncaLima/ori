package com.oriteam.ori.ui.app.screens.routine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** The color of an activity says when it happens, not which activity it is. */
class DayPartTest {

    @Test
    fun `morning from five, afternoon from noon, night from six in the evening, and no time apart`() {
        assertEquals(DayPart.NIGHT, dayPartOf(LocalTime.of(4, 59)))
        assertEquals(DayPart.MORNING, dayPartOf(LocalTime.of(5, 0)))
        assertEquals(DayPart.MORNING, dayPartOf(LocalTime.of(11, 59)))
        assertEquals(DayPart.AFTERNOON, dayPartOf(LocalTime.of(12, 0)))
        assertEquals(DayPart.AFTERNOON, dayPartOf(LocalTime.of(17, 59)))
        assertEquals(DayPart.NIGHT, dayPartOf(LocalTime.of(18, 0)))
        assertEquals(DayPart.NIGHT, dayPartOf(LocalTime.MIDNIGHT))
        assertEquals(DayPart.ANY_TIME, dayPartOf(null))
    }
}
