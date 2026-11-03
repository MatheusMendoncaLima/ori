package com.oriteam.ori.ui.app.screens.routine

import androidx.annotation.StringRes
import com.oriteam.ori.R
import com.oriteam.ori.domain.routine.ActivityValidationError

/** User-facing text for each form problem. `when` is exhaustive, so a new error cannot be forgotten. */
@StringRes
fun ActivityValidationError.messageRes(): Int = when (this) {
    ActivityValidationError.MISSING_TITLE -> R.string.activity_error_missing_title
    ActivityValidationError.TITLE_TOO_LONG -> R.string.activity_error_title_too_long
    ActivityValidationError.INVALID_TIME -> R.string.activity_error_invalid_time
    ActivityValidationError.DESCRIPTION_TOO_LONG -> R.string.activity_error_description_too_long
    ActivityValidationError.DUPLICATE -> R.string.activity_error_duplicate
}

/** Relative names for nearby days; other days show only the full date. */
val relativeDayLabels: Map<Long, Int> = mapOf(
    -1L to R.string.day_yesterday,
    0L to R.string.day_today,
    1L to R.string.day_tomorrow
)
