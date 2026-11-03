package com.oriteam.ori.domain.carelink

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow

/** What a caregiver may do with a primary user's data. Decided by the primary user. */
data class CareLinkPermissions(
    val viewRoutines: Boolean,
    val manageRoutines: Boolean,
    /** Create, edit, reorder and delete communication boards and symbols (never saved messages). */
    val manageCommunication: Boolean = false
)

enum class CareLinkStatus { ACTIVE, REVOKED }

/**
 * Support relationship between a caregiver and a primary user (`careLinks/{caregiver_primary}`).
 * Created only by the server when the primary user accepts an invitation.
 */
data class CareLink(
    val id: String,
    val caregiverUid: String,
    val primaryUserUid: String,
    val caregiverName: String,
    val primaryUserName: String,
    val status: CareLinkStatus,
    val permissions: CareLinkPermissions
)

/** Upper bound of one link query, also enforced by the Firestore rules. */
const val MAX_CARE_LINKS = 50

interface CareLinkRepository {
    /** Follows the active links (at most [MAX_CARE_LINKS]) where [uid] is on [side]. */
    fun observeActiveLinks(uid: String, side: CareLinkSide): Flow<DataResult<List<CareLink>>>

    /** Ends the link for both people. Final: a new invitation is needed to link again. */
    suspend fun revoke(linkId: String): DataResult<WriteStatus>

    /** Only the primary user may call this; Firestore rules reject anyone else. */
    suspend fun updatePermissions(linkId: String, permissions: CareLinkPermissions): DataResult<WriteStatus>
}
