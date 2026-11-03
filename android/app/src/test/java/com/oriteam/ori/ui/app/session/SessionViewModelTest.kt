package com.oriteam.ori.ui.app.session

import com.oriteam.ori.domain.auth.AuthenticatedUser
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.local.MoveLocalDataToAccount
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.profile.ProfileLookup
import com.oriteam.ori.domain.profile.UserProfile
import com.oriteam.ori.domain.session.SessionState
import com.oriteam.ori.testing.FakeAacBoardRepository
import com.oriteam.ori.testing.FakeAuthRepository
import com.oriteam.ori.testing.FakeDeviceProfileRepository
import com.oriteam.ori.testing.FakeLocalDataStore
import com.oriteam.ori.testing.FakePhotoStore
import com.oriteam.ori.testing.FakeProfilePhotoStore
import com.oriteam.ori.testing.FakeProfileRepository
import com.oriteam.ori.testing.FakeRoutineRepository
import com.oriteam.ori.testing.FakeSavedMessageRepository
import com.oriteam.ori.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository()
    private val deviceProfiles = FakeDeviceProfileRepository()
    private val localData = FakeLocalDataStore()
    private val user = AuthenticatedUser(uid = "pat", displayName = "Pat")
    private val profile = UserProfile("pat", "Pat")

    private val profilePhotos = FakeProfilePhotoStore()

    private fun TestScope.observedViewModel() = SessionViewModel(
        auth,
        profiles,
        deviceProfiles,
        localData,
        MoveLocalDataToAccount(
            localData,
            FakeRoutineRepository(),
            FakeAacBoardRepository(),
            FakeSavedMessageRepository(),
            FakePhotoStore()
        ),
        profilePhotos
    ).also {
        // stateIn(WhileSubscribed) only works while someone collects, like the UI does.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.state.collect() }
    }

    @Test
    fun `signed out when there is no authenticated user`() = runTest {
        val viewModel = observedViewModel()

        assertEquals(SessionState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `account without profile must create one before any access`() = runTest {
        val viewModel = observedViewModel()
        auth.signedInUser.value = user
        assertEquals(SessionState.Initializing, viewModel.state.value)

        profiles.lookupFor("pat").value = ProfileLookup.Missing

        assertEquals(SessionState.ProfileRequired(user), viewModel.state.value)
    }

    @Test
    fun `profile creation activates the session without a new login`() = runTest {
        val viewModel = observedViewModel()
        auth.signedInUser.value = user
        profiles.lookupFor("pat").value = ProfileLookup.Missing

        profiles.lookupFor("pat").value = ProfileLookup.Found(profile)

        assertEquals(SessionState.Active(profile), viewModel.state.value)
    }

    @Test
    fun `unreadable profile can be retried and signing out ends the session`() = runTest {
        val viewModel = observedViewModel()
        auth.signedInUser.value = user
        profiles.lookupFor("pat").value = ProfileLookup.Failure(DataError.NETWORK_UNAVAILABLE)
        assertEquals(SessionState.ProfileUnavailable(user, DataError.NETWORK_UNAVAILABLE), viewModel.state.value)
        val subscriptionsBeforeRetry = profiles.subscriptions

        viewModel.retry()
        assertEquals(subscriptionsBeforeRetry + 1, profiles.subscriptions)

        viewModel.signOut()
        assertEquals(SessionState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `switching accounts never shows the previous profile`() = runTest {
        val viewModel = observedViewModel()
        auth.signedInUser.value = user
        profiles.lookupFor("pat").value = ProfileLookup.Found(profile)

        auth.signedInUser.value = AuthenticatedUser(uid = "otto", displayName = "Otto")

        assertEquals(SessionState.Initializing, viewModel.state.value)
    }

    @Test
    fun `continuing without an account opens the app without any network`() = runTest {
        val viewModel = observedViewModel()
        assertEquals(SessionState.SignedOut, viewModel.state.value)

        viewModel.onContinueWithoutAccount()

        val active = viewModel.state.value as SessionState.Active
        assertEquals(true, active.profile.isLocal)
        assertEquals(0, profiles.subscriptions)
    }

    @Test
    fun `the local profile comes back when the app opens again`() = runTest {
        deviceProfiles.startLocalProfile()

        val viewModel = observedViewModel()

        assertEquals(true, (viewModel.state.value as SessionState.Active).profile.isLocal)
    }

    @Test
    fun `signing in wins over the local profile`() = runTest {
        deviceProfiles.startLocalProfile()
        profiles.lookupFor("pat").value = ProfileLookup.Found(profile)
        val viewModel = observedViewModel()

        auth.signedInUser.value = user

        assertEquals(SessionState.Active(profile), viewModel.state.value)
    }

    @Test
    fun `offline with a copy of the profile the account keeps working`() = runTest {
        deviceProfiles.saveAccountProfileCopy(profile)
        profiles.lookupFor("pat").value = ProfileLookup.Failure(DataError.NETWORK_UNAVAILABLE)
        auth.signedInUser.value = user

        val viewModel = observedViewModel()

        assertEquals(SessionState.Active(profile), viewModel.state.value)
    }

    @Test
    fun `offline without a copy the failure is shown, not the login`() = runTest {
        profiles.lookupFor("pat").value = ProfileLookup.Failure(DataError.NETWORK_UNAVAILABLE)
        auth.signedInUser.value = user

        val viewModel = observedViewModel()

        assertEquals(SessionState.ProfileUnavailable(user, DataError.NETWORK_UNAVAILABLE), viewModel.state.value)
    }

    @Test
    fun `an active account keeps a copy and ends the local profile once its data moved`() = runTest {
        deviceProfiles.startLocalProfile()
        val viewModel = observedViewModel()

        viewModel.onAccountActive(profile)

        assertEquals(profile, deviceProfiles.accountCopy.value)
        assertEquals(null, deviceProfiles.localProfile.value)
    }

    @Test
    fun `the picture follows the person when the device data moves to the account`() = runTest {
        profilePhotos.photos.value = mapOf(LOCAL_OWNER_UID to "local.jpg")
        val viewModel = observedViewModel()

        viewModel.onAccountActive(UserProfile("account", "Ana"))

        assertEquals(mapOf("account" to "local.jpg"), profilePhotos.photos.value)
    }

    @Test
    fun `erasing returns to the welcome`() = runTest {
        deviceProfiles.startLocalProfile()
        val viewModel = observedViewModel()

        viewModel.eraseLocalData()

        assertEquals(true, localData.wasErased)
        assertEquals(SessionState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `a local name stays on the device and an account name goes to the account`() = runTest {
        deviceProfiles.startLocalProfile()
        val viewModel = observedViewModel()

        viewModel.rename(deviceProfiles.localProfile.value!!, "Ana") {}
        viewModel.rename(profile, "Patrícia") {}

        assertEquals("Ana", deviceProfiles.localProfile.value?.displayName)
        assertEquals(listOf("pat" to "Patrícia"), profiles.renameCalls)
    }
}
