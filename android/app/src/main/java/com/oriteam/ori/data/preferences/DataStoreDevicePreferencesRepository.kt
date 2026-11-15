package com.oriteam.ori.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.DevicePreferences
import com.oriteam.ori.domain.preferences.DevicePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Device preferences and the local copy of the account preferences, in the same local DataStore
 * as the other display choices. Values are saved by enum name; an unknown name (for example after
 * a downgrade) falls back to the default instead of crashing.
 */
class DataStoreDevicePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : DevicePreferencesRepository {

    override val preferences: Flow<DevicePreferences> = dataStore.data
        .map { it.toDevicePreferences() }
        .distinctUntilChanged()

    override val accountCopy: Flow<AccountPreferences> = dataStore.data
        .map { it.toAccountPreferences() }
        .distinctUntilChanged()

    override suspend fun update(transform: (DevicePreferences) -> DevicePreferences) {
        dataStore.edit { stored ->
            val updated = transform(stored.toDevicePreferences())
            stored[DENSITY_KEY] = updated.density.name
            stored[LIST_PRESENTATION_KEY] = updated.listPresentation.name
            stored[START_AREA_KEY] = updated.startArea.name
            stored[UPPERCASE_KEY] = updated.isUppercase
            stored[INTRO_SEEN_KEY] = updated.hasSeenIntro
        }
    }

    override suspend fun saveAccountCopy(preferences: AccountPreferences) {
        dataStore.edit { stored ->
            stored[TEXT_SCALE_KEY] = preferences.textScale.name
            stored[THEME_KEY] = preferences.theme.name
            stored[SIMPLIFIED_KEY] = preferences.isSimplified
            stored[MOTION_KEY] = preferences.motion.name
        }
    }

    private fun Preferences.toDevicePreferences(): DevicePreferences {
        val defaults = DevicePreferences()
        return DevicePreferences(
            density = enumOrDefault(this[DENSITY_KEY], defaults.density),
            listPresentation = enumOrDefault(this[LIST_PRESENTATION_KEY], defaults.listPresentation),
            startArea = enumOrDefault(this[START_AREA_KEY], defaults.startArea),
            isUppercase = this[UPPERCASE_KEY] ?: defaults.isUppercase,
            hasSeenIntro = this[INTRO_SEEN_KEY] ?: defaults.hasSeenIntro
        )
    }

    private fun Preferences.toAccountPreferences(): AccountPreferences {
        val defaults = AccountPreferences()
        return AccountPreferences(
            textScale = enumOrDefault(this[TEXT_SCALE_KEY], defaults.textScale),
            theme = enumOrDefault(this[THEME_KEY], defaults.theme),
            isSimplified = this[SIMPLIFIED_KEY] ?: defaults.isSimplified,
            motion = enumOrDefault(this[MOTION_KEY], defaults.motion)
        )
    }

    private companion object {
        val DENSITY_KEY = stringPreferencesKey("device_density")
        val LIST_PRESENTATION_KEY = stringPreferencesKey("device_list_presentation")
        val START_AREA_KEY = stringPreferencesKey("device_start_area")
        val INTRO_SEEN_KEY = booleanPreferencesKey("device_intro_seen")

        // Name kept from the first version of this preference, so existing choices stay.
        val UPPERCASE_KEY = booleanPreferencesKey("display_uppercase")
        val TEXT_SCALE_KEY = stringPreferencesKey("account_copy_text_scale")
        val THEME_KEY = stringPreferencesKey("account_copy_theme")
        val SIMPLIFIED_KEY = booleanPreferencesKey("account_copy_simplified")
        val MOTION_KEY = stringPreferencesKey("account_copy_motion")
    }
}
