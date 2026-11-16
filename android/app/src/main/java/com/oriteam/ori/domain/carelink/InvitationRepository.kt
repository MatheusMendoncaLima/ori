package com.oriteam.ori.domain.carelink

import com.oriteam.ori.domain.common.DataResult

/** A code the caregiver shares with the primary user, valid until [expiresAtMillis]. */
data class CareInvitation(val code: String, val expiresAtMillis: Long)

/**
 * A caregiver makes a code; the person to be supported types it and the link is created. Creating a link grants
 * access to another person's data, so `functions/firestore.rules` checks every step: a real author, a 24-hour code
 * used once, no self link, and a link born with only "see the routine". Online only: a link must be confirmed.
 */
interface InvitationRepository {
    suspend fun createInvitation(): DataResult<CareInvitation>

    /** Returns the caregiver's name when the link is created. */
    suspend fun acceptInvitation(code: String): DataResult<String>
}
