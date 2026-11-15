package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.common.TranslatedLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

/** Portuguese, and what every language shares. English and Spanish have their own tests. */
class SentenceGrammarTest {
    private fun say(vararg words: String) = PortugueseGrammar.spoken(PortugueseGrammar.compose(words.toList()))

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
        assertEquals("Eles querem água", say("eles", "quero", "água"))
        assertEquals("Eu não quero água", say("eu", "não", "quero", "água"))
    }

    @Test
    fun `a subject and how one feels, with no verb, get the verb they miss`() {
        assertEquals("Eu estou com fome", say("eu", "fome"))
        assertEquals("Você está com sono", say("você", "sono"))
        assertEquals("Nós estamos com frio", say("nós", "frio"))
        assertEquals("Ela está cansada", say("ela", "cansada"))
        assertEquals("Eu estou bem", say("eu", "bem"))
        assertEquals("Eu não estou com dor", say("eu", "não", "dor"))
    }

    @Test
    fun `nothing is invented where the verb is not certain`() {
        // "quero", "preciso" or "gosto"? The person says which.
        assertEquals("Eu água", say("eu", "água"))
        assertEquals("Fome", say("fome"))
        assertEquals("Água fome", say("água", "fome"))
        assertEquals("Eu quero comer", say("eu", "quero", "comer"))
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
        assertEquals("Eu gosto delas", say("eu", "gosto", "elas"))
    }

    @Test
    fun `case on old boards does not change the result`() {
        assertEquals("Estou com Dor", say("estou", "Dor"))
        assertEquals("Eu gosto de Você", say("Eu", "Gosto", "Você").replace("Eu Gosto", "Eu gosto"))
    }

    @Test
    fun `each tapped word keeps its own piece, so it can be removed`() {
        val pieces = PortugueseGrammar.compose(listOf("eu", "gosto", "você"))

        assertEquals(listOf("eu", "gosto", "você"), pieces.map { it.text })
        assertEquals(listOf(null, null, "de"), pieces.map { it.connector })
    }

    @Test
    fun `only words of the tables are painted`() {
        assertEquals(WordClass.PERSON, PortugueseGrammar.wordClassOf("Eu"))
        assertEquals(WordClass.ACTION, PortugueseGrammar.wordClassOf("quero"))
        assertEquals(WordClass.FEELING, PortugueseGrammar.wordClassOf("dor"))
        assertEquals(WordClass.SOCIAL, PortugueseGrammar.wordClassOf("não"))
        assertEquals(WordClass.SOCIAL, PortugueseGrammar.wordClassOf("por favor"))
        assertEquals(WordClass.ACTION, PortugueseGrammar.wordClassOf("comer"))
        // An infinitive leaves "de" out, but is not painted: "lugar" ends like one.
        assertEquals(WordClass.OTHER, PortugueseGrammar.wordClassOf("lugar"))
    }

    @Test
    fun `a symbol alone still says its own phrase`() {
        val message = AacMessage().add(MessagePart(label = "Dor", spokenText = "Estou com dor"))

        assertEquals("Estou com dor", message.text)
    }

    @Test
    fun `every language of the app has its grammar, chosen by the language of a tag`() {
        TranslatedLanguages.forEach { assertNotSame(it, PlainGrammar, grammarFor(it)) }
        assertSame(PortugueseGrammar, grammarFor("pt-BR"))
        assertSame(EnglishGrammar, grammarFor("en-US"))
        assertSame(SpanishGrammar, grammarFor("es-419"))
    }

    @Test
    fun `a language without tables says the words as tapped and guesses no kind of word`() {
        val french = grammarFor("fr")
        val message = AacMessage(grammar = french).add(word("je")).add(word("veux")).add(word("eau"))

        assertSame(PlainGrammar, french)
        assertEquals("Je veux eau", message.text)
        assertEquals(WordClass.OTHER, french.wordClassOf("je"))
    }

    private fun word(text: String) = MessagePart(label = text, spokenText = text)
}
