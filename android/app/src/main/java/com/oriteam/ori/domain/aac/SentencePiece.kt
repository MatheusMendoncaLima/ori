package com.oriteam.ori.domain.aac

/**
 * One tapped word as it will be said: its [text], possibly agreeing with the subject ("você" +
 * "quero" says "quer"), and a [connector] the grammar needs before it ("gosto" + "você" needs "de").
 */
data class SentencePiece(val text: String, val connector: String? = null)

/**
 * The kind of a word, for the colors of a board: people, actions, feelings, and answers and courtesy ("sim", "não",
 * "por favor"). Only words the grammar knows have one.
 */
enum class WordClass { PERSON, ACTION, FEELING, SOCIAL, OTHER }

/** Who a verb agrees with. Each language says which word is which: "você" is THIRD, "you" and "tú" are SECOND. */
internal enum class Person { FIRST, SECOND, THIRD, FIRST_PLURAL, THIRD_PLURAL }

/** What a word is to the verb before it, which decides what the verb asks for ("de", "com", "to"…). */
internal enum class WordKind { THING, ACTION, STATE, QUALITY }

/** A verb of a table: its form for each person, and the word it asks for before each kind of word. */
internal class Verb(
    private val forms: Map<Person, String>,
    private val connectors: Map<WordKind, String> = emptyMap()
) {
    val allForms: Set<String> = forms.values.toSet()

    fun formFor(person: Person): String? = forms[person]

    fun connectorBefore(kind: WordKind): String? = connectors[kind]
}

/**
 * The tables of one language. They hold only what is certain: a word outside them stays as tapped, and the
 * order never changes. A table grows more safely than rules that try to guess ("ends in -ar" would make "lugar"
 * a verb). [states] are nouns of how one feels ("dor", "pain"); [qualities], adjectives of it ("hungry");
 * [actions], verbs in the infinitive ("go", "comer"). All three are painted on the boards.
 *
 * [missingVerbs] is the verb a state or a quality takes when the person tapped none: "eu" + "fome" says
 * "eu estou com fome", "I" + "hungry" says "I am hungry". It goes after the negation ("eu não estou com fome"),
 * or before it when [verbBeforeNegation] ("I am not hungry").
 *
 * [articles] are the nouns that take one after a verb, with it: "I need the bathroom", "necesito el baño". Only
 * nouns one counts; "I want water" takes none. [social] are answers and courtesy ("sim", "não", "por favor"), which
 * only get their color.
 */
internal open class GrammarTables(
    val verbs: List<Verb>,
    val subjects: Map<String, Person>,
    val linkingWords: Set<String>,
    val negation: String,
    val states: Set<String> = emptySet(),
    val qualities: Set<String> = emptySet(),
    val actions: Set<String> = emptySet(),
    val missingVerbs: Map<WordKind, Verb> = emptyMap(),
    val verbBeforeNegation: Boolean = false,
    val articles: Map<String, String> = emptyMap(),
    val social: Set<String> = emptySet()
) {
    /** Whether a word is an action to the verb before it. Portuguese also knows its infinitives by their ending. */
    open fun isAction(key: String) = key in actions

    fun verbOf(key: String) = verbs.firstOrNull { key in it.allForms }

    /** The kind of a word to the verb before it; null for a word that takes nothing ("de", "não", a verb). */
    fun kindOf(key: String): WordKind? = when {
        key in linkingWords || verbOf(key) != null -> null
        isAction(key) -> WordKind.ACTION
        key in states -> WordKind.STATE
        key in qualities -> WordKind.QUALITY
        else -> WordKind.THING
    }

    /**
     * The same, when the person said what the word is ([WordClass] chosen in the symbol form): an action, how one
     * feels (an adjective, unless the tables know it as a noun: "dor"), or a person or thing.
     */
    fun kindOf(key: String, chosen: WordClass?): WordKind? = when (chosen) {
        WordClass.ACTION -> WordKind.ACTION
        WordClass.FEELING -> if (key in states) WordKind.STATE else WordKind.QUALITY
        WordClass.PERSON, WordClass.OTHER -> kindOf(key)?.let { WordKind.THING }
        WordClass.SOCIAL, null -> kindOf(key)
    }

    fun classOf(word: String): WordClass {
        val key = word.grammarKey()
        return when {
            key in subjects -> WordClass.PERSON
            verbOf(key) != null || key in actions -> WordClass.ACTION
            key in states || key in qualities -> WordClass.FEELING
            key in social -> WordClass.SOCIAL
            else -> WordClass.OTHER
        }
    }
}

/**
 * One tapped word as the tables read it, for the rules of a single language that come after [read]. [isPerson]: a
 * subject of the tables ("eu", "mom") or a word the person marked as a person (a name).
 */
internal data class Reading(
    val key: String,
    val verb: Verb?,
    val person: Person?,
    val isAfterVerb: Boolean,
    val isPerson: Boolean = false
)

/**
 * Turns tapped words into pieces that read right, so a person who cannot build the grammar is still understood:
 * the verb agrees with whoever comes before it ("você" + "quero" says "quer") and brings the word it asks for
 * ("gosto" + "você" says "gosto de você"). A pronoun after a verb is not its subject, and a negation between the
 * verb and the next word keeps them connected ("não gosto de banho"). A subject followed by how one feels, with
 * no verb, gets the verb it is missing ("eu fome" says "eu estou com fome").
 */
internal fun GrammarTables.read(
    words: List<String>,
    classes: List<WordClass?> = emptyList()
): List<Pair<Reading, SentencePiece>> {
    val read = mutableListOf<Pair<Reading, SentencePiece>>()
    var person: Person? = null
    var previousVerb: Verb? = null
    words.forEachIndexed { index, word ->
        val key = word.grammarKey()
        val chosen = classes.getOrNull(index)
        val verb = verbOf(key)
        val kind = kindOf(key, chosen)
        // A name the person marked as a person agrees as "he" or "she" does: "Ana quer água".
        val subject = subjects[key] ?: Person.THIRD.takeIf { chosen == WordClass.PERSON }
        val seenVerb = read.any { it.first.verb != null }
        val connector = if (previousVerb != null) {
            val article = articles[key]?.takeIf { kind == WordKind.THING }
            listOfNotNull(kind?.let(previousVerb::connectorBefore), article).joinToString(" ").ifEmpty { null }
        } else {
            kind?.takeIf { verb == null && !seenVerb }?.let { missingVerbBefore(it, person, read) }
        }
        if (previousVerb == null && verb == null) subject?.let { person = it }
        val text = verb?.let { person?.let(it::formFor) } ?: word
        val reading = Reading(key, verb, person, isAfterVerb = seenVerb, isPerson = subject != null)
        read += reading to SentencePiece(text = text, connector = connector)
        previousVerb = verb ?: previousVerb?.takeIf { key == negation }
    }
    return read
}

/**
 * The verb [kind] takes when the subject came right before it, alone or with the negation: what goes before the
 * word, such as "estou com" in "eu estou com fome". Where the verb goes before the negation ("I am not hungry"),
 * the negation's piece receives it, and the word keeps only what the verb asks for ("in" in "I am not in pain").
 */
private fun GrammarTables.missingVerbBefore(
    kind: WordKind,
    person: Person?,
    read: MutableList<Pair<Reading, SentencePiece>>
): String? {
    val verb = missingVerbs[kind]
    val last = read.lastOrNull()?.first
    val isNegated = last?.key == negation
    val subject = if (isNegated) read.getOrNull(read.lastIndex - 1)?.first else last
    val asked = verb?.connectorBefore(kind)
    val form = person?.let { verb?.formFor(it) }?.takeIf { subject != null && subject.isPerson }
    return form?.let {
        if (isNegated && verbBeforeNegation) {
            val (negationReading, negationPiece) = read.last()
            read[read.lastIndex] = negationReading to negationPiece.copy(connector = form)
            asked
        } else {
            listOfNotNull(form, asked).joinToString(" ")
        }
    }
}

/** The sentence to say, with the [contractions] of two neighboring words that the screen does not show. */
internal fun List<SentencePiece>.spoken(contractions: Map<String, String> = emptyMap()): String {
    val joined = mutableListOf<String>()
    // A connector may hold two words ("a el" in "voy a el baño"); each one meets its neighbor ("al").
    flatMap { it.connector?.split(" ").orEmpty() + it.text }.forEach { word ->
        val contraction = joined.lastOrNull()?.let { contractions["${it.grammarKey()} ${word.grammarKey()}"] }
        if (contraction != null) joined[joined.lastIndex] = contraction else joined += word
    }
    return joined.joinToString(" ").replaceFirstChar { it.uppercase() }
}

/** Old boards have "Dor" and "Eu": the tables compare words in lowercase, without spaces around them. */
internal fun String.grammarKey() = trim().lowercase()
