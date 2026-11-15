package com.oriteam.ori.ui.app.screens.aac

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.AacMessage
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.AacUsageRepository
import com.oriteam.ori.domain.aac.MessagePart
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SavedMessageRepository
import com.oriteam.ori.domain.aac.SpeechSynthesizer
import com.oriteam.ori.domain.aac.SpeechVolume
import com.oriteam.ori.domain.aac.StarterVocabulary
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.positionsAfterMoving
import com.oriteam.ori.domain.aac.toMessagePart
import com.oriteam.ori.domain.aac.typedMessagePart
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.common.appLanguageTag
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.AacPreferencesRepository
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.ui.app.screens.common.mapContent
import com.oriteam.ori.ui.app.screens.common.messageRes
import com.oriteam.ori.ui.components.state.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val SELECTION_KEY = "aac_selection"
private const val LAST_BOARD_KEY = "aac_last_board"

/** A ready-made board offered to people who prefer not to start from an empty one. */
data class StarterBoardContent(val name: String, val symbols: List<SymbolDraft>)

/**
 * The communication screen of the primary user: choose a board, tap symbols, build and speak a
 * message, and keep messages to say again.
 */
// One public function per user event of a single screen; splitting them would split one state.
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class AacCommunicateViewModel @Inject constructor(
    private val boardRepository: AacBoardRepository,
    private val savedMessageRepository: SavedMessageRepository,
    private val preferencesRepository: AacPreferencesRepository,
    private val speech: SpeechSynthesizer,
    private val speechVolume: SpeechVolume,
    private val photoStore: PhotoStore,
    private val usage: AacUsageRepository,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle,
    starterVocabulary: StarterVocabulary
) : ViewModel() {

    private data class Request(val ownerUid: String, val attempt: Int = 0)

    private val request = MutableStateFlow<Request?>(null)

    /** Read again each time the screen opens: the person may have changed the app's language meanwhile. */
    private val appLanguage = MutableStateFlow(appLanguageTag)
    private val selectionKey = MutableStateFlow<String?>(
        savedStateHandle.get<String>(SELECTION_KEY) ?: AacSelection.SavedMessages.key
    )
    private val lastBoardKey = MutableStateFlow(savedStateHandle.get<String>(LAST_BOARD_KEY))
    private val draft = MessageDraft(savedStateHandle)
    private val message = draft.message
    private val interaction = MutableStateFlow(AacCommunicateUiState())

    /** After each text said: with the volume of the voice at zero, the screen warns that nobody heard it. */
    private val voice = SpeechWhenReady(speech, viewModelScope) {
        val isOff = speechVolume.isMuted(preferences.value.voice.sound)
        interaction.update { it.copy(isVolumeOff = isOff) }
    }

    /** The last message said with "Falar" or a phrase; the voice of a single symbol is only feedback. */
    private var lastSaid: String? = null

    private val preferences = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, AacPreferences())

    init {
        // The boards speak with the voice, tone and speed chosen in "Voz".
        viewModelScope.launch {
            preferences.map { it.voice }.distinctUntilChanged().collect { speech.configure(it) }
        }
        // A message the engine accepted and then could not say (a voice that needs internet, offline) is shown
        // in large letters, as with no voice at all: the person tapped and must not be left unheard.
        viewModelScope.launch {
            speech.failures.collect { text -> if (text == lastSaid) showLarge(text) }
        }
    }

    private val content = AacBoardContent(
        owner = request.filterNotNull().map { it.ownerUid },
        selectionKey = selectionKey,
        lastBoardKey = lastBoardKey,
        boardRepository = boardRepository,
        savedMessageRepository = savedMessageRepository,
        photoStore = photoStore,
        vocabulary = starterVocabulary,
        appLanguage = appLanguage,
        scope = viewModelScope
    )

    private val latestSymbols: Map<String, AacSymbol> get() = content.symbols.value.symbolsById

    /**
     * The language of the chosen board, which the voice and the grammar follow: the app's for the suggested board
     * ([StarterVocabulary]), the board's own otherwise ([AacBoard.language]).
     */
    private val boardLanguage = content.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** The language of what is being built: the chosen board's, or the app's while there is none (words typed). */
    private val messageLanguage: String get() = boardLanguage.value ?: appLanguage.value

    private fun currentMessage() = message.value.inLanguage(messageLanguage)

    private val local = combine(
        message,
        interaction,
        preferences,
        speech.state,
        boardLanguage
    ) { built, state, prefs, speechState, language ->
        val msg = built.inLanguage(language ?: appLanguage.value)
        state.copy(
            message = msg.parts.zip(msg.sentence).mapIndexed { index, (part, piece) ->
                part.toUiModel(index, photoStore).copy(label = piece.text, connector = piece.connector)
            },
            canSaveMessage = msg.canBeSaved,
            isMessageFull = msg.isFull,
            preferences = prefs,
            speechState = speechState,
            boardLanguage = language
        )
    }

    private val chosen = combine(content.boards, content.selection, content.buildBoard) { b, s, build ->
        Triple(b.mapContent { list -> list.map { BoardTabUiModel(it.id, it.name) } }, s, build)
    }

    /** Listeners run only while the screen collects this state. */
    val uiState: StateFlow<AacCommunicateUiState> = combine(
        chosen,
        content.symbols,
        content.saved,
        local
    ) { (boards, selection, build), lower, saved, state ->
        state.copy(
            boards = boards,
            selection = selection,
            buildBoard = build,
            symbols = lower.symbols,
            symbolsKey = lower.symbolsKey,
            savedMessages = saved
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), firstState())

    /**
     * What the first frame shows, before anything is read: already the tab and the board asked for. Without it the
     * screen opened on an empty "Montar" for one frame, and "Prontas" was made only in the next one (M21).
     */
    private fun firstState(): AacCommunicateUiState {
        val selection = AacSelection.parse(selectionKey.value)
        val lastBoard = AacSelection.parse(lastBoardKey.value)
        return AacCommunicateUiState(
            selection = selection,
            buildBoard = (selection as? AacSelection.Board) ?: (lastBoard as? AacSelection.Board)
        )
    }

    fun show(ownerUid: String) {
        appLanguage.value = appLanguageTag
        if (request.value?.ownerUid == ownerUid) return
        request.value = Request(ownerUid)
    }

    fun onRetry() = request.update { it?.copy(attempt = it.attempt + 1) }

    fun onSelect(selection: AacSelection) {
        if (selection is AacSelection.Board) {
            savedStateHandle[LAST_BOARD_KEY] = selection.key
            lastBoardKey.value = selection.key
        }
        select(selection.key)
    }

    /** Back to building: the last board used, or the first one if it no longer exists. */
    fun onShowBoards() = select(savedStateHandle.get<String>(LAST_BOARD_KEY))

    private fun select(key: String?) {
        savedStateHandle[SELECTION_KEY] = key
        selectionKey.value = key
    }

    /** A tile held and dragged in Montar (or moved by a screen reader action) to [toIndex]. */
    fun onMoveSymbol(symbolId: String, toIndex: Int) {
        val owner = request.value?.ownerUid ?: return
        val lower = content.symbols.value
        val changes = lower.positionsAfterMoving(symbolId, toIndex) ?: return
        runWrite(successRes = null) { boardRepository.reorderSymbols(owner, lower.symbolsKey.orEmpty(), changes) }
    }

    /** What a tap does depends on the person's message mode, not on a fixed model. */

    fun onSymbolTap(symbolId: String) {
        val part = latestSymbols[symbolId]?.toMessagePart() ?: return
        when (preferences.value.messageMode) {
            MessageMode.COMPOSE -> addPart(part)
            MessageMode.COMPOSE_WITH_FEEDBACK -> {
                // Says what this tap adds: a phrase only counts while the symbol stands alone.
                voice.speak(if (message.value.isEmpty) part.spokenText else part.label, messageLanguage)
                addPart(part)
            }
            MessageMode.SPEAK_IMMEDIATELY -> say(part.spokenText, messageLanguage)
        }
    }

    fun onTypedText(text: String) {
        typedMessagePart(text)?.let(::addPart)
    }

    fun onRemovePart(index: Int) = updateMessage { it.removeAt(index) }

    fun onRemoveLast() = updateMessage { it.removeLast() }

    fun onClear() = updateMessage { AacMessage() }

    fun onSpeak() = say(currentMessage().text, messageLanguage)

    fun onStopSpeaking() {
        voice.cancel()
        speech.stop()
    }

    /** Showing in large letters is a way of saying the message, so it counts like "Falar". */
    fun onShowMessage() {
        val text = currentMessage().text.ifEmpty { return }
        interaction.update { it.copy(displayedText = text) }
        recordCommunication()
    }

    /** [language] is the phrase's: a saved one keeps the language it was built in; a ready one is the app's (null). */
    fun onSayPhrase(text: String, language: String?) = say(text, language)

    fun onDismissDisplay() = interaction.update { it.copy(displayedText = null) }

    /** "Aumentar o volume", from the warning of a voice nobody heard: up to a volume that is heard. */
    fun onRaiseVolume() {
        speechVolume.raise(preferences.value.voice.sound)
        interaction.update { it.copy(isVolumeOff = false) }
    }

    fun onDismissVolumeWarning() = interaction.update { it.copy(isVolumeOff = false) }

    fun onSaveMessage() {
        val owner = request.value?.ownerUid ?: return
        val current = currentMessage()
        if (!current.canBeSaved) return
        val language = messageLanguage
        runWrite(successRes = R.string.aac_message_saved) {
            savedMessageRepository.saveMessage(owner, current.text, language)
        }
    }

    fun onDeleteSavedRequest(saved: SavedMessage) = interaction.update { it.copy(pendingDeletion = saved) }

    fun onDeleteSavedDismiss() = interaction.update { it.copy(pendingDeletion = null) }

    fun onDeleteSavedConfirm() {
        val owner = request.value?.ownerUid ?: return
        val saved = interaction.value.pendingDeletion ?: return
        interaction.update { it.copy(pendingDeletion = null) }
        runWrite(successRes = null) { savedMessageRepository.deleteSavedMessage(owner, saved.id) }
    }

    fun onChangeEmojiRequest(saved: SavedMessage) = interaction.update { it.copy(pendingEmojiChange = saved) }

    fun onChangeEmojiDismiss() = interaction.update { it.copy(pendingEmojiChange = null) }

    /** Null goes back to the app's picture for saved phrases. */
    fun onChangeEmojiConfirm(emoji: String?) {
        val owner = request.value?.ownerUid ?: return
        val saved = interaction.value.pendingEmojiChange ?: return
        interaction.update { it.copy(pendingEmojiChange = null) }
        if (emoji == saved.emoji) return
        runWrite(successRes = null) { savedMessageRepository.setSavedMessageEmoji(owner, saved.id, emoji) }
    }

    /** A saved phrase held and dragged in Frases (or moved by a screen reader action) to [toIndex]. */
    fun onMoveSaved(messageId: String, toIndex: Int) {
        val owner = request.value?.ownerUid ?: return
        val saved = (content.saved.value as? ContentState.Content)?.value ?: return
        val changes = saved.positionsAfterMoving(messageId, toIndex) ?: return
        runWrite(successRes = null) { savedMessageRepository.reorderSavedMessages(owner, changes) }
    }

    /**
     * A phrase that comes with the app held and dragged in Frases (or moved by a screen reader action) to [toIndex].
     * Its order is kept on this device, like the other choices of the screen.
     */
    fun onMoveReady(phraseId: String, toIndex: Int) {
        viewModelScope.launch {
            preferencesRepository.update { prefs ->
                val order = readyOrderAfterMoving(prefs.readyPhraseOrder, phraseId, toIndex)
                if (order == null) prefs else prefs.copy(readyPhraseOrder = order)
            }
        }
    }

    /** A second tap while the first is being saved does nothing, so only one board is created. */
    fun onCreateStarterBoard(content: StarterBoardContent) {
        val owner = request.value?.ownerUid ?: return
        if (interaction.value.isCreatingStarterBoard) return
        interaction.update { it.copy(isCreatingStarterBoard = true) }
        runWrite(successRes = null) {
            // The suggested board is written in the app's language, and is in it from now on.
            boardRepository.createBoardWithSymbols(
                owner,
                content.name,
                position = 0,
                symbols = content.symbols,
                language = appLanguageTag
            )
                .also { interaction.update { it.copy(isCreatingStarterBoard = false) } }
        }
    }

    /**
     * Without a voice the message is shown in large letters, so the person can still be understood. A voice
     * still starting is waited for: that is not "no voice".
     */
    private fun say(text: String, language: String?) {
        if (text.isBlank()) return
        lastSaid = text
        voice.speak(text, language, onSilent = { showLarge(text) })
        recordCommunication()
    }

    private fun showLarge(text: String) = interaction.update { it.copy(displayedText = text) }

    private fun recordCommunication() {
        viewModelScope.launch { usage.recordCommunication(LocalDate.now(clock)) }
    }

    private fun addPart(part: MessagePart) = updateMessage { it.add(part) }

    /** Old feedback ("Frase salva", an error) is cleared as soon as the person continues. */
    private fun updateMessage(transform: (AacMessage) -> AacMessage) {
        interaction.update { it.copy(actionErrorRes = null, noticeRes = null) }
        draft.update(transform)
    }

    private fun runWrite(@StringRes successRes: Int?, write: suspend () -> DataResult<WriteStatus>) {
        interaction.update { it.copy(actionErrorRes = null, noticeRes = null) }
        viewModelScope.launch {
            when (val result = write()) {
                is DataResult.Failure -> interaction.update { it.copy(actionErrorRes = result.error.messageRes()) }
                is DataResult.Success -> {
                    val isPending = result.value == WriteStatus.PENDING_SYNC
                    interaction.update {
                        it.copy(
                            noticeRes = if (isPending) R.string.sync_pending_changes else successRes
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        speech.close()
    }
}
