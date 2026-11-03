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
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.toMessagePart
import com.oriteam.ori.domain.aac.typedMessagePart
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.photos.PhotoStore
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.AacPreferencesRepository
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.ui.app.screens.common.mapContent
import com.oriteam.ori.ui.app.screens.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    preferencesRepository: AacPreferencesRepository,
    private val speech: SpeechSynthesizer,
    private val photoStore: PhotoStore,
    private val usage: AacUsageRepository,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private data class Request(val ownerUid: String, val attempt: Int = 0)

    private val request = MutableStateFlow<Request?>(null)
    private val selectionKey = MutableStateFlow<String?>(
        savedStateHandle.get<String>(SELECTION_KEY) ?: AacSelection.SavedMessages.key
    )
    private val draft = MessageDraft(savedStateHandle)
    private val message = draft.message
    private val interaction = MutableStateFlow(AacCommunicateUiState())

    private val preferences = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, AacPreferences())

    private val content = AacBoardContent(
        owner = request.filterNotNull().map { it.ownerUid },
        selectionKey = selectionKey,
        boardRepository = boardRepository,
        savedMessageRepository = savedMessageRepository,
        photoStore = photoStore,
        scope = viewModelScope
    )

    private val latestSymbols: Map<String, AacSymbol> get() = content.lower.value.symbolsById

    private val local = combine(message, interaction, preferences, speech.state) { msg, state, prefs, speechState ->
        state.copy(
            message = msg.parts.zip(msg.sentence).mapIndexed { index, (part, piece) ->
                part.toUiModel(index, photoStore).copy(label = piece.text, connector = piece.connector)
            },
            canSaveMessage = msg.canBeSaved,
            isMessageFull = msg.isFull,
            preferences = prefs,
            speechState = speechState
        )
    }

    /** Listeners run only while the screen collects this state. */
    val uiState: StateFlow<AacCommunicateUiState> = combine(
        content.boards,
        content.selection,
        content.lower,
        local
    ) { b, s, l, state ->
        state.copy(
            boards = b.mapContent { list -> list.map { BoardTabUiModel(it.id, it.name) } },
            selection = s,
            symbols = l.symbols,
            savedMessages = l.saved
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AacCommunicateUiState())

    fun show(ownerUid: String) {
        if (request.value?.ownerUid == ownerUid) return
        request.value = Request(ownerUid)
    }

    fun onRetry() = request.update { it?.copy(attempt = it.attempt + 1) }

    fun onSelect(selection: AacSelection) {
        if (selection is AacSelection.Board) savedStateHandle[LAST_BOARD_KEY] = selection.key
        select(selection.key)
    }

    /** Back to building: the last board used, or the first one if it no longer exists. */
    fun onShowBoards() = select(savedStateHandle.get<String>(LAST_BOARD_KEY))

    private fun select(key: String?) {
        savedStateHandle[SELECTION_KEY] = key
        selectionKey.value = key
    }

    /** What a tap does depends on the person's message mode, not on a fixed model. */
    fun onSymbolTap(symbolId: String) {
        val part = latestSymbols[symbolId]?.toMessagePart() ?: return
        when (preferences.value.messageMode) {
            MessageMode.COMPOSE -> addPart(part)
            MessageMode.COMPOSE_WITH_FEEDBACK -> {
                // Says what this tap adds: a phrase only counts while the symbol stands alone.
                speech.speak(if (message.value.isEmpty) part.spokenText else part.label)
                addPart(part)
            }
            MessageMode.SPEAK_IMMEDIATELY -> say(part.spokenText)
        }
    }

    fun onTypedText(text: String) {
        typedMessagePart(text)?.let(::addPart)
    }

    fun onRemovePart(index: Int) = updateMessage { it.removeAt(index) }

    fun onRemoveLast() = updateMessage { it.removeLast() }

    fun onClear() = updateMessage { AacMessage() }

    fun onSpeak() = say(message.value.text)

    fun onStopSpeaking() = speech.stop()

    /** Showing in large letters is a way of saying the message, so it counts like "Falar". */
    fun onShowMessage() {
        val text = message.value.text.ifEmpty { return }
        interaction.update { it.copy(displayedText = text) }
        recordCommunication()
    }

    fun onSayPhrase(text: String) = say(text)

    fun onDismissDisplay() = interaction.update { it.copy(displayedText = null) }

    fun onSaveMessage() {
        val owner = request.value?.ownerUid ?: return
        val current = message.value
        if (!current.canBeSaved) return
        runWrite(successRes = R.string.aac_message_saved) { savedMessageRepository.saveMessage(owner, current.text) }
    }

    fun onDeleteSavedRequest(saved: SavedMessage) = interaction.update { it.copy(pendingDeletion = saved) }

    fun onDeleteSavedDismiss() = interaction.update { it.copy(pendingDeletion = null) }

    fun onDeleteSavedConfirm() {
        val owner = request.value?.ownerUid ?: return
        val saved = interaction.value.pendingDeletion ?: return
        interaction.update { it.copy(pendingDeletion = null) }
        runWrite(successRes = null) { savedMessageRepository.deleteSavedMessage(owner, saved.id) }
    }

    /** A second tap while the first is being saved does nothing, so only one board is created. */
    fun onCreateStarterBoard(content: StarterBoardContent) {
        val owner = request.value?.ownerUid ?: return
        if (interaction.value.isCreatingStarterBoard) return
        interaction.update { it.copy(isCreatingStarterBoard = true) }
        runWrite(successRes = null) {
            boardRepository.createBoardWithSymbols(owner, content.name, position = 0, symbols = content.symbols)
                .also { interaction.update { it.copy(isCreatingStarterBoard = false) } }
        }
    }

    /** Without a voice the message is shown in large letters, so the person can still be understood. */
    private fun say(text: String) {
        if (text.isBlank()) return
        if (!speech.speak(text)) interaction.update { it.copy(displayedText = text) }
        recordCommunication()
    }

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
