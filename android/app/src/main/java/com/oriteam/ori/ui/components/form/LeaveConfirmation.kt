package com.oriteam.ori.ui.components.form

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog

/**
 * What "Cancelar", the arrow and the system "back" do in a form: leave at once when nothing changed, or ask first
 * when something did, so a stray tap never loses what was typed (G3 of the general test). Dismissing the question
 * keeps editing. Returns the action for "Cancelar" and the arrow; the system "back" is handled here.
 */
@Composable
fun rememberLeaveWithConfirmation(hasChanges: Boolean, onLeave: () -> Unit): () -> Unit {
    var isAsking by rememberSaveable { mutableStateOf(false) }
    val currentOnLeave by rememberUpdatedState(onLeave)
    BackHandler(enabled = hasChanges) { isAsking = true }
    if (isAsking) {
        OriConfirmationDialog(
            title = stringResource(R.string.form_discard_title),
            message = stringResource(R.string.form_discard_message),
            confirmLabel = stringResource(R.string.form_discard_confirm),
            dismissLabel = stringResource(R.string.form_keep_editing),
            onConfirm = {
                isAsking = false
                currentOnLeave()
            },
            onDismiss = { isAsking = false },
            isDestructive = true
        )
    }
    return { if (hasChanges) isAsking = true else currentOnLeave() }
}
