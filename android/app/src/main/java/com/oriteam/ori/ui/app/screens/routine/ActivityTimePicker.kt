package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.oriteam.ori.R
import com.oriteam.ori.domain.routine.TimeInput
import com.oriteam.ori.domain.routine.parseScheduledTime
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

/** With no time yet, the clock opens at the start of the morning instead of at midnight. */
private const val DEFAULT_HOUR = 8

/**
 * The time of an activity: typed, for who prefers the keyboard, or chosen on the clock of the system
 * with "Escolher". An empty field still means "no time".
 */
@Composable
internal fun ActivityTimeField(timeInput: String, onTimeChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var isPicking by rememberSaveable { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = modifier.fillMaxWidth()
    ) {
        LabeledTextField(
            label = stringResource(R.string.activity_time_input_label),
            value = timeInput,
            onValueChange = onTimeChange,
            placeholder = stringResource(R.string.activity_time_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions.Default,
            modifier = Modifier.weight(1f)
        )
        OriSecondaryButton(
            text = stringResource(R.string.activity_time_pick),
            onClick = { isPicking = true },
            isFullWidth = false
        )
    }
    if (isPicking) {
        TimePickerDialog(
            initial = (parseScheduledTime(timeInput) as? TimeInput.Valid)?.time,
            onPick = { time ->
                onTimeChange(time.format(timeFormat))
                isPicking = false
            },
            onDismiss = { isPicking = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initial: LocalTime?, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: DEFAULT_HOUR,
        initialMinute = initial?.minute ?: 0,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
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
