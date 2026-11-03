package com.oriteam.ori.ui.components.state

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatefulContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var retries = 0

    private fun render(state: ContentState<String>) {
        composeRule.setContent {
            OriTheme {
                StatefulContent(
                    state = state,
                    onRetry = { retries++ },
                    emptyContent = { EmptyState(title = "Nenhuma rotina", message = "Crie a primeira rotina") }
                ) { value -> Text(value) }
            }
        }
    }

    @Test
    fun `loading shows only the progress message`() {
        render(ContentState.Loading)

        composeRule.onNodeWithText("Carregando…").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar novamente").assertDoesNotExist()
    }

    @Test
    fun `empty shows the feature specific message`() {
        render(ContentState.Empty)

        composeRule.onNodeWithText("Nenhuma rotina").assertIsDisplayed()
    }

    @Test
    fun `error shows the user message and retries on request`() {
        render(ContentState.Error(R.string.error_network_unavailable))

        composeRule.onNodeWithText("Sem conexão. Verifique sua internet e tente novamente").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar novamente").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `non retryable error offers no retry button`() {
        render(ContentState.Error(R.string.error_unknown, canRetry = false))

        composeRule.onNodeWithText("Tentar novamente").assertDoesNotExist()
    }

    @Test
    fun `content shows the loaded value`() {
        render(ContentState.Content("Rotina da manhã"))

        composeRule.onNodeWithText("Rotina da manhã").assertIsDisplayed()
    }
}
