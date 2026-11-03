package com.oriteam.ori.data.firestore

import com.oriteam.ori.domain.carelink.CareLink
import com.oriteam.ori.domain.carelink.CareLinkPermissions
import com.oriteam.ori.domain.carelink.CareLinkStatus
import com.oriteam.ori.domain.model.TaskStatus
import com.oriteam.ori.domain.profile.UserProfile

/**
 * Names and values stored in Firestore. They are a persisted contract shared with
 * `functions/firestore.rules` and the Cloud Functions: changing one requires a migration.
 */
internal object FirestoreSchema {
    const val USERS = "users"
    const val ROUTINE_TASKS = "routineTasks"
    const val ROUTINE_SERIES = "routineSeries"
    const val PHOTOS = "photos"
    const val PHOTO_DATA = "data"
    const val CARE_LINKS = "careLinks"
    const val SETTINGS = "settings"
    const val PREFERENCES_DOCUMENT = "preferences"
    const val AAC_BOARDS = "aacBoards"
    const val AAC_SYMBOLS = "symbols"
    const val AAC_SAVED_MESSAGES = "aacSavedMessages"

    const val DISPLAY_NAME = "displayName"
    const val CREATED_AT = "createdAt"

    const val TITLE = "title"
    const val DATE = "date"
    const val SCHEDULED_TIME = "scheduledTime"
    const val DESCRIPTION = "description"
    const val CREATED_BY = "createdBy"
    const val UPDATED_AT = "updatedAt"
    const val STATUS = "status"
    const val OWNER_CAN_COMPLETE = "ownerCanComplete"
    const val IS_DONE = "isDone"
    const val COMPLETED_AT = "completedAt"
    const val COMPLETED_BY = "completedBy"
    const val REPEAT = "repeat"
    const val SKIPPED_DATES = "skippedDates"

    const val TEXT_SCALE = "textScale"
    const val THEME = "theme"
    const val SIMPLIFIED = "simplified"
    const val MOTION = "motion"

    const val NAME = "name"
    const val POSITION = "position"
    const val LABEL = "label"
    const val PHRASE = "phrase"
    const val IMAGE = "image"
    const val TEXT = "text"

    /** A symbol image is stored as a short reference with a type prefix, never as image bytes. */
    const val EMOJI_IMAGE_PREFIX = "emoji:"
    const val DEVICE_PHOTO_IMAGE_PREFIX = "device:"

    const val CAREGIVER_UID = "caregiverUid"
    const val PRIMARY_USER_UID = "primaryUserUid"
    const val CAREGIVER_NAME = "caregiverName"
    const val PRIMARY_USER_NAME = "primaryUserName"
    const val PERMISSIONS = "permissions"
    const val VIEW_ROUTINES = "viewRoutines"
    const val MANAGE_ROUTINES = "manageRoutines"
    const val MANAGE_COMMUNICATION = "manageCommunication"
    const val REVOKED_AT = "revokedAt"

    val taskStatusValues = mapOf(
        TaskStatus.ACTIVE to "active",
        TaskStatus.HIDDEN_FROM_CAREGIVER to "hidden_from_caregiver",
        TaskStatus.ARCHIVED to "archived"
    )
    val linkStatusValues = mapOf(CareLinkStatus.ACTIVE to "active", CareLinkStatus.REVOKED to "revoked")

    fun linkId(caregiverUid: String, primaryUserUid: String) = "${caregiverUid}_$primaryUserUid"
}

internal fun <T> Map<T, String>.keyFor(value: Any?): T? = entries.firstOrNull { it.value == value }?.key

/**
 * Converts stored documents into domain models. A document with a missing or unknown value
 * returns null instead of a half-filled model, so the app never acts on corrupted data.
 */
internal fun profileFrom(uid: String, data: Map<String, Any?>): UserProfile? {
    val name = data[FirestoreSchema.DISPLAY_NAME] as? String ?: return null
    return UserProfile(uid = uid, displayName = name)
}

internal fun careLinkFrom(id: String, data: Map<String, Any?>): CareLink? {
    val status = FirestoreSchema.linkStatusValues.keyFor(data[FirestoreSchema.STATUS])
    val caregiverUid = data[FirestoreSchema.CAREGIVER_UID] as? String
    val primaryUserUid = data[FirestoreSchema.PRIMARY_USER_UID] as? String
    if (status == null || caregiverUid == null || primaryUserUid == null) return null
    val permissions = data[FirestoreSchema.PERMISSIONS] as? Map<*, *>
    return CareLink(
        id = id,
        caregiverUid = caregiverUid,
        primaryUserUid = primaryUserUid,
        caregiverName = data[FirestoreSchema.CAREGIVER_NAME] as? String ?: "",
        primaryUserName = data[FirestoreSchema.PRIMARY_USER_NAME] as? String ?: "",
        status = status,
        // Missing permissions mean "no permission": access is denied by default.
        permissions = CareLinkPermissions(
            viewRoutines = permissions?.get(FirestoreSchema.VIEW_ROUTINES) == true,
            manageRoutines = permissions?.get(FirestoreSchema.MANAGE_ROUTINES) == true,
            // Links created before this permission existed do not have it: denied.
            manageCommunication = permissions?.get(FirestoreSchema.MANAGE_COMMUNICATION) == true
        )
    )
}
