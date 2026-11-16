package com.oriteam.ori.data.carelink

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.carelink.CareInvitation
import com.oriteam.ori.domain.carelink.CareLinkStatus
import com.oriteam.ori.domain.carelink.InvitationCodes
import com.oriteam.ori.domain.carelink.InvitationRepository
import com.oriteam.ori.domain.carelink.StoredInvitation
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom
import java.time.Clock
import java.util.Date
import javax.inject.Inject

/** Each step answers in under a second online; waiting this long means there is no connection. */
private const val ONLINE_TIMEOUT_MILLIS = 15_000L

/** A taken code is refused like any other write; three random codes in a row taken is not a collision. */
private const val MAX_CODE_ATTEMPTS = 3

/** A code with at least this much life left is shown again instead of replaced (the rules wait 30 s anyway). */
private const val REUSE_MIN_REMAINING_MILLIS = 60 * 60 * 1000L

/**
 * Invitations without Cloud Functions: the app writes, and `functions/firestore.rules` checks what the server used
 * to check (tested in `functions/security-tests/test/careInvitations.test.mjs`).
 *
 * - Making a code: the invitation and the pointer to the caregiver's latest code go in one batch; the previous
 *   code is removed with them, so only one code of each person works at a time.
 * - Accepting: the link and the invitation marked as used go in one batch, so one never exists without the other.
 *
 * Everything reads from the server and waits for it: a link must be confirmed, never queued offline.
 */
class FirestoreInvitationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val clock: Clock
) : InvitationRepository {

    private val random = SecureRandom()
    private val invitations get() = firestore.collection(FirestoreSchema.INVITATIONS)
    private val links get() = firestore.collection(FirestoreSchema.CARE_LINKS)

    override suspend fun createInvitation(): DataResult<CareInvitation> = online { uid ->
        val name = profileName(uid) ?: return@online DataResult.Failure(DataError.PERMISSION_DENIED)
        val pointer = firestore.collection(FirestoreSchema.INVITATION_OWNERS).document(uid)
        val previous = pointer.get(Source.SERVER).await().getString(FirestoreSchema.CODE)
            ?.let { invitations.document(it).get(Source.SERVER).await() }
        val reusable = previous?.storedInvitation()?.takeIf {
            it.isPending && it.expiresAtMillis - clock.millis() >= REUSE_MIN_REMAINING_MILLIS
        }
        if (previous != null && reusable != null) {
            return@online DataResult.Success(CareInvitation(previous.id, reusable.expiresAtMillis))
        }
        repeat(MAX_CODE_ATTEMPTS) {
            val code = InvitationCodes.generate(random)
            val expiresAt = clock.millis() + InvitationCodes.LIFETIME_MILLIS
            val batch = firestore.batch()
            previous?.takeIf { it.exists() }?.let { batch.delete(it.reference) }
            batch.set(
                invitations.document(code),
                mapOf(
                    FirestoreSchema.CAREGIVER_UID to uid,
                    FirestoreSchema.CAREGIVER_NAME to name,
                    FirestoreSchema.STATUS to FirestoreSchema.INVITATION_PENDING,
                    FirestoreSchema.CREATED_AT to FieldValue.serverTimestamp(),
                    FirestoreSchema.EXPIRES_AT to Timestamp(Date(expiresAt))
                )
            )
            batch.set(
                pointer,
                mapOf(FirestoreSchema.CODE to code, FirestoreSchema.CREATED_AT to FieldValue.serverTimestamp())
            )
            if (commitUnlessRefused(batch)) return@online DataResult.Success(CareInvitation(code, expiresAt))
        }
        DataResult.Failure(DataError.UNKNOWN)
    }

    override suspend fun acceptInvitation(code: String): DataResult<String> = online { uid ->
        val myName = profileName(uid) ?: return@online DataResult.Failure(DataError.PERMISSION_DENIED)
        val invitationRef = invitations.document(code)
        val invitation = invitationRef.get(Source.SERVER).await().storedInvitation()
            ?: return@online DataResult.Failure(DataError.NOT_FOUND)
        val active = FirestoreSchema.linkStatusValues.getValue(CareLinkStatus.ACTIVE)
        val hasActiveLink = links.whereEqualTo(FirestoreSchema.CAREGIVER_UID, invitation.caregiverUid)
            .whereEqualTo(FirestoreSchema.PRIMARY_USER_UID, uid)
            .limit(1).get(Source.SERVER).await().documents
            .any { it.getString(FirestoreSchema.STATUS) == active }
        // The first try worked but its answer was lost (timeout): trying again is not an error.
        if (invitation.acceptedBy == uid && hasActiveLink) return@online DataResult.Success(invitation.caregiverName)
        invitation.problemFor(uid, clock.millis())?.let { return@online DataResult.Failure(it) }
        if (hasActiveLink) return@online DataResult.Failure(DataError.CONFLICT)

        val batch = firestore.batch()
        batch.set(
            links.document(FirestoreSchema.linkId(invitation.caregiverUid, uid)),
            mapOf(
                FirestoreSchema.CAREGIVER_UID to invitation.caregiverUid,
                FirestoreSchema.PRIMARY_USER_UID to uid,
                FirestoreSchema.CAREGIVER_NAME to invitation.caregiverName,
                FirestoreSchema.PRIMARY_USER_NAME to myName,
                FirestoreSchema.STATUS to active,
                // Least privilege: the person decides afterwards what else the caregiver may do.
                FirestoreSchema.PERMISSIONS to mapOf(
                    FirestoreSchema.VIEW_ROUTINES to true,
                    FirestoreSchema.MANAGE_ROUTINES to false,
                    FirestoreSchema.MANAGE_COMMUNICATION to false
                ),
                FirestoreSchema.CREATED_AT to FieldValue.serverTimestamp(),
                FirestoreSchema.REVOKED_AT to null,
                FirestoreSchema.INVITATION_CODE to code
            )
        )
        batch.update(
            invitationRef,
            mapOf(
                FirestoreSchema.STATUS to FirestoreSchema.INVITATION_ACCEPTED,
                FirestoreSchema.ACCEPTED_BY to uid,
                FirestoreSchema.ACCEPTED_AT to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        DataResult.Success(invitation.caregiverName)
    }

    /** Signed in, online and within the time limit; any failure becomes a [DataResult.Failure]. */
    private suspend fun <T> online(operation: suspend (uid: String) -> DataResult<T>): DataResult<T> {
        val uid = auth.currentUser?.uid ?: return DataResult.Failure(DataError.PERMISSION_DENIED)
        val result = withTimeoutOrNull(ONLINE_TIMEOUT_MILLIS) { runDataOperation { operation(uid) } }
            ?: return DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        return when (result) {
            is DataResult.Failure -> result
            is DataResult.Success -> result.value
        }
    }

    private suspend fun profileName(uid: String): String? =
        firestore.collection(FirestoreSchema.USERS).document(uid).get(Source.SERVER).await()
            .getString(FirestoreSchema.DISPLAY_NAME)

    /** True when written; false when the rules refused it (a taken code), so another code is tried. */
    private suspend fun commitUnlessRefused(batch: WriteBatch): Boolean = try {
        batch.commit().await()
        true
    } catch (exception: FirebaseFirestoreException) {
        if (exception.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw exception
        false
    }
}

/** A missing document, or one without its required fields, is no invitation ("Código não encontrado"). */
private fun DocumentSnapshot.storedInvitation(): StoredInvitation? {
    val caregiverUid = getString(FirestoreSchema.CAREGIVER_UID) ?: return null
    val caregiverName = getString(FirestoreSchema.CAREGIVER_NAME) ?: return null
    val createdAt = getTimestamp(FirestoreSchema.CREATED_AT) ?: return null
    return StoredInvitation(
        caregiverUid = caregiverUid,
        caregiverName = caregiverName,
        isPending = getString(FirestoreSchema.STATUS) == FirestoreSchema.INVITATION_PENDING,
        acceptedBy = getString(FirestoreSchema.ACCEPTED_BY),
        createdAtMillis = createdAt.toDate().time
    )
}
