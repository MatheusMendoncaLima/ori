package com.oriteam.ori.ui.app.screens.aac

import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SavedMessageRepository
import com.oriteam.ori.domain.aac.StarterVocabulary
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.aac.boardOrder
import com.oriteam.ori.domain.aac.moved
import com.oriteam.ori.domain.aac.positionChanges
import com.oriteam.ori.domain.aac.symbolOrder
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.ListSnapshot
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.ui.app.screens.common.loadingAfter
import com.oriteam.ori.ui.app.screens.common.toContentState
import com.oriteam.ori.ui.components.state.ContentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/** Keeps the listeners alive briefly across rotation, but not while the app is in background. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * The positions that change when [symbolId] goes to [toIndex] of the board shown; null when nothing changes, there
 * is no board or the index is outside it.
 */
internal fun LowerContent.positionsAfterMoving(symbolId: String, toIndex: Int): Map<String, Int>? {
    val ordered = (symbols as? ContentState.Content)?.value?.map { it.id }.orEmpty()
    val from = ordered.indexOf(symbolId)
    val isMove = symbolsKey != null && from >= 0 && toIndex in ordered.indices && from != toIndex
    val positions = symbolsById.mapValues { it.value.position }
    return if (isMove) positionChanges(positions, ordered.moved(from, toIndex)).ifEmpty { null } else null
}

/** The symbols of the board of "Montar"; [symbolsById] keeps them as read, for the taps. */
internal data class LowerContent(
    val symbols: ContentState<List<AacSymbolUiModel>> = ContentState.Loading,
    val symbolsById: Map<String, AacSymbol> = emptyMap(),
    /** The board the symbols belong to: another board is another grid, shown at once. */
    val symbolsKey: String? = null,
    /** The language the chosen board is built and said in (see [StarterVocabulary.symbolsIn]); null with none. */
    val language: String? = null
)

/**
 * Reads what the communication screen shows: the boards, the symbols of the board of "Montar" and the saved
 * phrases of "Prontas". A new value of [owner] (also a retry) reads again. The listeners run only while the screen
 * collects, in [scope]. The suggested board is shown in [appLanguage] ([StarterVocabulary]).
 *
 * The symbols stay read while "Prontas" is open, so "Montar" is shown at once, without being made again (M16); the
 * saved phrases are read only while "Prontas" is open, and the list last read stays meanwhile.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList") // The sources of one screen, each replaceable in tests.
internal class AacBoardContent(
    owner: Flow<String>,
    selectionKey: Flow<String?>,
    lastBoardKey: Flow<String?>,
    private val boardRepository: AacBoardRepository,
    private val savedMessageRepository: SavedMessageRepository,
    private val photoStore: PhotoStore,
    private val vocabulary: StarterVocabulary,
    private val appLanguage: Flow<String>,
    scope: CoroutineScope
) {
    val boards: Flow<ContentState<List<AacBoard>>> = owner
        .flatMapLatest { ownerUid ->
            combine(boardRepository.observeBoards(ownerUid), appLanguage) { result, language ->
                result.toContentState { snapshot ->
                    snapshot.items.sortedWith(boardOrder).map { vocabulary.boardIn(it, language) }
                }
            }.onStart { emit(ContentState.Loading) }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    val selection: Flow<AacSelection?> = combine(boards, selectionKey, ::resolveSelection).distinctUntilChanged()

    /**
     * The board "Montar" shows: the chosen one, or, while "Prontas" is open, the last one used (the first board
     * without one, or when it is gone). Null without boards, or while they are read without a board asked.
     */
    val buildBoard: Flow<AacSelection.Board?> = combine(boards, selectionKey, lastBoardKey) { list, key, last ->
        val asked = if (AacSelection.parse(key) is AacSelection.Board) key else last
        resolveSelection(list, asked) as? AacSelection.Board
    }.distinctUntilChanged()

    val symbols: StateFlow<LowerContent> = combine(owner, buildBoard, ::Pair)
        .flatMapLatest { (ownerUid, board) ->
            when (board) {
                null -> flowOf(LowerContent(symbols = ContentState.Empty))
                else -> symbolsOf(ownerUid, board.id)
            }.loadingAfter { LowerContent() }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LowerContent())

    val saved: StateFlow<ContentState<List<SavedMessage>>> = combine(owner, selection, ::Pair)
        .flatMapLatest { (ownerUid, selected) ->
            if (selected == AacSelection.SavedMessages) {
                savedMessageRepository.observeSavedMessages(ownerUid)
                    .map { result -> result.toContentState { it } }
                    .loadingAfter { ContentState.Loading }
            } else {
                emptyFlow()
            }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ContentState.Loading)

    /**
     * The language the board of "Montar" is built and said in: the app's for the suggested board, its own otherwise
     * (Portuguese for a board from before boards had one); null without a board.
     */
    val language: Flow<String?> = symbols.map { it.language }.distinctUntilChanged()

    /** Photos added on another device arrive after the names, which never wait for them. */
    private fun symbolsOf(ownerUid: String, boardId: String): Flow<LowerContent> {
        val read = boardRepository.observeSymbols(ownerUid, boardId).transformLatest { result ->
            emit(result)
            val photos = (result as? DataResult.Success)?.value?.items.orEmpty()
                .mapNotNull { (it.image as? SymbolImage.DevicePhoto)?.photo }
            if (photoStore.fetchMissing(ownerUid, photos)) emit(result)
        }
        val board = boards.map { state -> (state as? ContentState.Content)?.value?.firstOrNull { it.id == boardId } }
            .distinctUntilChanged()
        return combine(read, board, appLanguage) { result, chosen, language ->
            symbolsContent(result, boardId, chosen, language)
        }
    }

    private fun symbolsContent(
        result: DataResult<ListSnapshot<AacSymbol>>,
        boardId: String,
        board: AacBoard?,
        language: String
    ): LowerContent {
        val ordered = (result as? DataResult.Success)?.value?.items.orEmpty().sortedWith(symbolOrder)
        val shown = board?.let { vocabulary.symbolsIn(it, ordered, language) }
        val symbols = shown?.symbols ?: ordered
        return LowerContent(
            symbols = result.toContentState { symbols.map { it.toUiModel(photoStore) } },
            symbolsById = symbols.associateBy { it.id },
            symbolsKey = boardId,
            language = shown?.language
        )
    }
}
