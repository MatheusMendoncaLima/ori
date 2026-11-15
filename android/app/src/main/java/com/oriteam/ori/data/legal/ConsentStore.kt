package com.oriteam.ori.data.legal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.awaitWriteAcknowledgement
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.legal.ConsentRepository
import com.oriteam.ori.domain.legal.LegalConsent
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val CONSENT_PARTS = 4

/**
 * The consent of each owner on this device (DataStore, one text per owner) and, for an account, in
 * `users/{uid}/legal/consent`. Reading uses the device copy first, so an accepted consent never blocks the app
 * offline; a device without a copy reads the account (accepted on another device) and keeps it.
 */
class ConsentStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val firestore: FirebaseFirestore
) : ConsentRepository {

    private fun keyOf(ownerUid: String) = stringPreferencesKey("legal_consent_$ownerUid")

    private fun documentOf(ownerUid: String) = firestore.collection(FirestoreSchema.USERS).document(ownerUid)
        .collection(FirestoreSchema.LEGAL).document(FirestoreSchema.CONSENT_DOCUMENT)

    override suspend fun getConsent(ownerUid: String): DataResult<LegalConsent?> {
        val onDevice = dataStore.data.first()[keyOf(ownerUid)]?.let(::consentFromText)
        if (onDevice != null || ownerUid == LOCAL_OWNER_UID) return DataResult.Success(onDevice)
        return runDataOperation {
            documentOf(ownerUid).get().await().data?.let(::consentFrom)?.also { keepOnDevice(ownerUid, it) }
        }
    }

    override suspend fun recordConsent(ownerUid: String, consent: LegalConsent): DataResult<WriteStatus> {
        keepOnDevice(ownerUid, consent)
        if (ownerUid == LOCAL_OWNER_UID) return DataResult.Success(WriteStatus.SYNCED)
        return runDataOperation {
            val write = documentOf(ownerUid).set(
                mapOf(
                    FirestoreSchema.VERSION to consent.version.toLong(),
                    FirestoreSchema.IS_ADULT to consent.isAdult,
                    FirestoreSchema.GUARDIAN_CONSENT to consent.hasGuardianConsent,
                    FirestoreSchema.ACCEPTED_AT to FieldValue.serverTimestamp()
                )
            )
            awaitWriteAcknowledgement { write.await() }
        }
    }

    override suspend fun clearConsent(ownerUid: String): DataResult<WriteStatus> {
        dataStore.edit { it.remove(keyOf(ownerUid)) }
        if (ownerUid == LOCAL_OWNER_UID) return DataResult.Success(WriteStatus.SYNCED)
        return runDataOperation {
            val write = documentOf(ownerUid).delete()
            awaitWriteAcknowledgement { write.await() }
        }
    }

    private suspend fun keepOnDevice(ownerUid: String, consent: LegalConsent) {
        dataStore.edit { it[keyOf(ownerUid)] = consent.toText() }
    }
}

/** "version|adult|guardian|millis": a short text, read back by [consentFromText]. */
internal fun LegalConsent.toText() = listOf(version, isAdult, hasGuardianConsent, acceptedAtMillis).joinToString("|")

internal fun consentFromText(text: String): LegalConsent? {
    val parts = text.split("|")
    if (parts.size != CONSENT_PARTS) return null
    return parts.first().toIntOrNull()?.let { version ->
        LegalConsent(version, parts[1].toBoolean(), parts[2].toBoolean(), parts.last().toLongOrNull() ?: 0L)
    }
}

internal fun consentFrom(data: Map<String, Any?>): LegalConsent? {
    val version = (data[FirestoreSchema.VERSION] as? Number)?.toInt() ?: return null
    return LegalConsent(
        version = version,
        isAdult = data[FirestoreSchema.IS_ADULT] == true,
        hasGuardianConsent = data[FirestoreSchema.GUARDIAN_CONSENT] == true,
        acceptedAtMillis = (data[FirestoreSchema.ACCEPTED_AT] as? Timestamp)?.toDate()?.time ?: 0L
    )
}
