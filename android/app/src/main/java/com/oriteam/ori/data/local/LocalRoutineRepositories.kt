package com.oriteam.ori.data.local

import androidx.room.withTransaction
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.MAX_ROUTINE_SERIES
import com.oriteam.ori.domain.routine.MAX_ROUTINE_TASKS
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineSeries
import com.oriteam.ori.domain.routine.RoutineSeriesRepository
import com.oriteam.ori.domain.routine.RoutineSeriesSnapshot
import com.oriteam.ori.domain.routine.RoutineSnapshot
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.domain.routine.TaskStatus
import com.oriteam.ori.domain.routine.repeatRuleFromStoredText
import com.oriteam.ori.domain.routine.toStoredText
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

internal fun RoutineTaskEntity.toDomain() = RoutineTask(
    id = id,
    ownerUid = LOCAL_OWNER_UID,
    title = title,
    date = LocalDate.parse(date),
    scheduledTime = scheduledTime?.let(LocalTime::parse),
    description = description,
    status = if (isHiddenFromCaregiver) TaskStatus.HIDDEN_FROM_CAREGIVER else TaskStatus.ACTIVE,
    ownerCanComplete = true,
    isDone = isDone,
    createdBy = LOCAL_OWNER_UID
)

internal fun ActivityDraft.toEntity(id: String, isDone: Boolean) = RoutineTaskEntity(
    id = id,
    title = title.trim(),
    date = date.toString(),
    scheduledTime = scheduledTime?.toString(),
    description = description?.trim()?.takeIf { it.isNotEmpty() },
    isHiddenFromCaregiver = isHiddenFromCaregiver,
    isDone = isDone
)

/** Routine of someone without an account. There are no caregivers here, so no visibility filter. */
class LocalRoutineRepository @Inject constructor(database: OriDatabase) : RoutineRepository {
    private val dao = database.routineDao()

    override fun observeDay(ownerUid: String, viewer: RoutineViewer, date: LocalDate) =
        dao.observeDay(date.toString(), MAX_ROUTINE_TASKS)
            .map { tasks ->
                RoutineSnapshot(
                    tasks.map { it.toDomain() },
                    isFromCache = false,
                    hasPendingWrites = false
                )
            }
            .asLocalResult()

    override suspend fun getDay(ownerUid: String, viewer: RoutineViewer, date: LocalDate) =
        runLocalRead { dao.getDay(date.toString(), MAX_ROUTINE_TASKS).map { it.toDomain() } }

    override fun newActivityId(ownerUid: String): String = UUID.randomUUID().toString()

    /** Saving twice with the same id keeps one activity, as with the Firestore document id. */
    override suspend fun createActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer
    ) = runLocalWrite { dao.upsert(draft.toEntity(activityId, isDone = false)) }

    override suspend fun updateActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer
    ) = runLocalWrite {
        // Completion is not touched by an edit.
        dao.upsert(draft.toEntity(activityId, isDone = dao.get(activityId)?.isDone == true))
    }

    override suspend fun deleteActivity(ownerUid: String, activityId: String) =
        runLocalWrite { dao.delete(activityId) }

    override suspend fun setTaskDone(ownerUid: String, taskId: String, isDone: Boolean) =
        runLocalWrite { dao.setDone(taskId, isDone) }
}

internal fun RoutineSeriesEntity.toDomain(): RoutineSeries? {
    val rule = repeatRuleFromStoredText(repeatRule) ?: return null
    return RoutineSeries(
        id = id,
        ownerUid = LOCAL_OWNER_UID,
        title = title,
        startDate = LocalDate.parse(startDate),
        scheduledTime = scheduledTime?.let(LocalTime::parse),
        description = description,
        status = if (isHiddenFromCaregiver) TaskStatus.HIDDEN_FROM_CAREGIVER else TaskStatus.ACTIVE,
        ownerCanComplete = true,
        createdBy = LOCAL_OWNER_UID,
        rule = rule,
        skippedDates = skippedDates.split(",").filter { it.isNotEmpty() }.map(LocalDate::parse).toSet()
    )
}

/** Repeating activities of someone without an account. */
class LocalRoutineSeriesRepository @Inject constructor(private val database: OriDatabase) : RoutineSeriesRepository {
    private val dao = database.routineSeriesDao()

    override fun observeSeries(ownerUid: String, viewer: RoutineViewer) = dao.observe(MAX_ROUTINE_SERIES)
        .map { all ->
            RoutineSeriesSnapshot(
                all.mapNotNull { it.toDomain() },
                isFromCache = false,
                hasPendingWrites = false
            )
        }
        .asLocalResult()

    override suspend fun getSeries(ownerUid: String, viewer: RoutineViewer) =
        runLocalRead { dao.getAll().mapNotNull { it.toDomain() } }

    override suspend fun createSeries(ownerUid: String, seriesId: String, draft: ActivityDraft, rule: RepeatRule) =
        runLocalWrite { dao.upsert(draft.toSeriesEntity(seriesId, rule, skippedDates = "")) }

    override suspend fun updateSeries(ownerUid: String, seriesId: String, draft: ActivityDraft, rule: RepeatRule) =
        runLocalWrite {
            dao.upsert(draft.toSeriesEntity(seriesId, rule, skippedDates = dao.get(seriesId)?.skippedDates.orEmpty()))
        }

    override suspend fun deleteSeries(ownerUid: String, seriesId: String) = runLocalWrite { dao.delete(seriesId) }

    override suspend fun replaceActivityWithSeries(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        rule: RepeatRule
    ) = runLocalWrite {
        database.withTransaction {
            dao.upsert(draft.toSeriesEntity(activityId, rule, skippedDates = ""))
            database.routineDao().delete(activityId)
        }
    }

    override suspend fun replaceSeriesWithActivity(ownerUid: String, seriesId: String, draft: ActivityDraft) =
        runLocalWrite {
            database.withTransaction {
                database.routineDao().upsert(draft.toEntity(seriesId, isDone = false))
                dao.delete(seriesId)
            }
        }

    override suspend fun skipDate(ownerUid: String, seriesId: String, date: LocalDate) = runLocalWrite {
        dao.get(seriesId)?.let { current ->
            val dates = current.skippedDates.split(",").filter { it.isNotEmpty() } + date.toString()
            dao.upsert(current.copy(skippedDates = dates.distinct().joinToString(",")))
        }
    }

    private fun ActivityDraft.toSeriesEntity(id: String, rule: RepeatRule, skippedDates: String) = RoutineSeriesEntity(
        id = id,
        title = title.trim(),
        startDate = date.toString(),
        scheduledTime = scheduledTime?.toString(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        isHiddenFromCaregiver = isHiddenFromCaregiver,
        repeatRule = rule.toStoredText(),
        skippedDates = skippedDates
    )
}
