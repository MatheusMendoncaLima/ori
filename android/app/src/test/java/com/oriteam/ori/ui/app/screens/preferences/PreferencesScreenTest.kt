package com.oriteam.ori.ui.app.screens.preferences

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.ContentDensity
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.domain.preferences.TextScale
import com.oriteam.ori.domain.preferences.ThemeMode
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<Any>()
    private val actions = PreferencesActions(
        onTextScale = { calls += it },
        onTheme = { calls += it },
        onSimplified = { calls += "simplified:$it" },
        onMotion = { calls += it },
        onDensity = { calls += it },
        onListPresentation = { calls += it },
        onStartArea = { calls += it },
        onUppercase = { calls += "uppercase:$it" }
    )

    private fun render(section: PreferenceSection, state: PreferencesUiState = PreferencesUiState()) {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 780) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PreferencesContent(
                        section = section,
                        state = state,
                        actions = actions,
                        symbolChoices = { Text("Escolhas de símbolos") }
                    )
                }
            }
        }
    }

    private fun tap(text: String) = composeRule.onNodeWithText(text).performScrollTo().performClick()

    @Test
    fun `the title of every settings page fits in one line of a phone`() {
        composeRule.setContent {
            OriTestWindow(widthDp = 360, heightDp = 780) {
                Column {
                    PreferenceSection.entries.forEach { section ->
                        OriTopBar(title = stringResource(sectionTitles.getValue(section)), onNavigateBack = {})
                    }
                }
            }
        }

        PreferenceSection.entries.forEach { section ->
            val title = composeRule.activity.getString(sectionTitles.getValue(section))
            val node = composeRule.onNode(hasText(title), useUnmergedTree = true).fetchSemanticsNode()
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            assertEquals(title, 1, layouts.single().lineCount)
        }
    }

    @Test
    fun `accessibility options call their own preference`() {
        render(PreferenceSection.ACCESSIBILITY)

        tap("Muito grande")
        tap("Letras maiúsculas")
        tap("Interface simplificada")
        tap("Desligadas")

        assertEquals(listOf(TextScale.LARGEST, "uppercase:true", "simplified:true", MotionLevel.OFF), calls)
    }

    @Test
    fun `screen options call their own preference`() {
        render(PreferenceSection.SCREEN)

        tap("Amplo (mais espaço entre os itens)")
        composeRule.onNodeWithText("Frases").performScrollTo().performClick()

        assertEquals(listOf(ContentDensity.SPACIOUS, StartArea.COMMUNICATION), calls)
    }

    @Test
    fun `the theme has its own screen`() {
        render(PreferenceSection.THEME)

        tap("Escuro")

        assertEquals(listOf<Any>(ThemeMode.DARK), calls)
    }

    @Test
    fun `the saved values appear selected`() {
        render(
            PreferenceSection.ACCESSIBILITY,
            PreferencesUiState(account = AccountPreferences(textScale = TextScale.LARGE, motion = MotionLevel.REDUCED))
        )

        composeRule.onNodeWithText("Reduzidas").performScrollTo().assertIsSelected()
    }

    @Test
    fun `communication shows the symbol choices`() {
        render(PreferenceSection.COMMUNICATION)

        composeRule.onNodeWithText("Escolhas de símbolos").performScrollTo()
    }
}
