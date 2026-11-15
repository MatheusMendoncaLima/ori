package com.oriteam.ori.ui.app.screens.aac

import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.SpeechSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** How long a tap waits for a voice that is still starting before the text is shown in large letters. */
internal const val VOICE_START_TIMEOUT_MILLIS = 3_000L

/**
 * The voice engine needs a moment to start after the screen opens. A tap in that moment waits for it,
 * instead of being taken as "no voice" (the first ready phrase used to open the large letters). Only
 * the latest tap waits: a new one replaces it, as speaking does. [onSaid] runs after each text the voice took (the
 * screen then checks that its volume is not at zero).
 */
internal class SpeechWhenReady(
    private val speech: SpeechSynthesizer,
    private val scope: CoroutineScope,
    private val onSaid: () -> Unit = {}
) {

    private var waiting: Job? = null

    /**
     * [language] is the one of [text] (null: the app's). [onSilent] runs when it cannot be said: no voice on the
     * device or in that language, or one that did not start in time.
     */
    fun speak(text: String, language: String? = null, onSilent: () -> Unit = {}) {
        waiting?.cancel()
        if (speech.state.value != SpeechState.PREPARING) {
            say(text, language, onSilent)
            return
        }
        waiting = scope.launch {
            val started = withTimeoutOrNull(VOICE_START_TIMEOUT_MILLIS) {
                speech.state.first { it != SpeechState.PREPARING }
            }
            if (started == null) onSilent() else say(text, language, onSilent)
        }
    }

    private fun say(text: String, language: String?, onSilent: () -> Unit) {
        if (speech.speak(text, language)) onSaid() else onSilent()
    }

    /** "Parar" also drops a text still waiting for the voice. */
    fun cancel() {
        waiting?.cancel()
    }
}
