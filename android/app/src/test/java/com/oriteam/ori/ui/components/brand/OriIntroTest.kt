package com.oriteam.ori.ui.components.brand

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.AppPreferences
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.ui.adaptive.windowLayoutFor
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OriIntroTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var finished = 0

    private fun render(motion: MotionLevel) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            OriTheme(
                windowLayout = windowLayoutFor(360, 780),
                preferences = AppPreferences(account = AccountPreferences(motion = motion))
            ) { OriIntroBrand(onFinished = { finished++ }) }
        }
    }

    @Test
    fun `with animations off everything is there at once and the options come right away`() {
        render(MotionLevel.OFF)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(100)

        assertEquals(1, finished)
        composeRule.onNodeWithContentDescription("ori").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("organizar • comunicar • cuidar").assertIsDisplayed()
    }

    @Test
    fun `the expressive opening ends once, in about three seconds, read as whole words`() {
        render(MotionLevel.EXPRESSIVE)
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals("still playing", 0, finished)

        composeRule.mainClock.advanceTimeBy(4_000)
        assertEquals(1, finished)
        composeRule.onNodeWithContentDescription("Logo Ori").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("ori").assertIsDisplayed()
    }
}
