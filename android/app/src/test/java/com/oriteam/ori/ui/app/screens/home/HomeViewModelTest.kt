package com.oriteam.ori.ui.app.screens.home

import com.oriteam.ori.domain.preferences.DevicePreferences
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `the opening plays on the first launch, and is recorded only when it ends`() {
        val preferences = FakeDevicePreferencesRepository()
        val viewModel = HomeViewModel(preferences)

        assertEquals(true, viewModel.uiState.value.playsIntro)
        assertFalse(viewModel.uiState.value.showOptions)
        assertFalse(preferences.preferences.value.hasSeenIntro)

        viewModel.onIntroFinished()

        assertTrue(viewModel.uiState.value.showOptions)
        assertTrue(preferences.preferences.value.hasSeenIntro)
    }

    @Test
    fun `after the first launch the options come after the usual pause`() {
        val viewModel = HomeViewModel(FakeDevicePreferencesRepository(device = DevicePreferences(hasSeenIntro = true)))

        assertEquals(false, viewModel.uiState.value.playsIntro)
        viewModel.onTimerFinished()
        assertTrue(viewModel.uiState.value.showOptions)
    }
}
