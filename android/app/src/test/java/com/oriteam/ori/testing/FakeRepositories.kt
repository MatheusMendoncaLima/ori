package com.oriteam.ori.testing

import com.oriteam.ori.domain.carelink.CareInvitation
import com.oriteam.ori.domain.carelink.CareLink
import com.oriteam.ori.domain.carelink.CareLinkPermissions
import com.oriteam.ori.domain.carelink.CareLinkRepository
import com.oriteam.ori.domain.carelink.CareLinkSide
import com.oriteam.ori.domain.carelink.InvitationRepository
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.profile.ProfileLookup
import com.oriteam.ori.domain.profile.ProfileRepository
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineSnapshot
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import java.time.LocalDate

/** Profiles kept in memory. [lookups] drives what observers see; subscriptions are counted. */
class FakeProfileRepository : ProfileRepository {
    val lookups = mutableMapOf<String, MutableStateFlow<ProfileLookup?>>()
    var createResult: DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)
    val createCalls = mutableListOf<Pair<String, String>>()
    val renameCalls = mutableListOf<Pair<String, String>>()
    var subscriptions = 0

    fun lookupFor(uid: String) = lookups.getOrPut(uid) { MutableStateFlow(null) }

    override fun observeProfile(uid: String): Flow<ProfileLookup> =
        lookupFor(uid).filterNotNull().onStart { subscriptions++ }

    override suspend fun createProfile(uid: String, displayName: String): DataResult<WriteStatus> {
        createCalls += uid to displayName
        return createResult
    }

    override suspend fun updateDisplayName(uid: String, displayName: String): DataResult<WriteStatus> {
        renameCalls += uid to displayName
        return createResult
    }
}

/** A write the fake received: which operation, for which activity, with which draft. */
data class RoutineWrite(val operation: String, val ownerUid: String, val activityId: String, val draft: ActivityDraft?)

class FakeRoutineRepository : RoutineRepository {
    /** What observers of any day receive. */
    val routine = MutableStateFlow<DataResult<RoutineSnapshot>?>(null)

    /** When a day is here, it answers with its own activities instead of [routine]. */
    var routineByDay: Map<LocalDate, DataResult<RoutineSnapshot>> = emptyMap()
    val observedDays = mutableListOf<LocalDate>()

    /** Answer of the one-time day read used for duplicate detection. */
    var dayResult: DataResult<List<RoutineTask>> = DataResult.Success(emptyList())
    var writeResult: DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)
    var setDoneResult: DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)

    /** When set, what creating a repeating activity answers instead of [writeResult] (a full account, say). */
    var seriesResult: DataResult<WriteStatus>? = null
    val writes = mutableListOf<RoutineWrite>()

    /** When set, writes wait for it: simulates a slow network. */
    var writeGate: CompletableDeferred<Unit>? = null
    val setDoneCalls = mutableListOf<Triple<String, String, Boolean>>()
    private var reservedIds = 0

    override fun observeDay(
        ownerUid: String,
        viewer: RoutineViewer,
        date: LocalDate
    ): Flow<DataResult<RoutineSnapshot>> =
        (routineByDay[date]?.let { flowOf(it) } ?: routine.filterNotNull()).onStart { observedDays += date }

    override suspend fun getDay(ownerUid: String, viewer: RoutineViewer, date: LocalDate) = dayResult

    /** The days of each period read: one entry is one read, as one range query in Firestore. */
    val periodReads = mutableListOf<List<LocalDate>>()

    override suspend fun getDays(
        ownerUid: String,
        viewer: RoutineViewer,
        dates: List<LocalDate>
    ): DataResult<Map<LocalDate, List<RoutineTask>>> {
        periodReads += dates
        return when (val day = dayResult) {
            is DataResult.Failure -> day
            is DataResult.Success -> DataResult.Success(dates.associateWith { day.value })
        }
    }

    override fun newActivityId(ownerUid: String): String = "reserved-${++reservedIds}"

    override suspend fun createActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer
    ) = draft.repeat?.let { seriesResult } ?: record(RoutineWrite("create", ownerUid, activityId, draft))

    override suspend fun updateActivity(
        ownerUid: String,
        activityId: String,
        draft: ActivityDraft,
        viewer: RoutineViewer
    ) = record(RoutineWrite("update", ownerUid, activityId, draft))

    override suspend fun deleteActivity(ownerUid: String, activityId: String) =
        record(RoutineWrite("delete", ownerUid, activityId, null))

    override suspend fun setTaskDone(ownerUid: String, taskId: String, isDone: Boolean): DataResult<WriteStatus> {
        setDoneCalls += Triple(ownerUid, taskId, isDone)
        return setDoneResult
    }

    private suspend fun record(write: RoutineWrite): DataResult<WriteStatus> {
        writeGate?.await()
        writes += write
        return writeResult
    }
}

class FakeCareLinkRepository : CareLinkRepository {
    val links = MutableStateFlow<DataResult<List<CareLink>>>(DataResult.Success(emptyList()))
    var actionResult: DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)
    val revokeCalls = mutableListOf<String>()
    val permissionCalls = mutableListOf<Pair<String, CareLinkPermissions>>()

    override fun observeActiveLinks(uid: String, side: CareLinkSide): Flow<DataResult<List<CareLink>>> = links

    override suspend fun revoke(linkId: String): DataResult<WriteStatus> {
        revokeCalls += linkId
        return actionResult
    }

    override suspend fun updatePermissions(linkId: String, permissions: CareLinkPermissions): DataResult<WriteStatus> {
        permissionCalls += linkId to permissions
        return actionResult
    }
}

class FakeInvitationRepository : InvitationRepository {
    var createResult: DataResult<CareInvitation> = DataResult.Success(CareInvitation("ABCD2345", 0L))
    var acceptResult: DataResult<String> = DataResult.Success("Cara")
    val acceptCalls = mutableListOf<String>()

    override suspend fun createInvitation(): DataResult<CareInvitation> = createResult

    override suspend fun acceptInvitation(code: String): DataResult<String> {
        acceptCalls += code
        return acceptResult
    }
}
