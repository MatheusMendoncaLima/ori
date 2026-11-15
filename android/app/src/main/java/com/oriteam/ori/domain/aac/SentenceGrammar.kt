package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.common.appLocaleFor
import java.util.Locale

/**
 * How tapped words become a sentence in one language: what is added between them, how it is said, and the
 * kind of each word (for the colors of a board). Each language has its own tables ([PortugueseGrammar],
 * [EnglishGrammar], [SpanishGrammar]); a language without them uses [PlainGrammar], which adds nothing: wrong
 * grammar would be worse than none.
 */
interface SentenceGrammar {
    /**
     * [classes] are the kinds the person chose for some words, in the same order as [words] (null: not chosen).
     * A chosen kind wins over the tables, so a word the tables do not know (a name, a new verb) still reads right.
     */
    fun compose(words: List<String>, classes: List<WordClass?> = emptyList()): List<SentencePiece>

    fun spoken(pieces: List<SentencePiece>): String

    fun wordClassOf(word: String): WordClass
}

/** A language without tables: the words stay as they were tapped, and no kind of word is guessed. */
object PlainGrammar : SentenceGrammar {
    override fun compose(words: List<String>, classes: List<WordClass?>) = words.map { SentencePiece(it) }

    override fun spoken(pieces: List<SentencePiece>) = pieces.spoken()

    override fun wordClassOf(word: String) = WordClass.OTHER
}

/** One per language with texts in the app (`TranslatedLanguages`), so each board is built in its language. */
private val grammars: Map<String, SentenceGrammar> = mapOf(
    "pt" to PortugueseGrammar,
    "en" to EnglishGrammar,
    "es" to SpanishGrammar
)

/** The grammar of a language ("pt", "pt-BR", "en-US", "es"…); one without tables gets [PlainGrammar]. */
fun grammarFor(language: String): SentenceGrammar =
    grammars[Locale.forLanguageTag(language).language] ?: PlainGrammar

/**
 * The grammar of the app's language: the default of a message until it is read in the language of its board
 * ([AacMessage.inLanguage]), and of a board from before boards recorded their language.
 */
val appGrammar: SentenceGrammar
    get() = grammarFor(appLocaleFor(listOf(Locale.getDefault())).language)
