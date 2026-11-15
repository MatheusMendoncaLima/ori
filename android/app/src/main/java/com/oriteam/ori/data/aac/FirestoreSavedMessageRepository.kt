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
import com.oriteam.ori.domain.aac.inChosenOrder
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Saved messages in `users/{owner}/aacSavedMessages`. Read newest first: ordering by one field uses the automatic
 * single-field index, so no composite index is needed. The order the person chose is applied after reading
 * ([inChosenOrder]): ordering by `position` in the query would leave out the messages never moved, which have none.
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
                    is DataResult.Success -> DataResult.Success(result.value.items.inChosenOrder())
                }
            }

    override suspend fun saveMessage(
        ownerUid: String,
        text: String,
        language: String,
        emoji: String?,
        position: Int?
    ): DataResult<WriteStatus> = runDataOperation {
        val fields = mapOf(
            FirestoreSchema.TEXT to text,
            FirestoreSchema.LANGUAGE to language,
            FirestoreSchema.CREATED_AT to FieldValue.serverTimestamp()
        ) + listOfNotNull(emoji?.let { FirestoreSchema.EMOJI to it }, position?.let { FirestoreSchema.POSITION to it })
        val write = messagesOf(ownerUid).document().set(fields)
        awaitWriteAcknowledgement { write.await() }
    }

    /** Only the picture changes: the rules refuse any other field in an update. */
    override suspend fun setSavedMessageEmoji(
        ownerUid: String,
        messageId: String,
        emoji: String?
    ): DataResult<WriteStatus> = runDataOperation {
        val write = messagesOf(ownerUid).document(messageId)
            .update(FirestoreSchema.EMOJI, emoji ?: FieldValue.delete())
        awaitWriteAcknowledgement { write.await() }
    }

    /** One atomic batch; only the position changes, as the rules require. */
    override suspend fun reorderSavedMessages(ownerUid: String, positions: Map<String, Int>): DataResult<WriteStatus> =
        runDataOperation {
            val batch = firestore.batch()
            positions.forEach { (id, position) ->
                batch.update(messagesOf(ownerUid).document(id), FirestoreSchema.POSITION, position)
            }
            val commit = batch.commit()
            awaitWriteAcknowledgement { commit.await() }
        }

    override suspend fun deleteSavedMessage(ownerUid: String, messageId: String): DataResult<WriteStatus> =
        runDataOperation {
            val write = messagesOf(ownerUid).document(messageId).delete()
            awaitWriteAcknowledgement { write.await() }
        }
}
