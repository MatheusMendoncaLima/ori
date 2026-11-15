package com.oriteam.ori.ui.components.text

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.preferences.AppPreferences
import com.oriteam.ori.domain.preferences.DevicePreferences
import com.oriteam.ori.ui.components.list.OriListItem
import com.oriteam.ori.ui.components.list.OriMenuRow
import com.oriteam.ori.ui.components.navigation.NavigationDestination
import com.oriteam.ori.ui.components.navigation.OriNavigationScaffold
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The "Aa" defect: the choice only reached the login screens, never the menus. */
@RunWith(AndroidJUnit4::class)
class UppercaseEverywhereTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the choice reaches menus, navigation, lists and titles`() {
        composeRule.setContent {
            OriTheme(preferences = AppPreferences(device = DevicePreferences(isUppercase = true))) {
                OriNavigationScaffold(
                    destinations = listOf(NavigationDestination("home", "Início", Icons.Filled.Home)),
                    selectedKey = "home",
                    onDestinationSelected = {}
                ) {
                    androidx.compose.foundation.layout.Column {
                        OriTopBar(title = "Minha rotina")
                        OriMenuRow(title = "Configurações", icon = Icons.Filled.Home, accent = null, onClick = {})
                        OriListItem(headline = "Dados pessoais")
                    }
                }
            }
        }

        listOf("INÍCIO", "MINHA ROTINA", "CONFIGURAÇÕES", "DADOS PESSOAIS").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
        composeRule.onNodeWithText("Configurações").assertDoesNotExist()
    }

    @Test
    fun `the button in any top bar switches the whole screen and back`() {
        var isUppercase by mutableStateOf(false)
        composeRule.setContent {
            OriTheme(preferences = AppPreferences(device = DevicePreferences(isUppercase = isUppercase))) {
                CompositionLocalProvider(LocalTextCaseToggle provides { isUppercase = !isUppercase }) {
                    OriTopBar(title = "Tarefas")
                }
            }
        }

        composeRule.onNodeWithText("Tarefas").assertExists()
        composeRule.onNode(isToggleable()).assertIsOff().performClick()
        composeRule.onNodeWithText("TAREFAS").assertExists()
        composeRule.onNode(isToggleable()).assertIsOn().performClick()
        composeRule.onNodeWithText("Tarefas").assertExists()
    }

    @Test
    fun `styled text keeps its styles`() {
        val styled =
            AnnotatedString("Olá ori", listOf(AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 4, 7)))

        val upper = styled.uppercasePreservingStyles()

        assertEquals("OLÁ ORI", upper.text)
        assertEquals(styled.spanStyles, upper.spanStyles)
    }
}
