package com.oriteam.ori.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.AacPreferencesRepository
import com.oriteam.ori.domain.preferences.VoiceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Stores communication preferences in the same local DataStore as the display preferences.
 * Values are saved by enum name; an unknown name (for example after a downgrade) falls back to
 * the default instead of crashing. The voice stays on this device too: the voices installed differ
 * from one device to another.
 */
class DataStoreAacPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AacPreferencesRepository {

    override val preferences: Flow<AacPreferences> = dataStore.data
        .map { it.toAacPreferences() }
        .distinctUntilChanged()

    override suspend fun update(transform: (AacPreferences) -> AacPreferences) {
        dataStore.edit { stored ->
            val updated = transform(stored.toAacPreferences())
            stored[SYMBOL_SIZE_KEY] = updated.symbolSize.name
            stored[LABEL_MODE_KEY] = updated.labelMode.name
            stored[MESSAGE_MODE_KEY] = updated.messageMode.name
            stored[DRAG_TO_REORDER_KEY] = updated.dragToReorder
            val order = updated.readyPhraseOrder.joinToString(SEPARATOR)
            if (order.isEmpty()) stored.remove(READY_ORDER_KEY) else stored[READY_ORDER_KEY] = order
            val voice = updated.voice.normalized()
            val voiceId = voice.voiceId
            if (voiceId == null) stored.remove(VOICE_ID_KEY) else stored[VOICE_ID_KEY] = voiceId
            stored[VOICE_PITCH_KEY] = voice.pitch
            stored[VOICE_RATE_KEY] = voice.rate
            stored[VOICE_SOUND_KEY] = voice.sound.name
        }
    }

    private fun Preferences.toAacPreferences(): AacPreferences {
        val defaults = AacPreferences()
        return AacPreferences(
            symbolSize = enumOrDefault(this[SYMBOL_SIZE_KEY], defaults.symbolSize),
            labelMode = enumOrDefault(this[LABEL_MODE_KEY], defaults.labelMode),
            messageMode = enumOrDefault(this[MESSAGE_MODE_KEY], defaults.messageMode),
            dragToReorder = this[DRAG_TO_REORDER_KEY] ?: defaults.dragToReorder,
            readyPhraseOrder = this[READY_ORDER_KEY]?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty(),
            voice = VoiceSettings(
                voiceId = this[VOICE_ID_KEY],
                pitch = this[VOICE_PITCH_KEY] ?: defaults.voice.pitch,
                rate = this[VOICE_RATE_KEY] ?: defaults.voice.rate,
                sound = enumOrDefault(this[VOICE_SOUND_KEY], defaults.voice.sound)
            ).normalized()
        )
    }

    private companion object {
        val SYMBOL_SIZE_KEY = stringPreferencesKey("aac_symbol_size")
        val LABEL_MODE_KEY = stringPreferencesKey("aac_label_mode")
        val MESSAGE_MODE_KEY = stringPreferencesKey("aac_message_mode")
        val DRAG_TO_REORDER_KEY = booleanPreferencesKey("aac_drag_to_reorder")

        /** The ids of the ready-made phrases in the chosen order, as "yes,help,no…". */
        val READY_ORDER_KEY = stringPreferencesKey("aac_ready_phrase_order")
        const val SEPARATOR = ","
        val VOICE_ID_KEY = stringPreferencesKey("aac_voice_id")
        val VOICE_PITCH_KEY = floatPreferencesKey("aac_voice_pitch")
        val VOICE_RATE_KEY = floatPreferencesKey("aac_voice_rate")
        val VOICE_SOUND_KEY = stringPreferencesKey("aac_voice_sound")
    }
}
