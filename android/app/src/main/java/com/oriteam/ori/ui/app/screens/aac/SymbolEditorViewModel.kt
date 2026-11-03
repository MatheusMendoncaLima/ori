package com.oriteam.ori.ui.app.screens.aac

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.AacViewer
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.aac.canUseDevicePhotos
import com.oriteam.ori.domain.aac.nextPosition
import com.oriteam.ori.domain.aac.validateSymbol
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.photos.PhotoImportResult
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.ui.app.screens.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SymbolEditorUiState(
    val labelInput: String = "",
    val phraseInput: String = "",
    val image: SymbolImage? = null,
    val imagePreview: SymbolImageUi? = null,
    val isNew: Boolean = true,
    val canUsePhotos: Boolean = false,
    val isImporting: Boolean = false,
    val isSaving: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

/** Creates or edits one symbol: label, associated phrase and picture. */
@HiltViewModel
class SymbolEditorViewModel @Inject constructor(
    private val boardRepository: AacBoardRepository,
    private val photoStore: PhotoStore,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    sealed interface Event {
        data object Saved : Event
    }

    private val form = SymbolFormState(savedStateHandle)
    private val _uiState = MutableStateFlow(form.restore().withPreview())
    val uiState: StateFlow<SymbolEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var ownerUid: String? = null
    private var boardId: String? = null

    /** Opens the form. A restored draft (rotation or process death) is kept instead of being reset. */
    fun start(
        ownerUid: String,
        viewer: AacViewer,
        boardId: String,
        existing: AacSymbol?,
        initialLabel: String = ""
    ) {
        this.ownerUid = ownerUid
        this.boardId = boardId
        val canUsePhotos = canUseDevicePhotos(viewer)
        if (form.hasDraft) {
            _uiState.update { it.copy(canUsePhotos = canUsePhotos) }
            return
        }
        // Reserved once: saving twice, or after a restart, writes the same symbol.
        form.start(
            symbolId = existing?.id ?: boardRepository.newSymbolId(ownerUid, boardId),
            originalImage = existing?.image
        )
        updateForm {
            SymbolEditorUiState(
                labelInput = existing?.label ?: initialLabel,
                phraseInput = existing?.phrase.orEmpty(),
                image = existing?.image,
                isNew = existing == null,
                canUsePhotos = canUsePhotos
            )
        }
    }

    fun onLabelChange(value: String) = updateForm { it.copy(labelInput = value, errorMessageRes = null) }

    fun onPhraseChange(value: String) = updateForm { it.copy(phraseInput = value, errorMessageRes = null) }

    /** Any emoji works, not only the suggested ones: the person may type one with the keyboard. */
    fun onEmojiChange(value: String) {
        val trimmed = value.trim()
        updateForm { it.copy(image = trimmed.ifEmpty { null }?.let(SymbolImage::Emoji), errorMessageRes = null) }
    }

    fun onRemoveImage() = updateForm { it.copy(image = null) }

    fun onPhotoPicked(sourceUri: String) {
        val state = _uiState.value
        if (!state.canUsePhotos || state.isImporting) return
        _uiState.update { it.copy(isImporting = true, errorMessageRes = null) }
        viewModelScope.launch {
            when (val result = photoStore.importPhoto(sourceUri)) {
                is PhotoImportResult.Failed ->
                    _uiState.update { it.copy(isImporting = false, errorMessageRes = result.error.messageRes()) }
                is PhotoImportResult.Imported -> {
                    val image = SymbolImage.DevicePhoto(result.photo.fileName)
                    form.rememberImported(image)
                    updateForm { it.copy(image = image, isImporting = false) }
                }
            }
        }
    }

    /** Closing without saving removes photos imported in this form, so no file is left behind. */
    fun onClose() {
        val imported = form.importedPhotos()
        viewModelScope.launch { imported.forEach { photoStore.delete(it.photo) } }
        discardDraft()
    }

    fun onSaveClick() {
        val state = _uiState.value
        val target = saveTarget()
        if (target == null || state.isSaving || state.isImporting) return
        val draft = SymbolDraft(
            label = state.labelInput.trim(),
            phrase = state.phraseInput.trim().ifEmpty { null },
            image = state.image
        )
        _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
        viewModelScope.launch { save(target, draft, state.isNew) }
    }

    /** Where the symbol is saved; null until [start] ran. */
    private data class SaveTarget(val ownerUid: String, val boardId: String, val symbolId: String)

    private fun saveTarget(): SaveTarget? {
        val owner = ownerUid
        val board = boardId
        val symbolId = form.symbolId
        return if (owner != null && board != null && symbolId != null) SaveTarget(owner, board, symbolId) else null
    }

    private suspend fun save(target: SaveTarget, draft: SymbolDraft, isNew: Boolean) {
        val (owner, board, symbolId) = target
        val current = boardRepository.getSymbols(owner, board)
        if (current is DataResult.Failure) return showError(current.error.messageRes())
        val symbols = (current as DataResult.Success).value
        val invalid = validateSymbol(draft, symbols, editingId = symbolId.takeUnless { isNew })
        if (invalid != null) return showError(invalid.messageRes())
        if (uploadNewPhoto(owner, draft)) writeSymbol(target, draft, isNew, symbols)
    }

    private suspend fun writeSymbol(target: SaveTarget, draft: SymbolDraft, isNew: Boolean, symbols: List<AacSymbol>) {
        val (owner, board, symbolId) = target
        val write = if (isNew) {
            boardRepository.createSymbol(owner, board, symbolId, draft, nextPosition(symbols.map { it.position }))
        } else {
            boardRepository.updateSymbol(owner, board, symbolId, draft)
        }
        when (write) {
            is DataResult.Failure -> showError(write.error.messageRes())
            is DataResult.Success -> {
                // Photos that are no longer used (replaced or tried and discarded) are removed.
                val unused = (form.importedPhotos() + listOfNotNull(form.originalImage as? SymbolImage.DevicePhoto))
                    .filter { it != draft.image }
                unused.forEach { photoStore.deleteEverywhere(owner, it.photo) }
                discardDraft()
                _events.send(Event.Saved)
            }
        }
    }

    /** Before the symbol, so no symbol ever points to a photo the account refused. */
    private suspend fun uploadNewPhoto(owner: String, draft: SymbolDraft): Boolean {
        val photo = (draft.image as? SymbolImage.DevicePhoto)?.takeIf { it != form.originalImage } ?: return true
        val upload = photoStore.upload(owner, photo.photo)
        if (upload is DataResult.Failure) showError(photoUploadMessage(upload.error))
        return upload !is DataResult.Failure
    }

    @StringRes
    private fun photoUploadMessage(error: DataError) =
        if (error == DataError.PRECONDITION_FAILED) R.string.aac_error_photo_limit else error.messageRes()

    private fun discardDraft() {
        form.clear()
        _uiState.value = SymbolEditorUiState()
    }

    private fun showError(@StringRes messageRes: Int) {
        _uiState.update { it.copy(isSaving = false, errorMessageRes = messageRes) }
    }

    private fun updateForm(transform: (SymbolEditorUiState) -> SymbolEditorUiState) {
        val updated = transform(_uiState.value).withPreview()
        _uiState.value = updated
        form.save(updated)
    }

    private fun SymbolEditorUiState.withPreview() = copy(imagePreview = image?.toUi(photoStore))
}
