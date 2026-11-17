package com.oriteam.ori.domain.local

import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.routine.RoutineSeries
import com.oriteam.ori.domain.routine.RoutineTask

/** One board kept on this device, with what is inside it. */
data class LocalBoard(val board: AacBoard, val symbols: List<AacSymbol>)

/** A copy of everything a person without an account has on this device. */
data class LocalDataSnapshot(
    val tasks: List<RoutineTask> = emptyList(),
    val series: List<RoutineSeries> = emptyList(),
    val boards: List<LocalBoard> = emptyList(),
    val savedMessages: List<SavedMessage> = emptyList()
) {
    val isEmpty: Boolean get() = tasks.isEmpty() && series.isEmpty() && boards.isEmpty() && savedMessages.isEmpty()
}

/** Reads the device-only data as a whole and removes it piece by piece, as it reaches the account. */
interface LocalDataStore {
    suspend fun snapshot(): LocalDataSnapshot

    suspend fun removeTask(id: String)

    suspend fun removeSeries(id: String)

    suspend fun removeBoard(id: String)

    suspend fun removeSymbol(id: String)

    suspend fun removeSavedMessage(id: String)

    /** The person asked to erase what is on this device. A failure keeps everything, and is said (R12). */
    suspend fun eraseAll(): DataResult<WriteStatus>
}
