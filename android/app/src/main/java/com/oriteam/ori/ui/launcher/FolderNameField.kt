package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.MAX_FOLDER_NAME
import com.oriteam.ori.ui.components.form.UppercaseVisualTransformation
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * The name of an open folder, at its top, changed right there as on One UI: a tap shows the keyboard. What is typed is
 * kept on the screen and written when the keyboard is done or the folder closes, not at each letter; a blank name
 * keeps the old one. On a locked home screen it is only read.
 */
@Composable
internal fun FolderNameField(
    folder: HomeEntry.Folder,
    isLocked: Boolean,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable(folder.id) { mutableStateOf(folder.name) }
    var isFocused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val written by rememberUpdatedState(folder.name)
    val typed by rememberUpdatedState(name)
    val rename by rememberUpdatedState(onRename)
    DisposableEffect(folder.id) { onDispose { if (typed.isNotBlank() && typed != written) rename(typed) } }
    val label = stringResource(R.string.launcher_folder_name)
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = name,
        onValueChange = { name = it.take(MAX_FOLDER_NAME) },
        readOnly = isLocked,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(colors.primary),
        visualTransformation = if (LocalOriAppearance.current.isUppercase) {
            UppercaseVisualTransformation
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (name.isBlank()) name = folder.name else onRename(name)
                focus.clearFocus()
            }
        ),
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .semantics { contentDescription = label },
        decorationBox = { field ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = OriDimensions.MinTouchTarget)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (isFocused) colors.surfaceContainerHighest else Color.Transparent)
                    .padding(horizontal = OriSpacing.Small)
            ) {
                if (name.isEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                field()
            }
        }
    )
}
