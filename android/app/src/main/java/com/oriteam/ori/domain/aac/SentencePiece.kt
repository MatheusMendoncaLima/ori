package com.oriteam.ori.domain.aac

/**
 * One tapped word as it will be said: its [text], possibly agreeing with the subject ("você" +
 * "quero" says "quer"), and a [connector] the grammar needs before it ("gosto" + "você" needs "de").
 */
data class SentencePiece(val text: String, val connector: String? = null)

private enum class Person { FIRST, THIRD, PLURAL }

private enum class Complement { NONE, OF, OF_UNLESS_VERB, WITH_FOR_STATES }

private class Verb(val first: String, val third: String, val plural: String, val complement: Complement) {
    fun formFor(person: Person) = when (person) {
        Person.FIRST -> first
        Person.THIRD -> third
        Person.PLURAL -> plural
    }

    val forms = setOf(first, third, plural)
}

private val verbs = listOf(
    Verb("quero", "quer", "queremos", Complement.NONE),
    Verb("preciso", "precisa", "precisamos", Complement.OF_UNLESS_VERB),
    Verb("gosto", "gosta", "gostamos", Complement.OF),
    Verb("estou", "está", "estamos", Complement.WITH_FOR_STATES),
    Verb("tenho", "tem", "temos", Complement.NONE)
)

private val subjects = mapOf(
    "eu" to Person.FIRST,
    "você" to Person.THIRD,
    "ele" to Person.THIRD,
    "ela" to Person.THIRD,
    "ele/ela" to Person.THIRD,
    "a gente" to Person.THIRD,
    "nós" to Person.PLURAL
)

/** What one is "com": "estou com dor", but "estou feliz" stays as it is. */
private val states = setOf("dor", "sono", "fome", "frio", "calor", "sede", "medo", "febre", "pressa", "raiva")

/** A word that already links two others, or that does not take a complement. */
private val linkingWords = setOf("de", "do", "da", "com", "para", "a", "ao", "à", "em", "no", "na", "que", "não", "e")

private val infinitiveEnding = Regex("(ar|er|ir)$")

/** Ways of saying two neighboring words that Portuguese always joins or changes. */
private val contractions = mapOf(
    "de ele" to "dele",
    "de ela" to "dela",
    "de eu" to "de mim",
    "com eu" to "comigo",
    "com nós" to "conosco"
)

private fun String.key() = trim().lowercase()

private fun verbOf(key: String) = verbs.firstOrNull { key in it.forms }

/**
 * Turns tapped words into a sentence that reads right, so a person who cannot build the grammar is
 * still understood: "eu" + "gosto" + "você" says "eu gosto de você". It only adds what is certain:
 * words it does not know stay as tapped, and the order is never changed. Only the verbs and states
 * of a first vocabulary are known; a table grows more safely than rules that try to guess.
 */
fun composeSentence(words: List<String>): List<SentencePiece> {
    var person: Person? = null
    var previousVerb: Verb? = null
    return words.map { word ->
        val key = word.key()
        val verb = verbOf(key)
        val connector = previousVerb?.connectorBefore(key)
        if (previousVerb == null && verb == null) subjects[key]?.let { person = it }
        val text = verb?.takeIf { person != null }?.formFor(person!!) ?: word
        previousVerb = verb ?: previousVerb?.takeIf { key == "não" }
        SentencePiece(text = text, connector = connector)
    }
}

private fun Verb.connectorBefore(key: String): String? {
    if (key in linkingWords || verbOf(key) != null) return null
    return when (complement) {
        Complement.NONE -> null
        Complement.OF -> "de"
        Complement.OF_UNLESS_VERB -> "de".takeUnless { infinitiveEnding.containsMatchIn(key) }
        Complement.WITH_FOR_STATES -> "com".takeIf { key in states }
    }
}

/** The kind of a word, for the colors of a board. Only words the grammar knows have one. */
enum class WordClass { PERSON, ACTION, FEELING, OTHER }

/**
 * Like the color keys used in communication boards (people, actions, feelings), but only for the words
 * of the tables above: a guess, such as "ends in -ar", would paint "lugar" as an action.
 */
fun wordClassOf(word: String): WordClass {
    val key = word.key()
    return when {
        key in subjects -> WordClass.PERSON
        verbOf(key) != null -> WordClass.ACTION
        key in states -> WordClass.FEELING
        else -> WordClass.OTHER
    }
}

/** The sentence to say, with the contractions the words on the screen do not show. */
fun List<SentencePiece>.toSpokenSentence(): String {
    val words = flatMap { listOfNotNull(it.connector, it.text) }
    val joined = mutableListOf<String>()
    words.forEach { word ->
        val contraction = joined.lastOrNull()?.let { contractions["${it.key()} ${word.key()}"] }
        if (contraction != null) joined[joined.lastIndex] = contraction else joined += word
    }
    return joined.joinToString(" ").replaceFirstChar { it.uppercase() }
}
