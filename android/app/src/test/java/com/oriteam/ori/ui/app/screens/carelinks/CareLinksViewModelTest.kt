package com.oriteam.ori.ui.app.screens.carelinks

import com.oriteam.ori.R
import com.oriteam.ori.domain.carelink.CareLink
import com.oriteam.ori.domain.carelink.CareLinkPermissions
import com.oriteam.ori.domain.carelink.CareLinkSide
import com.oriteam.ori.domain.carelink.CareLinkStatus
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.testing.FakeCareLinkRepository
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.ui.components.state.ContentState
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

class CareLinksViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCareLinkRepository()
    private val link = CareLink(
        id = "cara_pat",
        caregiverUid = "cara",
        primaryUserUid = "pat",
        caregiverName = "Cara",
        primaryUserName = "Pat",
        status = CareLinkStatus.ACTIVE,
        permissions = CareLinkPermissions(viewRoutines = true, manageRoutines = false)
    )

    private fun TestScope.viewModel(side: CareLinkSide = CareLinkSide.PRIMARY_USER) =
        CareLinksViewModel(repository).also {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() }
            it.start("pat", side)
        }

    @Test
    fun `the links show, none is empty, and a refused read is an error that can be retried`() = runTest {
        val viewModel = viewModel()
        assertEquals(ContentState.Empty, viewModel.uiState.value.links)

        repository.links.value = DataResult.Success(listOf(link))
        assertEquals(ContentState.Content(listOf(link)), viewModel.uiState.value.links)

        repository.links.value = DataResult.Failure(DataError.PERMISSION_DENIED)
        assertEquals(ContentState.Error(R.string.error_permission_denied), viewModel.uiState.value.links)
    }

    @Test
    fun `ending a link waits for the confirmation`() = runTest {
        val viewModel = viewModel()

        viewModel.onRevokeRequest(link)
        assertEquals(link, viewModel.uiState.value.pendingRevocation)
        viewModel.onRevokeDismiss()
        assertTrue(repository.revokeCalls.isEmpty())

        viewModel.onRevokeRequest(link)
        viewModel.onRevokeConfirm()
        assertEquals(listOf("cara_pat"), repository.revokeCalls)
        assertNull(viewModel.uiState.value.pendingRevocation)
    }

    @Test
    fun `only the person who is supported changes the permissions`() = runTest {
        val primary = viewModel()
        primary.onManagePermissionChange(link, isAllowed = true)
        primary.onCommunicationPermissionChange(link, isAllowed = true)
        assertEquals(
            listOf(
                "cara_pat" to link.permissions.copy(manageRoutines = true),
                "cara_pat" to link.permissions.copy(manageCommunication = true)
            ),
            repository.permissionCalls
        )

        val caregiver = CareLinksViewModel(repository).apply { start("cara", CareLinkSide.CAREGIVER) }
        caregiver.onManagePermissionChange(link, isAllowed = false)
        assertEquals(2, repository.permissionCalls.size)
    }

    @Test
    fun `a failed change is explained, and one waiting for the connection says so`() = runTest {
        val viewModel = viewModel()

        repository.actionResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        viewModel.onManagePermissionChange(link, isAllowed = true)
        assertEquals(R.string.error_network_unavailable, viewModel.uiState.value.actionErrorRes)

        repository.actionResult = DataResult.Success(WriteStatus.PENDING_SYNC)
        viewModel.onManagePermissionChange(link, isAllowed = true)
        assertNull(viewModel.uiState.value.actionErrorRes)
        assertTrue(viewModel.uiState.value.hasPendingChange)
    }
}
