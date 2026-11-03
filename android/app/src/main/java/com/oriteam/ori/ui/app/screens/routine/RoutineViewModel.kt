package com.oriteam.ori.ui.app.screens.routine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.routine.DeleteScope
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineSnapshot
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.domain.routine.canCreateActivity
import com.oriteam.ori.domain.routine.canDeleteActivity
import com.oriteam.ori.domain.routine.canToggleCompletion
import com.oriteam.ori.domain.routine.forDay
import com.oriteam.ori.ui.app.screens.common.messageRes
import com.oriteam.ori.ui.app.screens.common.withLastingSyncNotice
import com.oriteam.ori.ui.components.state.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Keeps the Firestore listener alive briefly across rotation, but not while the app is in background. */
private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val DATE_KEY = "routine_date"

/** One person's routine for one day, as seen by its owner or by a linked caregiver. */
@Suppress("TooManyFunctions") // One public function per user event of a single screen.
@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private data class Request(val ownerUid: String, val viewer: RoutineViewer, val attempt: Int = 0)

    /** Interaction state that does not come from Firestore. */
    private data class Interaction(val actionErrorRes: Int? = null, val pendingDeletionId: String? = null)

    /** The activities of the day as read, kept with what the screen shows of them, so actions find them. */
    private data class Day(val tasks: Map<String, RoutineTask>, val state: RoutineUiState)

    private val request = MutableStateFlow<Request?>(null)
    private val date = MutableStateFlow(savedStateHandle.get<String>(DATE_KEY)?.let(LocalDate::parse) ?: today())
    private val interaction = MutableStateFlow(Interaction())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val day: StateFlow<Day?> = combine(request.filterNotNull(), date) { current, day -> current to day }
        .flatMapLatest { (current, day) ->
            routineRepository.observeDay(current.ownerUid, current.viewer, day)
                .map { result -> toDay(result, current.viewer, day) }
                .onStart { emit(Day(emptyMap(), baseState(current.viewer, day))) }
        }
        .withLastingSyncNotice(
            notice = { it.state.syncNoticeRes },
            withoutNotice = { it.copy(state = it.state.copy(syncNoticeRes = null)) }
        )
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val latestTasks: Map<String, RoutineTask> get() = day.value?.tasks.orEmpty()

    /** The month on screen, or null while the week is shown. */
    private val shownMonth = MutableStateFlow<YearMonth?>(null)

    /** How many times the activities of the chosen day changed while it stayed chosen. */
    private data class DayEdits(val date: LocalDate? = null, val ids: Set<String> = emptySet(), val count: Int = 0) {
        fun after(date: LocalDate, ids: Set<String>) =
            DayEdits(date, ids, if (date == this.date && ids != this.ids) count + 1 else count)
    }

    /**
     * Changes when an activity is added to or removed from the chosen day. Choosing another day is not a
     * change, and neither is a day still loading: each would read the whole month again for nothing.
     */
    private val chosenDayEdits: Flow<Int> = day.filterNotNull()
        .filter { it.state.content !is ContentState.Loading }
        .scan(DayEdits()) { edits, current -> edits.after(current.state.date, current.tasks.keys) }
        .map { it.count }
        .distinctUntilChanged()

    /**
     * One read of the open month, again when the activities of the chosen day change: a repeating activity
     * added there may fall on other days too. A month that cannot be read shows no marks: they only help to
     * find a day.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthMarks: Flow<Set<LocalDate>> =
        combine(request.filterNotNull(), shownMonth, chosenDayEdits) { current, month, _ -> current to month }
            .mapLatest { (current, month) -> month?.let { datesWithActivities(current, it) }.orEmpty() }
            .onStart { emit(emptySet()) }

    /** The listener runs only while the screen collects this state (lifecycle-aware in the UI). */
    val uiState: StateFlow<RoutineUiState> =
        combine(day.filterNotNull(), interaction, monthMarks) { current, local, marks ->
            current.state.copy(
                actionErrorRes = local.actionErrorRes,
                pendingDeletion = local.pendingDeletionId?.let(current.state::step),
                daysWithActivities = marks
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            RoutineUiState(date = date.value)
        )

    /** Starts (or switches) the routine being shown. Calling it again with the same input does nothing. */
    fun show(ownerUid: String, viewer: RoutineViewer) {
        val current = request.value
        if (current?.ownerUid == ownerUid && current.viewer == viewer) return
        request.value = Request(ownerUid, viewer)
    }

    fun onToday() = selectDate(today())

    private var followingToday: Job? = null

    /**
     * For screens about today (Início, Tarefas): the date moves at midnight while the screen is open.
     * The delay may run late after the device sleeps, so the screen also calls this when it comes back.
     */
    fun followToday() {
        followingToday?.cancel()
        followingToday = viewModelScope.launch {
            while (true) {
                selectDate(today())
                delay(millisUntilTomorrow())
            }
        }
    }

    private fun millisUntilTomorrow(): Long {
        val now = LocalDateTime.now(clock)
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1
    }

    fun onSelectDate(newDate: LocalDate) = selectDate(newDate)

    fun onMonthShown(month: YearMonth?) {
        shownMonth.value = month
    }

    private suspend fun datesWithActivities(current: Request, month: YearMonth): Set<LocalDate> {
        val days = routineRepository.getDays(current.ownerUid, current.viewer, daysShownIn(month))
        return (days as? DataResult.Success)?.value.orEmpty().filterValues { it.forDay().isNotEmpty() }.keys
    }

    fun onRetry() {
        request.update { it?.copy(attempt = it.attempt + 1) }
    }

    fun onToggleDone(taskId: String) {
        val current = request.value ?: return
        val task = latestTasks[taskId] ?: return
        if (!canToggleCompletion(task, current.viewer)) return
        runWrite { routineRepository.setDone(current.ownerUid, task, isDone = !task.isDone) }
    }

    fun onDeleteRequest(taskId: String) {
        interaction.update { it.copy(pendingDeletionId = taskId) }
    }

    fun onDeleteDismiss() {
        interaction.update { it.copy(pendingDeletionId = null) }
    }

    /** Deletion always passes through a confirmation dialog because it cannot be undone. */
    fun onDeleteConfirm(scope: DeleteScope = DeleteScope.ALL_DAYS) {
        val current = request.value ?: return
        val task = interaction.value.pendingDeletionId?.let { id ->
            latestTasks[id] ?: latestTasks.values.firstOrNull { it.repeat?.seriesId == id }
        }
        interaction.update { it.copy(pendingDeletionId = null) }
        if (task == null || !canDeleteActivity(task, current.viewer)) return
        runWrite { routineRepository.delete(current.ownerUid, task, scope) }
    }

    private fun selectDate(newDate: LocalDate) {
        savedStateHandle[DATE_KEY] = newDate.toString()
        date.value = newDate
        interaction.value = Interaction()
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    /** A pending (offline) write needs no message here: the day shows the sync notice. */
    private fun runWrite(write: suspend () -> DataResult<WriteStatus>) {
        interaction.update { it.copy(actionErrorRes = null) }
        viewModelScope.launch {
            val result = write()
            if (result is DataResult.Failure) interaction.update { it.copy(actionErrorRes = result.error.messageRes()) }
        }
    }

    private fun baseState(viewer: RoutineViewer, day: LocalDate) = RoutineUiState(
        date = day,
        daysFromToday = ChronoUnit.DAYS.between(today(), day),
        canCreate = canCreateActivity(viewer)
    )

    private fun toDay(result: DataResult<RoutineSnapshot>, viewer: RoutineViewer, day: LocalDate): Day {
        val base = baseState(viewer, day)
        return when (result) {
            // A permission error (for example, a revoked link) is shown as an error, never as "empty".
            is DataResult.Failure -> Day(emptyMap(), base.copy(content = ContentState.Error(result.error.messageRes())))
            is DataResult.Success -> {
                val snapshot = result.value
                val tasks = snapshot.tasks.forDay()
                val steps = tasks.map { it.toUiModel(viewer) }
                val state = base.copy(
                    content = if (steps.isEmpty()) ContentState.Empty else ContentState.Content(steps),
                    syncNoticeRes = when {
                        snapshot.isRepeatingUnavailable -> R.string.routine_repeating_unavailable
                        snapshot.isFromCache -> R.string.sync_showing_saved_data
                        snapshot.hasPendingWrites -> R.string.sync_pending_changes
                        else -> null
                    },
                    isTruncated = snapshot.isTruncated
                )
                Day(tasks.associateBy { it.id }, state)
            }
        }
    }

    /** The full activity, for opening the editor. */
    fun activity(taskId: String): RoutineTask? =
        latestTasks[taskId] ?: latestTasks.values.firstOrNull { it.repeat?.seriesId == taskId }
}
