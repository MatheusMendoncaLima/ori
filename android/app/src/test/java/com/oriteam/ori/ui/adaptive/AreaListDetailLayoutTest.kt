package com.oriteam.ori.ui.adaptive

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.ui.components.list.OriListItem
import com.oriteam.ori.ui.components.text.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * List and detail by the width of the area: side by side when it is wide, one at a time otherwise, and always one at a
 * time where two panes are not allowed (the panel of ORI Home, where an empty half only took room on a tablet).
 */
@RunWith(AndroidJUnit4::class)
class AreaListDetailLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(allowsTwoPanes: Boolean) {
        composeRule.setContent {
            OriTestWindow(widthDp = TABLET_WIDTH, heightDp = TABLET_HEIGHT) {
                var selection by remember { mutableStateOf<String?>(null) }
                CompositionLocalProvider(LocalAllowsTwoPanes provides allowsTwoPanes) {
                    AreaListDetailLayout(
                        selection = selection,
                        onBack = { selection = null },
                        list = { modifier ->
                            Column(modifier) { OriListItem(headline = "Item a", onClick = { selection = "a" }) }
                        },
                        detail = { shown, _ -> Text("Detalhe $shown") },
                        emptyDetail = { Text("Escolha um item") }
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `a wide area shows the list and the detail side by side`() {
        render(allowsTwoPanes = true)

        composeRule.onNodeWithText("Escolha um item").assertIsDisplayed()
        composeRule.onNodeWithText("Item a").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Detalhe a").assertIsDisplayed()
        composeRule.onNodeWithText("Item a").assertIsDisplayed()
    }

    @Test
    fun `where two panes are not allowed, a wide area shows one at a time, with no empty half`() {
        render(allowsTwoPanes = false)

        composeRule.onNodeWithText("Escolha um item").assertDoesNotExist()
        composeRule.onNodeWithText("Item a").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Detalhe a").assertIsDisplayed()
        composeRule.onNodeWithText("Item a").assertDoesNotExist()
    }

    private companion object {
        const val TABLET_WIDTH = 800
        const val TABLET_HEIGHT = 1280
    }
}
