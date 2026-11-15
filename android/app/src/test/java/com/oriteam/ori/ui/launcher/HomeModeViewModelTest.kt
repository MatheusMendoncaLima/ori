package com.oriteam.ori.ui.launcher

import com.oriteam.ori.R
import com.oriteam.ori.testing.FakeHomeActivation
import com.oriteam.ori.testing.FakeLauncherSettingsRepository
import com.oriteam.ori.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Turning ORI Home on (approved and refused) and off, always through Android, never forced. */
class HomeModeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val activation = FakeHomeActivation()
    private val settings = FakeLauncherSettingsRepository()
    private val viewModel = HomeModeViewModel(activation, settings)

    @Test
    fun `turning it on remembers the launcher in use, turns the entry on and asks Android`() = runTest {
        viewModel.onActivate()

        assertEquals(HomeModeViewModel.Event.RequestHome, viewModel.events.first())
        assertTrue(activation.isEnabled)
        assertEquals("com.samsung.launcher", settings.settings.value.previousLauncher)
    }

    @Test
    fun `approved, ORI Home is the home screen`() = runTest {
        viewModel.onActivate()
        activation.answerRequest()
        viewModel.onRequestResult()

        assertTrue(viewModel.uiState.value.isOriHome)
        assertEquals(R.string.home_mode_approved, viewModel.uiState.value.messageRes)
    }

    @Test
    fun `refused, the entry goes away again and nothing else changes`() = runTest {
        activation.approvesRequest = false
        viewModel.onActivate()
        activation.answerRequest()
        viewModel.onRequestResult()

        assertFalse(viewModel.uiState.value.isOriHome)
        assertFalse(activation.isEnabled)
        assertEquals(listOf(true, false), activation.enabledChanges)
        assertEquals(R.string.home_mode_denied, viewModel.uiState.value.messageRes)
    }

    @Test
    fun `turning it off goes back to the launcher used before`() = runTest {
        viewModel.onActivate()
        activation.answerRequest()
        viewModel.onRequestResult()
        viewModel.events.take(1).toList()

        viewModel.onDeactivate()

        assertEquals(HomeModeViewModel.Event.OpenPreviousLauncher("com.samsung.launcher"), viewModel.events.first())
        assertFalse(activation.isEnabled)
        assertFalse(viewModel.uiState.value.isOriHome)
    }

    @Test
    fun `when ORI Home already is the home screen, asking again does nothing`() = runTest {
        activation.isHome = true
        val viewModel = HomeModeViewModel(activation, settings)

        viewModel.onActivate()

        assertTrue(viewModel.uiState.value.isOriHome)
        assertTrue(activation.enabledChanges.isEmpty())
    }
}
