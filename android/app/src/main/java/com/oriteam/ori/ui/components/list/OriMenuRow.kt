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
import com.oriteam.ori.ui.theme.OriAccent
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * A soft square with an icon, used to tell the rows of a menu apart at a glance. Its [accent] is chosen by the
 * screen: the color of the area the row leads to ([areaAccent], as on Início) or of the settings page
 * ([OriExtraColors.settings], as in the wireframe). Without one, the row has the app's own color, the brand cyan.
 */
@Composable
private fun OriIconTile(painter: Painter, accent: OriAccent?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val container = accent?.container ?: scheme.primaryContainer
    val tint = if (accent == null) scheme.primary else scheme.onSurface
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
    ) {
        Icon(painter = painter, contentDescription = null, tint = tint)
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
    accent: OriAccent?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    OriMenuRow(title, rememberVectorPainter(icon), accent, onClick, modifier, description)
}

@Composable
fun OriMenuRow(
    title: String,
    painter: Painter,
    accent: OriAccent?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    OriRow(modifier = modifier, isSelected = false, onClick = onClick) {
        if (LocalOriAppearance.current.showsDecoration) {
            OriIconTile(painter = painter, accent = accent)
            Spacer(modifier = Modifier.width(OriSpacing.Medium))
        }
        OriRowTexts(headline = title, supportingText = description, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(OriSpacing.Small))
        OriOpenArrow()
    }
}
