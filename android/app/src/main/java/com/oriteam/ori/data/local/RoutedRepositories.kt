package com.oriteam.ori.data.local

import com.oriteam.ori.data.aac.FirestoreAacBoardRepository
import com.oriteam.ori.data.aac.FirestoreSavedMessageRepository
import com.oriteam.ori.data.routine.FirestoreRoutineRepository
import com.oriteam.ori.data.routine.FirestoreRoutineSeriesRepository
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.SavedMessageRepository
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.DeleteScope
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineSeriesRepository
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import java.time.LocalDate
import javax.inject.Inject

/*
 * The owner of the data decides where it lives: on this device (LOCAL_OWNER_UID) or in the account.
 * Screens and ViewModels receive an owner and never need to know the difference, so every feature
 * works the same with and without an account.
 */

class RoutedRoutineRepository(
    private val account: RoutineRepository,
    private val local: RoutineRepository
) : RoutineRepository {
    /** For Hilt. The constructor above takes any implementation, so a test can check where each call goes. */
    @Inject constructor(firestore: FirestoreRoutineRepository, room: LocalRoutineRepository) :
        this(account = firestore, local = room)

    private fun source(ownerUid: String): RoutineRepository = if (ownerUid == LOCAL_OWNER_UID) local else account

    override fun observeDay(ownerUid: String, viewer: RoutineViewer, date: LocalDate) =
        source(ownerUid).observeDay(ownerUid, viewer, date)

    override suspend fun getDay(ownerUid: String, viewer: RoutineViewer, date: LocalDate) =
        source(ownerUid).getDay(ownerUid, viewer, date)

    override fun newActivityId(ownerUid: String) = source(ownerUid).newActivityId(ownerUid)

    override suspend fun createActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer
    ) = source(ownerUid).createActivity(ownerUid, activityId, draft, viewer)

    override suspend fun updateActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer
    ) = source(ownerUid).updateActivity(ownerUid, activityId, draft, viewer)

    override suspend fun deleteActivity(ownerUid: String, activityId: String) =
        source(ownerUid).deleteActivity(ownerUid, activityId)

    override suspend fun setTaskDone(ownerUid: String, taskId: String, isDone: Boolean) =
        source(ownerUid).setTaskDone(ownerUid, taskId, isDone)

    // The members below have a default body in the interface. Without these lines that default would run
    // here: getDays, for example, would read one day per query instead of the whole period at once.
    override suspend fun getDays(ownerUid: String, viewer: RoutineViewer, dates: List<LocalDate>) =
        source(ownerUid).getDays(ownerUid, viewer, dates)

    override suspend fun setDone(ownerUid: String, task: RoutineTask, isDone: Boolean) =
        source(ownerUid).setDone(ownerUid, task, isDone)

    override suspend fun delete(ownerUid: String, task: RoutineTask, scope: DeleteScope) =
        source(ownerUid).delete(ownerUid, task, scope)
}

class RoutedRoutineSeriesRepository(
    private val account: RoutineSeriesRepository,
    private val local: RoutineSeriesRepository
) : RoutineSeriesRepository {
    /** For Hilt. The constructor above takes any implementation, so a test can check where each call goes. */
    @Inject constructor(firestore: FirestoreRoutineSeriesRepository, room: LocalRoutineSeriesRepository) :
        this(account = firestore, local = room)

    private fun source(ownerUid: String): RoutineSeriesRepository =
        if (ownerUid == LOCAL_OWNER_UID) local else account

    override fun observeSeries(ownerUid: String, viewer: RoutineViewer) = source(
        ownerUid
    ).observeSeries(ownerUid, viewer)

    override suspend fun getSeries(ownerUid: String, viewer: RoutineViewer) =
        source(ownerUid).getSeries(ownerUid, viewer)

    override suspend fun createSeries(ownerUid: String, seriesId: String, draft: ActivityDraft, rule: RepeatRule) =
        source(ownerUid).createSeries(ownerUid, seriesId, draft, rule)

    override suspend fun updateSeries(ownerUid: String, seriesId: String, draft: ActivityDraft, rule: RepeatRule) =
        source(ownerUid).updateSeries(ownerUid, seriesId, draft, rule)

    override suspend fun deleteSeries(ownerUid: String, seriesId: String) =
        source(ownerUid).deleteSeries(ownerUid, seriesId)

    override suspend fun skipDate(ownerUid: String, seriesId: String, date: LocalDate) =
        source(ownerUid).skipDate(ownerUid, seriesId, date)

    override suspend fun replaceActivityWithSeries(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        rule: RepeatRule
    ) = source(ownerUid).replaceActivityWithSeries(ownerUid, activityId, draft, rule)

    override suspend fun replaceSeriesWithActivity(ownerUid: String, seriesId: String, draft: ActivityDraft) =
        source(ownerUid).replaceSeriesWithActivity(ownerUid, seriesId, draft)
}

@Suppress("TooManyFunctions") // Implements AacBoardRepository, one function per operation.
class RoutedAacBoardRepository(
    private val account: AacBoardRepository,
    private val local: AacBoardRepository
) : AacBoardRepository {
    /** For Hilt. The constructor above takes any implementation, so a test can check where each call goes. */
    @Inject constructor(firestore: FirestoreAacBoardRepository, room: LocalAacBoardRepository) :
        this(account = firestore, local = room)

    private fun source(ownerUid: String): AacBoardRepository = if (ownerUid == LOCAL_OWNER_UID) local else account

    override fun observeBoards(ownerUid: String) = source(ownerUid).observeBoards(ownerUid)

    override suspend fun getBoards(ownerUid: String) = source(ownerUid).getBoards(ownerUid)

    override fun observeSymbols(ownerUid: String, boardId: String) = source(ownerUid).observeSymbols(ownerUid, boardId)

    override suspend fun getSymbols(ownerUid: String, boardId: String) = source(ownerUid).getSymbols(ownerUid, boardId)

    override fun newSymbolId(ownerUid: String, boardId: String) = source(ownerUid).newSymbolId(ownerUid, boardId)

    override suspend fun createBoard(ownerUid: String, name: String, position: Int) =
        source(ownerUid).createBoard(ownerUid, name, position)

    override suspend fun createBoardWithSymbols(
        ownerUid: String,
        name: String,
        position: Int,
        symbols: List<SymbolDraft>
    ) = source(ownerUid).createBoardWithSymbols(ownerUid, name, position, symbols)

    override suspend fun renameBoard(ownerUid: String, boardId: String, name: String) =
        source(ownerUid).renameBoard(ownerUid, boardId, name)

    override suspend fun deleteBoard(ownerUid: String, boardId: String) = source(
        ownerUid
    ).deleteBoard(ownerUid, boardId)

    override suspend fun createSymbol(
        ownerUid: String,
        boardId: String,
        symbolId: String,
        draft: SymbolDraft,
        position: Int
    ) = source(ownerUid).createSymbol(ownerUid, boardId, symbolId, draft, position)

    override suspend fun updateSymbol(ownerUid: String, boardId: String, symbolId: String, draft: SymbolDraft) =
        source(ownerUid).updateSymbol(ownerUid, boardId, symbolId, draft)

    override suspend fun deleteSymbol(ownerUid: String, boardId: String, symbolId: String) =
        source(ownerUid).deleteSymbol(ownerUid, boardId, symbolId)

    override suspend fun reorderBoards(ownerUid: String, positions: Map<String, Int>) =
        source(ownerUid).reorderBoards(ownerUid, positions)

    override suspend fun reorderSymbols(ownerUid: String, boardId: String, positions: Map<String, Int>) =
        source(ownerUid).reorderSymbols(ownerUid, boardId, positions)
}

class RoutedSavedMessageRepository(
    private val account: SavedMessageRepository,
    private val local: SavedMessageRepository
) : SavedMessageRepository {
    /** For Hilt. The constructor above takes any implementation, so a test can check where each call goes. */
    @Inject constructor(firestore: FirestoreSavedMessageRepository, room: LocalSavedMessageRepository) :
        this(account = firestore, local = room)

    private fun source(ownerUid: String): SavedMessageRepository = if (ownerUid == LOCAL_OWNER_UID) local else account

    override fun observeSavedMessages(ownerUid: String) = source(ownerUid).observeSavedMessages(ownerUid)

    override suspend fun saveMessage(ownerUid: String, text: String) = source(ownerUid).saveMessage(ownerUid, text)

    override suspend fun deleteSavedMessage(ownerUid: String, messageId: String) =
        source(ownerUid).deleteSavedMessage(ownerUid, messageId)
}
