package com.oriteam.ori.ui.app.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.data.repository.AuthRepository
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

    sealed interface Event {
        data object LoginSuccess : Event
        data object NavigateToRegister : Event
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailInputChange(value: String) {
        _uiState.update { it.copy(emailInputValue = value, errorMessage = null) }
    }

    fun onPasswordInputChange(value: String) {
        _uiState.update { it.copy(passwordInputValue = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleUppercase() {
        _uiState.update { it.copy(isUppercase = !it.isUppercase) }
    }

    fun onLoginClick() {
        val state = _uiState.value

        if (state.emailInputValue.isBlank() || state.passwordInputValue.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Preencha o e-mail e a senha") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                authRepository.login(
                    email = state.emailInputValue,
                    password = state.passwordInputValue
                )
                _uiState.update { it.copy(isLoading = false) }
                _events.send(Event.LoginSuccess)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Erro ao realizar login")
                }
            }
        }
    }

    fun onForgotPasswordClick() {
        val email = _uiState.value.emailInputValue.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe o e-mail para redefinir a senha") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                authRepository.sendPasswordResetEmail(email)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "E-mail de redefinição enviado com sucesso!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Erro ao enviar e-mail")
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
