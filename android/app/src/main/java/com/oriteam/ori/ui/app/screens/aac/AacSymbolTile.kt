package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.preferences.SymbolLabelMode
import com.oriteam.ori.ui.components.motion.PRESSED_SYMBOL_SCALE
import com.oriteam.ori.ui.components.motion.pressScale
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Large, tappable symbol, announced once as a button with its label. Screen readers hear the
 * label even when the person chose to hide it, and the tile grows with enlarged text instead of
 * cutting words.
 */
@Composable
fun AacSymbolTile(
    symbol: AacSymbolUiModel,
    labelMode: SymbolLabelMode,
    spec: AacGridSpec,
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    /** The height of a tile with a picture, so a word without one is as tall as its neighbors. */
    minHeight: Dp = OriDimensions.MinTouchTarget * 2
) {
    val colors = MaterialTheme.colorScheme
    // Without a picture the label is the only content, whatever the chosen mode.
    val effectiveMode = if (symbol.image == null) SymbolLabelMode.TEXT_ONLY else labelMode
    val interaction = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .pressScale(interaction, PRESSED_SYMBOL_SCALE)
            .clip(MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClickLabel = onClickLabel,
                role = Role.Button,
                onClick = onClick
            )
            // The visible label is read as the tile's text; a hidden one becomes its description.
            .semantics { if (effectiveMode == SymbolLabelMode.IMAGE_ONLY) contentDescription = symbol.label },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = colors.onSurface),
        border = BorderStroke(1.dp, colors.outlineVariant).takeIf { containerColor == colors.surface }
    ) {
        Column(
            // The minimum height is on this column, so a label without a picture stays centered.
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                // No side padding: in a narrow tile those few dp would break a word in two.
                .padding(vertical = OriSpacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OriSpacing.ExtraSmall, Alignment.CenterVertically)
        ) {
            val labelStyle = when {
                spec.usesSmallLabels -> MaterialTheme.typography.labelSmall
                effectiveMode == SymbolLabelMode.TEXT_ONLY -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleSmall
            }
            val label: @Composable () -> Unit = {
                Text(text = symbol.label, style = labelStyle, textAlign = TextAlign.Center)
            }
            if (effectiveMode == SymbolLabelMode.ABOVE_IMAGE) label()
            if (effectiveMode != SymbolLabelMode.TEXT_ONLY) {
                symbol.image?.let { SymbolPicture(image = it, size = spec.imageSize) }
            }
            if (effectiveMode == SymbolLabelMode.BELOW_IMAGE || effectiveMode == SymbolLabelMode.TEXT_ONLY) label()
        }
    }
}
