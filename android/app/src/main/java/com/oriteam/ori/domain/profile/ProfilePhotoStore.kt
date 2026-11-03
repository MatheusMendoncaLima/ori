package com.oriteam.ori.domain.profile

import com.oriteam.ori.domain.photos.PhotoImportError
import kotlinx.coroutines.flow.Flow

/**
 * The picture of each profile. With an account it lives in the account, so every device of the person
 * shows it, and this device keeps a copy to work offline. Without an account it never leaves the device.
 */
interface ProfilePhotoStore {
    /** Path of the picture, or null while the profile has none. */
    fun observePhoto(uid: String): Flow<String?>

    /** Replaces the picture with a reduced copy of [sourceUri]; null when it worked. */
    suspend fun setPhoto(uid: String, sourceUri: String): PhotoImportError?

    suspend fun removePhoto(uid: String)

    /** After the device data moved to an account, the picture stays with the person. */
    suspend fun movePhoto(fromUid: String, toUid: String)
}
