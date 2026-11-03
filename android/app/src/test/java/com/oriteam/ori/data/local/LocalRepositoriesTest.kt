package com.oriteam.ori.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineSnapshot
import com.oriteam.ori.domain.routine.RoutineViewer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** The real database, in memory: proves that the data survives without any network or account. */
@RunWith(AndroidJUnit4::class)
class LocalRepositoriesTest {
    private lateinit var database: OriDatabase
    private val day = LocalDate.of(2026, 11, 2)
    private val draft = ActivityDraft(
        title = " Tomar banho ",
        date = day,
        scheduledTime = LocalTime.of(8, 0),
        description = " ",
        ownerCanComplete = true,
        isHiddenFromCaregiver = false
    )

    @Test
    fun `a repeating activity survives on the device, with the days skipped`() = runTest {
        val repository = LocalRoutineSeriesRepository(database)

        repository.createSeries(LOCAL_OWNER_UID, "s1", draft, RepeatRule.BusinessDays)
        repository.skipDate(LOCAL_OWNER_UID, "s1", day.plusDays(1))
        repository.updateSeries(LOCAL_OWNER_UID, "s1", draft.copy(title = "Banho"), RepeatRule.BusinessDays)

        val stored = (repository.getSeries(LOCAL_OWNER_UID, RoutineViewer.Owner) as DataResult.Success).value.single()
        assertEquals("Banho", stored.title)
        assertEquals(RepeatRule.BusinessDays, stored.rule)
        assertEquals(setOf(day.plusDays(1)), stored.skippedDates)
        assertTrue(stored.occursOn(day))
        assertFalse(stored.occursOn(day.plusDays(1)))

        repository.deleteSeries(LOCAL_OWNER_UID, "s1")
        assertTrue((repository.getSeries(LOCAL_OWNER_UID, RoutineViewer.Owner) as DataResult.Success).value.isEmpty())
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OriDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    private suspend fun LocalRoutineRepository.day(): RoutineSnapshot =
        (observeDay(LOCAL_OWNER_UID, RoutineViewer.Owner, day).first() as DataResult.Success).value

    @Test
    fun `starting and stopping to repeat leaves exactly one form of the activity`() = runTest {
        val tasks = LocalRoutineRepository(database)
        val series = LocalRoutineSeriesRepository(database)
        tasks.createActivity(LOCAL_OWNER_UID, "a1", draft)

        series.replaceActivityWithSeries(LOCAL_OWNER_UID, "a1", draft, RepeatRule.EveryDay)
        assertTrue((tasks.getDay(LOCAL_OWNER_UID, RoutineViewer.Owner, day) as DataResult.Success).value.isEmpty())
        assertEquals(
            listOf("a1"),
            (series.getSeries(LOCAL_OWNER_UID, RoutineViewer.Owner) as DataResult.Success).value.map { it.id }
        )

        series.replaceSeriesWithActivity(LOCAL_OWNER_UID, "a1", draft)
        assertTrue((series.getSeries(LOCAL_OWNER_UID, RoutineViewer.Owner) as DataResult.Success).value.isEmpty())
        assertEquals(
            listOf("a1"),
            (tasks.getDay(LOCAL_OWNER_UID, RoutineViewer.Owner, day) as DataResult.Success).value.map { it.id }
        )
    }

    @Test
    fun `an activity is saved, completed, edited and deleted without pending writes`() = runTest {
        val repository = LocalRoutineRepository(database)
        val id = repository.newActivityId(LOCAL_OWNER_UID)

        val created = repository.createActivity(LOCAL_OWNER_UID, id, draft)
        assertEquals(DataResult.Success(WriteStatus.SYNCED), created)

        val task = repository.day().tasks.single()
        assertEquals("Tomar banho", task.title)
        assertNull(task.description)
        assertTrue(task.isCreatedByOwner)
        assertFalse(repository.day().isFromCache)

        repository.setTaskDone(LOCAL_OWNER_UID, id, true)
        repository.updateActivity(LOCAL_OWNER_UID, id, draft.copy(title = "Banho"))
        val edited = repository.day().tasks.single()
        assertEquals("Banho", edited.title)
        assertTrue("an edit keeps the completion", edited.isDone)

        repository.deleteActivity(LOCAL_OWNER_UID, id)
        assertTrue(repository.day().tasks.isEmpty())
    }

    @Test
    fun `creating twice with the same id stores one activity`() = runTest {
        val repository = LocalRoutineRepository(database)

        repository.createActivity(LOCAL_OWNER_UID, "same", draft)
        repository.createActivity(LOCAL_OWNER_UID, "same", draft)

        assertEquals(1, repository.day().tasks.size)
    }

    @Test
    fun `deleting a board removes its symbols`() = runTest {
        val repository = LocalAacBoardRepository(database)
        repository.createBoardWithSymbols(
            LOCAL_OWNER_UID,
            "Básico",
            0,
            listOf(SymbolDraft("Água", null, SymbolImage.Emoji("💧")))
        )
        val board = (repository.getBoards(LOCAL_OWNER_UID) as DataResult.Success).value.items.single()
        val symbol = (repository.getSymbols(LOCAL_OWNER_UID, board.id) as DataResult.Success).value.single()
        assertEquals(SymbolImage.Emoji("💧"), symbol.image)

        repository.deleteBoard(LOCAL_OWNER_UID, board.id)

        assertEquals(DataResult.Success(emptyList<Any>()), repository.getSymbols(LOCAL_OWNER_UID, board.id))
    }

    @Test
    fun `saved messages come newest first`() = runTest {
        var now = 1L
        val clock = object : Clock() {
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: java.time.ZoneId?) = this
            override fun instant(): Instant = Instant.ofEpochMilli(now++)
        }
        val repository = LocalSavedMessageRepository(database, clock)
        repository.saveMessage(LOCAL_OWNER_UID, "primeira")
        repository.saveMessage(LOCAL_OWNER_UID, "segunda")

        val messages = (repository.observeSavedMessages(LOCAL_OWNER_UID).first() as DataResult.Success).value

        assertEquals(listOf("segunda", "primeira"), messages.map { it.text })
    }

    @Test
    fun `speech is counted per day and within the period`() = runTest {
        val usage = RoomAacUsageRepository(database)
        usage.recordCommunication(day)
        usage.recordCommunication(day)
        usage.recordCommunication(day.plusDays(5))

        assertEquals(2, usage.countBetween(day, day.plusDays(1)))
        assertEquals(3, usage.countBetween(day, day.plusDays(5)))
    }

    @Test
    fun `erasing leaves nothing behind and the snapshot sees everything before it`() = runTest {
        val store = RoomLocalDataStore(database)
        LocalRoutineRepository(database).createActivity(LOCAL_OWNER_UID, "t", draft)
        LocalAacBoardRepository(database).createBoard(LOCAL_OWNER_UID, "Básico", 0)
        assertFalse(store.snapshot().isEmpty)

        store.eraseAll()

        assertTrue(store.snapshot().isEmpty)
    }
}
