package com.oriteam.ori.ui.components.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriAppearance
import com.oriteam.ori.ui.theme.OriMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A moment in the middle of a step into a sub-screen (it takes 360 ms when expressive). */
private const val MIDWAY_MILLIS = 120L

/**
 * The animations follow "Animações" in Configurações. What is checked here is what a person who reduces
 * or turns off motion relies on: nothing slides for them, and nothing waits for an animation to be shown.
 */
@RunWith(AndroidJUnit4::class)
class MotionLevelsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val isOpen = mutableStateOf(false)

    /** Shows an area, opens its sub-screen, and stops the clock in the middle of the change. */
    private fun openSubscreenAndPause(level: MotionLevel) {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 640) {
                CompositionLocalProvider(LocalOriAppearance provides OriAppearance(motion = OriMotion(level))) {
                    OriPaneTransition(target = isOpen.value, isForward = { _, to -> to }) { shown ->
                        Box(modifier = Modifier.fillMaxSize()) { Text(if (shown) "Subscreen" else "Area") }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        // From here the test moves the clock itself, to look at the middle of the change.
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread {
            isOpen.value = true
            Snapshot.sendApplyNotifications()
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(MIDWAY_MILLIS)
    }

    @Test
    fun `with animations off, a sub-screen is complete at once and the area is gone`() {
        openSubscreenAndPause(MotionLevel.OFF)

        assertEquals(0.dp, composeRule.onNodeWithText("Subscreen").bounds.left)
        composeRule.onNodeWithText("Area").assertDoesNotExist()
    }

    @Test
    fun `reduced motion fades the sub-screen in where it belongs, without sliding`() {
        openSubscreenAndPause(MotionLevel.REDUCED)

        assertEquals(0.dp, composeRule.onNodeWithText("Subscreen").bounds.left)
    }

    @Test
    fun `expressive motion slides the sub-screen in from the end, and it settles in place`() {
        openSubscreenAndPause(MotionLevel.EXPRESSIVE)

        assertTrue(composeRule.onNodeWithText("Subscreen").bounds.left > 0.dp)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(0.dp, composeRule.onNodeWithText("Subscreen").bounds.left)
        composeRule.onNodeWithText("Area").assertDoesNotExist()
    }

    @Test
    fun `only expressive and subtle motion move things, and lists keep a fade until motion is off`() {
        assertTrue(OriMotion(MotionLevel.EXPRESSIVE).movesThings)
        assertTrue(OriMotion(MotionLevel.SUBTLE).movesThings)
        assertFalse(OriMotion(MotionLevel.REDUCED).movesThings)
        assertFalse(OriMotion(MotionLevel.OFF).movesThings)

        assertNull(OriMotion(MotionLevel.REDUCED).itemPlacement())
        assertTrue(OriMotion(MotionLevel.REDUCED).itemFade() != null)
        assertNull(OriMotion(MotionLevel.OFF).itemFade())
        assertEquals(0, OriMotion(MotionLevel.OFF).millis(300))
    }
}
