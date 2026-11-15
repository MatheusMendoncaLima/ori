package com.oriteam.ori.ui.components.form

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormOpeningTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `an opening keeps its name when the screen is recreated, and a form opened again gets a new one`() {
        val names = mutableListOf<String>()
        var isOpen by mutableStateOf(true)
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            if (isOpen) {
                val opening = rememberFormOpening()
                SideEffect { if (names.lastOrNull() != opening) names += opening }
            }
        }

        // Rotation or the system closing the app: the same opening, so its draft is kept.
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(1, names.size)

        // Closed (as the system "back" does) and opened again: another opening.
        composeRule.runOnIdle { isOpen = false }
        composeRule.runOnIdle { isOpen = true }
        composeRule.waitForIdle()

        assertEquals(2, names.distinct().size)
    }
}
