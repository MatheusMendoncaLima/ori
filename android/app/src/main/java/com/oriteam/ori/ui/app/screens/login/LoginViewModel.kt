package com.oriteam.ori.ui.app.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.auth.AuthRepository
import com.oriteam.ori.domain.auth.AuthResult
import com.oriteam.ori.domain.auth.validatePasswordReset
import com.oriteam.ori.domain.auth.validateSignIn
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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** One-time navigation requests. A Channel delivers each event once, unlike state. */
    sealed interface Event {
        data object LoginSuccess : Event
        data object NavigateToRegister : Event
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailInputChange(value: String) {
        _uiState.update { it.copy(emailInputValue = value, errorMessageRes = null) }
    }

    fun onPasswordInputChange(value: String) {
        _uiState.update { it.copy(passwordInputValue = value, errorMessageRes = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.isLoading) return

        val email = state.emailInputValue.trim()
        val validationError = validateSignIn(email, state.passwordInputValue)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessageRes = validationError.messageRes(), successMessageRes = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null, successMessageRes = null) }
            when (val result = authRepository.signIn(email, state.passwordInputValue)) {
                AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(Event.LoginSuccess)
                }
                is AuthResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessageRes = result.error.messageRes())
                }
            }
        }
    }

    fun onForgotPasswordClick() {
        val state = _uiState.value
        if (state.isLoading) return

        val email = state.emailInputValue.trim()
        val validationError = validatePasswordReset(email)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessageRes = validationError.messageRes(), successMessageRes = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null, successMessageRes = null) }
            when (val result = authRepository.sendPasswordResetEmail(email)) {
                AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, successMessageRes = R.string.password_reset_requested)
                }
                is AuthResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessageRes = result.error.messageRes())
                }
            }
        }
    }

    fun onNavigateToRegisterClick() {
        viewModelScope.launch {
            _events.send(Event.NavigateToRegister)
        }
    }
}
