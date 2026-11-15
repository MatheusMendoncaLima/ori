package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.adaptive.FillOrScrollColumn
import com.oriteam.ori.ui.components.form.FormActionBar
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.rememberFormOpening
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.settings.SettingsSwitchItem
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.LocalDate

/**
 * Form to create ([existing] null) or edit an activity. [editorKey] gives each form its own
 * ViewModel and saved draft.
 */
@Composable
fun ActivityEditorRoute(
    editorKey: String,
    ownerUid: String,
    viewer: RoutineViewer,
    existing: RoutineTask?,
    defaultDate: LocalDate,
    today: LocalDate,
    onSaved: (ActivityEditorViewModel.Event.Saved) -> Unit,
    onClose: () -> Unit,
    viewModel: ActivityEditorViewModel = hiltViewModel(key = editorKey)
) {
    val opening = rememberFormOpening()
    LaunchedEffect(editorKey, opening) { viewModel.start(opening, ownerUid, viewer, existing, defaultDate) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ActivityEditorViewModel.Event.Saved -> onSaved(event)
            }
        }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ActivityEditorScreen(
        state = state,
        today = today,
        onTitleChange = viewModel::onTitleChange,
        onTimeChange = viewModel::onTimeChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDateChange = viewModel::onDateChange,
        onRepeatChange = viewModel::onRepeatChange,
        onOwnerCanCompleteChange = viewModel::onOwnerCanCompleteChange,
        onHiddenFromCaregiverChange = viewModel::onHiddenFromCaregiverChange,
        onSave = viewModel::onSaveClick,
        onClose = {
            viewModel.onClose()
            onClose()
        }
    )
}

@Composable
fun ActivityEditorScreen(
    state: ActivityEditorUiState,
    today: LocalDate,
    onTitleChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onOwnerCanCompleteChange: (Boolean) -> Unit,
    onHiddenFromCaregiverChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onRepeatChange: (RepeatRule?) -> Unit = {}
) {
    // The form gets its date when the ViewModel starts; before that there is nothing to draw.
    val date = state.date ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        OriTopBar(
            maxWidth = OriDimensions.FormMaxWidth,
            title = stringResource(if (state.isNew) R.string.activity_new_title else R.string.activity_edit_title),
            onNavigateBack = onClose
        )
        FillOrScrollColumn(modifier = Modifier.weight(1f)) {
            CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
                // Named, because beside the list there is another calendar, which only chooses the day shown.
                SectionHeader(text = stringResource(R.string.activity_day_label))
                WeekStrip(date = date, today = today, onSelectDate = onDateChange)
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                LabeledTextField(
                    label = stringResource(R.string.activity_title_label),
                    value = state.titleInput,
                    onValueChange = onTitleChange,
                    placeholder = stringResource(R.string.activity_title_placeholder),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions.Default
                )
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                ActivityTimeField(timeInput = state.timeInput, onTimeChange = onTimeChange)
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                RepeatPicker(rule = state.repeat, startDate = date, onRuleChange = onRepeatChange)
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                LabeledTextField(
                    label = stringResource(R.string.activity_description_label),
                    value = state.descriptionInput,
                    onValueChange = onDescriptionChange,
                    placeholder = stringResource(R.string.activity_description_placeholder),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    keyboardActions = KeyboardActions.Default,
                    singleLine = false
                )
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                if (state.isEditedByOwner) {
                    if (!state.isLocalOwner) {
                        SettingsSwitchItem(
                            title = stringResource(R.string.activity_hide_from_caregiver),
                            description = stringResource(R.string.activity_hide_from_caregiver_description),
                            checked = state.isHiddenFromCaregiver,
                            onCheckedChange = onHiddenFromCaregiverChange
                        )
                    }
                } else {
                    SettingsSwitchItem(
                        title = stringResource(R.string.activity_owner_can_complete),
                        description = stringResource(R.string.activity_owner_can_complete_description),
                        checked = state.ownerCanComplete,
                        onCheckedChange = onOwnerCanCompleteChange
                    )
                }
                FormMessage(messageRes = state.errorMessageRes, type = FormMessageType.ERROR)
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
        FormActionBar(
            saveLabel = stringResource(R.string.activity_save),
            isSaving = state.isSaving,
            onSave = onSave,
            onCancel = onClose
        )
    }
}
