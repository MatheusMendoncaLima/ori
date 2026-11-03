package com.oriteam.ori.ui.components.state

import androidx.annotation.StringRes

/**
 * The four situations of any screen that loads data. A ViewModel exposes exactly one of them,
 * so the screen can never show a spinner and an error at the same time.
 *
 * [Empty] is a successful load with nothing to show; it is not used for failures, and an
 * authorization failure must be an [Error], never an [Empty] list.
 */
sealed interface ContentState<out T> {
    data object Loading : ContentState<Nothing>

    data object Empty : ContentState<Nothing>

    data class Error(@StringRes val messageRes: Int, val canRetry: Boolean = true) : ContentState<Nothing>

    data class Content<T>(val value: T) : ContentState<T>
}
