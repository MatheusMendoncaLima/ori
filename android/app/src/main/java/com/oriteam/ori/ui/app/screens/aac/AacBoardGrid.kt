package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SentenceGrammar
import com.oriteam.ori.domain.aac.SymbolColor
import com.oriteam.ori.domain.aac.WordClass
import com.oriteam.ori.domain.aac.appGrammar
import com.oriteam.ori.domain.preferences.AacPreferences
import com.oriteam.ori.domain.preferences.SymbolLabelMode
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.components.list.GridReorderState
import com.oriteam.ori.ui.components.list.rememberGridReorderState
import com.oriteam.ori.ui.components.list.reorderable
import com.oriteam.ori.ui.components.motion.itemAnimation
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriAccent
import com.oriteam.ori.ui.theme.OriAreaAccents
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriExtraColors
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.OriWordColors

/**
 * Grid of symbols that decides its columns from the width it actually receives (not the
 * window), the person's symbol size and the system text size. See [aacGridSpec].
 *
 * With [onMoveSymbol], holding a tile and dragging it changes its place; the other tiles make room as it passes,
 * and the new order is saved when the finger lifts. Screen readers get "move before" and "move after" actions on
 * each tile instead, since dragging is not possible for everyone.
 */
@Composable
fun AacBoardGrid(
    symbols: List<AacSymbolUiModel>,
    preferences: AacPreferences,
    onSymbolClick: (AacSymbolUiModel) -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    onAddSymbol: (() -> Unit)? = null,
    listKey: Any? = null,
    /** The grammar of the board's language, which knows the kind of each word (the color of its tile). */
    grammar: SentenceGrammar = appGrammar,
    /** The symbol and the index it goes to; null when the order cannot be changed here. */
    onMoveSymbol: ((symbolId: String, toIndex: Int) -> Unit)? = null
) {
    val baseTileWidth = LocalWindowLayout.current.aacSymbolMinWidth
    val fontScale = LocalDensity.current.fontScale
    val spacing = LocalOriAppearance.current.metrics.symbolSpacing
    val showsColors = LocalOriAppearance.current.showsDecoration
    val extraColors = LocalOriColors.current
    val surface = MaterialTheme.colorScheme.surface
    BoxWithConstraints(modifier = modifier) {
        val spec = aacGridSpec(
            maxWidth,
            baseTileWidth,
            preferences.symbolSize,
            preferences.labelMode,
            fontScale,
            spacing
        )
        val tileHeight = tileMinHeight(spec, preferences.labelMode)
        // Another board is another grid: it shows at once, instead of every symbol fading out and in.
        key(listKey) {
            val gridState = rememberLazyGridState()
            val drag = rememberGridReorderState<AacSymbolUiModel>(gridState) { it.id }
            // While a tile is dragged the grid shows the order being built; otherwise, the order as read.
            val shown = drag.shown(symbols)
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(spec.columns),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(items = shown, key = { symbol -> symbol.id }) { symbol ->
                    val isMoving = drag.isMoving(symbol)
                    AacSymbolTile(
                        modifier = (
                            if (isMoving) {
                                Modifier
                            } else {
                                Modifier.itemAnimation(
                                    this,
                                    LocalOriAppearance.current.motion
                                )
                            }
                            )
                            .then(
                                if (onMoveSymbol == null) {
                                    Modifier
                                } else {
                                    Modifier.movable(symbol, shown, drag, onMoveSymbol, symbols)
                                }
                            ),
                        symbol = symbol,
                        labelMode = preferences.labelMode,
                        spec = spec,
                        onClick = { onSymbolClick(symbol) },
                        onClickLabel = onClickLabel,
                        minHeight = tileHeight,
                        // The color the person chose, or the one that says the kind of word; simplified screens
                        // stay plain.
                        containerColor = if (showsColors) {
                            val wordClass = symbol.wordClass ?: grammar.wordClassOf(symbol.label)
                            extraColors.tileColorOf(symbol.color, wordClass) ?: surface
                        } else {
                            surface
                        }
                    )
                }
                onAddSymbol?.let { add ->
                    item(key = "add-symbol") {
                        Box(
                            Modifier.itemAnimation(this, LocalOriAppearance.current.motion)
                        ) { AddSymbolTile(onClick = add, minHeight = tileHeight) }
                    }
                }
            }
        }
    }
}

/** Drag to move, and "move before/after" for screen readers, who cannot drag. */
@Composable
private fun Modifier.movable(
    symbol: AacSymbolUiModel,
    shown: List<AacSymbolUiModel>,
    drag: GridReorderState<AacSymbolUiModel>,
    onMove: (String, Int) -> Unit,
    symbols: List<AacSymbolUiModel>
): Modifier {
    val index = shown.indexOf(symbol)
    val before = stringResource(R.string.aac_move_symbol_before, symbol.label)
    val after = stringResource(R.string.aac_move_symbol_after, symbol.label)
    return this
        .semantics {
            customActions = listOfNotNull(
                CustomAccessibilityAction(before) {
                    onMove(symbol.id, index - 1)
                    true
                }.takeIf { index > 0 },
                CustomAccessibilityAction(after) {
                    onMove(symbol.id, index + 1)
                    true
                }.takeIf { index < shown.lastIndex }
            )
        }
        .reorderable(symbol, symbols, drag, onMove = { moved, toIndex -> onMove(moved.id, toIndex) })
}

/**
 * The color of a tile: the one the person chose ([SymbolColor.NONE] is no color), or, without a choice, the one of
 * the kind of word, as in the color keys of communication boards: people in yellow, actions in green, feelings in
 * blue and answers and courtesy in pink ([OriWordColors]). Other words stay plain: a color that means nothing would
 * only add noise.
 */
internal fun OriExtraColors.tileColorOf(chosen: SymbolColor?, wordClass: WordClass): Color? =
    if (chosen == null) words.of(wordClass)?.container else accentOf(chosen)?.container

/** The soft colors a tile may get, the same ones the rest of the app uses. */
internal fun OriExtraColors.accentOf(color: SymbolColor): OriAccent? = when (color) {
    SymbolColor.YELLOW -> words.person
    SymbolColor.MINT -> words.action
    SymbolColor.BLUE -> words.feeling
    SymbolColor.PINK -> words.social
    SymbolColor.ORANGE -> accents[OriAreaAccents.TASKS]
    SymbolColor.LILAC -> accents[OriAreaAccents.COMMUNICATION]
    SymbolColor.GREEN -> accents[OriAreaAccents.CARE]
    SymbolColor.NONE -> null
}

private fun OriWordColors.of(wordClass: WordClass): OriAccent? = when (wordClass) {
    WordClass.PERSON -> person
    WordClass.ACTION -> action
    WordClass.FEELING -> feeling
    WordClass.SOCIAL -> social
    WordClass.OTHER -> null
}

/**
 * The height of a tile with a picture and one line of label, given to every tile of the grid: a word without a
 * picture ("com") and the "Nova palavra" tile used to be shorter than their neighbors. With text only there are no
 * pictures, and the tiles keep the usual minimum.
 */
@Composable
private fun tileMinHeight(spec: AacGridSpec, labelMode: SymbolLabelMode): Dp {
    val typography = MaterialTheme.typography
    val labelStyle = if (spec.usesSmallLabels) typography.labelSmall else typography.titleSmall
    val labelHeight = with(LocalDensity.current) { labelStyle.lineHeight.toDp() }
    val content = when (labelMode) {
        SymbolLabelMode.TEXT_ONLY -> 0.dp
        SymbolLabelMode.IMAGE_ONLY -> spec.imageSize
        SymbolLabelMode.BELOW_IMAGE, SymbolLabelMode.ABOVE_IMAGE ->
            spec.imageSize + OriSpacing.ExtraSmall + labelHeight
    }
    return maxOf(OriDimensions.MinTouchTarget * 2, content + OriSpacing.Small * 2)
}

/** The way to a word that is missing, at the end of the words, where it is looked for. */
@Composable
private fun AddSymbolTile(onClick: () -> Unit, minHeight: Dp) {
    val label = stringResource(R.string.aac_add_word)
    OutlinedCard(onClick = onClick, shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(vertical = OriSpacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Text(text = label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        }
    }
}
