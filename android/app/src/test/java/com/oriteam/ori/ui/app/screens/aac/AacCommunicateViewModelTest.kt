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
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.testing.FakeAacBoardRepository
import com.oriteam.ori.testing.FakeAacPreferencesRepository
import com.oriteam.ori.testing.FakeAacUsageRepository
import com.oriteam.ori.testing.FakePhotoStore
import com.oriteam.ori.testing.FakeSavedMessageRepository
import com.oriteam.ori.testing.FakeSpeechSynthesizer
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.OWNER
import com.oriteam.ori.testing.board
import com.oriteam.ori.testing.listSnapshotOf
import com.oriteam.ori.testing.symbol
import com.oriteam.ori.ui.components.state.ContentState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

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

    private fun TestScope.communicateViewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        AacCommunicateViewModel(
            boards,
            savedMessages,
            preferences,
            speech,
            FakePhotoStore(),
            FakeAacUsageRepository(),
            Clock.systemDefaultZone(),
            savedState
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
            FakePhotoStore(),
            FakeAacUsageRepository(),
            Clock.systemDefaultZone(),
            SavedStateHandle()
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.show(OWNER)

        assertEquals(AacSelection.SavedMessages, viewModel.uiState.value.selection)
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
    fun `a ready phrase is spoken and does not touch the message being built`() = runTest {
        val viewModel = communicateViewModel()
        viewModel.onSymbolTap("s2")

        viewModel.onSayPhrase("Preciso de ajuda")

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
        viewModel.onSayPhrase("Bom dia")
        assertEquals(listOf("Bom dia"), speech.spoken)

        viewModel.onDeleteSavedRequest(SavedMessage("m1", "Bom dia"))
        assertTrue(savedMessages.deleted.isEmpty())
        viewModel.onDeleteSavedConfirm()
        assertEquals(listOf("m1"), savedMessages.deleted)
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
