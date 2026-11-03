package com.oriteam.ori.domain.aac

/** Most pieces in one message; keeps the message bar readable and the saved text bounded. */
const val MAX_MESSAGE_PARTS = 30

/** Longest piece typed with the keyboard. */
const val MAX_TYPED_TEXT_LENGTH = 100

/** Longest saved message, also enforced by the rules. */
const val MAX_SAVED_MESSAGE_LENGTH = 500

/**
 * One piece of the message being built: a symbol or a word typed by the person. The message
 * keeps a copy of what to show and say, so it does not change if the symbol is edited later.
 */
data class MessagePart(
    val label: String,
    val spokenText: String,
    val image: SymbolImage? = null
)

fun AacSymbol.toMessagePart() = MessagePart(label = label, spokenText = spokenText, image = image)

/** A typed piece; blank text is not a piece. */
fun typedMessagePart(text: String): MessagePart? {
    val trimmed = text.trim().take(MAX_TYPED_TEXT_LENGTH)
    return if (trimmed.isEmpty()) null else MessagePart(label = trimmed, spokenText = trimmed)
}

/**
 * The message being built. Immutable: every change returns a new message, which makes the
 * screen state predictable and easy to save across a restart.
 */
data class AacMessage(val parts: List<MessagePart> = emptyList()) {
    val isEmpty: Boolean get() = parts.isEmpty()
    val isFull: Boolean get() = parts.size >= MAX_MESSAGE_PARTS

    /**
     * What is spoken. A symbol's phrase is a complete sentence, so it is said only when the symbol
     * stands alone ("Dor" says "Estou com dor"). In a built message each piece is a word of a sentence,
     * which [composeSentence] makes read right: "estou" + "Dor" says "Estou com Dor".
     */
    val text: String
        get() = parts.singleOrNull()?.spokenText ?: sentence.toSpokenSentence()

    /** Each piece as it will be said, in the same order as [parts]. */
    val sentence: List<SentencePiece> get() = composeSentence(parts.map { it.label })

    /** A full message ignores new pieces instead of silently dropping old ones. */
    fun add(part: MessagePart): AacMessage = if (isFull) this else copy(parts = parts + part)

    fun removeAt(index: Int): AacMessage =
        if (index in parts.indices) copy(parts = parts.filterIndexed { i, _ -> i != index }) else this

    fun removeLast(): AacMessage = removeAt(parts.lastIndex)

    /** The message can be saved only when its text fits the stored limit. */
    val canBeSaved: Boolean get() = !isEmpty && text.length <= MAX_SAVED_MESSAGE_LENGTH
}
