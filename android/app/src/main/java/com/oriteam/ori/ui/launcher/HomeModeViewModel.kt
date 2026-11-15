package com.oriteam.ori.ui.launcher

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.HomeActivation
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeModeUiState(
    val isOriHome: Boolean = false,
    @StringRes val messageRes: Int? = null
)

/**
 * Turning ORI Home on and off, from the settings of ORI. On: the home entry is turned on, the launcher in use is
 * remembered, and Android is asked (the person decides there). A refusal turns the entry off again, so ORI does not
 * linger in the list of home apps. Off: the entry is turned off and the previous launcher opens.
 */
@HiltViewModel
class HomeModeViewModel @Inject constructor(
    private val activation: HomeActivation,
    private val launcherSettings: LauncherSettingsRepository
) : ViewModel() {

    sealed interface Event {
        /** Show the request of Android (the role, or the screen of default apps). */
        data object RequestHome : Event

        data class OpenPreviousLauncher(val packageName: String?) : Event
    }

    private val _uiState = MutableStateFlow(HomeModeUiState(isOriHome = activation.isOriHome()))
    val uiState: StateFlow<HomeModeUiState> = _uiState.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** The person may change the home app in the system settings meanwhile: read it again when the screen shows. */
    fun refresh() = _uiState.update { it.copy(isOriHome = activation.isOriHome()) }

    fun onActivate() {
        if (activation.isOriHome()) return refresh()
        viewModelScope.launch {
            val previous = activation.currentHomePackage()
            if (previous != null) launcherSettings.update { it.copy(previousLauncher = previous) }
            activation.setLauncherEnabled(true)
            _uiState.update { it.copy(messageRes = null) }
            _events.send(Event.RequestHome)
        }
    }

    /** Back from the request of Android: approved, or the entry goes away again. */
    fun onRequestResult() {
        val isApproved = activation.isOriHome()
        if (!isApproved) activation.setLauncherEnabled(false)
        _uiState.update {
            it.copy(
                isOriHome = isApproved,
                messageRes = if (isApproved) R.string.home_mode_approved else R.string.home_mode_denied
            )
        }
    }

    fun onDeactivate() {
        activation.setLauncherEnabled(false)
        viewModelScope.launch {
            val previous = launcherSettings.settings.first().previousLauncher
            _uiState.update { it.copy(isOriHome = false, messageRes = null) }
            _events.send(Event.OpenPreviousLauncher(previous))
        }
    }
}
