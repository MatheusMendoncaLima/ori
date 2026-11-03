package com.oriteam.ori.data.carelink

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.awaitWriteAcknowledgement
import com.oriteam.ori.data.firestore.careLinkFrom
import com.oriteam.ori.data.firestore.dataErrorFor
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.carelink.CareLink
import com.oriteam.ori.domain.carelink.CareLinkPermissions
import com.oriteam.ori.domain.carelink.CareLinkRepository
import com.oriteam.ori.domain.carelink.CareLinkSide
import com.oriteam.ori.domain.carelink.CareLinkStatus
import com.oriteam.ori.domain.carelink.MAX_CARE_LINKS
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreCareLinkRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : CareLinkRepository {

    private val links get() = firestore.collection(FirestoreSchema.CARE_LINKS)

    override fun observeActiveLinks(uid: String, side: CareLinkSide): Flow<DataResult<List<CareLink>>> =
        callbackFlow {
            // The rules allow reading a link only to its two members, so the query filters by the caller.
            val ownField = when (side) {
                CareLinkSide.CAREGIVER -> FirestoreSchema.CAREGIVER_UID
                CareLinkSide.PRIMARY_USER -> FirestoreSchema.PRIMARY_USER_UID
            }
            val registration = links.whereEqualTo(ownField, uid)
                .whereEqualTo(FirestoreSchema.STATUS, FirestoreSchema.linkStatusValues.getValue(CareLinkStatus.ACTIVE))
                .limit(MAX_CARE_LINKS.toLong())
                .addSnapshotListener { snapshot, exception ->
                    val result = if (exception != null || snapshot == null) {
                        DataResult.Failure(exception?.let(::dataErrorFor) ?: DataError.UNKNOWN)
                    } else {
                        DataResult.Success(snapshot.documents.mapNotNull { careLinkFrom(it.id, it.data.orEmpty()) })
                    }
                    trySend(result)
                }
            awaitClose { registration.remove() }
        }

    override suspend fun revoke(linkId: String): DataResult<WriteStatus> = runDataOperation {
        val write = links.document(linkId).update(
            mapOf(
                FirestoreSchema.STATUS to FirestoreSchema.linkStatusValues.getValue(CareLinkStatus.REVOKED),
                FirestoreSchema.REVOKED_AT to FieldValue.serverTimestamp()
            )
        )
        awaitWriteAcknowledgement { write.await() }
    }

    override suspend fun updatePermissions(linkId: String, permissions: CareLinkPermissions): DataResult<WriteStatus> =
        runDataOperation {
            val value = mapOf(
                FirestoreSchema.VIEW_ROUTINES to permissions.viewRoutines,
                FirestoreSchema.MANAGE_ROUTINES to permissions.manageRoutines,
                FirestoreSchema.MANAGE_COMMUNICATION to permissions.manageCommunication
            )
            val write = links.document(linkId).update(FirestoreSchema.PERMISSIONS, value)
            awaitWriteAcknowledgement { write.await() }
        }
}
