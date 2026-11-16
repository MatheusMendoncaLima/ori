package com.oriteam.ori.domain.routine

import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/**
 * What the screens use as the routine: the activities stored for a day plus the repeating ones that
 * fall on it. The rest of the app sees plain [RoutineTask]s and does not know about rules, so the
 * week, "Tarefas", the reports and the move to an account work the same for both.
 */
@Suppress("TooManyFunctions") // Implements RoutineRepository, one function per operation.
class RepeatingRoutineRepository(
    private val tasks: RoutineRepository,
    private val series: RoutineSeriesRepository
) : RoutineRepository {

    /**
     * If the rules cannot be read the day still shows what it can, with a notice: an activity list
     * must not disappear because a different document was refused.
     */
    override fun observeDay(
        ownerUid: String,
        viewer: RoutineViewer,
        date: LocalDate
    ): Flow<DataResult<RoutineSnapshot>> =
        combine(tasks.observeDay(ownerUid, viewer, date), series.observeSeries(ownerUid, viewer)) { day, rules ->
            when (day) {
                is DataResult.Failure -> day
                is DataResult.Success -> DataResult.Success(
                    when (rules) {
                        is DataResult.Failure -> day.value.copy(isRepeatingUnavailable = true)
                        is DataResult.Success -> day.value.copy(
                            tasks = merged(day.value.tasks, rules.value.series, date),
                            isFromCache = day.value.isFromCache || rules.value.isFromCache,
                            hasPendingWrites = day.value.hasPendingWrites || rules.value.hasPendingWrites
                        )
                    }
                )
            }
        }

    override suspend fun getDay(
        ownerUid: String,
        viewer: RoutineViewer,
        date: LocalDate
    ): DataResult<List<RoutineTask>> {
        val day = tasks.getDay(ownerUid, viewer, date)
        if (day !is DataResult.Success) return day
        val rules = series.getSeries(ownerUid, viewer)
        return DataResult.Success(
            if (rules is DataResult.Success) merged(day.value, rules.value, date) else day.value
        )
    }

    /** One read of the rules for all the days: a monthly report does not read them 31 times. */
    override suspend fun getDays(
        ownerUid: String,
        viewer: RoutineViewer,
        dates: List<LocalDate>
    ): DataResult<Map<LocalDate, List<RoutineTask>>> {
        val rules = when (val result = series.getSeries(ownerUid, viewer)) {
            is DataResult.Failure -> return result
            is DataResult.Success -> result.value
        }
        return when (val days = tasks.getDays(ownerUid, viewer, dates)) {
            is DataResult.Failure -> days
            is DataResult.Success -> DataResult.Success(
                days.value.mapValues { (date, ofDay) -> merged(ofDay, rules, date) }
            )
        }
    }

    override fun newActivityId(ownerUid: String) = tasks.newActivityId(ownerUid)

    override suspend fun createActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer
    ): DataResult<WriteStatus> {
        val rule = draft.repeat ?: return tasks.createActivity(ownerUid, activityId, draft)
        val current = when (val read = series.getSeries(ownerUid, viewer)) {
            is DataResult.Failure -> return read
            is DataResult.Success -> read.value
        }
        // Saving again one that is already there (a repeated attempt, same id) updates it: it adds nothing,
        // and the rules refuse a second creation, because it would change who created it and when.
        return when {
            current.any { it.id == activityId } -> series.updateSeries(ownerUid, activityId, draft, rule)
            current.size >= MAX_ROUTINE_SERIES -> DataResult.Failure(DataError.PRECONDITION_FAILED)
            else -> series.createSeries(ownerUid, activityId, draft, rule)
        }
    }

    /**
     * The repeating activities are read as [viewer] sees them: a caregiver may only list the visible ones,
     * and a list the rules refuse would make every save fail.
     *
     * [activityId] is a plain activity or a series, and the form may have turned one into the other:
     * then the new form and the removal of the old one are a single write.
     */
    override suspend fun updateActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer
    ): DataResult<WriteStatus> {
        val rule = draft.repeat
        val isSeries = when (val current = series.getSeries(ownerUid, viewer)) {
            is DataResult.Failure -> if (rule == null) false else return current
            is DataResult.Success -> current.value.any { it.id == activityId }
        }
        return when {
            isSeries && rule != null -> series.updateSeries(ownerUid, activityId, draft, rule)
            isSeries -> series.replaceSeriesWithActivity(ownerUid, activityId, draft)
            rule != null -> series.replaceActivityWithSeries(ownerUid, activityId, draft, rule)
            else -> tasks.updateActivity(ownerUid, activityId, draft)
        }
    }

    override suspend fun deleteActivity(ownerUid: String, activityId: String) =
        tasks.deleteActivity(ownerUid, activityId)

    override suspend fun setTaskDone(ownerUid: String, taskId: String, isDone: Boolean) =
        tasks.setTaskDone(ownerUid, taskId, isDone)

    /** The first time a day of a series changes it gets a document, with the id of that day. */
    override suspend fun setDone(ownerUid: String, task: RoutineTask, isDone: Boolean): DataResult<WriteStatus> {
        if (task.repeat == null || task.repeat.isStored) return tasks.setTaskDone(ownerUid, task.id, isDone)
        val draft = ActivityDraft(
            title = task.title,
            date = task.date,
            scheduledTime = task.scheduledTime,
            description = task.description,
            ownerCanComplete = task.ownerCanComplete,
            isHiddenFromCaregiver = task.status == TaskStatus.HIDDEN_FROM_CAREGIVER
        )
        // The rules require a new activity to start as not done, so completion is a second step.
        return tasks.createActivity(ownerUid, task.id, draft)
            .thenWrite { tasks.setTaskDone(ownerUid, task.id, isDone) }
    }

    /**
     * Removing all the days keeps the ones already marked as done, because they are the history in the
     * reports. Only the document of the day being looked at goes away with the series. The other days that
     * have a document but are not done stay stored, and [merged] leaves them out.
     */
    override suspend fun delete(ownerUid: String, task: RoutineTask, scope: DeleteScope): DataResult<WriteStatus> {
        val repeat = task.repeat ?: return tasks.deleteActivity(ownerUid, task.id)
        return when (scope) {
            DeleteScope.THIS_DAY -> series.skipDate(ownerUid, repeat.seriesId, task.date)
                .thenWrite { deleteStoredDay(ownerUid, task) }
            // The day goes first: the rules let the owner remove a day a caregiver marked only while its series
            // still exists (R5). Otherwise the series went and an error showed for a day that could not go.
            DeleteScope.ALL_DAYS -> deleteStoredDay(ownerUid, task)
                .thenWrite { series.deleteSeries(ownerUid, repeat.seriesId) }
        }
    }

    private suspend fun deleteStoredDay(ownerUid: String, task: RoutineTask): DataResult<WriteStatus> =
        if (task.repeat?.isStored == true) {
            tasks.deleteActivity(ownerUid, task.id)
        } else {
            DataResult.Success(WriteStatus.SYNCED)
        }

    private inline fun DataResult<WriteStatus>.thenWrite(next: () -> DataResult<WriteStatus>) =
        if (this is DataResult.Success) next() else this

    private fun merged(stored: List<RoutineTask>, rules: List<RoutineSeries>, date: LocalDate): List<RoutineTask> {
        val byId = rules.associateBy { it.id }
        val days = stored.mapNotNull { task ->
            val seriesId = seriesIdOf(task.id) ?: return@mapNotNull task
            when {
                // What is edited in the series shows on every day; only completion belongs to the day.
                seriesId in byId -> byId.getValue(seriesId).occurrenceOn(date, task.isDone, stored = true)
                // The series is gone (or not visible to this viewer): a day marked as done stays as history,
                // and one that was marked and unmarked is not an activity of its own.
                task.isDone -> task
                else -> null
            }
        }
        val storedIds = stored.mapTo(mutableSetOf()) { it.id }
        return days.filterNot { it.repeat != null && it.date in byId.getValue(it.repeat.seriesId).skippedDates } +
            rules.filter { it.occursOn(date) && occurrenceIdOf(it.id, date) !in storedIds }
                .map { it.occurrenceOn(date) }
    }
}
