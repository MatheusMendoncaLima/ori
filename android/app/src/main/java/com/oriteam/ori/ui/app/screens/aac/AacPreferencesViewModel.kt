package com.oriteam.ori.ui.app.screens.aac

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.AacPreferencesRepository
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.domain.preferences.SymbolLabelMode
import com.oriteam.ori.domain.preferences.SymbolSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Communication preferences of this device: symbol size, labels and what a tap does. */
@HiltViewModel
class AacPreferencesViewModel @Inject constructor(
    private val preferencesRepository: AacPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<AacPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AacPreferences())

    fun onSymbolSizeChange(value: SymbolSize) = update { it.copy(symbolSize = value) }

    fun onLabelModeChange(value: SymbolLabelMode) = update { it.copy(labelMode = value) }

    fun onMessageModeChange(value: MessageMode) = update { it.copy(messageMode = value) }

    fun onDragToReorderChange(value: Boolean) = update { it.copy(dragToReorder = value) }

    private fun update(transform: (AacPreferences) -> AacPreferences) {
        viewModelScope.launch { preferencesRepository.update(transform) }
    }
}
