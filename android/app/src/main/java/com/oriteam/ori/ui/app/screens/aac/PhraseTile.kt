package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.list.GridReorderState
import com.oriteam.ori.ui.components.list.reorderable
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val PhrasePictureSize = 32.dp

/** A phrase of Frases: its picture, its text and what it offers at the end ([trailing]). One tap says it. */
@Composable
internal fun PhraseTile(
    text: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    OriCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = OriDimensions.MinTouchTarget)
        ) {
            SymbolPicture(image = SymbolImageUi.Emoji(emoji), size = PhrasePictureSize)
            Spacer(modifier = Modifier.width(OriSpacing.Small))
            Text(text = text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

/**
 * Drag to move [item] among [shown] (the order on screen; [stored] is the order as kept), and "move before/after" for
 * screen readers, who cannot drag. [text] names the phrase in those actions.
 */
@Composable
internal fun <T> Modifier.movable(
    item: T,
    text: String,
    shown: List<T>,
    stored: List<T>,
    drag: GridReorderState<T>,
    onMove: (T, Int) -> Unit
): Modifier {
    val index = shown.indexOf(item)
    val before = stringResource(R.string.aac_move_earlier, text)
    val after = stringResource(R.string.aac_move_later, text)
    return this
        .semantics {
            customActions = listOfNotNull(
                CustomAccessibilityAction(before) {
                    onMove(item, index - 1)
                    true
                }.takeIf { index > 0 },
                CustomAccessibilityAction(after) {
                    onMove(item, index + 1)
                    true
                }.takeIf { index < shown.lastIndex }
            )
        }
        .reorderable(item, stored, drag, onMove = onMove)
}
