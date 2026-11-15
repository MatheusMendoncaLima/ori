package com.oriteam.ori.data.photos

import android.util.Log
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.awaitWriteAcknowledgement
import com.oriteam.ori.data.firestore.dataErrorFor
import com.oriteam.ori.data.firestore.firestoreListenerExecutor
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.photos.CloudPhoto
import com.oriteam.ori.domain.photos.CloudPhotoRepository
import com.oriteam.ori.domain.photos.MAX_PHOTOS_PER_PERSON
import com.oriteam.ori.domain.photos.MAX_PHOTO_BYTES
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "OriPhotos"

/**
 * Photos in `users/{owner}/photos/{id}`, one document each, as bytes. They are not inside the
 * symbols because a board is read as a list: 60 photos in it would come along every time.
 */
class FirestoreCloudPhotoRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : CloudPhotoRepository {

    private fun photosOf(ownerUid: String) = firestore.collection(FirestoreSchema.USERS)
        .document(ownerUid)
        .collection(FirestoreSchema.PHOTOS)

    override fun observe(ownerUid: String, photoId: String): Flow<DataResult<CloudPhoto>> = callbackFlow {
        val registration = photosOf(
            ownerUid
        ).document(photoId).addSnapshotListener(firestoreListenerExecutor) { snapshot, exception ->
            val result = if (exception != null || snapshot == null) {
                DataResult.Failure(exception?.let(::dataErrorFor) ?: DataError.UNKNOWN)
            } else {
                DataResult.Success(
                    CloudPhoto(snapshot.getBlob(FirestoreSchema.PHOTO_DATA)?.toBytes(), snapshot.metadata.isFromCache)
                )
            }
            trySend(result)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun get(ownerUid: String, photoId: String): DataResult<ByteArray?> = runDataOperation {
        photosOf(ownerUid).document(photoId).get().await().getBlob(FirestoreSchema.PHOTO_DATA)?.toBytes()
    }

    override suspend fun upload(ownerUid: String, photoId: String, bytes: ByteArray): DataResult<WriteStatus> {
        if (bytes.size > MAX_PHOTO_BYTES || isAtPhotoLimit(ownerUid)) {
            return DataResult.Failure(DataError.PRECONDITION_FAILED)
        }
        return runDataOperation {
            val write = photosOf(ownerUid).document(photoId).set(
                mapOf(
                    FirestoreSchema.PHOTO_DATA to Blob.fromBytes(bytes),
                    FirestoreSchema.UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
            awaitWriteAcknowledgement { write.await() }
        }
    }

    /** Counting needs the server. Without one the photo waits in the queue, and is not refused. */
    private suspend fun isAtPhotoLimit(ownerUid: String): Boolean = try {
        val counted = photosOf(ownerUid).limit(MAX_PHOTOS_PER_PERSON + 1L).count()
            .get(AggregateSource.SERVER).await().count
        counted >= MAX_PHOTOS_PER_PERSON
    } catch (exception: FirebaseFirestoreException) {
        Log.w(TAG, "Photos could not be counted: ${exception.code}")
        false
    }

    override suspend fun delete(ownerUid: String, photoId: String): DataResult<WriteStatus> = runDataOperation {
        val write = photosOf(ownerUid).document(photoId).delete()
        awaitWriteAcknowledgement { write.await() }
    }
}
