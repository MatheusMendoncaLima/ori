package com.oriteam.ori.domain

import com.oriteam.ori.domain.auth.AuthError
import com.oriteam.ori.domain.auth.AuthResult
import com.oriteam.ori.domain.auth.AuthenticatedUser
import com.oriteam.ori.domain.auth.RegisterAccount
import com.oriteam.ori.domain.auth.RegistrationResult
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.profile.ProfileLookup
import com.oriteam.ori.domain.profile.UserProfile
import com.oriteam.ori.domain.session.SessionState
import com.oriteam.ori.domain.session.sessionStateFor
import com.oriteam.ori.testing.FakeAuthRepository
import com.oriteam.ori.testing.FakeProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizationDomainTest {

    private val user = AuthenticatedUser(uid = "pat", displayName = "Pat")
    private val profile = UserProfile(uid = "pat", displayName = "Pat")

    @Test
    fun `session separates identity from profile`() {
        assertEquals(SessionState.SignedOut, sessionStateFor(user = null, lookup = ProfileLookup.Found(profile)))
        assertEquals(SessionState.Initializing, sessionStateFor(user, lookup = null))
        assertEquals(SessionState.ProfileRequired(user), sessionStateFor(user, ProfileLookup.Missing))
        assertEquals(
            SessionState.ProfileUnavailable(user, DataError.NETWORK_UNAVAILABLE),
            sessionStateFor(user, ProfileLookup.Failure(DataError.NETWORK_UNAVAILABLE))
        )
        assertEquals(SessionState.Active(profile), sessionStateFor(user, ProfileLookup.Found(profile)))
    }

    @Test
    fun `registration creates the profile for the new uid with the chosen role`() = runTest {
        val profiles = FakeProfileRepository()

        val result = RegisterAccount(
            FakeAuthRepository(),
            profiles
        )("Cara", "c@ori.app", "Secret1")

        assertEquals(RegistrationResult.Completed, result)
        assertEquals(listOf("new-uid" to "Cara"), profiles.createCalls)
    }

    @Test
    fun `failed account creation never writes a profile`() = runTest {
        val profiles = FakeProfileRepository()
        val auth = FakeAuthRepository(nextResult = AuthResult.Failure(AuthError.EMAIL_ALREADY_IN_USE))

        val result = RegisterAccount(auth, profiles)("Ana", "a@ori.app", "Secret1")

        assertEquals(RegistrationResult.Failed(AuthError.EMAIL_ALREADY_IN_USE), result)
        assertTrue(profiles.createCalls.isEmpty())
    }

    @Test
    fun `profile failure is reported separately from account failure`() = runTest {
        val profiles = FakeProfileRepository().apply { createResult = DataResult.Failure(DataError.PERMISSION_DENIED) }

        val result = RegisterAccount(
            FakeAuthRepository(),
            profiles
        )("Ana", "a@ori.app", "Secret1")

        assertEquals(RegistrationResult.ProfileNotSaved(DataError.PERMISSION_DENIED), result)
    }
}
