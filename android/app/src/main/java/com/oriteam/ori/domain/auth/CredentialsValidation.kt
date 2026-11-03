package com.oriteam.ori.domain.auth

/** Minimum length accepted by Firebase Authentication for e-mail/password accounts. */
const val MIN_PASSWORD_LENGTH = 6

/** Input problems detected before any request reaches the identity provider. */
enum class CredentialsValidationError {
    MISSING_SIGN_IN_FIELDS,
    MISSING_RESET_EMAIL,
    MISSING_REGISTRATION_FIELDS,
    PASSWORD_MISMATCH,
    PASSWORD_TOO_SHORT
}

fun validateSignIn(email: String, password: String): CredentialsValidationError? =
    if (email.isBlank() || password.isBlank()) CredentialsValidationError.MISSING_SIGN_IN_FIELDS else null

fun validatePasswordReset(email: String): CredentialsValidationError? =
    if (email.isBlank()) CredentialsValidationError.MISSING_RESET_EMAIL else null

/** Checks run in order, so the user always sees the first problem to fix. */
fun validateRegistration(
    name: String,
    email: String,
    password: String,
    passwordConfirmation: String
): CredentialsValidationError? = when {
    name.isBlank() || email.isBlank() || password.isBlank() ->
        CredentialsValidationError.MISSING_REGISTRATION_FIELDS
    password != passwordConfirmation -> CredentialsValidationError.PASSWORD_MISMATCH
    password.length < MIN_PASSWORD_LENGTH -> CredentialsValidationError.PASSWORD_TOO_SHORT
    else -> null
}
