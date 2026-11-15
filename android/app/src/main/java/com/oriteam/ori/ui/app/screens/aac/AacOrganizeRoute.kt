package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import com.oriteam.ori.ui.adaptive.AreaListDetailLayout
import com.oriteam.ori.ui.components.motion.OriPaneTransition
import com.oriteam.ori.ui.components.state.EmptyState

/** The symbol form being shown, encoded as text so it survives process death. */
private sealed interface EditorPane {
    val key: String

    data object New : EditorPane {
        override val key = "new"
    }

    data class Edit(val symbolId: String) : EditorPane {
        override val key = "edit:$symbolId"
    }

    companion object {
        fun parse(key: String?): EditorPane? = when {
            key == null -> null
            key == New.key -> New
            else -> Edit(key.removePrefix("edit:"))
        }
    }
}

/**
 * Organizes the boards of [ownerUid] as seen by [viewer]: boards (list), symbols of the chosen
 * board (detail) and the symbol form. One or two panes depending on the width this area gets.
 */
@Composable
fun AacOrganizeRoute(
    ownerUid: String,
    viewer: AacViewer,
    title: String,
    onNavigateBack: (() -> Unit)?,
    viewModel: AacOrganizeViewModel = hiltViewModel(key = "aac-organize-$ownerUid")
) {
    LaunchedEffect(ownerUid, viewer) { viewModel.show(ownerUid, viewer) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editorKey by rememberSaveable(ownerUid) { mutableStateOf<String?>(null) }
    val editor = EditorPane.parse(editorKey)
    val boardId = state.selectedBoardId

    val boardsPane: @Composable (Modifier, (() -> Unit)?) -> Unit = { paneModifier, onBack ->
        BoardsPane(
            state = state,
            title = title,
            onNavigateBack = onBack,
            onOpenBoard = {
                editorKey = null
                viewModel.onOpenBoard(it)
            },
            onMoveBoard = viewModel::onMoveBoard,
            onCreateBoard = viewModel::onCreateBoardRequest,
            onRetry = viewModel::onRetry,
            modifier = paneModifier
        )
    }
    val detailPane: @Composable (String, (() -> Unit)?) -> Unit = { selectedBoard, onBack ->
        // The symbol form opens over the symbols of the board, as a step forward.
        OriPaneTransition(target = editor, isForward = { _, to -> to != null }, label = "SymbolForm") { shown ->
            if (shown != null) {
                SymbolEditorRoute(
                    editorKey = "symbol-$ownerUid-$selectedBoard-${shown.key}",
                    ownerUid = ownerUid,
                    viewer = viewer,
                    boardId = selectedBoard,
                    existing = (shown as? EditorPane.Edit)?.let { viewModel.symbol(it.symbolId) },
                    onDelete = (shown as? EditorPane.Edit)?.let { { viewModel.onDeleteSymbolRequest(it.symbolId) } },
                    onDone = { editorKey = null }
                )
            } else {
                SymbolsPane(
                    state = state,
                    onNavigateBack = onBack,
                    onEditSymbol = { editorKey = EditorPane.Edit(it).key },
                    onAddSymbol = { editorKey = EditorPane.New.key },
                    onMoveSymbol = viewModel::onMoveSymbol,
                    onRenameBoard = { viewModel.onRenameBoardRequest(selectedBoard) },
                    onDeleteBoard = { viewModel.onDeleteBoardRequest(selectedBoard) },
                    onRetry = viewModel::onRetry,
                    onAddSuggestedWords = viewModel::onAddSuggestedWords
                )
            }
        }
    }

    AreaListDetailLayout(
        selection = boardId,
        // An open symbol form closes first, then the board.
        onBack = { if (editorKey != null) editorKey = null else viewModel.onOpenBoard(null) },
        list = { listModifier -> boardsPane(listModifier, onNavigateBack) },
        detail = { board, onBack -> detailPane(board, onBack) },
        emptyDetail = {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = stringResource(R.string.aac_choose_board_title),
                message = stringResource(R.string.aac_choose_board_message)
            )
        }
    )

    OrganizerDialogs(state = state, viewModel = viewModel, onSymbolDeleted = { editorKey = null })
}
