package com.oriteam.ori.ui.app.screens.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.theme.OriDimensions
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The login form must stay usable in every window shape, including with 200% text. */
@RunWith(AndroidJUnit4::class)
class LoginScreenLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `phone portrait shows only the form`() {
        composeRule.setContent { OriTestWindow(widthDp = 411, heightDp = 891) { LoginScreen() } }

        composeRule.onNodeWithText("Entrar").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Logo Ori").assertDoesNotExist()
    }

    @Test
    fun `landscape phone with enlarged text shows the brand beside a scrollable form`() {
        composeRule.setContent {
            OriTestWindow(widthDp = 640, heightDp = 360, fontScale = 2f) { LoginScreen() }
        }

        val logo = composeRule.onNodeWithContentDescription("Logo Ori").bounds
        val lastButton = composeRule.onNodeWithText("Criar uma conta").performScrollTo()
        lastButton.assertIsDisplayed()
        assertTrue(logo.right <= lastButton.bounds.left)
    }

    @Test
    fun `tablet keeps the form at a readable width instead of stretching it`() {
        composeRule.setContent { OriTestWindow(widthDp = 800, heightDp = 1280) { LoginScreen() } }

        val button = composeRule.onNodeWithText("Entrar").bounds
        // The button fills the form column, which is limited to OriDimensions.FormMaxWidth.
        assertTrue(button.right - button.left <= OriDimensions.FormMaxWidth)
    }
}
