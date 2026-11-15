package com.oriteam.ori.ui.launcher

import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val WIDTH_PX = 120
private const val HEIGHT_PX = 60

/**
 * A widget held by the home screen: its own touch ends (no pressed shade, no click when let go), and it is pictured as
 * it looks, only while it is on the screen and laid out.
 */
@RunWith(AndroidJUnit4::class)
class ShownWidgetViewsTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val shown = ShownWidgetViews()

    private fun <T : View> T.laidOut(): T = apply {
        measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT_PX, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, WIDTH_PX, HEIGHT_PX)
    }

    private fun laidOutView() = View(context).apply { setBackgroundColor(Color.RED) }.laidOut()

    @Test
    fun `a widget on the screen is pictured at its size`() {
        shown.add(7, laidOutView())

        val picture = shown.lift(7)

        assertEquals(WIDTH_PX, picture?.width)
        assertEquals(HEIGHT_PX, picture?.height)
    }

    @Test
    fun `lifted, the widget is no longer pressed and does not open when the finger lets go`() {
        var clicks = 0
        val button = View(context).apply {
            isClickable = true
            setOnClickListener { clicks++ }
        }
        val widget = FrameLayout(context).apply { addView(button, FrameLayout.LayoutParams(WIDTH_PX, HEIGHT_PX)) }
            .laidOut()
        shown.add(7, widget)
        val start = SystemClock.uptimeMillis()
        widget.dispatchTouchEvent(MotionEvent.obtain(start, start, MotionEvent.ACTION_DOWN, 10f, 10f, 0))
        assertTrue(button.isPressed)

        shown.lift(7)
        widget.dispatchTouchEvent(MotionEvent.obtain(start, start + 1, MotionEvent.ACTION_UP, 10f, 10f, 0))

        assertFalse(button.isPressed)
        assertEquals(0, clicks)
    }

    @Test
    fun `a widget not on the screen, gone or not laid out yet has no picture`() {
        assertNull(shown.lift(7))

        val view = laidOutView()
        shown.add(7, view)
        shown.remove(7, view)
        assertNull(shown.lift(7))

        shown.add(8, View(context))
        assertNull(shown.lift(8))
    }

    @Test
    fun `a view of an older widget with the same id does not take out the new one`() {
        val old = laidOutView()
        shown.add(7, old)
        shown.add(7, laidOutView())

        shown.remove(7, old)

        assertEquals(WIDTH_PX, shown.lift(7)?.width)
    }
}
