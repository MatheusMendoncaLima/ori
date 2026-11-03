package com.oriteam.ori.ui.app.screens.aac

import com.oriteam.ori.domain.aac.SavedMessage

/** Everything the communication screen can ask its owner to do. */
data class AacCommunicateActions(
    val message: AacMessageBarActions,
    val onSelect: (AacSelection) -> Unit,
    val onShowBoards: () -> Unit,
    val onSymbolTap: (String) -> Unit,
    val onAddSymbol: () -> Unit = {},
    val onSayPhrase: (String) -> Unit,
    val onDeleteSavedRequest: (SavedMessage) -> Unit,
    val onCreateStarterBoard: () -> Unit,
    val onRetry: () -> Unit,
    val onDismissDisplay: () -> Unit,
    val onDeleteSavedConfirm: () -> Unit,
    val onDeleteSavedDismiss: () -> Unit
)
