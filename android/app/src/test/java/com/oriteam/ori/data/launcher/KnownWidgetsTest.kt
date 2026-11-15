package com.oriteam.ori.data.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The picture of each widget and adding one look in the list read last: asking Android for the whole list once per
 * row closed ORI Home on the test tablet (77 widgets, too big an answer). A widget not in it is asked to its app only.
 */
class KnownWidgetsTest {
    private val known = KnownWidgets<String> { it.substringBefore(':') }

    @Test
    fun `a widget of the list read last is found without asking Android again`() {
        known.remember(listOf("clock/.Big:1", "notes/.Note:2"))
        var asked = 0

        val found = known.find("notes/.Note") {
            asked++
            emptyList()
        }

        assertEquals("notes/.Note:2", found)
        assertEquals(0, asked)
    }

    @Test
    fun `a widget missing from the list is asked to its app once, and kept`() {
        known.remember(listOf("clock/.Big:1"))
        var asked = 0
        val readApp = {
            asked++
            listOf("weather/.Now:3")
        }

        assertEquals("weather/.Now:3", known.find("weather/.Now", readApp))
        assertEquals("weather/.Now:3", known.find("weather/.Now", readApp))
        assertEquals(1, asked)
    }

    @Test
    fun `when Android cannot answer, the widget is not found, and nothing fails`() {
        known.remember(emptyList())

        assertNull(known.find("weather/.Now") { null })
        assertNull(known.find("weather/.Now") { listOf("other/.One:4") })
    }
}
