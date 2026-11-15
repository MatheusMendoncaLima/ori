package com.oriteam.ori.ui.app.screens.aac

import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.VoiceOption
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.VoicePreset
import com.oriteam.ori.domain.preferences.VoiceSettings
import com.oriteam.ori.domain.preferences.VoiceSound
import com.oriteam.ori.testing.FakeAacPreferencesRepository
import com.oriteam.ori.testing.FakeSpeechSynthesizer
import com.oriteam.ori.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val SAMPLE = "Olá! Eu quero água."

class VoiceSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeAacPreferencesRepository()
    private val speech = FakeSpeechSynthesizer()

    private fun TestScope.voiceSettings() = VoiceSettingsViewModel(preferences, speech).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    @Test
    fun `the voices of the device are offered, and choosing one saves it and plays it`() = runTest {
        speech.voices.value = listOf(VoiceOption("pt-br-x-afs-local", false), VoiceOption("pt-br-x-ptd-network", true))
        val viewModel = voiceSettings()
        assertEquals(2, viewModel.uiState.value.voices.size)

        viewModel.onVoiceSelected("pt-br-x-ptd-network", SAMPLE)

        assertEquals("pt-br-x-ptd-network", preferences.preferences.value.voice.voiceId)
        assertEquals(SAMPLE to VoiceSettings(voiceId = "pt-br-x-ptd-network"), speech.spokenWith.last())
    }

    @Test
    fun `a style is saved and heard at once`() = runTest {
        val viewModel = voiceSettings()

        viewModel.onPresetSelected(VoicePreset.DEEP, SAMPLE)

        assertEquals(VoicePreset.DEEP, preferences.preferences.value.voice.preset)
        assertEquals(VoicePreset.DEEP, viewModel.uiState.value.settings.preset)
        assertEquals(VoiceSettings().with(VoicePreset.DEEP), speech.spokenWith.last().second)
    }

    @Test
    fun `a slider shows its value while it moves, and saves and plays only when it is let go`() = runTest {
        val viewModel = voiceSettings()

        viewModel.onPitchChange(1.3333f)
        assertEquals(1.35f, viewModel.uiState.value.settings.pitch)
        assertEquals(AacPreferences().voice, preferences.preferences.value.voice)
        assertTrue(speech.spoken.isEmpty())

        viewModel.onAdjustmentFinished(SAMPLE)

        assertEquals(1.35f, preferences.preferences.value.voice.pitch)
        assertEquals(listOf(SAMPLE), speech.spoken)
    }

    @Test
    fun `back to the default is the device's voice as it comes, at the volume chosen`() = runTest {
        preferences.preferences.value =
            AacPreferences(voice = VoiceSettings("pt-br-x-afs-local", 1.6f, 1.1f, VoiceSound.RING))
        val viewModel = voiceSettings()

        viewModel.onReset(SAMPLE)

        assertEquals(VoiceSettings(sound = VoiceSound.RING), preferences.preferences.value.voice)
    }

    @Test
    fun `the volume of the voice is saved and the example is heard at it`() = runTest {
        val viewModel = voiceSettings()

        viewModel.onSoundSelected(VoiceSound.RING, SAMPLE)

        assertEquals(VoiceSound.RING, preferences.preferences.value.voice.sound)
        assertEquals(VoiceSound.RING, speech.configured.last().sound)
        assertEquals(SAMPLE, speech.spoken.last())
    }

    @Test
    fun `a choice made while the voice is still starting is heard when it starts`() = runTest {
        speech.state.value = SpeechState.PREPARING
        val viewModel = voiceSettings()

        viewModel.onPresetSelected(VoicePreset.CHILD, SAMPLE)
        assertTrue(speech.spoken.isEmpty())
        speech.state.value = SpeechState.READY
        runCurrent()

        assertEquals(listOf(SAMPLE), speech.spoken)
    }

    @Test
    fun `the example plays with what is on screen, even before it is saved`() = runTest {
        val viewModel = voiceSettings()
        viewModel.onRateChange(0.7f)

        viewModel.onPreview(SAMPLE)

        assertEquals(SAMPLE to VoiceSettings(rate = 0.7f), speech.spokenWith.last())
    }
}
