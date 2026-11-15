package com.oriteam.ori.data.aac

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.awaitWriteAcknowledgement
import com.oriteam.ori.data.firestore.observeList
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.MAX_AAC_BOARDS
import com.oriteam.ori.domain.aac.MAX_SYMBOLS_PER_BOARD
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.ListSnapshot
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Boards in `users/{owner}/aacBoards`, symbols in `.../aacBoards/{board}/symbols`. Queries use
 * only a limit (no filters or ordering), so no composite index is needed; the order is applied in
 * the domain ([com.oriteam.ori.domain.aac.symbolOrder]).
 */
@Suppress("TooManyFunctions") // Implements AacBoardRepository, one function per operation.
class FirestoreAacBoardRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : AacBoardRepository {

    private fun boardsOf(ownerUid: String) = firestore.collection(FirestoreSchema.USERS)
        .document(ownerUid)
        .collection(FirestoreSchema.AAC_BOARDS)

    private fun symbolsOf(ownerUid: String, boardId: String) =
        boardsOf(ownerUid).document(boardId).collection(FirestoreSchema.AAC_SYMBOLS)

    private fun symbolsQuery(ownerUid: String, boardId: String) =
        symbolsOf(ownerUid, boardId).limit(MAX_SYMBOLS_PER_BOARD.toLong())

    override fun observeBoards(ownerUid: String): Flow<DataResult<ListSnapshot<AacBoard>>> =
        boardsOf(ownerUid).limit(MAX_AAC_BOARDS.toLong()).observeList { aacBoardFrom(it.id, it.data.orEmpty()) }

    override suspend fun getBoards(ownerUid: String): DataResult<ListSnapshot<AacBoard>> = runDataOperation {
        val snapshot = boardsOf(ownerUid).limit(MAX_AAC_BOARDS.toLong()).get().await()
        ListSnapshot(
            items = snapshot.documents.mapNotNull { aacBoardFrom(it.id, it.data.orEmpty()) },
            isFromCache = snapshot.metadata.isFromCache
        )
    }

    override fun observeSymbols(ownerUid: String, boardId: String): Flow<DataResult<ListSnapshot<AacSymbol>>> =
        symbolsQuery(ownerUid, boardId).observeList { aacSymbolFrom(it.id, boardId, it.data.orEmpty()) }

    override suspend fun getSymbols(ownerUid: String, boardId: String): DataResult<List<AacSymbol>> =
        runDataOperation {
            symbolsQuery(ownerUid, boardId).get().await().documents
                .mapNotNull { aacSymbolFrom(it.id, boardId, it.data.orEmpty()) }
        }

    override fun newSymbolId(ownerUid: String, boardId: String): String = symbolsOf(ownerUid, boardId).document().id

    override suspend fun createBoard(ownerUid: String, name: String, position: Int, language: String) =
        createBoardWithSymbols(ownerUid, name, position, symbols = emptyList(), language = language)

    override suspend fun createBoardWithSymbols(
        ownerUid: String,
        name: String,
        position: Int,
        symbols: List<SymbolDraft>,
        language: String
    ): DataResult<WriteStatus> = writeAsAuthor { author ->
        val board = boardsOf(ownerUid).document()
        set(board, newBoardDocument(name, position, author, language))
        symbols.forEachIndexed { index, draft ->
            set(symbolsOf(ownerUid, board.id).document(), draft.toNewDocument(author, position = index))
        }
    }

    override suspend fun renameBoard(ownerUid: String, boardId: String, name: String) = write {
        update(
            boardsOf(ownerUid).document(boardId),
            mapOf(FirestoreSchema.NAME to name, FirestoreSchema.UPDATED_AT to FieldValue.serverTimestamp())
        )
    }

    /**
     * Reads the symbols first (cache when offline), then deletes them and the board in one batch.
     * The app never stores more than [MAX_SYMBOLS_PER_BOARD] symbols, so one read covers them.
     */
    override suspend fun deleteBoard(ownerUid: String, boardId: String): DataResult<WriteStatus> {
        val symbols = runDataOperation { symbolsQuery(ownerUid, boardId).get().await().documents }
        if (symbols is DataResult.Failure) return symbols
        return write {
            (symbols as DataResult.Success).value.forEach { delete(it.reference) }
            delete(boardsOf(ownerUid).document(boardId))
        }
    }

    override suspend fun createSymbol(
        ownerUid: String,
        boardId: String,
        symbolId: String,
        draft: SymbolDraft,
        position: Int
    ) = writeAsAuthor { author ->
        set(symbolsOf(ownerUid, boardId).document(symbolId), draft.toNewDocument(author, position))
    }

    override suspend fun updateSymbol(ownerUid: String, boardId: String, symbolId: String, draft: SymbolDraft) =
        write { update(symbolsOf(ownerUid, boardId).document(symbolId), draft.toEditableFields()) }

    override suspend fun deleteSymbol(ownerUid: String, boardId: String, symbolId: String) =
        write { delete(symbolsOf(ownerUid, boardId).document(symbolId)) }

    override suspend fun reorderBoards(ownerUid: String, positions: Map<String, Int>) = write {
        positions.forEach { (id, position) -> update(boardsOf(ownerUid).document(id), positionFields(position)) }
    }

    override suspend fun reorderSymbols(ownerUid: String, boardId: String, positions: Map<String, Int>) = write {
        positions.forEach { (id, position) ->
            update(symbolsOf(ownerUid, boardId).document(id), positionFields(position))
        }
    }

    private fun positionFields(position: Int): Map<String, Any> =
        mapOf(FirestoreSchema.POSITION to position, FirestoreSchema.UPDATED_AT to FieldValue.serverTimestamp())

    /** Every change is one atomic batch: it is applied completely or not at all. */
    private suspend fun write(changes: WriteBatch.() -> Unit): DataResult<WriteStatus> = runDataOperation {
        val commit = firestore.batch().apply(changes).commit()
        awaitWriteAcknowledgement { commit.await() }
    }

    /** New documents record their author; the rules check that it is the caller. */
    private suspend fun writeAsAuthor(changes: WriteBatch.(author: String) -> Unit): DataResult<WriteStatus> {
        val author = firebaseAuth.currentUser?.uid ?: return DataResult.Failure(DataError.PERMISSION_DENIED)
        return write { changes(author) }
    }
}
