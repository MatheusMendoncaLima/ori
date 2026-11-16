package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.oriteam.ori.R
import com.oriteam.ori.domain.routine.TimeInput
import com.oriteam.ori.domain.routine.parseScheduledTime
import com.oriteam.ori.ui.components.dialog.OriAlertDialog
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberTimeFormat
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** The example in the field: half past eight, as the device writes it. */
private const val EXAMPLE_HOUR = 8
private const val EXAMPLE_MINUTE = 30
private const val AFTERNOON_HOUR = 13

/** With no time yet, the clock opens at the start of the morning instead of at midnight. */
private const val DEFAULT_HOUR = 8

/**
 * The time of an activity: typed, for who prefers the keyboard, or chosen on the clock of the system
 * with "Escolher". An empty field still means "no time". The example, the clock and the time the clock writes follow
 * the time format of the device (G4: the example was in 12 hours while the times showed in 24); with 24 hours the
 * keyboard is of numbers. A time that cannot be read is said when the person leaves the field, not only on saving.
 */
@Composable
internal fun ActivityTimeField(timeInput: String, onTimeChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var isPicking by rememberSaveable { mutableStateOf(false) }
    var hasLeftField by rememberSaveable { mutableStateOf(false) }
    val timeFormat = rememberTimeFormat()
    val example = remember(timeFormat) { LocalTime.of(EXAMPLE_HOUR, EXAMPLE_MINUTE).format(timeFormat) }
    val uses24Hours = remember(timeFormat) { usesTwentyFourHours(timeFormat) }
    val isInvalid = hasLeftField && parseScheduledTime(timeInput) == TimeInput.Invalid
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            modifier = Modifier.fillMaxWidth()
        ) {
            LabeledTextField(
                label = stringResource(R.string.activity_time_input_label),
                value = timeInput,
                onValueChange = onTimeChange,
                placeholder = stringResource(R.string.activity_time_placeholder, example),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (uses24Hours) KeyboardType.Number else KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions.Default,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focus -> hasLeftField = !focus.hasFocus && timeInput.isNotBlank() }
            )
            OriSecondaryButton(
                text = stringResource(R.string.activity_time_pick),
                onClick = { isPicking = true },
                isFullWidth = false
            )
        }
        FormMessage(
            message = if (isInvalid) stringResource(R.string.activity_time_invalid_example, example) else null,
            type = FormMessageType.ERROR
        )
    }
    if (isPicking) {
        TimePickerDialog(
            initial = (parseScheduledTime(timeInput) as? TimeInput.Valid)?.time,
            is24Hour = uses24Hours,
            onPick = { time ->
                onTimeChange(time.format(timeFormat))
                isPicking = false
            },
            onDismiss = { isPicking = false }
        )
    }
}

/** Whether [format] writes the afternoon as 13 and on, rather than 1 with PM. */
internal fun usesTwentyFourHours(format: DateTimeFormatter): Boolean =
    LocalTime.of(AFTERNOON_HOUR, 0).format(format).contains(AFTERNOON_HOUR.toString())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime?,
    is24Hour: Boolean,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: DEFAULT_HOUR,
        initialMinute = initial?.minute ?: 0,
        is24Hour = is24Hour
    )
    OriAlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        paneTitle = stringResource(R.string.activity_time_label),
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text(text = stringResource(R.string.activity_time_pick_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.action_cancel)) }
        },
        text = { TimePicker(state = state) }
    )
}
