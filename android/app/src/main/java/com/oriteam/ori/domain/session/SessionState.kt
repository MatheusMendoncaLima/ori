package com.oriteam.ori.domain.session

import com.oriteam.ori.domain.auth.AuthenticatedUser
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.profile.ProfileLookup
import com.oriteam.ori.domain.profile.UserProfile

/**
 * Where the app stands between "who is this?" (authentication) and "what may they do?"
 * (authorization, decided by ownership and care links). Using the app without an account is
 * also [Active], with a profile that exists only on this device.
 */
sealed interface SessionState {
    /** The SDK is restoring the session or the profile is being read. */
    data object Initializing : SessionState

    data object SignedOut : SessionState

    /** Authenticated, but without a profile: the rules give no access to app data. */
    data class ProfileRequired(val user: AuthenticatedUser) : SessionState

    /** Authenticated, but the profile could not be read (network, permission). Retrying may help. */
    data class ProfileUnavailable(val user: AuthenticatedUser, val error: DataError) : SessionState

    data class Active(val profile: UserProfile) : SessionState
}

/** Combines identity and profile into one state. A null [lookup] means "still loading". */
fun sessionStateFor(user: AuthenticatedUser?, lookup: ProfileLookup?): SessionState = when {
    user == null -> SessionState.SignedOut
    lookup == null -> SessionState.Initializing
    else -> when (lookup) {
        is ProfileLookup.Found -> SessionState.Active(lookup.profile)
        ProfileLookup.Missing -> SessionState.ProfileRequired(user)
        is ProfileLookup.Failure -> SessionState.ProfileUnavailable(user, lookup.error)
    }
}
