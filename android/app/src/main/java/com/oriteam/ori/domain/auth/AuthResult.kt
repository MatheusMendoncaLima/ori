package com.oriteam.ori.domain.auth

/** Outcome of an authentication operation. Failures are values, so callers must handle them explicitly. */
sealed interface AuthResult {
    data object Success : AuthResult

    data class Failure(val error: AuthError) : AuthResult
}
