package com.oriteam.ori.domain.carelink

import com.oriteam.ori.domain.common.DataError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Random

class InvitationCodesTest {

    @Test
    fun `a code has 8 characters of the alphabet, and codes differ`() {
        val random = Random(1)
        val codes = List(200) { InvitationCodes.generate(random) }

        assertTrue(codes.all(InvitationCodes::isValid))
        assertTrue(codes.toSet().size > 195)
    }

    @Test
    fun `typed codes are cleaned, and look-alike characters are refused`() {
        assertEquals("ABCD2345", InvitationCodes.normalize(" abcd 2345 "))
        listOf("ABCD234", "ABCD23456", "ABCD234O", "ABCD2340", "ABCD234I", "ABCD2341", "ABCD-345").forEach {
            assertFalse(it, InvitationCodes.isValid(it))
        }
    }

    @Test
    fun `the rules accept exactly the alphabet of the app`() {
        val rules = File("../../functions/firestore.rules").readText()
        val pattern = Regex("""code\.matches\('(\^\[[^']+\]\{8\}\$)'\)""").find(rules)?.groupValues?.get(1)
            ?: error("isInvitationCode not found in firestore.rules")
        val accepted = ('A'..'Z') + ('0'..'9')

        assertEquals(
            InvitationCodes.ALPHABET.toSet(),
            accepted.filter { Regex(pattern).matches(it.toString().repeat(InvitationCodes.LENGTH)) }.toSet()
        )
    }

    @Test
    fun `an invitation is accepted only by someone else, once, within 24 hours`() {
        val createdAt = 1_000_000L
        val pending = StoredInvitation("cara", "Cara", isPending = true, acceptedBy = null, createdAtMillis = createdAt)
        val justBefore = createdAt + InvitationCodes.LIFETIME_MILLIS - 1

        assertNull(pending.problemFor("pat", justBefore))
        assertEquals(DataError.PRECONDITION_FAILED, pending.problemFor("cara", createdAt))
        assertEquals(DataError.PRECONDITION_FAILED, pending.problemFor("pat", justBefore + 1))
        assertEquals(
            DataError.PRECONDITION_FAILED,
            pending.copy(isPending = false, acceptedBy = "otto").problemFor("pat", createdAt)
        )
    }
}
