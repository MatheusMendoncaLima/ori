package com.oriteam.ori.domain.auth

import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.profile.ProfileRepository
import javax.inject.Inject

/** Outcome of registration, which spans two systems: Authentication and the profile in Firestore. */
sealed interface RegistrationResult {
    data object Completed : RegistrationResult

    /** The account exists and is signed in, but has no profile yet; the app will ask for it again. */
    data class ProfileNotSaved(val error: DataError) : RegistrationResult

    data class Failed(val error: AuthError) : RegistrationResult
}

/**
 * Creates the account and then its profile.
 *
 * The two steps cannot be atomic. If the second fails, the session shows the profile screen,
 * so the user can finish without creating a second account.
 */
class RegisterAccount @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String
    ): RegistrationResult {
        val creation = authRepository.createAccount(name, email, password)
        val user = authRepository.currentUser()
        return when {
            creation is AuthResult.Failure -> RegistrationResult.Failed(creation.error)
            user == null -> RegistrationResult.Failed(AuthError.UNKNOWN)
            else -> when (val profile = profileRepository.createProfile(user.uid, name)) {
                is DataResult.Success -> RegistrationResult.Completed
                is DataResult.Failure -> RegistrationResult.ProfileNotSaved(profile.error)
            }
        }
    }
}
