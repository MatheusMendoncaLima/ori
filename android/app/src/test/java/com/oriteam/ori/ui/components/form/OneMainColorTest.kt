package com.oriteam.ori.ui.components.form

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.AppPreferences
import com.oriteam.ori.domain.preferences.ThemeMode
import com.oriteam.ori.ui.adaptive.windowLayoutFor
import com.oriteam.ori.ui.theme.OriAppColors
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * I11: inside the app a chosen option and a button are the same teal, in both themes. In the dark theme they were
 * two (a deep teal for choices, a light one for buttons), which looked unfinished.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OneMainColorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun fillOf(text: String, picture: Bitmap): Int {
        val bounds = composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        // Just left of the text, inside the filled shape.
        return picture.getPixel((bounds.left - 8).toInt(), bounds.center.y.toInt())
    }

    private fun renderAndCompare(theme: ThemeMode) {
        composeRule.setContent {
            OriTheme(
                windowLayout = windowLayoutFor(360, 640),
                preferences = AppPreferences(account = AccountPreferences(theme = theme))
            ) {
                OriAppColors {
                    Column {
                        OriChoiceChip(label = "Escolhida", isSelected = true, onClick = {})
                        OriPrimaryButton(text = "Salvar", onClick = {})
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        val view = composeRule.activity.window.decorView
        val picture = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(picture))

        assertEquals(theme.name, fillOf("Salvar", picture), fillOf("Escolhida", picture))
    }

    @Test
    fun `a chosen option and a button share one teal in the dark theme`() = renderAndCompare(ThemeMode.DARK)

    @Test
    fun `and in the light theme`() = renderAndCompare(ThemeMode.LIGHT)
}
