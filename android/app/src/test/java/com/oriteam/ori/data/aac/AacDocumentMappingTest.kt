package com.oriteam.ori.data.aac

import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.SymbolImage
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
    fun `a new symbol has every field the rules require`() {
        val document = SymbolDraft("Água", null, null).toNewDocument(createdBy = "pat", position = 2)

        assertEquals(
            setOf(
                FirestoreSchema.LABEL,
                FirestoreSchema.PHRASE,
                FirestoreSchema.IMAGE,
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
                FirestoreSchema.POSITION,
                FirestoreSchema.CREATED_BY,
                FirestoreSchema.CREATED_AT,
                FirestoreSchema.UPDATED_AT
            ),
            newBoardDocument("Básico", 0, "pat").keys
        )
    }
}
