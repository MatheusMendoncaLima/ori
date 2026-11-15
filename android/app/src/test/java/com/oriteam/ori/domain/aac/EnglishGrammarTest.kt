package com.oriteam.ori.domain.aac

import org.junit.Assert.assertEquals
import org.junit.Test

class EnglishGrammarTest {
    private fun say(vararg words: String) = EnglishGrammar.spoken(EnglishGrammar.compose(words.toList()))

    @Test
    fun `the verb agrees with whoever comes before it`() {
        assertEquals("He wants water", say("he", "want", "water"))
        assertEquals("You are sleepy", say("you", "am", "sleepy"))
        assertEquals("She has food", say("she", "have", "food"))
        assertEquals("We need help", say("we", "need", "help"))
        assertEquals("I want water", say("I", "wants", "water"))
    }

    @Test
    fun `an action takes to, a state that is a noun takes in`() {
        assertEquals("I want to go", say("I", "want", "go"))
        assertEquals("I need to rest", say("I", "need", "rest"))
        assertEquals("I have to eat", say("I", "have", "eat"))
        assertEquals("I am in pain", say("I", "am", "pain"))
    }

    @Test
    fun `a subject and how one feels, with no verb, get the verb they miss`() {
        assertEquals("I am hungry", say("I", "hungry"))
        assertEquals("I am in pain", say("I", "pain"))
        assertEquals("You are cold", say("you", "cold"))
        assertEquals("He is sleepy", say("he", "sleepy"))
        assertEquals("They are in pain", say("they", "pain"))
        assertEquals("We are tired", say("we", "tired"))
    }

    @Test
    fun `with not, the missing verb goes before it`() {
        assertEquals("I am not hungry", say("I", "not", "hungry"))
        assertEquals("She is not in pain", say("she", "not", "pain"))
        val pieces = EnglishGrammar.compose(listOf("I", "not", "pain"))
        assertEquals(listOf(null, "am", "in"), pieces.map { it.connector })
    }

    @Test
    fun `nothing is invented where the verb is not certain`() {
        // "want", "need" or "like"? The person says which.
        assertEquals("I water", say("I", "water"))
        assertEquals("Hungry", say("hungry"))
        assertEquals("Water hungry", say("water", "hungry"))
    }

    @Test
    fun `a noun one counts takes the after a verb, one one does not count takes none`() {
        assertEquals("I need the bathroom", say("I", "need", "bathroom"))
        assertEquals("I don't want the ball", say("I", "not", "want", "ball"))
        assertEquals("I want water", say("I", "want", "water"))
        assertEquals("I need help", say("I", "need", "help"))
        assertEquals("I need the bathroom", say("I", "need", "the", "bathroom"))
    }

    @Test
    fun `nothing is added where it does not belong`() {
        assertEquals("I am hungry", say("I", "am", "hungry"))
        assertEquals("I need help", say("I", "need", "help"))
        assertEquals("I like water", say("I", "like", "water"))
        assertEquals("I want to go", say("I", "want", "to", "go"))
    }

    @Test
    fun `not is said with do, and the verb goes back to its plain form`() {
        assertEquals("I don't want water", say("I", "not", "want", "water"))
        assertEquals("He doesn't like food", say("he", "not", "like", "food"))
        assertEquals("I am not hungry", say("I", "am", "not", "hungry"))
        assertEquals("I am not in pain", say("I", "am", "not", "pain"))
    }

    @Test
    fun `a pronoun that is not the subject takes its object form`() {
        assertEquals("You like me", say("you", "like", "I"))
        assertEquals("I like him", say("I", "like", "he"))
        assertEquals("With us", say("with", "we"))
        // Before the verb, it is the subject and stays as it is.
        assertEquals("He wants me", say("he", "want", "I"))
    }

    @Test
    fun `I is always written in capitals`() {
        assertEquals(listOf("I", "want"), EnglishGrammar.compose(listOf("i", "want")).map { it.text })
    }

    @Test
    fun `each tapped word keeps its own piece, with what it needs before it`() {
        val pieces = EnglishGrammar.compose(listOf("I", "am", "pain"))

        assertEquals(listOf("I", "am", "pain"), pieces.map { it.text })
        assertEquals(listOf(null, null, "in"), pieces.map { it.connector })
    }

    @Test
    fun `people, actions and feelings are painted`() {
        assertEquals(WordClass.PERSON, EnglishGrammar.wordClassOf("I"))
        assertEquals(WordClass.ACTION, EnglishGrammar.wordClassOf("want"))
        assertEquals(WordClass.ACTION, EnglishGrammar.wordClassOf("go"))
        assertEquals(WordClass.FEELING, EnglishGrammar.wordClassOf("pain"))
        assertEquals(WordClass.FEELING, EnglishGrammar.wordClassOf("hungry"))
        assertEquals(WordClass.SOCIAL, EnglishGrammar.wordClassOf("not"))
        assertEquals(WordClass.SOCIAL, EnglishGrammar.wordClassOf("yes"))
        assertEquals(WordClass.OTHER, EnglishGrammar.wordClassOf("water"))
    }
}
