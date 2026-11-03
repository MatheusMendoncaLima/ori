package com.oriteam.ori.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.LoadingButton
import com.oriteam.ori.ui.components.list.OriListItem
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Components must grow with the system font size (Android allows up to 200%) instead of
 * clipping text. Each test renders the same component at 100% and 200% and compares heights.
 */
@RunWith(AndroidJUnit4::class)
class EnlargedTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun renderAtBothScales(component: @Composable (tag: String) -> Unit) {
        composeRule.setContent {
            OriTheme {
                Column(modifier = Modifier.width(320.dp)) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1f)) { component(NORMAL) }
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { component(LARGE) }
                }
            }
        }
    }

    private fun assertGrowsWithText() {
        val normal = composeRule.onNodeWithTag(NORMAL).bounds
        val large = composeRule.onNodeWithTag(LARGE).bounds
        assertTrue(
            "Expected ${large.bottom - large.top} > ${normal.bottom - normal.top}",
            large.bottom - large.top > normal.bottom - normal.top
        )
    }

    @Test
    fun `text field keeps enlarged text entirely inside the field`() {
        renderAtBothScales { tag ->
            LabeledTextField(
                label = "E-mail",
                value = "ana@ori.app",
                onValueChange = {},
                placeholder = "",
                keyboardOptions = KeyboardOptions.Default,
                keyboardActions = KeyboardActions.Default,
                modifier = Modifier.testTag(tag)
            )
        }

        val inputs = composeRule.onAllNodes(hasSetTextAction())
        val normalInput = inputs[0].bounds
        val largeInput = inputs[1].bounds
        val largeComponent = composeRule.onNodeWithTag(LARGE).bounds
        assertTrue(largeInput.bottom - largeInput.top > normalInput.bottom - normalInput.top)
        assertTrue(largeInput.top >= largeComponent.top && largeInput.bottom <= largeComponent.bottom)
    }

    @Test
    fun `primary button grows when an enlarged label needs more lines`() {
        renderAtBothScales { tag ->
            LoadingButton(text = LONG_BUTTON_LABEL, isLoading = false, onClick = {}, modifier = Modifier.testTag(tag))
        }

        assertGrowsWithText()
    }

    @Test
    fun `list item wraps long text instead of cutting it`() {
        renderAtBothScales { tag ->
            OriListItem(headline = LONG_TEXT, supportingText = "Toda manhã", modifier = Modifier.testTag(tag))
        }

        assertGrowsWithText()
        // The enlarged copy (second node) is fully laid out, not replaced by an ellipsis.
        composeRule.onAllNodesWithText(LONG_TEXT, useUnmergedTree = true)[1].assertIsDisplayed()
    }

    private companion object {
        const val NORMAL = "normal"
        const val LARGE = "large"
        const val LONG_BUTTON_LABEL = "Já tenho uma conta e quero entrar agora"
        const val LONG_TEXT = "Separar a roupa da escola, conferir o material e colocar a garrafa de água na mochila"
    }
}
