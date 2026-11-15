package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.ListSnapshot
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow

/**
 * Boards and symbols of one person ([ownerUid]). Every write works offline: it is shown at
 * once and sent when the connection returns ([WriteStatus.PENDING_SYNC]).
 */
@Suppress("TooManyFunctions") // One function per persisted operation; splitting would scatter one collection.
interface AacBoardRepository {
    /** Follows the boards (at most [MAX_AAC_BOARDS]), cached data first when available. */
    fun observeBoards(ownerUid: String): Flow<DataResult<ListSnapshot<AacBoard>>>

    /** One-time read of the boards; [ListSnapshot.isFromCache] says whether the server answered. */
    suspend fun getBoards(ownerUid: String): DataResult<ListSnapshot<AacBoard>>

    /** Follows the symbols of one board (at most [MAX_SYMBOLS_PER_BOARD]). */
    fun observeSymbols(ownerUid: String, boardId: String): Flow<DataResult<ListSnapshot<AacSymbol>>>

    /** One-time read of a board's symbols, used to check duplicates and the board limit. */
    suspend fun getSymbols(ownerUid: String, boardId: String): DataResult<List<AacSymbol>>

    /**
     * Reserves an id without network. Creating twice with the same id stores one symbol, so a
     * repeated tap or a form restored after a restart never duplicates it.
     */
    fun newSymbolId(ownerUid: String, boardId: String): String

    /** [language] is the one the board is written in, as a language tag (see [AacBoard.language]). */
    suspend fun createBoard(ownerUid: String, name: String, position: Int, language: String): DataResult<WriteStatus>

    /** Creates a board already filled with [symbols], in one atomic write. */
    suspend fun createBoardWithSymbols(
        ownerUid: String,
        name: String,
        position: Int,
        symbols: List<SymbolDraft>,
        language: String
    ): DataResult<WriteStatus>

    suspend fun renameBoard(ownerUid: String, boardId: String, name: String): DataResult<WriteStatus>

    /** Deletes the board and its symbols together, so no symbol is left without a board. */
    suspend fun deleteBoard(ownerUid: String, boardId: String): DataResult<WriteStatus>

    suspend fun createSymbol(
        ownerUid: String,
        boardId: String,
        symbolId: String,
        draft: SymbolDraft,
        position: Int
    ): DataResult<WriteStatus>

    suspend fun updateSymbol(
        ownerUid: String,
        boardId: String,
        symbolId: String,
        draft: SymbolDraft
    ): DataResult<WriteStatus>

    suspend fun deleteSymbol(ownerUid: String, boardId: String, symbolId: String): DataResult<WriteStatus>

    /** Writes new positions (from [positionChanges]) in one atomic batch. */
    suspend fun reorderBoards(ownerUid: String, positions: Map<String, Int>): DataResult<WriteStatus>

    suspend fun reorderSymbols(ownerUid: String, boardId: String, positions: Map<String, Int>): DataResult<WriteStatus>
}
