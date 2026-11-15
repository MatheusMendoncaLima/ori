package com.oriteam.ori.data.aac

import com.google.firebase.firestore.FieldValue
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SymbolColor
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.aac.storedKey
import com.oriteam.ori.domain.aac.wordClassFromKey

/** "emoji:💧" or "device:3f2a….jpg". Unknown prefixes read as "no image" rather than failing. */
internal fun SymbolImage.toStoredImage(): String = when (this) {
    is SymbolImage.Emoji -> FirestoreSchema.EMOJI_IMAGE_PREFIX + value
    is SymbolImage.DevicePhoto -> FirestoreSchema.DEVICE_PHOTO_IMAGE_PREFIX + fileName
}

internal fun symbolImageFrom(value: Any?): SymbolImage? {
    val stored = value as? String ?: return null
    return when {
        stored.startsWith(FirestoreSchema.EMOJI_IMAGE_PREFIX) ->
            SymbolImage.Emoji(stored.removePrefix(FirestoreSchema.EMOJI_IMAGE_PREFIX))
        stored.startsWith(FirestoreSchema.DEVICE_PHOTO_IMAGE_PREFIX) ->
            SymbolImage.DevicePhoto(stored.removePrefix(FirestoreSchema.DEVICE_PHOTO_IMAGE_PREFIX))
        else -> null
    }
}

/** Firestore returns whole numbers as Long; a missing position sorts first instead of hiding the item. */
private fun positionFrom(value: Any?): Int = (value as? Number)?.toInt() ?: 0

/** A board without a name or author is skipped (null) instead of being shown half-filled. */
internal fun aacBoardFrom(id: String, data: Map<String, Any?>): AacBoard? {
    val name = data[FirestoreSchema.NAME] as? String ?: return null
    val createdBy = data[FirestoreSchema.CREATED_BY] as? String ?: return null
    return AacBoard(
        id = id,
        name = name,
        position = positionFrom(data[FirestoreSchema.POSITION]),
        createdBy = createdBy,
        language = data[FirestoreSchema.LANGUAGE] as? String
    )
}

internal fun aacSymbolFrom(id: String, boardId: String, data: Map<String, Any?>): AacSymbol? {
    val label = data[FirestoreSchema.LABEL] as? String ?: return null
    val createdBy = data[FirestoreSchema.CREATED_BY] as? String ?: return null
    return AacSymbol(
        id = id,
        boardId = boardId,
        label = label,
        phrase = data[FirestoreSchema.PHRASE] as? String,
        image = symbolImageFrom(data[FirestoreSchema.IMAGE]),
        position = positionFrom(data[FirestoreSchema.POSITION]),
        createdBy = createdBy,
        color = SymbolColor.fromKey(data[FirestoreSchema.COLOR] as? String),
        wordClass = wordClassFromKey(data[FirestoreSchema.WORD_CLASS] as? String)
    )
}

internal fun savedMessageFrom(id: String, data: Map<String, Any?>): SavedMessage? {
    val text = data[FirestoreSchema.TEXT] as? String ?: return null
    return SavedMessage(
        id = id,
        text = text,
        language = data[FirestoreSchema.LANGUAGE] as? String,
        emoji = data[FirestoreSchema.EMOJI] as? String,
        position = (data[FirestoreSchema.POSITION] as? Number)?.toInt()
    )
}

/** Fields that an edit may change. Authorship, creation time and position stay as they are. */
internal fun SymbolDraft.toEditableFields(): Map<String, Any?> = mapOf(
    FirestoreSchema.LABEL to label,
    FirestoreSchema.PHRASE to phrase,
    FirestoreSchema.IMAGE to image?.toStoredImage(),
    FirestoreSchema.COLOR to color?.key,
    FirestoreSchema.WORD_CLASS to wordClass?.storedKey,
    FirestoreSchema.UPDATED_AT to FieldValue.serverTimestamp()
)

internal fun SymbolDraft.toNewDocument(createdBy: String, position: Int): Map<String, Any?> =
    toEditableFields() + mapOf(
        FirestoreSchema.POSITION to position,
        FirestoreSchema.CREATED_BY to createdBy,
        FirestoreSchema.CREATED_AT to FieldValue.serverTimestamp()
    )

internal fun newBoardDocument(
    name: String,
    position: Int,
    createdBy: String,
    language: String
): Map<String, Any?> = mapOf(
    FirestoreSchema.NAME to name,
    FirestoreSchema.LANGUAGE to language,
    FirestoreSchema.POSITION to position,
    FirestoreSchema.CREATED_BY to createdBy,
    FirestoreSchema.CREATED_AT to FieldValue.serverTimestamp(),
    FirestoreSchema.UPDATED_AT to FieldValue.serverTimestamp()
)
