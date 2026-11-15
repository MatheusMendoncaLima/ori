package com.oriteam.ori.ui.app.screens.legal

import com.oriteam.ori.R
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.legal.LEGAL_DOCUMENTS_VERSION
import com.oriteam.ori.domain.legal.LegalConsent
import com.oriteam.ori.testing.FakeConsentRepository
import com.oriteam.ori.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ConsentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC)
    private val consents = FakeConsentRepository()

    private fun viewModel() = ConsentViewModel(consents, clock).apply { start("ana") }

    @Test
    fun `without a consent the screen asks, and an old version asks again`() {
        assertEquals(ConsentStatus.REQUIRED, viewModel().uiState.value.status)

        consents.stored = mapOf("ana" to LegalConsent(LEGAL_DOCUMENTS_VERSION - 1, true, false, 0))
        assertEquals(ConsentStatus.REQUIRED, viewModel().uiState.value.status)

        consents.stored = mapOf("ana" to LegalConsent(LEGAL_DOCUMENTS_VERSION, true, false, 0))
        assertEquals(ConsentStatus.ACCEPTED, viewModel().uiState.value.status)
    }

    @Test
    fun `an adult accepts only after the terms and the sensitive data, and it is recorded`() {
        val viewModel = viewModel()
        viewModel.onAgeChange(isAdult = true)
        viewModel.onTermsChange(true)
        assertFalse(viewModel.uiState.value.canAccept)
        viewModel.onSensitiveDataChange(true)
        assertTrue(viewModel.uiState.value.canAccept)

        viewModel.onAccept()

        assertEquals(ConsentStatus.ACCEPTED, viewModel.uiState.value.status)
        assertEquals(LegalConsent(LEGAL_DOCUMENTS_VERSION, true, false, 1_000), consents.stored["ana"])
    }

    @Test
    fun `under 18 needs the guardian, and changing the age clears that answer`() {
        val viewModel = viewModel()
        viewModel.onAgeChange(isAdult = false)
        viewModel.onTermsChange(true)
        viewModel.onSensitiveDataChange(true)
        viewModel.onAccept()
        assertTrue("nothing recorded without the guardian", consents.stored.isEmpty())

        viewModel.onGuardianConsentChange(true)
        assertTrue(viewModel.uiState.value.canAccept)
        viewModel.onAgeChange(isAdult = true)
        viewModel.onAgeChange(isAdult = false)
        assertFalse(viewModel.uiState.value.hasGuardianConsent)
    }

    @Test
    fun `a failure to record keeps the screen and explains it, and a failed read can be retried`() {
        consents.writeResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        val viewModel = viewModel()
        viewModel.onAgeChange(isAdult = true)
        viewModel.onTermsChange(true)
        viewModel.onSensitiveDataChange(true)
        viewModel.onAccept()
        assertEquals(ConsentStatus.REQUIRED, viewModel.uiState.value.status)
        assertEquals(R.string.error_network_unavailable, viewModel.uiState.value.errorMessageRes)

        consents.readResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        val unreadable = viewModel()
        assertEquals(ConsentStatus.UNAVAILABLE, unreadable.uiState.value.status)
        consents.readResult = null
        unreadable.onRetry()
        assertEquals(ConsentStatus.REQUIRED, unreadable.uiState.value.status)
    }
}
