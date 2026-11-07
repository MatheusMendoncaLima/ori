package com.oriteam.ori.domain.routine

import com.oriteam.ori.domain.carelink.CareLinkPermissions
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.model.TaskStatus
import com.oriteam.ori.testing.FakeRoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

private const val OWNER = "owner"

private class FakeSeries : RoutineSeriesRepository {
    val series = MutableStateFlow<DataResult<List<RoutineSeries>>>(DataResult.Success(emptyList()))
    val log = mutableListOf<String>()

    override fun observeSeries(ownerUid: String, viewer: RoutineViewer) = series.map { result ->
        when (result) {
            is DataResult.Failure -> result
            is DataResult.Success -> DataResult.Success(RoutineSeriesSnapshot(result.value, false, false))
        }
    }

    val viewersAsked = mutableListOf<RoutineViewer>()

    override suspend fun getSeries(ownerUid: String, viewer: RoutineViewer): DataResult<List<RoutineSeries>> {
        viewersAsked += viewer
        return series.value
    }

    override suspend fun createSeries(ownerUid: String, seriesId: String, draft: ActivityDraft, rule: RepeatRule) =
        record("create $seriesId")

    override suspend fun updateSeries(ownerUid: String, seriesId: String, draft: ActivityDraft, rule: RepeatRule) =
        record("update $seriesId")

    override suspend fun deleteSeries(ownerUid: String, seriesId: String) = record("delete $seriesId")

    override suspend fun skipDate(ownerUid: String, seriesId: String, date: LocalDate) = record("skip $date")

    override suspend fun replaceActivityWithSeries(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        rule: RepeatRule
    ) = record("activity $activityId becomes a series")

    override suspend fun replaceSeriesWithActivity(ownerUid: String, seriesId: String, draft: ActivityDraft) =
        record("series $seriesId becomes an activity")

    private fun record(entry: String): DataResult<WriteStatus> {
        log += entry
        return DataResult.Success(WriteStatus.SYNCED)
    }
}

class RepeatingRoutineRepositoryTest {
    private val monday = LocalDate.of(2026, 11, 2)
    private val tasks = FakeRoutineRepository()
    private val rules = FakeSeries()
    private val repository = RepeatingRoutineRepository(tasks, rules)

    private fun everyMonday(skipped: Set<LocalDate> = emptySet()) = RoutineSeries(
        id = "s1",
        ownerUid = OWNER,
        title = "Terapia",
        startDate = monday,
        scheduledTime = null,
        description = null,
        status = TaskStatus.ACTIVE,
        ownerCanComplete = true,
        createdBy = OWNER,
        rule = RepeatRule.Weekly(setOf(DayOfWeek.MONDAY)),
        skippedDates = skipped
    )

    private fun plainDraft(date: LocalDate = monday) = ActivityDraft("Terapia", date, null, null, true, false)

    private suspend fun dayOf(date: LocalDate): List<RoutineTask> {
        tasks.routine.value = DataResult.Success(RoutineSnapshot(emptyList(), false, false))
        val result = repository.observeDay(OWNER, RoutineViewer.Owner, date).first()
        return (result as DataResult.Success).value.tasks
    }

    @Test
    fun `a repeating activity shows on its days only, and never before its first day`() = runTest {
        rules.series.value = DataResult.Success(listOf(everyMonday()))

        assertEquals(listOf("s1__2026-11-09"), dayOf(monday.plusWeeks(1)).map { it.id })
        assertTrue(dayOf(monday.plusDays(1)).isEmpty())
        assertTrue(dayOf(monday.minusWeeks(1)).isEmpty())
    }

    @Test
    fun `a skipped day does not show`() = runTest {
        rules.series.value = DataResult.Success(listOf(everyMonday(skipped = setOf(monday.plusWeeks(1)))))

        assertTrue(dayOf(monday.plusWeeks(1)).isEmpty())
        assertEquals(1, dayOf(monday.plusWeeks(2)).size)
    }

    @Test
    fun `a day with its own document is shown once, with its completion and the series data`() = runTest {
        val series = everyMonday()
        rules.series.value = DataResult.Success(listOf(series))
        val stored = series.occurrenceOn(monday).copy(title = "Título antigo", isDone = true, repeat = null)
        tasks.routine.value = DataResult.Success(RoutineSnapshot(listOf(stored), false, false))

        val day = (repository.observeDay(OWNER, RoutineViewer.Owner, monday).first() as DataResult.Success).value.tasks

        assertEquals(1, day.size)
        assertTrue(day.single().isDone)
        assertEquals("Terapia", day.single().title)
        assertTrue(day.single().repeat!!.isStored)
    }

    @Test
    fun `after a series is deleted only its days marked as done stay, as history`() = runTest {
        rules.series.value = DataResult.Success(emptyList())
        val plain = everyMonday().occurrenceOn(monday).copy(id = "plain", repeat = null)
        // Marked and then unmarked: it has a document, but it is not history.
        val unmarked = everyMonday().occurrenceOn(monday).copy(repeat = null)
        val done = everyMonday().copy(id = "s2").occurrenceOn(monday).copy(isDone = true, repeat = null)
        tasks.routine.value = DataResult.Success(RoutineSnapshot(listOf(plain, unmarked, done), false, false))
        tasks.dayResult = DataResult.Success(listOf(plain, unmarked, done))

        val day = (repository.observeDay(OWNER, RoutineViewer.Owner, monday).first() as DataResult.Success).value.tasks
        val days = (repository.getDays(OWNER, RoutineViewer.Owner, listOf(monday)) as DataResult.Success).value

        assertEquals(setOf("plain", "s2__2026-11-02"), day.map { it.id }.toSet())
        assertEquals(setOf("plain", "s2__2026-11-02"), days.getValue(monday).map { it.id }.toSet())
    }

    @Test
    fun `when the rules cannot be read the day still shows, with a notice`() = runTest {
        rules.series.value = DataResult.Failure(DataError.PERMISSION_DENIED)
        tasks.routine.value = DataResult.Success(RoutineSnapshot(emptyList(), false, false))

        val day = repository.observeDay(OWNER, RoutineViewer.Owner, monday).first() as DataResult.Success

        assertTrue(day.value.isRepeatingUnavailable)
    }

    @Test
    fun `a failure of the day itself is not hidden`() = runTest {
        tasks.routine.value = DataResult.Failure(DataError.PERMISSION_DENIED)

        assertTrue(repository.observeDay(OWNER, RoutineViewer.Owner, monday).first() is DataResult.Failure)
    }

    @Test
    fun `marking a day for the first time creates its document, then completes it`() = runTest {
        val occurrence = everyMonday().occurrenceOn(monday)

        repository.setDone(OWNER, occurrence, isDone = true)

        assertEquals(listOf("create"), tasks.writes.map { it.operation })
        assertEquals(occurrence.id, tasks.writes.single().activityId)
        assertEquals(listOf(Triple(OWNER, occurrence.id, true)), tasks.setDoneCalls)
    }

    @Test
    fun `marking a day that already has a document only updates it`() = runTest {
        repository.setDone(OWNER, everyMonday().occurrenceOn(monday, isDone = true, stored = true), isDone = false)

        assertTrue(tasks.writes.isEmpty())
        assertEquals(1, tasks.setDoneCalls.size)
    }

    @Test
    fun `saving without a rule creates a plain activity, with a rule creates a series`() = runTest {
        repository.createActivity(OWNER, "a1", plainDraft())
        repository.createActivity(OWNER, "s2", plainDraft().copy(repeat = RepeatRule.EveryDay))

        assertEquals(listOf("create"), tasks.writes.map { it.operation })
        assertEquals(listOf("create s2"), rules.log)
    }

    @Test
    fun `a caregiver reads the repeating activities as a caregiver, which the rules accept`() = runTest {
        val caregiver = RoutineViewer.Caregiver(CareLinkPermissions(viewRoutines = true, manageRoutines = true))

        repository.createActivity(OWNER, "s2", plainDraft().copy(repeat = RepeatRule.EveryDay), caregiver)
        repository.updateActivity(OWNER, "s2", plainDraft().copy(repeat = RepeatRule.EveryDay), caregiver)

        assertEquals(listOf<RoutineViewer>(caregiver, caregiver), rules.viewersAsked)
    }

    @Test
    fun `there is a limit of repeating activities`() = runTest {
        rules.series.value = DataResult.Success(List(MAX_ROUTINE_SERIES) { everyMonday().copy(id = "s$it") })

        val result = repository.createActivity(OWNER, "new", plainDraft().copy(repeat = RepeatRule.EveryDay))

        assertEquals(DataResult.Failure(DataError.PRECONDITION_FAILED), result)
        assertTrue(rules.log.isEmpty())
    }

    @Test
    fun `creating again a repeating activity that is already there updates it, even at the limit`() = runTest {
        rules.series.value = DataResult.Success(List(MAX_ROUTINE_SERIES) { everyMonday().copy(id = "s$it") })

        // A move to the account that was interrupted writes the same series again, with the same id.
        repository.createActivity(OWNER, "s0", plainDraft().copy(repeat = RepeatRule.EveryDay))

        assertEquals(listOf("update s0"), rules.log)
    }

    @Test
    fun `starting or stopping to repeat is one write, so the day never shows the activity twice`() = runTest {
        rules.series.value = DataResult.Success(listOf(everyMonday()))

        repository.updateActivity(OWNER, "s1", plainDraft())
        assertTrue(tasks.writes.isEmpty())
        assertEquals(listOf("series s1 becomes an activity"), rules.log)

        tasks.writes.clear()
        rules.log.clear()
        repository.updateActivity(OWNER, "plain", plainDraft().copy(repeat = RepeatRule.EveryDay))
        assertEquals(listOf("activity plain becomes a series"), rules.log)
        assertTrue(tasks.writes.isEmpty())
    }

    @Test
    fun `deleting one day skips it, deleting all removes the series`() = runTest {
        val occurrence = everyMonday().occurrenceOn(monday)

        repository.delete(OWNER, occurrence, DeleteScope.THIS_DAY)
        assertEquals(listOf("skip 2026-11-02"), rules.log)
        assertTrue(tasks.writes.isEmpty())

        rules.log.clear()
        repository.delete(OWNER, occurrence, DeleteScope.ALL_DAYS)
        assertEquals(listOf("delete s1"), rules.log)
    }

    @Test
    fun `deleting a day that has a document removes the document too`() = runTest {
        repository.delete(OWNER, everyMonday().occurrenceOn(monday, stored = true), DeleteScope.THIS_DAY)

        assertEquals(listOf("skip 2026-11-02"), rules.log)
        assertEquals("delete", tasks.writes.single().operation)
    }

    @Test
    fun `reading many days reads the rules once and includes them`() = runTest {
        rules.series.value = DataResult.Success(listOf(everyMonday()))

        val days = repository.getDays(OWNER, RoutineViewer.Owner, listOf(monday, monday.plusDays(1)))

        val result = (days as DataResult.Success).value
        assertEquals(1, result.getValue(monday).size)
        assertTrue(result.getValue(monday.plusDays(1)).isEmpty())
        assertFalse(result.getValue(monday).single().isDone)
    }

    @Test
    fun `the activity being edited is not its own duplicate`() {
        val series = everyMonday()
        val draft = plainDraft()

        assertEquals(null, validateActivity(draft, listOf(series.occurrenceOn(monday)), editingId = "s1"))
        assertEquals(
            ActivityValidationError.DUPLICATE,
            validateActivity(draft, listOf(series.occurrenceOn(monday)), editingId = "other")
        )
    }
}
