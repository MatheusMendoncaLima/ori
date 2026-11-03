package com.oriteam.ori.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oriteam.ori.domain.profile.DeviceProfileRepository
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.profile.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Small facts about the people using this device, in the same local file as the preferences. */
class DataStoreDeviceProfileRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : DeviceProfileRepository {

    override val localProfile: Flow<UserProfile?> = dataStore.data
        .map { stored ->
            if (stored[LOCAL_ACTIVE_KEY] == true) {
                UserProfile(uid = LOCAL_OWNER_UID, displayName = stored[LOCAL_NAME_KEY].orEmpty())
            } else {
                null
            }
        }
        .distinctUntilChanged()

    override fun accountProfileCopy(uid: String): Flow<UserProfile?> = dataStore.data
        .map { stored ->
            stored[ACCOUNT_NAME_KEY]
                ?.takeIf { stored[ACCOUNT_UID_KEY] == uid }
                ?.let { UserProfile(uid = uid, displayName = it) }
        }
        .distinctUntilChanged()

    override suspend fun startLocalProfile() {
        dataStore.edit { it[LOCAL_ACTIVE_KEY] = true }
    }

    override suspend fun renameLocalProfile(displayName: String) {
        dataStore.edit { it[LOCAL_NAME_KEY] = displayName }
    }

    override suspend fun clearLocalProfile() {
        dataStore.edit {
            it.remove(LOCAL_ACTIVE_KEY)
            it.remove(LOCAL_NAME_KEY)
        }
    }

    override suspend fun saveAccountProfileCopy(profile: UserProfile) {
        dataStore.edit {
            it[ACCOUNT_UID_KEY] = profile.uid
            it[ACCOUNT_NAME_KEY] = profile.displayName
        }
    }

    private companion object {
        val LOCAL_ACTIVE_KEY = booleanPreferencesKey("local_profile_active")
        val LOCAL_NAME_KEY = stringPreferencesKey("local_profile_name")
        val ACCOUNT_UID_KEY = stringPreferencesKey("account_profile_uid")
        val ACCOUNT_NAME_KEY = stringPreferencesKey("account_profile_name")
    }
}
