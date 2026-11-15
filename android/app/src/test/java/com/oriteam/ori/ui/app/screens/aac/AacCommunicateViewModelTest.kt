package com.oriteam.ori.ui.app.screens.aac

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.domain.preferences.VoiceSettings
import com.oriteam.ori.domain.preferences.VoiceSound
import com.oriteam.ori.testing.AacWrite
import com.oriteam.ori.testing.FakeAacBoardRepository
import com.oriteam.ori.testing.FakeAacPreferencesRepository
import com.oriteam.ori.testing.FakeAacUsageRepository
import com.oriteam.ori.testing.FakePhotoStore
import com.oriteam.ori.testing.FakeSavedMessageRepository
import com.oriteam.ori.testing.FakeSpeechSynthesizer
import com.oriteam.ori.testing.FakeSpeechVolume
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.OWNER
import com.oriteam.ori.testing.board
import com.oriteam.ori.testing.listSnapshotOf
import com.oriteam.ori.testing.starterVocabulary
import com.oriteam.ori.testing.symbol
import com.oriteam.ori.testing.withDefaultLocale
import com.oriteam.ori.ui.components.state.ContentState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.util.Locale

/** Runs on Robolectric because the message draft is saved as Android Bundles. */
@RunWith(AndroidJUnit4::class)
class AacCommunicateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val boards = FakeAacBoardRepository().apply {
        boards.value = listSnapshotOf(board("b2", "Comida", position = 1), board("b1", "Básico", position = 0))
        symbols.value = listSnapshotOf(
            symbol("s2", "Comer", position = 1),
            symbol("s1", "Água", phrase = "Eu quero água", position = 0)
        )
    }
    private val savedMessages = FakeSavedMessageRepository()
    private val preferences = FakeAacPreferencesRepository()
    private val speech = FakeSpeechSynthesizer()
    private val volume = FakeSpeechVolume()

    private fun TestScope.communicateViewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        AacCommunicateViewModel(
            boards,
            savedMessages,
            preferences,
            speech,
            volume,
            FakePhotoStore(),
            FakeAacUsageRepository(),
            Clock.systemDefaultZone(),
            savedState,
            starterVocabulary
        ).also {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() }
            it.show(OWNER)
            // The screen opens on the ready phrases; these tests are about building a sentence.
            if (!savedState.contains("aac_selection")) it.onShowBoards()
        }

    private fun AacCommunicateViewModel.labels() =
        (uiState.value.symbols as ContentState.Content).value.map { it.label }

    @Test
    fun `the screen opens on the ready phrases, even with boards`() = runTest {
        val viewModel = AacCommunicateViewModel(
            boards,
            savedMessages,
            preferences,
            speech,
            volume,
            FakePhotoStore(),
            FakeAacUsageRepository(),
            Clock.systemDefaultZone(),
            SavedStateHandle(),
            starterVocabulary
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.show(OWNER)

        assertEquals(AacSelection.SavedMessages, viewModel.uiState.value.selection)
    }

    @Test
    fun `the boards speak with the voice chosen in Voz, and follow a change at once`() = runTest {
        val chosen = VoiceSettings(voiceId = "pt-br-x-ptd-local", pitch = 0.75f, rate = 0.95f)
        preferences.preferences.value = AacPreferences(voice = chosen)
        val viewModel = communicateViewModel()
        assertEquals(chosen, speech.configured.last())

        preferences.preferences.value = AacPreferences(voice = chosen.copy(pitch = 1.6f))
        viewModel.onSayPhrase("Oi", null)

        assertEquals("Oi" to chosen.copy(pitch = 1.6f), speech.spokenWith.last())
    }

    @Test
    fun `the words of a built sentence show what will be said`() = runTest {
        boards.symbols.value = listSnapshotOf(
            symbol("s1", "eu", position = 0),
            symbol("s2", "gosto", position = 1),
            symbol("s3", "você", position = 2)
        )
        val viewModel = communicateViewModel()

        listOf("s1", "s2", "s3").forEach(viewModel::onSymbolTap)
        viewModel.onSpeak()

        assertEquals(listOf("Eu gosto de você"), speech.spoken)
        assertEquals("de", viewModel.uiState.value.message.last().connector)
    }

    @Test
    fun `opens the first board with its symbols in order`() = runTest {
        val viewModel = communicateViewModel()

        assertEquals(AacSelection.Board("b1"), viewModel.uiState.value.selection)
        assertEquals(listOf("b1"), boards.observedBoards)
        assertEquals(listOf("Água", "Comer"), viewModel.labels())
    }

    @Test
    fun `in compose mode taps build a message spoken word by word`() = runTest {
        val viewModel = communicateViewModel()

        viewModel.onSymbolTap("s1")
        viewModel.onTypedText("gelada")
        viewModel.onSpeak()

        assertEquals(listOf("Água", "gelada"), viewModel.uiState.value.message.map { it.label })
        assertEquals(listOf("Água gelada"), speech.spoken)
    }

    @Test
    fun `with feedback each symbol is also spoken when added`() = runTest {
        preferences.preferences.value = AacPreferences(messageMode = MessageMode.COMPOSE_WITH_FEEDBACK)
        val viewModel = communicateViewModel()

        viewModel.onSymbolTap("s1")

        assertEquals(1, viewModel.uiState.value.message.size)
        assertEquals(listOf("Eu quero água"), speech.spoken)

        // The second piece says only its word, the same that it adds to the sentence.
        viewModel.onSymbolTap("s1")
        assertEquals(listOf("Eu quero água", "Água"), speech.spoken)
    }

    @Test
    fun `a symbol alone says its phrase`() = runTest {
        val viewModel = communicateViewModel()

        viewModel.onSymbolTap("s1")
        viewModel.onSpeak()

        assertEquals(listOf("Eu quero água"), speech.spoken)
    }

    @Test
    fun `leaving the phrases returns to the last board used`() = runTest {
        val viewModel = communicateViewModel()
        viewModel.onSelect(AacSelection.Board("b2"))
        viewModel.onSelect(AacSelection.SavedMessages)

        viewModel.onShowBoards()

        assertEquals(AacSelection.Board("b2"), viewModel.uiState.value.selection)
    }

    @Test
    fun `the symbols of Montar stay while the phrases are open, and coming back reads nothing again`() = runTest {
        val viewModel = communicateViewModel()

        viewModel.onSelect(AacSelection.SavedMessages)

        assertEquals(AacSelection.Board("b1"), viewModel.uiState.value.buildBoard)
        assertEquals(listOf("Água", "Comer"), viewModel.labels())
        viewModel.onShowBoards()
        assertEquals(listOf("Água", "Comer"), viewModel.labels())
        assertEquals(listOf("b1"), boards.observedBoards)
    }

    @Test
    fun `the saved phrases read last stay while Montar is open, without being read meanwhile`() = runTest {
        savedMessages.messages.value = DataResult.Success(listOf(SavedMessage("m1", "Bom dia")))
        val viewModel = communicateViewModel()
        viewModel.onSelect(AacSelection.SavedMessages)
        val whileShown = savedMessages.subscriptions

        viewModel.onShowBoards()

        assertEquals(AacSelection.Board("b1"), viewModel.uiState.value.selection)
        val saved = viewModel.uiState.value.savedMessages as ContentState.Content
        assertEquals(listOf("Bom dia"), saved.value.map { it.text })
        assertEquals(whileShown, savedMessages.subscriptions)
    }

    @Test
    fun `a ready phrase is spoken and does not touch the message being built`() = runTest {
        val viewModel = communicateViewModel()
        viewModel.onSymbolTap("s2")

        viewModel.onSayPhrase("Preciso de ajuda", null)

        assertEquals(listOf("Preciso de ajuda"), speech.spoken)
        assertEquals(listOf("Comer"), viewModel.uiState.value.message.map { it.label })
    }

    @Test
    fun `in immediate mode a tap speaks at once and builds nothing`() = runTest {
        preferences.preferences.value = AacPreferences(messageMode = MessageMode.SPEAK_IMMEDIATELY)
        val viewModel = communicateViewModel()

        viewModel.onSymbolTap("s1")

        assertTrue(viewModel.uiState.value.message.isEmpty())
        assertEquals(listOf("Eu quero água"), speech.spoken)
    }

    @Test
    fun `without a voice the message is shown in large letters instead`() = runTest {
        speech.state.value = SpeechState.UNAVAILABLE
        val viewModel = communicateViewModel()

        viewModel.onSymbolTap("s1")
        viewModel.onSpeak()

        assertTrue(speech.spoken.isEmpty())
        assertEquals("Eu quero água", viewModel.uiState.value.displayedText)
        assertEquals(SpeechState.UNAVAILABLE, viewModel.uiState.value.speechState)
        viewModel.onDismissDisplay()
        assertNull(viewModel.uiState.value.displayedText)
    }

    @Test
    fun `a board in another language is said and built in it, and saved with it`() = runTest {
        boards.boards.value = listSnapshotOf(board("es", "Comida", language = "es"))
        boards.symbols.value = listSnapshotOf(
            symbol("s1", "yo", boardId = "es", position = 0),
            symbol("s2", "me gusta", boardId = "es", position = 1),
            symbol("s3", "agua", boardId = "es", position = 2)
        )
        val viewModel = communicateViewModel()
        viewModel.onSelect(AacSelection.Board("es"))
        listOf("s1", "s2", "s3").forEach(viewModel::onSymbolTap)

        viewModel.onSpeak()
        viewModel.onSaveMessage()

        // The Spanish grammar, though the app is in Portuguese: "a mí" and "el" on the screen and in the voice.
        assertEquals(listOf("mí", "me gusta", "agua"), viewModel.uiState.value.message.map { it.label })
        assertEquals("A mí me gusta el agua" to "es", speech.spokenIn.last())
        assertEquals(listOf("es"), savedMessages.savedLanguages)
    }

    @Test
    fun `a board from before boards had a language is Portuguese, even with the app in English`() = runTest {
        boards.boards.value = listSnapshotOf(board("old", "Básico"))
        boards.symbols.value = listSnapshotOf(
            symbol("s1", "estou", boardId = "old", position = 0),
            symbol("s2", "dor", boardId = "old", position = 1)
        )
        withDefaultLocale(Locale.US) {
            val viewModel = communicateViewModel()
            viewModel.onSelect(AacSelection.Board("old"))
            viewModel.onSymbolTap("s1")
            viewModel.onSymbolTap("s2")

            viewModel.onSpeak()
            viewModel.onSaveMessage()
        }

        // Before 08/11/2026 the app was only in Portuguese: no English voice reading "Estou dor".
        assertEquals("Estou com dor" to "pt-BR", speech.spokenIn.last())
        assertEquals(listOf("pt-BR"), savedMessages.savedLanguages)
    }

    @Test
    fun `the suggested board follows the app's language, in its words, its grammar and its voice`() = runTest {
        boards.boards.value = listSnapshotOf(board("b", "Básico", language = "pt-BR"))
        boards.symbols.value = listSnapshotOf(
            symbol("s1", "eu", image = SymbolImage.Emoji("🙋"), boardId = "b", position = 0),
            symbol("s2", "estou", image = SymbolImage.Emoji("🌙"), boardId = "b", position = 1),
            symbol("s3", "dor", image = SymbolImage.Emoji("🤕"), boardId = "b", position = 2),
            // Added by the person: theirs, as written.
            symbol("s4", "Bolo", image = SymbolImage.Emoji("🍰"), boardId = "b", position = 3)
        )
        val english = withDefaultLocale(Locale.US) {
            val viewModel = communicateViewModel()
            viewModel.onSelect(AacSelection.Board("b"))
            viewModel.onSymbolTap("s1")
            viewModel.onSymbolTap("s3")
            viewModel.onSpeak()
            viewModel
        }

        assertEquals(listOf("Basics"), (english.uiState.value.boards as ContentState.Content).value.map { it.name })
        assertEquals(listOf("I", "am", "pain", "Bolo"), english.labels())
        // "I" + "pain", without the verb, is said as English says it.
        assertEquals("I am in pain" to "en-US", speech.spokenIn.last())

        val spanish = withDefaultLocale(Locale.forLanguageTag("es-ES")) {
            val viewModel = communicateViewModel()
            viewModel.onSelect(AacSelection.Board("b"))
            viewModel.onSymbolTap("s1")
            viewModel.onSymbolTap("s3")
            viewModel.onSpeak()
            viewModel
        }
        assertEquals(listOf("yo", "tengo", "dolor", "Bolo"), spanish.labels())
        assertEquals("Yo tengo dolor" to "es-ES", speech.spokenIn.last())
    }

    @Test
    fun `without a voice for the board's language the message is shown, not read with another voice`() = runTest {
        speech.missingLanguages += "es"
        boards.boards.value = listSnapshotOf(board("es", "Comida", language = "es"))
        boards.symbols.value = listSnapshotOf(symbol("s1", "agua", boardId = "es"))
        val viewModel = communicateViewModel()
        viewModel.onSelect(AacSelection.Board("es"))
        viewModel.onSymbolTap("s1")

        viewModel.onSpeak()

        assertTrue(speech.spoken.isEmpty())
        assertEquals("agua", viewModel.uiState.value.displayedText)
    }

    @Test
    fun `a saved phrase is said in its own language, a ready one in the app's`() = runTest {
        val viewModel = communicateViewModel()

        viewModel.onSayPhrase("Hola", "es")
        viewModel.onSayPhrase("Oi", null)

        assertEquals(listOf("Hola" to "es", "Oi" to null), speech.spokenIn)
    }

    @Test
    fun `the suggested board is created in the app's language`() = runTest {
        val viewModel = communicateViewModel()

        viewModel.onCreateStarterBoard(StarterBoardContent("Básico", emptyList()))

        assertEquals(listOf("pt-BR"), boards.boardLanguages)
    }

    @Test
    fun `a phrase tapped while the voice is starting is said when it starts, not shown in large letters`() = runTest {
        speech.state.value = SpeechState.PREPARING
        val viewModel = communicateViewModel()

        viewModel.onSayPhrase("Preciso de ajuda", null)
        assertNull(viewModel.uiState.value.displayedText)

        speech.state.value = SpeechState.READY
        runCurrent()

        assertEquals(listOf("Preciso de ajuda"), speech.spoken)
        assertNull(viewModel.uiState.value.displayedText)
    }

    @Test
    fun `a message the voice accepts and then cannot say is shown in large letters, a single word is not`() = runTest {
        preferences.preferences.value = AacPreferences(messageMode = MessageMode.COMPOSE_WITH_FEEDBACK)
        val viewModel = communicateViewModel()

        // The word of one symbol is feedback: its failure shows nothing.
        viewModel.onSymbolTap("s2")
        speech.failures.emit("Comer")
        assertNull(viewModel.uiState.value.displayedText)

        // A voice that needs internet, offline: the engine accepted "Falar" and fails a moment later.
        viewModel.onSpeak()
        speech.failures.emit("Comer")

        assertEquals("Comer", viewModel.uiState.value.displayedText)
    }

    @Test
    fun `a voice that does not start in time, or cannot start, gives way to the large letters`() = runTest {
        speech.state.value = SpeechState.PREPARING
        val viewModel = communicateViewModel()

        viewModel.onSayPhrase("Preciso de ajuda", null)
        advanceTimeBy(VOICE_START_TIMEOUT_MILLIS + 1)
        assertEquals("Preciso de ajuda", viewModel.uiState.value.displayedText)

        viewModel.onDismissDisplay()
        viewModel.onSayPhrase("Quero água", null)
        speech.state.value = SpeechState.UNAVAILABLE
        runCurrent()

        assertEquals("Quero água", viewModel.uiState.value.displayedText)
        assertTrue(speech.spoken.isEmpty())
    }

    @Test
    fun `a phrase said with the volume at zero warns that nobody heard it, and turning it up clears it`() = runTest {
        volume.muted = true
        val viewModel = communicateViewModel()

        viewModel.onSayPhrase("Quero água", null)
        assertEquals(listOf("Quero água"), speech.spoken)
        assertTrue(viewModel.uiState.value.isVolumeOff)

        viewModel.onRaiseVolume()
        assertEquals(listOf(VoiceSound.MEDIA), volume.raised)
        assertFalse(viewModel.uiState.value.isVolumeOff)
    }

    @Test
    fun `with the voice on the ringtone, only that volume decides the warning and is turned up`() = runTest {
        preferences.preferences.value = AacPreferences(voice = VoiceSettings(sound = VoiceSound.RING))
        val viewModel = communicateViewModel()
        assertEquals(VoiceSound.RING, speech.configured.last().sound)

        volume.mutedSounds += VoiceSound.MEDIA
        viewModel.onSayPhrase("Oi", null)
        assertFalse(viewModel.uiState.value.isVolumeOff)

        volume.mutedSounds += VoiceSound.RING
        viewModel.onSayPhrase("Oi", null)
        assertTrue(viewModel.uiState.value.isVolumeOff)
        viewModel.onRaiseVolume()
        assertEquals(listOf(VoiceSound.RING), volume.raised)
    }

    @Test
    fun `the volume warning goes when closed or when a text is heard, and never shows without a voice`() = runTest {
        volume.muted = true
        val viewModel = communicateViewModel()
        viewModel.onSayPhrase("Oi", null)
        viewModel.onDismissVolumeWarning()
        assertFalse(viewModel.uiState.value.isVolumeOff)

        viewModel.onSayPhrase("Oi", null)
        volume.muted = false
        viewModel.onSayPhrase("Tchau", null)
        assertFalse(viewModel.uiState.value.isVolumeOff)

        // Without a voice the text shows in large letters: a warning about the volume would not be true.
        volume.muted = true
        speech.state.value = SpeechState.UNAVAILABLE
        viewModel.onSayPhrase("Socorro", null)
        assertEquals("Socorro", viewModel.uiState.value.displayedText)
        assertFalse(viewModel.uiState.value.isVolumeOff)
    }

    @Test
    fun `a phrase waiting for the voice to start is checked against the volume when it is said`() = runTest {
        volume.muted = true
        speech.state.value = SpeechState.PREPARING
        val viewModel = communicateViewModel()

        viewModel.onSayPhrase("Preciso de ajuda", null)
        assertFalse(viewModel.uiState.value.isVolumeOff)

        speech.state.value = SpeechState.READY
        runCurrent()
        assertTrue(viewModel.uiState.value.isVolumeOff)
    }

    @Test
    fun `the message and the chosen board survive an app restart`() = runTest {
        val savedState = SavedStateHandle()
        communicateViewModel(savedState).apply {
            onSelect(AacSelection.Board("b2"))
            onSymbolTap("s1")
            onTypedText("agora")
        }

        val restored = communicateViewModel(savedState)

        assertEquals(AacSelection.Board("b2"), restored.uiState.value.selection)
        assertEquals(listOf("Água", "agora"), restored.uiState.value.message.map { it.label })
        restored.onSpeak()
        assertEquals(listOf("Água agora"), speech.spoken)
    }

    @Test
    fun `pieces can be removed one by one or all at once`() = runTest {
        val viewModel = communicateViewModel()
        listOf("s1", "s2", "s1").forEach(viewModel::onSymbolTap)

        viewModel.onRemovePart(1)
        assertEquals(listOf("Água", "Água"), viewModel.uiState.value.message.map { it.label })
        viewModel.onRemoveLast()
        assertEquals(1, viewModel.uiState.value.message.size)
        viewModel.onClear()
        assertTrue(viewModel.uiState.value.message.isEmpty())
    }

    @Test
    fun `saving a message confirms it, or keeps the message and explains a failure`() = runTest {
        val viewModel = communicateViewModel()
        viewModel.onSymbolTap("s1")

        viewModel.onSaveMessage()
        assertEquals(listOf("Eu quero água"), savedMessages.saved)
        assertEquals(R.string.aac_message_saved, viewModel.uiState.value.noticeRes)

        savedMessages.writeResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        viewModel.onSaveMessage()
        assertEquals(R.string.error_network_unavailable, viewModel.uiState.value.actionErrorRes)
        assertEquals(1, viewModel.uiState.value.message.size)

        savedMessages.writeResult = DataResult.Success(WriteStatus.PENDING_SYNC)
        viewModel.onSaveMessage()
        assertEquals(R.string.sync_pending_changes, viewModel.uiState.value.noticeRes)
    }

    @Test
    fun `saved messages are read only while shown and deleted only after confirmation`() = runTest {
        savedMessages.messages.value = DataResult.Success(listOf(SavedMessage("m1", "Bom dia")))
        val viewModel = communicateViewModel()
        val beforeOpening = savedMessages.subscriptions

        viewModel.onSelect(AacSelection.SavedMessages)
        assertEquals(beforeOpening + 1, savedMessages.subscriptions)
        viewModel.onSayPhrase("Bom dia", null)
        assertEquals(listOf("Bom dia"), speech.spoken)

        viewModel.onDeleteSavedRequest(SavedMessage("m1", "Bom dia"))
        assertTrue(savedMessages.deleted.isEmpty())
        viewModel.onDeleteSavedConfirm()
        assertEquals(listOf("m1"), savedMessages.deleted)
    }

    @Test
    fun `the emoji of a saved message changes only on confirmation, and back to the default with null`() = runTest {
        val viewModel = communicateViewModel()
        val saved = SavedMessage("m1", "Bom dia", emoji = "☀️")

        viewModel.onChangeEmojiRequest(saved)
        assertEquals(saved, viewModel.uiState.value.pendingEmojiChange)
        viewModel.onChangeEmojiDismiss()
        assertTrue(savedMessages.emojiChanges.isEmpty())

        viewModel.onChangeEmojiRequest(saved)
        viewModel.onChangeEmojiConfirm("☀️")
        assertTrue("the same emoji writes nothing", savedMessages.emojiChanges.isEmpty())

        viewModel.onChangeEmojiRequest(saved)
        viewModel.onChangeEmojiConfirm("🌟")
        viewModel.onChangeEmojiRequest(saved)
        viewModel.onChangeEmojiConfirm(null)
        assertEquals(listOf("m1" to "🌟", "m1" to null), savedMessages.emojiChanges)
        assertEquals(null, viewModel.uiState.value.pendingEmojiChange)
    }

    @Test
    fun `a symbol dragged to another place saves only the positions that changed`() = runTest {
        boards.symbols.value = listSnapshotOf(
            symbol("s1", "Água", position = 0),
            symbol("s2", "Comer", position = 1),
            symbol("s3", "Dormir", position = 2)
        )
        val viewModel = communicateViewModel()
        viewModel.onSelect(AacSelection.Board("b1"))

        viewModel.onMoveSymbol("s3", 0)
        // Already first, or past the end: nothing to write.
        viewModel.onMoveSymbol("s1", 0)
        viewModel.onMoveSymbol("s1", 9)

        assertEquals(
            listOf(AacWrite("reorderSymbols", "b1", mapOf("s3" to 0, "s1" to 1, "s2" to 2))),
            boards.writes
        )
    }

    @Test
    fun `a saved phrase dragged to another place gives every phrase its place`() = runTest {
        savedMessages.messages.value = DataResult.Success(
            listOf(
                SavedMessage("new", "Oi"),
                SavedMessage("a", "Tchau", position = 0),
                SavedMessage("b", "Sim", position = 1)
            )
        )
        val viewModel = communicateViewModel()
        viewModel.onSelect(AacSelection.SavedMessages)

        viewModel.onMoveSaved("b", 0)
        // Already first, or past the end: nothing to write.
        viewModel.onMoveSaved("new", 0)
        viewModel.onMoveSaved("new", 9)

        // The phrase never moved gets its place too, so it stays where the person saw it.
        assertEquals(listOf(mapOf("b" to 0, "new" to 1, "a" to 2)), savedMessages.reorders)
    }

    @Test
    fun `a ready-made phrase dragged to another place keeps the new order on this device`() = runTest {
        val viewModel = communicateViewModel()
        val appOrder = readyPhrases.map { it.id }

        viewModel.onMoveReady("help", 0)
        // Already first: the order stays.
        viewModel.onMoveReady("help", 0)

        assertEquals(listOf("help") + (appOrder - "help"), preferences.preferences.value.readyPhraseOrder)
        assertEquals("help", readyPhrasesIn(viewModel.uiState.value.preferences.readyPhraseOrder).first().id)
    }

    @Test
    fun `a refused read is an error, never an empty board`() = runTest {
        boards.boards.value = DataResult.Failure(DataError.PERMISSION_DENIED)
        val viewModel = communicateViewModel()

        assertEquals(ContentState.Error(R.string.error_permission_denied), viewModel.uiState.value.boards)
    }

    @Test
    fun `a deleted board falls back to the first remaining one`() = runTest {
        val viewModel = communicateViewModel()
        viewModel.onSelect(AacSelection.Board("b2"))

        boards.boards.value = listSnapshotOf(board("b1", "Básico"))

        assertEquals(AacSelection.Board("b1"), viewModel.uiState.value.selection)
    }

    @Test
    fun `the starter board is created once even with a double tap on a slow network`() = runTest {
        boards.boards.value = listSnapshotOf()
        boards.writeGate = CompletableDeferred()
        val viewModel = communicateViewModel()
        val content = StarterBoardContent("Básico", listOf(SymbolDraft("Sim", null, null)))

        viewModel.onCreateStarterBoard(content)
        viewModel.onCreateStarterBoard(content)
        boards.writeGate?.complete(Unit)

        assertEquals(listOf("createBoardWithSymbols"), boards.writes.map { it.operation })
        assertEquals(ContentState.Empty, viewModel.uiState.value.symbols)
    }

    @Test
    fun `leaving the screen for good releases the voice engine`() = runTest {
        val viewModel = communicateViewModel()
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
        }
        ViewModelProvider(store, factory)[AacCommunicateViewModel::class.java]

        store.clear()

        assertTrue(speech.isClosed)
    }
}
