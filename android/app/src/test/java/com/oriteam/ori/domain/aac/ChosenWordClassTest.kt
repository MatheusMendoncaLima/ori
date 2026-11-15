package com.oriteam.ori.domain.aac

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The wider English and Spanish tables (N7) and the kind of word the person chooses in the symbol form (N8): a name
 * agrees as "he" or "she", a new action takes what an action takes, and how one feels gets the verb it misses.
 */
class ChosenWordClassTest {
    private fun SentenceGrammar.say(vararg words: Pair<String, WordClass?>) =
        spoken(compose(words.map { it.first }, words.map { it.second }))

    private fun SentenceGrammar.say(vararg words: String) = spoken(compose(words.toList()))

    @Test
    fun `the wider English tables`() {
        assertEquals("I love to swim", EnglishGrammar.say("I", "love", "swim"))
        assertEquals("Grandma wants water", EnglishGrammar.say("grandma", "want", "water"))
        assertEquals("I am excited", EnglishGrammar.say("I", "excited"))
        assertEquals("I need the toilet", EnglishGrammar.say("I", "need", "toilet"))
        assertEquals("She feels tired", EnglishGrammar.say("she", "feel", "tired"))
        assertEquals("I don't like to clean", EnglishGrammar.say("I", "not", "like", "clean"))
    }

    @Test
    fun `the wider Spanish tables`() {
        assertEquals("Yo voy a comer", SpanishGrammar.say("yo", "voy", "comer"))
        assertEquals("Yo voy al baño", SpanishGrammar.say("yo", "voy", "baño"))
        assertEquals("Ella puede ir", SpanishGrammar.say("ella", "puedo", "ir"))
        assertEquals("Yo estoy contenta", SpanishGrammar.say("yo", "contenta"))
        assertEquals("Abuela quiere agua", SpanishGrammar.say("abuela", "quiero", "agua"))
    }

    @Test
    fun `a name marked as a person agrees as he or she, in the three languages`() {
        val person = WordClass.PERSON
        assertEquals("Ana quer água", PortugueseGrammar.say("Ana" to person, "quero" to null, "água" to null))
        assertEquals("Ana wants water", EnglishGrammar.say("Ana" to person, "want" to null, "water" to null))
        assertEquals("Ana quiere agua", SpanishGrammar.say("Ana" to person, "quiero" to null, "agua" to null))
        assertEquals("A Ana le gusta el agua", SpanishGrammar.say("Ana" to person, "me gusta" to null, "agua" to null))
        // Without the choice, a name the tables do not know stays as tapped.
        assertEquals("Ana want water", EnglishGrammar.say("Ana", "want", "water"))
    }

    @Test
    fun `a new action and a new feeling read as the known ones do`() {
        assertEquals(
            "I want to skate",
            EnglishGrammar.say("I" to null, "want" to null, "skate" to WordClass.ACTION)
        )
        assertEquals(
            "Yo tengo que patinar",
            SpanishGrammar.say("yo" to null, "tengo" to null, "patinar" to WordClass.ACTION)
        )
        assertEquals("I am grumpy", EnglishGrammar.say("I" to null, "grumpy" to WordClass.FEELING))
        assertEquals("Eu estou animado", PortugueseGrammar.say("eu" to null, "animado" to WordClass.FEELING))
        assertEquals("I want grumpy", EnglishGrammar.say("I", "want", "grumpy"))
    }

    @Test
    fun `the chosen kind travels with the symbol into the message`() {
        val ana = AacSymbol("s1", "b1", "Ana", null, null, 0, "pat", wordClass = WordClass.PERSON)
        val message = AacMessage(grammar = EnglishGrammar)
            .add(ana.toMessagePart())
            .add(MessagePart("want", "want"))
            .add(MessagePart("water", "water"))

        assertEquals("Ana wants water", message.text)
        assertEquals(WordClass.PERSON, wordClassFromKey(WordClass.PERSON.storedKey))
        assertEquals(null, wordClassFromKey("animal"))
    }
}
