package com.oriteam.ori.data.firestore

import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.profile.ProfileLookup
import com.oriteam.ori.domain.profile.UserProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Offline policies, verified with virtual time (no real waiting, no Firebase). */
class FirestorePoliciesTest {

    private val profileData = mapOf("role" to "caregiver", "displayName" to "Cara")
    private val profile = UserProfile("cara", "Cara")

    @Test
    fun `a write acknowledged by the server is synced`() = runTest {
        assertEquals(WriteStatus.SYNCED, awaitWriteAcknowledgement { })
    }

    @Test
    fun `a write without acknowledgement is reported as pending instead of hanging`() = runTest {
        val status = awaitWriteAcknowledgement(timeoutMillis = 3_000) { awaitCancellation() }

        assertEquals(WriteStatus.PENDING_SYNC, status)
    }

    @Test
    fun `a rejected write is still an error`() = runTest {
        val rejection = IllegalStateException("PERMISSION_DENIED")
        val failure = CompletableDeferred<Unit>().apply { completeExceptionally(rejection) }

        val result = runCatching { awaitWriteAcknowledgement { failure.await() } }

        // Coroutines may copy the exception to recover the stack trace, so compare type and message.
        val error = result.exceptionOrNull()
        assertEquals(rejection.javaClass, error?.javaClass)
        assertEquals(rejection.message, error?.message)
    }

    @Test
    fun `a missing profile in the cache waits for the server`() = runTest {
        val states = MutableSharedFlow<DocumentState>()
        val lookups = mutableListOf<ProfileLookup>()
        backgroundScope.launch { profileLookups("cara", states, serverWaitMillis = 8_000).toList(lookups) }
        runCurrent()

        states.emit(DocumentState(exists = false, isFromCache = true))
        advanceTimeBy(1_000)
        states.emit(DocumentState(exists = true, isFromCache = false, data = profileData))
        advanceTimeBy(10_000)

        assertEquals(listOf<ProfileLookup>(ProfileLookup.Found(profile)), lookups)
    }

    @Test
    fun `an unreachable server ends in a network error instead of loading forever`() = runTest {
        val states = MutableSharedFlow<DocumentState>()
        val lookups = mutableListOf<ProfileLookup>()
        backgroundScope.launch { profileLookups("cara", states, serverWaitMillis = 8_000).toList(lookups) }
        runCurrent()

        states.emit(DocumentState(exists = false, isFromCache = true))
        advanceTimeBy(8_001)
        assertEquals(listOf<ProfileLookup>(ProfileLookup.Failure(DataError.NETWORK_UNAVAILABLE)), lookups)

        // When the connection returns, the server answer replaces the error.
        states.emit(DocumentState(exists = false, isFromCache = false))
        runCurrent()
        assertEquals(ProfileLookup.Missing, lookups.last())
    }

    @Test
    fun `a cached profile works offline and listener errors pass through`() = runTest {
        val states = MutableSharedFlow<DocumentState>()
        val lookups = mutableListOf<ProfileLookup>()
        backgroundScope.launch { profileLookups("cara", states).toList(lookups) }
        runCurrent()

        states.emit(DocumentState(exists = true, isFromCache = true, data = profileData))
        states.emit(DocumentState(exists = false, isFromCache = false, error = DataError.PERMISSION_DENIED))
        runCurrent()

        assertEquals(
            listOf(ProfileLookup.Found(profile), ProfileLookup.Failure(DataError.PERMISSION_DENIED)),
            lookups
        )
    }
}
