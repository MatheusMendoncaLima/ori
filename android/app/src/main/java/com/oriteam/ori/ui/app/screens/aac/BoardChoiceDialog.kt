package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.dialog.OriAlertDialog
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * "Mudar prancha", from the "Mais" menu: every board one tap away, the open one marked, near the hand that
 * builds the message. The chips above the grid do the same, but only appear with two boards or more and sit
 * at the other end of the screen. "Organizar pranchas" is here too, to make another board.
 */
@Composable
fun BoardChoiceDialog(
    boards: List<BoardTabUiModel>,
    selectedId: String?,
    onChoose: (String) -> Unit,
    onOrganize: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    OriAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.aac_choose_board_title),
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                boards.forEach { board ->
                    val isSelected = board.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = OriDimensions.MinTouchTarget)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onChoose(board.id) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(OriSpacing.Medium))
                        Text(text = board.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOrganize) {
                Text(text = stringResource(R.string.aac_organize), style = MaterialTheme.typography.labelLarge)
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
        titleContentColor = colors.onSurface
    )
}
