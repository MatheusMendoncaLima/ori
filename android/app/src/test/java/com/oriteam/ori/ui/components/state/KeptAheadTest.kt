package com.oriteam.ori.ui.components.state

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A part of a screen made ahead (all the apps of ORI Home, "Montar" of the communication) is kept made out of sight:
 * not drawn, not read, not touched, and showing it again composes nothing.
 */
@RunWith(AndroidJUnit4::class)
class KeptAheadTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private class Made

    private var isShown by mutableStateOf(false)
    private var made = 0
    private var touches by mutableIntStateOf(0)

    private fun render() {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 640) {
                Box(modifier = Modifier.fillMaxSize().outOfSightUnless(isShown)) {
                    remember { Made().also { made++ } }
                    Box(modifier = Modifier.fillMaxSize().clickable { touches++ }) { Text("Parte feita antes") }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `out of sight, a part is made but neither read nor touched`() {
        render()

        composeRule.onNodeWithText("Parte feita antes").assertDoesNotExist()
        composeRule.onRoot().performTouchInput { click(Offset(20f, 20f)) }
        composeRule.waitForIdle()

        assertEquals(1, made)
        assertEquals(0, touches)
    }

    @Test
    fun `shown again, the part is there at once, without being made again`() {
        render()

        isShown = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Parte feita antes").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { click(Offset(20f, 20f)) }
        composeRule.waitForIdle()
        assertEquals(1, made)
        assertEquals(1, touches)
    }

    @Test
    fun `covered by a sub-screen, a screen leaves without being undone and comes back without being made again`() {
        var isCovered by mutableStateOf(false)
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 640) {
                KeptUnder(isShown = !isCovered, modifier = Modifier.fillMaxSize()) {
                    remember { Made().also { made++ } }
                    Box(modifier = Modifier.fillMaxSize().clickable { touches++ }) { Text("Tela por baixo") }
                }
            }
        }
        composeRule.waitForIdle()

        isCovered = true
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tela por baixo").assertDoesNotExist()
        composeRule.onRoot().performTouchInput { click(Offset(20f, 20f)) }
        composeRule.waitForIdle()
        assertEquals(0, touches)

        isCovered = false
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tela por baixo").assertIsDisplayed()
        assertEquals(1, made)
    }

    @Test
    fun `the screen counts as rested only after its first frames, and then for good`() {
        val seen = mutableListOf<Boolean>()
        composeRule.setContent {
            val hasRested = rememberScreenRested()
            seen += hasRested
        }

        composeRule.waitForIdle()

        assertEquals(false, seen.first())
        assertEquals(true, seen.last())
    }
}
