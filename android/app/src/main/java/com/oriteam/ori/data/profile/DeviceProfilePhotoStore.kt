package com.oriteam.ori.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.photos.CloudPhoto
import com.oriteam.ori.domain.photos.CloudPhotoRepository
import com.oriteam.ori.domain.photos.PROFILE_PHOTO_ID
import com.oriteam.ori.domain.photos.PhotoImportError
import com.oriteam.ori.domain.photos.PhotoImportResult
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.domain.photos.StoredPhoto
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.profile.ProfilePhotoStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun photoKey(uid: String) = stringPreferencesKey("profile_photo_$uid")

/**
 * Uses the photo store shared with the symbols, which already reduces the picture, straightens it and
 * keeps it in the private storage of the app. With an account the picture lives in the account and
 * this device keeps a copy to work offline; the name of the copy changes with its content, so the
 * screen knows when to draw it again. Without an account it never leaves the device.
 */
class DeviceProfilePhotoStore @Inject constructor(
    private val photos: PhotoStore,
    private val cloud: CloudPhotoRepository,
    private val dataStore: DataStore<Preferences>
) : ProfilePhotoStore {

    /**
     * The path always comes from what this device stores. With an account, while someone shows the
     * picture, the copy follows the account; the new path then arrives like any other change.
     */
    override fun observePhoto(uid: String): Flow<String?> {
        val path = dataStore.data.map { it.pathFor(uid) }.distinctUntilChanged()
        if (uid == LOCAL_OWNER_UID) return path
        return channelFlow {
            launch {
                cloud.observe(uid, PROFILE_PHOTO_ID).collect { result ->
                    if (result is DataResult.Success) keepCopy(uid, result.value)
                }
            }
            path.collect { send(it) }
        }
    }

    private fun Preferences.pathFor(uid: String) = this[
        photoKey(
            uid
        )
    ]?.let { photos.pathOf(StoredPhoto(it)) }

    /** An empty answer from the cache proves nothing, so only the server can remove the copy. */
    private suspend fun keepCopy(uid: String, photo: CloudPhoto) {
        val bytes = photo.bytes
        if (bytes == null) {
            if (!photo.isFromCache) removeLocalCopy(uid)
            return
        }
        val copy = StoredPhoto("profile-$uid-${bytes.contentHashCode().toUInt().toString(radix = 16)}.jpg")
        if (photos.pathOf(copy) != null || !photos.saveBytes(copy, bytes)) return
        removeLocalCopy(uid)
        dataStore.edit { it[photoKey(uid)] = copy.fileName }
    }

    override suspend fun setPhoto(uid: String, sourceUri: String): PhotoImportError? =
        when (val result = photos.importPhoto(sourceUri)) {
            is PhotoImportResult.Failed -> result.error
            is PhotoImportResult.Imported ->
                if (uid == LOCAL_OWNER_UID) keepOnDevice(uid, result.photo) else sendToAccount(uid, result.photo)
        }

    private suspend fun keepOnDevice(uid: String, photo: StoredPhoto): PhotoImportError? {
        removeLocalCopy(uid)
        dataStore.edit { it[photoKey(uid)] = photo.fileName }
        return null
    }

    /** The imported file was only the way to get the reduced bytes: the copy comes from the account. */
    private suspend fun sendToAccount(uid: String, photo: StoredPhoto): PhotoImportError? {
        val bytes = photos.readBytes(photo)
        photos.delete(photo)
        if (bytes == null) return PhotoImportError.STORAGE_FAILED
        return PhotoImportError.UPLOAD_FAILED.takeIf {
            cloud.upload(
                uid,
                PROFILE_PHOTO_ID,
                bytes
            ) is DataResult.Failure
        }
    }

    override suspend fun removePhoto(uid: String) {
        if (uid != LOCAL_OWNER_UID) cloud.delete(uid, PROFILE_PHOTO_ID)
        removeLocalCopy(uid)
    }

    /** A picture the account already has is the one the person chose for it. */
    override suspend fun movePhoto(fromUid: String, toUid: String) {
        val moved = dataStore.data.first()[photoKey(fromUid)]?.let(::StoredPhoto) ?: return
        val existing = cloud.get(toUid, PROFILE_PHOTO_ID) as? DataResult.Success ?: return
        val bytes = photos.readBytes(moved)
        if (existing.value == null && bytes != null) {
            if (cloud.upload(toUid, PROFILE_PHOTO_ID, bytes) is DataResult.Failure) return
        }
        removeLocalCopy(fromUid)
    }

    private suspend fun removeLocalCopy(uid: String) {
        dataStore.data.first()[photoKey(uid)]?.let { photos.delete(StoredPhoto(it)) }
        dataStore.edit { it.remove(photoKey(uid)) }
    }
}
