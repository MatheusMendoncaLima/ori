package com.oriteam.ori.ui.app.screens.aac

import com.oriteam.ori.R
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.ListSnapshot
import com.oriteam.ori.ui.app.screens.common.toContentState
import com.oriteam.ori.ui.components.state.ContentState

/** What the organizer read, kept with the rows the screen shows, so the actions find the full items. */
internal data class Listed<T, R>(
    val items: List<T> = emptyList(),
    val rows: ContentState<List<OrganizerItemUiModel<R>>> = ContentState.Loading,
    val noticeRes: Int? = null
)

/**
 * Each row knows whether it can move up or down, so the arrows can be disabled at the ends. [shown] is the list as
 * the screen shows it (the suggested board in the app's language); the actions then work on what the person sees.
 */
internal fun <T, R> listed(
    result: DataResult<ListSnapshot<T>>,
    order: Comparator<in T>,
    shown: (List<T>) -> List<T> = { it },
    model: (T) -> R
): Listed<T, R> {
    val ordered = shown((result as? DataResult.Success)?.value?.items.orEmpty().sortedWith(order))
    val rows = ordered.mapIndexed { index, item ->
        OrganizerItemUiModel(model(item), index > 0, index < ordered.lastIndex)
    }
    return Listed(ordered, result.toContentState { rows }, result.syncNotice())
}

private fun <T> DataResult<ListSnapshot<T>>.syncNotice(): Int? {
    val snapshot = (this as? DataResult.Success)?.value ?: return null
    return when {
        snapshot.isFromCache -> R.string.sync_showing_saved_data
        snapshot.hasPendingWrites -> R.string.sync_pending_changes
        else -> null
    }
}
