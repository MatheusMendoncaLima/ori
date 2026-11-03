package com.oriteam.ori.ui.app.screens.register

import com.oriteam.ori.R
import com.oriteam.ori.domain.auth.AuthError
import com.oriteam.ori.domain.auth.AuthResult
import com.oriteam.ori.domain.auth.RegisterAccount
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.testing.FakeAuthRepository
import com.oriteam.ori.testing.FakeProfileRepository
import com.oriteam.ori.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val profiles = FakeProfileRepository()

    private val viewModel by lazy { RegisterViewModel(RegisterAccount(repository, profiles)) }

    private fun fillForm(
        name: String = "Ana",
        email: String = "ana@ori.app",
        password: String = "Secret1",
        confirmation: String = password
    ) {
        viewModel.onNameInputChange(name)
        viewModel.onEmailInputChange(email)
        viewModel.onPasswordInputChange(password)
        viewModel.onConfirmPasswordInputChange(confirmation)
    }

    @Test
    fun `invalid form shows the validation message without calling the repository`() {
        fillForm(password = "Secret1", confirmation = "Secret2")

        viewModel.onRegisterClick()

        assertEquals(R.string.error_password_mismatch, viewModel.uiState.value.errorMessageRes)
        assertTrue(repository.createAccountCalls.isEmpty())
    }

    @Test
    fun `successful registration emits RegisterSuccess with trimmed name and email`() = runTest {
        fillForm(name = " Ana ", email = " ana@ori.app ")

        viewModel.onRegisterClick()

        assertEquals(RegisterViewModel.Event.RegisterSuccess, viewModel.events.first())
        assertEquals(listOf(Triple("Ana", "ana@ori.app", "Secret1")), repository.createAccountCalls)
    }

    @Test
    fun `registration saves the profile`() = runTest {
        fillForm(name = "Cara")

        viewModel.onRegisterClick()

        assertEquals(RegisterViewModel.Event.RegisterSuccess, viewModel.events.first())
        assertEquals(listOf("new-uid" to "Cara"), profiles.createCalls)
    }

    @Test
    fun `account created without profile still moves on so the session can ask for it`() = runTest {
        profiles.createResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        fillForm()

        viewModel.onRegisterClick()

        assertEquals(RegisterViewModel.Event.RegisterSuccess, viewModel.events.first())
    }

    @Test
    fun `failed registration shows the mapped message and stops loading`() {
        repository.nextResult = AuthResult.Failure(AuthError.EMAIL_ALREADY_IN_USE)
        fillForm()

        viewModel.onRegisterClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_email_already_in_use, state.errorMessageRes)
        assertFalse(state.isLoading)
    }
}
