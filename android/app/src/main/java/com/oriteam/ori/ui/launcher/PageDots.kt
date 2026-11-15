package com.oriteam.ori.ui.launcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing

private val DotSize = 7.dp
private val PanelDashWidth = 12.dp
private val PanelDashHeight = 3.dp
private val HomeMarkSize = 12.dp
private const val MARK_MILLIS = 200
private const val INACTIVE_DOT_ALPHA = 0.3f

/**
 * The dots of the pages, as on One UI: a dash for the panel to the left, a house for the main page (where the Home
 * button goes), a dot for each other page and, while editing ([hasAddPage]), a "+" for the card that adds one; the
 * one on the screen is full. [current] is -1 on the panel and [count] on "+". Only a picture: the pages say where
 * they are to the screen reader.
 */
@Composable
internal fun PageDots(
    count: Int,
    current: Int,
    mainPage: Int,
    hasPanel: Boolean,
    modifier: Modifier = Modifier,
    hasAddPage: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(DotsHeight).clearAndSetSemantics { }
    ) {
        if (count + (if (hasPanel) 1 else 0) + (if (hasAddPage) 1 else 0) < 2) return@Row
        val ink = LocalContentColor.current
        if (hasPanel) {
            PageMark(isCurrent = current < 0) {
                Box(modifier = Modifier.size(PanelDashWidth, PanelDashHeight).clip(CircleShape).background(ink))
            }
        }
        repeat(count) { page ->
            PageMark(isCurrent = page == current) {
                if (page == mainPage) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        tint = ink,
                        modifier = Modifier.size(HomeMarkSize)
                    )
                } else {
                    Box(modifier = Modifier.size(DotSize).clip(CircleShape).background(ink))
                }
            }
        }
        if (hasAddPage) {
            PageMark(isCurrent = current == count) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = ink, modifier = Modifier.size(HomeMarkSize))
            }
        }
    }
}

/** One mark of the dots, full when its page is on the screen and soft otherwise, changing with the motion level. */
@Composable
private fun PageMark(isCurrent: Boolean, mark: @Composable () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = if (isCurrent) 1f else INACTIVE_DOT_ALPHA,
        animationSpec = LocalOriAppearance.current.motion.feedback(MARK_MILLIS),
        label = "PageDot"
    )
    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha }) { mark() }
}
