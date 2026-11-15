package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.AacViewer
import com.oriteam.ori.ui.components.navigation.SubscreenHost
import com.oriteam.ori.ui.components.navigation.rememberOpenSubscreen
import com.oriteam.ori.ui.components.state.KeptUnder

/** The areas of the primary user's communication module. */
/** What opens over communicating, which is where the area always comes back to. */
private enum class AacArea { ORGANIZE, PREFERENCES, ADD_WORD }

/**
 * Communication module of the primary user [ownerUid]. Communicating is the default area;
 * organizing and preferences are one tap away and "back" always returns to communicating.
 */
@Composable
fun AacRoute(ownerUid: String, viewModel: AacCommunicateViewModel = hiltViewModel(key = "aac-$ownerUid")) {
    LaunchedEffect(ownerUid) { viewModel.show(ownerUid) }
    val area = rememberOpenSubscreen<AacArea>(ownerUid)
    val backToCommunicate = area::close
    var newWord by rememberSaveable(ownerUid) { mutableStateOf("") }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VoiceVolumeKeys(state.preferences.voice.sound)
    val openBoardId = state.buildBoard?.id
    val addWord = { word: String ->
        newWord = word
        area.open(AacArea.ADD_WORD)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Communicating stays made under organizing, preferences and a new word: back only shows it again (made again,
        // "Montar" took 95 to 120 ms on the test tablet).
        KeptUnder(isShown = area.current == null, modifier = Modifier.fillMaxSize()) {
            val starterBoard = rememberStarterBoard()
            AacCommunicateScreen(
                state = state,
                actions = AacCommunicateActions(
                    message = AacMessageBarActions(
                        onRemovePart = viewModel::onRemovePart,
                        onRemoveLast = viewModel::onRemoveLast,
                        onClear = viewModel::onClear,
                        onSpeak = viewModel::onSpeak,
                        onStop = viewModel::onStopSpeaking,
                        onShow = viewModel::onShowMessage,
                        onSave = viewModel::onSaveMessage,
                        onTyped = viewModel::onTypedText,
                        onOrganize = { area.open(AacArea.ORGANIZE) },
                        onPreferences = { area.open(AacArea.PREFERENCES) },
                        onSaveWord = addWord.takeIf { openBoardId != null }
                    ),
                    onSelect = viewModel::onSelect,
                    onShowBoards = viewModel::onShowBoards,
                    onSymbolTap = viewModel::onSymbolTap,
                    onAddSymbol = { addWord("") },
                    onSayPhrase = viewModel::onSayPhrase,
                    onDeleteSavedRequest = viewModel::onDeleteSavedRequest,
                    onCreateStarterBoard = { viewModel.onCreateStarterBoard(starterBoard) },
                    onRetry = viewModel::onRetry,
                    onDismissDisplay = viewModel::onDismissDisplay,
                    onDeleteSavedConfirm = viewModel::onDeleteSavedConfirm,
                    onDeleteSavedDismiss = viewModel::onDeleteSavedDismiss,
                    onChangeEmojiRequest = viewModel::onChangeEmojiRequest,
                    onChangeEmojiConfirm = viewModel::onChangeEmojiConfirm,
                    onChangeEmojiDismiss = viewModel::onChangeEmojiDismiss,
                    onMoveSymbol = viewModel::onMoveSymbol,
                    onMoveSaved = viewModel::onMoveSaved,
                    onMoveReady = viewModel::onMoveReady,
                    onRaiseVolume = viewModel::onRaiseVolume,
                    onDismissVolumeWarning = viewModel::onDismissVolumeWarning
                )
            )
        }
        SubscreenHost(area) { current ->
            when (current) {
                null -> Unit
                AacArea.ORGANIZE -> {
                    AacOrganizeRoute(
                        ownerUid = ownerUid,
                        viewer = AacViewer.Owner,
                        title = stringResource(R.string.aac_organize),
                        onNavigateBack = backToCommunicate
                    )
                }
                AacArea.PREFERENCES -> AacPreferencesRoute(onNavigateBack = backToCommunicate)
                AacArea.ADD_WORD -> {
                    // The board stays open behind this form; if it is deleted meanwhile, there is nothing to add to.
                    openBoardId?.let { boardId ->
                        SymbolEditorRoute(
                            editorKey = "add-word-$ownerUid-$boardId",
                            ownerUid = ownerUid,
                            viewer = AacViewer.Owner,
                            boardId = boardId,
                            existing = null,
                            onDelete = null,
                            onDone = backToCommunicate,
                            initialLabel = newWord
                        )
                    }
                }
            }
        }
    }
}
