package com.oriteam.ori.data.routine

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineSeriesRepository
import com.oriteam.ori.domain.routine.RoutineSeriesSnapshot
import com.oriteam.ori.domain.routine.RoutineSnapshot
import com.oriteam.ori.domain.routine.RoutineViewer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformWhile
import java.time.LocalDate

/**
 * Time a reading keeps running after the last observer leaves. It covers the moment between leaving one
 * day and opening the next. Screens already keep their own subscription for 5 s when they leave, so going
 * from Início to Tarefas also finds the reading running.
 */
internal const val SHARED_READING_KEEP_ALIVE_MILLIS = 1_000L

/**
 * One running reading per key, given to everyone who observes that key at the same time:
 * - the first observer starts it, and it stops [SHARED_READING_KEEP_ALIVE_MILLIS] after the last one leaves;
 * - whoever arrives while it runs receives the latest value at once;
 * - a reading ends after a failure, so "Tentar novamente" starts a new one instead of receiving the old error.
 */
private class SharedReadings<K : Any, T>(private val scope: CoroutineScope, private val hasFailed: (T) -> Boolean) {
    private val running = HashMap<K, SharedFlow<T>>()

    /** The reading is looked up when collection starts, so an observer does not join one that already ended. */
    fun observe(key: K, start: () -> Flow<T>): Flow<T> = flow { emitAll(runningFor(key, start)) }

    private fun runningFor(key: K, start: () -> Flow<T>): SharedFlow<T> = synchronized(running) {
        running.getOrPut(key) {
            lateinit var reading: SharedFlow<T>
            reading = start()
                .transformWhile { value ->
                    emit(value)
                    !hasFailed(value)
                }
                .onCompletion { synchronized(running) { if (running[key] === reading) running.remove(key) } }
                .shareIn(scope, SharingStarted.WhileSubscribed(SHARED_READING_KEEP_ALIVE_MILLIS), replay = 1)
            reading
        }
    }
}

private data class DayKey(val ownerUid: String, val viewer: RoutineViewer, val date: LocalDate)

private data class SeriesKey(val ownerUid: String, val viewer: RoutineViewer)

/**
 * Início, Tarefas and Rotina look at the same day of the same person. Each one used to open its own
 * listeners; now they share one reading per owner, viewer and day. Everything else goes straight to [inner].
 */
class SharedReadingRoutineRepository(
    private val inner: RoutineRepository,
    scope: CoroutineScope
) : RoutineRepository by inner {
    private val days = SharedReadings<DayKey, DataResult<RoutineSnapshot>>(scope) { it is DataResult.Failure }

    override fun observeDay(ownerUid: String, viewer: RoutineViewer, date: LocalDate) =
        days.observe(DayKey(ownerUid, viewer, date)) { inner.observeDay(ownerUid, viewer, date) }
}

/**
 * The repeating activities do not depend on the day, but every change of day used to open their listener
 * again. Here all the days of the same person share one reading of them.
 */
class SharedReadingSeriesRepository(
    private val inner: RoutineSeriesRepository,
    scope: CoroutineScope
) : RoutineSeriesRepository by inner {
    private val series = SharedReadings<SeriesKey, DataResult<RoutineSeriesSnapshot>>(scope) {
        it is DataResult.Failure
    }

    override fun observeSeries(ownerUid: String, viewer: RoutineViewer) =
        series.observe(SeriesKey(ownerUid, viewer)) { inner.observeSeries(ownerUid, viewer) }
}
