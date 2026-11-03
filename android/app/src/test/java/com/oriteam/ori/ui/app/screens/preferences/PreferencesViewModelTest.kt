package com.oriteam.ori.ui.app.screens.preferences

import com.oriteam.ori.R
import com.oriteam.ori.domain.auth.AuthenticatedUser
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.preferences.AccountPreferencesSync
import com.oriteam.ori.domain.preferences.ContentDensity
import com.oriteam.ori.domain.preferences.ListPresentation
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.domain.preferences.TextScale
import com.oriteam.ori.domain.preferences.ThemeMode
import com.oriteam.ori.testing.FakeAccountPreferencesRepository
import com.oriteam.ori.testing.FakeAuthRepository
import com.oriteam.ori.testing.FakeDevicePreferencesRepository
import com.oriteam.ori.testing.MainDispatcherRule
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

class PreferencesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository().apply {
        signedInUser.value = AuthenticatedUser(uid = "pat", displayName = "Pat")
    }
    private val account = FakeAccountPreferencesRepository()
    private val device = FakeDevicePreferencesRepository()
    private val sync = AccountPreferencesSync(auth, account, device)

    private fun TestScope.viewModel() = PreferencesViewModel(sync, device).also {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() }
    }

    @Test
    fun `personal choices go to the account`() = runTest {
        val viewModel = viewModel()

        viewModel.onTextScaleChange(TextScale.LARGER)
        viewModel.onThemeChange(ThemeMode.DARK)
        viewModel.onSimplifiedChange(true)
        viewModel.onMotionChange(MotionLevel.OFF)

        val state = viewModel.uiState.value.account
        assertEquals(TextScale.LARGER, state.textScale)
        assertEquals(ThemeMode.DARK, state.theme)
        assertTrue(state.isSimplified)
        assertEquals(MotionLevel.OFF, state.motion)
        assertEquals(state, account.saved.last().second)
    }

    @Test
    fun `device choices never go to the account`() = runTest {
        val viewModel = viewModel()

        viewModel.onDensityChange(ContentDensity.SPACIOUS)
        viewModel.onListPresentationChange(ListPresentation.SUMMARY)
        viewModel.onStartAreaChange(StartArea.COMMUNICATION)

        val state = viewModel.uiState.value.device
        assertEquals(ContentDensity.SPACIOUS, state.density)
        assertEquals(ListPresentation.SUMMARY, state.listPresentation)
        assertEquals(StartArea.COMMUNICATION, state.startArea)
        assertTrue(account.saved.isEmpty())
    }

    @Test
    fun `a refused change shows why and goes back`() = runTest {
        account.saveResult = DataResult.Failure(DataError.PERMISSION_DENIED)
        val viewModel = viewModel()

        viewModel.onThemeChange(ThemeMode.DARK)

        assertEquals(R.string.error_permission_denied, viewModel.uiState.value.errorRes)
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.account.theme)
    }

    @Test
    fun `offline the person is told the change waits for the connection, and the next one clears it`() = runTest {
        account.saveResult = DataResult.Success(WriteStatus.PENDING_SYNC)
        val viewModel = viewModel()

        viewModel.onThemeChange(ThemeMode.DARK)
        assertEquals(R.string.sync_pending_changes, viewModel.uiState.value.noticeRes)

        viewModel.onDensityChange(ContentDensity.COMPACT)
        assertNull(viewModel.uiState.value.noticeRes)
    }
}
