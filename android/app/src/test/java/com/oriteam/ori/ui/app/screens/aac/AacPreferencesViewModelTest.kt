package com.oriteam.ori.ui.app.screens.aac

import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.domain.preferences.SymbolLabelMode
import com.oriteam.ori.domain.preferences.SymbolSize
import com.oriteam.ori.testing.FakeAacPreferencesRepository
import com.oriteam.ori.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AacPreferencesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `each choice changes only its own preference, and the voice stays as it was`() = runTest {
        val repository = FakeAacPreferencesRepository()
        val viewModel = AacPreferencesViewModel(repository)
        val voice = repository.preferences.value.voice

        viewModel.onSymbolSizeChange(SymbolSize.LARGE)
        viewModel.onLabelModeChange(SymbolLabelMode.TEXT_ONLY)
        viewModel.onMessageModeChange(MessageMode.SPEAK_IMMEDIATELY)
        viewModel.onDragToReorderChange(false)

        assertEquals(
            AacPreferences(
                symbolSize = SymbolSize.LARGE,
                labelMode = SymbolLabelMode.TEXT_ONLY,
                messageMode = MessageMode.SPEAK_IMMEDIATELY,
                voice = voice,
                dragToReorder = false
            ),
            repository.preferences.first()
        )
    }

    @Test
    fun `the screen shows what is stored`() = runTest {
        val stored = AacPreferences(symbolSize = SymbolSize.LARGE, dragToReorder = false)
        val viewModel = AacPreferencesViewModel(FakeAacPreferencesRepository(stored))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.preferences.collect() }

        assertEquals(stored, viewModel.preferences.value)
    }
}
