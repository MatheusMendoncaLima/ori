package com.oriteam.ori.ui.components.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/*
 * The buttons of the app. They differ only in emphasis: same height, same text style and full width
 * by default, so two buttons together always line up. Side by side, use [EqualWidthRow]. A compact
 * button next to other content (in a row of a list, beside a field) says so with `isFullWidth = false`.
 */

private fun Modifier.buttonSize(isFullWidth: Boolean) =
    (if (isFullWidth) fillMaxWidth() else this).heightIn(min = OriDimensions.ControlMinHeight)

/** The main action of a screen. For an action that takes time, use [LoadingButton]. */
@Composable
fun OriPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isFullWidth: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier.buttonSize(isFullWidth)
    ) {
        ButtonLabel(text)
    }
}

/** An alternative to the main action. [color] marks a destructive one. */
@Composable
fun OriSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
    isFullWidth: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier.buttonSize(isFullWidth)
    ) {
        ButtonLabel(text, color.takeIf { enabled } ?: Color.Unspecified)
    }
}

/** The least emphasis: cancel, sign out, try again. It keeps the same touch area as the others. */
@Composable
fun OriTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
    isFullWidth: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.buttonSize(isFullWidth)
    ) {
        ButtonLabel(text, color.takeIf { enabled } ?: Color.Unspecified)
    }
}

@Composable
private fun ButtonLabel(text: String, color: Color = Color.Unspecified) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = color, textAlign = TextAlign.Center)
}

/**
 * Buttons side by side with the same width and height, whatever their labels say. All in one row if
 * the longest label fits in an equal share, otherwise two per row, or one. So enlarged text moves a
 * button to a new row and never breaks a word: a flow layout with weights would first give each one
 * an equal share, and only then find out that a word does not fit.
 */
@Composable
fun EqualWidthRow(
    modifier: Modifier = Modifier,
    spacing: Dp = OriSpacing.Small,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        if (measurables.isEmpty()) return@Layout layout(0, 0) {}
        val gap = spacing.roundToPx()
        val widest = measurables.maxOf { it.maxIntrinsicWidth(constraints.maxHeight) }
        val perRow = listOf(measurables.size, 2)
            .firstOrNull { count -> count * widest + (count - 1) * gap <= constraints.maxWidth }
            ?: 1
        val itemWidth = (constraints.maxWidth - (perRow - 1) * gap) / perRow
        // The tallest button of the whole group, so no two buttons differ, even across rows.
        val itemHeight = measurables.maxOf { it.minIntrinsicHeight(itemWidth) }
        val items = measurables.map { it.measure(Constraints.fixed(itemWidth, itemHeight)) }
        val rows = items.chunked(perRow)
        layout(constraints.maxWidth, rows.size * itemHeight + (rows.size - 1) * gap) {
            rows.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { index, item ->
                    item.placeRelative(index * (itemWidth + gap), rowIndex * (itemHeight + gap))
                }
            }
        }
    }
}
