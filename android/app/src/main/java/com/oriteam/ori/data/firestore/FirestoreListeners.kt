package com.oriteam.ori.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.ListSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Where Firestore calls the listeners of the app: one background thread, so turning documents into models
 * never competes with drawing the screen (the SDK uses the main thread by default), and the events of a
 * listener keep their order.
 */
internal val firestoreListenerExecutor: Executor = Executors.newSingleThreadExecutor { task ->
    Thread(task, "ori-firestore-listeners").apply { isDaemon = true }
}

/**
 * Follows a bounded query while the flow is collected and removes the listener when collection
 * stops (the screen left). Documents that [mapper] cannot read are skipped.
 *
 * Metadata changes (cache to server, pending to confirmed) are included: they cost no document
 * reads and let the screen tell the person whether the data is confirmed.
 */
/** The same result with its list turned into another snapshot type (a day, the series), keeping a failure as is. */
internal fun <T, R> DataResult<ListSnapshot<T>>.mapList(transform: (ListSnapshot<T>) -> R): DataResult<R> =
    when (this) {
        is DataResult.Failure -> this
        is DataResult.Success -> DataResult.Success(transform(value))
    }

internal fun <T : Any> Query.observeList(mapper: (DocumentSnapshot) -> T?): Flow<DataResult<ListSnapshot<T>>> =
    callbackFlow {
        val registration =
            addSnapshotListener(firestoreListenerExecutor, MetadataChanges.INCLUDE) { snapshot, exception ->
                val result = if (exception != null || snapshot == null) {
                    DataResult.Failure(exception?.let(::dataErrorFor) ?: DataError.UNKNOWN)
                } else {
                    DataResult.Success(
                        ListSnapshot(
                            items = snapshot.documents.mapNotNull(mapper),
                            isFromCache = snapshot.metadata.isFromCache,
                            hasPendingWrites = snapshot.metadata.hasPendingWrites()
                        )
                    )
                }
                trySend(result)
            }
        awaitClose { registration.remove() }
    }
