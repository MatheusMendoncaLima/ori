package com.oriteam.ori.data.account

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.dataErrorFor
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.account.AccountDataExporter
import com.oriteam.ori.domain.account.AccountDeletion
import com.oriteam.ori.domain.account.AccountDeletionResult
import com.oriteam.ori.domain.account.accountExportJson
import com.oriteam.ori.domain.carelink.CareLinkStatus
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.legal.ConsentRepository
import com.oriteam.ori.domain.profile.DeviceProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Firebase asks for a sign-in this recent before deleting an account. */
private const val RECENT_SIGN_IN_MILLIS = 5 * 60 * 1000L

/** Deleting everything takes seconds online; this much waiting means there is no connection. */
private const val DELETION_TIMEOUT_MILLIS = 60_000L

/** A batch holds up to 500 writes; a margin keeps it clear of the limit. */
private const val BATCH_SIZE = 400

/** Everything in the account, read section by section (see [AccountDataExporter]). */
class FirestoreAccountDataExporter @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val clock: Clock
) : AccountDataExporter {
    override suspend fun export(ownerUid: String): DataResult<String> = runDataOperation {
        val user = firestore.userOf(ownerUid)
        val boards = user.collection(FirestoreSchema.AAC_BOARDS).readAll(AccountPages.BOARDS)
        accountExportJson(
            linkedMapOf(
                "app" to "ORI",
                "exportedAt" to Instant.now(clock).toString(),
                "profile" to user.get().await().takeIf { it.exists() }?.readable(),
                "accountPreferences" to user.collection(FirestoreSchema.SETTINGS)
                    .document(FirestoreSchema.PREFERENCES_DOCUMENT).get().await().takeIf { it.exists() }?.readable(),
                "consent" to user.collection(FirestoreSchema.LEGAL).document(FirestoreSchema.CONSENT_DOCUMENT)
                    .get().await().takeIf { it.exists() }?.readable(),
                "routineActivities" to user.collection(FirestoreSchema.ROUTINE_TASKS)
                    .readAll(AccountPages.ROUTINE_TASKS).map { it.readable() },
                "repeatingActivities" to user.collection(FirestoreSchema.ROUTINE_SERIES)
                    .readAll(AccountPages.ROUTINE_SERIES).map { it.readable() },
                "boards" to boards.map { board ->
                    board.readable() + (
                        "symbols" to board.reference.collection(FirestoreSchema.AAC_SYMBOLS)
                            .readAll(AccountPages.SYMBOLS).map { it.readable() }
                        )
                },
                "savedPhrases" to user.collection(FirestoreSchema.AAC_SAVED_MESSAGES)
                    .readAll(AccountPages.SAVED_MESSAGES).map { it.readable() },
                "photos" to user.collection(FirestoreSchema.PHOTOS).readAll(AccountPages.PHOTOS).map { it.readable() },
                "careLinks" to firestore.careLinksOf(ownerUid).map { it.readable() }
            )
        )
    }
}

/**
 * Deletes the account from the app itself (no server function): first everything under the profile, then the links
 * (ended, then removed, so the other person's list loses them too), the consent, the profile and last the sign-in.
 * The rules let the owner delete each of these (tested in functions/security-tests).
 */
class FirestoreAccountDeletion @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val consents: ConsentRepository,
    private val deviceProfiles: DeviceProfileRepository,
    private val clock: Clock
) : AccountDeletion {

    override suspend fun deleteAccount(ownerUid: String): AccountDeletionResult {
        val user = auth.currentUser?.takeIf { it.uid == ownerUid }
        val lastSignIn = user?.metadata?.lastSignInTimestamp ?: 0L
        return when {
            user == null -> AccountDeletionResult.Failed(DataError.PERMISSION_DENIED)
            clock.millis() - lastSignIn > RECENT_SIGN_IN_MILLIS -> AccountDeletionResult.NeedsRecentSignIn
            else -> deleteSignedIn(user)
        }
    }

    @Suppress("TooGenericExceptionCaught") // Any failure is reported, never a half-silent success.
    private suspend fun deleteSignedIn(user: FirebaseUser): AccountDeletionResult =
        try {
            // Offline, a delete waits for the server forever: past the limit it is a connection failure.
            val isDone = withTimeoutOrNull(DELETION_TIMEOUT_MILLIS) { deleteData(user.uid) } != null
            if (isDone) {
                user.delete().await()
                deviceProfiles.clearAccountProfileCopy()
                AccountDeletionResult.Deleted
            } else {
                AccountDeletionResult.Failed(DataError.NETWORK_UNAVAILABLE)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            AccountDeletionResult.NeedsRecentSignIn
        } catch (exception: Exception) {
            AccountDeletionResult.Failed(dataErrorFor(exception))
        }

    private suspend fun deleteData(ownerUid: String) {
        val user = firestore.userOf(ownerUid)
        // The mark lets the owner delete also the activities a caregiver reserved (see the rules).
        user.update(FirestoreSchema.DELETING_AT, FieldValue.serverTimestamp()).await()
        val boards = user.collection(FirestoreSchema.AAC_BOARDS).readAll(AccountPages.BOARDS)
        val symbols = boards.flatMap {
            it.reference.collection(FirestoreSchema.AAC_SYMBOLS).readAll(AccountPages.SYMBOLS)
        }
        delete(symbols.map { it.reference } + boards.map { it.reference })
        delete(user.collection(FirestoreSchema.ROUTINE_TASKS).readAll(AccountPages.ROUTINE_TASKS).map { it.reference })
        delete(
            user.collection(FirestoreSchema.ROUTINE_SERIES).readAll(AccountPages.ROUTINE_SERIES).map { it.reference }
        )
        delete(
            user.collection(
                FirestoreSchema.AAC_SAVED_MESSAGES
            ).readAll(AccountPages.SAVED_MESSAGES).map { it.reference }
        )
        delete(user.collection(FirestoreSchema.PHOTOS).readAll(AccountPages.PHOTOS).map { it.reference })
        delete(listOf(user.collection(FirestoreSchema.SETTINGS).document(FirestoreSchema.PREFERENCES_DOCUMENT)))
        deleteCareLinks(ownerUid)
        consents.clearConsent(ownerUid)
        delete(listOf(user))
    }

    /** A link is ended first (the rules let either person end it), then removed with its names. */
    private suspend fun deleteCareLinks(ownerUid: String) {
        val links = firestore.careLinksOf(ownerUid)
        val revoked = FirestoreSchema.linkStatusValues.getValue(CareLinkStatus.REVOKED)
        val active = links.filter { it.getString(FirestoreSchema.STATUS) != revoked }
        active.chunked(BATCH_SIZE).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach {
                batch.update(
                    it.reference,
                    mapOf(FirestoreSchema.STATUS to revoked, FirestoreSchema.REVOKED_AT to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
        }
        delete(links.map { it.reference })
    }

    private suspend fun delete(references: List<DocumentReference>) {
        references.chunked(BATCH_SIZE).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach(batch::delete)
            batch.commit().await()
        }
    }
}
