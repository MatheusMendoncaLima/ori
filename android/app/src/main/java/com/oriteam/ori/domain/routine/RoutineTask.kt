package com.oriteam.ori.domain.routine

import java.time.LocalDate
import java.time.LocalTime

/**
 * One activity of a primary user's routine on a given [date]
 * (`users/{ownerUid}/routineTasks/{id}`).
 */
data class RoutineTask(
    val id: String,
    val ownerUid: String,
    val title: String,
    val date: LocalDate,
    /** Optional: activities without a time are listed after the timed ones. */
    val scheduledTime: LocalTime?,
    val description: String?,
    val status: TaskStatus,
    /** Whether the primary user may mark this activity as done. */
    val ownerCanComplete: Boolean,
    val isDone: Boolean,
    /** Who created it. Activities created by the caregiver are reserved to the caregiver. */
    val createdBy: String,
    /** Set when this is one day of an activity that repeats. */
    val repeat: TaskRepeat? = null
) {
    val isCreatedByOwner: Boolean get() = createdBy == ownerUid
}

/** What the person typed in the activity form, before it becomes a stored activity. */
data class ActivityDraft(
    val title: String,
    val date: LocalDate,
    val scheduledTime: LocalTime?,
    val description: String?,
    val ownerCanComplete: Boolean,
    /** Only the primary user can hide their own activities from the caregiver. */
    val isHiddenFromCaregiver: Boolean,
    /** Null: it happens only on [date]. Otherwise [date] is the first day. */
    val repeat: RepeatRule? = null
)

/** Limits shared with the Firestore rules (`functions/firestore.rules`). */
const val MAX_ACTIVITY_TITLE_LENGTH = 200
const val MAX_ACTIVITY_DESCRIPTION_LENGTH = 500

/**
 * Order shown for a day: timed activities first, by time; then those without a time,
 * alphabetically. Done activities keep their place so the day stays predictable.
 */
val dayOrder: Comparator<RoutineTask> =
    compareBy<RoutineTask, LocalTime?>(nullsLast()) { it.scheduledTime }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        .thenBy { it.id }

/** Archived activities are kept for history but are not part of the day. */
fun List<RoutineTask>.forDay(): List<RoutineTask> =
    filter { it.status != TaskStatus.ARCHIVED }.sortedWith(dayOrder)
