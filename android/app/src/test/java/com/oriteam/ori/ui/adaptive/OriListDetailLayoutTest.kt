package com.oriteam.ori.ui.adaptive

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@RunWith(AndroidJUnit4::class)
class OriListDetailLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var windowWidth by mutableIntStateOf(PHONE_WIDTH)

    private fun setContent() {
        composeRule.setContent {
            OriTestWindow(widthDp = windowWidth, heightDp = WINDOW_HEIGHT) {
                OriListDetailLayout<String>(
                    listPane = { onItemSelected ->
                        Column {
                            listOf("a", "b").forEach { id ->
                                OriListItem(headline = "Item $id", onClick = { onItemSelected(id) })
                            }
                        }
                    },
                    detailPane = { id, _ -> Text("Detalhe $id") },
                    emptyDetailPane = { Text("Selecione um item") }
                )
            }
        }
    }

    private fun pressSystemBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun `narrow window shows one pane at a time and back returns to the list`() {
        setContent()
        composeRule.onNodeWithText("Selecione um item").assertDoesNotExist()

        composeRule.onNodeWithText("Item a").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Detalhe a").assertIsDisplayed()
        composeRule.onNodeWithText("Item b").assertDoesNotExist()

        pressSystemBack()

        composeRule.onNodeWithText("Item b").assertIsDisplayed()
        composeRule.onNodeWithText("Detalhe a").assertDoesNotExist()
    }

    @Test
    fun `wide window shows list and detail side by side`() {
        windowWidth = TABLET_WIDTH
        setContent()

        composeRule.onNodeWithText("Item a").assertIsDisplayed()
        composeRule.onNodeWithText("Selecione um item").assertIsDisplayed()

        composeRule.onNodeWithText("Item a").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Detalhe a").assertIsDisplayed()
        composeRule.onNodeWithText("Item b").assertIsDisplayed()
    }

    @Test
    fun `widening the window keeps the open detail and reveals the list`() {
        setContent()
        composeRule.onNodeWithText("Item a").performClick()
        composeRule.waitForIdle()

        windowWidth = TABLET_WIDTH
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Detalhe a").assertIsDisplayed()
        composeRule.onNodeWithText("Item b").assertIsDisplayed()
    }

    private companion object {
        const val PHONE_WIDTH = 411
        const val TABLET_WIDTH = 1280
        const val WINDOW_HEIGHT = 800
    }
}
