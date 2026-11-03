package com.oriteam.ori.domain.preferences

import com.oriteam.ori.domain.auth.AuthRepository
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single path of the account preferences. The screen always reads the device copy, so the choice
 * applies at once, offline and on the login screen. While someone is signed in, the copy follows
 * the account.
 *
 * An account that never saved preferences keeps the current copy. Someone who needs large text
 * should not lose it on the first sign-in. A failed read also keeps the copy: appearance is never
 * a reason to block the app.
 */
class AccountPreferencesSync @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountPreferencesRepository,
    private val deviceRepository: DevicePreferencesRepository
) {
    /** Current preferences. The account is followed only while this flow is collected. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val preferences: Flow<AppPreferences> = channelFlow {
        launch {
            authRepository.signedInUser
                .map { it?.uid }
                .distinctUntilChanged()
                .flatMapLatest { uid -> uid?.let(accountRepository::observe) ?: emptyFlow() }
                .collect { result ->
                    val stored = (result as? DataResult.Success)?.value
                    if (stored != null) deviceRepository.saveAccountCopy(stored)
                }
        }
        combine(deviceRepository.accountCopy, deviceRepository.preferences, ::AppPreferences)
            .distinctUntilChanged()
            .collect { send(it) }
    }

    /**
     * Applies the change on the device and sends it to the account. If the account refuses, the
     * previous value returns, so the screen never shows a choice that was not saved. Signed out,
     * the change stays on the device.
     */
    suspend fun update(transform: (AccountPreferences) -> AccountPreferences): DataResult<WriteStatus> {
        val previous = deviceRepository.accountCopy.first()
        val updated = transform(previous)
        deviceRepository.saveAccountCopy(updated)
        val uid = authRepository.currentUser()?.uid ?: return DataResult.Success(WriteStatus.SYNCED)
        val result = accountRepository.save(uid, updated)
        if (result is DataResult.Failure) deviceRepository.saveAccountCopy(previous)
        return result
    }
}
