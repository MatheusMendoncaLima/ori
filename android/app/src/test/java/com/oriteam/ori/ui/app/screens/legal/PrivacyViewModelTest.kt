package com.oriteam.ori.ui.app.screens.legal

import com.oriteam.ori.R
import com.oriteam.ori.domain.account.AccountDeletionResult
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.testing.FakeAccountDataExporter
import com.oriteam.ori.testing.FakeAccountDeletion
import com.oriteam.ori.testing.FakeUserFileWriter
import com.oriteam.ori.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrivacyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exporter = FakeAccountDataExporter(DataResult.Success("""{"app":"ORI"}"""))
    private val deletion = FakeAccountDeletion()
    private val files = FakeUserFileWriter()
    private val viewModel = PrivacyViewModel(exporter, deletion, files)

    @Test
    fun `the copy goes to the chosen file, and closing the save as does nothing`() {
        viewModel.onExportTo("ana", null)
        assertTrue(exporter.exported.isEmpty())

        viewModel.onExportTo("ana", "content://file")

        assertEquals("""{"app":"ORI"}""", files.written["content://file"])
        assertEquals(R.string.privacy_export_done, viewModel.uiState.value.successMessageRes)
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun `a copy that cannot be read or written is explained, never reported as saved`() {
        files.succeeds = false
        viewModel.onExportTo("ana", "content://file")
        assertEquals(R.string.privacy_export_failed, viewModel.uiState.value.errorMessageRes)
        assertNull(viewModel.uiState.value.successMessageRes)

        exporter.result = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        viewModel.onExportTo("ana", "content://file")
        assertEquals(R.string.error_network_unavailable, viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `the account is deleted only after the confirmation`() {
        viewModel.onDeleteRequest()
        assertTrue(viewModel.uiState.value.isConfirmingDeletion)
        viewModel.onDeleteDismiss()
        assertTrue(deletion.deleted.isEmpty())

        viewModel.onDeleteRequest()
        viewModel.onDeleteConfirm("ana")

        assertEquals(listOf("ana"), deletion.deleted)
        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun `an old sign in and a failure are explained`() {
        deletion.result = AccountDeletionResult.NeedsRecentSignIn
        viewModel.onDeleteConfirm("ana")
        assertEquals(R.string.privacy_delete_sign_in_again, viewModel.uiState.value.errorMessageRes)

        deletion.result = AccountDeletionResult.Failed(DataError.NETWORK_UNAVAILABLE)
        viewModel.onDeleteConfirm("ana")
        assertEquals(R.string.error_network_unavailable, viewModel.uiState.value.errorMessageRes)
        assertFalse(viewModel.uiState.value.isDeleting)
    }
}
