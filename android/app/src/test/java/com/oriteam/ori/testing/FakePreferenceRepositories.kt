package com.oriteam.ori.testing

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.AccountPreferencesRepository
import com.oriteam.ori.domain.preferences.DevicePreferences
import com.oriteam.ori.domain.preferences.DevicePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart

/** The account document. [stored] drives what observers see; subscribers are recorded by uid. */
class FakeAccountPreferencesRepository : AccountPreferencesRepository {
    val stored = MutableStateFlow<DataResult<AccountPreferences?>?>(null)
    var saveResult: DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)
    val observedUids = mutableListOf<String>()
    val saved = mutableListOf<Pair<String, AccountPreferences>>()

    override fun observe(uid: String): Flow<DataResult<AccountPreferences?>> =
        stored.filterNotNull().onStart { observedUids += uid }

    override suspend fun save(uid: String, preferences: AccountPreferences): DataResult<WriteStatus> {
        saved += uid to preferences
        return saveResult
    }
}

class FakeDevicePreferencesRepository(
    account: AccountPreferences = AccountPreferences(),
    device: DevicePreferences = DevicePreferences()
) : DevicePreferencesRepository {
    override val preferences = MutableStateFlow(device)
    override val accountCopy = MutableStateFlow(account)

    override suspend fun update(transform: (DevicePreferences) -> DevicePreferences) {
        preferences.value = transform(preferences.value)
    }

    override suspend fun saveAccountCopy(preferences: AccountPreferences) {
        accountCopy.value = preferences
    }
}
