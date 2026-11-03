package com.oriteam.ori.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.AacPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Stores communication preferences in the same local DataStore as the display preferences.
 * Values are saved by enum name; an unknown name (for example after a downgrade) falls back to
 * the default instead of crashing.
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
        }
    }

    private fun Preferences.toAacPreferences(): AacPreferences {
        val defaults = AacPreferences()
        return AacPreferences(
            symbolSize = enumOrDefault(this[SYMBOL_SIZE_KEY], defaults.symbolSize),
            labelMode = enumOrDefault(this[LABEL_MODE_KEY], defaults.labelMode),
            messageMode = enumOrDefault(this[MESSAGE_MODE_KEY], defaults.messageMode)
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        val SYMBOL_SIZE_KEY = stringPreferencesKey("aac_symbol_size")
        val LABEL_MODE_KEY = stringPreferencesKey("aac_label_mode")
        val MESSAGE_MODE_KEY = stringPreferencesKey("aac_message_mode")
    }
}
