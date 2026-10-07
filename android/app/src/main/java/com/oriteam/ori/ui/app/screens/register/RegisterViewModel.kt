package com.oriteam.ori.ui.app.screens.register

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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    sealed interface Event {
        data object RegisterSuccess : Event
    }
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onNameInputChange(value: String) {
        _uiState.update { it.copy(nameInputValue = value, errorMessage = null) }
    }

    fun onEmailInputChange(value: String) {
        _uiState.update { it.copy(emailInputValue = value, errorMessage = null) }
    }

    fun onPasswordInputChange(value: String) {
        _uiState.update { it.copy(passwordInputValue = value, errorMessage = null) }
    }

    fun onConfirmPasswordInputChange(value: String) {
        _uiState.update { it.copy(confirmPasswordInputValue = value, errorMessage = null) }
    }

    fun onRegisterClick() {
        val state = _uiState.value
        if (state.nameInputValue.isBlank() || state.emailInputValue.isBlank() || state.passwordInputValue.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Preencha todos os campos") }
            return
        }
        if (state.passwordInputValue != state.confirmPasswordInputValue) {
            _uiState.update { it.copy(errorMessage = "As senhas não coincidem") }
            return
        }
        if (state.passwordInputValue.length < 6) {
            _uiState.update { it.copy(errorMessage = "A senha precisa ter no mínimo 6 caracteres") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                authRepository.register(
                    name = state.nameInputValue,
                    email = state.emailInputValue,
                    password = state.passwordInputValue
                )
                _uiState.update { it.copy(isLoading = false) }
                _events.send(Event.RegisterSuccess)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Erro ao cadastrar")
                }
            }
        }
    }
}