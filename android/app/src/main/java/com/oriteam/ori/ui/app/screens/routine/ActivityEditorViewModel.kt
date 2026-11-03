package com.oriteam.ori.ui.app.screens.routine

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.model.TaskStatus
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.routine.ActivityDraft
import com.oriteam.ori.domain.routine.ActivityValidationError
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.domain.routine.TimeInput
import com.oriteam.ori.domain.routine.parseScheduledTime
import com.oriteam.ori.domain.routine.repeatRuleFromStoredText
import com.oriteam.ori.domain.routine.toStoredText
import com.oriteam.ori.domain.routine.validateActivity
import com.oriteam.ori.ui.app.screens.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ActivityEditorUiState(
    val titleInput: String = "",
    val timeInput: String = "",
    val descriptionInput: String = "",
    /** Null until [ActivityEditorViewModel.start] opens the form: there is no day to show before that. */
    val date: LocalDate? = null,
    /** Null: only on [date]. Otherwise [date] is the first day. */
    val repeat: RepeatRule? = null,
    val ownerCanComplete: Boolean = true,
    val isHiddenFromCaregiver: Boolean = false,
    /** Without an account there is no caregiver to hide anything from. */
    val isLocalOwner: Boolean = false,
    val isNew: Boolean = true,
    /** The primary user sees "hide from caregiver"; the caregiver sees "may be completed by the user". */
    val isEditedByOwner: Boolean = true,
    val isSaving: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

/** Every form field lives in the SavedStateHandle, so a draft survives process death. */
private object EditorKeys {
    const val ACTIVITY_ID = "editor_activity_id"
    const val TITLE = "editor_title"
    const val TIME = "editor_time"
    const val DESCRIPTION = "editor_description"
    const val DATE = "editor_date"
    const val REPEAT = "editor_repeat"
    const val OWNER_CAN_COMPLETE = "editor_owner_can_complete"
    const val HIDDEN = "editor_hidden"
    const val IS_NEW = "editor_is_new"
    val all = listOf(ACTIVITY_ID, TITLE, TIME, DESCRIPTION, DATE, REPEAT, OWNER_CAN_COMPLETE, HIDDEN, IS_NEW)
}

private val timeInputFormat = DateTimeFormatter.ofPattern("HH:mm")

/** Creates or edits one activity: form state, validation, duplicate check and saving. */
@HiltViewModel
class ActivityEditorViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    sealed interface Event {
        /**
         * Saved; [isPendingSync] when it waits on the device for a connection. [date] is the day of a plain
         * activity, and null for a repeating one, which falls on many days.
         */
        data class Saved(val activityId: String, val date: LocalDate?, val isPendingSync: Boolean) : Event
    }

    private val _uiState = MutableStateFlow(restoredState())
    val uiState: StateFlow<ActivityEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var ownerUid: String? = null
    private var viewer: RoutineViewer = RoutineViewer.Owner

    /**
     * Opens the form for [existing] or for a new activity on [defaultDate]. A restored draft
     * (after rotation or process death) is kept instead of being reset.
     */
    fun start(ownerUid: String, viewer: RoutineViewer, existing: RoutineTask?, defaultDate: LocalDate) {
        this.ownerUid = ownerUid
        this.viewer = viewer
        val isOwner = viewer == RoutineViewer.Owner
        if (savedStateHandle.contains(EditorKeys.ACTIVITY_ID)) {
            _uiState.update { it.copy(isEditedByOwner = isOwner, isLocalOwner = ownerUid == LOCAL_OWNER_UID) }
            return
        }
        // Reserved once: saving twice, or after a restart, writes the same activity.
        // One day of a repeating activity opens the whole activity, from its first day.
        savedStateHandle[EditorKeys.ACTIVITY_ID] =
            existing?.repeat?.seriesId ?: existing?.id ?: routineRepository.newActivityId(ownerUid)
        updateForm {
            ActivityEditorUiState(
                titleInput = existing?.title.orEmpty(),
                timeInput = existing?.scheduledTime?.format(timeInputFormat).orEmpty(),
                descriptionInput = existing?.description.orEmpty(),
                date = existing?.repeat?.startDate ?: existing?.date ?: defaultDate,
                repeat = existing?.repeat?.rule,
                ownerCanComplete = existing?.ownerCanComplete ?: true,
                isHiddenFromCaregiver = existing?.status == TaskStatus.HIDDEN_FROM_CAREGIVER,
                isNew = existing == null,
                isEditedByOwner = isOwner,
                isLocalOwner = ownerUid == LOCAL_OWNER_UID
            )
        }
    }

    fun onTitleChange(value: String) = updateForm { it.copy(titleInput = value, errorMessageRes = null) }

    fun onTimeChange(value: String) = updateForm { it.copy(timeInput = value, errorMessageRes = null) }

    fun onDescriptionChange(value: String) = updateForm { it.copy(descriptionInput = value, errorMessageRes = null) }

    fun onDateChange(value: LocalDate) = updateForm { it.copy(date = value, errorMessageRes = null) }

    fun onRepeatChange(value: RepeatRule?) = updateForm { it.copy(repeat = value, errorMessageRes = null) }

    fun onOwnerCanCompleteChange(value: Boolean) = updateForm { it.copy(ownerCanComplete = value) }

    fun onHiddenFromCaregiverChange(value: Boolean) = updateForm { it.copy(isHiddenFromCaregiver = value) }

    /** Closing the form discards the draft, so the next "new activity" starts empty. */
    fun onClose() {
        EditorKeys.all.forEach { savedStateHandle.remove<Any>(it) }
        _uiState.value = ActivityEditorUiState()
    }

    fun onSaveClick() {
        val state = _uiState.value
        val owner = ownerUid
        val activityId = savedStateHandle.get<String>(EditorKeys.ACTIVITY_ID)
        val date = state.date
        val time = parseScheduledTime(state.timeInput)
        when {
            owner == null || activityId == null || date == null || state.isSaving -> Unit
            time == TimeInput.Invalid -> showError(ActivityValidationError.INVALID_TIME.messageRes())
            else -> {
                val draft = draftFrom(state, date, (time as? TimeInput.Valid)?.time)
                _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
                viewModelScope.launch { save(owner, activityId, draft, state.isNew) }
            }
        }
    }

    private fun draftFrom(state: ActivityEditorUiState, date: LocalDate, time: LocalTime?): ActivityDraft {
        val isOwner = viewer == RoutineViewer.Owner
        return ActivityDraft(
            title = state.titleInput.trim(),
            date = date,
            scheduledTime = time,
            description = state.descriptionInput.trim().ifEmpty { null },
            // The primary user's own activities are always self-completable; only they can hide one.
            ownerCanComplete = isOwner || state.ownerCanComplete,
            isHiddenFromCaregiver = isOwner && state.isHiddenFromCaregiver,
            repeat = state.repeat
        )
    }

    private suspend fun save(owner: String, activityId: String, draft: ActivityDraft, isNew: Boolean) {
        val sameDay = routineRepository.getDay(owner, viewer, draft.date)
        if (sameDay is DataResult.Failure) return showError(sameDay.error.messageRes())
        val activities = (sameDay as DataResult.Success).value
        validateActivity(draft, activities, editingId = activityId)?.let { return showError(it.messageRes()) }

        val write = if (isNew) {
            routineRepository.createActivity(owner, activityId, draft, viewer)
        } else {
            routineRepository.updateActivity(owner, activityId, draft, viewer)
        }
        when (write) {
            is DataResult.Failure -> showError(write.error.messageRes())
            is DataResult.Success -> {
                onClose()
                _events.send(
                    Event.Saved(
                        activityId,
                        date = draft.date.takeIf { draft.repeat == null },
                        isPendingSync = write.value == WriteStatus.PENDING_SYNC
                    )
                )
            }
        }
    }

    private fun showError(@StringRes messageRes: Int) {
        _uiState.update { it.copy(isSaving = false, errorMessageRes = messageRes) }
    }

    private fun updateForm(transform: (ActivityEditorUiState) -> ActivityEditorUiState) {
        val updated = transform(_uiState.value)
        _uiState.value = updated
        savedStateHandle[EditorKeys.TITLE] = updated.titleInput
        savedStateHandle[EditorKeys.TIME] = updated.timeInput
        savedStateHandle[EditorKeys.DESCRIPTION] = updated.descriptionInput
        savedStateHandle[EditorKeys.DATE] = updated.date?.toString()
        savedStateHandle[EditorKeys.REPEAT] = updated.repeat?.toStoredText()
        savedStateHandle[EditorKeys.OWNER_CAN_COMPLETE] = updated.ownerCanComplete
        savedStateHandle[EditorKeys.HIDDEN] = updated.isHiddenFromCaregiver
        savedStateHandle[EditorKeys.IS_NEW] = updated.isNew
    }

    private fun restoredState(): ActivityEditorUiState {
        if (!savedStateHandle.contains(EditorKeys.ACTIVITY_ID)) return ActivityEditorUiState()
        return ActivityEditorUiState(
            titleInput = savedStateHandle[EditorKeys.TITLE] ?: "",
            timeInput = savedStateHandle[EditorKeys.TIME] ?: "",
            descriptionInput = savedStateHandle[EditorKeys.DESCRIPTION] ?: "",
            date = savedStateHandle.get<String>(EditorKeys.DATE)?.let(LocalDate::parse),
            repeat = repeatRuleFromStoredText(savedStateHandle[EditorKeys.REPEAT]),
            ownerCanComplete = savedStateHandle[EditorKeys.OWNER_CAN_COMPLETE] ?: true,
            isHiddenFromCaregiver = savedStateHandle[EditorKeys.HIDDEN] ?: false,
            isNew = savedStateHandle[EditorKeys.IS_NEW] ?: true
        )
    }
}
