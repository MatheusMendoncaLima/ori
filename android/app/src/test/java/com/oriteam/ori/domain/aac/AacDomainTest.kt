package com.oriteam.ori.domain.aac

import com.oriteam.ori.testing.board
import com.oriteam.ori.testing.communicationManager
import com.oriteam.ori.testing.routineOnlyCaregiver
import com.oriteam.ori.testing.symbol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AacDomainTest {

    private fun draft(label: String = "Água", phrase: String? = null, image: SymbolImage? = null) =
        SymbolDraft(label, phrase, image)

    @Test
    fun `a symbol needs a label within the limits and a single short emoji`() {
        assertEquals(SymbolValidationError.MISSING_LABEL, validateSymbol(draft(label = "  "), emptyList(), null))
        assertEquals(
            SymbolValidationError.LABEL_TOO_LONG,
            validateSymbol(draft(label = "a".repeat(MAX_SYMBOL_LABEL_LENGTH + 1)), emptyList(), null)
        )
        assertNull(validateSymbol(draft(label = "a".repeat(MAX_SYMBOL_LABEL_LENGTH)), emptyList(), null))
        assertEquals(
            SymbolValidationError.PHRASE_TOO_LONG,
            validateSymbol(draft(phrase = "a".repeat(MAX_SYMBOL_PHRASE_LENGTH + 1)), emptyList(), null)
        )
        assertEquals(
            SymbolValidationError.INVALID_EMOJI,
            validateSymbol(draft(image = SymbolImage.Emoji("💧".repeat(MAX_EMOJI_LENGTH))), emptyList(), null)
        )
        // Family and flag emoji are several code units long and must still be accepted.
        assertNull(validateSymbol(draft(image = SymbolImage.Emoji("👨‍👩‍👧")), emptyList(), null))
    }

    @Test
    fun `duplicate labels in the same board are refused, ignoring case and spaces`() {
        val existing = listOf(symbol("s1", label = "Água"))

        assertEquals(SymbolValidationError.DUPLICATE_LABEL, validateSymbol(draft(" água "), existing, null))
        // Editing a symbol does not compare it with itself.
        assertNull(validateSymbol(draft("Água"), existing, editingId = "s1"))
        assertNull(validateSymbol(draft("Comer"), existing, null))
    }

    @Test
    fun `a full board refuses new symbols but still allows edits`() {
        val full = (1..MAX_SYMBOLS_PER_BOARD).map { symbol("s$it", label = "S$it") }

        assertEquals(SymbolValidationError.BOARD_FULL, validateSymbol(draft("Novo"), full, null))
        assertNull(validateSymbol(draft("Renomeado"), full, editingId = "s1"))
    }

    @Test
    fun `board names are required, short, unique and limited in number`() {
        val boards = listOf(board("b1", name = "Comida"))

        assertEquals(BoardValidationError.MISSING_NAME, validateBoardName(" ", boards, null))
        assertEquals(
            BoardValidationError.NAME_TOO_LONG,
            validateBoardName("a".repeat(MAX_BOARD_NAME_LENGTH + 1), boards, null)
        )
        assertEquals(BoardValidationError.DUPLICATE_NAME, validateBoardName("comida", boards, null))
        assertNull(validateBoardName("Comida", boards, editingId = "b1"))
        val many = (1..MAX_AAC_BOARDS).map { board("b$it", name = "P$it") }
        assertEquals(BoardValidationError.LIMIT_REACHED, validateBoardName("Nova", many, null))
    }

    @Test
    fun `a symbol alone says its phrase, and in a built message it says its word`() {
        val water = symbol("s1", label = "Água", phrase = "Eu quero água")
        val alone = AacMessage().add(water.toMessagePart())
        val message = alone.add(typedMessagePart(" agora ")!!)

        assertEquals("Eu quero água", alone.text)
        assertEquals("Água agora", message.text)
        assertEquals(listOf("Água", "agora"), message.parts.map { it.label })
        assertNull(typedMessagePart("   "))
    }

    @Test
    fun `removing pieces ignores stale indexes`() {
        val message = AacMessage().add(MessagePart("a", "a")).add(MessagePart("b", "b"))

        assertEquals(listOf("b"), message.removeAt(0).parts.map { it.label })
        assertEquals(listOf("a"), message.removeLast().parts.map { it.label })
        assertEquals(message, message.removeAt(5))
        assertEquals(AacMessage(), AacMessage().removeLast())
    }

    @Test
    fun `a full message ignores new pieces and a long one cannot be saved`() {
        val full = (1..MAX_MESSAGE_PARTS).fold(AacMessage()) { message, i -> message.add(MessagePart("$i", "$i")) }

        assertTrue(full.isFull)
        assertEquals(full, full.add(MessagePart("x", "x")))
        val long = AacMessage().add(MessagePart("x", "a".repeat(MAX_SAVED_MESSAGE_LENGTH + 1)))
        assertFalse(long.canBeSaved)
        assertFalse(AacMessage().canBeSaved)
    }

    @Test
    fun `moving an item rewrites only the positions that change`() {
        val order = listOf("a", "b", "c", "d")

        assertEquals(listOf("a", "c", "b", "d"), order.moved(2, 1))
        assertEquals(order, order.moved(0, -1))
        assertEquals(order, order.moved(3, 4))
        assertEquals(
            mapOf("c" to 1, "b" to 2),
            positionChanges(mapOf("a" to 0, "b" to 1, "c" to 2, "d" to 3), listOf("a", "c", "b", "d"))
        )
    }

    @Test
    fun `ties left by concurrent additions are repaired by the next move`() {
        // Two people added a symbol at the same time: both got position 1.
        val changes = positionChanges(mapOf("a" to 0, "b" to 1, "c" to 1), listOf("a", "c", "b"))

        assertEquals(mapOf("b" to 2), changes)
    }

    @Test
    fun `items are ordered by position, then name, so ties are stable`() {
        val symbols =
            listOf(
                symbol("z", "Zebra", position = 1),
                symbol("b", "Bola", position = 1),
                symbol("a", "Água", position = 0)
            )

        assertEquals(listOf("a", "b", "z"), symbols.sortedWith(symbolOrder).map { it.id })
        assertEquals(3, nextPosition(listOf(0, 2, 1)))
        assertEquals(0, nextPosition(emptyList()))
    }

    @Test
    fun `permissions follow the primary user's choices`() {
        assertTrue(canManageBoards(AacViewer.Owner))
        assertTrue(canManageBoards(communicationManager))
        assertFalse(canManageBoards(routineOnlyCaregiver))
        // Saved messages and device photos are the person's own, whatever the caregiver may do.
        assertTrue(canUseSavedMessages(AacViewer.Owner))
        assertFalse(canUseSavedMessages(communicationManager))
        assertTrue(canUseDevicePhotos(AacViewer.Owner))
        assertFalse(canUseDevicePhotos(communicationManager))
    }
}
