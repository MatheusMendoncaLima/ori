package com.oriteam.ori.ui.components.form

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.ui.theme.OriDimensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The defect: "Escolher foto" and "Remover imagem" side by side, each with its own size. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OriButtonsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun bounds(text: String) = composeRule.onNodeWithText(text).getBoundsInRoot()

    private fun render(fontScale: Float = 1f) {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 640, fontScale = fontScale) {
                Column {
                    EqualWidthRow(modifier = Modifier.fillMaxWidth()) {
                        OriSecondaryButton(text = "Escolher foto", onClick = {})
                        OriSecondaryButton(text = "Remover imagem com um nome bem longo", onClick = {})
                    }
                    OriPrimaryButton(text = "Principal", onClick = {})
                    OriSecondaryButton(text = "Secundário", onClick = {})
                    OriTextButton(text = "Texto", onClick = {})
                }
            }
        }
    }

    @Test
    fun `buttons side by side have the same size whatever their labels say`() {
        render()

        val first = composeRule.onNodeWithText("Escolher foto").fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithText(
            "Remover imagem com um nome bem longo"
        ).fetchSemanticsNode().boundsInRoot

        assertEquals(first.width, second.width, 1f)
        assertEquals(first.height, second.height, 1f)
    }

    @Test
    fun `all kinds of button reach the same minimum height`() {
        render()

        listOf("Principal", "Secundário", "Texto").forEach {
            assertTrue(it, bounds(it).height >= OriDimensions.ControlMinHeight - 1.dp)
        }
    }

    @Test
    fun `with a very large text they stack and keep the same width`() {
        render(fontScale = 3f)

        val first = composeRule.onNodeWithText("Escolher foto").fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithText(
            "Remover imagem com um nome bem longo"
        ).fetchSemanticsNode().boundsInRoot

        assertEquals(first.width, second.width, 1f)
        assertTrue(second.top >= first.bottom)
    }
}
