package com.oriteam.ori.ui.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriSpacing

/** A soft colored square with an icon, used to tell the rows of a menu apart at a glance. */
@Composable
private fun OriIconTile(painter: Painter, accentIndex: Int, modifier: Modifier = Modifier) {
    val accents = LocalOriColors.current.accents
    val accent = accents[accentIndex % accents.size]
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(accent.container)
    ) {
        Icon(painter = painter, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * A row of the profile and settings menus: a list row ([OriListItem]) with a colored icon and the arrow
 * saying that it opens another screen. Simplified screens drop the colored tile, never the text.
 */
@Composable
fun OriMenuRow(
    title: String,
    icon: ImageVector,
    accentIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    OriMenuRow(title, rememberVectorPainter(icon), accentIndex, onClick, modifier, description)
}

@Composable
fun OriMenuRow(
    title: String,
    painter: Painter,
    accentIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    OriRow(modifier = modifier, isSelected = false, onClick = onClick) {
        if (LocalOriAppearance.current.showsDecoration) {
            OriIconTile(painter = painter, accentIndex = accentIndex)
            Spacer(modifier = Modifier.width(OriSpacing.Medium))
        }
        OriRowTexts(headline = title, supportingText = description, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(OriSpacing.Small))
        OriOpenArrow()
    }
}
