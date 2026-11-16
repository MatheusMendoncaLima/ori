package com.oriteam.ori.data.account

import android.util.Base64
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.oriteam.ori.data.firestore.FirestoreSchema
import kotlinx.coroutines.tasks.await
import java.time.Instant

/** The page sizes the rules accept for each list (they refuse a list without a limit or above it). */
internal object AccountPages {
    const val ROUTINE_TASKS = 100
    const val ROUTINE_SERIES = 50
    const val BOARDS = 30
    const val SYMBOLS = 60
    const val SAVED_MESSAGES = 50
    const val PHOTOS = 101
    const val CARE_LINKS = 50
}

internal fun FirebaseFirestore.userOf(ownerUid: String) = collection(FirestoreSchema.USERS).document(ownerUid)

/** Every document of a collection, in pages of [pageSize] ordered by id, so no list goes past the rules' limit. */
internal suspend fun CollectionReference.readAll(pageSize: Int): List<DocumentSnapshot> {
    val all = mutableListOf<DocumentSnapshot>()
    var last: DocumentSnapshot? = null
    do {
        val base = orderBy(FieldPath.documentId()).limit(pageSize.toLong())
        val page = (last?.let { base.startAfter(it) } ?: base).get().await().documents
        all += page
        last = page.lastOrNull()
    } while (page.size == pageSize)
    return all
}

/** The links where the person is on either side (as caregiver or as the one supported). */
internal suspend fun FirebaseFirestore.careLinksOf(ownerUid: String): List<DocumentSnapshot> =
    listOf(FirestoreSchema.CAREGIVER_UID, FirestoreSchema.PRIMARY_USER_UID).flatMap { side ->
        collection(FirestoreSchema.CARE_LINKS).whereEqualTo(side, ownerUid)
            .limit(AccountPages.CARE_LINKS.toLong()).get().await().documents
    }.distinctBy { it.id }

/** A document as plain values a person can read: dates as text, photo bytes as Base64. */
internal fun DocumentSnapshot.readable(): Map<String, Any?> =
    linkedMapOf<String, Any?>("id" to id) + data.orEmpty().mapValues { (_, value) -> readableValue(value) }

private fun readableValue(value: Any?): Any? = when (value) {
    is Timestamp -> Instant.ofEpochSecond(value.seconds, value.nanoseconds.toLong()).toString()
    is Blob -> Base64.encodeToString(value.toBytes(), Base64.NO_WRAP)
    is Map<*, *> -> value.mapValues { (_, item) -> readableValue(item) }
    is List<*> -> value.map(::readableValue)
    else -> value
}

internal fun FirebaseFirestore.invitationOwnerOf(ownerUid: String) =
    collection(FirestoreSchema.INVITATION_OWNERS).document(ownerUid)

/** The person's latest invitation code, if it still exists (only one works at a time). */
internal suspend fun FirebaseFirestore.invitationOf(ownerUid: String): DocumentSnapshot? =
    invitationOwnerOf(ownerUid).get().await().getString(FirestoreSchema.CODE)
        ?.let { collection(FirestoreSchema.INVITATIONS).document(it).get().await() }
        ?.takeIf { it.exists() && it.getString(FirestoreSchema.CAREGIVER_UID) == ownerUid }
