package com.oriteam.ori.ui.app.screens.preferences

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.AccountPreferencesSync
import com.oriteam.ori.domain.preferences.AppPreferences
import com.oriteam.ori.domain.preferences.ContentDensity
import com.oriteam.ori.domain.preferences.DevicePreferences
import com.oriteam.ori.domain.preferences.DevicePreferencesRepository
import com.oriteam.ori.domain.preferences.ListPresentation
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.domain.preferences.TextScale
import com.oriteam.ori.domain.preferences.ThemeMode
import com.oriteam.ori.ui.app.screens.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * What the root of the app applies. Null only until the stored choices are read, so nobody with
 * large text or a dark theme sees a first frame with the defaults.
 */
@HiltViewModel
class AppPreferencesViewModel @Inject constructor(
    sync: AccountPreferencesSync,
    private val deviceRepository: DevicePreferencesRepository
) : ViewModel() {
    val preferences: StateFlow<AppPreferences?> = sync.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    /** The "Aa" button available at the top of the screens. */
    fun onToggleUppercase() {
        viewModelScope.launch { deviceRepository.update { it.copy(isUppercase = !it.isUppercase) } }
    }
}

data class PreferencesUiState(
    val account: AccountPreferences = AccountPreferences(),
    val device: DevicePreferences = DevicePreferences(),
    @StringRes val errorRes: Int? = null,
    @StringRes val noticeRes: Int? = null
)

/** The preferences screen. Each change applies at once, because the root follows the same data. */
@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val sync: AccountPreferencesSync,
    private val deviceRepository: DevicePreferencesRepository
) : ViewModel() {

    private data class Feedback(@StringRes val errorRes: Int? = null, @StringRes val noticeRes: Int? = null)

    private val feedback = MutableStateFlow(Feedback())

    val uiState: StateFlow<PreferencesUiState> = combine(sync.preferences, feedback) { preferences, message ->
        PreferencesUiState(preferences.account, preferences.device, message.errorRes, message.noticeRes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PreferencesUiState())

    fun onTextScaleChange(value: TextScale) = updateAccount { it.copy(textScale = value) }

    fun onThemeChange(value: ThemeMode) = updateAccount { it.copy(theme = value) }

    fun onSimplifiedChange(value: Boolean) = updateAccount { it.copy(isSimplified = value) }

    fun onMotionChange(value: MotionLevel) = updateAccount { it.copy(motion = value) }

    fun onDensityChange(value: ContentDensity) = updateDevice { it.copy(density = value) }

    fun onListPresentationChange(value: ListPresentation) = updateDevice { it.copy(listPresentation = value) }

    fun onStartAreaChange(value: StartArea) = updateDevice { it.copy(startArea = value) }

    fun onUppercaseChange(value: Boolean) = updateDevice { it.copy(isUppercase = value) }

    private fun updateAccount(transform: (AccountPreferences) -> AccountPreferences) {
        feedback.value = Feedback()
        viewModelScope.launch {
            when (val result = sync.update(transform)) {
                is DataResult.Failure -> feedback.update { Feedback(errorRes = result.error.messageRes()) }
                is DataResult.Success -> if (result.value == WriteStatus.PENDING_SYNC) {
                    feedback.update { Feedback(noticeRes = R.string.sync_pending_changes) }
                }
            }
        }
    }

    private fun updateDevice(transform: (DevicePreferences) -> DevicePreferences) {
        feedback.value = Feedback()
        viewModelScope.launch { deviceRepository.update(transform) }
    }
}
