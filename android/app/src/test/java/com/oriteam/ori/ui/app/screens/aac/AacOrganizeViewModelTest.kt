package com.oriteam.ori.ui.app.screens.aac

import androidx.lifecycle.SavedStateHandle
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.AacViewer
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.testing.AacWrite
import com.oriteam.ori.testing.FakeAacBoardRepository
import com.oriteam.ori.testing.FakePhotoStore
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.OWNER
import com.oriteam.ori.testing.board
import com.oriteam.ori.testing.communicationManager
import com.oriteam.ori.testing.listSnapshotOf
import com.oriteam.ori.testing.routineOnlyCaregiver
import com.oriteam.ori.testing.symbol
import com.oriteam.ori.ui.components.state.ContentState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AacOrganizeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val photo = SymbolImage.DevicePhoto("mom.jpg")
    private val boards = FakeAacBoardRepository().apply {
        boards.value = listSnapshotOf(board("b1", "Básico", 0), board("b2", "Comida", 1), board("b3", "Lugares", 2))
        symbols.value = listSnapshotOf(
            symbol("s1", "Água", position = 0),
            symbol("s2", "Mãe", image = photo, position = 1)
        )
        symbolsResult = DataResult.Success(listOf(symbol("s2", "Mãe", image = photo)))
    }
    private val photos = FakePhotoStore()

    private fun TestScope.organizer(
        viewer: AacViewer = AacViewer.Owner,
        savedState: SavedStateHandle = SavedStateHandle()
    ) = AacOrganizeViewModel(boards, photos, savedState).also {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() }
        it.show(OWNER, viewer)
    }

    private fun AacOrganizeViewModel.boardRows() = (uiState.value.boards as ContentState.Content).value

    @Test
    fun `boards are listed in order with the moves each one allows`() = runTest {
        val rows = organizer().boardRows()

        assertEquals(listOf("Básico", "Comida", "Lugares"), rows.map { it.item.name })
        assertFalse(rows.first().canMoveEarlier)
        assertTrue(rows.first().canMoveLater)
        assertFalse(rows.last().canMoveLater)
    }

    @Test
    fun `moving a board writes only the positions that change`() = runTest {
        val viewModel = organizer()

        viewModel.onMoveBoard("b3", -1)

        assertEquals(listOf(AacWrite("reorderBoards", OWNER, mapOf("b3" to 1, "b2" to 2))), boards.writes)
    }

    @Test
    fun `moving a symbol past the edge does nothing`() = runTest {
        val viewModel = organizer()
        viewModel.onOpenBoard("b1")

        viewModel.onMoveSymbol("s1", -1)
        assertTrue(boards.writes.isEmpty())
        viewModel.onMoveSymbol("s1", 1)
        assertEquals(mapOf("s2" to 0, "s1" to 1), boards.writes.single().detail)
    }

    @Test
    fun `a new board name is validated before any write`() = runTest {
        val viewModel = organizer()

        viewModel.onCreateBoardRequest()
        viewModel.onBoardNameChange(" básico ")
        viewModel.onBoardDialogConfirm()

        assertEquals(R.string.aac_error_duplicate_board, viewModel.uiState.value.boardDialog?.errorRes)
        assertTrue(boards.writes.isEmpty())

        viewModel.onBoardNameChange("Sentimentos")
        viewModel.onBoardDialogConfirm()
        assertEquals(listOf(AacWrite("createBoard", "Sentimentos", 3)), boards.writes)
        assertNull(viewModel.uiState.value.boardDialog)
    }

    @Test
    fun `a failed rename keeps the dialog open with the reason`() = runTest {
        boards.writeResult = DataResult.Failure(DataError.PERMISSION_DENIED)
        val viewModel = organizer()

        viewModel.onRenameBoardRequest("b1")
        viewModel.onBoardNameChange("Início")
        viewModel.onBoardDialogConfirm()

        val dialog = viewModel.uiState.value.boardDialog
        assertEquals(R.string.error_permission_denied, dialog?.errorRes)
        assertFalse(dialog!!.isSaving)
    }

    @Test
    fun `deleting a board asks first and then removes its photos from this device`() = runTest {
        val viewModel = organizer()

        viewModel.onDeleteBoardRequest("b1")
        assertTrue(boards.writes.isEmpty())
        viewModel.onDeleteConfirm()

        assertEquals(listOf(AacWrite("deleteBoard", "b1")), boards.writes)
        assertEquals(listOf(photo.photo), photos.deleted)
    }

    @Test
    fun `a failed delete keeps the photos and shows the reason`() = runTest {
        boards.writeResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        val viewModel = organizer()
        viewModel.onOpenBoard("b1")

        viewModel.onDeleteSymbolRequest("s2")
        viewModel.onDeleteConfirm()

        assertTrue(photos.deleted.isEmpty())
        assertEquals(R.string.error_network_unavailable, viewModel.uiState.value.actionErrorRes)
    }

    @Test
    fun `a caregiver with permission organizes but never deletes photos on their device`() = runTest {
        val viewModel = organizer(viewer = communicationManager)
        viewModel.onOpenBoard("b1")

        viewModel.onDeleteSymbolRequest("s2")
        viewModel.onDeleteConfirm()

        assertEquals(listOf(AacWrite("deleteSymbol", "s2")), boards.writes)
        assertTrue(photos.deleted.isEmpty())
        assertTrue(viewModel.uiState.value.canManage)
        assertFalse(viewModel.uiState.value.canUsePhotos)
    }

    @Test
    fun `a caregiver without the permission cannot change anything`() = runTest {
        val viewModel = organizer(viewer = routineOnlyCaregiver)
        viewModel.onOpenBoard("b1")

        viewModel.onMoveBoard("b3", -1)
        viewModel.onDeleteBoardRequest("b1")
        viewModel.onDeleteConfirm()
        viewModel.onCreateBoardRequest()
        viewModel.onBoardNameChange("Nova")
        viewModel.onBoardDialogConfirm()

        assertTrue(boards.writes.isEmpty())
        assertFalse(viewModel.uiState.value.canManage)
    }

    @Test
    fun `the open board survives an app restart`() = runTest {
        val savedState = SavedStateHandle()
        organizer(savedState = savedState).onOpenBoard("b2")

        val restored = organizer(savedState = savedState)

        assertEquals("b2", restored.uiState.value.selectedBoardId)
        assertEquals("Comida", restored.uiState.value.selectedBoardName)
    }
}
