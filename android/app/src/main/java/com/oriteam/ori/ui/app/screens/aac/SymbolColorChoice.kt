package com.oriteam.ori.ui.app.screens.aac

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SymbolColor
import com.oriteam.ori.ui.components.form.OriChoiceChip
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriSpacing

private val colorNames = mapOf(
    SymbolColor.YELLOW to R.string.aac_symbol_color_yellow,
    SymbolColor.MINT to R.string.aac_symbol_color_mint,
    SymbolColor.BLUE to R.string.aac_symbol_color_blue,
    SymbolColor.PINK to R.string.aac_symbol_color_pink,
    SymbolColor.ORANGE to R.string.aac_symbol_color_orange,
    SymbolColor.LILAC to R.string.aac_symbol_color_lilac,
    SymbolColor.GREEN to R.string.aac_symbol_color_green,
    SymbolColor.NONE to R.string.aac_symbol_color_none
)

/**
 * The color of the tile in Montar. "Pelo tipo de palavra" (no choice) keeps the color keys of communication boards;
 * any other choice is the person's, so a board can follow the colors the person already knows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymbolColorChoice(selected: SymbolColor?, onSelect: (SymbolColor?) -> Unit) {
    Column {
        SectionHeader(text = stringResource(R.string.aac_symbol_color))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)
        ) {
            ColorChip(R.string.aac_symbol_color_auto, null, selected, onSelect)
            SymbolColor.entries.forEach { color -> ColorChip(colorNames.getValue(color), color, selected, onSelect) }
        }
    }
}

/** A chip with a dot of the color, read as one choice: its name and whether it is selected. */
@Composable
private fun ColorChip(
    @StringRes nameRes: Int,
    color: SymbolColor?,
    current: SymbolColor?,
    onSelect: (SymbolColor?) -> Unit
) {
    val name = stringResource(nameRes)
    val isSelected = color == current
    val dot = color?.let { LocalOriColors.current.accentOf(it)?.container }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.ExtraSmall),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = name
            selected = isSelected
            role = Role.RadioButton
            onClick {
                onSelect(color)
                true
            }
        }
    ) {
        if (dot != null) {
            Box(
                modifier = Modifier
                    .size(DOT_SIZE.dp)
                    .background(dot, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
        OriChoiceChip(label = name, isSelected = isSelected, onClick = { onSelect(color) })
    }
}

private const val DOT_SIZE = 16
