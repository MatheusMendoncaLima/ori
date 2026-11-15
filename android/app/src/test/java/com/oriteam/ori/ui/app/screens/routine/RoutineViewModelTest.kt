package com.oriteam.ori.ui.app.screens.routine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.routine.MAX_ROUTINE_TASKS
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.testing.DAY
import com.oriteam.ori.testing.FakeRoutineRepository
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.OWNER
import com.oriteam.ori.testing.activity
import com.oriteam.ori.testing.fixedClock
import com.oriteam.ori.testing.snapshotOf
import com.oriteam.ori.testing.viewer
import com.oriteam.ori.ui.app.screens.common.LOADING_GRACE_MILLIS
import com.oriteam.ori.ui.app.screens.common.SYNC_NOTICE_DELAY_MILLIS
import com.oriteam.ori.ui.components.state.ContentState
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

class RoutineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val routines = FakeRoutineRepository()

    /** Creates the ViewModel and collects its state, like a visible screen. */
    private fun TestScope.routineViewModel(
        viewer: RoutineViewer = RoutineViewer.Owner,
        savedState: SavedStateHandle = SavedStateHandle()
    ) = RoutineViewModel(routines, fixedClock, savedState).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.show(OWNER, viewer)
    }

    private fun RoutineViewModel.steps() = (uiState.value.content as ContentState.Content).value

    @Test
    fun `the open month marks the days with activities, and the week marks none`() = runTest {
        routines.dayResult = DataResult.Success(listOf(activity("a")))
        val viewModel = routineViewModel()
        assertTrue(viewModel.uiState.value.daysWithActivities.isEmpty())

        viewModel.onMonthShown(YearMonth.from(DAY))
        advanceUntilIdle()
        assertTrue(DAY in viewModel.uiState.value.daysWithActivities)

        viewModel.onMonthShown(null)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.daysWithActivities.isEmpty())
    }

    @Test
    fun `choosing another day of the open month does not read the month again`() = runTest {
        routines.routineByDay = mapOf(
            DAY to snapshotOf(activity("a")),
            DAY.plusDays(1) to snapshotOf(activity("b", date = DAY.plusDays(1))),
            DAY.minusDays(1) to snapshotOf()
        )
        val viewModel = routineViewModel()
        viewModel.onMonthShown(YearMonth.from(DAY))
        advanceUntilIdle()

        viewModel.onSelectDate(DAY.plusDays(1))
        advanceUntilIdle()
        viewModel.onSelectDate(DAY.minusDays(1))
        advanceUntilIdle()
        assertEquals(1, routines.periodReads.size)

        // Another month is another read.
        viewModel.onMonthShown(YearMonth.from(DAY).plusMonths(1))
        advanceUntilIdle()
        assertEquals(2, routines.periodReads.size)
    }

    @Test
    fun `an activity added to the chosen day reads the open month again`() = runTest {
        routines.routine.value = snapshotOf(activity("a"))
        val viewModel = routineViewModel()
        viewModel.onMonthShown(YearMonth.from(DAY))
        advanceUntilIdle()

        // A repeating one may fall on other days of the month too, so the whole month is read.
        routines.routine.value = snapshotOf(activity("a"), activity("b"))
        advanceUntilIdle()

        assertEquals(2, routines.periodReads.size)
    }

    @Test
    fun `another day replaces the list without passing through Carregando`() = runTest {
        routines.routineByDay = mapOf(
            DAY to snapshotOf(activity("a")),
            DAY.plusDays(1) to snapshotOf(activity("b", date = DAY.plusDays(1)))
        )
        val viewModel = routineViewModel()
        val shown = mutableListOf<ContentState<*>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { shown += it.content }
        }
        advanceUntilIdle()
        shown.clear()

        // A read from the device cache takes a few milliseconds: long enough to flash "Carregando".
        routines.dayReadMillis = 40
        viewModel.onSelectDate(DAY.plusDays(1))
        advanceUntilIdle()

        assertTrue(shown.none { it == ContentState.Loading })
        assertEquals(listOf("b"), viewModel.steps().map { it.id })
    }

    @Test
    fun `a day that takes long says Carregando after a moment, and the date changes at once`() = runTest {
        routines.routine.value = snapshotOf(activity("a"))
        val viewModel = routineViewModel()
        routines.routine.value = null

        viewModel.onSelectDate(DAY.plusDays(1))
        assertEquals(DAY.plusDays(1), viewModel.uiState.value.date)
        advanceTimeBy(LOADING_GRACE_MILLIS - 1)
        assertTrue(viewModel.uiState.value.content is ContentState.Content)

        advanceTimeBy(2)
        assertEquals(ContentState.Loading, viewModel.uiState.value.content)
    }

    @Test
    fun `opens on today and queries one day at a time`() = runTest {
        val viewModel = routineViewModel()

        viewModel.onSelectDate(viewModel.uiState.value.date.plusDays(1))
        viewModel.onSelectDate(viewModel.uiState.value.date.plusDays(1))
        viewModel.onSelectDate(viewModel.uiState.value.date.minusDays(1))
        viewModel.onToday()

        assertEquals(listOf(DAY, DAY.plusDays(1), DAY.plusDays(2), DAY.plusDays(1), DAY), routines.observedDays)
        assertEquals(0L, viewModel.uiState.value.daysFromToday)
    }

    @Test
    fun `the selected day survives an app restart`() = runTest {
        val savedState = SavedStateHandle()
        routineViewModel(savedState = savedState).run { onSelectDate(uiState.value.date.plusDays(1)) }

        // A new ViewModel with the same saved state is what Android creates after process death.
        val restored = routineViewModel(savedState = savedState)

        assertEquals(DAY.plusDays(1), restored.uiState.value.date)
        assertEquals(DAY.plusDays(1), routines.observedDays.last())
    }

    @Test
    fun `activities are shown in day order with the actions each one allows`() = runTest {
        val viewModel = routineViewModel()

        routines.routine.value = snapshotOf(
            activity("reserved", time = java.time.LocalTime.of(9, 0), ownerCanComplete = false),
            activity("own", time = java.time.LocalTime.of(7, 0), createdBy = OWNER)
        )

        val (own, reserved) = viewModel.steps()
        assertEquals(listOf("own", "reserved"), viewModel.steps().map { it.id })
        assertTrue(own.canEdit && own.canDelete && own.canToggle)
        assertFalse(reserved.canEdit || reserved.canDelete || reserved.canToggle)
        assertEquals(listOf(ActivityBadge.SET_BY_CAREGIVER), reserved.badges)
        assertTrue(viewModel.uiState.value.canCreate)
    }

    @Test
    fun `a view-only caregiver cannot create and sees no edit actions`() = runTest {
        val viewModel = routineViewModel(viewer = viewer)
        routines.routine.value = snapshotOf(activity("a"))

        assertFalse(viewModel.uiState.value.canCreate)
        assertFalse(viewModel.steps().single().canEdit)
    }

    @Test
    fun `a network failure while reading shows an error and retry reads again`() = runTest {
        val viewModel = routineViewModel()
        routines.routine.value = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        assertEquals(ContentState.Error(R.string.error_network_unavailable), viewModel.uiState.value.content)

        routines.routine.value = snapshotOf(activity("a"))
        viewModel.onRetry()

        assertEquals(listOf("a"), viewModel.steps().map { it.id })
    }

    @Test
    fun `offline data and unsent changes are announced when they last`() = runTest {
        val viewModel = routineViewModel()

        routines.routine.value = snapshotOf(activity("a"), isFromCache = true)
        advanceTimeBy(SYNC_NOTICE_DELAY_MILLIS + 1)
        assertEquals(R.string.sync_showing_saved_data, viewModel.uiState.value.syncNoticeRes)

        routines.routine.value = snapshotOf(activity("a", isDone = true), hasPendingWrites = true)
        advanceTimeBy(SYNC_NOTICE_DELAY_MILLIS + 1)
        assertEquals(R.string.sync_pending_changes, viewModel.uiState.value.syncNoticeRes)
    }

    @Test
    fun `online, a change confirmed by the server never flashes a notice, and the data is not held back`() =
        runTest {
            val viewModel = routineViewModel()
            routines.routine.value = snapshotOf(activity("a"))

            routines.routine.value = snapshotOf(activity("a", isDone = true), hasPendingWrites = true)
            assertEquals(null, viewModel.uiState.value.syncNoticeRes)
            assertTrue("the change shows at once", viewModel.steps().single().isDone)

            advanceTimeBy(SYNC_NOTICE_DELAY_MILLIS / 3)
            routines.routine.value = snapshotOf(activity("a", isDone = true))
            advanceTimeBy(SYNC_NOTICE_DELAY_MILLIS * 2)

            assertEquals(null, viewModel.uiState.value.syncNoticeRes)
        }

    @Test
    fun `screens about today move to the new day at midnight`() = runTest {
        var now = LocalDateTime.of(2026, 11, 2, 23, 59, 30)
        val clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC
            override fun withZone(zone: ZoneId?): Clock = this
            override fun instant(): Instant = now.toInstant(ZoneOffset.UTC)
        }
        val viewModel = RoutineViewModel(routines, clock, SavedStateHandle()).also {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() }
            it.show(OWNER, RoutineViewer.Owner)
        }

        viewModel.followToday()
        assertEquals(LocalDate.of(2026, 11, 2), viewModel.uiState.value.date)

        now = LocalDateTime.of(2026, 11, 3, 0, 0, 1)
        advanceTimeBy(31_000)
        assertEquals(LocalDate.of(2026, 11, 3), viewModel.uiState.value.date)
        // The wait for the next midnight never ends by itself, as on a screen left open.
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `a full day is flagged as possibly truncated`() = runTest {
        val viewModel = routineViewModel()

        routines.routine.value = snapshotOf(*Array(MAX_ROUTINE_TASKS) { activity("t$it") })

        assertTrue(viewModel.uiState.value.isTruncated)
    }

    @Test
    fun `completion is only sent when allowed and failures are explained`() = runTest {
        val viewModel = routineViewModel()
        routines.routine.value = snapshotOf(activity("reserved", ownerCanComplete = false), activity("ok"))
        routines.setDoneResult = DataResult.Failure(DataError.PERMISSION_DENIED)

        viewModel.onToggleDone("reserved")
        viewModel.onToggleDone("ok")

        assertEquals(listOf(Triple(OWNER, "ok", true)), routines.setDoneCalls)
        assertEquals(R.string.error_permission_denied, viewModel.uiState.value.actionErrorRes)
    }

    @Test
    fun `deletion needs confirmation and permission`() = runTest {
        val viewModel = routineViewModel()
        routines.routine.value = snapshotOf(activity("own", createdBy = OWNER), activity("reserved"))

        viewModel.onDeleteRequest("own")
        assertEquals("own", viewModel.uiState.value.pendingDeletion?.id)
        assertTrue(routines.writes.isEmpty())
        viewModel.onDeleteConfirm()

        viewModel.onDeleteRequest("reserved")
        viewModel.onDeleteConfirm()

        assertEquals(listOf("delete" to "own"), routines.writes.map { it.operation to it.activityId })
        assertNull(viewModel.uiState.value.pendingDeletion)
    }
}
