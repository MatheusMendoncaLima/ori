package com.oriteam.ori.data.aac

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.SpeechSynthesizer
import com.oriteam.ori.domain.common.AppLocale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject

private const val TAG = "OriSpeech"

/** Brazilian Portuguese, the language of the app texts and of the default vocabulary. */

/**
 * Uses the voice engine installed on the device (Android TextToSpeech). It works offline when the
 * engine has a local Portuguese voice. Nothing spoken is logged or sent anywhere by the app.
 *
 * Without an engine or a Portuguese voice the state is [SpeechState.UNAVAILABLE] and the screen
 * shows the message in large letters instead: a voice in another language would be misleading.
 */
class AndroidSpeechSynthesizer @Inject constructor(
    @ApplicationContext context: Context
) : SpeechSynthesizer {

    private val _state = MutableStateFlow(SpeechState.PREPARING)
    override val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val engine: TextToSpeech = TextToSpeech(context) { status -> onEngineReady(status) }

    private fun onEngineReady(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Speech engine unavailable: status $status")
            _state.value = SpeechState.UNAVAILABLE
            return
        }
        val language = engine.setLanguage(AppLocale)
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "No Portuguese voice installed: result $language")
            _state.value = SpeechState.UNAVAILABLE
            return
        }
        engine.setOnUtteranceProgressListener(progressListener)
        _state.value = SpeechState.READY
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _state.value = SpeechState.SPEAKING
        }

        override fun onDone(utteranceId: String?) {
            _state.value = SpeechState.READY
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            _state.value = SpeechState.READY
        }

        @Deprecated("Required override for API levels before 21")
        override fun onError(utteranceId: String?) {
            _state.value = SpeechState.READY
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            Log.w(TAG, "Speech failed: code $errorCode")
            _state.value = SpeechState.READY
        }
    }

    override fun speak(text: String): Boolean {
        val usable = _state.value == SpeechState.READY || _state.value == SpeechState.SPEAKING
        if (!usable || text.isBlank()) return false
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        return result == TextToSpeech.SUCCESS
    }

    override fun stop() {
        engine.stop()
    }

    override fun close() {
        engine.stop()
        engine.shutdown()
        _state.value = SpeechState.UNAVAILABLE
    }
}
