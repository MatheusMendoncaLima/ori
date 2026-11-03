package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow

/** A message the person kept to say again with one tap. */
data class SavedMessage(val id: String, val text: String)

/** Most saved messages; also enforced by the rules on list queries. */
const val MAX_SAVED_MESSAGES = 50

/** Saved messages of the primary user. Private: the rules deny them to caregivers. */
interface SavedMessageRepository {
    /** Follows the newest saved messages, at most [MAX_SAVED_MESSAGES]. */
    fun observeSavedMessages(ownerUid: String): Flow<DataResult<List<SavedMessage>>>

    suspend fun saveMessage(ownerUid: String, text: String): DataResult<WriteStatus>

    suspend fun deleteSavedMessage(ownerUid: String, messageId: String): DataResult<WriteStatus>
}
