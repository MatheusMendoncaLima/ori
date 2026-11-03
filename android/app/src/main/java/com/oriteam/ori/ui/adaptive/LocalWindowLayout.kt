package com.oriteam.ori.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity

/**
 * The [WindowLayout] of the current window. Provided once by `OriTheme`; tests and previews
 * can provide another value to simulate any window size.
 */
val LocalWindowLayout = staticCompositionLocalOf { WindowLayout.Compact }

/**
 * Reads the real window size and posture. Recomposes automatically when the window is
 * resized, rotated, unfolded or moved to split screen.
 */
@Composable
fun currentWindowLayout(): WindowLayout {
    val sizeInPixels = currentWindowSize()
    val posture = currentWindowAdaptiveInfo().windowPosture
    val dpSize = with(LocalDensity.current) { sizeInPixels.width.toDp() to sizeInPixels.height.toDp() }
    return windowLayoutFor(
        widthDp = dpSize.first.value.toInt(),
        heightDp = dpSize.second.value.toInt(),
        isTabletop = posture.isTabletop
    )
}
