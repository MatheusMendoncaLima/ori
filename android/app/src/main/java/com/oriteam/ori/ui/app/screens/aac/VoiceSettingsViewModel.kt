package com.oriteam.ori.ui.app.screens.aac

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.SpeechSynthesizer
import com.oriteam.ori.domain.aac.VoiceOption
import com.oriteam.ori.domain.preferences.AacPreferencesRepository
import com.oriteam.ori.domain.preferences.VoicePreset
import com.oriteam.ori.domain.preferences.VoiceSettings
import com.oriteam.ori.domain.preferences.VoiceSound
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Tone and speed move in steps of 0.05: fine enough to hear, and the styles land exactly on their values. */
private const val STEPS_PER_UNIT = 20

data class VoiceSettingsUiState(
    val settings: VoiceSettings = VoiceSettings(),
    val voices: List<VoiceOption> = emptyList(),
    val speechState: SpeechState = SpeechState.PREPARING
)

/**
 * The voice of the communication boards: which installed voice, its tone and its speed. It has a voice
 * engine of its own, so the example sounds exactly as the boards will. Choosing a voice or a style plays the
 * example at once; a slider plays it when the finger lifts, which is also when the choice is saved.
 */
@HiltViewModel
class VoiceSettingsViewModel @Inject constructor(
    private val preferencesRepository: AacPreferencesRepository,
    private val speech: SpeechSynthesizer
) : ViewModel() {

    private val saved = preferencesRepository.preferences.map { it.voice }
        .stateIn(viewModelScope, SharingStarted.Eagerly, VoiceSettings())

    /** What the screen shows while a slider moves, or while a change is on its way to the storage. */
    private val adjusting = MutableStateFlow<VoiceSettings?>(null)

    /** A choice made while the voice is still starting is heard as soon as it starts. */
    private val voice = SpeechWhenReady(speech, viewModelScope)

    val uiState: StateFlow<VoiceSettingsUiState> =
        combine(saved, adjusting, speech.voices, speech.state) { stored, draft, voices, state ->
            VoiceSettingsUiState(settings = draft ?: stored, voices = voices, speechState = state)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), VoiceSettingsUiState())

    init {
        viewModelScope.launch {
            saved.collect { stored ->
                // The stored value caught up with the screen: the screen can follow the storage again.
                if (adjusting.value == stored) adjusting.value = null
                if (adjusting.value == null) speech.configure(stored)
            }
        }
    }

    private val current: VoiceSettings get() = adjusting.value ?: saved.value

    /** null is the default voice of the device. */
    fun onVoiceSelected(voiceId: String?, sample: String) = save(sample) { it.copy(voiceId = voiceId) }

    fun onPresetSelected(preset: VoicePreset, sample: String) = save(sample) { it.with(preset) }

    fun onPitchChange(value: Float) {
        adjusting.value = current.copy(pitch = value.toStep())
    }

    fun onRateChange(value: Float) {
        adjusting.value = current.copy(rate = value.toStep())
    }

    /** The finger left a slider: the new tone or speed is saved and heard. */
    fun onAdjustmentFinished(sample: String) = save(sample) { it }

    fun onPreview(sample: String) {
        speech.configure(current)
        voice.speak(sample)
    }

    /** "Volume da voz": the example is heard at the volume chosen. */
    fun onSoundSelected(sound: VoiceSound, sample: String) = save(sample) { it.copy(sound = sound) }

    /** Back to the device's voice, as it comes; the volume it plays at stays as chosen. */
    fun onReset(sample: String) = save(sample) { VoiceSettings(sound = it.sound) }

    private fun save(sample: String, transform: (VoiceSettings) -> VoiceSettings) {
        val updated = transform(current).normalized()
        adjusting.value = updated
        speech.configure(updated)
        voice.speak(sample)
        viewModelScope.launch { preferencesRepository.update { it.copy(voice = updated) } }
    }

    private fun Float.toStep(): Float = (this * STEPS_PER_UNIT).roundToInt() / STEPS_PER_UNIT.toFloat()

    override fun onCleared() {
        speech.close()
    }
}
