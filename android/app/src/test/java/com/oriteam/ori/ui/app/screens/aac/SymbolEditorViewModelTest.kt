package com.oriteam.ori.ui.app.screens.aac

import androidx.lifecycle.SavedStateHandle
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.AacViewer
import com.oriteam.ori.domain.aac.SymbolColor
import com.oriteam.ori.domain.aac.SymbolDraft
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.photos.PhotoImportError
import com.oriteam.ori.domain.photos.PhotoImportResult
import com.oriteam.ori.domain.photos.StoredPhoto
import com.oriteam.ori.testing.FakeAacBoardRepository
import com.oriteam.ori.testing.FakePhotoStore
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.OWNER
import com.oriteam.ori.testing.communicationManager
import com.oriteam.ori.testing.symbol
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val FIRST_OPENING = "opening-1"

class SymbolEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val boards = FakeAacBoardRepository().apply {
        symbolsResult = DataResult.Success(
            listOf(symbol("s1", "Água", position = 0), symbol("s2", "Comer", position = 4))
        )
    }
    private val photos = FakePhotoStore()

    private fun editor(
        savedState: SavedStateHandle = SavedStateHandle(),
        viewer: AacViewer = AacViewer.Owner,
        existing: com.oriteam.ori.domain.aac.AacSymbol? = null
    ) = SymbolEditorViewModel(boards, photos, savedState).apply { start(FIRST_OPENING, OWNER, viewer, "b1", existing) }

    @Test
    fun `a new symbol is saved at the end of the board with its phrase and emoji`() = runTest {
        val viewModel = editor()

        viewModel.onLabelChange(" Banheiro ")
        viewModel.onPhraseChange("Preciso ir ao banheiro")
        viewModel.onEmojiChange("🚻")
        viewModel.onSaveClick()

        val write = boards.writes.single()
        assertEquals("createSymbol", write.operation)
        assertEquals(SymbolDraft("Banheiro", "Preciso ir ao banheiro", SymbolImage.Emoji("🚻")) to 5, write.detail)
    }

    @Test
    fun `the chosen tile color is saved, and survives a restart of the form`() = runTest {
        val savedState = SavedStateHandle()
        editor(savedState).apply {
            onLabelChange("Banheiro")
            onColorChange(SymbolColor.PINK)
        }

        val restored = editor(savedState)
        assertEquals(SymbolColor.PINK, restored.uiState.value.color)
        restored.onColorChange(null)
        restored.onSaveClick()

        assertEquals(SymbolDraft("Banheiro", null, null, color = null) to 5, boards.writes.single().detail)
    }

    @Test
    fun `duplicates and missing labels are refused before writing`() = runTest {
        val viewModel = editor()

        viewModel.onSaveClick()
        assertEquals(R.string.aac_error_missing_label, viewModel.uiState.value.errorMessageRes)
        viewModel.onLabelChange("água")
        viewModel.onSaveClick()
        assertEquals(R.string.aac_error_duplicate_label, viewModel.uiState.value.errorMessageRes)

        assertTrue(boards.writes.isEmpty())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `tapping save twice on a slow network writes once`() = runTest {
        boards.writeGate = CompletableDeferred()
        val viewModel = editor()
        viewModel.onLabelChange("Banheiro")

        viewModel.onSaveClick()
        viewModel.onSaveClick()
        boards.writeGate?.complete(Unit)

        assertEquals(1, boards.writes.size)
    }

    @Test
    fun `a storage failure keeps the form filled and explains it`() = runTest {
        boards.writeResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        val viewModel = editor()
        viewModel.onLabelChange("Banheiro")

        viewModel.onSaveClick()

        assertEquals(R.string.error_network_unavailable, viewModel.uiState.value.errorMessageRes)
        assertEquals("Banheiro", viewModel.uiState.value.labelInput)
    }

    @Test
    fun `the draft and the reserved id survive a restart, so saving never duplicates`() = runTest {
        val savedState = SavedStateHandle()
        editor(savedState).apply {
            onLabelChange("Banheiro")
            onEmojiChange("🚻")
        }

        val restored = editor(savedState)
        assertEquals("Banheiro", restored.uiState.value.labelInput)
        assertEquals(SymbolImage.Emoji("🚻"), restored.uiState.value.image)
        restored.onSaveClick()

        // The id reserved before the restart is reused: a second form would have reserved "reserved-2".
        assertEquals("reserved-1", boards.writes.single().target)
    }

    @Test
    fun `a photo that cannot be read or stored is explained and nothing changes`() = runTest {
        val viewModel = editor()

        photos.importResult = PhotoImportResult.Failed(PhotoImportError.UNREADABLE)
        viewModel.onPhotoPicked("content://broken")
        assertEquals(R.string.photo_error_unreadable, viewModel.uiState.value.errorMessageRes)

        photos.importResult = PhotoImportResult.Failed(PhotoImportError.STORAGE_FAILED)
        viewModel.onPhotoPicked("content://big")
        assertEquals(R.string.photo_error_storage, viewModel.uiState.value.errorMessageRes)
        assertEquals(null, viewModel.uiState.value.image)
    }

    @Test
    fun `closing without saving removes the photos imported by the form`() = runTest {
        val viewModel = editor()
        viewModel.onPhotoPicked("content://photo")
        assertEquals(SymbolImage.DevicePhoto("new-photo.jpg"), viewModel.uiState.value.image)

        viewModel.onClose()

        assertEquals(listOf(StoredPhoto("new-photo.jpg")), photos.deleted)
    }

    @Test
    fun `a form left with the system back opens again empty, and its imported photo is removed`() = runTest {
        // The ViewModel outlives the form: the same one is used when the form opens again.
        val viewModel = editor()
        viewModel.onLabelChange("Banheiro")
        viewModel.onPhotoPicked("content://photo")

        viewModel.start("opening-2", OWNER, AacViewer.Owner, "b1", existing = null)

        assertEquals("", viewModel.uiState.value.labelInput)
        assertEquals(null, viewModel.uiState.value.image)
        assertEquals(listOf(StoredPhoto("new-photo.jpg")), photos.deleted)
    }

    @Test
    fun `replacing a photo removes the old file after the edit is saved`() = runTest {
        val old = SymbolImage.DevicePhoto("old.jpg")
        val viewModel = editor(existing = symbol("s1", "Água", image = old))

        viewModel.onPhotoPicked("content://photo")
        assertTrue(photos.deleted.isEmpty())
        viewModel.onSaveClick()

        assertEquals("updateSymbol", boards.writes.single().operation)
        assertEquals(listOf(old.photo), photos.deleted)
    }

    @Test
    fun `a caregiver cannot add photos`() = runTest {
        val viewModel = editor(viewer = communicationManager)

        viewModel.onPhotoPicked("content://photo")

        assertFalse(viewModel.uiState.value.canUsePhotos)
        assertTrue(photos.imported.isEmpty())
    }
}
