package com.oriteam.ori.ui.app.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.preferences.DevicePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The first page before signing in. On the first launch of this device the logo plays its opening
 * ([com.oriteam.ori.ui.components.brand.OriIntroBrand]) and the options come after it; from then on, the
 * options come after a short pause, as before.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val devicePreferences: DevicePreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasSeen = devicePreferences.preferences.first().hasSeenIntro
            _uiState.update { it.copy(playsIntro = it.playsIntro ?: !hasSeen) }
        }
    }

    fun onTimerFinished() {
        _uiState.update { it.copy(showOptions = true) }
    }

    /** Recorded at the end, so leaving in the middle plays it again next time. */
    fun onIntroFinished() {
        _uiState.update { it.copy(showOptions = true) }
        viewModelScope.launch { devicePreferences.update { it.copy(hasSeenIntro = true) } }
    }
}
