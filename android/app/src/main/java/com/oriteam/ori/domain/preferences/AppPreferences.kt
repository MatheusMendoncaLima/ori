package com.oriteam.ori.domain.preferences

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow

/** Multiplies the text size chosen in the system settings; it never replaces it. */
enum class TextScale { NORMAL, LARGE, LARGER, LARGEST }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** [SYSTEM] follows "remove animations" in the system settings. */
enum class MotionLevel { SYSTEM, EXPRESSIVE, SUBTLE, REDUCED, OFF }

/**
 * Needs of the person, which stay the same on any device: they follow the account.
 * A copy is kept on the device, so they also apply on the login screen and offline.
 */
data class AccountPreferences(
    val textScale: TextScale = TextScale.NORMAL,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val isSimplified: Boolean = false,
    val motion: MotionLevel = MotionLevel.SYSTEM
)

/** Space between and inside items. Touch targets never go below the accessibility minimum. */
enum class ContentDensity { COMPACT, COMFORTABLE, SPACIOUS }

/** [SUMMARY] shows only the main line of list items; the rest stays available when opening them. */
enum class ListPresentation { DETAILED, SUMMARY }

/** What opens first. */
enum class StartArea { HOME, ROUTINE, COMMUNICATION }

/** Choices that depend on the screen or on how this device is used: they stay on it. */
data class DevicePreferences(
    val density: ContentDensity = ContentDensity.COMFORTABLE,
    val listPresentation: ListPresentation = ListPresentation.DETAILED,
    val startArea: StartArea = StartArea.HOME,
    /** Needed before anyone signs in (the "Aa" button on the login screen), so it stays here. */
    val isUppercase: Boolean = false,
    /** The opening animation of the logo plays once per device, on the first launch. */
    val hasSeenIntro: Boolean = false
)

/** Everything the root of the app applies. */
data class AppPreferences(
    val account: AccountPreferences = AccountPreferences(),
    val device: DevicePreferences = DevicePreferences()
)

/** The copy of [AccountPreferences] in the account (`users/{uid}/settings/preferences`). */
interface AccountPreferencesRepository {
    /** Follows the stored preferences; null means this account never saved any. */
    fun observe(uid: String): Flow<DataResult<AccountPreferences?>>

    suspend fun save(uid: String, preferences: AccountPreferences): DataResult<WriteStatus>
}

/** What is stored only on this device, including the local copy of the account preferences. */
interface DevicePreferencesRepository {
    val preferences: Flow<DevicePreferences>
    val accountCopy: Flow<AccountPreferences>

    suspend fun update(transform: (DevicePreferences) -> DevicePreferences)

    suspend fun saveAccountCopy(preferences: AccountPreferences)
}
