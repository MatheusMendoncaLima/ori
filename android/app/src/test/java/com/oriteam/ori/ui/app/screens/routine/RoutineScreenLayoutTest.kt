package com.oriteam.ori.ui.app.screens.routine

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.testing.DAY
import com.oriteam.ori.testing.FakeRoutineRepository
import com.oriteam.ori.testing.OWNER
import com.oriteam.ori.testing.OriTestWindow
import com.oriteam.ori.testing.activity
import com.oriteam.ori.testing.fixedClock
import com.oriteam.ori.testing.snapshotOf
import com.oriteam.ori.ui.app.screens.common.LOADING_GRACE_MILLIS
import com.oriteam.ori.ui.components.text.LocalTextCaseToggle
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/** The routine decides one or two panes from the space it receives, not from the device type. */
@RunWith(AndroidJUnit4::class)
class RoutineScreenLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val routines = FakeRoutineRepository().apply {
        routine.value = snapshotOf(activity("a", title = "Escovar os dentes", description = "Depois do café"))
    }

    private fun render(widthDp: Int): RoutineViewModel {
        val viewModel = RoutineViewModel(routines, fixedClock, SavedStateHandle())
        composeRule.setContent {
            OriTestWindow(widthDp = widthDp, heightDp = 800) {
                CompositionLocalProvider(LocalTextCaseToggle provides {}) {
                    RoutineRoute(
                        ownerUid = OWNER,
                        viewer = RoutineViewer.Owner,
                        title = "Minha rotina",
                        viewModel = viewModel
                    )
                }
            }
        }
        return viewModel
    }

    @Test
    fun `open details wait for a day that is loading instead of saying the activity is gone`() {
        val viewModel = render(widthDp = 1000)
        composeRule.onNodeWithText("Escovar os dentes").performClick()

        // Saving an activity on another day moves the routine there, with the details still open.
        routines.routine.value = null
        composeRule.runOnIdle { viewModel.onSelectDate(DAY.plusDays(1)) }

        // While the next day is on its way, the details wait instead of saying the activity is gone.
        composeRule.onNodeWithText("Atividade não encontrada").assertDoesNotExist()

        // A day that takes long says so, in the list and in the details.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(LOADING_GRACE_MILLIS))
        composeRule.onNodeWithText("Atividade não encontrada").assertDoesNotExist()
        composeRule.onAllNodesWithText("Carregando…").assertCountEquals(2)
    }

    @Test
    fun `choosing another week closes the open details instead of saying the activity is gone`() {
        render(widthDp = 1000)
        composeRule.onNodeWithText("Escovar os dentes").performClick()
        composeRule.onNodeWithText("Depois do café").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Próxima semana").performClick()

        composeRule.onNodeWithText("Escolha uma atividade").assertIsDisplayed()
        composeRule.onNodeWithText("Depois do café").assertDoesNotExist()
        composeRule.onNodeWithText("Atividade não encontrada").assertDoesNotExist()
    }

    @Test
    fun `wide area shows the day and the details side by side`() {
        render(widthDp = 1000)
        composeRule.onNodeWithText("Escolha uma atividade").assertIsDisplayed()

        composeRule.onNodeWithText("Escovar os dentes").performClick()

        composeRule.onNodeWithText("Depois do café").assertIsDisplayed()
        composeRule.onNodeWithText("Adicionar atividade").assertIsDisplayed()
    }

    @Test
    fun `side by side there is one Aa, at the right edge, not one per pane`() {
        render(widthDp = 1000)
        composeRule.onAllNodesWithContentDescription("Mostrar textos em letras maiúsculas").assertCountEquals(1)

        composeRule.onNodeWithText("Escovar os dentes").performClick()
        composeRule.onAllNodesWithContentDescription("Mostrar textos em letras maiúsculas").assertCountEquals(1)
    }

    @Test
    fun `in a wide area back closes the open details instead of leaving the routine`() {
        render(widthDp = 1000)
        composeRule.onNodeWithText("Escovar os dentes").performClick()
        composeRule.onNodeWithText("Depois do café").assertIsDisplayed()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Escolha uma atividade").assertIsDisplayed()
        assertFalse(composeRule.activity.isFinishing)
    }

    @Test
    fun `narrow area shows details alone and back returns to the day`() {
        render(widthDp = 400)
        composeRule.onNodeWithText("Escolha uma atividade").assertDoesNotExist()

        composeRule.onNodeWithText("Escovar os dentes").performClick()
        composeRule.onNodeWithText("Depois do café").assertIsDisplayed()
        composeRule.onNodeWithText("Adicionar atividade").assertDoesNotExist()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Adicionar atividade").assertIsDisplayed()
    }

    @Test
    fun `an activity reserved to the caregiver offers no edit or delete to the primary user`() {
        routines.routine.value = snapshotOf(activity("r", title = "Remédio", ownerCanComplete = false))
        render(widthDp = 1000)

        composeRule.onNodeWithText("Remédio").performClick()

        composeRule.onNodeWithText("Esta atividade só pode ser alterada por quem a gerencia.").assertIsDisplayed()
        composeRule.onNodeWithText("Editar").assertDoesNotExist()
        composeRule.onNodeWithText("Excluir").assertDoesNotExist()
    }
}
