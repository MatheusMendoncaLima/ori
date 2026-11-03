package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.form.EqualWidthRow
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.list.OriListItem
import com.oriteam.ori.ui.components.list.OriOpenArrow
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.StatefulContent
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val SymbolRowPictureSize = 40.dp

/** The person's boards, with order buttons. Without permission it explains why nothing can change. */
@Composable
fun BoardsPane(
    state: AacOrganizeUiState,
    title: String,
    onNavigateBack: (() -> Unit)?,
    onOpenBoard: (String) -> Unit,
    onMoveBoard: (String, Int) -> Unit,
    onCreateBoard: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        OriTopBar(title = title, onNavigateBack = onNavigateBack)
        CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
            FormMessage(messageRes = state.actionErrorRes, type = FormMessageType.ERROR)
            state.syncNoticeRes?.let { OriNotice(stringResource(it), isLive = true) }
            if (state.canManage) {
                OriPrimaryButton(text = stringResource(R.string.aac_new_board), onClick = onCreateBoard)
            }
        }
        StatefulContent(
            state = state.boards,
            onRetry = onRetry,
            emptyContent = {
                EmptyState(
                    title = stringResource(R.string.aac_no_boards_title),
                    message = stringResource(R.string.aac_organize_empty_message)
                )
            },
            modifier = Modifier.weight(1f)
        ) { boards ->
            OrganizerList {
                items(boards, key = { it.item.id }) { row ->
                    OriListItem(
                        headline = row.item.name,
                        isSelected = row.item.id == state.selectedBoardId,
                        onClick = { onOpenBoard(row.item.id) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.canManage) {
                                    MoveButtons(row.item.name, row, onMove = { onMoveBoard(row.item.id, it) })
                                }
                                // Tapping the board opens its symbols.
                                OriOpenArrow()
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Symbols of one board: add, edit (tap), reorder; rename or delete the board. */
@Composable
fun SymbolsPane(
    state: AacOrganizeUiState,
    onNavigateBack: (() -> Unit)?,
    onEditSymbol: (String) -> Unit,
    onAddSymbol: () -> Unit,
    onMoveSymbol: (String, Int) -> Unit,
    onRenameBoard: () -> Unit,
    onDeleteBoard: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        OriTopBar(title = state.selectedBoardName.orEmpty(), onNavigateBack = onNavigateBack)
        if (state.canManage) {
            CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                OriPrimaryButton(text = stringResource(R.string.aac_add_symbol), onClick = onAddSymbol)
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                EqualWidthRow(modifier = Modifier.fillMaxWidth()) {
                    OriSecondaryButton(text = stringResource(R.string.aac_rename_board), onClick = onRenameBoard)
                    OriSecondaryButton(
                        text = stringResource(R.string.aac_delete_board),
                        onClick = onDeleteBoard,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        StatefulContent(
            state = state.symbols,
            onRetry = onRetry,
            emptyContent = {
                EmptyState(
                    title = stringResource(R.string.aac_board_empty_title),
                    message = stringResource(R.string.aac_organize_board_empty_message)
                )
            },
            modifier = Modifier.weight(1f)
        ) { symbols ->
            OrganizerList {
                items(symbols, key = { it.item.id }) { row ->
                    SymbolRow(row, state.canManage, onEditSymbol, onMoveSymbol)
                }
            }
        }
    }
}

@Composable
private fun SymbolRow(
    row: OrganizerItemUiModel<AacSymbolUiModel>,
    canManage: Boolean,
    onEditSymbol: (String) -> Unit,
    onMoveSymbol: (String, Int) -> Unit
) {
    val symbol = row.item
    OriListItem(
        headline = symbol.label,
        supportingText = stringResource(R.string.aac_symbol_says, symbol.spokenText),
        onClick = if (canManage) ({ onEditSymbol(symbol.id) }) else null,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                symbol.image?.let { SymbolPicture(image = it, size = SymbolRowPictureSize) }
                if (canManage) MoveButtons(symbol.label, row, onMove = { onMoveSymbol(symbol.id, it) })
            }
        }
    )
}

/** Boards or symbols, with the same margin and width as the other lists of the app. */
@Composable
private fun OrganizerList(content: LazyListScope.() -> Unit) {
    CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = OriSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(LocalOriAppearance.current.metrics.listSpacing),
            content = content
        )
    }
}

/** Accessible reordering: two buttons with clear names instead of a drag gesture. */
@Composable
private fun MoveButtons(name: String, row: OrganizerItemUiModel<*>, onMove: (Int) -> Unit) {
    Row {
        IconButton(onClick = { onMove(-1) }, enabled = row.canMoveEarlier) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.aac_move_earlier, name))
        }
        IconButton(onClick = { onMove(1) }, enabled = row.canMoveLater) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.aac_move_later, name))
        }
    }
}
