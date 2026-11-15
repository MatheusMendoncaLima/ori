package com.oriteam.ori.ui.app.screens.auth

import com.oriteam.ori.R
import com.oriteam.ori.domain.auth.AuthError
import com.oriteam.ori.domain.auth.AuthResult
import com.oriteam.ori.testing.FakeAuthRepository
import com.oriteam.ori.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GoogleSignInViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val viewModel = GoogleSignInViewModel(auth)

    @Test
    fun `a Google account signs in and the session changes`() {
        viewModel.onRequestStarted()
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.onCredential(GoogleCredentialResult.Token("token"))

        assertEquals(listOf("token"), auth.googleTokens)
        assertEquals("google-uid", auth.currentUser()?.uid)
        assertEquals(GoogleSignInUiState(), viewModel.uiState.value)
    }

    @Test
    fun `closing the account sheet is not an error`() {
        viewModel.onRequestStarted()
        viewModel.onCredential(GoogleCredentialResult.Cancelled)

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessageRes)
        assertTrue(auth.googleTokens.isEmpty())
    }

    @Test
    fun `no account on the device and a refused sign in are explained`() {
        viewModel.onRequestStarted()
        viewModel.onCredential(GoogleCredentialResult.Failed(R.string.auth_google_no_account))
        assertEquals(R.string.auth_google_no_account, viewModel.uiState.value.errorMessageRes)

        auth.nextResult = AuthResult.Failure(AuthError.NETWORK_UNAVAILABLE)
        viewModel.onRequestStarted()
        assertNull("a new try clears the old message", viewModel.uiState.value.errorMessageRes)
        viewModel.onCredential(GoogleCredentialResult.Token("token"))

        assertEquals(R.string.error_network_unavailable, viewModel.uiState.value.errorMessageRes)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(auth.currentUser())
    }
}
