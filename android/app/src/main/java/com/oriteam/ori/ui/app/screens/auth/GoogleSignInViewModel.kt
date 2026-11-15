package com.oriteam.ori.ui.app.screens.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.auth.AuthRepository
import com.oriteam.ori.domain.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoogleSignInUiState(val isLoading: Boolean = false, @StringRes val errorMessageRes: Int? = null)

/**
 * Signs in with the Google account Android gave ([GoogleCredentialResult]). Success needs nothing here: the session
 * changes and the signed-in area replaces the screen. Closing the account sheet is not an error.
 */
@HiltViewModel
class GoogleSignInViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GoogleSignInUiState())
    val uiState: StateFlow<GoogleSignInUiState> = _uiState.asStateFlow()

    /** The system sheet is open: a second tap meanwhile does nothing. */
    fun onRequestStarted() = _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }

    fun onCredential(result: GoogleCredentialResult) {
        when (result) {
            GoogleCredentialResult.Cancelled -> _uiState.update { it.copy(isLoading = false) }
            is GoogleCredentialResult.Failed ->
                _uiState.update { it.copy(isLoading = false, errorMessageRes = result.messageRes) }
            is GoogleCredentialResult.Token -> viewModelScope.launch {
                val outcome = authRepository.signInWithGoogle(result.idToken)
                _uiState.update {
                    when (outcome) {
                        AuthResult.Success -> it.copy(isLoading = false)
                        is AuthResult.Failure -> it.copy(
                            isLoading = false,
                            errorMessageRes = outcome.error.messageRes()
                        )
                    }
                }
            }
        }
    }
}
