package com.oriteam.ori.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileTest {

    @Test
    fun `the greeting uses the first name only, whatever the spaces around it`() {
        assertEquals("Ana", UserProfile("u", "Ana Maria da Silva").firstName)
        assertEquals("Ana", UserProfile("u", "  Ana \t Silva ").firstName)
        assertEquals("Ana", UserProfile("u", "Ana").firstName)
    }

    @Test
    fun `without a name there is no first name, so the greeting has none`() {
        assertEquals("", UserProfile("u", "").firstName)
        assertEquals("", UserProfile("u", "   ").firstName)
    }
}
