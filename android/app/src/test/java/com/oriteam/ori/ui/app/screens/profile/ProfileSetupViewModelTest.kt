package com.oriteam.ori.ui.app.screens.profile

import com.oriteam.ori.R
import com.oriteam.ori.domain.auth.AuthenticatedUser
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.testing.FakeProfileRepository
import com.oriteam.ori.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileSetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profiles = FakeProfileRepository()
    private val viewModel = ProfileSetupViewModel(profiles)

    @Test
    fun `the name of the account comes filled in, and the trimmed name is saved`() {
        viewModel.start(AuthenticatedUser(uid = "ana-uid", displayName = "Ana Lima"))
        assertEquals("Ana Lima", viewModel.uiState.value.nameInputValue)

        viewModel.onNameInputChange("  Ana  ")
        viewModel.onSaveClick()

        assertEquals(listOf("ana-uid" to "Ana"), profiles.createCalls)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `a blank name is refused before writing`() {
        viewModel.start(AuthenticatedUser(uid = "ana-uid", displayName = null))
        viewModel.onSaveClick()

        assertEquals(R.string.error_missing_profile_name, viewModel.uiState.value.errorMessageRes)
        assertTrue(profiles.createCalls.isEmpty())
    }

    @Test
    fun `a failure keeps the name and explains it, and typing clears the message`() {
        profiles.createResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        viewModel.start(AuthenticatedUser(uid = "ana-uid", displayName = "Ana"))
        viewModel.onSaveClick()

        assertEquals(R.string.error_network_unavailable, viewModel.uiState.value.errorMessageRes)
        assertEquals("Ana", viewModel.uiState.value.nameInputValue)
        viewModel.onNameInputChange("Ana L")
        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `nothing is saved before the account is known`() {
        viewModel.onNameInputChange("Ana")
        viewModel.onSaveClick()

        assertTrue(profiles.createCalls.isEmpty())
    }
}
