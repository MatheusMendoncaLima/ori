package com.oriteam.ori.ui.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class OriNavigationScaffoldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val destinations = listOf(
        NavigationDestination(key = "routine", label = "Rotina", icon = Icons.Filled.Home),
        NavigationDestination(key = "aac", label = "Comunicação", icon = Icons.Filled.Star),
        NavigationDestination(key = "settings", label = "Configurações", icon = Icons.Filled.Settings)
    )

    private var windowWidth by mutableIntStateOf(PHONE_WIDTH)
    private var windowHeight by mutableIntStateOf(PHONE_HEIGHT)
    private var selectedKey by mutableStateOf("routine")

    private fun setContent() {
        composeRule.setContent {
            OriTestWindow(widthDp = windowWidth, heightDp = windowHeight) {
                OriNavigationScaffold(
                    destinations = destinations,
                    selectedKey = selectedKey,
                    onDestinationSelected = { selectedKey = it.key }
                ) { Text("Conteúdo") }
            }
        }
    }

    private fun itemsAreInARow(): Boolean {
        val first = composeRule.onNodeWithText("Rotina").bounds
        val second = composeRule.onNodeWithText("Configurações").bounds
        return first.top == second.top && first.left < second.left
    }

    @Test
    fun `phone portrait shows a bottom bar with every label`() {
        setContent()

        destinations.forEach { composeRule.onNodeWithText(it.label).assertIsDisplayed() }
        assertTrue(itemsAreInARow())
        val content = composeRule.onNodeWithText("Conteúdo").bounds
        assertTrue(composeRule.onNodeWithText("Rotina").bounds.top > content.top)
    }

    @Test
    fun `resizing the window switches between bottom bar and rail without losing the selection`() {
        setContent()
        composeRule.onNodeWithText("Comunicação").performClick()

        windowWidth = PHONE_HEIGHT
        windowHeight = PHONE_WIDTH
        composeRule.waitForIdle()

        // Rail labels are centered under their icons, so the centers line up vertically.
        val first = composeRule.onNodeWithText("Rotina").bounds
        val last = composeRule.onNodeWithText("Configurações").bounds
        val centerDifference = (first.left + first.right) / 2 - (last.left + last.right) / 2
        assertTrue("centers differ by $centerDifference", abs(centerDifference.value) < 1f)
        assertTrue(first.top < last.top)
        assertEquals("aac", selectedKey)

        windowWidth = PHONE_WIDTH
        windowHeight = PHONE_HEIGHT
        composeRule.waitForIdle()

        assertTrue(itemsAreInARow())
    }

    @Test
    fun `wide window shows labelled destinations stacked on the side`() {
        windowWidth = TABLET_LONG_SIDE
        windowHeight = TABLET_SHORT_SIDE
        setContent()

        val first = composeRule.onNodeWithText("Rotina").bounds
        val content = composeRule.onNodeWithText("Conteúdo").bounds
        assertTrue(first.right <= content.left)
        composeRule.onNodeWithText("Rotina", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `selected destination is exposed to accessibility services`() {
        setContent()

        composeRule.onNodeWithText("Rotina").assertIsSelected()
    }

    private companion object {
        const val PHONE_WIDTH = 411
        const val PHONE_HEIGHT = 891
        const val TABLET_LONG_SIDE = 1280
        const val TABLET_SHORT_SIDE = 800
    }
}
