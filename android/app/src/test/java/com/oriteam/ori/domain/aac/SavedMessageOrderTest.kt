package com.oriteam.ori.domain.aac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedMessageOrderTest {
    private val newest = SavedMessage("n", "Oi")
    private val older = SavedMessage("o", "Tchau")
    private val movedFirst = SavedMessage("a", "Sim", position = 0)
    private val movedSecond = SavedMessage("b", "Não", position = 1)

    @Test
    fun `phrases never moved stay on top, newest first, and the moved ones follow in their order`() {
        val read = listOf(movedSecond, newest, movedFirst, older)

        assertEquals(listOf(newest, older, movedFirst, movedSecond), read.inChosenOrder())
    }

    @Test
    fun `a move writes only the places that change, and gives one to the phrases never moved`() {
        val shown = listOf(newest, movedFirst, movedSecond)

        // "Sim" stays first, at position 0: nothing to write for it.
        assertEquals(mapOf("n" to 1, "b" to 2), shown.positionsAfterMoving("n", 1))
        assertEquals(mapOf("b" to 0, "n" to 1, "a" to 2), shown.positionsAfterMoving("b", 0))
    }

    @Test
    fun `nothing is written for a phrase in its place, out of the list or moved past the end`() {
        val shown = listOf(newest, movedFirst)

        assertNull(shown.positionsAfterMoving("n", 0))
        assertNull(shown.positionsAfterMoving("x", 0))
        assertNull(shown.positionsAfterMoving("n", 2))
        assertNull(shown.positionsAfterMoving("n", -1))
    }
}
