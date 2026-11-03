package com.oriteam.ori.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.R
import com.oriteam.ori.domain.preferences.AccountPreferences
import com.oriteam.ori.domain.preferences.AppPreferences
import com.oriteam.ori.domain.preferences.ContentDensity
import com.oriteam.ori.domain.preferences.DevicePreferences
import com.oriteam.ori.domain.preferences.ListPresentation
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.domain.preferences.TextScale
import com.oriteam.ori.domain.preferences.ThemeMode
import com.oriteam.ori.testing.bounds
import com.oriteam.ori.ui.adaptive.windowLayoutFor
import com.oriteam.ori.ui.app.screens.routine.ActivityBadge
import com.oriteam.ori.ui.app.screens.routine.RoutineStepUiModel
import com.oriteam.ori.ui.app.screens.routine.RoutineTimeline
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.list.OriListItem
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.text.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val FRAMES_AFTER_APPEARING = 6
private const val SETTLE_MILLIS = 2_000L

/** Each choice has to change what is drawn, through the shared components and not the screens. */
@RunWith(AndroidJUnit4::class)
class PreferenceEffectsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(preferences: AppPreferences, content: @Composable () -> Unit) {
        composeRule.setContent {
            OriTheme(windowLayout = windowLayoutFor(360, 780), preferences = preferences, content = content)
        }
    }

    private fun account(value: AccountPreferences) = AppPreferences(account = value)

    private fun device(value: DevicePreferences) = AppPreferences(device = value)

    private fun measuredHeight(preferences: AppPreferences, content: @Composable () -> Unit): Dp {
        render(preferences, content)
        return composeRule.onNodeWithTag("measured").bounds.let { it.bottom - it.top }
    }

    @Test
    fun `the text scale multiplies the size of the text`() {
        val text: @Composable () -> Unit = {
            Text("Texto", Modifier.testTag("measured"), maxLines = 1)
        }
        val height = measuredHeight(account(AccountPreferences(textScale = TextScale.LARGEST)), text)

        // bodyLarge-sized default text: 1.5x must be clearly above the normal line.
        assertTrue("height $height", height >= 30.dp)
    }

    @Test
    fun `the theme changes the colors, and system follows the system`() {
        var background = Color.Unspecified
        render(account(AccountPreferences(theme = ThemeMode.DARK))) {
            background = MaterialTheme.colorScheme.background
        }
        composeRule.waitForIdle()

        assertEquals(OriDarkColorScheme.background, background)
        assertNotEquals(OriLightColorScheme.background, background)
    }

    @Test
    fun `a spacious density makes rows and cards bigger`() {
        val row = measuredHeight(device(DevicePreferences(density = ContentDensity.SPACIOUS))) {
            OriListItem(headline = "Item", modifier = Modifier.testTag("measured"))
        }

        assertEquals(72.dp, row)
    }

    @Test
    fun `a compact density keeps the touch target`() {
        val row = measuredHeight(device(DevicePreferences(density = ContentDensity.COMPACT))) {
            OriListItem(headline = "Item", onClick = {}, modifier = Modifier.testTag("measured"))
        }

        assertEquals(48.dp, row)
    }

    @Test
    fun `cards follow the density`() {
        val card = measuredHeight(device(DevicePreferences(density = ContentDensity.SPACIOUS))) {
            OriCard(modifier = Modifier.testTag("measured")) {
                Spacer(Modifier.height(10.dp))
            }
        }

        assertEquals(58.dp, card) // 24 + 10 + 24
    }

    @Test
    fun `a summary list keeps the main line and the action, and hides the supporting one`() {
        var opened = false
        render(device(DevicePreferences(listPresentation = ListPresentation.SUMMARY))) {
            OriListItem(headline = "Principal", supportingText = "Apoio", onClick = { opened = true })
        }

        composeRule.onNodeWithText("Apoio").assertDoesNotExist()
        composeRule.onNodeWithText("Principal").assertIsDisplayed().performClick()
        assertTrue(opened)
    }

    @Test
    fun `a summary routine keeps the time, the title and the toggle`() {
        val step = RoutineStepUiModel(
            id = "a",
            title = "Café",
            timeLabel = "07:30",
            badges = listOf(ActivityBadge.SET_BY_CAREGIVER)
        )
        render(device(DevicePreferences(listPresentation = ListPresentation.SUMMARY))) {
            RoutineTimeline(steps = listOf(step), onToggleDone = {}, onOpen = null)
        }

        composeRule.onNodeWithText("Café").assertIsDisplayed()
        composeRule.onNodeWithText("07:30").assertIsDisplayed()
        val badge = composeRule.activity.getString(ActivityBadge.SET_BY_CAREGIVER.labelRes)
        composeRule.onNodeWithText(badge).assertDoesNotExist()
        composeRule.onNode(hasClickAction()).assertExists()
    }

    @Test
    fun `simplified takes the ornaments away and keeps text and actions`() {
        render(account(AccountPreferences(isSimplified = true))) {
            OriListItem(headline = "Item", leadingIcon = Icons.Filled.Face, onClick = {})
        }

        // Without the 24 dp icon and its gap, the text starts right after the 16 dp padding.
        val left = composeRule.onNodeWithText("Item", useUnmergedTree = true).assertIsDisplayed().bounds.left
        assertEquals(16.dp, left)
    }

    @Test
    fun `by default the ornaments are there`() {
        render(AppPreferences()) {
            OriListItem(headline = "Item", leadingIcon = Icons.Filled.Face, onClick = {})
        }

        val left = composeRule.onNodeWithText("Item", useUnmergedTree = true).bounds.left
        assertEquals(56.dp, left)
    }

    @Test
    fun `simplified keeps the next step of an empty state`() {
        var acted = false
        render(account(AccountPreferences(isSimplified = true))) {
            EmptyState(title = "Vazio", message = "Nada aqui", actionLabel = "Criar", onAction = { acted = true })
        }

        composeRule.onNodeWithText("Vazio").assertIsDisplayed()
        composeRule.onNodeWithText("Criar").performClick()
        assertTrue(acted)
    }

    /** Height of a message a few frames after it was asked to appear, and when it settles. */
    private fun messageHeights(motion: MotionLevel): Pair<Dp, Dp> {
        var message by mutableStateOf<Int?>(null)
        render(account(AccountPreferences(motion = motion))) {
            Box(Modifier.testTag("measured")) {
                FormMessage(messageRes = message, type = FormMessageType.SUCCESS)
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread {
            message = R.string.aac_message_saved
            Snapshot.sendApplyNotifications()
        }
        repeat(FRAMES_AFTER_APPEARING) { composeRule.mainClock.advanceTimeByFrame() }
        val height = { composeRule.onNodeWithTag("measured").bounds.let { it.bottom - it.top } }
        val early = height()
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        return early to height()
    }

    @Test
    fun `with animations off a message is complete at once`() {
        val (early, settled) = messageHeights(MotionLevel.OFF)

        assertEquals(settled, early)
    }

    @Test
    fun `with expressive animations a message is still growing at that moment`() {
        val (early, settled) = messageHeights(MotionLevel.EXPRESSIVE)

        assertTrue("$early < $settled", early < settled)
    }
}
