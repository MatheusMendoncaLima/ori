package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.grammarFor
import com.oriteam.ori.domain.common.appLanguageTag
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.navigation.OriFilterTab
import com.oriteam.ori.ui.components.navigation.OriFilterTabs
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.StatefulContent
import com.oriteam.ori.ui.components.state.outOfSightUnless
import com.oriteam.ori.ui.components.state.rememberScreenRested
import com.oriteam.ori.ui.components.text.TextCaseToggle
import com.oriteam.ori.ui.theme.OriSpacing

private enum class AacTab { PHRASES, BUILD }

/**
 * The primary user's communication screen, with two ways of saying something: "Prontas", what it
 * opens with, says a whole sentence with one tap; "Montar" builds one from words. Keeping them apart is
 * what prevents a sentence from being added inside another one. The tabs replace the title, so the
 * symbols get the height; organizing and preferences are in "Mais". Without any board it still
 * works: the person can type and speak, or use the phrases.
 */
@Composable
fun AacCommunicateScreen(state: AacCommunicateUiState, actions: AacCommunicateActions) {
    val padding = LocalWindowLayout.current.horizontalPadding
    val tab = if (state.selection == AacSelection.SavedMessages) AacTab.PHRASES else AacTab.BUILD
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = padding)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = OriSpacing.Small)
        ) {
            OriFilterTabs(
                tabs = listOf(
                    OriFilterTab(AacTab.PHRASES, stringResource(R.string.aac_tab_ready)),
                    OriFilterTab(AacTab.BUILD, stringResource(R.string.aac_tab_build))
                ),
                selected = tab,
                onSelect = {
                    when (it) {
                        AacTab.BUILD -> actions.onShowBoards()
                        AacTab.PHRASES -> actions.onSelect(AacSelection.SavedMessages)
                    }
                },
                modifier = Modifier.weight(1f)
            )
            TextCaseToggle()
        }
        FormMessage(messageRes = state.actionErrorRes, type = FormMessageType.ERROR)
        FormMessage(messageRes = state.noticeRes, type = FormMessageType.SUCCESS)
        if (state.isVolumeOff) {
            VolumeOffWarning(onRaise = actions.onRaiseVolume, onDismiss = actions.onDismissVolumeWarning)
        }
        AacTabs(tab, state, actions, Modifier.weight(1f))
    }

    state.displayedText?.let { MessageDisplayDialog(text = it, onDismiss = actions.onDismissDisplay) }
    state.pendingEmojiChange?.let { saved ->
        SavedEmojiDialog(
            message = saved,
            onConfirm = actions.onChangeEmojiConfirm,
            onDismiss = actions.onChangeEmojiDismiss
        )
    }
    state.pendingDeletion?.let { saved ->
        OriConfirmationDialog(
            title = stringResource(R.string.aac_delete_saved_title),
            message = saved.text,
            confirmLabel = stringResource(R.string.aac_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = actions.onDeleteSavedConfirm,
            onDismiss = actions.onDeleteSavedDismiss,
            isDestructive = true
        )
    }
}

/**
 * The two ways of saying something. Each one, once made, stays made, out of sight while the other is open: going back
 * only shows it again ("Montar" was made again at every visit, 66 to 144 ms on the test tablet: M16). "Montar" is
 * also made ahead, when the screen has rested, so even the first visit only shows it.
 */
@Composable
private fun AacTabs(tab: AacTab, state: AacCommunicateUiState, actions: AacCommunicateActions, modifier: Modifier) {
    val isBuildMade = rememberScreenRested()
    val hasShownPhrases = remember { BooleanArray(1) }
    if (tab == AacTab.PHRASES) hasShownPhrases[0] = true
    // A field of the tab left behind lets go of the keyboard, as when the tab was not kept.
    val focus = LocalFocusManager.current
    val lastTab = remember { arrayOf(tab) }
    LaunchedEffect(tab) {
        if (lastTab[0] != tab) focus.clearFocus()
        lastTab[0] = tab
    }
    Box(modifier = modifier) {
        if (tab == AacTab.PHRASES || hasShownPhrases[0]) {
            PhrasesPane(state, actions, Modifier.fillMaxSize().outOfSightUnless(tab == AacTab.PHRASES))
        }
        if (tab == AacTab.BUILD || isBuildMade) {
            BuildPane(state, actions, Modifier.fillMaxSize().outOfSightUnless(tab == AacTab.BUILD))
        }
    }
}

@Composable
private fun PhrasesPane(state: AacCommunicateUiState, actions: AacCommunicateActions, modifier: Modifier) {
    AacPhrasesPane(
        saved = state.savedMessages,
        speechState = state.speechState,
        onSay = actions.onSayPhrase,
        onStop = actions.message.onStop,
        onDeleteSaved = actions.onDeleteSavedRequest,
        onChangeEmoji = actions.onChangeEmojiRequest,
        onRetry = actions.onRetry,
        modifier = modifier,
        ready = readyPhrasesIn(state.preferences.readyPhraseOrder),
        onMoveSaved = actions.onMoveSaved.takeIf { state.preferences.dragToReorder },
        onMoveReady = actions.onMoveReady.takeIf { state.preferences.dragToReorder }
    )
}

@Composable
private fun BuildPane(state: AacCommunicateUiState, actions: AacCommunicateActions, modifier: Modifier) {
    var isChoosingBoard by rememberSaveable { mutableStateOf(false) }
    val boards = (state.boards as? ContentState.Content)?.value.orEmpty()
    val messageActions = actions.message.copy(onChangeBoard = { isChoosingBoard = true }.takeIf { boards.isNotEmpty() })
    Column(modifier = modifier) {
        BoardSelector(boards = boards, selection = state.buildBoard, onSelect = actions.onSelect)
        AacBoardLayout(
            messageBar = {
                AacMessageBar(
                    message = state.message,
                    speechState = state.speechState,
                    isFull = state.isMessageFull,
                    actions = messageActions,
                    modifier = it
                )
            },
            content = { LowerContent(state, actions, it) },
            actions = {
                AacMessageActions(
                    hasMessage = state.message.isNotEmpty(),
                    speechState = state.speechState,
                    canSave = state.canSaveMessage,
                    actions = messageActions
                )
            }
        )
    }
    if (isChoosingBoard && boards.isNotEmpty()) {
        BoardChoiceDialog(
            boards = boards,
            selectedId = state.buildBoard?.id,
            onChoose = { id ->
                isChoosingBoard = false
                actions.onSelect(AacSelection.Board(id))
            },
            onOrganize = {
                isChoosingBoard = false
                actions.message.onOrganize()
            },
            onDismiss = { isChoosingBoard = false }
        )
    }
}

@Composable
private fun LowerContent(state: AacCommunicateUiState, actions: AacCommunicateActions, modifier: Modifier) {
    when (state.buildBoard) {
        is AacSelection.Board -> StatefulContent(
            state = state.symbols,
            onRetry = actions.onRetry,
            emptyContent = {
                EmptyState(
                    icon = OriIcons.Chat,
                    title = stringResource(R.string.aac_board_empty_title),
                    message = stringResource(R.string.aac_board_empty_message),
                    actionLabel = stringResource(R.string.aac_organize),
                    onAction = actions.message.onOrganize
                )
            },
            modifier = modifier
        ) { symbols ->
            AacBoardGrid(
                symbols = symbols,
                listKey = state.symbolsKey,
                preferences = state.preferences,
                grammar = grammarFor(state.boardLanguage ?: appLanguageTag),
                onSymbolClick = { actions.onSymbolTap(it.id) },
                onAddSymbol = actions.onAddSymbol,
                onMoveSymbol = actions.onMoveSymbol.takeIf { state.preferences.dragToReorder },
                onClickLabel = stringResource(
                    if (state.preferences.messageMode == MessageMode.SPEAK_IMMEDIATELY) {
                        R.string.aac_tap_to_speak
                    } else {
                        R.string.aac_tap_to_add
                    }
                )
            )
        }
        // Without a board, only while there are none (or they are being read).
        null -> StatefulContent(
            state = state.boards,
            onRetry = actions.onRetry,
            emptyContent = {
                NoBoardsState(
                    isCreating = state.isCreatingStarterBoard,
                    onCreateStarterBoard = actions.onCreateStarterBoard,
                    onCreateOwn = actions.message.onOrganize
                )
            },
            modifier = modifier
        ) { /* Boards exist: one is selected automatically, so this is a brief transition. */ }
    }
}
