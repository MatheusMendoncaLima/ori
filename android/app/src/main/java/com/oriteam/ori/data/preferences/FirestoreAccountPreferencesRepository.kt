package com.oriteam.ori.data.preferences

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.data.firestore.awaitWriteAcknowledgement
import com.oriteam.ori.data.firestore.dataErrorFor
import com.oriteam.ori.data.firestore.keyFor
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.AccountPreferencesRepository
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.domain.preferences.TextScale
import com.oriteam.ori.domain.preferences.ThemeMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Stored values: a persisted contract shared with `functions/firestore.rules`. */
private val textScaleValues = mapOf(
    TextScale.NORMAL to "normal",
    TextScale.LARGE to "large",
    TextScale.LARGER to "larger",
    TextScale.LARGEST to "largest"
)
private val themeValues = mapOf(
    ThemeMode.SYSTEM to "system",
    ThemeMode.LIGHT to "light",
    ThemeMode.DARK to "dark"
)
private val motionValues = mapOf(
    MotionLevel.SYSTEM to "system",
    MotionLevel.EXPRESSIVE to "expressive",
    MotionLevel.SUBTLE to "subtle",
    MotionLevel.REDUCED to "reduced",
    MotionLevel.OFF to "off"
)

/** Each field stands alone: a value from a newer app version falls back without losing the others. */
internal fun accountPreferencesFrom(data: Map<String, Any?>): AccountPreferences {
    val defaults = AccountPreferences()
    return AccountPreferences(
        textScale = textScaleValues.keyFor(data[FirestoreSchema.TEXT_SCALE]) ?: defaults.textScale,
        theme = themeValues.keyFor(data[FirestoreSchema.THEME]) ?: defaults.theme,
        isSimplified = data[FirestoreSchema.SIMPLIFIED] as? Boolean ?: defaults.isSimplified,
        motion = motionValues.keyFor(data[FirestoreSchema.MOTION]) ?: defaults.motion
    )
}

internal fun AccountPreferences.toDocument(): Map<String, Any> = mapOf(
    FirestoreSchema.TEXT_SCALE to textScaleValues.getValue(textScale),
    FirestoreSchema.THEME to themeValues.getValue(theme),
    FirestoreSchema.SIMPLIFIED to isSimplified,
    FirestoreSchema.MOTION to motionValues.getValue(motion),
    FirestoreSchema.UPDATED_AT to FieldValue.serverTimestamp()
)

/** One small document per person; its listener lives only while the app is on screen. */
class FirestoreAccountPreferencesRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : AccountPreferencesRepository {

    private fun documentOf(uid: String) = firestore.collection(FirestoreSchema.USERS)
        .document(uid)
        .collection(FirestoreSchema.SETTINGS)
        .document(FirestoreSchema.PREFERENCES_DOCUMENT)

    override fun observe(uid: String): Flow<DataResult<AccountPreferences?>> = callbackFlow {
        val registration = documentOf(uid).addSnapshotListener { snapshot, exception ->
            val result = if (exception != null || snapshot == null) {
                DataResult.Failure(exception?.let(::dataErrorFor) ?: DataError.UNKNOWN)
            } else {
                DataResult.Success(snapshot.data?.let(::accountPreferencesFrom))
            }
            trySend(result)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun save(uid: String, preferences: AccountPreferences): DataResult<WriteStatus> =
        runDataOperation {
            val write = documentOf(uid).set(preferences.toDocument())
            awaitWriteAcknowledgement { write.await() }
        }
}
