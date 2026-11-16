package com.oriteam.ori.ui.app.screens.routine

import androidx.annotation.StringRes
import com.oriteam.ori.domain.routine.RepeatRule
import java.time.LocalDate

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
    /** Something differs from how the form opened: leaving asks first, so a stray tap loses nothing (G3). */
    val hasChanges: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

/**
 * Whether what the person can change differs from [opened], the form as it opened. Without [opened] (the app was
 * closed by the system and the draft came back) it cannot be told, so it counts as changed.
 */
internal fun ActivityEditorUiState.differsFrom(opened: ActivityEditorUiState?): Boolean =
    opened == null || editableFields() != opened.editableFields()

private fun ActivityEditorUiState.editableFields() =
    listOf(titleInput, timeInput, descriptionInput, date, repeat, ownerCanComplete, isHiddenFromCaregiver)
