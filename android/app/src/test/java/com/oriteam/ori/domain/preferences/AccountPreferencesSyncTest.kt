package com.oriteam.ori.domain.preferences

import com.oriteam.ori.domain.auth.AuthenticatedUser
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.testing.FakeAccountPreferencesRepository
import com.oriteam.ori.testing.FakeAuthRepository
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountPreferencesSyncTest {

    private val large = AccountPreferences(textScale = TextScale.LARGEST)
    private val dark = AccountPreferences(theme = ThemeMode.DARK)

    private val auth = FakeAuthRepository()
    private val account = FakeAccountPreferencesRepository()
    private val device = FakeDevicePreferencesRepository(account = large)
    private val sync = AccountPreferencesSync(auth, account, device)

    private fun signIn(uid: String) {
        auth.signedInUser.value = AuthenticatedUser(uid = uid, displayName = uid)
    }

    private fun TestScope.follow() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sync.preferences.collect() }
    }

    @Test
    fun `signed out, the copy on the device applies, so the login screen keeps large text`() = runTest {
        var latest: AppPreferences? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sync.preferences.collect { latest = it } }

        assertEquals(large, latest?.account)
        assertTrue(account.observedUids.isEmpty())
    }

    @Test
    fun `the account replaces the copy after signing in on another device`() = runTest {
        follow()
        signIn("pat")

        account.stored.value = DataResult.Success(dark)

        assertEquals(listOf("pat"), account.observedUids)
        assertEquals(dark, device.accountCopy.value)
    }

    @Test
    fun `an account that never saved preferences keeps the current copy`() = runTest {
        follow()
        signIn("new")

        account.stored.value = DataResult.Success(null)

        assertEquals(large, device.accountCopy.value)
    }

    @Test
    fun `a failed read keeps the copy and never blocks the app`() = runTest {
        follow()
        signIn("pat")

        account.stored.value = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)

        assertEquals(large, device.accountCopy.value)
    }

    @Test
    fun `switching account follows the new one`() = runTest {
        follow()
        signIn("pat")
        signIn("cara")

        assertEquals(listOf("pat", "cara"), account.observedUids)
    }

    @Test
    fun `a change applies on the device and goes to the account of who is signed in`() = runTest {
        signIn("pat")

        val result = sync.update { it.copy(theme = ThemeMode.DARK) }

        val expected = large.copy(theme = ThemeMode.DARK)
        assertEquals(DataResult.Success(WriteStatus.SYNCED), result)
        assertEquals(expected, device.accountCopy.value)
        assertEquals(listOf("pat" to expected), account.saved)
    }

    @Test
    fun `offline the change stays applied and waits for the connection`() = runTest {
        signIn("pat")
        account.saveResult = DataResult.Success(WriteStatus.PENDING_SYNC)

        val result = sync.update { it.copy(isSimplified = true) }

        assertEquals(DataResult.Success(WriteStatus.PENDING_SYNC), result)
        assertTrue(device.accountCopy.value.isSimplified)
    }

    @Test
    fun `a refused change returns to the previous value`() = runTest {
        signIn("pat")
        account.saveResult = DataResult.Failure(DataError.PERMISSION_DENIED)

        val result = sync.update { it.copy(theme = ThemeMode.DARK) }

        assertEquals(DataResult.Failure(DataError.PERMISSION_DENIED), result)
        assertEquals(large, device.accountCopy.value)
    }

    @Test
    fun `signed out, a change stays on the device`() = runTest {
        sync.update { it.copy(motion = MotionLevel.OFF) }

        assertEquals(MotionLevel.OFF, device.accountCopy.value.motion)
        assertTrue(account.saved.isEmpty())
    }
}
