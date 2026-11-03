package com.oriteam.ori.ui.app.screens.common

import androidx.annotation.StringRes
import com.oriteam.ori.R
import com.oriteam.ori.domain.photos.PhotoImportError

/** Text for a photo that could not be kept, for a symbol or for the profile. */
@StringRes
fun PhotoImportError.messageRes(): Int = when (this) {
    PhotoImportError.UNREADABLE -> R.string.photo_error_unreadable
    PhotoImportError.STORAGE_FAILED -> R.string.photo_error_storage
    PhotoImportError.UPLOAD_FAILED -> R.string.photo_error_upload
}
