package com.oriteam.ori.domain.common

/**
 * Why a data operation failed. [PERMISSION_DENIED] is never turned into "empty": the screen
 * must tell the user that access was refused (for example, after a care link was revoked).
 */
enum class DataError {
    PERMISSION_DENIED,
    NETWORK_UNAVAILABLE,
    NOT_FOUND,
    INVALID_INPUT,
    PRECONDITION_FAILED,
    CONFLICT,
    UNKNOWN
}

/**
 * Where an accepted write stands. Writes are applied on the device first; [PENDING_SYNC] means
 * the server has not confirmed yet (usually offline) and the SDK will send it later.
 */
enum class WriteStatus { SYNCED, PENDING_SYNC }

/** Outcome of reading or writing app data. Failures are values that callers must handle. */
sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>

    data class Failure(val error: DataError) : DataResult<Nothing>
}
