package com.oriteam.ori.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.components.motion.EntranceStyle
import com.oriteam.ori.ui.components.motion.entrance
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

/** The width of a dialog, as Material's: never a narrow column, never a whole tablet. */
private val DialogMinWidth = 280.dp
private val DialogMaxWidth = 560.dp

/**
 * A question or a short choice over the screen, with the look of Material's `AlertDialog` (the same slots and colors),
 * drawn by [OriDialog] in the window of the screen: it opens without Android creating a window for it. [title] is also
 * what the screen reader says when it opens ([paneTitle], for a dialog without a title). The buttons go at the end,
 * [dismissButton] before [confirmButton], and move to more lines when they do not fit.
 */
@Composable
fun OriAlertDialog(
    onDismissRequest: () -> Unit,
    title: String?,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    paneTitle: String = title.orEmpty(),
    containerColor: Color = AlertDialogDefaults.containerColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    text: (@Composable () -> Unit)? = null
) {
    OriDialog(onDismissRequest = onDismissRequest, paneTitle = paneTitle) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = modifier
                .widthIn(min = DialogMinWidth, max = DialogMaxWidth)
                .entrance(style = EntranceStyle.POP)
        ) {
            Column(modifier = Modifier.padding(OriSpacing.Large)) {
                title?.let {
                    Styled(titleContentColor, MaterialTheme.typography.headlineSmall) {
                        Text(text = it, modifier = Modifier.padding(bottom = OriSpacing.Medium))
                    }
                }
                text?.let { DialogText(textContentColor, it) }
                Box(modifier = Modifier.align(Alignment.End)) {
                    Styled(MaterialTheme.colorScheme.primary, MaterialTheme.typography.labelLarge) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small, Alignment.End),
                            verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)
                        ) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

/** The body of the dialog: it takes the room left by the title and the buttons, and scrolls inside it when long. */
@Composable
private fun ColumnScope.DialogText(color: Color, text: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f, fill = false)
            .padding(bottom = OriSpacing.Large)
            .align(Alignment.Start)
    ) {
        Styled(color, MaterialTheme.typography.bodyMedium, text)
    }
}

@Composable
private fun Styled(color: Color, style: TextStyle, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides color, LocalTextStyle provides style, content = content)
}
