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
    val createdBy: String
)

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
    val createdBy: String
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
    val image: SymbolImage?
)

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
