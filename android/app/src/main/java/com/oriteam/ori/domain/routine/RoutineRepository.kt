package com.oriteam.ori.domain.routine

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Persistence of routine activities. Every write works offline: the change is shown at once
 * and sent when the connection returns ([WriteStatus.PENDING_SYNC]).
 */
interface RoutineRepository {
    /**
     * Follows the activities of [ownerUid] on [date] as seen by [viewer], with at most
     * [MAX_ROUTINE_TASKS]. Emits cached data first when available, then server data.
     */
    fun observeDay(ownerUid: String, viewer: RoutineViewer, date: LocalDate): Flow<DataResult<RoutineSnapshot>>

    /** One-time read of a day (server, or the device cache when offline), used to detect duplicates. */
    suspend fun getDay(ownerUid: String, viewer: RoutineViewer, date: LocalDate): DataResult<List<RoutineTask>>

    /**
     * Reserves an id for a new activity without any network call. Creating twice with the same id
     * stores a single activity, so a repeated tap or a restored form never duplicates it.
     */
    fun newActivityId(ownerUid: String): String

    /**
     * [viewer] is who saves. A plain activity does not need it; a repeating one reads the other repeating
     * activities first, and a caregiver may only read the visible ones.
     */
    suspend fun createActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer = RoutineViewer.Owner
    ): DataResult<WriteStatus>

    suspend fun updateActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer = RoutineViewer.Owner
    ): DataResult<WriteStatus>

    suspend fun deleteActivity(ownerUid: String, activityId: String): DataResult<WriteStatus>

    suspend fun setTaskDone(ownerUid: String, taskId: String, isDone: Boolean): DataResult<WriteStatus>

    /** Several days at once. Only the repeating routine has something to save by reading them together. */
    suspend fun getDays(
        ownerUid: String,
        viewer: RoutineViewer,
        dates: List<LocalDate>
    ): DataResult<Map<LocalDate, List<RoutineTask>>> = coroutineScope {
        val results = dates.map { date -> async { date to getDay(ownerUid, viewer, date) } }.awaitAll()
        results.firstNotNullOfOrNull { (_, result) -> result as? DataResult.Failure }
            ?: DataResult.Success(results.associate { (date, result) -> date to (result as DataResult.Success).value })
    }

    /** Marks an activity. A day of a repeating activity may not have a document yet. */
    suspend fun setDone(ownerUid: String, task: RoutineTask, isDone: Boolean): DataResult<WriteStatus> =
        setTaskDone(ownerUid, task.id, isDone)

    suspend fun delete(
        ownerUid: String,
        task: RoutineTask,
        scope: DeleteScope = DeleteScope.ALL_DAYS
    ): DataResult<WriteStatus> = deleteActivity(ownerUid, task.id)
}

/** What deleting one day of a repeating activity means. */
enum class DeleteScope { THIS_DAY, ALL_DAYS }

/**
 * Upper bound of one day query. A day is small; the limit protects cost and memory and is
 * also enforced by the Firestore rules (`request.query.limit`).
 */
const val MAX_ROUTINE_TASKS = 100

/** A day read plus where it came from, so the screen can be honest about stale data. */
data class RoutineSnapshot(
    val tasks: List<RoutineTask>,
    /** True when the server could not be reached and the device cache answered. */
    val isFromCache: Boolean,
    /** True while some local change has not been confirmed by the server. */
    val hasPendingWrites: Boolean,
    /** The repeating activities could not be read: the day may be missing some. */
    val isRepeatingUnavailable: Boolean = false
) {
    /** The limit was reached, so there may be more activities than shown. */
    val isTruncated: Boolean get() = tasks.size >= MAX_ROUTINE_TASKS
}
