package com.oriteam.ori.domain.auth

/**
 * Authentication failures the app knows how to explain to the user.
 *
 * Technical details stay in the data layer; the interface only maps these values to messages.
 */
enum class AuthError {
    INVALID_EMAIL,

    /** Wrong password or unknown account: kept together so the screen never reveals which accounts exist. */
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    USER_DISABLED,
    TOO_MANY_REQUESTS,
    NETWORK_UNAVAILABLE,
    UNKNOWN
}
