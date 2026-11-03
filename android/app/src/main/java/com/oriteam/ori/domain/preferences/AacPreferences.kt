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

data class AacPreferences(
    val symbolSize: SymbolSize = SymbolSize.SMALL,
    val labelMode: SymbolLabelMode = SymbolLabelMode.BELOW_IMAGE,
    val messageMode: MessageMode = MessageMode.COMPOSE
)

/**
 * Communication preferences of this device. They depend on the screen and on who holds the
 * device, so they are not synced with the account.
 */
interface AacPreferencesRepository {
    val preferences: Flow<AacPreferences>

    suspend fun update(transform: (AacPreferences) -> AacPreferences)
}
