package com.oriteam.ori.testing

import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SavedMessageRepository
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.SpeechSynthesizer
import com.oriteam.ori.domain.aac.SpeechVolume
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.VoiceOption
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.ListSnapshot
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.photos.CloudPhoto
import com.oriteam.ori.domain.photos.CloudPhotoRepository
import com.oriteam.ori.domain.photos.PhotoImportResult
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.domain.photos.StoredPhoto
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.AacPreferencesRepository
import com.oriteam.ori.domain.preferences.VoiceSettings
import com.oriteam.ori.domain.preferences.VoiceSound
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** A write the fake received, described in words so tests read like the operation. */
data class AacWrite(val operation: String, val target: String, val detail: Any? = null)

class FakeAacBoardRepository : AacBoardRepository {
    val boards = MutableStateFlow<DataResult<ListSnapshot<AacBoard>>?>(null)

    /** Symbols that observers of any board receive. */
    val symbols = MutableStateFlow<DataResult<ListSnapshot<AacSymbol>>?>(null)
    val observedBoards = mutableListOf<String>()
    var symbolsResult: DataResult<List<AacSymbol>> = DataResult.Success(emptyList())
    var writeResult: DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)

    /** When set, writes wait for it: simulates a slow network. */
    var writeGate: CompletableDeferred<Unit>? = null
    val writes = mutableListOf<AacWrite>()
    private var reservedIds = 0

    override fun observeBoards(ownerUid: String): Flow<DataResult<ListSnapshot<AacBoard>>> = boards.filterNotNull()

    override suspend fun getBoards(ownerUid: String): DataResult<ListSnapshot<AacBoard>> =
        boards.value ?: DataResult.Success(ListSnapshot(emptyList()))

    override fun observeSymbols(ownerUid: String, boardId: String): Flow<DataResult<ListSnapshot<AacSymbol>>> =
        symbols.filterNotNull().onStart { observedBoards += boardId }

    override suspend fun getSymbols(ownerUid: String, boardId: String) = symbolsResult

    override fun newSymbolId(ownerUid: String, boardId: String) = "reserved-${++reservedIds}"

    /** The language each new board was created in, in order. */
    val boardLanguages = mutableListOf<String>()

    override suspend fun createBoard(
        ownerUid: String,
        name: String,
        position: Int,
        language: String
    ): DataResult<WriteStatus> {
        boardLanguages += language
        return record(AacWrite("createBoard", name, position))
    }

    override suspend fun createBoardWithSymbols(
        ownerUid: String,
        name: String,
        position: Int,
        symbols: List<SymbolDraft>,
        language: String
    ): DataResult<WriteStatus> {
        boardLanguages += language
        return record(AacWrite("createBoardWithSymbols", name, symbols))
    }

    override suspend fun renameBoard(ownerUid: String, boardId: String, name: String) =
        record(AacWrite("renameBoard", boardId, name))

    override suspend fun deleteBoard(ownerUid: String, boardId: String) = record(AacWrite("deleteBoard", boardId))

    override suspend fun createSymbol(
        ownerUid: String,
        boardId: String,
        symbolId: String,
        draft: SymbolDraft,
        position: Int
    ) = record(AacWrite("createSymbol", symbolId, draft to position))

    override suspend fun updateSymbol(ownerUid: String, boardId: String, symbolId: String, draft: SymbolDraft) =
        record(AacWrite("updateSymbol", symbolId, draft))

    override suspend fun deleteSymbol(ownerUid: String, boardId: String, symbolId: String) =
        record(AacWrite("deleteSymbol", symbolId))

    override suspend fun reorderBoards(ownerUid: String, positions: Map<String, Int>) =
        record(AacWrite("reorderBoards", ownerUid, positions))

    override suspend fun reorderSymbols(ownerUid: String, boardId: String, positions: Map<String, Int>) =
        record(AacWrite("reorderSymbols", boardId, positions))

    private suspend fun record(write: AacWrite): DataResult<WriteStatus> {
        writeGate?.await()
        writes += write
        return writeResult
    }
}

class FakeSavedMessageRepository : SavedMessageRepository {
    val messages = MutableStateFlow<DataResult<List<SavedMessage>>?>(null)
    var subscriptions = 0
    var writeResult: DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)
    val saved = mutableListOf<String>()
    val deleted = mutableListOf<String>()

    override fun observeSavedMessages(ownerUid: String): Flow<DataResult<List<SavedMessage>>> =
        messages.filterNotNull().onStart { subscriptions++ }

    /** The language each message was saved in, in order. */
    val savedLanguages = mutableListOf<String>()

    /** The emoji each message was saved with, in order. */
    val savedEmojis = mutableListOf<String?>()
    val savedPositions = mutableListOf<Int?>()
    val emojiChanges = mutableListOf<Pair<String, String?>>()

    /** The places written by each move, in order. */
    val reorders = mutableListOf<Map<String, Int>>()

    override suspend fun saveMessage(
        ownerUid: String,
        text: String,
        language: String,
        emoji: String?,
        position: Int?
    ): DataResult<WriteStatus> {
        saved += text
        savedLanguages += language
        savedEmojis += emoji
        savedPositions += position
        return writeResult
    }

    override suspend fun reorderSavedMessages(ownerUid: String, positions: Map<String, Int>): DataResult<WriteStatus> {
        reorders += positions
        return writeResult
    }

    override suspend fun setSavedMessageEmoji(
        ownerUid: String,
        messageId: String,
        emoji: String?
    ): DataResult<WriteStatus> {
        emojiChanges += messageId to emoji
        return writeResult
    }

    override suspend fun deleteSavedMessage(ownerUid: String, messageId: String): DataResult<WriteStatus> {
        deleted += messageId
        return writeResult
    }
}

/** The volumes of the voice, one per sound: [mutedSounds] are at zero; [raise] turns one up, as on a device. */
class FakeSpeechVolume : SpeechVolume {
    val mutedSounds = mutableSetOf<VoiceSound>()
    val raised = mutableListOf<VoiceSound>()

    /** Every volume at zero, or none. */
    var muted: Boolean
        get() = mutedSounds.isNotEmpty()
        set(value) {
            if (value) mutedSounds.addAll(VoiceSound.entries) else mutedSounds.clear()
        }

    override fun isMuted(sound: VoiceSound) = sound in mutedSounds

    override fun raise(sound: VoiceSound) {
        raised += sound
        mutedSounds -= sound
    }
}

/** Records what would be spoken. [state] decides whether a voice is available. */
class FakeSpeechSynthesizer(initial: SpeechState = SpeechState.READY) : SpeechSynthesizer {
    override val state = MutableStateFlow(initial)
    override val voices = MutableStateFlow<List<VoiceOption>>(emptyList())
    override val failures = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val spoken = mutableListOf<String>()

    /** Every configuration received, the latest last; [spokenWith] pairs each text with the one in force. */
    val configured = mutableListOf<VoiceSettings>()
    val spokenWith = mutableListOf<Pair<String, VoiceSettings?>>()

    override fun configure(settings: VoiceSettings) {
        configured += settings
    }
    var stopCalls = 0
    var isClosed = false

    /** Languages this "device" has no voice for: speaking in them fails, as on a real device. */
    val missingLanguages = mutableSetOf<String>()

    /** Each text with the language it was asked in (null: the app's). */
    val spokenIn = mutableListOf<Pair<String, String?>>()

    override fun speak(text: String, language: String?): Boolean {
        if (state.value != SpeechState.READY && state.value != SpeechState.SPEAKING) return false
        if (language != null && language in missingLanguages) return false
        spoken += text
        spokenIn += text to language
        spokenWith += text to configured.lastOrNull()
        return true
    }

    override fun stop() {
        stopCalls++
    }

    override fun close() {
        isClosed = true
    }
}

class FakePhotoStore : PhotoStore {
    var importResult: PhotoImportResult = PhotoImportResult.Imported(StoredPhoto("new-photo.jpg"))
    val imported = mutableListOf<String>()
    val deleted = mutableListOf<StoredPhoto>()

    /** Photos present on this "device"; others have no path, as on another device. */
    val files = mutableSetOf<String>()

    override suspend fun importPhoto(sourceUri: String): PhotoImportResult {
        imported += sourceUri
        (importResult as? PhotoImportResult.Imported)?.let { files += it.photo.fileName }
        return importResult
    }

    override fun pathOf(photo: StoredPhoto): String? =
        "/photos/${photo.fileName}".takeIf { photo.fileName in files }

    override suspend fun delete(photo: StoredPhoto) {
        deleted += photo
        files -= photo.fileName
    }

    var uploadResult: DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)
    val uploaded = mutableListOf<Pair<String, StoredPhoto>>()
    val deletedFromAccount = mutableListOf<StoredPhoto>()

    /** Photos the account has and this device still does not. */
    val inAccount = mutableSetOf<String>()

    override suspend fun readBytes(photo: StoredPhoto): ByteArray? =
        photo.fileName.toByteArray().takeIf { photo.fileName in files }

    override suspend fun saveBytes(photo: StoredPhoto, bytes: ByteArray): Boolean {
        files += photo.fileName
        return true
    }

    override suspend fun upload(ownerUid: String, photo: StoredPhoto): DataResult<WriteStatus> {
        uploaded += ownerUid to photo
        return uploadResult
    }

    override suspend fun deleteEverywhere(ownerUid: String, photo: StoredPhoto) {
        delete(photo)
        deletedFromAccount += photo
    }

    override suspend fun fetchMissing(ownerUid: String, photos: List<StoredPhoto>): Boolean {
        val arrived = photos.filter { it.fileName in inAccount && it.fileName !in files }
        files += arrived.map { it.fileName }
        return arrived.isNotEmpty()
    }
}

class FakeCloudPhotoRepository : CloudPhotoRepository {
    val photos = MutableStateFlow<Map<Pair<String, String>, ByteArray>>(emptyMap())
    var uploadResult: DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)

    override fun observe(ownerUid: String, photoId: String) = photos.map {
        DataResult.Success(CloudPhoto(it[ownerUid to photoId], isFromCache = false))
    }

    override suspend fun get(ownerUid: String, photoId: String) = DataResult.Success(photos.value[ownerUid to photoId])

    override suspend fun upload(ownerUid: String, photoId: String, bytes: ByteArray): DataResult<WriteStatus> {
        if (uploadResult is DataResult.Success) photos.value += (ownerUid to photoId) to bytes
        return uploadResult
    }

    override suspend fun delete(ownerUid: String, photoId: String): DataResult<WriteStatus> {
        photos.value -= ownerUid to photoId
        return DataResult.Success(WriteStatus.SYNCED)
    }
}

class FakeAacPreferencesRepository(initial: AacPreferences = AacPreferences()) : AacPreferencesRepository {
    override val preferences = MutableStateFlow(initial)

    override suspend fun update(transform: (AacPreferences) -> AacPreferences) {
        preferences.value = transform(preferences.value)
    }
}
