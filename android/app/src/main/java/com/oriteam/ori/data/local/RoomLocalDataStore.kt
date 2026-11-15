package com.oriteam.ori.data.local

import androidx.room.withTransaction
import com.oriteam.ori.domain.aac.AacUsageRepository
import com.oriteam.ori.domain.aac.MAX_SYMBOLS_PER_BOARD
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.local.LocalBoard
import com.oriteam.ori.domain.local.LocalDataSnapshot
import com.oriteam.ori.domain.local.LocalDataStore
import java.time.LocalDate
import javax.inject.Inject

class RoomLocalDataStore @Inject constructor(private val database: OriDatabase) : LocalDataStore {
    private val routine = database.routineDao()
    private val series = database.routineSeriesDao()
    private val aac = database.aacDao()
    private val messages = database.savedMessageDao()

    override suspend fun snapshot(): LocalDataSnapshot = database.withTransaction {
        LocalDataSnapshot(
            tasks = routine.getAll().map { it.toDomain() },
            series = series.getAll().mapNotNull { it.toDomain() },
            boards = aac.getBoards().map { board ->
                LocalBoard(
                    board.toDomain(),
                    aac.getSymbols(board.id, MAX_SYMBOLS_PER_BOARD).map { it.toDomain() }
                )
            },
            savedMessages = messages.getAll().map { it.toSavedMessage() }
        )
    }

    override suspend fun removeTask(id: String) = routine.delete(id)

    override suspend fun removeSeries(id: String) = series.delete(id)

    override suspend fun removeBoard(id: String) = aac.deleteBoard(id)

    override suspend fun removeSymbol(id: String) = aac.deleteSymbol(id)

    override suspend fun removeSavedMessage(id: String) = messages.delete(id)

    override suspend fun eraseAll() {
        database.withTransaction {
            routine.deleteAll()
            series.deleteAll()
            aac.deleteAllBoards()
            messages.deleteAll()
            database.aacUsageDao().deleteAll()
        }
    }
}

class RoomAacUsageRepository @Inject constructor(database: OriDatabase) : AacUsageRepository {
    private val dao = database.aacUsageDao()

    override suspend fun recordCommunication(date: LocalDate) {
        runLocalRead { dao.record(date.toString()) }
    }

    /** A failed read counts nothing: the count is a summary, and the report shows the rest of the data anyway. */
    override suspend fun countBetween(start: LocalDate, end: LocalDate): Int {
        val result = runLocalRead { dao.countBetween(start.toString(), end.toString()) }
        return (result as? DataResult.Success)?.value ?: 0
    }
}
