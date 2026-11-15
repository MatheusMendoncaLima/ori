package com.oriteam.ori.domain.aac

import org.junit.Assert.assertEquals
import org.junit.Test

class StarterVocabularyTest {
    private val vocabulary = StarterVocabulary(
        names = mapOf("pt" to "Básico", "en" to "Basics", "es" to "Básico"),
        words = mapOf(
            "pt" to mapOf("i" to "eu", "hungry" to "fome", "with" to "com"),
            "en" to mapOf("i" to "I", "hungry" to "hungry", "with" to "with"),
            "es" to mapOf("i" to "yo", "hungry" to "hambre", "with" to "con")
        ),
        pictures = mapOf("i" to "🙋", "hungry" to "🍽️", "with" to null)
    )

    private val board = AacBoard("b", "Básico", 0, "u", language = "pt-BR")

    private fun symbol(id: String, label: String, emoji: String?, phrase: String? = null) =
        AacSymbol(id, "b", label, phrase, emoji?.let(SymbolImage::Emoji), 0, "u")

    @Test
    fun `the words of the app are shown in the app's language, and the board speaks it`() {
        val shown = vocabulary.symbolsIn(
            board,
            listOf(symbol("1", "eu", "🙋"), symbol("2", "fome", "🍽️"), symbol("3", "com", null)),
            "en-US"
        )

        assertEquals(listOf("I", "hungry", "with"), shown.symbols.map { it.label })
        assertEquals(listOf("I", "hungry", "with"), shown.symbols.map { it.spokenText })
        assertEquals("en-US", shown.language)
        assertEquals("Basics", vocabulary.boardIn(board, "en-US").name)
    }

    @Test
    fun `what the person wrote or changed stays as written`() {
        val shown = vocabulary.symbolsIn(
            board,
            listOf(
                symbol("1", "Bosta", "🍞"),
                // Another picture, a phrase of its own, another word: the person's now.
                symbol("2", "eu", "📷"),
                symbol("3", "fome", "🍽️", phrase = "Estou com muita fome"),
                symbol("4", "Fome", "🍽️"),
                symbol("5", "eu", "🙋")
            ),
            "es-ES"
        )

        assertEquals(listOf("Bosta", "eu", "fome", "Fome", "yo"), shown.symbols.map { it.label })
    }

    @Test
    fun `a board without words of the app keeps its own language, and a renamed one its name`() {
        val own = vocabulary.symbolsIn(board, listOf(symbol("1", "Bosta", "🍞")), "en-US")
        val renamed = board.copy(name = "Meu básico")

        assertEquals("pt-BR", own.language)
        assertEquals("Meu básico", vocabulary.boardIn(renamed, "en-US").name)
    }

    @Test
    fun `in the board's own language nothing changes, whatever the region`() {
        val symbols = listOf(symbol("1", "eu", "🙋"))

        assertEquals(symbols, vocabulary.symbolsIn(board, symbols, "pt-PT").symbols)
        assertEquals("pt-BR", vocabulary.symbolsIn(board, symbols, "pt-PT").language)
        assertEquals("Básico", vocabulary.boardIn(board, "pt-PT").name)
    }

    @Test
    fun `a board from before boards had a language is Portuguese, and follows the app too`() {
        val old = AacBoard("b", "Básico", 0, "u")

        assertEquals("I", vocabulary.symbolsIn(old, listOf(symbol("1", "eu", "🙋")), "en-US").symbols.single().label)
        assertEquals("Basics", vocabulary.boardIn(old, "en-US").name)
    }

    @Test
    fun `an older suggested board lacks the words the app added since, in its own language and order`() {
        val missing = vocabulary.missingFrom(board, listOf(symbol("1", "eu", "🙋")))

        assertEquals(
            listOf(SymbolDraft("fome", null, SymbolImage.Emoji("🍽️")), SymbolDraft("com", null, null)),
            missing
        )
    }

    @Test
    fun `a word already there in any form is not missing, and a board without words of the app lacks none`() {
        // "eu" with another picture is the person's, but the word is there: no second "eu".
        val missing = vocabulary.missingFrom(board, listOf(symbol("1", "Eu", "📷"), symbol("2", "fome", "🍽️")))

        assertEquals(listOf("com"), missing.map { it.label })
        assertEquals(emptyList<SymbolDraft>(), vocabulary.missingFrom(board, listOf(symbol("1", "Bosta", "🍞"))))
    }

    @Test
    fun `a suggested board made in English follows the app into Portuguese`() {
        val english = AacBoard("b", "Basics", 0, "u", language = "en-US")
        val shown = vocabulary.symbolsIn(english, listOf(symbol("1", "hungry", "🍽️")), "pt-BR")

        assertEquals("fome", shown.symbols.single().label)
        assertEquals("pt-BR", shown.language)
        assertEquals("Básico", vocabulary.boardIn(english, "pt-BR").name)
    }
}
