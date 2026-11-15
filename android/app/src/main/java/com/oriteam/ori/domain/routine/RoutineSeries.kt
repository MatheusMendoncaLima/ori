package com.oriteam.ori.domain.routine

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/** Most repeating activities of one person; also enforced by the rules on list queries. */
const val MAX_ROUTINE_SERIES = 50

private const val OCCURRENCE_SEPARATOR = "__"

/**
 * An activity that repeats. Only the rule is stored, never one copy per day, so it has no end and
 * costs one document. A day gets a document of its own only when something happens to it (it was
 * marked as done), under an id made from the series and the date: [occurrenceIdOf].
 */
data class RoutineSeries(
    val id: String,
    val ownerUid: String,
    val title: String,
    val startDate: LocalDate,
    val scheduledTime: LocalTime?,
    val description: String?,
    val status: TaskStatus,
    val ownerCanComplete: Boolean,
    val createdBy: String,
    val rule: RepeatRule,
    /** Days removed one by one ("só este dia"). */
    val skippedDates: Set<LocalDate> = emptySet()
) {
    fun occursOn(date: LocalDate): Boolean =
        !date.isBefore(startDate) && date !in skippedDates && rule.occursOn(date, startDate)

    /** How this series looks on [date]; [stored] says whether that day has its own document. */
    fun occurrenceOn(date: LocalDate, isDone: Boolean = false, stored: Boolean = false) = RoutineTask(
        id = occurrenceIdOf(id, date),
        ownerUid = ownerUid,
        title = title,
        date = date,
        scheduledTime = scheduledTime,
        description = description,
        status = status,
        ownerCanComplete = ownerCanComplete,
        isDone = isDone,
        createdBy = createdBy,
        repeat = TaskRepeat(id, rule, startDate, isStored = stored)
    )

    fun toDraft() = ActivityDraft(
        title = title,
        date = startDate,
        scheduledTime = scheduledTime,
        description = description,
        ownerCanComplete = ownerCanComplete,
        isHiddenFromCaregiver = status == TaskStatus.HIDDEN_FROM_CAREGIVER,
        repeat = rule
    )
}

/** On a task: it is one day of a series. */
data class TaskRepeat(
    val seriesId: String,
    val rule: RepeatRule,
    val startDate: LocalDate,
    val isStored: Boolean
)

fun occurrenceIdOf(seriesId: String, date: LocalDate) = "$seriesId$OCCURRENCE_SEPARATOR$date"

/** The series of an occurrence id; null for an activity that never repeated. */
fun seriesIdOf(taskId: String): String? = taskId.substringBeforeLast(OCCURRENCE_SEPARATOR, "").ifEmpty { null }

data class RoutineSeriesSnapshot(
    val series: List<RoutineSeries>,
    val isFromCache: Boolean,
    val hasPendingWrites: Boolean
)

/** Storage of the rules. The screens never see this: they use [RepeatingRoutineRepository]. */
interface RoutineSeriesRepository {
    fun observeSeries(ownerUid: String, viewer: RoutineViewer): Flow<DataResult<RoutineSeriesSnapshot>>

    suspend fun getSeries(ownerUid: String, viewer: RoutineViewer): DataResult<List<RoutineSeries>>

    suspend fun createSeries(
        ownerUid: String,
        seriesId: String,
        draft: ActivityDraft,
        rule: RepeatRule
    ): DataResult<WriteStatus>

    suspend fun updateSeries(
        ownerUid: String,
        seriesId: String,
        draft: ActivityDraft,
        rule: RepeatRule
    ): DataResult<WriteStatus>

    suspend fun deleteSeries(ownerUid: String, seriesId: String): DataResult<WriteStatus>

    suspend fun skipDate(ownerUid: String, seriesId: String, date: LocalDate): DataResult<WriteStatus>

    /**
     * Turns the plain activity [activityId] into a series with the same id, in one write: the day never
     * shows both, and nothing is lost if it fails.
     */
    suspend fun replaceActivityWithSeries(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        rule: RepeatRule
    ): DataResult<WriteStatus>

    /** Turns the series [seriesId] back into a plain activity with the same id, in one write. */
    suspend fun replaceSeriesWithActivity(
        ownerUid: String,
        seriesId: String,
        draft: ActivityDraft
    ): DataResult<WriteStatus>
}
