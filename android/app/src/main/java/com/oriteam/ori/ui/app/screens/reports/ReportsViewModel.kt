package com.oriteam.ori.ui.app.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.aac.AacUsageRepository
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.reports.ReportPeriod
import com.oriteam.ori.domain.reports.RoutineReport
import com.oriteam.ori.domain.reports.dayResultOf
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.ui.app.screens.common.messageRes
import com.oriteam.ori.ui.components.state.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val report: ContentState<RoutineReport> = ContentState.Loading
)

/**
 * Computes the report from the days themselves, so there is no second copy of the data to keep
 * right. The days of the period come in one read over the date range (see
 * `FirestoreRoutineRepository.getDays`), plus one read of the repeating activities; offline they come
 * from the device.
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val routine: RoutineRepository,
    private val usage: AacUsageRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private var ownerUid: String? = null
    private var loading: Job? = null

    /**
     * Called each time the screen appears, so the report includes what was marked since the last time.
     * The report already on screen stays there while the new one is read.
     */
    fun show(ownerUid: String) {
        val isSameOwner = this.ownerUid == ownerUid
        this.ownerUid = ownerUid
        load(keepsShownReport = isSameOwner)
    }

    /** Another period: the numbers on screen belong to the old one, so they give way to "Carregando". */
    fun onPeriodChange(period: ReportPeriod) {
        if (period == _uiState.value.period) return
        _uiState.update { it.copy(period = period) }
        load(keepsShownReport = false)
    }

    fun onRetry() = load(keepsShownReport = false)

    private fun load(keepsShownReport: Boolean) {
        val owner = ownerUid ?: return
        val period = _uiState.value.period
        loading?.cancel()
        loading = viewModelScope.launch {
            if (!keepsShownReport || _uiState.value.report !is ContentState.Content) {
                _uiState.update { it.copy(report = ContentState.Loading) }
            }
            _uiState.update { it.copy(report = reportFor(owner, period.daysUntil(LocalDate.now(clock)))) }
        }
    }

    private suspend fun reportFor(owner: String, days: List<LocalDate>): ContentState<RoutineReport> {
        val results = when (val read = routine.getDays(owner, RoutineViewer.Owner, days)) {
            is DataResult.Failure -> return ContentState.Error(read.error.messageRes())
            is DataResult.Success -> read.value
        }
        val report = RoutineReport(
            days = days.map { day -> dayResultOf(day, results[day].orEmpty()) },
            speechCount = usage.countBetween(days.first(), days.last())
        )
        return if (report.isEmpty) ContentState.Empty else ContentState.Content(report)
    }
}
