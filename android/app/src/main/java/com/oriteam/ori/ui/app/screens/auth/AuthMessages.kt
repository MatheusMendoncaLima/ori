package com.oriteam.ori.ui.app.screens.auth

import androidx.annotation.StringRes
import com.oriteam.ori.R
import com.oriteam.ori.domain.auth.AuthError
import com.oriteam.ori.domain.auth.CredentialsValidationError

/** User-facing text for each authentication error. `when` is exhaustive, so a new error cannot be forgotten. */
@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.INVALID_EMAIL -> R.string.error_invalid_email
    AuthError.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
    AuthError.EMAIL_ALREADY_IN_USE -> R.string.error_email_already_in_use
    AuthError.WEAK_PASSWORD -> R.string.error_weak_password
    AuthError.USER_DISABLED -> R.string.error_user_disabled
    AuthError.TOO_MANY_REQUESTS -> R.string.error_too_many_requests
    AuthError.NETWORK_UNAVAILABLE -> R.string.error_network_unavailable
    AuthError.UNKNOWN -> R.string.error_unknown
}

@StringRes
fun CredentialsValidationError.messageRes(): Int = when (this) {
    CredentialsValidationError.MISSING_SIGN_IN_FIELDS -> R.string.error_missing_sign_in_fields
    CredentialsValidationError.MISSING_RESET_EMAIL -> R.string.error_missing_reset_email
    CredentialsValidationError.MISSING_REGISTRATION_FIELDS -> R.string.error_missing_registration_fields
    CredentialsValidationError.PASSWORD_MISMATCH -> R.string.error_password_mismatch
    CredentialsValidationError.PASSWORD_TOO_SHORT -> R.string.error_password_too_short
}
