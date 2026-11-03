package com.oriteam.ori.data.firestore

import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.profile.ProfileLookup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How long a write waits for the server before being reported as [WriteStatus.PENDING_SYNC].
 * The write is not cancelled: the SDK keeps it on the device and sends it when possible.
 */
internal const val WRITE_ACK_TIMEOUT_MILLIS = 3_000L

/** How long to wait for the server before admitting that a profile cannot be confirmed. */
internal const val PROFILE_SERVER_WAIT_MILLIS = 8_000L

/**
 * Waits for the server acknowledgement of a write for up to [timeoutMillis].
 *
 * Online, the result (success or a rules rejection) arrives quickly. Offline, the change is
 * already applied to the local cache and visible on screen, so the caller learns it is pending
 * instead of waiting forever. If the server later rejects it, the SDK rolls back the cache and
 * listeners show the previous value.
 */
internal suspend fun awaitWriteAcknowledgement(
    timeoutMillis: Long = WRITE_ACK_TIMEOUT_MILLIS,
    acknowledgement: suspend () -> Unit
): WriteStatus = withTimeoutOrNull(timeoutMillis) {
    acknowledgement()
    WriteStatus.SYNCED
} ?: WriteStatus.PENDING_SYNC

/** What a Firestore document listener reported, without SDK types (so it can be tested). */
internal data class DocumentState(
    val exists: Boolean,
    val isFromCache: Boolean,
    val data: Map<String, Any?> = emptyMap(),
    val error: DataError? = null
)

/**
 * Turns profile document states into [ProfileLookup]s.
 *
 * A "missing" answer from the cache is not trusted at once (it is normal right after sign-in),
 * but it is not trusted forever either: if the server does not answer within [serverWaitMillis],
 * the lookup fails with [DataError.NETWORK_UNAVAILABLE], so the screen shows "try again" instead
 * of loading forever. A later server answer still replaces that failure.
 */
internal fun profileLookups(
    uid: String,
    states: Flow<DocumentState>,
    serverWaitMillis: Long = PROFILE_SERVER_WAIT_MILLIS
): Flow<ProfileLookup> = channelFlow {
    var serverWait: Job? = null
    states.collect { state ->
        if (!state.exists && state.isFromCache && state.error == null) {
            if (serverWait == null) {
                serverWait = launch {
                    delay(serverWaitMillis)
                    send(ProfileLookup.Failure(DataError.NETWORK_UNAVAILABLE))
                }
            }
            return@collect
        }
        serverWait?.cancel()
        serverWait = null
        send(lookupFor(uid, state))
    }
}

private fun lookupFor(uid: String, state: DocumentState): ProfileLookup = when {
    state.error != null -> ProfileLookup.Failure(state.error)
    !state.exists -> ProfileLookup.Missing
    else -> profileFrom(uid, state.data)?.let { ProfileLookup.Found(it) } ?: ProfileLookup.Failure(DataError.UNKNOWN)
}
