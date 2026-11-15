package com.oriteam.ori.domain.profile

import kotlinx.coroutines.flow.Flow

/** What this device remembers about who uses it, without depending on the network. */
interface DeviceProfileRepository {
    /** The profile of "continue without an account"; null until the person chooses it. */
    val localProfile: Flow<UserProfile?>

    /** The last account profile read here, so a signed-in person opens the app offline. */
    fun accountProfileCopy(uid: String): Flow<UserProfile?>

    suspend fun startLocalProfile()

    suspend fun renameLocalProfile(displayName: String)

    /** Forgets the local profile. Its data is handled by [com.oriteam.ori.domain.local.LocalDataStore]. */
    suspend fun clearLocalProfile()

    suspend fun saveAccountProfileCopy(profile: UserProfile)

    /** Forgets the copy of the account's profile: leaving the account leaves nothing of it in this file. */
    suspend fun clearAccountProfileCopy()
}
