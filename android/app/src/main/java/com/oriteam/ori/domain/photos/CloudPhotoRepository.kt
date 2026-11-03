package com.oriteam.ori.domain.photos

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow

/**
 * Largest stored photo. Firestore keeps a whole document in the cache and sends it whole, so this
 * is what one photo may cost: at most 100 photos × 100 KB, or 10 MB, for each person.
 * Also enforced by the rules (`functions/firestore.rules`).
 */
const val MAX_PHOTO_BYTES = 100 * 1024

/** Most photos per person, checked by the app: Firestore rules cannot count documents. */
const val MAX_PHOTOS_PER_PERSON = 100

/** The picture of a profile is one document, replaced when the picture changes. */
const val PROFILE_PHOTO_ID = "profile.jpg"

/** A photo as the account has it. Null [bytes] from the cache may only mean "not read yet". */
class CloudPhoto(val bytes: ByteArray?, val isFromCache: Boolean)

/** Photos in the person's account, so they follow the person to another device. */
interface CloudPhotoRepository {
    fun observe(ownerUid: String, photoId: String): Flow<DataResult<CloudPhoto>>

    /** Null when the account does not have this photo. */
    suspend fun get(ownerUid: String, photoId: String): DataResult<ByteArray?>

    /**
     * Refuses a photo over [MAX_PHOTO_BYTES] and, when offline is not in the way, one over
     * [MAX_PHOTOS_PER_PERSON] (`DataError.PRECONDITION_FAILED`). Without a connection it waits in the
     * queue, like any write.
     */
    suspend fun upload(ownerUid: String, photoId: String, bytes: ByteArray): DataResult<WriteStatus>

    suspend fun delete(ownerUid: String, photoId: String): DataResult<WriteStatus>
}
