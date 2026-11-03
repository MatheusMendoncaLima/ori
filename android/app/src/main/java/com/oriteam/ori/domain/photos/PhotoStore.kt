package com.oriteam.ori.domain.photos

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus

/**
 * A photo the app keeps: a reduced copy named [fileName] in the private storage of the device and, with
 * an account, in a document of its own. Symbol pictures and the profile picture are both kept this way.
 */
data class StoredPhoto(val fileName: String)

/** Why a photo could not be kept. */
enum class PhotoImportError {
    /** The file is not an image the device can decode, or it could not be opened. */
    UNREADABLE,

    /** The device could not write the reduced copy (for example, storage is full). */
    STORAGE_FAILED,

    /** The account did not accept the picture. */
    UPLOAD_FAILED
}

sealed interface PhotoImportResult {
    data class Imported(val photo: StoredPhoto) : PhotoImportResult

    data class Failed(val error: PhotoImportError) : PhotoImportResult
}

/**
 * Photos chosen by the person, for symbols and for the profile. They are reduced to at most
 * [MAX_PHOTO_BYTES] and kept in the app storage of this device. With an account the same bytes go to the
 * account ([CloudPhotoRepository]), and another device fetches them when it needs one; without an account
 * they stay here.
 */
interface PhotoStore {
    /** Copies and reduces the picture at [sourceUri] (from the system photo picker). */
    suspend fun importPhoto(sourceUri: String): PhotoImportResult

    /** Absolute path of the stored file, or null when this device does not have it. */
    fun pathOf(photo: StoredPhoto): String?

    /** Removes a stored photo. Missing files are ignored: the goal is that it no longer exists. */
    suspend fun delete(photo: StoredPhoto)

    suspend fun readBytes(photo: StoredPhoto): ByteArray?

    suspend fun saveBytes(photo: StoredPhoto, bytes: ByteArray): Boolean

    /** Sends the photo to the account of [ownerUid]. Without an account there is nothing to do. */
    suspend fun upload(ownerUid: String, photo: StoredPhoto): DataResult<WriteStatus>

    /** Removes it from this device and from the account. */
    suspend fun deleteEverywhere(ownerUid: String, photo: StoredPhoto)

    /** Brings from the account the photos this device does not have; true when any arrived. */
    suspend fun fetchMissing(ownerUid: String, photos: List<StoredPhoto>): Boolean
}
