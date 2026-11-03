package com.oriteam.ori.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import com.oriteam.ori.ui.adaptive.WindowLayout
import com.oriteam.ori.ui.adaptive.windowLayoutFor
import com.oriteam.ori.ui.theme.OriTheme

/**
 * Renders [content] as if it were inside a window of [widthDp] x [heightDp] with the given
 * system font scale. The [WindowLayout] is derived from the same size, exactly as on a device.
 */
@Composable
fun OriTestWindow(
    widthDp: Int,
    heightDp: Int,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val size = DpSize(Dp(widthDp.toFloat()), Dp(heightDp.toFloat()))
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.ForcedSize(size) then DeviceConfigurationOverride.FontScale(fontScale)
    ) {
        OriTheme(windowLayout = windowLayoutFor(widthDp, heightDp), content = content)
    }
}

val SemanticsNodeInteraction.bounds: DpRect
    get() = getUnclippedBoundsInRoot()
