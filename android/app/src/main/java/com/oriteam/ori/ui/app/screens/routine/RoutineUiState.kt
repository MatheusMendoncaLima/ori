package com.oriteam.ori.ui.app.screens.routine

import androidx.annotation.StringRes
import com.oriteam.ori.ui.components.state.ContentState
import java.time.LocalDate

data class RoutineUiState(
    /** Day being shown. Survives rotation and process death (SavedStateHandle). */
    val date: LocalDate,
    /** Days between [date] and today: 0 is today, -1 yesterday, 1 tomorrow. */
    val daysFromToday: Long = 0,
    val content: ContentState<List<RoutineStepUiModel>> = ContentState.Loading,
    val canCreate: Boolean = false,
    /** Failure of the last toggle or deletion, shown without hiding the routine. */
    @StringRes val actionErrorRes: Int? = null,
    /** Offline or unsynchronized data notice; null when everything matches the server. */
    @StringRes val syncNoticeRes: Int? = null,
    /** The query limit was reached, so some activities may not be shown. */
    val isTruncated: Boolean = false,
    /** Activity waiting for the person to confirm its deletion. */
    val pendingDeletion: RoutineStepUiModel? = null,
    /** Days of the open month that have activities, marked with a dot; empty while the week is shown. */
    val daysWithActivities: Set<LocalDate> = emptySet(),
    /**
     * The day the activities on screen belong to. Right after another day is chosen it is still the previous
     * one, for the few milliseconds the new day takes to arrive: the list is replaced as a whole when it changes.
     */
    val contentDate: LocalDate? = null
) {
    /** Today, for the week strip. */
    val today: LocalDate get() = date.minusDays(daysFromToday)

    /** [id] may also be the series: the form saves a series, and the day shows one of its occurrences. */
    fun step(id: String): RoutineStepUiModel? =
        (content as? ContentState.Content)?.value?.firstOrNull { it.id == id || it.seriesId == id }
}
