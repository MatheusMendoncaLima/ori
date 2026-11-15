package com.oriteam.ori.ui.launcher

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.domain.launcher.LauncherApp

/**
 * Reads the icon of an app off the main thread, in the shape the person chose (see AndroidInstalledApps.iconOf); null
 * when it is gone. Another shape is another loader, so the icons are read again.
 */
val LocalAppIconLoader = staticCompositionLocalOf<suspend (String) -> Bitmap?> { { null } }

/** The shape of every icon (and of a folder), as the person chose it. */
internal val LocalIconShape = staticCompositionLocalOf { IconShape.ROUNDED }

/** The corners of a rounded icon (and folder), as a part of its side: the rounded square of One UI. */
private const val ROUNDED_CORNER_PERCENT = 28

/** [IconShape] as drawn by the screen, for a folder or a tile that sits with the icons. */
internal fun IconShape.outline(): Shape = when (this) {
    IconShape.ROUND -> CircleShape
    IconShape.ROUNDED -> RoundedCornerShape(percent = ROUNDED_CORNER_PERCENT)
    IconShape.SQUARE -> RectangleShape
}

/**
 * The icon of an app already in memory, or null, read at once. A page is built again each time it is turned to, and
 * without this its icons would blink in one frame late.
 */
val LocalCachedAppIcon = staticCompositionLocalOf<(String) -> Bitmap?> { { null } }

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * The icon of an app, only visual (the label next to it says the name). In low stimulation it is grey, so the
 * colors of many apps do not compete for attention; the place keeps its size while the icon loads.
 */
@Composable
fun AppIcon(app: LauncherApp, size: Dp, isMonochrome: Boolean, modifier: Modifier = Modifier) {
    val load = LocalAppIconLoader.current
    val cached = LocalCachedAppIcon.current
    val icon by produceState(initialValue = cached(app.key), app.key, load) { value = load(app.key) }
    Box(modifier = modifier.size(size)) {
        icon?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                colorFilter = grayscale.takeIf { isMonochrome },
                modifier = Modifier.size(size)
            )
        }
    }
}
