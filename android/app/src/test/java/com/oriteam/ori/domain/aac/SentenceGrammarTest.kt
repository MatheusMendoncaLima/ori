package com.oriteam.ori.domain.aac

import org.junit.Assert.assertEquals
import org.junit.Test

class SentenceGrammarTest {
    private fun say(vararg words: String) = composeSentence(words.toList()).toSpokenSentence()

    @Test
    fun `the verb brings the word it needs`() {
        assertEquals("Eu gosto de você", say("eu", "gosto", "você"))
        assertEquals("Eu preciso de ajuda", say("eu", "preciso", "ajuda"))
        assertEquals("Estou com dor", say("estou", "dor"))
    }

    @Test
    fun `a word the person already tapped is not repeated`() {
        assertEquals("Estou com dor", say("estou", "com", "dor"))
        assertEquals("Eu gosto de você", say("eu", "gosto", "de", "você"))
    }

    @Test
    fun `nothing is added where it does not belong`() {
        assertEquals("Eu quero água", say("eu", "quero", "água"))
        assertEquals("Estou feliz", say("estou", "feliz"))
        assertEquals("Preciso comer", say("preciso", "comer"))
        assertEquals("Água gelada", say("água", "gelada"))
    }

    @Test
    fun `the verb agrees with whoever comes before it`() {
        assertEquals("Você quer água", say("você", "quero", "água"))
        assertEquals("Você está com sono", say("você", "estou", "sono"))
        assertEquals("Nós gostamos de você", say("nós", "gosto", "você"))
        assertEquals("Eu não quero água", say("eu", "não", "quero", "água"))
    }

    @Test
    fun `a pronoun after the verb is not its subject`() {
        assertEquals("Eu gosto de você", say("eu", "gosto", "você"))
        assertEquals("Eu não gosto de você", say("eu", "não", "gosto", "você"))
    }

    @Test
    fun `negation keeps the verb connected to what follows`() {
        assertEquals("Não gosto de banho", say("não", "gosto", "banho"))
        assertEquals("Eu gosto não", say("eu", "gosto", "não"))
    }

    @Test
    fun `contractions are said as they are in Portuguese`() {
        assertEquals("Eu gosto dele", say("eu", "gosto", "ele"))
        assertEquals("Você gosta de mim", say("você", "gosto", "eu"))
        assertEquals("Estou comigo", say("estou", "com", "eu"))
    }

    @Test
    fun `case on old boards does not change the result`() {
        assertEquals("Estou com Dor", say("estou", "Dor"))
        assertEquals("Eu gosto de Você", say("Eu", "Gosto", "Você").replace("Eu Gosto", "Eu gosto"))
    }

    @Test
    fun `each tapped word keeps its own piece, so it can be removed`() {
        val pieces = composeSentence(listOf("eu", "gosto", "você"))

        assertEquals(listOf("eu", "gosto", "você"), pieces.map { it.text })
        assertEquals(listOf(null, null, "de"), pieces.map { it.connector })
    }

    @Test
    fun `a symbol alone still says its own phrase`() {
        val message = AacMessage().add(MessagePart(label = "Dor", spokenText = "Estou com dor"))

        assertEquals("Estou com dor", message.text)
    }
}
