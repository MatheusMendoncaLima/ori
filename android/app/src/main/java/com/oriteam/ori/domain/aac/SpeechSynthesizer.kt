package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.preferences.VoiceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class SpeechState {
    /** The voice engine is starting. */
    PREPARING,
    READY,
    SPEAKING,

    /** No voice engine, or no voice in the app's language: the message is shown on screen instead. */
    UNAVAILABLE
}

/**
 * A voice of the app's language installed on the device. Engines give it only a technical name ([id]): they do not say
 * whether it is of a man, a woman or a child, so the screen numbers the voices and lets the person listen.
 */
data class VoiceOption(val id: String, val needsNetwork: Boolean)

/**
 * Reads messages aloud. Each instance holds a voice engine connection, so its owner must call
 * [close] when the screen is gone.
 */
interface SpeechSynthesizer {
    val state: StateFlow<SpeechState>

    /** The voices of the app's language on this device; empty while the engine starts or when it offers no choice. */
    val voices: StateFlow<List<VoiceOption>>

    /** How everything spoken from now on sounds. A voice that is no longer installed falls back to the default. */
    fun configure(settings: VoiceSettings)

    /**
     * Speaks [text], replacing anything being spoken, in [language] (a language tag such as "es"; null is the
     * app's). Returns false when it cannot speak, also when the device has no voice in that language.
     */
    fun speak(text: String, language: String? = null): Boolean

    /**
     * Texts that [speak] accepted but the engine could not say, for example with a voice that needs internet and
     * no connection. The answer to [speak] comes before the engine tries, so this is the only way to know.
     */
    val failures: Flow<String>

    fun stop()

    fun close()
}
