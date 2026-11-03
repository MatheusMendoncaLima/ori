package com.oriteam.ori.domain.carelink

import com.oriteam.ori.domain.common.DataResult

/** A code the caregiver shares with the primary user, valid until [expiresAtMillis]. */
data class CareInvitation(val code: String, val expiresAtMillis: Long)

/**
 * Invitations run on the server (Cloud Functions), because creating a care link grants access
 * to another person's data and must not be decided by the client.
 */
interface InvitationRepository {
    suspend fun createInvitation(): DataResult<CareInvitation>

    /** Returns the caregiver's name when the link is created. */
    suspend fun acceptInvitation(code: String): DataResult<String>
}
