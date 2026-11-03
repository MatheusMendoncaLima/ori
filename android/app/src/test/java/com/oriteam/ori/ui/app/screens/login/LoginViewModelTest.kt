package com.oriteam.ori.ui.app.screens.login

import com.oriteam.ori.R
import com.oriteam.ori.domain.auth.AuthError
import com.oriteam.ori.domain.auth.AuthResult
import com.oriteam.ori.testing.FakeAuthRepository
import com.oriteam.ori.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val viewModel by lazy { LoginViewModel(repository) }

    private fun fillCredentials(email: String = "ana@ori.app", password: String = "Secret1") {
        viewModel.onEmailInputChange(email)
        viewModel.onPasswordInputChange(password)
    }

    @Test
    fun `blank fields show a validation message without calling the repository`() {
        viewModel.onLoginClick()

        assertEquals(R.string.error_missing_sign_in_fields, viewModel.uiState.value.errorMessageRes)
        assertTrue(repository.signInCalls.isEmpty())
    }

    @Test
    fun `successful sign in emits LoginSuccess with trimmed email and untouched password`() = runTest {
        fillCredentials(email = "  ana@ori.app ", password = "Secret1")

        viewModel.onLoginClick()

        assertEquals(LoginViewModel.Event.LoginSuccess, viewModel.events.first())
        assertEquals(listOf("ana@ori.app" to "Secret1"), repository.signInCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `the password reaches the repository exactly as typed`() = runTest {
        fillCredentials(password = "Secret1")

        viewModel.onLoginClick()

        assertEquals("Secret1", repository.signInCalls.single().second)
    }

    @Test
    fun `failed sign in shows the mapped message and stops loading`() {
        repository.nextResult = AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        fillCredentials()

        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_invalid_credentials, state.errorMessageRes)
        assertFalse(state.isLoading)
    }

    @Test
    fun `editing a field clears the previous error`() {
        viewModel.onLoginClick()

        viewModel.onEmailInputChange("a")

        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `password reset requires an email`() {
        viewModel.onForgotPasswordClick()

        assertEquals(R.string.error_missing_reset_email, viewModel.uiState.value.errorMessageRes)
        assertTrue(repository.passwordResetCalls.isEmpty())
    }

    @Test
    fun `password reset success shows a neutral confirmation`() {
        viewModel.onEmailInputChange(" ana@ori.app ")

        viewModel.onForgotPasswordClick()

        assertEquals(listOf("ana@ori.app"), repository.passwordResetCalls)
        assertEquals(R.string.password_reset_requested, viewModel.uiState.value.successMessageRes)
    }

    @Test
    fun `password reset failure is reported as an error, never as success`() {
        repository.nextResult = AuthResult.Failure(AuthError.NETWORK_UNAVAILABLE)
        viewModel.onEmailInputChange("ana@ori.app")

        viewModel.onForgotPasswordClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_network_unavailable, state.errorMessageRes)
        assertNull(state.successMessageRes)
    }
}
