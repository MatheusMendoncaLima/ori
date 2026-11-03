package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.MAX_TYPED_TEXT_LENGTH
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Typing is part of communicating: a word that is not on any board can still be said. The field
 * opens on request, already focused, so writing takes one tap and the symbols keep the space the
 * rest of the time. A full message keeps the text in the field instead of losing it.
 */
@Composable
fun TypedTextInput(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: (String) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onSaveWord: ((String) -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    val canAdd = isEnabled && text.isNotBlank()
    val add = {
        if (canAdd) {
            onAdd(text)
            onTextChange("")
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small)
        ) {
            LabeledTextField(
                label = stringResource(R.string.aac_type_label),
                value = text,
                onValueChange = { onTextChange(it.take(MAX_TYPED_TEXT_LENGTH)) },
                placeholder = stringResource(R.string.aac_type_placeholder),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )
            OriSecondaryButton(
                text = stringResource(R.string.aac_type_add),
                onClick = add,
                enabled = canAdd,
                isFullWidth = false
            )
        }
        if (onSaveWord != null) {
            OriTextButton(
                text = stringResource(R.string.aac_save_word),
                onClick = { onSaveWord(text.trim()) },
                enabled = text.isNotBlank()
            )
        }
    }
}
