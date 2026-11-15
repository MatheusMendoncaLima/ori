package com.oriteam.ori.domain.aac

import java.util.Locale

/**
 * The suggested board in every language of the app: its name and its words by a key ("i", "am"…), each with
 * its picture. It is the app's own vocabulary, like the ready phrases, so it follows the app's language instead
 * of keeping the one it was created in: put in English, the app shows "Basics" with "I", "am", "hungry", and
 * builds and speaks them in English.
 *
 * Only what the app made follows it. A word of the suggested board is the app's while it is untouched: the same
 * word, the same picture and no phrase of its own. What the person wrote or changed is theirs and stays as
 * written, and so does a board renamed. A board with no word of the app keeps its own language (see
 * [AacBoard.language]).
 */
class StarterVocabulary(
    /** The name of the suggested board, by language ("pt", "en", "es"). */
    private val names: Map<String, String>,
    /** Each word of the suggested board by its key, by language. */
    private val words: Map<String, Map<String, String>>,
    /** The picture of each word by its key; null for a word with none ("com"). */
    private val pictures: Map<String, String?>
) {
    /** [board] as shown in [language]: the suggested board, while it has the name the app gave it, is named in it. */
    fun boardIn(board: AacBoard, language: String): AacBoard {
        val from = board.contentLanguage.primary()
        val name = names[language.primary()]?.takeIf { from != language.primary() && names[from] == board.name }
        return name?.let { board.copy(name = it) } ?: board
    }

    /**
     * The symbols of [board] as shown in [language], and the language the board then speaks: [language] when some
     * word of the app is now in it, so the grammar and the voice match the words on the screen; otherwise the
     * board's own.
     */
    fun symbolsIn(board: AacBoard, symbols: List<AacSymbol>, language: String): ShownSymbols {
        val from = board.contentLanguage.primary()
        val target = words[language.primary()]?.takeIf { language.primary() != from }
        val shown = symbols.map { symbol ->
            target?.let { symbol.keyIn(from)?.let(it::get) }?.let { symbol.copy(label = it) } ?: symbol
        }
        val followsApp = target != null && symbols.any { it.keyIn(from) != null }
        return ShownSymbols(shown, if (followsApp) language else board.contentLanguage)
    }

    /**
     * The words of the suggested board that [board] lacks, in the board's own language and as the app makes them
     * (word, picture, no phrase), in the order of the suggested board. Only for a board that already has words of the
     * app, such as a suggested board made when it had fewer words; empty for any other. [symbols] are the board's as
     * stored, not as shown in another language.
     */
    fun missingFrom(board: AacBoard, symbols: List<AacSymbol>): List<SymbolDraft> {
        val from = board.contentLanguage.primary()
        val labels = symbols.map { it.label.grammarKey() }.toSet()
        val own = words[from].orEmpty().takeIf { symbols.any { it.keyIn(from) != null } }.orEmpty()
        return own.filterValues { it.grammarKey() !in labels }.map { (key, word) ->
            SymbolDraft(label = word, phrase = null, image = pictures[key]?.let(SymbolImage::Emoji))
        }
    }

    /** The key of an untouched word of the app in [language]: same word, same picture, no phrase of its own. */
    private fun AacSymbol.keyIn(language: String): String? = words[language]?.entries?.firstOrNull { (key, word) ->
        word == label && phrase == null && image == pictures[key]?.let(SymbolImage::Emoji)
    }?.key
}

/** The symbols of a board as the screen shows them, and the language its messages are built and said in. */
data class ShownSymbols(val symbols: List<AacSymbol>, val language: String)

/** "pt-BR" and "pt" are the same words: the vocabulary is by language, not by region. */
private fun String.primary(): String = Locale.forLanguageTag(this).language
