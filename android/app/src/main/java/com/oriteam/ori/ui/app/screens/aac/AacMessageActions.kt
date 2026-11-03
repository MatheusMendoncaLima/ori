package com.oriteam.ori.ui.app.screens.aac

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.ui.components.form.EqualWidthRow
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/** On a tablet the actions stay together instead of stretching across the whole screen. */
private val ActionsMaxWidth = 360.dp
private val ActionSpacing = OriSpacing.ExtraSmall
private val ActionPadding = PaddingValues(horizontal = 2.dp, vertical = OriSpacing.ExtraSmall)

/**
 * The actions of the message at the bottom of the board: "Apagar" (last piece), "Salvar", "Falar" and,
 * for what is used less, "Mais". Each keeps an icon and a label, because not everyone reads and not
 * every icon is obvious. Erasing the whole sentence is in "Mais", so it is no longer a slip of a finger.
 */
@Composable
fun AacMessageActions(
    hasMessage: Boolean,
    speechState: SpeechState,
    canSave: Boolean,
    actions: AacMessageBarActions,
    modifier: Modifier = Modifier
) {
    EqualWidthRow(
        spacing = ActionSpacing,
        modifier = modifier
            .widthIn(max = ActionsMaxWidth)
            .fillMaxWidth()
    ) {
        val action = Modifier.heightIn(min = OriDimensions.ControlMinHeight)
        SecondaryAction(
            icon = ImageVector.vectorResource(R.drawable.ic_backspace),
            label = stringResource(R.string.aac_remove_last_short),
            description = stringResource(R.string.aac_remove_last),
            onClick = actions.onRemoveLast,
            enabled = hasMessage,
            modifier = action
        )
        SecondaryAction(
            icon = Icons.Filled.Star,
            label = stringResource(R.string.aac_save_short),
            description = stringResource(R.string.aac_save_message),
            onClick = actions.onSave,
            enabled = canSave,
            isActive = canSave,
            modifier = action
        )
        SpeakAction(hasMessage = hasMessage, speechState = speechState, actions = actions, modifier = action)
        MoreAction(hasMessage = hasMessage, actions = actions)
    }
}

/** "Falar" is the main action, so it is the only filled button. While speaking it becomes "Parar". */
@Composable
private fun SpeakAction(
    hasMessage: Boolean,
    speechState: SpeechState,
    actions: AacMessageBarActions,
    modifier: Modifier
) {
    val isSpeaking = speechState == SpeechState.SPEAKING
    Button(
        onClick = if (isSpeaking) actions.onStop else actions.onSpeak,
        enabled = isSpeaking || hasMessage,
        shape = MaterialTheme.shapes.medium,
        contentPadding = ActionPadding,
        modifier = modifier
    ) {
        ActionContent(
            icon = if (isSpeaking) Icons.Filled.Close else Icons.Filled.PlayArrow,
            label = stringResource(if (isSpeaking) R.string.aac_stop else R.string.aac_speak)
        )
    }
}

@Composable
private fun SecondaryAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    isActive: Boolean = false,
    description: String? = null
) {
    val colors = if (isActive) {
        ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    } else {
        ButtonDefaults.outlinedButtonColors()
    }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = colors,
        contentPadding = ActionPadding,
        modifier = modifier
    ) {
        ActionContent(icon = icon, label = label, description = description)
    }
}

/** A short label on screen may have a fuller name for screen readers ([description]). */
@Composable
private fun ActionContent(icon: ImageVector, label: String, description: String? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (description == null) {
            Modifier
        } else {
            Modifier.clearAndSetSemantics { contentDescription = description }
        }
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(text = label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MoreAction(hasMessage: Boolean, actions: AacMessageBarActions) {
    var isOpen by remember { mutableStateOf(false) }
    Box {
        SecondaryAction(
            icon = Icons.Filled.MoreVert,
            label = stringResource(R.string.aac_more),
            description = stringResource(R.string.aac_more_description),
            onClick = { isOpen = true },
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = OriDimensions.ControlMinHeight)
        )
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            MoreMenuItem(ImageVector.vectorResource(R.drawable.ic_visibility), R.string.aac_show, hasMessage) {
                isOpen = false
                actions.onShow()
            }
            MoreMenuItem(Icons.Filled.Delete, R.string.aac_clear, hasMessage) {
                isOpen = false
                actions.onClear()
            }
            MoreMenuItem(Icons.Filled.Edit, R.string.aac_organize, enabled = true) {
                isOpen = false
                actions.onOrganize()
            }
            MoreMenuItem(Icons.Filled.Settings, R.string.aac_preferences, enabled = true) {
                isOpen = false
                actions.onPreferences()
            }
        }
    }
}

@Composable
private fun MoreMenuItem(icon: ImageVector, @StringRes textRes: Int, enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(textRes), style = MaterialTheme.typography.labelLarge) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null) },
        enabled = enabled,
        onClick = onClick
    )
}
