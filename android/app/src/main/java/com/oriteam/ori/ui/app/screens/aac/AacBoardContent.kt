package com.oriteam.ori.ui.app.screens.aac

import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SavedMessageRepository
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.aac.boardOrder
import com.oriteam.ori.domain.aac.symbolOrder
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.ListSnapshot
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.ui.app.screens.common.toContentState
import com.oriteam.ori.ui.components.state.ContentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/** Keeps the listeners alive briefly across rotation, but not while the app is in background. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** The lower part of the communication screen; [symbolsById] keeps the symbols as read, for the taps. */
internal data class LowerContent(
    val symbols: ContentState<List<AacSymbolUiModel>> = ContentState.Loading,
    val saved: ContentState<List<SavedMessage>> = ContentState.Loading,
    val symbolsById: Map<String, AacSymbol> = emptyMap()
)

/**
 * Reads what the communication screen shows: the boards and, below them, the symbols of the chosen board
 * or the saved phrases. A new value of [owner] (also a retry) reads again. The listeners run only while
 * the screen collects, in [scope].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AacBoardContent(
    owner: Flow<String>,
    selectionKey: Flow<String?>,
    private val boardRepository: AacBoardRepository,
    private val savedMessageRepository: SavedMessageRepository,
    private val photoStore: PhotoStore,
    scope: CoroutineScope
) {
    val boards: Flow<ContentState<List<AacBoard>>> = owner
        .flatMapLatest { ownerUid ->
            boardRepository.observeBoards(ownerUid)
                .map { result -> result.toContentState { it.items.sortedWith(boardOrder) } }
                .onStart { emit(ContentState.Loading) }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    val selection: Flow<AacSelection?> = combine(boards, selectionKey, ::resolveSelection).distinctUntilChanged()

    val lower: StateFlow<LowerContent> = combine(owner, selection, ::Pair)
        .flatMapLatest { (ownerUid, selected) ->
            when (selected) {
                is AacSelection.Board -> symbolsOf(ownerUid, selected.id)
                AacSelection.SavedMessages -> savedMessageRepository.observeSavedMessages(ownerUid)
                    .map { result -> LowerContent(saved = result.toContentState { it }) }
                null -> flowOf(LowerContent(symbols = ContentState.Empty))
            }.onStart { emit(LowerContent()) }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LowerContent())

    /** Photos added on another device arrive after the names, which never wait for them. */
    private fun symbolsOf(ownerUid: String, boardId: String): Flow<LowerContent> =
        boardRepository.observeSymbols(ownerUid, boardId).transformLatest { result ->
            emit(symbolsContent(result))
            val photos = (result as? DataResult.Success)?.value?.items.orEmpty()
                .mapNotNull { (it.image as? SymbolImage.DevicePhoto)?.photo }
            if (photoStore.fetchMissing(ownerUid, photos)) emit(symbolsContent(result))
        }

    private fun symbolsContent(result: DataResult<ListSnapshot<AacSymbol>>): LowerContent {
        val ordered = (result as? DataResult.Success)?.value?.items.orEmpty().sortedWith(symbolOrder)
        return LowerContent(
            symbols = result.toContentState { ordered.map { it.toUiModel(photoStore) } },
            symbolsById = ordered.associateBy { it.id }
        )
    }
}
