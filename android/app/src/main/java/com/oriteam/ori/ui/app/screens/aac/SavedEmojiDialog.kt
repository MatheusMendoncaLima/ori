package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.savedEmojiOrNull
import com.oriteam.ori.ui.components.dialog.OriAlertDialog
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Chooses the picture of a saved phrase: one of the suggestions or any emoji typed with the keyboard, the same
 * choices as a symbol's. "Usar o padrão" goes back to the app's picture for saved phrases.
 */
@Composable
fun SavedEmojiDialog(message: SavedMessage, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var emoji by rememberSaveable(message.id) { mutableStateOf(message.emoji.orEmpty()) }
    val colors = MaterialTheme.colorScheme
    OriAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.aac_saved_emoji_title),
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = message.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                LabeledTextField(
                    label = stringResource(R.string.aac_symbol_emoji),
                    value = emoji,
                    onValueChange = { emoji = it.trim() },
                    placeholder = stringResource(R.string.aac_symbol_emoji_placeholder),
                    keyboardOptions = KeyboardOptions.Default,
                    keyboardActions = KeyboardActions.Default
                )
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                EmojiSuggestions(selected = emoji.ifEmpty { null }, onSelect = { emoji = it })
                OriTextButton(text = stringResource(R.string.aac_saved_emoji_default), onClick = { onConfirm(null) })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(savedEmojiOrNull(emoji)) }) {
                Text(
                    text = stringResource(R.string.aac_saved_emoji_save),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface
                )
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant
    )
}
