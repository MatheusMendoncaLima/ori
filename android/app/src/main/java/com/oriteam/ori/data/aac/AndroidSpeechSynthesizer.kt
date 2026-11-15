package com.oriteam.ori.data.aac

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.SpeechSynthesizer
import com.oriteam.ori.domain.aac.VoiceOption
import com.oriteam.ori.domain.common.appLocaleFor
import com.oriteam.ori.domain.preferences.VoiceSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.concurrent.thread

private const val TAG = "OriSpeech"

/** Failures waiting for the screen; more than a few at once means the same voice failing again. */
private const val FAILURE_BUFFER = 4

private val missingLanguage = setOf(TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED)

/**
 * Uses the voice engine installed on the device (Android TextToSpeech), in the language of the app's
 * texts, or in the one [speak] is given (a board's or a saved phrase's). It works offline when the engine
 * has a local voice in that language. Nothing spoken is logged or sent anywhere by the app.
 *
 * Without an engine or a voice in the app's language the state is [SpeechState.UNAVAILABLE]; without a
 * voice in the language of one text, [speak] returns false. Either way the screen shows the message in
 * large letters instead: a voice in another language would be misleading.
 */
class AndroidSpeechSynthesizer @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechSynthesizer {

    /** Read each time: the person may change the language while this engine is alive. */
    private val appLanguage: Locale
        get() = context.resources.configuration.locales.let { list -> appLocaleFor(List(list.size()) { list[it] }) }

    /** The language the engine speaks now; null until it is ready. A board in another language changes it. */
    private var engineLanguage: Locale? = null

    private val _state = MutableStateFlow(SpeechState.PREPARING)
    override val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _voices = MutableStateFlow<List<VoiceOption>>(emptyList())
    override val voices: StateFlow<List<VoiceOption>> = _voices.asStateFlow()

    private val _failures = MutableSharedFlow<String>(extraBufferCapacity = FAILURE_BUFFER)
    override val failures: Flow<String> = _failures.asSharedFlow()

    /** What [configure] asked for; applied as soon as the engine is ready. */
    private var settings = VoiceSettings()

    /**
     * The text of each utterance on its way, to report the one that fails. The engine calls back on
     * its own threads.
     */
    private val inFlight = ConcurrentHashMap<String, String>()

    // Declared before the engine: the engine may call back as soon as it exists.
    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _state.value = SpeechState.SPEAKING
        }

        override fun onDone(utteranceId: String?) {
            utteranceId?.let(inFlight::remove)
            _state.value = SpeechState.READY
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            utteranceId?.let(inFlight::remove)
            _state.value = SpeechState.READY
        }

        @Deprecated("Required override for API levels before 21")
        override fun onError(utteranceId: String?) {
            onError(utteranceId, TextToSpeech.ERROR)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            // The code only: the text is the person's message and is never logged.
            Log.w(TAG, "Speech failed: code $errorCode")
            utteranceId?.let(inFlight::remove)?.let { _failures.tryEmit(it) }
            _state.value = SpeechState.READY
        }
    }

    /**
     * Made away from the main thread: asking Android for its voice engines took about 24 ms on it, while the
     * communication screen was opening (M21). Nothing speaks before [onEngineReady]; stopping or closing earlier
     * waits for the engine to exist.
     */
    private val engineMaking = lazy { TextToSpeech(context) { status -> onEngineReady(status) } }
    private val engine: TextToSpeech by engineMaking

    init {
        thread(isDaemon = true, name = "OriVoiceEngine") { engine }
    }

    private fun onEngineReady(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Speech engine unavailable: status $status")
            _state.value = SpeechState.UNAVAILABLE
            return
        }
        val support = engine.setLanguage(appLanguage)
        if (support in missingLanguage) {
            Log.w(TAG, "No voice installed for the app's language: result $support")
            _state.value = SpeechState.UNAVAILABLE
            return
        }
        engine.setOnUtteranceProgressListener(progressListener)
        _voices.value = voicesOf(appLanguage).map { VoiceOption(it.name, it.isNetworkConnectionRequired) }
        apply(settings, appLanguage)
        _state.value = SpeechState.READY
    }

    /**
     * Installed voices of [wanted]'s language: those of its region first (a Spanish of Spain before one of
     * Mexico), then those that work offline. Requiring the region would leave without a voice someone
     * whose engine has the language only from another country.
     */
    private fun voicesOf(wanted: Locale): List<Voice> {
        return engine.voices.orEmpty()
            .filter { it.locale.language == wanted.language }
            .filterNot { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED in it.features.orEmpty() }
            .sortedWith(
                compareBy<Voice> { it.locale.country != wanted.country }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenBy { it.name }
            )
    }

    override fun configure(settings: VoiceSettings) {
        this.settings = settings.normalized()
        val isReady = _state.value == SpeechState.READY || _state.value == SpeechState.SPEAKING
        if (isReady) apply(this.settings, engineLanguage ?: appLanguage)
    }

    /**
     * Sets the voice for [locale], with the chosen tone and speed. The chosen voice is of the app's language;
     * in another language, or when it left the device, the engine's voice for [locale] speaks. Returns false
     * when the device has no voice in [locale]: then nothing changes.
     */
    private fun apply(settings: VoiceSettings, locale: Locale): Boolean {
        // The volume it plays at ("Mídia" or "Toque"); without it, Android's engine plays as media.
        engine.setAudioAttributes(settings.sound.audioAttributes)
        val voice = settings.voiceId?.let { id -> voicesOf(locale).firstOrNull { it.name == id } }
        val isAvailable = if (voice != null) {
            engine.setVoice(voice) == TextToSpeech.SUCCESS
        } else {
            engine.setLanguage(locale) !in missingLanguage
        }
        if (!isAvailable) return false
        engine.setPitch(settings.pitch)
        engine.setSpeechRate(settings.rate)
        engineLanguage = locale
        return true
    }

    override fun speak(text: String, language: String?): Boolean {
        val usable = _state.value == SpeechState.READY || _state.value == SpeechState.SPEAKING
        // A board or phrase in another language is said with a voice of that language, or not at all: a
        // Portuguese voice reading Spanish would be misleading, and the screen shows it in large letters.
        val wanted = language?.let(Locale::forLanguageTag) ?: appLanguage
        val canSpeak = usable && text.isNotBlank() && (wanted == engineLanguage || apply(settings, wanted))
        if (!canSpeak) return false
        val utteranceId = UUID.randomUUID().toString()
        inFlight[utteranceId] = text
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) inFlight.remove(utteranceId)
        return result == TextToSpeech.SUCCESS
    }

    override fun stop() {
        if (engineMaking.isInitialized()) engine.stop()
    }

    override fun close() {
        engine.stop()
        engine.shutdown()
        _state.value = SpeechState.UNAVAILABLE
    }
}
