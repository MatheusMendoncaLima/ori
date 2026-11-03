package com.oriteam.ori.data.local

import com.oriteam.ori.data.aac.symbolImageFrom
import com.oriteam.ori.data.aac.toStoredImage
import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.MAX_AAC_BOARDS
import com.oriteam.ori.domain.aac.MAX_SAVED_MESSAGES
import com.oriteam.ori.domain.aac.MAX_SYMBOLS_PER_BOARD
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SavedMessageRepository
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.ListSnapshot
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

internal fun AacBoardEntity.toDomain() = AacBoard(id, name, position, createdBy = LOCAL_OWNER_UID)

internal fun AacSymbolEntity.toDomain() = AacSymbol(
    id = id,
    boardId = boardId,
    label = label,
    phrase = phrase,
    image = symbolImageFrom(image),
    position = position,
    createdBy = LOCAL_OWNER_UID
)

/** Boards and symbols of someone without an account. */
@Suppress("TooManyFunctions") // Implements AacBoardRepository, one function per operation.
class LocalAacBoardRepository @Inject constructor(database: OriDatabase) : AacBoardRepository {
    private val dao = database.aacDao()

    override fun observeBoards(ownerUid: String): Flow<DataResult<ListSnapshot<AacBoard>>> =
        dao.observeBoards(MAX_AAC_BOARDS).map { boards -> ListSnapshot(boards.map { it.toDomain() }) }.asLocalResult()

    override suspend fun getBoards(ownerUid: String) =
        runLocalRead { ListSnapshot(dao.getBoards().map { it.toDomain() }) }

    override fun observeSymbols(ownerUid: String, boardId: String): Flow<DataResult<ListSnapshot<AacSymbol>>> =
        dao.observeSymbols(boardId, MAX_SYMBOLS_PER_BOARD)
            .map { symbols -> ListSnapshot(symbols.map { it.toDomain() }) }
            .asLocalResult()

    override suspend fun getSymbols(ownerUid: String, boardId: String) =
        runLocalRead { dao.getSymbols(boardId, MAX_SYMBOLS_PER_BOARD).map { it.toDomain() } }

    override fun newSymbolId(ownerUid: String, boardId: String): String = UUID.randomUUID().toString()

    override suspend fun createBoard(ownerUid: String, name: String, position: Int) =
        createBoardWithSymbols(ownerUid, name, position, symbols = emptyList())

    override suspend fun createBoardWithSymbols(
        ownerUid: String,
        name: String,
        position: Int,
        symbols: List<SymbolDraft>
    ) = runLocalWrite {
        val board = AacBoardEntity(UUID.randomUUID().toString(), name, position)
        dao.insertBoardWithSymbols(
            board,
            symbols.mapIndexed { index, draft -> draft.toEntity(UUID.randomUUID().toString(), board.id, index) }
        )
    }

    override suspend fun renameBoard(ownerUid: String, boardId: String, name: String) =
        runLocalWrite { dao.renameBoard(boardId, name) }

    override suspend fun deleteBoard(ownerUid: String, boardId: String) = runLocalWrite { dao.deleteBoard(boardId) }

    override suspend fun createSymbol(
        ownerUid: String,
        boardId: String,
        symbolId: String,
        draft: SymbolDraft,
        position: Int
    ) = runLocalWrite { dao.upsertSymbol(draft.toEntity(symbolId, boardId, position)) }

    override suspend fun updateSymbol(ownerUid: String, boardId: String, symbolId: String, draft: SymbolDraft) =
        runLocalWrite { dao.updateSymbol(symbolId, draft.label, draft.phrase, draft.image?.toStoredImage()) }

    override suspend fun deleteSymbol(ownerUid: String, boardId: String, symbolId: String) =
        runLocalWrite { dao.deleteSymbol(symbolId) }

    override suspend fun reorderBoards(ownerUid: String, positions: Map<String, Int>) =
        runLocalWrite { dao.moveBoards(positions) }

    override suspend fun reorderSymbols(ownerUid: String, boardId: String, positions: Map<String, Int>) =
        runLocalWrite { dao.moveSymbols(positions) }

    private fun SymbolDraft.toEntity(id: String, boardId: String, position: Int) =
        AacSymbolEntity(id, boardId, label, phrase, image?.toStoredImage(), position)
}

/** Saved messages of someone without an account. */
class LocalSavedMessageRepository @Inject constructor(
    database: OriDatabase,
    private val clock: Clock
) : SavedMessageRepository {
    private val dao = database.savedMessageDao()

    override fun observeSavedMessages(ownerUid: String): Flow<DataResult<List<SavedMessage>>> =
        dao.observe(MAX_SAVED_MESSAGES).map { list -> list.map { SavedMessage(it.id, it.text) } }.asLocalResult()

    override suspend fun saveMessage(ownerUid: String, text: String) = runLocalWrite {
        dao.upsert(SavedMessageEntity(UUID.randomUUID().toString(), text, clock.millis()))
    }

    override suspend fun deleteSavedMessage(ownerUid: String, messageId: String) =
        runLocalWrite { dao.delete(messageId) }
}
