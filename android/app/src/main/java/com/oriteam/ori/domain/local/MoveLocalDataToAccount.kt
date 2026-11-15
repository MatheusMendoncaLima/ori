package com.oriteam.ori.domain.local

import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.MAX_AAC_BOARDS
import com.oriteam.ori.domain.aac.MAX_SYMBOLS_PER_BOARD
import com.oriteam.ori.domain.aac.SavedMessageRepository
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.aac.contentLanguage
import com.oriteam.ori.domain.aac.nextPosition
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.DeleteScope
import com.oriteam.ori.domain.routine.MAX_ROUTINE_TASKS
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineSeries
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.domain.routine.TaskStatus
import com.oriteam.ori.domain.routine.seriesIdOf
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** How moving the device data to the account ended. */
enum class LocalDataMoveResult {
    /** There was nothing on the device, or everything is in the account now. */
    COMPLETED,

    /** No confirmation from the server. What was not confirmed stays here for the next attempt. */
    WAITING_FOR_CONNECTION,

    /** Some items do not fit the limits of the account. They stay on the device. */
    SOME_ITEMS_DID_NOT_FIT
}

private fun DataResult<WriteStatus>.isConfirmed() = this == DataResult.Success(WriteStatus.SYNCED)

/**
 * Moves what a person created without an account into the account they just signed in to.
 *
 * It is a merge, because the account may already have data, and it can be interrupted at any point
 * and run again: each item leaves the device only after the server confirmed it, and items the
 * account already has (same name, or same title on the same day and time) are not created twice.
 */
class MoveLocalDataToAccount @Inject constructor(
    private val localData: LocalDataStore,
    private val routine: RoutineRepository,
    private val boards: AacBoardRepository,
    private val savedMessages: SavedMessageRepository,
    private val photos: PhotoStore
) {
    /** Ends the attempt: what was not confirmed stays on the device. */
    private class NoConfirmation : Exception()

    suspend operator fun invoke(accountUid: String): LocalDataMoveResult {
        val data = localData.snapshot()
        if (data.isEmpty) return LocalDataMoveResult.COMPLETED
        return Attempt().run(accountUid, data)
    }

    /** One attempt. What it learns lives only as long as the attempt, so two attempts never share it. */
    private inner class Attempt {
        private var hasItemsOverLimit = false

        suspend fun run(accountUid: String, data: LocalDataSnapshot): LocalDataMoveResult = try {
            // Before the days: an occurrence that was marked finds its series already there. The days of a
            // series that stays here stay with it, or the account would show them as loose activities.
            val keptSeries = moveSeries(accountUid, data.series)
            moveTasks(accountUid, data.tasks.filterNot { seriesIdOf(it.id) in keptSeries })
            moveBoards(accountUid, data.boards)
            moveSavedMessages(accountUid, data)
            if (hasItemsOverLimit) LocalDataMoveResult.SOME_ITEMS_DID_NOT_FIT else LocalDataMoveResult.COMPLETED
        } catch (_: NoConfirmation) {
            LocalDataMoveResult.WAITING_FOR_CONNECTION
        }

        /**
         * The same id as on the device: a repeated attempt writes the same series. A series the account has no
         * room for stays on the device, and the others go on. Returns the ids of the ones that stayed.
         */
        private suspend fun moveSeries(accountUid: String, localSeries: List<RoutineSeries>): Set<String> {
            val kept = mutableSetOf<String>()
            localSeries.forEach { series ->
                val creation = routine.createActivity(accountUid, series.id, series.toDraft())
                if (creation == DataResult.Failure(DataError.PRECONDITION_FAILED)) {
                    hasItemsOverLimit = true
                    kept += series.id
                    return@forEach
                }
                creation.confirm()
                series.skippedDates.forEach { date ->
                    routine.delete(accountUid, series.occurrenceOn(date), DeleteScope.THIS_DAY).confirm()
                }
                localData.removeSeries(series.id)
            }
            return kept
        }

        private suspend fun moveTasks(accountUid: String, tasks: List<RoutineTask>) {
            tasks.groupBy { it.date }.forEach { (date, ofDay) ->
                val existing = routine.getDay(accountUid, RoutineViewer.Owner, date).valueOrWait().toMutableList()
                ofDay.forEach { task -> moveTask(accountUid, task, existing) }
            }
        }

        private suspend fun moveTask(accountUid: String, task: RoutineTask, existing: MutableList<RoutineTask>) {
            val match = existing.firstOrNull { it.title == task.title && it.scheduledTime == task.scheduledTime }
            when {
                match != null -> {
                    if (task.isDone && !match.isDone) routine.setDone(accountUid, match, true).confirm()
                    localData.removeTask(task.id)
                }
                existing.size >= MAX_ROUTINE_TASKS -> hasItemsOverLimit = true
                else -> {
                    moveNewTask(accountUid, task)
                    existing += task
                }
            }
        }

        private suspend fun moveNewTask(accountUid: String, task: RoutineTask) {
            val draft = ActivityDraft(
                title = task.title,
                date = task.date,
                scheduledTime = task.scheduledTime,
                description = task.description,
                ownerCanComplete = true,
                isHiddenFromCaregiver = task.status == TaskStatus.HIDDEN_FROM_CAREGIVER
            )
            // The same id as on the device: a repeated attempt writes the same activity.
            routine.createActivity(accountUid, task.id, draft).confirm()
            // The rules require a new activity to start as not done, so completion is a second step.
            if (task.isDone) routine.setTaskDone(accountUid, task.id, true).confirm()
            localData.removeTask(task.id)
        }

        private suspend fun moveBoards(accountUid: String, localBoards: List<LocalBoard>) {
            if (localBoards.isEmpty()) return
            val snapshot = boards.getBoards(accountUid).valueOrWait()
            // A cached answer could hide boards of the account and lead to duplicates.
            if (snapshot.isFromCache) throw NoConfirmation()
            val existing = snapshot.items.toMutableList()
            localBoards.forEach { local ->
                val match = existing.firstOrNull { it.name.equals(local.board.name, ignoreCase = true) }
                when {
                    match != null -> mergeSymbols(accountUid, match, local)
                    existing.size >= MAX_AAC_BOARDS -> hasItemsOverLimit = true
                    else -> {
                        local.symbols.forEach { uploadPhotoOf(accountUid, it) }
                        boards.createBoardWithSymbols(
                            accountUid,
                            local.board.name,
                            nextPosition(existing.map { it.position }),
                            local.symbols.map { it.toDraft() },
                            // A board from before boards recorded their language is in the app's.
                            local.board.contentLanguage
                        ).confirm()
                        localData.removeBoard(local.board.id)
                        existing += local.board.copy(position = nextPosition(existing.map { it.position }))
                    }
                }
            }
        }

        private suspend fun mergeSymbols(accountUid: String, target: AacBoard, local: LocalBoard) {
            val existing = boards.getSymbols(accountUid, target.id).valueOrWait().toMutableList()
            var isBoardEmptied = true
            local.symbols.forEach { symbol ->
                val isInAccount = existing.any { it.label.equals(symbol.label, ignoreCase = true) }
                when {
                    isInAccount -> localData.removeSymbol(symbol.id)
                    existing.size >= MAX_SYMBOLS_PER_BOARD -> {
                        hasItemsOverLimit = true
                        isBoardEmptied = false
                    }
                    else -> {
                        val position = nextPosition(existing.map { it.position })
                        uploadPhotoOf(accountUid, symbol)
                        boards.createSymbol(accountUid, target.id, symbol.id, symbol.toDraft(), position).confirm()
                        localData.removeSymbol(symbol.id)
                        existing += symbol.copy(position = position)
                    }
                }
            }
            if (isBoardEmptied) localData.removeBoard(local.board.id)
        }

        /** Over the limit of photos the symbol still moves: its photo just stays on this device. */
        private suspend fun uploadPhotoOf(accountUid: String, symbol: AacSymbol) {
            val photo = symbol.image as? SymbolImage.DevicePhoto ?: return
            val upload = photos.upload(accountUid, photo.photo)
            if (upload == DataResult.Failure(DataError.PRECONDITION_FAILED)) {
                hasItemsOverLimit = true
            } else {
                upload.confirm()
            }
        }

        private suspend fun moveSavedMessages(accountUid: String, data: LocalDataSnapshot) {
            if (data.savedMessages.isEmpty()) return
            val existing = savedMessages.observeSavedMessages(accountUid).first().valueOrWait().map { it.text }.toSet()
            data.savedMessages.forEach { message ->
                if (message.text !in existing) {
                    savedMessages.saveMessage(
                        accountUid,
                        message.text,
                        message.contentLanguage,
                        message.emoji,
                        message.position
                    ).confirm()
                }
                localData.removeSavedMessage(message.id)
            }
        }
    }

    private fun AacSymbol.toDraft() = SymbolDraft(label, phrase, image, color, wordClass)

    private fun <T> DataResult<T>.valueOrWait(): T = (this as? DataResult.Success)?.value ?: throw NoConfirmation()

    /** Anything but a confirmation stops here, so the item stays on the device. */
    private fun DataResult<WriteStatus>.confirm() {
        if (!isConfirmed()) throw NoConfirmation()
    }
}
