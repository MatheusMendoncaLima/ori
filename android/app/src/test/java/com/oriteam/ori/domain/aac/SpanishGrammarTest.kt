package com.oriteam.ori.domain.aac

import org.junit.Assert.assertEquals
import org.junit.Test

class SpanishGrammarTest {
    private fun say(vararg words: String) = SpanishGrammar.spoken(SpanishGrammar.compose(words.toList()))

    @Test
    fun `the verb agrees with whoever comes before it`() {
        assertEquals("Tú tienes hambre", say("tú", "tengo", "hambre"))
        assertEquals("Ella quiere agua", say("ella", "quiero", "agua"))
        assertEquals("Nosotros necesitamos ayuda", say("nosotros", "necesito", "ayuda"))
        assertEquals("Ellos están con dolor", say("ellos", "estoy", "dolor"))
        assertEquals("Yo no quiero comida", say("yo", "no", "quiero", "comida"))
    }

    @Test
    fun `the verb brings the word it needs`() {
        assertEquals("Tengo que ir", say("tengo", "ir"))
        assertEquals("Estoy con dolor", say("estoy", "dolor"))
    }

    @Test
    fun `a subject and how one feels, with no verb, get the verb they miss`() {
        assertEquals("Yo tengo hambre", say("yo", "hambre"))
        assertEquals("Yo tengo dolor", say("yo", "dolor"))
        assertEquals("Tú tienes sueño", say("tú", "sueño"))
        assertEquals("Ellos tienen frío", say("ellos", "frío"))
        assertEquals("Yo estoy cansada", say("yo", "cansada"))
        assertEquals("Ella está feliz", say("ella", "feliz"))
    }

    @Test
    fun `with no, the missing verb goes after it`() {
        assertEquals("Yo no tengo hambre", say("yo", "no", "hambre"))
        assertEquals("Nosotros no tenemos frío", say("nosotros", "no", "frío"))
        assertEquals("Tú no estás triste", say("tú", "no", "triste"))
    }

    @Test
    fun `nothing is invented where the verb is not certain`() {
        // "quiero", "necesito" or "me gusta"? The person says which.
        assertEquals("Yo agua", say("yo", "agua"))
        assertEquals("Hambre", say("hambre"))
        assertEquals("Agua hambre", say("agua", "hambre"))
    }

    @Test
    fun `a noun one counts takes its article after a verb, one one does not count takes none`() {
        assertEquals("Yo necesito el baño", say("yo", "necesito", "baño"))
        assertEquals("Quiero la pelota", say("quiero", "pelota"))
        assertEquals("Yo quiero agua", say("yo", "quiero", "agua"))
        assertEquals("Necesito ayuda", say("necesito", "ayuda"))
    }

    @Test
    fun `nothing is added where it does not belong`() {
        assertEquals("Yo tengo dolor", say("yo", "tengo", "dolor"))
        assertEquals("Quiero comer", say("quiero", "comer"))
        assertEquals("Estoy cansada", say("estoy", "cansada"))
        assertEquals("Estoy con dolor", say("estoy", "con", "dolor"))
        assertEquals("Tengo que ir", say("tengo", "que", "ir"))
    }

    @Test
    fun `me gusta is said as Spanish says it`() {
        assertEquals("A mí me gusta el agua", say("yo", "me gusta", "agua"))
        assertEquals("A ti te gusta la comida", say("tú", "me gusta", "comida"))
        assertEquals("A ella no le gusta el frío", say("ella", "no", "me gusta", "frío"))
        assertEquals("Me gusta comer", say("me gusta", "comer"))
        // It also agrees with whoever is liked.
        assertEquals("A mí me gustas tú", say("yo", "me gusta", "tú"))
        assertEquals("A ti te gusto yo", say("tú", "me gusta", "yo"))
        assertEquals("Me gustan ellos", say("me gusta", "ellos"))
        // A thing whose gender is not certain keeps no article.
        assertEquals("Me gusta Lucas", say("me gusta", "Lucas"))
    }

    @Test
    fun `me gusta shows on the screen what is said`() {
        val pieces = SpanishGrammar.compose(listOf("yo", "me gusta", "agua"))

        assertEquals(listOf("mí", "me gusta", "agua"), pieces.map { it.text })
        assertEquals(listOf("a", null, "el"), pieces.map { it.connector })
    }

    @Test
    fun `contractions are said as they are in Spanish`() {
        assertEquals("Quiero estar conmigo", say("quiero", "estar", "con", "yo"))
        assertEquals("Quiero agua para ti", say("quiero", "agua", "para", "tú"))
        assertEquals("Necesito ir al baño", say("necesito", "ir", "a", "el", "baño"))
    }

    @Test
    fun `people, actions and feelings are painted`() {
        assertEquals(WordClass.PERSON, SpanishGrammar.wordClassOf("yo"))
        assertEquals(WordClass.ACTION, SpanishGrammar.wordClassOf("me gusta"))
        assertEquals(WordClass.ACTION, SpanishGrammar.wordClassOf("tengo"))
        assertEquals(WordClass.ACTION, SpanishGrammar.wordClassOf("comer"))
        assertEquals(WordClass.FEELING, SpanishGrammar.wordClassOf("hambre"))
        assertEquals(WordClass.SOCIAL, SpanishGrammar.wordClassOf("sí"))
        assertEquals(WordClass.SOCIAL, SpanishGrammar.wordClassOf("gracias"))
        assertEquals(WordClass.FEELING, SpanishGrammar.wordClassOf("cansada"))
        assertEquals(WordClass.OTHER, SpanishGrammar.wordClassOf("agua"))
    }
}
