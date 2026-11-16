package com.oriteam.ori.ui.app.screens.carelinks

import com.oriteam.ori.R
import com.oriteam.ori.domain.carelink.CareInvitation
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.testing.FakeInvitationRepository
import com.oriteam.ori.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InvitationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeInvitationRepository()
    private val viewModel = InvitationViewModel(repository)

    @Test
    fun `a caregiver creates a code`() {
        viewModel.onCreateInvitationClick()

        assertEquals(CareInvitation("ABCD2345", 0L), viewModel.uiState.value.createdInvitation)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `no connection is not reported as a wrong code`() {
        repository.createResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        viewModel.onCreateInvitationClick()

        assertEquals(R.string.error_network_unavailable, viewModel.uiState.value.errorMessageRes)
        assertNull(viewModel.uiState.value.createdInvitation)
    }

    @Test
    fun `a code is cleaned before it is sent, and a short one never reaches the server`() {
        viewModel.onCodeInputChange("abc")
        viewModel.onAcceptInvitationClick()
        assertEquals(R.string.invitation_error_format, viewModel.uiState.value.errorMessageRes)
        assertTrue(repository.acceptCalls.isEmpty())
        // O and 0, I and 1 are not in the codes: a typing mistake is said at once.
        viewModel.onCodeInputChange("ABCD234O")
        viewModel.onAcceptInvitationClick()
        assertEquals(R.string.invitation_error_format, viewModel.uiState.value.errorMessageRes)
        assertTrue(repository.acceptCalls.isEmpty())

        viewModel.onCodeInputChange(" abcd 2345 ")
        viewModel.onAcceptInvitationClick()

        assertEquals(listOf("ABCD2345"), repository.acceptCalls)
        assertEquals("Cara", viewModel.uiState.value.linkedCaregiverName)
        assertEquals("", viewModel.uiState.value.codeInputValue)
    }

    @Test
    fun `each refusal of the server has its own message`() {
        mapOf(
            DataError.NOT_FOUND to R.string.invitation_error_not_found,
            DataError.PRECONDITION_FAILED to R.string.invitation_error_expired,
            DataError.CONFLICT to R.string.invitation_error_already_linked,
            DataError.PERMISSION_DENIED to R.string.invitation_error_not_allowed
        ).forEach { (error, message) ->
            repository.acceptResult = DataResult.Failure(error)
            viewModel.onCodeInputChange("ABCD2345")
            viewModel.onAcceptInvitationClick()
            assertEquals(error.name, message, viewModel.uiState.value.errorMessageRes)
        }
    }
}
