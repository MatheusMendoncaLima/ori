package com.oriteam.ori.ui.app.screens.legal

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.R
import com.oriteam.ori.domain.account.AccountDataExporter
import com.oriteam.ori.domain.account.AccountDeletion
import com.oriteam.ori.domain.account.AccountDeletionResult
import com.oriteam.ori.domain.account.UserFileWriter
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.ui.app.screens.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyUiState(
    val isExporting: Boolean = false,
    val isDeleting: Boolean = false,
    val isConfirmingDeletion: Boolean = false,
    @StringRes val successMessageRes: Int? = null,
    @StringRes val errorMessageRes: Int? = null
)

/**
 * The rights of the person over their data (LGPD art. 18, J5): a copy of everything, and deleting the account. When
 * the deletion works, the account signs out and the app goes back to the welcome by itself.
 */
@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val exporter: AccountDataExporter,
    private val deletion: AccountDeletion,
    private val files: UserFileWriter
) : ViewModel() {
    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    /** [fileUri] is the file the person chose; null when the "save as" was closed. */
    fun onExportTo(ownerUid: String, fileUri: String?) {
        if (fileUri == null || _uiState.value.isExporting) return
        _uiState.update { it.copy(isExporting = true, successMessageRes = null, errorMessageRes = null) }
        viewModelScope.launch {
            val message = when (val result = exporter.export(ownerUid)) {
                is DataResult.Failure -> result.error.messageRes()
                is DataResult.Success -> if (files.write(
                        fileUri,
                        result.value
                    )
                ) {
                    null
                } else {
                    R.string.privacy_export_failed
                }
            }
            _uiState.update {
                it.copy(
                    isExporting = false,
                    successMessageRes = R.string.privacy_export_done.takeIf { message == null },
                    errorMessageRes = message
                )
            }
        }
    }

    fun onDeleteRequest() = _uiState.update { it.copy(isConfirmingDeletion = true, errorMessageRes = null) }

    fun onDeleteDismiss() = _uiState.update { it.copy(isConfirmingDeletion = false) }

    fun onDeleteConfirm(ownerUid: String) {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isConfirmingDeletion = false, isDeleting = true, errorMessageRes = null) }
        viewModelScope.launch {
            val message = when (val result = deletion.deleteAccount(ownerUid)) {
                AccountDeletionResult.Deleted -> null
                AccountDeletionResult.NeedsRecentSignIn -> R.string.privacy_delete_sign_in_again
                is AccountDeletionResult.Failed -> result.error.messageRes()
            }
            _uiState.update { it.copy(isDeleting = false, errorMessageRes = message) }
        }
    }
}
