package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.dialog.OriAlertDialog
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.text.Text

/** The board name dialog and the delete confirmations of the organizer. */
@Composable
fun OrganizerDialogs(state: AacOrganizeUiState, viewModel: AacOrganizeViewModel, onSymbolDeleted: () -> Unit) {
    state.boardDialog?.let { dialog ->
        BoardNameDialogView(
            dialog = dialog,
            onNameChange = viewModel::onBoardNameChange,
            onConfirm = viewModel::onBoardDialogConfirm,
            onDismiss = viewModel::onBoardDialogDismiss
        )
    }
    state.pendingDeletion?.let { pending ->
        val (titleRes, messageRes) = when (pending) {
            is PendingAacDeletion.Board -> R.string.aac_delete_board_title to R.string.aac_delete_board_message
            is PendingAacDeletion.Symbol -> R.string.aac_delete_symbol_title to R.string.aac_delete_symbol_message
        }
        OriConfirmationDialog(
            title = stringResource(titleRes, pending.name),
            message = stringResource(messageRes),
            confirmLabel = stringResource(R.string.aac_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                viewModel.onDeleteConfirm()
                if (pending is PendingAacDeletion.Symbol) onSymbolDeleted()
            },
            onDismiss = viewModel::onDeleteDismiss,
            isDestructive = true
        )
    }
}

@Composable
private fun BoardNameDialogView(
    dialog: BoardNameDialog,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    OriAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(if (dialog.boardId == null) R.string.aac_new_board else R.string.aac_rename_board),
        text = {
            Column {
                LabeledTextField(
                    label = stringResource(R.string.aac_board_name),
                    value = dialog.input,
                    onValueChange = onNameChange,
                    placeholder = stringResource(R.string.aac_board_name_placeholder),
                    keyboardOptions = KeyboardOptions(KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() })
                )
                FormMessage(messageRes = dialog.errorRes, type = FormMessageType.ERROR)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !dialog.isSaving) {
                Text(stringResource(R.string.aac_board_save), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
