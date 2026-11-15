package com.oriteam.ori.ui.app.screens.legal

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.legal.ConsentRepository
import com.oriteam.ori.domain.legal.LEGAL_DOCUMENTS_VERSION
import com.oriteam.ori.domain.legal.LegalConsent
import com.oriteam.ori.domain.legal.isCurrent
import com.oriteam.ori.ui.app.screens.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** Where the consent stands: being read, missing (the screen asks), given, or not readable (retry). */
enum class ConsentStatus { CHECKING, REQUIRED, ACCEPTED, UNAVAILABLE }

data class ConsentUiState(
    val status: ConsentStatus = ConsentStatus.CHECKING,
    /** Null until the person answers. */
    val isAdult: Boolean? = null,
    val hasGuardianConsent: Boolean = false,
    val acceptsTerms: Boolean = false,
    val acceptsSensitiveData: Boolean = false,
    val isSaving: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
) {
    /** Every answer the law asks for is there: age, a guardian under 18, the terms and the sensitive data. */
    val canAccept: Boolean
        get() = isAdult != null && (isAdult || hasGuardianConsent) && acceptsTerms && acceptsSensitiveData && !isSaving
}

/**
 * The consent asked on the first use (J2, J3) and again when the documents change ([LEGAL_DOCUMENTS_VERSION]).
 * Nothing of the account is written before it is given; the app goes on only after it is recorded.
 */
@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val consents: ConsentRepository,
    private val clock: Clock
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState: StateFlow<ConsentUiState> = _uiState.asStateFlow()

    private var ownerUid: String? = null

    fun start(ownerUid: String) {
        if (this.ownerUid == ownerUid) return
        this.ownerUid = ownerUid
        check()
    }

    fun onRetry() = check()

    fun onAgeChange(isAdult: Boolean) =
        _uiState.update { it.copy(isAdult = isAdult, hasGuardianConsent = false, errorMessageRes = null) }

    fun onGuardianConsentChange(value: Boolean) = _uiState.update { it.copy(hasGuardianConsent = value) }

    fun onTermsChange(value: Boolean) = _uiState.update { it.copy(acceptsTerms = value) }

    fun onSensitiveDataChange(value: Boolean) = _uiState.update { it.copy(acceptsSensitiveData = value) }

    fun onAccept() {
        val owner = ownerUid ?: return
        val state = _uiState.value
        val isAdult = state.isAdult
        if (!state.canAccept || isAdult == null) return
        _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
        val consent = LegalConsent(LEGAL_DOCUMENTS_VERSION, isAdult, state.hasGuardianConsent, clock.millis())
        viewModelScope.launch {
            when (val result = consents.recordConsent(owner, consent)) {
                // Waiting for the connection is fine: the device copy is kept, and the account gets it later.
                is DataResult.Success -> _uiState.update { it.copy(isSaving = false, status = ConsentStatus.ACCEPTED) }
                is DataResult.Failure ->
                    _uiState.update { it.copy(isSaving = false, errorMessageRes = result.error.messageRes()) }
            }
        }
    }

    private fun check() {
        val owner = ownerUid ?: return
        _uiState.update { it.copy(status = ConsentStatus.CHECKING, errorMessageRes = null) }
        viewModelScope.launch {
            val status = when (val result = consents.getConsent(owner)) {
                is DataResult.Success -> if (result.value.isCurrent) ConsentStatus.ACCEPTED else ConsentStatus.REQUIRED
                is DataResult.Failure -> {
                    _uiState.update { it.copy(errorMessageRes = result.error.messageRes()) }
                    ConsentStatus.UNAVAILABLE
                }
            }
            _uiState.update { it.copy(status = status) }
        }
    }
}
