package com.oriteam.ori.ui.app.screens.aac

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.AacViewer
import com.oriteam.ori.domain.aac.MAX_SYMBOLS_PER_BOARD
import com.oriteam.ori.domain.aac.StarterVocabulary
import com.oriteam.ori.domain.aac.SymbolDraft
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
import com.oriteam.ori.domain.common.ListSnapshot
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.common.appLanguageTag
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
import kotlinx.coroutines.flow.distinctUntilChanged
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

/** The symbols of the open board as shown, and the words of the suggested board it lacks (see [StarterVocabulary]). */
private data class BoardSymbols(
    val listed: Listed<AacSymbol, AacSymbolUiModel> = Listed(),
    val missing: List<SymbolDraft> = emptyList()
)
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
    private val savedStateHandle: SavedStateHandle,
    private val starterVocabulary: StarterVocabulary
) : ViewModel() {

    private data class Request(val ownerUid: String, val viewer: AacViewer, val attempt: Int = 0)

    private val request = MutableStateFlow<Request?>(null)

    /** Read again each time the screen opens: the suggested board is shown in the app's language. */
    private val appLanguage = MutableStateFlow(appLanguageTag)
    private val selectedBoardId = MutableStateFlow(savedStateHandle.get<String>(BOARD_KEY))
    private val interaction = MutableStateFlow(AacOrganizeUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val boards: StateFlow<Listed<AacBoard, BoardTabUiModel>> = request.filterNotNull()
        .flatMapLatest { current ->
            combine(boardRepository.observeBoards(current.ownerUid), appLanguage, ::boardsIn)
                .onStart { emit(Listed()) }
                .withLastingSyncNotice(notice = { it.noticeRes }, withoutNotice = { it.copy(noticeRes = null) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Listed())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val symbols: StateFlow<BoardSymbols> =
        combine(request.filterNotNull(), selectedBoardId, ::Pair).flatMapLatest { (current, id) ->
            if (id == null) {
                flowOf(BoardSymbols(Listed(rows = ContentState.Empty)))
            } else {
                val read = boardRepository.observeSymbols(current.ownerUid, id).transformLatest { result ->
                    emit(result)
                    // Photos added on another device arrive after the names, which never wait for them.
                    val photos = (result as? DataResult.Success)?.value?.items?.let(::photosOf).orEmpty()
                    if (photoStore.fetchMissing(current.ownerUid, photos)) emit(result)
                }
                val board = boards.map { list -> list.items.firstOrNull { it.id == id } }.distinctUntilChanged()
                combine(read, board, appLanguage) { result, chosen, language ->
                    val shown = { list: List<AacSymbol> ->
                        chosen?.let { starterVocabulary.symbolsIn(it, list, language).symbols } ?: list
                    }
                    // The missing words are found among the symbols as stored, in the board's own language.
                    val stored = (result as? DataResult.Success)?.value?.items.orEmpty()
                    BoardSymbols(
                        listed = listed(result, symbolOrder, shown) { it.toUiModel(photoStore) },
                        missing = chosen?.let { starterVocabulary.missingFrom(it, stored) }.orEmpty()
                    )
                }.onStart { emit(BoardSymbols()) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), BoardSymbols())

    /** The boards as shown, the suggested one named in the app's [language]. */
    private fun boardsIn(result: DataResult<ListSnapshot<AacBoard>>, language: String) =
        listed(result, boardOrder, shown = { list -> list.map { starterVocabulary.boardIn(it, language) } }) {
            BoardTabUiModel(it.id, it.name)
        }

    private val latestBoards: List<AacBoard> get() = boards.value.items
    private val latestSymbols: List<AacSymbol> get() = symbols.value.listed.items

    val uiState: StateFlow<AacOrganizeUiState> =
        combine(boards, symbols, selectedBoardId, interaction) { boardList, symbolList, boardId, local ->
            local.copy(
                boards = boardList.rows,
                symbols = symbolList.listed.rows,
                suggestedWordsToAdd = symbolList.missing.size,
                selectedBoardId = boardId,
                syncNoticeRes = boardList.noticeRes
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AacOrganizeUiState())

    fun show(ownerUid: String, viewer: AacViewer) {
        appLanguage.value = appLanguageTag
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

    /**
     * Completes a suggested board made when it had fewer words: the words it lacks go to its end, as the app makes
     * them, so they also follow the app's language. One write each, so it works offline; a second tap meanwhile
     * does nothing, and a board never passes [MAX_SYMBOLS_PER_BOARD].
     */
    fun onAddSuggestedWords() {
        val current = managingRequest() ?: return
        val boardId = selectedBoardId.value ?: return
        if (interaction.value.isAddingSuggestedWords) return
        val drafts = symbols.value.missing.take((MAX_SYMBOLS_PER_BOARD - latestSymbols.size).coerceAtLeast(0))
        if (drafts.isEmpty()) return
        val first = nextPosition(latestSymbols.map { it.position })
        interaction.update { it.copy(isAddingSuggestedWords = true, actionErrorRes = null) }
        viewModelScope.launch {
            val failure = drafts.withIndex().firstNotNullOfOrNull { (index, draft) ->
                val symbolId = boardRepository.newSymbolId(current.ownerUid, boardId)
                boardRepository.createSymbol(current.ownerUid, boardId, symbolId, draft, first + index)
                    as? DataResult.Failure
            }
            interaction.update {
                it.copy(isAddingSuggestedWords = false, actionErrorRes = failure?.error?.messageRes())
            }
        }
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
                boardRepository.createBoard(
                    current.ownerUid,
                    name,
                    nextPosition(latestBoards.map { it.position }),
                    // A new board is written in the app's language: its voice and grammar follow it from now on.
                    appLanguageTag
                )
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
                runWrite(onSuccess = { removePhotos(current, photos) }) {
                    boardRepository.deleteSymbol(current.ownerUid, boardId, pending.id)
                }
            }
        }
    }

    /** Photos of the deleted symbols are removed (here and in the account) only after the delete was accepted. */
    private fun deleteBoard(current: Request, boardId: String) {
        runWrite {
            val symbolsResult = boardRepository.getSymbols(current.ownerUid, boardId)
            val photos = (symbolsResult as? DataResult.Success)?.value?.let(::photosOf).orEmpty()
            boardRepository.deleteBoard(current.ownerUid, boardId).also { result ->
                if (result is DataResult.Success) {
                    removePhotos(current, photos)
                    if (selectedBoardId.value == boardId) onOpenBoard(null)
                }
            }
        }
    }

    private suspend fun removePhotos(current: Request, photos: List<StoredPhoto>) {
        // Whoever may delete the symbols removes their photos too, so none is left counting in the owner's limit (R6).
        if (canManageBoards(current.viewer)) photos.forEach { photoStore.deleteEverywhere(current.ownerUid, it) }
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
