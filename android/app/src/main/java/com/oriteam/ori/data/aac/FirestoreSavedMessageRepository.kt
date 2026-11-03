package com.oriteam.ori.data.aac

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.awaitWriteAcknowledgement
import com.oriteam.ori.data.firestore.observeList
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.aac.MAX_SAVED_MESSAGES
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SavedMessageRepository
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Saved messages in `users/{owner}/aacSavedMessages`. Newest first: ordering by one field uses
 * the automatic single-field index, so no composite index is needed.
 */
class FirestoreSavedMessageRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : SavedMessageRepository {

    private fun messagesOf(ownerUid: String) = firestore.collection(FirestoreSchema.USERS)
        .document(ownerUid)
        .collection(FirestoreSchema.AAC_SAVED_MESSAGES)

    override fun observeSavedMessages(ownerUid: String): Flow<DataResult<List<SavedMessage>>> =
        messagesOf(ownerUid)
            .orderBy(FirestoreSchema.CREATED_AT, Query.Direction.DESCENDING)
            .limit(MAX_SAVED_MESSAGES.toLong())
            .observeList { savedMessageFrom(it.id, it.data.orEmpty()) }
            .map { result ->
                when (result) {
                    is DataResult.Failure -> result
                    is DataResult.Success -> DataResult.Success(result.value.items)
                }
            }

    override suspend fun saveMessage(ownerUid: String, text: String): DataResult<WriteStatus> = runDataOperation {
        val write = messagesOf(ownerUid).document().set(
            mapOf(FirestoreSchema.TEXT to text, FirestoreSchema.CREATED_AT to FieldValue.serverTimestamp())
        )
        awaitWriteAcknowledgement { write.await() }
    }

    override suspend fun deleteSavedMessage(ownerUid: String, messageId: String): DataResult<WriteStatus> =
        runDataOperation {
            val write = messagesOf(ownerUid).document(messageId).delete()
            awaitWriteAcknowledgement { write.await() }
        }
}
