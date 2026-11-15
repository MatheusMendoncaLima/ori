package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.AacViewer
import com.oriteam.ori.ui.components.form.rememberFormOpening

/**
 * Form to create ([existing] null) or edit a symbol. [editorKey] gives each form its own
 * ViewModel and saved draft.
 */
@Composable
fun SymbolEditorRoute(
    editorKey: String,
    ownerUid: String,
    viewer: AacViewer,
    boardId: String,
    existing: AacSymbol?,
    onDelete: (() -> Unit)?,
    onDone: () -> Unit,
    initialLabel: String = "",
    viewModel: SymbolEditorViewModel = hiltViewModel(key = editorKey)
) {
    val opening = rememberFormOpening()
    LaunchedEffect(editorKey, opening) { viewModel.start(opening, ownerUid, viewer, boardId, existing, initialLabel) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SymbolEditorViewModel.Event.Saved -> onDone()
            }
        }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SymbolEditorScreen(
        state = state,
        actions = SymbolEditorActions(
            onLabelChange = viewModel::onLabelChange,
            onPhraseChange = viewModel::onPhraseChange,
            onEmojiChange = viewModel::onEmojiChange,
            onPhotoPicked = viewModel::onPhotoPicked,
            onRemoveImage = viewModel::onRemoveImage,
            onColorChange = viewModel::onColorChange,
            onWordClassChange = viewModel::onWordClassChange,
            onSave = viewModel::onSaveClick,
            onDelete = onDelete,
            onClose = {
                viewModel.onClose()
                onDone()
            }
        )
    )
}
