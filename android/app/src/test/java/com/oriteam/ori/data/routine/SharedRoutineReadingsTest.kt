package com.oriteam.ori.data.routine

import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RepeatingRoutineRepository
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineSeries
import com.oriteam.ori.domain.routine.RoutineSeriesRepository
import com.oriteam.ori.domain.routine.RoutineSeriesSnapshot
import com.oriteam.ori.domain.routine.RoutineSnapshot
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.testing.DAY
import com.oriteam.ori.testing.FakeRoutineRepository
import com.oriteam.ori.testing.OWNER
import com.oriteam.ori.testing.assertForwardsEveryMember
import com.oriteam.ori.testing.snapshotOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Counts how many readings of the repeating activities were opened and closed. */
private class CountingSeries : RoutineSeriesRepository {
    var started = 0
    var stopped = 0

    override fun observeSeries(ownerUid: String, viewer: RoutineViewer) = flow {
        emit(DataResult.Success(RoutineSeriesSnapshot(emptyList(), isFromCache = false, hasPendingWrites = false)))
        awaitCancellation()
    }.onStart { started++ }.onCompletion { stopped++ }

    override suspend fun getSeries(ownerUid: String, viewer: RoutineViewer) =
        DataResult.Success(emptyList<RoutineSeries>())

    override suspend fun createSeries(ownerUid: String, seriesId: String, draft: ActivityDraft, rule: RepeatRule) =
        written()

    override suspend fun updateSeries(ownerUid: String, seriesId: String, draft: ActivityDraft, rule: RepeatRule) =
        written()

    override suspend fun deleteSeries(ownerUid: String, seriesId: String) = written()

    override suspend fun skipDate(ownerUid: String, seriesId: String, date: LocalDate) = written()

    override suspend fun replaceActivityWithSeries(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        rule: RepeatRule
    ) = written()

    override suspend fun replaceSeriesWithActivity(ownerUid: String, seriesId: String, draft: ActivityDraft) =
        written()

    private fun written(): DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)
}

class SharedRoutineReadingsTest {
    private val tasks = FakeRoutineRepository().apply { routine.value = snapshotOf() }
    private val series = CountingSeries()

    /** Built as the app builds it, with the readings running on the test clock. */
    private fun TestScope.repository(): RoutineRepository {
        val readings = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        return SharedReadingRoutineRepository(
            RepeatingRoutineRepository(tasks, SharedReadingSeriesRepository(series, readings)),
            readings
        )
    }

    /** A screen showing [date]; cancelling the returned job is the screen leaving. */
    private fun TestScope.screen(
        repository: RoutineRepository,
        date: LocalDate,
        received: MutableList<DataResult<RoutineSnapshot>> = mutableListOf()
    ) = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        repository.observeDay(OWNER, RoutineViewer.Owner, date).collect { received += it }
    }

    @Test
    fun `screens showing the same day share one reading, and the second gets the day at once`() = runTest {
        val repository = repository()
        val today = mutableListOf<DataResult<RoutineSnapshot>>()
        val tasksScreen = mutableListOf<DataResult<RoutineSnapshot>>()

        screen(repository, DAY, today)
        screen(repository, DAY, tasksScreen)

        assertEquals(listOf(DAY), tasks.observedDays)
        assertEquals(1, series.started)
        assertEquals(today, tasksScreen)
        assertEquals(1, tasksScreen.size)
    }

    @Test
    fun `moving to another day keeps the one reading of the repeating activities`() = runTest {
        val repository = repository()

        screen(repository, DAY).cancel()
        screen(repository, DAY.plusDays(1))

        assertEquals(listOf(DAY, DAY.plusDays(1)), tasks.observedDays)
        assertEquals(1, series.started)
    }

    @Test
    fun `readings stop a moment after the last screen leaves`() = runTest {
        val repository = repository()

        screen(repository, DAY).cancel()
        // The day stops first; then the repeating activities, which only that day was reading.
        advanceTimeBy(2 * SHARED_READING_KEEP_ALIVE_MILLIS + 1)

        assertEquals(1, series.stopped)
        screen(repository, DAY)
        assertEquals(listOf(DAY, DAY), tasks.observedDays)
        assertEquals(2, series.started)
    }

    @Test
    fun `after a failure, trying again opens a new reading instead of repeating the error`() = runTest {
        tasks.routine.value = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        val repository = repository()
        val received = mutableListOf<DataResult<RoutineSnapshot>>()

        screen(repository, DAY, received).cancel()
        screen(repository, DAY)

        assertTrue(received.single() is DataResult.Failure)
        assertEquals(listOf(DAY, DAY), tasks.observedDays)
    }

    @Test
    fun `everything that is not a shared reading goes straight through, reading several days included`() = runTest {
        assertForwardsEveryMember(RoutineRepository::class.java, OWNER) {
            SharedReadingRoutineRepository(it, backgroundScope)
        }
        assertForwardsEveryMember(RoutineSeriesRepository::class.java, OWNER) {
            SharedReadingSeriesRepository(it, backgroundScope)
        }
    }
}
