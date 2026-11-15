package com.oriteam.ori.ui.app.screens.aac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The phrases that come with the app, in the order the person chose by dragging them. */
class ReadyPhraseOrderTest {
    private val appOrder = readyPhrases.map { it.id }

    @Test
    fun `every phrase has its own id, which is what the order keeps`() {
        assertEquals(appOrder.distinct(), appOrder)
        assertEquals(readyPhrases.map { it.textRes }.distinct().size, readyPhrases.size)
    }

    @Test
    fun `with no order chosen, the app's order`() {
        assertEquals(readyPhrases, readyPhrasesIn(emptyList()))
    }

    @Test
    fun `the chosen ones first, in their order, then the rest in the app's order, skipping ids no longer there`() {
        val shown = readyPhrasesIn(listOf("help", "gone", "yes", "help")).map { it.id }

        assertEquals(listOf("help", "yes") + (appOrder - setOf("help", "yes")), shown)
        assertEquals(readyPhrases.size, shown.size)
    }

    @Test
    fun `moving one gives the order of every phrase, and a move that changes nothing gives none`() {
        val moved = readyOrderAfterMoving(emptyList(), "pain", 0)

        assertEquals(listOf("pain") + (appOrder - "pain"), moved)
        val again = readyOrderAfterMoving(moved!!, "yes", 0)
        assertEquals(listOf("yes", "pain") + (appOrder - setOf("yes", "pain")), again)
        assertNull(readyOrderAfterMoving(emptyList(), "yes", 0))
        assertNull(readyOrderAfterMoving(emptyList(), "yes", appOrder.size))
        assertNull(readyOrderAfterMoving(emptyList(), "gone", 0))
    }
}
