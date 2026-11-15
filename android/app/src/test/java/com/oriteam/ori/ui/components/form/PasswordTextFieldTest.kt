package com.oriteam.ori.ui.components.form

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A1: the button that shows the password is as big as every other button, for touch and for screen readers. */
@RunWith(AndroidJUnit4::class)
class PasswordTextFieldTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the show password button has the 48 dp touch area and switches the text`() {
        var isVisible by mutableStateOf(false)
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 640) {
                PasswordTextField(
                    label = "Senha",
                    value = "segredo",
                    onValueChange = {},
                    isPasswordVisible = isVisible,
                    onTogglePasswordVisibility = { isVisible = !isVisible },
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions.Default
                )
            }
        }

        val button = composeRule.onNodeWithContentDescription("Exibir senha").bounds
        assertTrue("width ${button.width}", button.width >= 48.dp)
        assertTrue("height ${button.height}", button.height >= 48.dp)

        composeRule.onNodeWithContentDescription("Exibir senha").performClick()
        composeRule.onNodeWithContentDescription("Ocultar senha").bounds
    }
}
