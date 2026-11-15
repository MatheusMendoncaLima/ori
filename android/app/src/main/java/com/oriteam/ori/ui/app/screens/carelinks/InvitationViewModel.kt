package com.oriteam.ori.ui.app.screens.carelinks

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.carelink.CareInvitation
import com.oriteam.ori.domain.carelink.InvitationRepository
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.ui.app.screens.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvitationUiState(
    val createdInvitation: CareInvitation? = null,
    val codeInputValue: String = "",
    val isLoading: Boolean = false,
    /** Name of the caregiver after a successful link. */
    val linkedCaregiverName: String? = null,
    @StringRes val errorMessageRes: Int? = null
)

/** Invitation texts are more specific than the generic data errors. */
@StringRes
internal fun DataError.invitationMessageRes(): Int = when (this) {
    DataError.INVALID_INPUT -> R.string.invitation_error_format
    DataError.NOT_FOUND -> R.string.invitation_error_not_found
    DataError.PRECONDITION_FAILED -> R.string.invitation_error_expired
    DataError.CONFLICT -> R.string.invitation_error_already_linked
    DataError.PERMISSION_DENIED -> R.string.invitation_error_not_allowed
    DataError.NETWORK_UNAVAILABLE, DataError.SERVICE_UNAVAILABLE, DataError.UNKNOWN -> messageRes()
}

/** Caregiver creates a code; primary user types it. Both operations run on the server. */
@HiltViewModel
class InvitationViewModel @Inject constructor(
    private val invitationRepository: InvitationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvitationUiState())
    val uiState: StateFlow<InvitationUiState> = _uiState.asStateFlow()

    fun onCodeInputChange(value: String) {
        _uiState.update { it.copy(codeInputValue = value, errorMessageRes = null, linkedCaregiverName = null) }
    }

    fun onCreateInvitationClick() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
            when (val result = invitationRepository.createInvitation()) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        createdInvitation = result.value
                    )
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessageRes = result.error.invitationMessageRes())
                }
            }
        }
    }

    fun onAcceptInvitationClick() {
        val state = _uiState.value
        if (state.isLoading) return
        val code = state.codeInputValue.filterNot(Char::isWhitespace).uppercase()
        if (code.length != INVITATION_CODE_LENGTH) {
            _uiState.update { it.copy(errorMessageRes = R.string.invitation_error_format) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
            when (val result = invitationRepository.acceptInvitation(code)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isLoading = false, codeInputValue = "", linkedCaregiverName = result.value)
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessageRes = result.error.invitationMessageRes())
                }
            }
        }
    }

    private companion object {
        /** Same length the server expects (functions/functions/src/careInvitations.ts). */
        const val INVITATION_CODE_LENGTH = 8
    }
}
