package com.oriteam.ori.data.account

import com.oriteam.ori.data.aac.toStoredImage
import com.oriteam.ori.data.firestore.FirestoreSchema
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.storedKey
import com.oriteam.ori.domain.account.AccountDataExporter
import com.oriteam.ori.domain.account.accountExportJson
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.legal.ConsentRepository
import com.oriteam.ori.domain.local.LocalBoard
import com.oriteam.ori.domain.local.LocalDataStore
import com.oriteam.ori.domain.profile.DeviceProfileRepository
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.routine.RoutineSeries
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.toStoredText
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** What a person without an account has on this device, in the same sections as the account's export. */
class LocalAccountDataExporter @Inject constructor(
    private val localData: LocalDataStore,
    private val deviceProfiles: DeviceProfileRepository,
    private val consents: ConsentRepository,
    private val clock: Clock
) : AccountDataExporter {
    override suspend fun export(ownerUid: String): DataResult<String> {
        val data = localData.snapshot()
        val consent = (consents.getConsent(LOCAL_OWNER_UID) as? DataResult.Success)?.value
        val sections = linkedMapOf(
            "app" to "ORI",
            "exportedAt" to Instant.now(clock).toString(),
            "profile" to deviceProfiles.localProfile.first()?.let { mapOf("displayName" to it.displayName) },
            "consent" to consent?.let {
                mapOf(
                    "version" to it.version,
                    "isAdult" to it.isAdult,
                    "guardianConsent" to it.hasGuardianConsent,
                    "acceptedAt" to Instant.ofEpochMilli(it.acceptedAtMillis).toString()
                )
            },
            "routineActivities" to data.tasks.map(::taskSection),
            "repeatingActivities" to data.series.map(::seriesSection),
            "boards" to data.boards.map(::boardSection),
            "savedPhrases" to data.savedMessages.map(::savedSection)
        )
        return DataResult.Success(accountExportJson(sections))
    }
}

private fun taskSection(task: RoutineTask): Map<String, Any?> =
    mapOf(
        "id" to task.id,
        FirestoreSchema.TITLE to task.title,
        FirestoreSchema.DATE to task.date.toString(),
        FirestoreSchema.SCHEDULED_TIME to task.scheduledTime?.toString(),
        FirestoreSchema.DESCRIPTION to task.description,
        FirestoreSchema.IS_DONE to task.isDone
    )

private fun seriesSection(series: RoutineSeries): Map<String, Any?> =
    mapOf(
        "id" to series.id,
        FirestoreSchema.TITLE to series.title,
        FirestoreSchema.DATE to series.startDate.toString(),
        FirestoreSchema.SCHEDULED_TIME to series.scheduledTime?.toString(),
        FirestoreSchema.DESCRIPTION to series.description,
        FirestoreSchema.REPEAT to series.rule.toStoredText(),
        FirestoreSchema.SKIPPED_DATES to series.skippedDates.map { it.toString() }
    )

private fun boardSection(local: LocalBoard): Map<String, Any?> =
    mapOf(
        "id" to local.board.id,
        FirestoreSchema.NAME to local.board.name,
        FirestoreSchema.LANGUAGE to local.board.language,
        "symbols" to local.symbols.map { symbol ->
            mapOf(
                "id" to symbol.id,
                FirestoreSchema.LABEL to symbol.label,
                FirestoreSchema.PHRASE to symbol.phrase,
                FirestoreSchema.IMAGE to symbol.image?.toStoredImage(),
                FirestoreSchema.COLOR to symbol.color?.key,
                FirestoreSchema.WORD_CLASS to symbol.wordClass?.storedKey
            )
        }
    )

private fun savedSection(it: SavedMessage): Map<String, Any?> =
    mapOf(
        "id" to it.id,
        FirestoreSchema.TEXT to it.text,
        FirestoreSchema.LANGUAGE to it.language,
        FirestoreSchema.EMOJI to it.emoji,
        FirestoreSchema.POSITION to it.position
    )

/** The export of the account, or of this device for someone without one; the screen does not know which. */
class RoutedAccountDataExporter(
    private val account: AccountDataExporter,
    private val local: AccountDataExporter
) : AccountDataExporter {
    @Inject constructor(firestore: FirestoreAccountDataExporter, device: LocalAccountDataExporter) :
        this(account = firestore, local = device)

    override suspend fun export(ownerUid: String) =
        (if (ownerUid == LOCAL_OWNER_UID) local else account).export(ownerUid)
}
