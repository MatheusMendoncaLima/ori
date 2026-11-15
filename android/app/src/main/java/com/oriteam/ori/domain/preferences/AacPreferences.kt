package com.oriteam.ori.domain.preferences

import kotlinx.coroutines.flow.Flow

/** How big symbols are; the grid shows fewer columns for bigger symbols. */
enum class SymbolSize { SMALL, MEDIUM, LARGE }

/** What a symbol shows. The label is always read by screen readers, even when hidden. */
enum class SymbolLabelMode { BELOW_IMAGE, ABOVE_IMAGE, TEXT_ONLY, IMAGE_ONLY }

/**
 * How a tap on a symbol is used. People communicate differently: some build sentences,
 * some need each word spoken at once.
 */
enum class MessageMode {
    /** Taps build a message that is spoken with "Falar". */
    COMPOSE,

    /** Same as [COMPOSE], and each symbol is also spoken when added (auditory feedback). */
    COMPOSE_WITH_FEEDBACK,

    /** Each tap speaks the symbol right away; no message is built. */
    SPEAK_IMMEDIATELY
}

/**
 * Which volume of the device the voice plays at. [MEDIA] is the volume of music and videos, heard also in silent mode;
 * [RING] is the volume of the ringtone (the bell of the volume control), silent with the phone.
 */
enum class VoiceSound { MEDIA, RING }

/** Lowest and highest tone and speed: beyond them the voice of most engines is hard to understand. */
const val MIN_VOICE_PITCH = 0.5f
const val MAX_VOICE_PITCH = 2f
const val MIN_VOICE_RATE = 0.5f
const val MAX_VOICE_RATE = 2f

/**
 * How the voice of the boards sounds: one of the voices installed on this device, its tone and its speed.
 * 1 is the voice as the engine gives it. It is the person's own voice, so every part can be adjusted.
 */
data class VoiceSettings(
    /** A voice of the device's engine, by the engine's name; null uses its default Portuguese voice. */
    val voiceId: String? = null,
    /** Below 1 the voice is deeper, above 1 higher. */
    val pitch: Float = 1f,
    /** Below 1 slower, above 1 faster. */
    val rate: Float = 1f,
    /** The volume of the device it plays at. */
    val sound: VoiceSound = VoiceSound.MEDIA
) {
    /** The same settings within the supported ranges, whatever was stored. */
    fun normalized() = copy(
        pitch = pitch.coerceIn(MIN_VOICE_PITCH, MAX_VOICE_PITCH),
        rate = rate.coerceIn(MIN_VOICE_RATE, MAX_VOICE_RATE)
    )

    /** The style these settings match, if any. */
    val preset: VoicePreset? get() = VoicePreset.entries.firstOrNull { it.pitch == pitch && it.rate == rate }

    /** Keeps the chosen voice and takes the tone and speed of [style]. */
    fun with(style: VoicePreset) = copy(pitch = style.pitch, rate = style.rate)
}

/**
 * Ready-made tone and speed. Engines do not say whether a voice is of a man, a woman or a child, so the
 * style changes how the chosen voice sounds: a deep style on a deep voice sounds like a man, a child style
 * is higher and a little faster.
 */
enum class VoicePreset(val pitch: Float, val rate: Float) {
    DEEP(pitch = 0.75f, rate = 0.95f),
    NATURAL(pitch = 1f, rate = 1f),
    HIGH(pitch = 1.25f, rate = 1f),
    CHILD(pitch = 1.6f, rate = 1.1f)
}

data class AacPreferences(
    val symbolSize: SymbolSize = SymbolSize.SMALL,
    val labelMode: SymbolLabelMode = SymbolLabelMode.BELOW_IMAGE,
    val messageMode: MessageMode = MessageMode.COMPOSE,
    val voice: VoiceSettings = VoiceSettings(),
    /**
     * Holding and dragging a symbol in Montar or a phrase in Prontas changes its place. Off for who moves them by
     * accident.
     */
    val dragToReorder: Boolean = true,
    /** The phrases that come with the app, in the order the person chose, by their ids; empty: the app's order. */
    val readyPhraseOrder: List<String> = emptyList()
)

/**
 * Communication preferences of this device. They depend on the screen, on who holds the device and on the
 * voices installed on it, so they are not synced with the account.
 */
interface AacPreferencesRepository {
    val preferences: Flow<AacPreferences>

    suspend fun update(transform: (AacPreferences) -> AacPreferences)
}
