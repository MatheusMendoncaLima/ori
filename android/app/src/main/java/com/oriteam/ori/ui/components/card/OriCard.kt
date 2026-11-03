package com.oriteam.ori.ui.components.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.theme.LocalOriAppearance

/**
 * ORI card: white surface, light border, no shadow (low visual noise).
 *
 * Pass [onClick] only when the whole card is one action; otherwise put buttons inside it.
 * [isHighlighted] uses a stronger border, for example for the current routine step.
 */
@Composable
fun OriCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isHighlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val appearance = LocalOriAppearance.current
    // A highlight carries meaning and stays; the plain border is ornament, gone when simplified.
    val border = when {
        isHighlighted -> BorderStroke(2.dp, colors.primary)
        appearance.showsDecoration -> BorderStroke(1.dp, colors.outlineVariant)
        else -> null
    }
    val cardColors = CardDefaults.cardColors(containerColor = colors.surface, contentColor = colors.onSurface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val body: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(appearance.metrics.cardPadding), content = content)
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = cardColors,
            elevation = elevation,
            border = border,
            content = body
        )
    } else {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = cardColors,
            elevation = elevation,
            border = border,
            content = body
        )
    }
}
