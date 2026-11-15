package com.oriteam.ori.domain.profile

import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow

/**
 * Who is using the app. Every account can do the same: follow a routine, communicate, support
 * other people and be supported. What one person may do with the data of another is decided by
 * the care link between them. Without an account the profile lives only on this device.
 */
data class UserProfile(
    val uid: String,
    val displayName: String
) {
    val isLocal: Boolean get() = uid == LOCAL_OWNER_UID

    /** The first word of the name, for a short greeting ("Olá, Ana!"); empty when there is no name. */
    val firstName: String get() = displayName.trim().split(WHITESPACE).first()
}

private val WHITESPACE = Regex("\\s+")

/**
 * Owner of the data kept only on this device. Firebase ids are 28 characters long, so this value
 * never collides with an account, and repositories use it to choose where the data lives.
 */
const val LOCAL_OWNER_UID = "local"

/** Result of looking up a profile. [Missing] is a valid state: the account must create one. */
sealed interface ProfileLookup {
    data class Found(val profile: UserProfile) : ProfileLookup

    data object Missing : ProfileLookup

    data class Failure(val error: DataError) : ProfileLookup
}

interface ProfileRepository {
    /** Follows the profile of [uid]; emits again if it is created or changed. */
    fun observeProfile(uid: String): Flow<ProfileLookup>

    /** Creates the profile once. */
    suspend fun createProfile(uid: String, displayName: String): DataResult<WriteStatus>

    suspend fun updateDisplayName(uid: String, displayName: String): DataResult<WriteStatus>
}
