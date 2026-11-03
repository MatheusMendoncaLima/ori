package com.oriteam.ori.data.routine

import com.google.firebase.firestore.FieldValue
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.keyFor
import com.oriteam.ori.domain.model.TaskStatus
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineSeries
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.repeatRuleFromStoredText
import com.oriteam.ori.domain.routine.toStoredText
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Dates and times are stored as text ("2026-10-29", "07:30"): the routine follows the person's
 * local calendar, not an instant, so a trip across time zones does not move activities.
 */
private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun LocalDate.toStoredDate(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)

internal fun LocalTime.toStoredTime(): String = format(timeFormat)

private fun String.toLocalDateOrNull(): LocalDate? =
    try {
        LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }

private fun String.toLocalTimeOrNull(): LocalTime? =
    try {
        LocalTime.parse(this, timeFormat)
    } catch (_: DateTimeParseException) {
        null
    }

/**
 * Converts a stored activity into the domain model. Documents without a valid title, date,
 * status or author are skipped (null) instead of being shown half-filled.
 */
internal fun routineTaskFrom(id: String, ownerUid: String, data: Map<String, Any?>): RoutineTask? {
    val title = data[FirestoreSchema.TITLE] as? String ?: return null
    val date = (data[FirestoreSchema.DATE] as? String)?.toLocalDateOrNull() ?: return null
    val status = FirestoreSchema.taskStatusValues.keyFor(data[FirestoreSchema.STATUS]) ?: return null
    val createdBy = data[FirestoreSchema.CREATED_BY] as? String ?: return null
    return RoutineTask(
        id = id,
        ownerUid = ownerUid,
        title = title,
        date = date,
        scheduledTime = (data[FirestoreSchema.SCHEDULED_TIME] as? String)?.toLocalTimeOrNull(),
        description = data[FirestoreSchema.DESCRIPTION] as? String,
        status = status,
        ownerCanComplete = data[FirestoreSchema.OWNER_CAN_COMPLETE] == true,
        isDone = data[FirestoreSchema.IS_DONE] == true,
        createdBy = createdBy
    )
}

/** Converts a stored series. A rule this version does not understand is skipped, never misread. */
internal fun routineSeriesFrom(id: String, ownerUid: String, data: Map<String, Any?>): RoutineSeries? {
    val task = routineTaskFrom(id, ownerUid, data) ?: return null
    val rule = repeatRuleFromStoredText(data[FirestoreSchema.REPEAT] as? String) ?: return null
    return RoutineSeries(
        id = id,
        ownerUid = ownerUid,
        title = task.title,
        startDate = task.date,
        scheduledTime = task.scheduledTime,
        description = task.description,
        status = task.status,
        ownerCanComplete = task.ownerCanComplete,
        createdBy = task.createdBy,
        rule = rule,
        skippedDates = (data[FirestoreSchema.SKIPPED_DATES] as? List<*>).orEmpty()
            .mapNotNull { (it as? String)?.toLocalDateOrNull() }
            .toSet()
    )
}

/** Fields that the activity form controls. Used by both creation and edition. */
internal fun ActivityDraft.toEditableFields(): Map<String, Any?> = mapOf(
    FirestoreSchema.TITLE to title.trim(),
    FirestoreSchema.DATE to date.toStoredDate(),
    FirestoreSchema.SCHEDULED_TIME to scheduledTime?.toStoredTime(),
    FirestoreSchema.DESCRIPTION to description?.trim()?.takeIf { it.isNotEmpty() },
    FirestoreSchema.OWNER_CAN_COMPLETE to ownerCanComplete,
    FirestoreSchema.STATUS to FirestoreSchema.taskStatusValues.getValue(
        if (isHiddenFromCaregiver) TaskStatus.HIDDEN_FROM_CAREGIVER else TaskStatus.ACTIVE
    ),
    FirestoreSchema.UPDATED_AT to FieldValue.serverTimestamp()
)

internal fun ActivityDraft.toSeriesFields(rule: RepeatRule): Map<String, Any?> =
    toEditableFields() + (FirestoreSchema.REPEAT to rule.toStoredText())

internal fun ActivityDraft.toNewSeriesDocument(rule: RepeatRule, createdBy: String): Map<String, Any?> =
    toSeriesFields(rule) + mapOf(
        FirestoreSchema.CREATED_BY to createdBy,
        FirestoreSchema.CREATED_AT to FieldValue.serverTimestamp(),
        FirestoreSchema.SKIPPED_DATES to emptyList<String>()
    )

/** Full document of a new activity: editable fields plus authorship and an empty completion. */
internal fun ActivityDraft.toNewDocument(createdBy: String): Map<String, Any?> = toEditableFields() + mapOf(
    FirestoreSchema.CREATED_BY to createdBy,
    FirestoreSchema.CREATED_AT to FieldValue.serverTimestamp(),
    FirestoreSchema.IS_DONE to false,
    FirestoreSchema.COMPLETED_AT to null,
    FirestoreSchema.COMPLETED_BY to null
)
