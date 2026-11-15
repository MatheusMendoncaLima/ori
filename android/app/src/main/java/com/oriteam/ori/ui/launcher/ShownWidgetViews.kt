package com.oriteam.ori.ui.launcher

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.view.drawToBitmap
import java.lang.ref.WeakReference

private const val TAG = "OriShownWidgets"

/**
 * The views of the widgets on the home screen now, by id, to lift one when it is held. Held weakly: a view that left
 * the screen is not kept alive for this. Used on the main thread only, as views are.
 */
internal class ShownWidgetViews {
    private val views = mutableMapOf<Int, WeakReference<View>>()

    fun add(widgetId: Int, view: View) {
        views[widgetId] = WeakReference(view)
    }

    fun remove(widgetId: Int, view: View) {
        if (views[widgetId]?.get() === view) views.remove(widgetId)
    }

    /**
     * The widget [widgetId] is held by the home screen: its own touch ends there, as a home screen of Android does
     * (the finger no longer presses it, so it shows no pressed shade and opens nothing when let go), and its picture
     * as it looks now, for the copy that follows the finger. Null when it is not laid out on the screen or cannot be
     * drawn.
     */
    fun lift(widgetId: Int): ImageBitmap? {
        val view = views[widgetId]?.get()?.takeIf { it.isLaidOut && it.width > 0 && it.height > 0 } ?: return null
        view.endOwnTouch()
        return runCatching { pictureOf(view) }
            .onFailure { Log.w(TAG, "A widget could not be drawn to be carried: ${it.javaClass.simpleName}") }
            .getOrNull()
            ?.asImageBitmap()
    }
}

/** The touch the view was following is canceled, and its pressed shade goes at once (not fading). */
private fun View.endOwnTouch() {
    val now = SystemClock.uptimeMillis()
    val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
    dispatchTouchEvent(cancel)
    cancel.recycle()
    cancelLongPress()
    jumpDrawablesToCurrentState()
}

/**
 * The view drawn as the screen draws it: by the graphics card (Android 10+), with the round corners and the effects
 * only it draws; in plain memory before Android 10, or when the graphics card gave nothing.
 */
private fun pictureOf(view: View): Bitmap {
    val byGraphicsCard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching { drawnByGraphicsCard(view) }
            .onFailure { Log.w(TAG, "The graphics card did not draw a widget: ${it.javaClass.simpleName}") }
            .getOrNull()
    } else {
        null
    }
    return byGraphicsCard ?: view.drawToBitmap()
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun drawnByGraphicsCard(view: View): Bitmap? {
    val node = RenderNode("CarriedWidget").apply { setPosition(0, 0, view.width, view.height) }
    view.draw(node.beginRecording())
    node.endRecording()
    val usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
    val reader = ImageReader.newInstance(view.width, view.height, PixelFormat.RGBA_8888, 1, usage)
    val renderer = HardwareRenderer()
    return try {
        renderer.setSurface(reader.surface)
        renderer.setContentRoot(node)
        renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
        reader.acquireNextImage()?.use { image ->
            image.hardwareBuffer?.use { buffer ->
                // Copied into the app's memory, so the buffer of the graphics card can go now.
                Bitmap.wrapHardwareBuffer(buffer, ColorSpace.get(ColorSpace.Named.SRGB))
                    ?.copy(Bitmap.Config.ARGB_8888, false)
            }
        }
    } finally {
        renderer.destroy()
        reader.close()
        node.discardDisplayList()
    }
}
