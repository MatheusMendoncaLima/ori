package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.aac.Person.FIRST
import com.oriteam.ori.domain.aac.Person.FIRST_PLURAL
import com.oriteam.ori.domain.aac.Person.SECOND
import com.oriteam.ori.domain.aac.Person.THIRD
import com.oriteam.ori.domain.aac.Person.THIRD_PLURAL

/** "I want to go", "I need to rest": an action after these verbs takes "to". */
private val toActions = mapOf(WordKind.ACTION to "to")

/** Only "he", "she" and "it" change the verb ("wants"); [base] is every other person's form. */
private fun englishVerb(base: String, third: String) = Verb(
    forms = mapOf(FIRST to base, SECOND to base, THIRD to third, FIRST_PLURAL to base, THIRD_PLURAL to base),
    connectors = toActions
)

/** "I am in pain", but "I am hungry": a state that is a noun takes "in", one that is an adjective takes nothing. */
private val toBe = Verb(
    forms = mapOf(FIRST to "am", SECOND to "are", THIRD to "is", FIRST_PLURAL to "are", THIRD_PLURAL to "are"),
    connectors = mapOf(WordKind.STATE to "in")
)

private val english = GrammarTables(
    verbs = listOf(
        englishVerb("want", "wants"),
        englishVerb("need", "needs"),
        englishVerb("like", "likes"),
        englishVerb("have", "has"),
        englishVerb("love", "loves"),
        englishVerb("hate", "hates"),
        englishVerb("prefer", "prefers"),
        // "I feel tired": how one feels follows with nothing between.
        Verb(
            forms = mapOf(
                FIRST to "feel",
                SECOND to "feel",
                THIRD to "feels",
                FIRST_PLURAL to "feel",
                THIRD_PLURAL to "feel"
            )
        ),
        toBe
    ),
    subjects = mapOf(
        "i" to FIRST,
        "you" to SECOND,
        "he" to THIRD,
        "she" to THIRD,
        "it" to THIRD,
        "he/she" to THIRD,
        "we" to FIRST_PLURAL,
        "they" to THIRD_PLURAL,
        // People named by what they are to the person: "mom wants water".
        "mom" to THIRD,
        "dad" to THIRD,
        "grandma" to THIRD,
        "grandpa" to THIRD,
        "brother" to THIRD,
        "sister" to THIRD,
        "friend" to THIRD,
        "baby" to THIRD
    ),
    linkingWords = setOf(
        "to", "of", "with", "for", "in", "at", "on", "about", "from", "without",
        "a", "an", "the", "and", "or", "not", "don't", "doesn't"
    ),
    negation = "not",
    states = setOf("pain"),
    qualities = setOf(
        "hungry", "thirsty", "sleepy", "tired", "cold", "hot", "sick", "scared", "sad", "happy", "angry", "bored",
        "hurt", "dizzy", "worried", "nervous", "calm", "full", "excited", "lonely", "confused", "frustrated",
        "uncomfortable", "itchy", "sore", "better", "worse", "fine", "okay", "ok", "good", "bad", "ready", "wet",
        "dirty", "done"
    ),
    // "help" is left out on purpose: "I need help" is far more common than "I need to help".
    actions = setOf(
        "go", "come", "eat", "drink", "sleep", "rest", "play", "talk", "walk", "leave", "stop", "see", "watch",
        "hear", "listen", "read", "sit", "stand", "wash", "bathe", "shower", "study", "work", "sing", "dance",
        "draw", "paint", "run", "swim", "cook", "wait", "hug", "open", "close", "clean", "write", "call", "use",
        "finish", "try", "change", "get dressed", "brush teeth", "get up", "lie down", "go out", "jump", "hide",
        "ride", "pee"
    ),
    // "I hungry" says "I am hungry", "I pain" says "I am in pain", and "I not hungry" says "I am not hungry".
    missingVerbs = mapOf(WordKind.STATE to toBe, WordKind.QUALITY to toBe),
    verbBeforeNegation = true,
    // "I need the bathroom"; "I want water" and "I need help" take none.
    articles = setOf(
        "bathroom", "bed", "doctor", "nurse", "teacher", "phone", "car", "ball", "book", "toy", "park", "door",
        "window", "light", "tv", "school", "toilet", "kitchen", "bedroom", "table", "chair", "computer", "tablet",
        "blanket", "pillow", "bus"
    ).associateWith { "the" },
    social = setOf(
        "yes", "no", "not", "maybe", "please", "thanks", "thank you", "hi", "hello", "bye", "sorry", "ok", "okay",
        "excuse me", "you're welcome", "good morning", "good night"
    )
)

/** After a verb or one of these, "I" is "me" and "he" is "him". */
private val prepositions = setOf("to", "of", "with", "for", "at", "about", "from", "without")

private val objectForms =
    mapOf("i" to "me", "he" to "him", "she" to "her", "he/she" to "him/her", "we" to "us", "they" to "them")

/**
 * English: besides the agreement ("he wants") and "to" before an action, two rules English cannot do without.
 * "not" before a verb other than "be" is said with "do" ("I don't want", "he doesn't like"), and a pronoun that
 * is not the subject takes its object form ("you like me"). Both show on the screen, as the agreement does.
 */
object EnglishGrammar : SentenceGrammar {
    override fun compose(words: List<String>, classes: List<WordClass?>): List<SentencePiece> {
        val read = english.read(words, classes)
        return read.mapIndexed { index, (reading, piece) ->
            val previous = read.getOrNull(index - 1)?.first
            val next = read.getOrNull(index + 1)?.first
            when {
                reading.key == "not" && next.takesDo() -> piece.copy(text = doNotFor(next))
                previous?.key == "not" && reading.takesDo() -> piece.copy(text = reading.baseForm ?: piece.text)
                reading.key in objectForms && (reading.isAfterVerb || previous?.key in prepositions) ->
                    piece.copy(text = objectForms.getValue(reading.key))
                reading.key == "i" -> piece.copy(text = "I")
                else -> piece
            }
        }
    }

    override fun spoken(pieces: List<SentencePiece>) = pieces.spoken()

    override fun wordClassOf(word: String) = english.classOf(word)

    /** "be" is denied without "do": "I am not hungry". */
    private fun Reading?.takesDo() = this?.verb != null && verb != toBe

    private fun doNotFor(verb: Reading?) = if (verb?.person == THIRD) "doesn't" else "don't"

    /** After "doesn't" the verb goes back to its plain form: "he doesn't want". */
    private val Reading.baseForm get() = verb?.formFor(FIRST)
}
