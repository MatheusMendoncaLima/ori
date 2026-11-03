package com.oriteam.ori.ui.app.screens

import com.oriteam.ori.R
import com.oriteam.ori.domain.auth.AuthenticatedUser
import com.oriteam.ori.domain.carelink.CareLink
import com.oriteam.ori.domain.carelink.CareLinkPermissions
import com.oriteam.ori.domain.carelink.CareLinkSide
import com.oriteam.ori.domain.carelink.CareLinkStatus
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.testing.FakeCareLinkRepository
import com.oriteam.ori.testing.FakeInvitationRepository
import com.oriteam.ori.testing.FakeProfileRepository
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.ui.app.screens.carelinks.CareLinksViewModel
import com.oriteam.ori.ui.app.screens.carelinks.InvitationViewModel
import com.oriteam.ori.ui.app.screens.profile.ProfileSetupViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignedInViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewOnly = CareLinkPermissions(viewRoutines = true, manageRoutines = false)
    private val link = CareLink("cara_pat", "cara", "pat", "Cara", "Pat", CareLinkStatus.ACTIVE, viewOnly)

    /** The ViewModels only listen while observed (WhileSubscribed), like a visible screen. */
    private fun TestScope.observe(state: StateFlow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect() }
    }

    @Test
    fun `revoking a link needs confirmation`() = runTest {
        val links = FakeCareLinkRepository()
        val viewModel = CareLinksViewModel(links).also { observe(it.uiState) }
        viewModel.start("pat", CareLinkSide.PRIMARY_USER)

        viewModel.onRevokeRequest(link)
        assertTrue(links.revokeCalls.isEmpty())
        viewModel.onRevokeConfirm()

        assertEquals(listOf("cara_pat"), links.revokeCalls)
        assertNull(viewModel.uiState.value.pendingRevocation)
    }

    @Test
    fun `a link change saved offline is reported as pending`() = runTest {
        val links = FakeCareLinkRepository().apply { actionResult = DataResult.Success(WriteStatus.PENDING_SYNC) }
        val viewModel = CareLinksViewModel(links).also { observe(it.uiState) }
        viewModel.start("pat", CareLinkSide.PRIMARY_USER)

        viewModel.onManagePermissionChange(link, isAllowed = true)

        assertTrue(viewModel.uiState.value.hasPendingChange)
    }

    @Test
    fun `only the primary user changes link permissions`() {
        val links = FakeCareLinkRepository()
        val caregiverView = CareLinksViewModel(links).apply { start("cara", CareLinkSide.CAREGIVER) }
        caregiverView.onManagePermissionChange(link, isAllowed = true)
        assertTrue(links.permissionCalls.isEmpty())

        val primaryView = CareLinksViewModel(links).apply { start("pat", CareLinkSide.PRIMARY_USER) }
        primaryView.onManagePermissionChange(link, isAllowed = true)

        assertEquals(listOf("cara_pat" to viewOnly.copy(manageRoutines = true)), links.permissionCalls)
    }

    @Test
    fun `malformed invitation codes are rejected before calling the server`() {
        val invitations = FakeInvitationRepository()
        val viewModel = InvitationViewModel(invitations)

        viewModel.onCodeInputChange("abc")
        viewModel.onAcceptInvitationClick()

        assertEquals(R.string.invitation_error_format, viewModel.uiState.value.errorMessageRes)
        assertTrue(invitations.acceptCalls.isEmpty())
    }

    @Test
    fun `server invitation errors get specific messages`() {
        val invitations = FakeInvitationRepository().apply {
            acceptResult = DataResult.Failure(DataError.PRECONDITION_FAILED)
        }
        val viewModel = InvitationViewModel(invitations)

        viewModel.onCodeInputChange("abcd 2345")
        viewModel.onAcceptInvitationClick()

        assertEquals(listOf("ABCD2345"), invitations.acceptCalls)
        assertEquals(R.string.invitation_error_expired, viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `accepted invitation shows the caregiver name`() {
        val viewModel = InvitationViewModel(FakeInvitationRepository())

        viewModel.onCodeInputChange("ABCD2345")
        viewModel.onAcceptInvitationClick()

        assertEquals("Cara", viewModel.uiState.value.linkedCaregiverName)
    }

    @Test
    fun `profile setup requires a name and saves it for the signed-in uid`() {
        val profiles = FakeProfileRepository()
        val viewModel = ProfileSetupViewModel(profiles)
        viewModel.start(AuthenticatedUser(uid = "nobody", displayName = null))

        viewModel.onSaveClick()
        assertEquals(R.string.error_missing_profile_name, viewModel.uiState.value.errorMessageRes)

        viewModel.onNameInputChange(" Ana ")
        viewModel.onSaveClick()

        assertEquals(listOf("nobody" to "Ana"), profiles.createCalls)
    }
}
