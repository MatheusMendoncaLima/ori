package com.oriteam.ori.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing

private const val INDICATOR_ANIMATION_MILLIS = 300

/** Row of dots that highlights the current page of a pager with a stretch animation. */
@Composable
fun AnimatedPagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    val motion = LocalOriAppearance.current.motion
    val activeColor = MaterialTheme.colorScheme.onBackground
    val inactiveColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { page ->
            val isSelected = pagerState.currentPage == page
            val color by animateColorAsState(
                targetValue = if (isSelected) activeColor else inactiveColor,
                animationSpec = motion.decorative(INDICATOR_ANIMATION_MILLIS),
                label = "IndicatorColor"
            )
            val width by animateDpAsState(
                targetValue = if (isSelected) 18.dp else 8.dp,
                animationSpec = motion.playful(INDICATOR_ANIMATION_MILLIS),
                label = "IndicatorWidth"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
