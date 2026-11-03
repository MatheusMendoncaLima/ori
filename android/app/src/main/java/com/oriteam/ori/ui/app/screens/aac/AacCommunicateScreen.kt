package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.navigation.OriFilterTab
import com.oriteam.ori.ui.components.navigation.OriFilterTabs
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.StatefulContent
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
        when (tab) {
            AacTab.PHRASES -> AacPhrasesPane(
                saved = state.savedMessages,
                speechState = state.speechState,
                onSay = actions.onSayPhrase,
                onStop = actions.message.onStop,
                onDeleteSaved = actions.onDeleteSavedRequest,
                onRetry = actions.onRetry,
                modifier = Modifier.weight(1f)
            )
            AacTab.BUILD -> BuildPane(state, actions, Modifier.weight(1f))
        }
    }

    state.displayedText?.let { MessageDisplayDialog(text = it, onDismiss = actions.onDismissDisplay) }
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

@Composable
private fun BuildPane(state: AacCommunicateUiState, actions: AacCommunicateActions, modifier: Modifier) {
    Column(modifier = modifier) {
        val boards = (state.boards as? ContentState.Content)?.value.orEmpty()
        BoardSelector(boards = boards, selection = state.selection, onSelect = actions.onSelect)
        AacBoardLayout(
            messageBar = {
                AacMessageBar(
                    message = state.message,
                    speechState = state.speechState,
                    isFull = state.isMessageFull,
                    actions = actions.message,
                    modifier = it
                )
            },
            content = { LowerContent(state, actions, it) },
            actions = {
                AacMessageActions(
                    hasMessage = state.message.isNotEmpty(),
                    speechState = state.speechState,
                    canSave = state.canSaveMessage,
                    actions = actions.message
                )
            }
        )
    }
}

@Composable
private fun LowerContent(state: AacCommunicateUiState, actions: AacCommunicateActions, modifier: Modifier) {
    when (state.selection) {
        is AacSelection.Board -> StatefulContent(
            state = state.symbols,
            onRetry = actions.onRetry,
            emptyContent = {
                EmptyState(
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
                preferences = state.preferences,
                onSymbolClick = { actions.onSymbolTap(it.id) },
                onAddSymbol = actions.onAddSymbol,
                onClickLabel = stringResource(
                    if (state.preferences.messageMode == MessageMode.SPEAK_IMMEDIATELY) {
                        R.string.aac_tap_to_speak
                    } else {
                        R.string.aac_tap_to_add
                    }
                )
            )
        }
        // Saved phrases are another tab; here nothing is selected only while there are no boards.
        AacSelection.SavedMessages, null -> StatefulContent(
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
