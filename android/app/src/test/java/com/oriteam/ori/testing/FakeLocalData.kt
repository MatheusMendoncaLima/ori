package com.oriteam.ori.testing

import com.oriteam.ori.domain.aac.AacUsageRepository
import com.oriteam.ori.domain.local.LocalDataSnapshot
import com.oriteam.ori.domain.local.LocalDataStore
import com.oriteam.ori.domain.photos.PhotoImportError
import com.oriteam.ori.domain.profile.DeviceProfileRepository
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.profile.ProfilePhotoStore
import com.oriteam.ori.domain.profile.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeDeviceProfileRepository(hasLocalProfile: Boolean = false) : DeviceProfileRepository {
    override val localProfile = MutableStateFlow(
        if (hasLocalProfile) UserProfile(LOCAL_OWNER_UID, "") else null
    )
    val accountCopy = MutableStateFlow<UserProfile?>(null)

    override fun accountProfileCopy(uid: String): Flow<UserProfile?> =
        accountCopy.map { copy -> copy?.takeIf { it.uid == uid } }

    override suspend fun startLocalProfile() {
        localProfile.value = UserProfile(LOCAL_OWNER_UID, "")
    }

    override suspend fun renameLocalProfile(displayName: String) {
        localProfile.value = localProfile.value?.copy(displayName = displayName)
    }

    override suspend fun clearLocalProfile() {
        localProfile.value = null
    }

    override suspend fun saveAccountProfileCopy(profile: UserProfile) {
        accountCopy.value = profile
    }
}

class FakeLocalDataStore(var data: LocalDataSnapshot = LocalDataSnapshot()) : LocalDataStore {
    var wasErased = false

    override suspend fun snapshot() = data

    override suspend fun removeTask(id: String) {
        data = data.copy(tasks = data.tasks.filterNot { it.id == id })
    }

    override suspend fun removeSeries(id: String) {
        data = data.copy(series = data.series.filterNot { it.id == id })
    }

    override suspend fun removeBoard(id: String) {
        data = data.copy(boards = data.boards.filterNot { it.board.id == id })
    }

    override suspend fun removeSymbol(id: String) {
        data = data.copy(
            boards = data.boards.map { board -> board.copy(symbols = board.symbols.filterNot { it.id == id }) }
        )
    }

    override suspend fun removeSavedMessage(id: String) {
        data = data.copy(savedMessages = data.savedMessages.filterNot { it.id == id })
    }

    override suspend fun eraseAll() {
        wasErased = true
        data = LocalDataSnapshot()
    }
}

class FakeAacUsageRepository : AacUsageRepository {
    val recorded = mutableListOf<LocalDate>()

    override suspend fun recordCommunication(date: LocalDate) {
        recorded += date
    }

    override suspend fun countBetween(start: LocalDate, end: LocalDate): Int =
        recorded.count { !it.isBefore(start) && !it.isAfter(end) }
}

class FakeProfilePhotoStore : ProfilePhotoStore {
    val photos = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observePhoto(uid: String): Flow<String?> = photos.map { it[uid] }

    override suspend fun setPhoto(uid: String, sourceUri: String): PhotoImportError? {
        photos.value += uid to sourceUri
        return null
    }

    override suspend fun removePhoto(uid: String) {
        photos.value -= uid
    }

    override suspend fun movePhoto(fromUid: String, toUid: String) {
        photos.value[fromUid]?.let { photos.value = photos.value - fromUid + (toUid to it) }
    }
}
