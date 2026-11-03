package com.oriteam.ori.ui.app.screens.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.auth.AuthenticatedUser
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.profile.ProfileRepository
import com.oriteam.ori.ui.app.screens.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSetupUiState(
    val nameInputValue: String = "",
    val isSaving: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

/**
 * Creates the missing profile of a signed-in account. There is no success event: once the
 * profile exists, the session becomes active and the app leaves this screen by itself.
 */
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private var user: AuthenticatedUser? = null

    fun start(user: AuthenticatedUser) {
        if (this.user?.uid == user.uid) return
        this.user = user
        _uiState.update { it.copy(nameInputValue = user.displayName.orEmpty()) }
    }

    fun onNameInputChange(value: String) {
        _uiState.update { it.copy(nameInputValue = value, errorMessageRes = null) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        val uid = user?.uid
        val name = state.nameInputValue.trim()
        when {
            uid == null || state.isSaving -> Unit
            name.isBlank() -> _uiState.update { it.copy(errorMessageRes = R.string.error_missing_profile_name) }
            else -> save(uid, name)
        }
    }

    private fun save(uid: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
            val result = profileRepository.createProfile(uid, name)
            _uiState.update {
                it.copy(isSaving = false, errorMessageRes = (result as? DataResult.Failure)?.error?.messageRes())
            }
        }
    }
}
