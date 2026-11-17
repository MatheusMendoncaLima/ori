package com.oriteam.ori.ui.app.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.auth.AuthRepository
import com.oriteam.ori.domain.auth.AuthenticatedUser
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.local.LocalDataMoveResult
import com.oriteam.ori.domain.local.LocalDataStore
import com.oriteam.ori.domain.local.MoveLocalDataToAccount
import com.oriteam.ori.domain.profile.DeviceProfileRepository
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.profile.ProfileLookup
import com.oriteam.ori.domain.profile.ProfilePhotoStore
import com.oriteam.ori.domain.profile.ProfileRepository
import com.oriteam.ori.domain.profile.UserProfile
import com.oriteam.ori.domain.session.SessionState
import com.oriteam.ori.domain.session.sessionStateFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Single source of truth for "who is using the app". The root composable shows a different area
 * for each [SessionState]. A person without an account is active too, with a profile on the device.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val deviceProfiles: DeviceProfileRepository,
    private val localData: LocalDataStore,
    private val moveLocalData: MoveLocalDataToAccount,
    private val profilePhotos: ProfilePhotoStore
) : ViewModel() {

    private val retryRequests = MutableStateFlow(0)
    private val _localDataMove = MutableStateFlow<LocalDataMoveResult?>(null)

    /** Last result of moving device data to the account; null while nothing needs saying. */
    val localDataMove: StateFlow<LocalDataMoveResult?> = _localDataMove.asStateFlow()

    /** The move in progress, if any: a second sign-in event while it runs does not start another one. */
    private var moving: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<SessionState> = combine(authRepository.signedInUser, retryRequests) { user, _ -> user }
        .flatMapLatest { user ->
            if (user == null) {
                deviceProfiles.localProfile.map { local ->
                    local?.let { SessionState.Active(it) } ?: SessionState.SignedOut
                }
            } else {
                accountSession(user)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SessionState.Initializing)

    /**
     * Without network and without a cached document the profile cannot be read, but the person is
     * still signed in. The copy on the device lets them keep using the app; the data they create
     * waits in the SDK queue and goes out when the connection returns.
     */
    private fun accountSession(user: AuthenticatedUser): Flow<SessionState> =
        combine(
            profileRepository.observeProfile(user.uid),
            deviceProfiles.accountProfileCopy(user.uid)
        ) { lookup, copy ->
            val usable = if (lookup is ProfileLookup.Failure && copy != null) ProfileLookup.Found(copy) else lookup
            sessionStateFor(user, usable)
        }.onStart { emit(sessionStateFor(user, lookup = null)) }

    /** Called when an account session becomes active: keeps the copy and moves any device data. */
    fun onAccountActive(profile: UserProfile) {
        viewModelScope.launch { deviceProfiles.saveAccountProfileCopy(profile) }
        moveDeviceData(profile.uid)
    }

    fun onRetryLocalDataMove(profile: UserProfile) = moveDeviceData(profile.uid)

    fun onDismissLocalDataMove() {
        _localDataMove.value = null
    }

    private fun moveDeviceData(uid: String) {
        if (moving?.isActive == true) return
        moving = viewModelScope.launch {
            val result = moveLocalData(uid)
            if (result == LocalDataMoveResult.COMPLETED) {
                profilePhotos.movePhoto(LOCAL_OWNER_UID, uid)
                deviceProfiles.clearLocalProfile()
            } else {
                _localDataMove.value = result
            }
        }
    }

    /** "Continuar sem conta": no network, no account and no data leaves the device. */
    fun onContinueWithoutAccount() {
        viewModelScope.launch { deviceProfiles.startLocalProfile() }
    }

    /** Reads the profile again, for example after a network failure. */
    fun retry() {
        retryRequests.update { it + 1 }
    }

    /**
     * Leaves the account. The data of the account stays in the account, and the copy of its profile kept for
     * opening offline is forgotten. The Firestore cache stays: the next account only queries its own data,
     * and clearing the cache would mean stopping Firestore for the rest of the session (see perfil-e-conta.md).
     */
    fun signOut() {
        _localDataMove.value = null
        authRepository.signOut()
        viewModelScope.launch { deviceProfiles.clearAccountProfileCopy() }
    }

    /**
     * The person did not agree: an account signs out; without an account the app goes back to the welcome. Nothing
     * is erased: what was on the device stays there, and agreeing later opens it again.
     */
    fun declineConsent(profile: UserProfile) {
        if (profile.isLocal) {
            viewModelScope.launch { deviceProfiles.clearLocalProfile() }
        } else {
            signOut()
        }
    }

    /**
     * Erases everything the person created without an account and goes back to the welcome. [onResult] receives the
     * failure, when the database could not erase (everything stays, and the screen says so), or null.
     */
    fun eraseLocalData(onResult: (DataError?) -> Unit = {}) {
        viewModelScope.launch {
            when (val erased = localData.eraseAll()) {
                is DataResult.Failure -> onResult(erased.error)
                is DataResult.Success -> {
                    profilePhotos.removePhoto(LOCAL_OWNER_UID)
                    deviceProfiles.clearLocalProfile()
                    onResult(null)
                }
            }
        }
    }

    /** [onResult] receives the failure, or null when the name was accepted. */
    fun rename(profile: UserProfile, name: String, onResult: (DataError?) -> Unit) {
        viewModelScope.launch {
            if (profile.isLocal) {
                deviceProfiles.renameLocalProfile(name)
                onResult(null)
            } else {
                onResult((profileRepository.updateDisplayName(profile.uid, name) as? DataResult.Failure)?.error)
            }
        }
    }
}
