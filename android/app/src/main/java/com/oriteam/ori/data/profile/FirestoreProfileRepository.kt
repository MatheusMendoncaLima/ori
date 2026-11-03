package com.oriteam.ori.data.profile

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.oriteam.ori.data.firestore.DocumentState
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.awaitWriteAcknowledgement
import com.oriteam.ori.data.firestore.dataErrorFor
import com.oriteam.ori.data.firestore.profileLookups
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.profile.ProfileLookup
import com.oriteam.ori.domain.profile.ProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProfileRepository {

    /** One document listener while the session is observed; it stops when the app is in background. */
    override fun observeProfile(uid: String): Flow<ProfileLookup> {
        val states = callbackFlow {
            val registration = firestore.collection(FirestoreSchema.USERS).document(uid)
                .addSnapshotListener { snapshot, exception ->
                    val state = when {
                        exception != null -> DocumentState(
                            exists = false,
                            isFromCache = false,
                            error = dataErrorFor(exception)
                        )
                        snapshot == null -> null
                        else -> DocumentState(
                            exists = snapshot.exists(),
                            isFromCache = snapshot.metadata.isFromCache,
                            data = snapshot.data.orEmpty()
                        )
                    }
                    state?.let { trySend(it) }
                }
            awaitClose { registration.remove() }
        }
        return profileLookups(uid, states)
    }

    override suspend fun createProfile(uid: String, displayName: String): DataResult<WriteStatus> =
        runDataOperation {
            // Only the name: there is one kind of account, so a profile has no role.
            val profile = mapOf(
                FirestoreSchema.DISPLAY_NAME to displayName,
                FirestoreSchema.CREATED_AT to FieldValue.serverTimestamp()
            )
            val write = firestore.collection(FirestoreSchema.USERS).document(uid).set(profile)
            awaitWriteAcknowledgement { write.await() }
        }

    override suspend fun updateDisplayName(uid: String, displayName: String): DataResult<WriteStatus> =
        runDataOperation {
            val write = firestore.collection(FirestoreSchema.USERS).document(uid)
                .update(FirestoreSchema.DISPLAY_NAME, displayName)
            awaitWriteAcknowledgement { write.await() }
        }
}
