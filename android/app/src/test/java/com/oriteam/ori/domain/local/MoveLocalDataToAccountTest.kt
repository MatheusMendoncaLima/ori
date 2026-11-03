package com.oriteam.ori.domain.local

import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.ListSnapshot
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.model.TaskStatus
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineSeries
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.occurrenceIdOf
import com.oriteam.ori.testing.FakeAacBoardRepository
import com.oriteam.ori.testing.FakeLocalDataStore
import com.oriteam.ori.testing.FakePhotoStore
import com.oriteam.ori.testing.FakeRoutineRepository
import com.oriteam.ori.testing.FakeSavedMessageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MoveLocalDataToAccountTest {
    private val day = LocalDate.of(2026, 11, 2)
    private val routine = FakeRoutineRepository()
    private val boards = FakeAacBoardRepository()
    private val messages = FakeSavedMessageRepository()
    private val photos = FakePhotoStore()

    private fun task(id: String, isDone: Boolean = false) = RoutineTask(
        id = id,
        ownerUid = LOCAL_OWNER_UID,
        title = "Tomar remédio",
        date = day,
        scheduledTime = null,
        description = null,
        status = TaskStatus.ACTIVE,
        ownerCanComplete = true,
        isDone = isDone,
        createdBy = LOCAL_OWNER_UID
    )

    private fun board(name: String = "Básico", vararg labels: String) = LocalBoard(
        AacBoard("board-$name", name, 0, LOCAL_OWNER_UID),
        labels.map { AacSymbol("symbol-$it", "board-$name", it, null, null, 0, LOCAL_OWNER_UID) }
    )

    private fun move(store: FakeLocalDataStore) = MoveLocalDataToAccount(store, routine, boards, messages, photos)

    @Test
    fun `nothing on the device needs no network`() = runTest {
        routine.writeResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)

        assertEquals(LocalDataMoveResult.COMPLETED, move(FakeLocalDataStore())("account"))
        assertTrue(routine.writes.isEmpty())
    }

    @Test
    fun `an activity keeps its id and completion and leaves the device only after confirmation`() = runTest {
        val store = FakeLocalDataStore(LocalDataSnapshot(tasks = listOf(task("t1", isDone = true))))

        assertEquals(LocalDataMoveResult.COMPLETED, move(store)("account"))

        assertEquals(listOf("create"), routine.writes.map { it.operation })
        assertEquals(listOf(Triple("account", "t1", true)), routine.setDoneCalls)
        assertTrue(routine.writes.all { it.ownerUid == "account" && it.activityId == "t1" })
        assertTrue(store.data.isEmpty)
    }

    @Test
    fun `a repeating activity keeps its id, rule and skipped days, and leaves only after confirmation`() = runTest {
        val skipped = day.plusWeeks(1)
        val series = RoutineSeries(
            id = "s1",
            ownerUid = LOCAL_OWNER_UID,
            title = "Terapia",
            startDate = day,
            scheduledTime = null,
            description = null,
            status = TaskStatus.ACTIVE,
            ownerCanComplete = true,
            createdBy = LOCAL_OWNER_UID,
            rule = RepeatRule.EveryDay,
            skippedDates = setOf(skipped)
        )
        val store = FakeLocalDataStore(LocalDataSnapshot(series = listOf(series)))

        routine.writeResult = DataResult.Success(WriteStatus.PENDING_SYNC)
        assertEquals(LocalDataMoveResult.WAITING_FOR_CONNECTION, move(store)("account"))
        assertEquals(listOf(series), store.data.series)

        routine.writes.clear()
        routine.writeResult = DataResult.Success(WriteStatus.SYNCED)
        assertEquals(LocalDataMoveResult.COMPLETED, move(store)("account"))

        assertEquals("s1", routine.writes.first().activityId)
        assertEquals(RepeatRule.EveryDay, routine.writes.first().draft?.repeat)
        assertEquals(occurrenceIdOf("s1", skipped), routine.writes.last().activityId)
        assertTrue(store.data.isEmpty)
    }

    @Test
    fun `a photo goes to the account before its symbol, and waits with it`() = runTest {
        val picture = SymbolImage.DevicePhoto("a.jpg")
        val local = LocalBoard(
            AacBoard("b", "Básico", 0, LOCAL_OWNER_UID),
            listOf(AacSymbol("s", "b", "Mãe", null, picture, 0, LOCAL_OWNER_UID))
        )
        val store = FakeLocalDataStore(LocalDataSnapshot(boards = listOf(local)))

        photos.uploadResult = DataResult.Success(WriteStatus.PENDING_SYNC)
        assertEquals(LocalDataMoveResult.WAITING_FOR_CONNECTION, move(store)("account"))
        assertEquals(listOf(local), store.data.boards)

        photos.uploadResult = DataResult.Success(WriteStatus.SYNCED)
        assertEquals(LocalDataMoveResult.COMPLETED, move(store)("account"))
        assertEquals("account" to picture.photo, photos.uploaded.last())
        assertTrue(store.data.isEmpty)
    }

    @Test
    fun `over the limit of photos the symbol moves and the person is told`() = runTest {
        val local = LocalBoard(
            AacBoard("b", "Básico", 0, LOCAL_OWNER_UID),
            listOf(AacSymbol("s", "b", "Mãe", null, SymbolImage.DevicePhoto("a.jpg"), 0, LOCAL_OWNER_UID))
        )
        photos.uploadResult = DataResult.Failure(DataError.PRECONDITION_FAILED)

        val store = FakeLocalDataStore(LocalDataSnapshot(boards = listOf(local)))

        assertEquals(LocalDataMoveResult.SOME_ITEMS_DID_NOT_FIT, move(store)("account"))
    }

    @Test
    fun `a write that is only pending keeps the item on the device for the next attempt`() = runTest {
        routine.writeResult = DataResult.Success(WriteStatus.PENDING_SYNC)
        val store = FakeLocalDataStore(LocalDataSnapshot(tasks = listOf(task("t1"))))

        assertEquals(LocalDataMoveResult.WAITING_FOR_CONNECTION, move(store)("account"))
        assertEquals(1, store.data.tasks.size)
    }

    @Test
    fun `a rejected write does not lose the item`() = runTest {
        routine.writeResult = DataResult.Failure(DataError.PERMISSION_DENIED)
        val store = FakeLocalDataStore(LocalDataSnapshot(tasks = listOf(task("t1"))))

        assertEquals(LocalDataMoveResult.WAITING_FOR_CONNECTION, move(store)("account"))
        assertEquals(1, store.data.tasks.size)
    }

    @Test
    fun `an activity the account already has is not created twice`() = runTest {
        routine.dayResult = DataResult.Success(listOf(task("in-account").copy(ownerUid = "account")))
        val store = FakeLocalDataStore(LocalDataSnapshot(tasks = listOf(task("t1"))))

        assertEquals(LocalDataMoveResult.COMPLETED, move(store)("account"))

        assertTrue(routine.writes.isEmpty())
        assertTrue(store.data.tasks.isEmpty())
    }

    @Test
    fun `a new board goes whole with its symbols`() = runTest {
        boards.boards.value = DataResult.Success(ListSnapshot(emptyList()))
        val store = FakeLocalDataStore(LocalDataSnapshot(boards = listOf(board("Básico", "Água", "Sim"))))

        assertEquals(LocalDataMoveResult.COMPLETED, move(store)("account"))

        assertEquals(1, boards.writes.size)
        assertTrue(store.data.boards.isEmpty())
    }

    @Test
    fun `a board with the same name is merged, adding only the missing symbols`() = runTest {
        val existing = AacBoard("remote", "básico", 0, "account")
        boards.boards.value = DataResult.Success(ListSnapshot(listOf(existing)))
        boards.symbolsResult = DataResult.Success(
            listOf(AacSymbol("r1", "remote", "Água", null, null, 0, "account"))
        )
        val store = FakeLocalDataStore(LocalDataSnapshot(boards = listOf(board("Básico", "Água", "Sim"))))

        assertEquals(LocalDataMoveResult.COMPLETED, move(store)("account"))

        assertEquals(1, boards.writes.size)
        assertEquals("symbol-Sim", boards.writes.single().target)
        assertTrue(store.data.boards.isEmpty())
    }

    @Test
    fun `boards read from the cache wait, because they could hide what the account has`() = runTest {
        boards.boards.value = DataResult.Success(ListSnapshot(emptyList(), isFromCache = true))
        val store = FakeLocalDataStore(LocalDataSnapshot(boards = listOf(board("Básico", "Água"))))

        assertEquals(LocalDataMoveResult.WAITING_FOR_CONNECTION, move(store)("account"))

        assertTrue(boards.writes.isEmpty())
        assertFalse(store.data.boards.isEmpty())
    }

    @Test
    fun `a saved message moves once`() = runTest {
        messages.messages.value = DataResult.Success(listOf(SavedMessage("r", "Oi")))
        val store = FakeLocalDataStore(
            LocalDataSnapshot(savedMessages = listOf(SavedMessage("1", "Oi"), SavedMessage("2", "Tchau")))
        )

        assertEquals(LocalDataMoveResult.COMPLETED, move(store)("account"))

        assertEquals(listOf("Tchau"), messages.saved)
        assertTrue(store.data.savedMessages.isEmpty())
    }
}
