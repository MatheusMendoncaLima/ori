package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow

/**
 * A message the person kept to say again with one tap, in the [language] it was built in ("pt-BR"), which the
 * voice follows. Null for one saved before messages recorded it, which is in Portuguese (see [contentLanguage]).
 */
/**
 * [emoji] is the picture the person chose for the phrase; null shows the app's picture for saved phrases.
 * [position] is the place the person moved it to; null for one never moved (see [inChosenOrder]).
 */
data class SavedMessage(
    val id: String,
    val text: String,
    val language: String? = null,
    val emoji: String? = null,
    val position: Int? = null
)

/**
 * The order the person sees: the phrases never moved first, newest first (the order they are read in), then the
 * ones moved, by [SavedMessage.position]. A phrase saved after a move goes on top, where the person looks for it.
 */
fun List<SavedMessage>.inChosenOrder(): List<SavedMessage> = sortedWith(compareBy(nullsFirst()) { it.position })

/**
 * The positions to write when the phrase [messageId] goes to [toIndex] of this list (in the order shown): every
 * phrase gets its place, so the ones never moved keep theirs too. Null when nothing changes.
 */
fun List<SavedMessage>.positionsAfterMoving(messageId: String, toIndex: Int): Map<String, Int>? {
    val from = indexOfFirst { it.id == messageId }
    if (from < 0 || toIndex !in indices || from == toIndex) return null
    val placed = mapNotNull { message -> message.position?.let { message.id to it } }.toMap()
    return positionChanges(placed, map { it.id }.moved(from, toIndex)).ifEmpty { null }
}

/** Longest emoji text kept (a family or a flag is several characters); the rules check the same limit. */
const val MAX_SAVED_EMOJI_LENGTH = 32

/** The typed or chosen emoji, trimmed; null when empty or too long, which means the app's picture. */
fun savedEmojiOrNull(value: String): String? =
    value.trim().takeIf { it.isNotEmpty() && it.length <= MAX_SAVED_EMOJI_LENGTH }

/** The language the message is in: its own, or Portuguese for one from before messages recorded it. */
val SavedMessage.contentLanguage: String get() = language ?: OLD_CONTENT_LANGUAGE

/** Most saved messages; also enforced by the rules on list queries. */
const val MAX_SAVED_MESSAGES = 50

/** Saved messages of the primary user. Private: the rules deny them to caregivers. */
interface SavedMessageRepository {
    /** Follows the newest saved messages, at most [MAX_SAVED_MESSAGES], in the order the person chose. */
    fun observeSavedMessages(ownerUid: String): Flow<DataResult<List<SavedMessage>>>

    /** [position] keeps the place of a message brought from the device; null puts it on top, as a new one. */
    suspend fun saveMessage(
        ownerUid: String,
        text: String,
        language: String,
        emoji: String? = null,
        position: Int? = null
    ): DataResult<WriteStatus>

    /** Writes the new places of saved messages (see [positionsAfterMoving]), all of them or none. */
    suspend fun reorderSavedMessages(ownerUid: String, positions: Map<String, Int>): DataResult<WriteStatus>

    /** Changes the picture of a saved message; null goes back to the app's picture. */
    suspend fun setSavedMessageEmoji(ownerUid: String, messageId: String, emoji: String?): DataResult<WriteStatus>

    suspend fun deleteSavedMessage(ownerUid: String, messageId: String): DataResult<WriteStatus>
}
