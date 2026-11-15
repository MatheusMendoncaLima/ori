package com.oriteam.ori.ui.app.screens.common

import androidx.annotation.StringRes
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.DataError

/** Generic user-facing text for data errors. Features may give a more specific text first. */
@StringRes
fun DataError.messageRes(): Int = when (this) {
    DataError.PERMISSION_DENIED -> R.string.error_permission_denied
    DataError.NETWORK_UNAVAILABLE -> R.string.error_network_unavailable
    DataError.NOT_FOUND -> R.string.error_not_found
    DataError.INVALID_INPUT -> R.string.error_invalid_input
    DataError.PRECONDITION_FAILED, DataError.CONFLICT -> R.string.error_action_not_possible
    DataError.SERVICE_UNAVAILABLE -> R.string.error_service_unavailable
    DataError.UNKNOWN -> R.string.error_unknown
}
