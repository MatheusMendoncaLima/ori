package com.oriteam.ori.domain.aac

import kotlinx.coroutines.flow.StateFlow

enum class SpeechState {
    /** The voice engine is starting. */
    PREPARING,
    READY,
    SPEAKING,

    /** No voice engine, or no Portuguese voice installed: the message is shown on screen instead. */
    UNAVAILABLE
}

/**
 * Reads messages aloud. Each instance holds a voice engine connection, so its owner must call
 * [close] when the screen is gone.
 */
interface SpeechSynthesizer {
    val state: StateFlow<SpeechState>

    /** Speaks [text], replacing anything being spoken. Returns false when it cannot speak. */
    fun speak(text: String): Boolean

    fun stop()

    fun close()
}
