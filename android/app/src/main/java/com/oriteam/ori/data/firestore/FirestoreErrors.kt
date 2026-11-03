package com.oriteam.ori.data.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import kotlinx.coroutines.CancellationException

private const val TAG = "OriData"

private val dataErrorsByFirestoreCode = mapOf(
    FirebaseFirestoreException.Code.PERMISSION_DENIED to DataError.PERMISSION_DENIED,
    FirebaseFirestoreException.Code.UNAUTHENTICATED to DataError.PERMISSION_DENIED,
    FirebaseFirestoreException.Code.UNAVAILABLE to DataError.NETWORK_UNAVAILABLE,
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED to DataError.NETWORK_UNAVAILABLE,
    FirebaseFirestoreException.Code.NOT_FOUND to DataError.NOT_FOUND,
    FirebaseFirestoreException.Code.FAILED_PRECONDITION to DataError.PRECONDITION_FAILED,
    FirebaseFirestoreException.Code.ALREADY_EXISTS to DataError.CONFLICT,
    FirebaseFirestoreException.Code.INVALID_ARGUMENT to DataError.INVALID_INPUT
)

private val dataErrorsByFunctionsCode = mapOf(
    FirebaseFunctionsException.Code.PERMISSION_DENIED to DataError.PERMISSION_DENIED,
    FirebaseFunctionsException.Code.UNAUTHENTICATED to DataError.PERMISSION_DENIED,
    FirebaseFunctionsException.Code.UNAVAILABLE to DataError.NETWORK_UNAVAILABLE,
    FirebaseFunctionsException.Code.DEADLINE_EXCEEDED to DataError.NETWORK_UNAVAILABLE,
    FirebaseFunctionsException.Code.NOT_FOUND to DataError.NOT_FOUND,
    FirebaseFunctionsException.Code.FAILED_PRECONDITION to DataError.PRECONDITION_FAILED,
    FirebaseFunctionsException.Code.ALREADY_EXISTS to DataError.CONFLICT,
    FirebaseFunctionsException.Code.INVALID_ARGUMENT to DataError.INVALID_INPUT
)

internal fun dataErrorFor(code: FirebaseFirestoreException.Code): DataError =
    dataErrorsByFirestoreCode[code] ?: DataError.UNKNOWN

internal fun dataErrorFor(code: FirebaseFunctionsException.Code): DataError =
    dataErrorsByFunctionsCode[code] ?: DataError.UNKNOWN

/** Maps any SDK failure to a [DataError]. Logs only the error code, never document content. */
internal fun dataErrorFor(exception: Exception): DataError {
    val error = when (exception) {
        is FirebaseFirestoreException -> dataErrorFor(exception.code)
        is FirebaseFunctionsException -> dataErrorFor(exception.code)
        else -> DataError.UNKNOWN
    }
    Log.w(TAG, "Data operation failed: ${exception.javaClass.simpleName} -> $error")
    return error
}

/** Runs a Firestore or Functions call and converts failures into [DataResult.Failure]. */
@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> runDataOperation(operation: suspend () -> T): DataResult<T> = try {
    DataResult.Success(operation())
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Exception) {
    DataResult.Failure(dataErrorFor(exception))
}
