package com.oriteam.ori.ui.components.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * A screen with the floating [OriActionPill] at the bottom. [content] receives the room the pill takes,
 * measured: lists use it as padding at their end and empty states as space below them, so nothing stays
 * behind the pill, also when enlarged text makes it taller. Without the pill ([showsPill] false) the room is 0.
 */
@Composable
fun ActionPillLayout(
    pillLabel: String,
    onPillClick: () -> Unit,
    modifier: Modifier = Modifier,
    showsPill: Boolean = true,
    content: @Composable (clearance: Dp) -> Unit
) {
    val density = LocalDensity.current
    // Its smallest height until it is measured, so the first frame is already close.
    var pillHeight by remember { mutableStateOf(OriDimensions.ControlMinHeight) }
    Box(modifier = modifier) {
        content(if (showsPill) pillHeight + OriSpacing.Medium * 2 else 0.dp)
        if (showsPill) {
            OriActionPill(
                label = pillLabel,
                onClick = onPillClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = OriSpacing.Medium)
                    .onSizeChanged { pillHeight = with(density) { it.height.toDp() } }
            )
        }
    }
}
