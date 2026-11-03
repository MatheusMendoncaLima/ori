package com.oriteam.ori.domain.aac

import java.util.Locale

enum class SymbolValidationError {
    MISSING_LABEL,
    LABEL_TOO_LONG,
    PHRASE_TOO_LONG,
    INVALID_EMOJI,
    DUPLICATE_LABEL,
    BOARD_FULL
}

enum class BoardValidationError {
    MISSING_NAME,
    NAME_TOO_LONG,
    DUPLICATE_NAME,
    LIMIT_REACHED
}

/** Compares names as a person would: ignoring case and extra spaces. */
internal fun normalizedName(value: String): String =
    value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

/**
 * Checks a symbol before saving. [boardSymbols] are the symbols already in the board;
 * [editingId] is the symbol being edited, which is not compared with itself.
 */
fun validateSymbol(draft: SymbolDraft, boardSymbols: List<AacSymbol>, editingId: String?): SymbolValidationError? {
    val label = draft.label.trim()
    val others = boardSymbols.filter { it.id != editingId }
    val emoji = (draft.image as? SymbolImage.Emoji)?.value
    return when {
        label.isEmpty() -> SymbolValidationError.MISSING_LABEL
        label.length > MAX_SYMBOL_LABEL_LENGTH -> SymbolValidationError.LABEL_TOO_LONG
        (draft.phrase?.length ?: 0) > MAX_SYMBOL_PHRASE_LENGTH -> SymbolValidationError.PHRASE_TOO_LONG
        emoji != null && (emoji.isBlank() || emoji.length > MAX_EMOJI_LENGTH) -> SymbolValidationError.INVALID_EMOJI
        others.any { normalizedName(it.label) == normalizedName(label) } -> SymbolValidationError.DUPLICATE_LABEL
        editingId == null && others.size >= MAX_SYMBOLS_PER_BOARD -> SymbolValidationError.BOARD_FULL
        else -> null
    }
}

/** Checks a board name. [boards] are the person's boards; [editingId] is the one being renamed. */
fun validateBoardName(name: String, boards: List<AacBoard>, editingId: String?): BoardValidationError? {
    val trimmed = name.trim()
    val others = boards.filter { it.id != editingId }
    return when {
        trimmed.isEmpty() -> BoardValidationError.MISSING_NAME
        trimmed.length > MAX_BOARD_NAME_LENGTH -> BoardValidationError.NAME_TOO_LONG
        others.any { normalizedName(it.name) == normalizedName(trimmed) } -> BoardValidationError.DUPLICATE_NAME
        editingId == null && others.size >= MAX_AAC_BOARDS -> BoardValidationError.LIMIT_REACHED
        else -> null
    }
}
