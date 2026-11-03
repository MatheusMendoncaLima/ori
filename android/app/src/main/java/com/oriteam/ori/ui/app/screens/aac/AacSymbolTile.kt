package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.WordClass
import com.oriteam.ori.domain.aac.wordClassOf
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.SymbolLabelMode
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
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
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    val colors = MaterialTheme.colorScheme
    // Without a picture the label is the only content, whatever the chosen mode.
    val effectiveMode = if (symbol.image == null) SymbolLabelMode.TEXT_ONLY else labelMode
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
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
                .heightIn(min = OriDimensions.MinTouchTarget * 2)
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

/**
 * Grid of symbols that decides its columns from the width it actually receives (not the
 * window), the person's symbol size and the system text size. See [aacGridSpec].
 */
@Composable
fun AacBoardGrid(
    symbols: List<AacSymbolUiModel>,
    preferences: AacPreferences,
    onSymbolClick: (AacSymbolUiModel) -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    onAddSymbol: (() -> Unit)? = null
) {
    val baseTileWidth = LocalWindowLayout.current.aacSymbolMinWidth
    val fontScale = LocalDensity.current.fontScale
    val spacing = LocalOriAppearance.current.metrics.symbolSpacing
    val showsColors = LocalOriAppearance.current.showsDecoration
    val accents = LocalOriColors.current.accents
    BoxWithConstraints(modifier = modifier) {
        val spec = aacGridSpec(
            maxWidth,
            baseTileWidth,
            preferences.symbolSize,
            preferences.labelMode,
            fontScale,
            spacing
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(spec.columns),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items(items = symbols, key = { symbol -> symbol.id }) { symbol ->
                AacSymbolTile(
                    symbol = symbol,
                    labelMode = preferences.labelMode,
                    spec = spec,
                    onClick = { onSymbolClick(symbol) },
                    onClickLabel = onClickLabel,
                    // The color says the kind of word; simplified screens stay plain.
                    containerColor = if (showsColors) {
                        wordClassAccent(wordClassOf(symbol.label))?.let { accents[it].container }
                            ?: MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            }
            onAddSymbol?.let { add -> item(key = "add-symbol") { AddSymbolTile(onClick = add) } }
        }
    }
}

/**
 * People in yellow, actions in green and feelings in blue, as in the color keys of communication boards.
 * Other words stay plain: a color that means nothing would only add noise.
 */
private fun wordClassAccent(wordClass: WordClass): Int? = when (wordClass) {
    WordClass.PERSON -> YELLOW_ACCENT
    WordClass.ACTION -> GREEN_ACCENT
    WordClass.FEELING -> BLUE_ACCENT
    WordClass.OTHER -> null
}

private const val GREEN_ACCENT = 0
private const val YELLOW_ACCENT = 2
private const val BLUE_ACCENT = 3

/** The way to a word that is missing, at the end of the words, where it is looked for. */
@Composable
private fun AddSymbolTile(onClick: () -> Unit) {
    val label = stringResource(R.string.aac_add_word)
    OutlinedCard(onClick = onClick, shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = OriDimensions.MinTouchTarget * 2)
                .padding(vertical = OriSpacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Text(text = label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        }
    }
}
