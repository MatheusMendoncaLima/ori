package com.oriteam.ori.ui.app.screens.aac

import androidx.annotation.StringRes
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.BoardValidationError
import com.oriteam.ori.domain.aac.SymbolValidationError

/** Text shown for each validation problem. Exhaustive: a new error does not compile without one. */
@StringRes
fun SymbolValidationError.messageRes(): Int = when (this) {
    SymbolValidationError.MISSING_LABEL -> R.string.aac_error_missing_label
    SymbolValidationError.LABEL_TOO_LONG -> R.string.aac_error_label_too_long
    SymbolValidationError.PHRASE_TOO_LONG -> R.string.aac_error_phrase_too_long
    SymbolValidationError.INVALID_EMOJI -> R.string.aac_error_invalid_emoji
    SymbolValidationError.DUPLICATE_LABEL -> R.string.aac_error_duplicate_label
    SymbolValidationError.BOARD_FULL -> R.string.aac_error_board_full
}

@StringRes
fun BoardValidationError.messageRes(): Int = when (this) {
    BoardValidationError.MISSING_NAME -> R.string.aac_error_missing_board_name
    BoardValidationError.NAME_TOO_LONG -> R.string.aac_error_board_name_too_long
    BoardValidationError.DUPLICATE_NAME -> R.string.aac_error_duplicate_board
    BoardValidationError.LIMIT_REACHED -> R.string.aac_error_board_limit
}
