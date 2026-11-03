package com.oriteam.ori.data.auth

import com.oriteam.ori.domain.auth.AuthError

/** Error codes documented in `FirebaseAuthException.getErrorCode()`. Unlisted codes become [AuthError.UNKNOWN]. */
private val authErrorsByFirebaseCode = mapOf(
    "ERROR_INVALID_EMAIL" to AuthError.INVALID_EMAIL,
    "ERROR_WRONG_PASSWORD" to AuthError.INVALID_CREDENTIALS,
    "ERROR_USER_NOT_FOUND" to AuthError.INVALID_CREDENTIALS,
    "ERROR_INVALID_CREDENTIAL" to AuthError.INVALID_CREDENTIALS,
    "ERROR_EMAIL_ALREADY_IN_USE" to AuthError.EMAIL_ALREADY_IN_USE,
    "ERROR_WEAK_PASSWORD" to AuthError.WEAK_PASSWORD,
    "ERROR_USER_DISABLED" to AuthError.USER_DISABLED,
    "ERROR_TOO_MANY_REQUESTS" to AuthError.TOO_MANY_REQUESTS,
    "ERROR_NETWORK_REQUEST_FAILED" to AuthError.NETWORK_UNAVAILABLE
)

internal fun authErrorFromFirebaseCode(errorCode: String): AuthError =
    authErrorsByFirebaseCode[errorCode] ?: AuthError.UNKNOWN
