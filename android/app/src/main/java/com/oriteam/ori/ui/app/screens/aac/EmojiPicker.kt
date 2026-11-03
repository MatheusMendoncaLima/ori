package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Suggestions only: any emoji can be typed in the field next to them. Grouped loosely as needs,
 * feelings, people, food, places and activities. Emoji are drawn by the device font, so the app
 * ships no third-party pictures (see docs/funcionalidades/caa.md, "Licenças").
 */
internal val suggestedEmoji = listOf(
    "👍", "👎", "🙋", "🆘", "✋", "➕", "🏁", "❓",
    "😊", "😢", "😠", "😨", "😴", "🤕", "🤒", "🥶",
    "👩", "👨", "👵", "👴", "👶", "🧑‍🏫", "🧑‍⚕️", "🐶",
    "💧", "🍽️", "🍎", "🍞", "🥛", "☕", "🍫", "🍕",
    "🏠", "🏫", "🏥", "🌳", "🚻", "🛏️", "🚗", "🛒",
    "🎵", "📺", "🎮", "📚", "⚽", "🛁", "🪥", "👕"
)

private val EmojiCellSize = 28.dp

/** A grid of suggested emoji; the selected one is marked for sight and for screen readers. */
@Composable
fun EmojiSuggestions(selected: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(OriSpacing.ExtraSmall)
    ) {
        suggestedEmoji.forEach { emoji ->
            val isSelected = emoji == selected
            val colors = MaterialTheme.colorScheme
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) colors.primaryContainer else colors.surface,
                modifier = Modifier
                    .size(OriDimensions.MinTouchTarget)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(emoji) })
                    .semantics { contentDescription = emoji }
            ) {
                SymbolPicture(image = SymbolImageUi.Emoji(emoji), size = EmojiCellSize)
            }
        }
    }
}
