package com.oriteam.ori.ui.components.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriDimensions

/** Three tabs ("Pendentes", "Concluídas", "Todas") fit in one line on a 360 dp phone. */
private val ChipHorizontalPadding = 12.dp

/**
 * One option of a single choice, the same everywhere: the tabs, the board chosen in Comunicação and
 * the options of "Repetir". The chosen one is filled with the brand teal, like a chosen day; the others
 * are outlined. Place the options in a `selectableGroup()` so screen readers count them.
 *
 * [role] is [Role.Tab] when the choice changes what the screen shows, and [Role.RadioButton] in a form.
 */
@Composable
fun OriChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.RadioButton
) {
    val colors = LocalOriColors.current
    val shape = MaterialTheme.shapes.medium
    Surface(
        shape = shape,
        color = if (isSelected) colors.strong else MaterialTheme.colorScheme.surface,
        contentColor = if (isSelected) colors.onStrong else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant).takeUnless { isSelected },
        modifier = modifier
            .clip(shape)
            .selectable(selected = isSelected, role = role, onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = OriDimensions.MinTouchTarget)
                .padding(horizontal = ChipHorizontalPadding)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
