package com.oriteam.ori.ui.app.screens.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.auth.RegisterAccount
import com.oriteam.ori.domain.auth.RegistrationResult
import com.oriteam.ori.domain.auth.validateRegistration
import com.oriteam.ori.ui.app.screens.auth.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerAccount: RegisterAccount
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /** One-time navigation requests. A Channel delivers each event once, unlike state. */
    sealed interface Event {
        data object RegisterSuccess : Event
        data object NavigateToLogin : Event
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onNameInputChange(value: String) {
        _uiState.update { it.copy(nameInputValue = value, errorMessageRes = null) }
    }

    fun onEmailInputChange(value: String) {
        _uiState.update { it.copy(emailInputValue = value, errorMessageRes = null) }
    }

    fun onPasswordInputChange(value: String) {
        _uiState.update { it.copy(passwordInputValue = value, errorMessageRes = null) }
    }

    fun onConfirmPasswordInputChange(value: String) {
        _uiState.update { it.copy(confirmPasswordInputValue = value, errorMessageRes = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun onNavigateToLoginClick() {
        viewModelScope.launch {
            _events.send(Event.NavigateToLogin)
        }
    }

    fun onRegisterClick() {
        val state = _uiState.value
        if (state.isLoading) return

        val name = state.nameInputValue.trim()
        val email = state.emailInputValue.trim()
        val validationError = validateRegistration(
            name = name,
            email = email,
            password = state.passwordInputValue,
            passwordConfirmation = state.confirmPasswordInputValue
        )
        if (validationError != null) {
            _uiState.update { it.copy(errorMessageRes = validationError.messageRes()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
            when (val result = registerAccount(name, email, state.passwordInputValue)) {
                // Without a saved profile the session asks for it again, so both lead forward.
                RegistrationResult.Completed, is RegistrationResult.ProfileNotSaved -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(Event.RegisterSuccess)
                }
                is RegistrationResult.Failed -> _uiState.update {
                    it.copy(isLoading = false, errorMessageRes = result.error.messageRes())
                }
            }
        }
    }
}
