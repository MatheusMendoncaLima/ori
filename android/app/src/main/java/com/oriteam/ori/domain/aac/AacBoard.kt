package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.photos.StoredPhoto

/**
 * A communication board (prancha): a named group of symbols, such as "Comida" or "Sentimentos".
 * Boards are how each person organizes their own vocabulary; there is no mandatory set.
 */
data class AacBoard(
    val id: String,
    val name: String,
    /** Place in the list of boards; lower comes first. */
    val position: Int,
    val createdBy: String,
    /**
     * The language the board is written in ("pt-BR"), which the voice and the grammar follow. Null for a board
     * from before boards recorded it, which is in Portuguese (see [contentLanguage]).
     */
    val language: String? = null
)

/**
 * The language of a board or saved phrase that has none. Boards and phrases record it since 08/11/2026, and
 * before that the app was only in Portuguese, so they are Portuguese whatever the app's language is now:
 * following the app's would read a Portuguese board with an English voice.
 */
const val OLD_CONTENT_LANGUAGE = "pt-BR"

/** The language the board is in: its own, or Portuguese for one from before boards recorded it. */
val AacBoard.contentLanguage: String get() = language ?: OLD_CONTENT_LANGUAGE

/**
 * One symbol of a board: what is shown ([label] and [image]) and what is said ([spokenText]).
 * A symbol without image is valid: some people read, and not every word has a picture.
 */
data class AacSymbol(
    val id: String,
    val boardId: String,
    val label: String,
    /** Associated phrase, for example label "Água" says "Eu quero água". Null says the label. */
    val phrase: String?,
    val image: SymbolImage?,
    val position: Int,
    val createdBy: String,
    /** The color the person chose for the tile; null follows the kind of word. */
    val color: SymbolColor? = null,
    /** What the word is, chosen by the person for a word the grammar does not know; null lets the tables say. */
    val wordClass: WordClass? = null
) {
    val spokenText: String get() = phrase ?: label
}

/**
 * Picture of a symbol. Only a short reference is stored in Firestore, never the image bytes.
 */
sealed interface SymbolImage {
    /** A Unicode emoji, drawn by the device font: no image file is shipped or stored. */
    data class Emoji(val value: String) : SymbolImage

    /** A photo reduced and kept by the app (see [com.oriteam.ori.domain.photos.PhotoStore]). */
    data class DevicePhoto(val fileName: String) : SymbolImage {
        /** The same photo, as the photo store knows it. */
        val photo: StoredPhoto get() = StoredPhoto(fileName)
    }
}

/** What the symbol form produces. */
data class SymbolDraft(
    val label: String,
    val phrase: String?,
    val image: SymbolImage?,
    val color: SymbolColor? = null,
    val wordClass: WordClass? = null
)

/**
 * A tile color the person may choose in the symbol form, stored by [key]. The soft colors are the app's own;
 * [NONE] keeps the tile plain even when the word has a kind with a color.
 */
/** How [WordClass] is stored: short lowercase words, checked by the rules. Unknown text reads as no choice. */
val WordClass.storedKey: String get() = name.lowercase()

fun wordClassFromKey(key: String?): WordClass? = WordClass.entries.firstOrNull { it.storedKey == key }

enum class SymbolColor(val key: String) {
    YELLOW("yellow"),
    MINT("mint"),
    BLUE("blue"),
    PINK("pink"),
    ORANGE("orange"),
    LILAC("lilac"),
    GREEN("green"),
    NONE("none");

    companion object {
        /** Null for no choice and for a color this version does not know, which then follows the kind of word. */
        fun fromKey(key: String?): SymbolColor? = entries.firstOrNull { it.key == key }
    }
}

/** Most boards one person can have; also enforced by the rules on list queries. */
const val MAX_AAC_BOARDS = 30

/** Most symbols in one board: a full board fits on a tablet screen and costs a bounded read. */
const val MAX_SYMBOLS_PER_BOARD = 60

const val MAX_BOARD_NAME_LENGTH = 40
const val MAX_SYMBOL_LABEL_LENGTH = 40
const val MAX_SYMBOL_PHRASE_LENGTH = 200

/** Long enough for flags and family emoji (several code units), short enough to be one picture. */
const val MAX_EMOJI_LENGTH = 16

/** Stable order: position first, then name and id, so ties never make items jump around. */
val boardOrder: Comparator<AacBoard> = compareBy<AacBoard> { it.position }.thenBy { it.name }.thenBy { it.id }

val symbolOrder: Comparator<AacSymbol> = compareBy<AacSymbol> { it.position }.thenBy { it.label }.thenBy { it.id }

/** Position for an item added at the end of [positions]. */
fun nextPosition(positions: List<Int>): Int = (positions.maxOrNull() ?: -1) + 1
