package com.oriteam.ori.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog
import com.oriteam.ori.ui.components.dialog.OriDialogHost
import com.oriteam.ori.ui.components.settings.SettingsChoice
import com.oriteam.ori.ui.components.settings.SettingsChoiceGroup
import com.oriteam.ori.ui.components.settings.SettingsSwitchItem
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InteractiveComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `tapping anywhere on a switch row toggles it and exposes the state`() {
        var isEnabled by mutableStateOf(false)
        composeRule.setContent {
            OriTheme {
                SettingsSwitchItem(
                    title = "Reduzir animações",
                    description = "Mostra apenas os movimentos essenciais",
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it }
                )
            }
        }

        composeRule.onNodeWithText("Reduzir animações").assertIsOff()
        composeRule.onNodeWithText("Mostra apenas os movimentos essenciais").performClick()

        composeRule.onNodeWithText("Reduzir animações").assertIsOn()
        assertTrue(isEnabled)
    }

    @Test
    fun `choice group selects exactly one option`() {
        var selected by mutableStateOf("subtle")
        composeRule.setContent {
            OriTheme {
                SettingsChoiceGroup(
                    title = "Animações",
                    choices = listOf(
                        SettingsChoice("expressive", "Expressivas"),
                        SettingsChoice("subtle", "Sutis"),
                        SettingsChoice("off", "Desligadas")
                    ),
                    selectedValue = selected,
                    onValueSelected = { selected = it }
                )
            }
        }

        composeRule.onNodeWithText("Desligadas").performClick()

        composeRule.onNodeWithText("Desligadas").assertIsSelected()
        composeRule.onNodeWithText("Sutis").assertIsNotSelected()
        assertEquals("off", selected)
    }

    @Test
    fun `confirmation dialog reports confirm and dismiss separately`() {
        val calls = mutableListOf<String>()
        composeRule.setContent {
            OriTheme {
                OriDialogHost {
                    OriConfirmationDialog(
                        title = "Encerrar vínculo?",
                        message = "O responsável deixará de ver suas rotinas.",
                        confirmLabel = "Encerrar",
                        dismissLabel = "Cancelar",
                        onConfirm = { calls += "confirm" },
                        onDismiss = { calls += "dismiss" },
                        isDestructive = true
                    )
                }
            }
        }

        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Encerrar").performClick()

        assertEquals(listOf("dismiss", "confirm"), calls)
    }
}
