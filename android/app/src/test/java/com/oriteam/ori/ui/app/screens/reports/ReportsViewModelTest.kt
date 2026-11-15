package com.oriteam.ori.ui.app.screens.reports

import com.oriteam.ori.R
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.reports.ReportPeriod
import com.oriteam.ori.testing.FakeAacUsageRepository
import com.oriteam.ori.testing.FakeRoutineRepository
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.OWNER
import com.oriteam.ori.testing.activity
import com.oriteam.ori.ui.components.state.ContentState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class ReportsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Wednesday, November 11: the week started on Sunday the 8th, the month on the 1st.
    private val today = LocalDate.of(2026, 11, 11)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val routine = FakeRoutineRepository()
    private val usage = FakeAacUsageRepository()
    private val viewModel = ReportsViewModel(routine, usage, clock)

    private val content get() = (viewModel.uiState.value.report as ContentState.Content).value

    @Test
    fun `the month is read in one go, with what was done and the communication`() = runTest {
        routine.dayResult = DataResult.Success(listOf(activity("a", isDone = true), activity("b")))
        usage.recordCommunication(today)

        viewModel.show(OWNER)

        assertEquals(listOf((1..11).map { LocalDate.of(2026, 11, it) }), routine.periodReads)
        assertEquals(11, content.done)
        assertEquals(22, content.total)
        assertEquals(1, content.speechCount)
    }

    @Test
    fun `nothing planned and nothing said is empty, not a report of zeros`() = runTest {
        viewModel.show(OWNER)

        assertEquals(ContentState.Empty, viewModel.uiState.value.report)
    }

    @Test
    fun `a failed read is an error that can be retried`() = runTest {
        routine.dayResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        viewModel.show(OWNER)
        assertEquals(
            R.string.error_network_unavailable,
            (viewModel.uiState.value.report as ContentState.Error).messageRes
        )

        routine.dayResult = DataResult.Success(listOf(activity("a", isDone = true)))
        viewModel.onRetry()
        assertEquals("one done on each of the 11 days", 11, content.done)
    }

    @Test
    fun `changing the period reads its days, and choosing the same one reads nothing`() = runTest {
        routine.dayResult = DataResult.Success(listOf(activity("a", isDone = true)))
        viewModel.show(OWNER)
        viewModel.onPeriodChange(ReportPeriod.MONTH)
        assertEquals(1, routine.periodReads.size)

        viewModel.onPeriodChange(ReportPeriod.WEEK)

        assertEquals(ReportPeriod.WEEK, viewModel.uiState.value.period)
        assertEquals((8..11).map { LocalDate.of(2026, 11, it) }, routine.periodReads.last())
        assertEquals(4, content.done)
    }

    @Test
    fun `coming back to the screen reads again, so what was marked meanwhile counts`() = runTest {
        routine.dayResult = DataResult.Success(listOf(activity("a")))
        viewModel.show(OWNER)
        routine.dayResult = DataResult.Success(listOf(activity("a", isDone = true)))

        viewModel.show(OWNER)

        assertEquals(2, routine.periodReads.size)
        assertTrue(content.done > 0)
    }
}
