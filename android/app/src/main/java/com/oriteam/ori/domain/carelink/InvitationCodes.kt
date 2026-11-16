package com.oriteam.ori.domain.carelink

import com.oriteam.ori.domain.common.DataError
import java.util.Random

/**
 * Invitation codes: 8 characters without 0/O and 1/I, easy to read aloud and type. The same alphabet and life are
 * checked by `functions/firestore.rules` (`isInvitationCode`, 24 hours from `createdAt`).
 */
object InvitationCodes {
    const val LENGTH = 8
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LIFETIME_MILLIS = 24 * 60 * 60 * 1000L

    /** [random] is a `SecureRandom` in the app: a code that can be predicted could be accepted by someone else. */
    fun generate(random: Random): String = buildString(LENGTH) {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** What the person typed, without spaces and in capitals ("abcd efgh" is "ABCDEFGH"). */
    fun normalize(typed: String): String = typed.filterNot(Char::isWhitespace).uppercase()

    fun isValid(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }
}

/** A stored invitation, as read before accepting it. */
data class StoredInvitation(
    val caregiverUid: String,
    val caregiverName: String,
    val isPending: Boolean,
    val acceptedBy: String?,
    val createdAtMillis: Long
) {
    val expiresAtMillis: Long get() = createdAtMillis + InvitationCodes.LIFETIME_MILLIS

    /**
     * Why [acceptingUid] cannot accept it, or null when they can. The rules refuse the same cases; checking first
     * gives each one its own message instead of a generic "not allowed".
     */
    fun problemFor(acceptingUid: String, nowMillis: Long): DataError? = when {
        caregiverUid == acceptingUid -> DataError.PRECONDITION_FAILED
        !isPending -> DataError.PRECONDITION_FAILED
        expiresAtMillis <= nowMillis -> DataError.PRECONDITION_FAILED
        else -> null
    }
}
