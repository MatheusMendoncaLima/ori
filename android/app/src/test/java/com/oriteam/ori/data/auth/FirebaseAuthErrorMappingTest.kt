package com.oriteam.ori.data.auth

import com.oriteam.ori.domain.auth.AuthError
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseAuthErrorMappingTest {

    @Test
    fun `wrong password and unknown user share one error so accounts cannot be discovered`() {
        val errors = listOf("ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL")
            .map(::authErrorFromFirebaseCode)

        assertEquals(List(errors.size) { AuthError.INVALID_CREDENTIALS }, errors)
    }

    @Test
    fun `known codes map to their specific error`() {
        assertEquals(AuthError.EMAIL_ALREADY_IN_USE, authErrorFromFirebaseCode("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals(AuthError.WEAK_PASSWORD, authErrorFromFirebaseCode("ERROR_WEAK_PASSWORD"))
        assertEquals(AuthError.INVALID_EMAIL, authErrorFromFirebaseCode("ERROR_INVALID_EMAIL"))
    }

    @Test
    fun `unknown codes become a generic error`() {
        assertEquals(AuthError.UNKNOWN, authErrorFromFirebaseCode("ERROR_SOMETHING_NEW"))
    }
}
