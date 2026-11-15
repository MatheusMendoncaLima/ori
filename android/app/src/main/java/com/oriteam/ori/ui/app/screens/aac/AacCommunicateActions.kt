package com.oriteam.ori.ui.app.screens.aac

import com.oriteam.ori.domain.aac.SavedMessage

/** Everything the communication screen can ask its owner to do. */
data class AacCommunicateActions(
    val message: AacMessageBarActions,
    val onSelect: (AacSelection) -> Unit,
    val onShowBoards: () -> Unit,
    val onSymbolTap: (String) -> Unit,
    val onAddSymbol: () -> Unit = {},
    val onSayPhrase: (text: String, language: String?) -> Unit,
    val onDeleteSavedRequest: (SavedMessage) -> Unit,
    val onCreateStarterBoard: () -> Unit,
    val onRetry: () -> Unit,
    val onDismissDisplay: () -> Unit,
    val onDeleteSavedConfirm: () -> Unit,
    val onDeleteSavedDismiss: () -> Unit,
    val onChangeEmojiRequest: (SavedMessage) -> Unit = {},
    val onChangeEmojiConfirm: (String?) -> Unit = {},
    val onChangeEmojiDismiss: () -> Unit = {},
    val onMoveSymbol: (symbolId: String, toIndex: Int) -> Unit = { _, _ -> },
    val onMoveSaved: (messageId: String, toIndex: Int) -> Unit = { _, _ -> },
    val onMoveReady: (phraseId: String, toIndex: Int) -> Unit = { _, _ -> },
    val onRaiseVolume: () -> Unit = {},
    val onDismissVolumeWarning: () -> Unit = {}
)
