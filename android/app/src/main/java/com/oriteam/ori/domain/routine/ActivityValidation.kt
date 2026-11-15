package com.oriteam.ori.domain.routine

import java.time.LocalTime

/** Problems found in the activity form before anything is saved. */
enum class ActivityValidationError {
    MISSING_TITLE,
    TITLE_TOO_LONG,
    INVALID_TIME,
    DESCRIPTION_TOO_LONG,

    /** Same title, date and time as another activity of the same person. */
    DUPLICATE
}

/** Result of reading the time field. Empty is valid: the activity has no fixed time. */
sealed interface TimeInput {
    data object Empty : TimeInput

    data class Valid(val time: LocalTime) : TimeInput

    data object Invalid : TimeInput
}

/** Hours and minutes with ":" or "h" between them ("8:30", "8h30"), and AM/PM for the 12-hour clock. */
private val timePattern = Regex("""^(\d{1,2})[:hH](\d{2})\s*(?:([aApP])\.?\s?[mM]\.?)?$""")
private const val LAST_HOUR = 23
private const val LAST_MINUTE = 59
private const val NOON = 12

/**
 * Accepts the ways people write a time: "8:30", "08:30" and "8h30" on the 24-hour clock, and "8:30 PM" or
 * "8:30pm" on the 12-hour one, as in American English. With AM/PM the hour goes from 1 to 12.
 */
fun parseScheduledTime(text: String): TimeInput {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return TimeInput.Empty
    val time = timePattern.matchEntire(trimmed)?.destructured?.let { (hourText, minuteText, period) ->
        val hour = hourOf(hourText.toInt(), period.lowercase())
        val minute = minuteText.toInt()
        if (hour != null && hour in 0..LAST_HOUR && minute in 0..LAST_MINUTE) LocalTime.of(hour, minute) else null
    }
    return time?.let(TimeInput::Valid) ?: TimeInput.Invalid
}

/** The hour on the 24-hour clock; null for a 12-hour one out of 1..12 ("13 PM"). */
private fun hourOf(hour: Int, period: String): Int? = when (period) {
    "" -> hour
    "a" -> if (hour in 1..NOON) hour % NOON else null
    else -> if (hour in 1..NOON) hour % NOON + NOON else null
}

/** Two activities are the same when title (ignoring case and spaces), date and time match. */
fun ActivityDraft.isDuplicateOf(task: RoutineTask): Boolean =
    task.date == date &&
        task.scheduledTime == scheduledTime &&
        task.title.normalizedTitle() == title.normalizedTitle()

private fun String.normalizedTitle() = trim().replace(Regex("\\s+"), " ").lowercase()

/** A day of a repeating activity is the activity: editing the series must not find its own days. */
private fun RoutineTask.isBeingEdited(editingId: String?) =
    id == editingId || (editingId != null && repeat?.seriesId == editingId)

/**
 * Validates a draft against the activities already on its date. [editingId] is the activity
 * being edited, which must not count as its own duplicate. Checks run in reading order, so the
 * person sees the first field to fix.
 */
fun validateActivity(
    draft: ActivityDraft,
    activitiesOnDate: List<RoutineTask>,
    editingId: String? = null
): ActivityValidationError? = when {
    draft.title.isBlank() -> ActivityValidationError.MISSING_TITLE
    draft.title.trim().length > MAX_ACTIVITY_TITLE_LENGTH -> ActivityValidationError.TITLE_TOO_LONG
    (draft.description?.trim()?.length ?: 0) > MAX_ACTIVITY_DESCRIPTION_LENGTH ->
        ActivityValidationError.DESCRIPTION_TOO_LONG
    activitiesOnDate.any { !it.isBeingEdited(editingId) && draft.isDuplicateOf(it) } ->
        ActivityValidationError.DUPLICATE
    else -> null
}
