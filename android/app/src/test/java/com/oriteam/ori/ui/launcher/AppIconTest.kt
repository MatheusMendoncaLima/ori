package com.oriteam.ori.ui.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.launcherApp
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** An icon already in memory is drawn at once: a page turned to does not show empty places first. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppIconTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `an icon in memory shows while the slow reading has not answered`() {
        val red = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.Red.toArgb()) }
        val app = launcherApp("Câmera")
        composeRule.setContent {
            CompositionLocalProvider(
                LocalAppIconLoader provides { awaitCancellation() },
                LocalCachedAppIcon provides { key -> red.takeIf { key == app.key } }
            ) {
                AppIcon(app = app, size = 48.dp, isMonochrome = false)
            }
        }

        composeRule.waitForIdle()
        val view = composeRule.activity.window.decorView
        val picture = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(picture))
        // The icon sits at the top left of the window, 48 dp wide: its middle is red.
        val middle = with(composeRule.density) { 24.dp.roundToPx() }
        assertEquals(Color.Red.toArgb(), picture.getPixel(middle, middle))
    }
}
