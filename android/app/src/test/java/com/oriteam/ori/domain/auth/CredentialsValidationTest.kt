package com.oriteam.ori.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialsValidationTest {

    @Test
    fun `sign in requires email and password`() {
        assertEquals(CredentialsValidationError.MISSING_SIGN_IN_FIELDS, validateSignIn("", "secret"))
        assertEquals(CredentialsValidationError.MISSING_SIGN_IN_FIELDS, validateSignIn("a@b.com", " "))
        assertNull(validateSignIn("a@b.com", "secret"))
    }

    @Test
    fun `password reset requires an email`() {
        assertEquals(CredentialsValidationError.MISSING_RESET_EMAIL, validatePasswordReset("  "))
        assertNull(validatePasswordReset("a@b.com"))
    }

    @Test
    fun `registration reports missing fields before any other problem`() {
        val error = validateRegistration(name = "", email = "a@b.com", password = "1", passwordConfirmation = "2")

        assertEquals(CredentialsValidationError.MISSING_REGISTRATION_FIELDS, error)
    }

    @Test
    fun `registration rejects different passwords`() {
        val error = validateRegistration("Ana", "a@b.com", password = "secret1", passwordConfirmation = "secret2")

        assertEquals(CredentialsValidationError.PASSWORD_MISMATCH, error)
    }

    @Test
    fun `registration enforces the minimum password length at its boundary`() {
        val tooShort = "a".repeat(MIN_PASSWORD_LENGTH - 1)
        val exact = "a".repeat(MIN_PASSWORD_LENGTH)

        assertEquals(
            CredentialsValidationError.PASSWORD_TOO_SHORT,
            validateRegistration("Ana", "a@b.com", tooShort, tooShort)
        )
        assertNull(validateRegistration("Ana", "a@b.com", exact, exact))
    }
}
