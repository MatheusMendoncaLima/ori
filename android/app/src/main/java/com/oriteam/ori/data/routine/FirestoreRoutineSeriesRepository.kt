package com.oriteam.ori.data.routine

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.awaitWriteAcknowledgement
import com.oriteam.ori.data.firestore.mapList
import com.oriteam.ori.data.firestore.observeList
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.MAX_ROUTINE_SERIES
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineSeries
import com.oriteam.ori.domain.routine.RoutineSeriesRepository
import com.oriteam.ori.domain.routine.RoutineSeriesSnapshot
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.domain.routine.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

/** Repeating activities in `users/{owner}/routineSeries`. See docs/funcionalidades/rotina.md. */
class FirestoreRoutineSeriesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : RoutineSeriesRepository {

    private fun seriesOf(ownerUid: String) = firestore.collection(FirestoreSchema.USERS)
        .document(ownerUid)
        .collection(FirestoreSchema.ROUTINE_SERIES)

    /** A caregiver asks only for active ones, because the rules reject a query that could return hidden ones. */
    private fun query(ownerUid: String, viewer: RoutineViewer): Query {
        val visible = when (viewer) {
            RoutineViewer.Owner -> seriesOf(ownerUid)
            is RoutineViewer.Caregiver -> seriesOf(ownerUid).whereEqualTo(
                FirestoreSchema.STATUS,
                FirestoreSchema.taskStatusValues.getValue(TaskStatus.ACTIVE)
            )
        }
        return visible.limit(MAX_ROUTINE_SERIES.toLong())
    }

    override fun observeSeries(ownerUid: String, viewer: RoutineViewer): Flow<DataResult<RoutineSeriesSnapshot>> =
        query(ownerUid, viewer)
            .observeList { routineSeriesFrom(it.id, ownerUid, it.data.orEmpty()) }
            .map { result -> result.mapList { RoutineSeriesSnapshot(it.items, it.isFromCache, it.hasPendingWrites) } }

    override suspend fun getSeries(ownerUid: String, viewer: RoutineViewer): DataResult<List<RoutineSeries>> =
        runDataOperation {
            query(ownerUid, viewer).get().await().documents
                .mapNotNull { routineSeriesFrom(it.id, ownerUid, it.data.orEmpty()) }
        }

    override suspend fun createSeries(
        ownerUid: String,
        seriesId: String,
        draft: ActivityDraft,
        rule: RepeatRule
    ): DataResult<WriteStatus> {
        val author = firebaseAuth.currentUser?.uid ?: return DataResult.Failure(DataError.PERMISSION_DENIED)
        return runDataOperation {
            val write = seriesOf(ownerUid).document(seriesId).set(draft.toNewSeriesDocument(rule, createdBy = author))
            awaitWriteAcknowledgement { write.await() }
        }
    }

    override suspend fun updateSeries(
        ownerUid: String,
        seriesId: String,
        draft: ActivityDraft,
        rule: RepeatRule
    ): DataResult<WriteStatus> = runDataOperation {
        val write = seriesOf(ownerUid).document(seriesId).update(draft.toSeriesFields(rule))
        awaitWriteAcknowledgement { write.await() }
    }

    override suspend fun deleteSeries(ownerUid: String, seriesId: String): DataResult<WriteStatus> =
        runDataOperation {
            val write = seriesOf(ownerUid).document(seriesId).delete()
            awaitWriteAcknowledgement { write.await() }
        }

    override suspend fun replaceActivityWithSeries(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        rule: RepeatRule
    ): DataResult<WriteStatus> {
        val author = firebaseAuth.currentUser?.uid ?: return DataResult.Failure(DataError.PERMISSION_DENIED)
        return runDataOperation {
            // A batch is applied whole or not at all, also when it waits offline.
            val write = firestore.batch()
                .set(seriesOf(ownerUid).document(activityId), draft.toNewSeriesDocument(rule, createdBy = author))
                .delete(activitiesOf(ownerUid).document(activityId))
                .commit()
            awaitWriteAcknowledgement { write.await() }
        }
    }

    override suspend fun replaceSeriesWithActivity(
        ownerUid: String,
        seriesId: String,
        draft: ActivityDraft
    ): DataResult<WriteStatus> {
        val author = firebaseAuth.currentUser?.uid ?: return DataResult.Failure(DataError.PERMISSION_DENIED)
        return runDataOperation {
            val write = firestore.batch()
                .set(activitiesOf(ownerUid).document(seriesId), draft.toNewDocument(createdBy = author))
                .delete(seriesOf(ownerUid).document(seriesId))
                .commit()
            awaitWriteAcknowledgement { write.await() }
        }
    }

    private fun activitiesOf(ownerUid: String) = firestore.routineTasksOf(ownerUid)

    override suspend fun skipDate(ownerUid: String, seriesId: String, date: LocalDate): DataResult<WriteStatus> =
        runDataOperation {
            val write = seriesOf(ownerUid).document(seriesId).update(
                mapOf(
                    FirestoreSchema.SKIPPED_DATES to FieldValue.arrayUnion(date.toStoredDate()),
                    FirestoreSchema.UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
            awaitWriteAcknowledgement { write.await() }
        }
}
