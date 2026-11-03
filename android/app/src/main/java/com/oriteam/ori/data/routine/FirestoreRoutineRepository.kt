package com.oriteam.ori.data.routine

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.awaitWriteAcknowledgement
import com.oriteam.ori.data.firestore.dataErrorFor
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.model.TaskStatus
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.MAX_ROUTINE_TASKS
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineSnapshot
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

/** A month has at most 31 days of [MAX_ROUTINE_TASKS] activities: this bound is never reached in practice. */
private const val MAX_RANGE_PAGES = 31

class FirestoreRoutineRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : RoutineRepository {

    private fun activitiesOf(ownerUid: String) = firestore.collection(FirestoreSchema.USERS)
        .document(ownerUid)
        .collection(FirestoreSchema.ROUTINE_TASKS)

    /**
     * One day, limited. Owner: every activity of the day. Caregiver: only active ones, because the
     * rules reject a query that could return hidden activities. Only equality filters are used, so
     * no composite index is needed; the day order is applied in the domain ([forDay]).
     */
    private fun dayQuery(ownerUid: String, viewer: RoutineViewer, date: LocalDate): Query {
        val byDate = activitiesOf(ownerUid).whereEqualTo(FirestoreSchema.DATE, date.toStoredDate())
        val visible = when (viewer) {
            RoutineViewer.Owner -> byDate
            is RoutineViewer.Caregiver -> byDate.whereEqualTo(
                FirestoreSchema.STATUS,
                FirestoreSchema.taskStatusValues.getValue(TaskStatus.ACTIVE)
            )
        }
        return visible.limit(MAX_ROUTINE_TASKS.toLong())
    }

    override fun observeDay(
        ownerUid: String,
        viewer: RoutineViewer,
        date: LocalDate
    ): Flow<DataResult<RoutineSnapshot>> = callbackFlow {
        // Metadata changes (cache -> server, pending -> confirmed) do not cost document reads.
        val registration = dayQuery(ownerUid, viewer, date)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, exception ->
                val result = if (exception != null || snapshot == null) {
                    DataResult.Failure(exception?.let(::dataErrorFor) ?: DataError.UNKNOWN)
                } else {
                    DataResult.Success(
                        RoutineSnapshot(
                            tasks = snapshot.documents.mapNotNull {
                                routineTaskFrom(
                                    it.id,
                                    ownerUid,
                                    it.data.orEmpty()
                                )
                            },
                            isFromCache = snapshot.metadata.isFromCache,
                            hasPendingWrites = snapshot.metadata.hasPendingWrites()
                        )
                    )
                }
                trySend(result)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getDay(
        ownerUid: String,
        viewer: RoutineViewer,
        date: LocalDate
    ): DataResult<List<RoutineTask>> = runDataOperation {
        // Default source: the server, or the device cache when offline.
        dayQuery(ownerUid, viewer, date).get().await().documents
            .mapNotNull { routineTaskFrom(it.id, ownerUid, it.data.orEmpty()) }
    }

    /**
     * Several days (the reports, the marks of the month): one query over the date range, page by page,
     * instead of one query per day. The owner's range uses the automatic index on `date`; a caregiver's
     * also filters `status`, which uses the composite index in `functions/firestore.indexes.json`.
     */
    override suspend fun getDays(
        ownerUid: String,
        viewer: RoutineViewer,
        dates: List<LocalDate>
    ): DataResult<Map<LocalDate, List<RoutineTask>>> {
        if (dates.isEmpty()) return DataResult.Success(emptyMap())
        return runDataOperation {
            val visible = when (viewer) {
                RoutineViewer.Owner -> activitiesOf(ownerUid)
                is RoutineViewer.Caregiver -> activitiesOf(ownerUid).whereEqualTo(
                    FirestoreSchema.STATUS,
                    FirestoreSchema.taskStatusValues.getValue(TaskStatus.ACTIVE)
                )
            }
            val range = visible
                .whereGreaterThanOrEqualTo(FirestoreSchema.DATE, dates.min().toStoredDate())
                .whereLessThanOrEqualTo(FirestoreSchema.DATE, dates.max().toStoredDate())
                .orderBy(FirestoreSchema.DATE)
                .limit(MAX_ROUTINE_TASKS.toLong())
            val found = mutableListOf<RoutineTask>()
            var page = range.get().await()
            found += page.documents.mapNotNull { routineTaskFrom(it.id, ownerUid, it.data.orEmpty()) }
            var pages = 1
            while (page.size() == MAX_ROUTINE_TASKS && pages < MAX_RANGE_PAGES) {
                page = range.startAfter(page.documents.last()).get().await()
                found += page.documents.mapNotNull { routineTaskFrom(it.id, ownerUid, it.data.orEmpty()) }
                pages++
            }
            val byDate = found.groupBy { it.date }
            dates.associateWith { byDate[it].orEmpty() }
        }
    }

    override fun newActivityId(ownerUid: String): String = activitiesOf(ownerUid).document().id

    override suspend fun createActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer
    ): DataResult<WriteStatus> {
        val author = firebaseAuth.currentUser?.uid ?: return DataResult.Failure(DataError.PERMISSION_DENIED)
        return runDataOperation {
            val write = activitiesOf(ownerUid).document(activityId).set(draft.toNewDocument(createdBy = author))
            awaitWriteAcknowledgement { write.await() }
        }
    }

    override suspend fun updateActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer
    ): DataResult<WriteStatus> = runDataOperation {
        // Field-level update: completion and authorship are not touched by an edit.
        val write = activitiesOf(ownerUid).document(activityId).update(draft.toEditableFields())
        awaitWriteAcknowledgement { write.await() }
    }

    override suspend fun deleteActivity(ownerUid: String, activityId: String): DataResult<WriteStatus> =
        runDataOperation {
            val write = activitiesOf(ownerUid).document(activityId).delete()
            awaitWriteAcknowledgement { write.await() }
        }

    override suspend fun setTaskDone(ownerUid: String, taskId: String, isDone: Boolean): DataResult<WriteStatus> {
        val uid = firebaseAuth.currentUser?.uid ?: return DataResult.Failure(DataError.PERMISSION_DENIED)
        return runDataOperation {
            // The rules check that completedBy is the caller and the times are the server time.
            val completion = mapOf(
                FirestoreSchema.IS_DONE to isDone,
                FirestoreSchema.COMPLETED_BY to uid.takeIf { isDone },
                FirestoreSchema.COMPLETED_AT to FieldValue.serverTimestamp().takeIf { isDone },
                FirestoreSchema.UPDATED_AT to FieldValue.serverTimestamp()
            )
            val write = activitiesOf(ownerUid).document(taskId).update(completion)
            awaitWriteAcknowledgement { write.await() }
        }
    }
}
