package com.oriteam.ori.ui.app.screens.account

import androidx.annotation.StringRes
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.local.LocalDataMoveResult

/** What the profile asks of the session; grouped so the screens receive one value. */
data class ProfileActions(
    val localDataMove: LocalDataMoveResult?,
    val onRetryLocalDataMove: () -> Unit,
    val onSignOut: () -> Unit,
    val onEraseLocalData: (onResult: (DataError?) -> Unit) -> Unit,
    val onRename: (name: String, onResult: (DataError?) -> Unit) -> Unit,
    val photoPath: String? = null,
    @StringRes val photoErrorRes: Int? = null,
    val onPhotoPicked: (sourceUri: String) -> Unit = {},
    val onRemovePhoto: () -> Unit = {}
)
