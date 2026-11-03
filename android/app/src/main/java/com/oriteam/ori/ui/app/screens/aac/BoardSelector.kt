package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.oriteam.ori.ui.components.form.OriChoiceChip
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Chooses the board shown below the message. A scrolling row keeps every board one tap away
 * without hiding names behind a menu; with a single board there is nothing to choose, so it is gone.
 */
@Composable
fun BoardSelector(
    boards: List<BoardTabUiModel>,
    selection: AacSelection?,
    onSelect: (AacSelection) -> Unit,
    modifier: Modifier = Modifier
) {
    if (boards.size < 2) return
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small)
    ) {
        boards.forEach { board ->
            val option = AacSelection.Board(board.id)
            OriChoiceChip(
                label = board.name,
                isSelected = selection == option,
                onClick = { onSelect(option) },
                role = Role.Tab
            )
        }
    }
}
