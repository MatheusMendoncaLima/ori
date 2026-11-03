package com.oriteam.ori.domain.auth

import kotlinx.coroutines.flow.Flow

/** The identity proven by Firebase Authentication. It says who the user is, not what they may do. */
data class AuthenticatedUser(val uid: String, val displayName: String?)

/**
 * Contract between the app and the identity provider.
 *
 * Screens depend on this interface, not on Firebase, so their rules can be tested with a fake.
 * Implementations must never throw for expected failures; they return [AuthResult.Failure].
 */
interface AuthRepository {
    /**
     * Emits the signed-in user, or null when signed out. The SDK restores a valid session on
     * app start and refreshes tokens on its own, so users are not asked to log in again.
     */
    val signedInUser: Flow<AuthenticatedUser?>

    fun currentUser(): AuthenticatedUser?

    suspend fun signIn(email: String, password: String): AuthResult

    /** Creates the account and signs it in. The app profile is created separately. */
    suspend fun createAccount(name: String, email: String, password: String): AuthResult

    /**
     * Requests a password reset link. Succeeds even when no account uses [email],
     * so the result cannot be used to discover registered addresses.
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult

    fun signOut()
}
