package com.oriteam.ori.data.aac

import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.domain.aac.SymbolColor
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.aac.WordClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AacDocumentMappingTest {

    @Test
    fun `images are stored as short references and read back`() {
        listOf(SymbolImage.Emoji("💧"), SymbolImage.DevicePhoto("abc-1.jpg")).forEach { image ->
            assertEquals(image, symbolImageFrom(image.toStoredImage()))
        }
        assertEquals("emoji:💧", SymbolImage.Emoji("💧").toStoredImage())
        // Unknown formats (for example a future image type) read as "no image", not as a crash.
        assertNull(symbolImageFrom("url:https://example.com/a.png"))
        assertNull(symbolImageFrom(42))
    }

    @Test
    fun `symbols without label or author are skipped`() {
        val valid = mapOf(
            FirestoreSchema.LABEL to "Água",
            FirestoreSchema.PHRASE to "Eu quero água",
            FirestoreSchema.IMAGE to "emoji:💧",
            FirestoreSchema.POSITION to 3L,
            FirestoreSchema.CREATED_BY to "pat"
        )

        val symbol = aacSymbolFrom("s1", "b1", valid)!!
        assertEquals("Eu quero água", symbol.spokenText)
        assertEquals(3, symbol.position)
        assertNull(aacSymbolFrom("s1", "b1", valid - FirestoreSchema.LABEL))
        assertNull(aacSymbolFrom("s1", "b1", valid - FirestoreSchema.CREATED_BY))
        assertNull(aacBoardFrom("b1", mapOf(FirestoreSchema.POSITION to 0L)))
    }

    @Test
    fun `a symbol keeps its chosen color, and an unknown color follows the kind of word`() {
        val base = mapOf(FirestoreSchema.LABEL to "Água", FirestoreSchema.CREATED_BY to "pat")

        assertEquals(SymbolColor.PINK, aacSymbolFrom("s1", "b1", base + (FirestoreSchema.COLOR to "pink"))?.color)
        assertNull(aacSymbolFrom("s1", "b1", base + (FirestoreSchema.COLOR to "gold"))?.color)
        assertNull(aacSymbolFrom("s1", "b1", base)?.color)
        assertEquals(
            WordClass.PERSON,
            aacSymbolFrom("s1", "b1", base + (FirestoreSchema.WORD_CLASS to "person"))?.wordClass
        )
        assertNull(aacSymbolFrom("s1", "b1", base + (FirestoreSchema.WORD_CLASS to "animal"))?.wordClass)
        assertEquals(
            "none",
            SymbolDraft("Água", null, null, SymbolColor.NONE).toEditableFields()[FirestoreSchema.COLOR]
        )
    }

    @Test
    fun `a new symbol has every field the rules require`() {
        val document = SymbolDraft("Água", null, null).toNewDocument(createdBy = "pat", position = 2)

        assertEquals(
            setOf(
                FirestoreSchema.LABEL,
                FirestoreSchema.PHRASE,
                FirestoreSchema.IMAGE,
                // Always written, null without a choice: the rules accept both.
                FirestoreSchema.COLOR,
                FirestoreSchema.WORD_CLASS,
                FirestoreSchema.POSITION,
                FirestoreSchema.CREATED_BY,
                FirestoreSchema.CREATED_AT,
                FirestoreSchema.UPDATED_AT
            ),
            document.keys
        )
        assertEquals(
            setOf(
                FirestoreSchema.NAME,
                FirestoreSchema.LANGUAGE,
                FirestoreSchema.POSITION,
                FirestoreSchema.CREATED_BY,
                FirestoreSchema.CREATED_AT,
                FirestoreSchema.UPDATED_AT
            ),
            newBoardDocument("Básico", 0, "pat", "pt-BR").keys
        )
    }

    @Test
    fun `boards and saved messages keep their language, and those from before it existed have none`() {
        val base = mapOf(FirestoreSchema.NAME to "Comida", FirestoreSchema.CREATED_BY to "pat")

        assertEquals("es", aacBoardFrom("b", base + (FirestoreSchema.LANGUAGE to "es"))?.language)
        assertEquals(null, aacBoardFrom("b", base)?.language)
        val spanish = mapOf(FirestoreSchema.TEXT to "Hola", FirestoreSchema.LANGUAGE to "es")
        assertEquals("es", savedMessageFrom("m", spanish)?.language)
        assertEquals(null, savedMessageFrom("m", mapOf(FirestoreSchema.TEXT to "Oi"))?.language)
    }
}
