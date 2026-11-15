package com.oriteam.ori.data.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.withClip
import com.oriteam.ori.domain.launcher.IconShape

/** The corners of a rounded icon, as a part of its side: the rounded square of One UI. */
private const val ROUNDED_CORNER_SHARE = 0.28f

/**
 * An adaptive icon's layers are drawn this much bigger than the icon: Android shows only their middle (72 of 108 dp),
 * and the rest is room for the shape and its movements.
 */
private const val ADAPTIVE_LAYER_SCALE = 1.5f

/**
 * An icon made before adaptive icons is drawn this big on a white plate, as Android itself does with them, so it gets
 * the same shape as the others instead of its own square or odd outline.
 */
private const val LEGACY_ICON_SHARE = 0.7f

/**
 * The icon [this] drawn at [sizePx] in [shape], the same for every app (One UI draws them all in one shape; drawn as
 * each app brings it, some were round, some square, some neither). An adaptive icon shows the middle of its layers,
 * as Android does; any other is drawn on a white plate.
 */
internal fun Drawable.inShape(shape: IconShape, sizePx: Int): Bitmap {
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    canvas.withClip(outlineOf(shape, sizePx.toFloat())) {
        val icon = this@inShape
        if (icon is AdaptiveIconDrawable) {
            val bleed = (sizePx * (ADAPTIVE_LAYER_SCALE - 1f) / 2f).toInt()
            listOfNotNull(icon.background, icon.foreground).forEach { layer ->
                layer.setBounds(-bleed, -bleed, sizePx + bleed, sizePx + bleed)
                layer.draw(this)
            }
        } else {
            drawColor(Color.WHITE)
            val margin = (sizePx * (1f - LEGACY_ICON_SHARE) / 2f).toInt()
            icon.setBounds(margin, margin, sizePx - margin, sizePx - margin)
            icon.draw(this)
        }
    }
    return bitmap
}

/**
 * [icon] with the badge of [user] when it is another profile (the work one), as Android badges its apps; the icon of
 * the person's own profile as it is.
 */
internal fun Context.badgedFor(user: UserHandle, icon: Bitmap): Bitmap {
    if (user == Process.myUserHandle()) return icon
    return packageManager.getUserBadgedIcon(icon.toDrawable(resources), user).toBitmap(icon.width, icon.height)
}

/** The outline of [shape] on a square of [side]. */
internal fun outlineOf(shape: IconShape, side: Float): Path = Path().apply {
    when (shape) {
        IconShape.ROUND -> addCircle(side / 2f, side / 2f, side / 2f, Path.Direction.CW)
        IconShape.ROUNDED -> {
            val corner = side * ROUNDED_CORNER_SHARE
            addRoundRect(0f, 0f, side, side, corner, corner, Path.Direction.CW)
        }
        IconShape.SQUARE -> addRect(0f, 0f, side, side, Path.Direction.CW)
    }
}
