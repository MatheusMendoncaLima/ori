package com.oriteam.ori.ui.app.screens.aac

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.ui.components.state.ContentState

/** One board in the selector. */
@Immutable
data class BoardTabUiModel(val id: String, val name: String)

/**
 * What the lower part of the screen shows: the symbols of a board or the saved messages.
 * Encoded as text so it survives process death.
 */
sealed interface AacSelection {
    val key: String

    data class Board(val id: String) : AacSelection {
        override val key = "board:$id"
    }

    data object SavedMessages : AacSelection {
        override val key = "saved"
    }

    companion object {
        fun parse(key: String?): AacSelection? = when {
            key == null -> null
            key == SavedMessages.key -> SavedMessages
            else -> Board(key.removePrefix("board:"))
        }
    }
}

@Immutable
data class AacCommunicateUiState(
    val boards: ContentState<List<BoardTabUiModel>> = ContentState.Loading,
    val selection: AacSelection? = null,
    /** The board "Montar" shows, also while "Prontas" is open (the last one used); null without boards. */
    val buildBoard: AacSelection.Board? = null,
    val symbols: ContentState<List<AacSymbolUiModel>> = ContentState.Loading,
    /** The board [symbols] belong to. */
    val symbolsKey: String? = null,
    /** The language of that board (a language tag), which colors its words; null is the app's. */
    val boardLanguage: String? = null,
    val savedMessages: ContentState<List<SavedMessage>> = ContentState.Loading,
    val message: List<AacSymbolUiModel> = emptyList(),
    val canSaveMessage: Boolean = false,
    val isMessageFull: Boolean = false,
    val preferences: AacPreferences = AacPreferences(),
    val speechState: SpeechState = SpeechState.PREPARING,
    /** Text shown in large letters (on request, or because there is no voice). */
    val displayedText: String? = null,
    /** The last text was said with the volume at zero: nobody heard it, and the screen warns. */
    val isVolumeOff: Boolean = false,
    val isCreatingStarterBoard: Boolean = false,
    val pendingDeletion: SavedMessage? = null,
    /** The saved phrase whose picture is being chosen. */
    val pendingEmojiChange: SavedMessage? = null,
    @StringRes val actionErrorRes: Int? = null,
    @StringRes val noticeRes: Int? = null
)

/**
 * The selected board, or the first one when nothing was chosen or the chosen board was deleted
 * (also by a caregiver on another device). Saved messages stay selected even without boards.
 */
internal fun resolveSelection(boards: ContentState<List<AacBoard>>, key: String?): AacSelection? {
    val requested = AacSelection.parse(key)
    val list = when (boards) {
        is ContentState.Content -> boards.value
        ContentState.Empty -> emptyList()
        // Still loading or failed: keep the choice until the boards are known.
        ContentState.Loading, is ContentState.Error -> null
    }
    return when {
        requested == AacSelection.SavedMessages || list == null -> requested
        list.any { AacSelection.Board(it.id) == requested } -> requested
        else -> list.firstOrNull()?.let { AacSelection.Board(it.id) }
    }
}
