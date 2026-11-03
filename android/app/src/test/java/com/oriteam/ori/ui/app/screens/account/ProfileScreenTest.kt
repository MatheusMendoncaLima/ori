package com.oriteam.ori.ui.app.screens.account

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.R
import com.oriteam.ori.domain.profile.UserProfile
import com.oriteam.ori.ui.components.text.LocalTextCaseToggle
import com.oriteam.ori.ui.theme.OriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the profile, which has no top bar, still offers the Aa button`() {
        var toggles = 0
        composeRule.setContent {
            OriTheme {
                CompositionLocalProvider(LocalTextCaseToggle provides { toggles++ }) {
                    ProfileRoute(
                        profile = UserProfile("account", "Ana"),
                        actions = ProfileActions(null, {}, {}, {}, { _, _ -> })
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.text_case_toggle_description)
        ).performClick()

        assertEquals(1, toggles)
    }
}
