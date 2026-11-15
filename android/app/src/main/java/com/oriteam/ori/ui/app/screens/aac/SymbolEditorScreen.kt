package com.oriteam.ori.ui.app.screens.aac

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SymbolColor
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.aac.WordClass
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.adaptive.FillOrScrollColumn
import com.oriteam.ori.ui.components.form.EqualWidthRow
import com.oriteam.ori.ui.components.form.FormActionBar
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val PreviewSize = 96.dp

/** What the symbol form can ask for. */
data class SymbolEditorActions(
    val onLabelChange: (String) -> Unit,
    val onPhraseChange: (String) -> Unit,
    val onEmojiChange: (String) -> Unit,
    val onPhotoPicked: (String) -> Unit,
    val onRemoveImage: () -> Unit,
    val onColorChange: (SymbolColor?) -> Unit = {},
    val onWordClassChange: (WordClass?) -> Unit = {},
    val onSave: () -> Unit,
    val onDelete: (() -> Unit)?,
    val onClose: () -> Unit
)

/**
 * Form of one symbol: label (shown), associated phrase (spoken) and picture (emoji, a photo
 * from this device, or none). Photos are chosen with the system photo picker, which needs no
 * storage permission and only gives the app the one picture chosen.
 */
@Composable
fun SymbolEditorScreen(state: SymbolEditorUiState, actions: SymbolEditorActions) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { actions.onPhotoPicked(it.toString()) }
    }
    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime)) {
        OriTopBar(
            maxWidth = OriDimensions.FormMaxWidth,
            title = stringResource(if (state.isNew) R.string.aac_new_symbol else R.string.aac_edit_symbol),
            onNavigateBack = actions.onClose
        )
        FillOrScrollColumn(modifier = Modifier.weight(1f)) {
            CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
                LabeledTextField(
                    label = stringResource(R.string.aac_symbol_label),
                    value = state.labelInput,
                    onValueChange = actions.onLabelChange,
                    placeholder = stringResource(R.string.aac_symbol_label_placeholder),
                    keyboardOptions = KeyboardOptions(KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions.Default
                )
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                LabeledTextField(
                    label = stringResource(R.string.aac_symbol_phrase),
                    value = state.phraseInput,
                    onValueChange = actions.onPhraseChange,
                    placeholder = stringResource(R.string.aac_symbol_phrase_placeholder),
                    keyboardOptions = KeyboardOptions(KeyboardCapitalization.Sentences),
                    keyboardActions = KeyboardActions.Default,
                    singleLine = false
                )
                OriNotice(stringResource(R.string.aac_symbol_phrase_help))
                SectionHeader(text = stringResource(R.string.aac_symbol_picture))
                state.imagePreview?.let { SymbolPicture(image = it, size = PreviewSize) }
                    ?: OriNotice(stringResource(R.string.aac_symbol_no_picture))
                LabeledTextField(
                    label = stringResource(R.string.aac_symbol_emoji),
                    value = (state.image as? SymbolImage.Emoji)?.value.orEmpty(),
                    onValueChange = actions.onEmojiChange,
                    placeholder = stringResource(R.string.aac_symbol_emoji_placeholder),
                    keyboardOptions = KeyboardOptions.Default,
                    keyboardActions = KeyboardActions.Default
                )
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                EmojiSuggestions(
                    selected = (state.image as? SymbolImage.Emoji)?.value,
                    onSelect = actions.onEmojiChange
                )
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                PictureButtons(
                    state = state,
                    onPickPhoto = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemoveImage = actions.onRemoveImage
                )
                WordClassChoice(selected = state.wordClass, onSelect = actions.onWordClassChange)
                SymbolColorChoice(selected = state.color, onSelect = actions.onColorChange)
                FormMessage(messageRes = state.errorMessageRes, type = FormMessageType.ERROR)
                // Deleting stays at the end of the form, away from "Salvar": it cannot be undone.
                actions.onDelete?.let { onDelete ->
                    Spacer(modifier = Modifier.height(OriSpacing.Medium))
                    OriTextButton(
                        text = stringResource(R.string.aac_delete_symbol),
                        onClick = onDelete,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
        FormActionBar(
            saveLabel = stringResource(R.string.aac_save_symbol),
            isSaving = state.isSaving,
            onSave = actions.onSave,
            onCancel = actions.onClose
        )
    }
}

@Composable
private fun PictureButtons(state: SymbolEditorUiState, onPickPhoto: () -> Unit, onRemoveImage: () -> Unit) {
    EqualWidthRow(modifier = Modifier.fillMaxWidth()) {
        if (state.canUsePhotos) {
            OriSecondaryButton(
                text = stringResource(
                    if (state.isImporting) R.string.aac_photo_importing else R.string.aac_choose_photo
                ),
                onClick = onPickPhoto,
                enabled = !state.isImporting
            )
        }
        OriSecondaryButton(
            text = stringResource(R.string.aac_remove_picture),
            onClick = onRemoveImage,
            enabled = state.image != null
        )
    }
    OriNotice(
        stringResource(if (state.canUsePhotos) R.string.aac_photo_device_note else R.string.aac_photo_caregiver_note)
    )
}
