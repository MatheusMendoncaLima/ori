package com.oriteam.ori.ui.app.screens.account

import com.oriteam.ori.R
import com.oriteam.ori.domain.photos.PhotoImportError
import com.oriteam.ori.testing.FakeProfilePhotoStore
import com.oriteam.ori.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProfilePhotoViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = FakeProfilePhotoStore()

    // Created inside each test, after the rule replaced the main dispatcher (the state starts in viewModelScope).
    private val viewModel by lazy { ProfilePhotoViewModel(store) }

    @Test
    fun `a picked photo shows, and removing it leaves none`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.photoPath.collect() }
        viewModel.show("ana-uid")

        viewModel.onPhotoPicked("content://photo")
        assertEquals("content://photo", viewModel.photoPath.value)

        viewModel.onRemovePhoto()
        assertNull(viewModel.photoPath.value)
    }

    @Test
    fun `a photo that cannot be used is explained, and the next try clears the message`() = runTest {
        viewModel.show("ana-uid")
        store.setError = PhotoImportError.UNREADABLE
        viewModel.onPhotoPicked("content://broken")
        assertEquals(R.string.photo_error_unreadable, viewModel.errorRes.value)

        store.setError = null
        viewModel.onPhotoPicked("content://photo")
        assertNull(viewModel.errorRes.value)
    }

    @Test
    fun `nothing changes before the profile is known`() {
        viewModel.onPhotoPicked("content://photo")

        assertEquals(emptyMap<String, String>(), store.photos.value)
    }
}
