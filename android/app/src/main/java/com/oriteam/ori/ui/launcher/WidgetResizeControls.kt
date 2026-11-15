package com.oriteam.ori.ui.launcher

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.motion.EntranceStyle
import com.oriteam.ori.ui.components.motion.entrance
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

private val ControlsMinWidth = 240.dp

/**
 * The card of a widget being resized, over it (under it when there is no room above), as the menu is: its width and
 * its height in cells, each with a button to take one cell away and one to add one (offered only when that size is
 * possible), and "Pronto". It does not take the focus, so the handles of the frame keep working while it shows.
 */
@Composable
internal fun WidgetResizeControls(sizes: WidgetSizes, onSize: (HomeSpan) -> Unit, onDone: () -> Unit) {
    val density = LocalDensity.current
    val bars = WindowInsets.safeDrawing
    val top = bars.getTop(density)
    val bottom = bars.getBottom(density)
    val gap = with(density) { OriSpacing.Small.roundToPx() }
    val position = remember(top, bottom, gap) { AboveFirst(top = top, bottom = bottom, gap = gap) }
    val span = sizes.current
    Popup(popupPositionProvider = position, properties = PopupProperties(focusable = false)) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = MenuDefaults.ShadowElevation,
            modifier = Modifier.entrance(style = EntranceStyle.POP)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .widthIn(min = ControlsMinWidth)
                    .padding(OriSpacing.Small)
            ) {
                SizeRow(
                    label = stringResource(R.string.launcher_resize_width),
                    count = span.columns,
                    less = Step(R.string.launcher_resize_narrower, span.copy(columns = span.columns - 1)),
                    more = Step(R.string.launcher_resize_wider, span.copy(columns = span.columns + 1)),
                    sizes = sizes,
                    onSize = onSize
                )
                SizeRow(
                    label = stringResource(R.string.launcher_resize_height),
                    count = span.rows,
                    less = Step(R.string.launcher_resize_shorter, span.copy(rows = span.rows - 1)),
                    more = Step(R.string.launcher_resize_taller, span.copy(rows = span.rows + 1)),
                    sizes = sizes,
                    onSize = onSize
                )
                OriTextButton(text = stringResource(R.string.launcher_done), onClick = onDone)
            }
        }
    }
}

/** A size one cell away, and what its button says. */
private class Step(@StringRes val label: Int, val span: HomeSpan)

@Composable
private fun SizeRow(
    label: String,
    count: Int,
    less: Step,
    more: Step,
    sizes: WidgetSizes,
    onSize: (HomeSpan) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label + ": " + pluralStringResource(R.plurals.launcher_resize_cells, count, count),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        StepButton(less, OriIcons.Remove, sizes, onSize)
        StepButton(more, Icons.Filled.Add, sizes, onSize)
    }
}

@Composable
private fun StepButton(step: Step, icon: ImageVector, sizes: WidgetSizes, onSize: (HomeSpan) -> Unit) {
    IconButton(onClick = { onSize(step.span) }, enabled = sizes.canTake(step.span)) {
        Icon(icon, contentDescription = stringResource(step.label))
    }
}
