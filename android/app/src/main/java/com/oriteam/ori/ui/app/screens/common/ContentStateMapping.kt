package com.oriteam.ori.ui.app.screens.common

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.ui.components.state.ContentState

/**
 * Turns a list read into a screen state. A failure (including a permission error after a
 * revoked link) becomes [ContentState.Error], never an empty list.
 */
fun <T, R> DataResult<T>.toContentState(transform: (T) -> List<R>): ContentState<List<R>> = when (this) {
    is DataResult.Failure -> ContentState.Error(error.messageRes())
    is DataResult.Success -> transform(value).let { if (it.isEmpty()) ContentState.Empty else ContentState.Content(it) }
}

fun <T, R> ContentState<T>.mapContent(transform: (T) -> R): ContentState<R> = when (this) {
    is ContentState.Content -> ContentState.Content(transform(value))
    ContentState.Empty -> ContentState.Empty
    ContentState.Loading -> ContentState.Loading
    is ContentState.Error -> this
}
