package com.oriteam.ori.ui.app.screens.routine

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.oriteam.ori.R
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.domain.routine.TaskStatus
import com.oriteam.ori.domain.routine.canDeleteActivity
import com.oriteam.ori.domain.routine.canEditActivity
import com.oriteam.ori.domain.routine.canToggleCompletion
import java.time.LocalDate
import java.time.LocalTime

/** Completion shown to the person. Each value carries its spoken and visible label. */
enum class ActivityProgress(@StringRes val labelRes: Int) {
    PENDING(R.string.routine_step_pending),
    DONE(R.string.routine_step_done);

    companion object {
        fun of(isDone: Boolean) = if (isDone) DONE else PENDING
    }
}

/** Extra facts about an activity, shown as small labels on the card and in the details. */
enum class ActivityBadge(@StringRes val labelRes: Int) {
    HIDDEN_FROM_CAREGIVER(R.string.activity_badge_hidden),
    SET_BY_CAREGIVER(R.string.activity_badge_set_by_caregiver),
    REPEATS(R.string.activity_badge_repeats)
}

/** What the routine screens need to draw one activity. Built by [RoutineViewModel]. */
@Immutable
data class RoutineStepUiModel(
    val id: String,
    val title: String,
    /** Written by the screen, in the format of the language and of the device's 12/24-hour choice. */
    val time: LocalTime? = null,
    val isDone: Boolean = false,
    val isCurrent: Boolean = false,
    /** False when the viewer may not mark this activity; the toggle is then read-only. */
    val canToggle: Boolean = true,
    val description: String? = null,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val badges: List<ActivityBadge> = emptyList(),
    /** Set on one day of a repeating activity. */
    val seriesId: String? = null,
    val repeatRule: RepeatRule? = null,
    val repeatStart: LocalDate? = null
) {
    val progress: ActivityProgress get() = ActivityProgress.of(isDone)
}

/** What a card shows of an activity, and what [viewer] may do with it. */
internal fun RoutineTask.toUiModel(viewer: RoutineViewer) = RoutineStepUiModel(
    id = id,
    title = title,
    time = scheduledTime,
    isDone = isDone,
    canToggle = canToggleCompletion(this, viewer),
    description = description,
    canEdit = canEditActivity(this, viewer),
    canDelete = canDeleteActivity(this, viewer),
    badges = listOfNotNull(
        ActivityBadge.HIDDEN_FROM_CAREGIVER.takeIf { status == TaskStatus.HIDDEN_FROM_CAREGIVER },
        ActivityBadge.SET_BY_CAREGIVER.takeIf { viewer == RoutineViewer.Owner && !isCreatedByOwner },
        ActivityBadge.REPEATS.takeIf { repeat != null }
    ),
    seriesId = repeat?.seriesId,
    repeatRule = repeat?.rule,
    repeatStart = repeat?.startDate
)
