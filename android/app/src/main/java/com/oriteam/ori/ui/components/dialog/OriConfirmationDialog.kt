package com.oriteam.ori.ui.components.dialog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oriteam.ori.ui.components.text.Text

/**
 * Asks the user to confirm an action. The question goes in [title]; [message] explains the
 * consequence in plain words. A destructive action (deleting, ending a care link) is shown in
 * the error color and is never the only way out: dismissing always cancels.
 */
@Composable
fun OriConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    OriAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = title,
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isDestructive) colors.error else colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissLabel, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant
    )
}
