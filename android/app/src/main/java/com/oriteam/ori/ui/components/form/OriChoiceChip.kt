package com.oriteam.ori.ui.components.form

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriDimensions

/** Three tabs ("Pendentes", "Concluídas", "Todas") fit in one line on a 360 dp phone. */
private val ChipHorizontalPadding = 12.dp

private const val CHOICE_MILLIS = 200

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
    val shape = MaterialTheme.shapes.medium
    val motion = LocalOriAppearance.current.motion
    val container = animateColorAsState(
        // The main color, as the buttons: one teal in both themes (I11). In the dark theme the deep teal of
        // `strong` beside the light teal of a button looked like two colors for one role.
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        animationSpec = motion.feedback(CHOICE_MILLIS),
        label = "ChoiceContainer"
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.feedback(CHOICE_MILLIS),
        label = "ChoiceContent"
    )
    Surface(
        shape = shape,
        color = Color.Transparent,
        contentColor = content,
        // The edge of a control (3:1): a choice not taken still has to look like something to touch.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline).takeUnless { isSelected },
        modifier = modifier
            .clip(shape)
            .drawBehind { drawRect(container.value) }
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
