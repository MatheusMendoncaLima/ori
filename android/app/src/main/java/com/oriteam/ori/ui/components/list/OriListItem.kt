package com.oriteam.ori.ui.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * One row of a list. Text wraps instead of being cut, so long titles and enlarged fonts
 * stay readable; the row grows from the minimum of the chosen density, never below 48 dp.
 *
 * [isSelected] highlights the item shown in the detail pane of a list–detail layout. A row that
 * opens another screen ends with [OriOpenArrow], as the menus do.
 */
@Composable
fun OriListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    OriRow(modifier = modifier, isSelected = isSelected, onClick = onClick) {
        leadingIcon?.takeIf { LocalOriAppearance.current.showsDecoration }?.let {
            Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(OriSpacing.Medium))
        }
        OriRowTexts(headline = headline, supportingText = supportingText, modifier = Modifier.weight(1f))
        trailingContent?.let {
            Spacer(modifier = Modifier.width(OriSpacing.Small))
            it()
        }
    }
}

/**
 * What every row of the app is drawn on, in lists and in menus: the surface, light border and corners
 * of [com.oriteam.ori.ui.components.card.OriCard]. Simplified screens drop the border, as cards do.
 */
@Composable
internal fun OriRow(
    modifier: Modifier,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    content: @Composable RowScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val appearance = LocalOriAppearance.current
    val shape = MaterialTheme.shapes.large
    val border = if (appearance.showsDecoration) Modifier.border(1.dp, colors.outlineVariant, shape) else Modifier
    val clickModifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = appearance.metrics.listItemMinHeight)
            .clip(shape)
            .background(if (isSelected) colors.secondaryContainer else colors.surface)
            .then(border)
            .then(clickModifier)
            .semantics { selected = isSelected }
            .padding(
                horizontal = appearance.metrics.cardPadding,
                vertical = appearance.metrics.listItemVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Title and supporting line of a row; a summary list keeps only the title. */
@Composable
internal fun OriRowTexts(headline: String, supportingText: String?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(text = headline, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
        supportingText?.takeIf { LocalOriAppearance.current.showsSecondaryText }?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

/** Says that a row opens another screen. Not announced: the row itself is the button. */
@Composable
fun OriOpenArrow() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Title that separates groups of content, the same on every screen. It starts at the content margin,
 * like the fields and cards below it. Marked as a heading for screen readers.
 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = OriSpacing.Small)
            .semantics { heading() }
    )
}
