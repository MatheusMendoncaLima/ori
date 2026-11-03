package com.oriteam.ori.ui.app.screens.routine

import androidx.lifecycle.SavedStateHandle
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.model.TaskStatus
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.testing.DAY
import com.oriteam.ori.testing.FakeRoutineRepository
import com.oriteam.ori.testing.MainDispatcherRule
import com.oriteam.ori.testing.OWNER
import com.oriteam.ori.testing.activity
import com.oriteam.ori.testing.manager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class ActivityEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val routines = FakeRoutineRepository()

    private fun editor(
        viewer: RoutineViewer = RoutineViewer.Owner,
        savedState: SavedStateHandle = SavedStateHandle(),
        existing: RoutineTask? = null
    ) = ActivityEditorViewModel(routines, savedState).apply { start(OWNER, viewer, existing, DAY) }

    @Test
    fun `creating saves the typed activity with a reserved id and reports success`() = runTest {
        val viewModel = editor()
        viewModel.onTitleChange("  Tomar café ")
        viewModel.onTimeChange("7:30")
        viewModel.onDescriptionChange("Com frutas")
        viewModel.onHiddenFromCaregiverChange(true)

        viewModel.onSaveClick()

        val write = routines.writes.single()
        assertEquals("create" to "reserved-1", write.operation to write.activityId)
        assertEquals("Tomar café", write.draft?.title)
        assertEquals(LocalTime.of(7, 30), write.draft?.scheduledTime)
        assertEquals(DAY, write.draft?.date)
        assertTrue(write.draft!!.isHiddenFromCaregiver && write.draft!!.ownerCanComplete)
        assertEquals(
            ActivityEditorViewModel.Event.Saved("reserved-1", date = DAY, isPendingSync = false),
            viewModel.events.first()
        )
    }

    @Test
    fun `a repeating activity is reported without a day, because it falls on many`() = runTest {
        val viewModel = editor()
        viewModel.onTitleChange("Terapia")
        viewModel.onRepeatChange(RepeatRule.Weekly(setOf(DayOfWeek.MONDAY)))

        viewModel.onSaveClick()

        assertNull((viewModel.events.first() as ActivityEditorViewModel.Event.Saved).date)
    }

    @Test
    fun `a duplicate on the same day and time is blocked before any write`() = runTest {
        routines.dayResult = DataResult.Success(listOf(activity("x", title = "Tomar Café", time = LocalTime.of(7, 30))))
        val viewModel = editor()
        viewModel.onTitleChange("tomar café")
        viewModel.onTimeChange("07:30")

        viewModel.onSaveClick()

        assertEquals(R.string.activity_error_duplicate, viewModel.uiState.value.errorMessageRes)
        assertTrue(routines.writes.isEmpty())
    }

    @Test
    fun `tapping save twice while saving writes once`() = runTest {
        val slowNetwork = CompletableDeferred<Unit>()
        routines.writeGate = slowNetwork
        val viewModel = editor()
        viewModel.onTitleChange("Ler")

        viewModel.onSaveClick()
        assertTrue(viewModel.uiState.value.isSaving)
        viewModel.onSaveClick()
        slowNetwork.complete(Unit)

        assertEquals(1, routines.writes.count { it.operation == "create" })
    }

    @Test
    fun `an invalid time is explained and nothing is written`() = runTest {
        val viewModel = editor()
        viewModel.onTitleChange("Ler")
        viewModel.onTimeChange("25:00")

        viewModel.onSaveClick()

        assertEquals(R.string.activity_error_invalid_time, viewModel.uiState.value.errorMessageRes)
        assertTrue(routines.writes.isEmpty())
    }

    @Test
    fun `a network failure keeps the form so the person can try again`() = runTest {
        routines.dayResult = DataResult.Failure(DataError.NETWORK_UNAVAILABLE)
        val viewModel = editor()
        viewModel.onTitleChange("Ler")

        viewModel.onSaveClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_network_unavailable, state.errorMessageRes)
        assertEquals("Ler", state.titleInput)
        assertFalse(state.isSaving)
        assertTrue(routines.writes.isEmpty())
    }

    @Test
    fun `an offline save is accepted as pending`() = runTest {
        routines.writeResult = DataResult.Success(WriteStatus.PENDING_SYNC)
        val viewModel = editor()
        viewModel.onTitleChange("Ler")

        viewModel.onSaveClick()

        assertEquals(
            ActivityEditorViewModel.Event.Saved("reserved-1", date = DAY, isPendingSync = true),
            viewModel.events.first()
        )
    }

    @Test
    fun `a draft and its id survive an app restart, so saving never duplicates`() = runTest {
        val savedState = SavedStateHandle()
        editor(savedState = savedState).apply {
            onTitleChange("Arrumar a mochila")
            onTimeChange("18:00")
        }

        // After process death Android recreates the ViewModel with the same saved state.
        val restored = editor(savedState = savedState)
        assertEquals("Arrumar a mochila", restored.uiState.value.titleInput)
        assertEquals("18:00", restored.uiState.value.timeInput)

        restored.onSaveClick()

        // The id reserved before the restart is reused; no second id was reserved.
        assertEquals(listOf("reserved-1"), routines.writes.map { it.activityId })
    }

    @Test
    fun `editing updates the same activity and does not count it as its own duplicate`() = runTest {
        val existing = activity("own", title = "Ler", createdBy = OWNER)
        routines.dayResult = DataResult.Success(listOf(existing))
        val viewModel = editor(existing = existing)
        viewModel.onDescriptionChange("10 páginas")

        viewModel.onSaveClick()

        val write = routines.writes.single()
        assertEquals("update" to "own", write.operation to write.activityId)
        assertEquals("10 páginas", write.draft?.description)
    }

    @Test
    fun `a caregiver can reserve completion but can never hide an activity`() = runTest {
        val hiddenBefore = activity("a", status = TaskStatus.HIDDEN_FROM_CAREGIVER)
        val viewModel = editor(viewer = manager, existing = hiddenBefore)
        viewModel.onOwnerCanCompleteChange(false)
        viewModel.onHiddenFromCaregiverChange(true)

        viewModel.onSaveClick()

        val draft = routines.writes.single().draft!!
        assertFalse(draft.ownerCanComplete)
        assertFalse(draft.isHiddenFromCaregiver)
    }

    @Test
    fun `closing discards the draft so the next new activity starts empty`() = runTest {
        val savedState = SavedStateHandle()
        editor(savedState = savedState).apply {
            onTitleChange("Rascunho")
            onClose()
        }

        val next = editor(savedState = savedState)

        assertEquals("", next.uiState.value.titleInput)
        assertEquals(true, next.uiState.value.isNew)
    }
}
