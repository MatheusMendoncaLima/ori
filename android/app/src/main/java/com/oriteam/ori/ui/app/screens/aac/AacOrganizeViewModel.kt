package com.oriteam.ori.ui.app.screens.aac

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.AacViewer
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.aac.boardOrder
import com.oriteam.ori.domain.aac.canManageBoards
import com.oriteam.ori.domain.aac.canUseDevicePhotos
import com.oriteam.ori.domain.aac.moved
import com.oriteam.ori.domain.aac.nextPosition
import com.oriteam.ori.domain.aac.positionChanges
import com.oriteam.ori.domain.aac.symbolOrder
import com.oriteam.ori.domain.aac.validateBoardName
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.domain.photos.StoredPhoto
import com.oriteam.ori.ui.app.screens.common.messageRes
import com.oriteam.ori.ui.app.screens.common.withLastingSyncNotice
import com.oriteam.ori.ui.components.state.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val BOARD_KEY = "organizer_board"

/**
 * Organizes someone's boards: create, rename, reorder and delete boards and symbols. Used by
 * the primary user and by a caregiver with the "manage communication" permission.
 */
// One public function per user event of the organizer; they all act on the same state.
@Suppress("TooManyFunctions")
@HiltViewModel
class AacOrganizeViewModel @Inject constructor(
    private val boardRepository: AacBoardRepository,
    private val photoStore: PhotoStore,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private data class Request(val ownerUid: String, val viewer: AacViewer, val attempt: Int = 0)

    private val request = MutableStateFlow<Request?>(null)
    private val selectedBoardId = MutableStateFlow(savedStateHandle.get<String>(BOARD_KEY))
    private val interaction = MutableStateFlow(AacOrganizeUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val boards: StateFlow<Listed<AacBoard, BoardTabUiModel>> = request.filterNotNull()
        .flatMapLatest { current ->
            boardRepository.observeBoards(current.ownerUid)
                .map { result -> listed(result, boardOrder) { BoardTabUiModel(it.id, it.name) } }
                .onStart { emit(Listed()) }
                .withLastingSyncNotice(notice = { it.noticeRes }, withoutNotice = { it.copy(noticeRes = null) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Listed())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val symbols: StateFlow<Listed<AacSymbol, AacSymbolUiModel>> =
        combine(request.filterNotNull(), selectedBoardId, ::Pair).flatMapLatest { (current, id) ->
            if (id == null) {
                flowOf(Listed(rows = ContentState.Empty))
            } else {
                boardRepository.observeSymbols(current.ownerUid, id)
                    .transformLatest { result ->
                        emit(listed(result, symbolOrder) { it.toUiModel(photoStore) })
                        // Photos added on another device arrive after the names, which never wait for them.
                        val photos = (result as? DataResult.Success)?.value?.items?.let(::photosOf).orEmpty()
                        if (photoStore.fetchMissing(current.ownerUid, photos)) {
                            emit(listed(result, symbolOrder) { it.toUiModel(photoStore) })
                        }
                    }
                    .onStart { emit(Listed()) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Listed())

    private val latestBoards: List<AacBoard> get() = boards.value.items
    private val latestSymbols: List<AacSymbol> get() = symbols.value.items

    val uiState: StateFlow<AacOrganizeUiState> =
        combine(boards, symbols, selectedBoardId, interaction) { boardList, symbolList, boardId, local ->
            local.copy(
                boards = boardList.rows,
                symbols = symbolList.rows,
                selectedBoardId = boardId,
                syncNoticeRes = boardList.noticeRes
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AacOrganizeUiState())

    fun show(ownerUid: String, viewer: AacViewer) {
        val current = request.value
        if (current?.ownerUid == ownerUid && current.viewer == viewer) return
        interaction.update { it.copy(canManage = canManageBoards(viewer), canUsePhotos = canUseDevicePhotos(viewer)) }
        request.value = Request(ownerUid, viewer)
    }

    fun onRetry() = request.update { it?.copy(attempt = it.attempt + 1) }

    fun onOpenBoard(boardId: String?) {
        savedStateHandle[BOARD_KEY] = boardId
        selectedBoardId.value = boardId
    }

    /** The full symbol, for opening the editor. */
    fun symbol(symbolId: String): AacSymbol? = latestSymbols.firstOrNull { it.id == symbolId }

    fun onMoveBoard(boardId: String, offset: Int) {
        val current = managingRequest() ?: return
        val changes = reorder(latestBoards.map { it.id to it.position }, boardId, offset) ?: return
        runWrite { boardRepository.reorderBoards(current.ownerUid, changes) }
    }

    fun onMoveSymbol(symbolId: String, offset: Int) {
        val current = managingRequest() ?: return
        val boardId = selectedBoardId.value ?: return
        val changes = reorder(latestSymbols.map { it.id to it.position }, symbolId, offset) ?: return
        runWrite { boardRepository.reorderSymbols(current.ownerUid, boardId, changes) }
    }

    fun onCreateBoardRequest() = interaction.update { it.copy(boardDialog = BoardNameDialog(boardId = null)) }

    fun onRenameBoardRequest(boardId: String) {
        val board = latestBoards.firstOrNull { it.id == boardId } ?: return
        interaction.update { it.copy(boardDialog = BoardNameDialog(boardId = boardId, input = board.name)) }
    }

    fun onBoardNameChange(value: String) =
        interaction.update { it.copy(boardDialog = it.boardDialog?.copy(input = value, errorRes = null)) }

    fun onBoardDialogDismiss() = interaction.update { it.copy(boardDialog = null) }

    fun onBoardDialogConfirm() {
        val current = managingRequest() ?: return
        val dialog = interaction.value.boardDialog ?: return
        if (dialog.isSaving) return
        val name = dialog.input.trim()
        validateBoardName(name, latestBoards, dialog.boardId)?.let { error ->
            return interaction.update { it.copy(boardDialog = dialog.copy(errorRes = error.messageRes())) }
        }
        interaction.update { it.copy(boardDialog = dialog.copy(isSaving = true)) }
        viewModelScope.launch {
            val result = if (dialog.boardId == null) {
                boardRepository.createBoard(current.ownerUid, name, nextPosition(latestBoards.map { it.position }))
            } else {
                boardRepository.renameBoard(current.ownerUid, dialog.boardId, name)
            }
            interaction.update { state ->
                when (result) {
                    is DataResult.Failure ->
                        state.copy(boardDialog = dialog.copy(isSaving = false, errorRes = result.error.messageRes()))
                    is DataResult.Success -> state.copy(boardDialog = null)
                }
            }
        }
    }

    fun onDeleteBoardRequest(boardId: String) {
        val board = latestBoards.firstOrNull { it.id == boardId } ?: return
        interaction.update { it.copy(pendingDeletion = PendingAacDeletion.Board(board.id, board.name)) }
    }

    fun onDeleteSymbolRequest(symbolId: String) {
        val symbol = symbol(symbolId) ?: return
        interaction.update { it.copy(pendingDeletion = PendingAacDeletion.Symbol(symbol.id, symbol.label)) }
    }

    fun onDeleteDismiss() = interaction.update { it.copy(pendingDeletion = null) }

    fun onDeleteConfirm() {
        val current = managingRequest() ?: return
        val pending = interaction.value.pendingDeletion ?: return
        interaction.update { it.copy(pendingDeletion = null) }
        when (pending) {
            is PendingAacDeletion.Board -> deleteBoard(current, pending.id)
            is PendingAacDeletion.Symbol -> {
                val boardId = selectedBoardId.value ?: return
                val photos = photosOf(latestSymbols.filter { it.id == pending.id })
                runWrite(onSuccess = { removeLocalPhotos(current, photos) }) {
                    boardRepository.deleteSymbol(current.ownerUid, boardId, pending.id)
                }
            }
        }
    }

    /** Photos of the deleted symbols are removed from this device only after the delete was accepted. */
    private fun deleteBoard(current: Request, boardId: String) {
        runWrite {
            val symbolsResult = boardRepository.getSymbols(current.ownerUid, boardId)
            val photos = (symbolsResult as? DataResult.Success)?.value?.let(::photosOf).orEmpty()
            boardRepository.deleteBoard(current.ownerUid, boardId).also { result ->
                if (result is DataResult.Success) {
                    removeLocalPhotos(current, photos)
                    if (selectedBoardId.value == boardId) onOpenBoard(null)
                }
            }
        }
    }

    private suspend fun removeLocalPhotos(current: Request, photos: List<StoredPhoto>) {
        if (canUseDevicePhotos(current.viewer)) photos.forEach { photoStore.deleteEverywhere(current.ownerUid, it) }
    }

    private fun photosOf(symbols: List<AacSymbol>) =
        symbols.mapNotNull { (it.image as? SymbolImage.DevicePhoto)?.photo }

    /** Actions are offered only with permission; this also ignores stale taps after a revocation. */
    private fun managingRequest(): Request? = request.value?.takeIf { canManageBoards(it.viewer) }

    private fun reorder(items: List<Pair<String, Int>>, id: String, offset: Int): Map<String, Int>? {
        val ids = items.map { it.first }
        val from = ids.indexOf(id)
        val newOrder = ids.moved(from, from + offset)
        return positionChanges(items.toMap(), newOrder).takeIf { it.isNotEmpty() }
    }

    private fun runWrite(
        onSuccess: suspend () -> Unit = {},
        write: suspend () -> DataResult<WriteStatus>
    ) {
        interaction.update { it.copy(actionErrorRes = null) }
        viewModelScope.launch {
            when (val result = write()) {
                is DataResult.Failure -> interaction.update { it.copy(actionErrorRes = result.error.messageRes()) }
                is DataResult.Success -> onSuccess()
            }
        }
    }
}
