package com.oriteam.ori.ui.theme

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.preferences.ContentDensity
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.ui.app.main.MainDestination
import com.oriteam.ori.ui.app.main.startDestinationFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OriThemeRulesTest {

    @Test
    fun `system motion follows the system, and an explicit choice wins over it`() {
        assertEquals(MotionLevel.OFF, OriMotion.resolve(MotionLevel.SYSTEM, systemRemovedAnimations = true).level)
        assertEquals(
            MotionLevel.EXPRESSIVE,
            OriMotion.resolve(MotionLevel.SYSTEM, systemRemovedAnimations = false).level
        )
        assertEquals(MotionLevel.SUBTLE, OriMotion.resolve(MotionLevel.SUBTLE, systemRemovedAnimations = true).level)
    }

    @Test
    fun `each level changes the animations it should`() {
        assertTrue(OriMotion(MotionLevel.EXPRESSIVE).playful<Float>(300) is SpringSpec)
        assertEquals(300, (OriMotion(MotionLevel.EXPRESSIVE).decorative<Float>(300) as TweenSpec).durationMillis)

        assertTrue(OriMotion(MotionLevel.SUBTLE).playful<Float>(300) is TweenSpec)
        assertEquals(180, (OriMotion(MotionLevel.SUBTLE).decorative<Float>(300) as TweenSpec).durationMillis)

        // Reduced keeps only what helps to notice a change.
        assertTrue(OriMotion(MotionLevel.REDUCED).decorative<Float>(300) is SnapSpec)
        assertEquals(120, (OriMotion(MotionLevel.REDUCED).feedback<Float>(300) as TweenSpec).durationMillis)
        assertFalse(OriMotion(MotionLevel.REDUCED).animatesScrolling)

        assertTrue(OriMotion(MotionLevel.OFF).feedback<Float>(300) is SnapSpec)
        assertTrue(OriMotion(MotionLevel.EXPRESSIVE).animatesScrolling)
    }

    @Test
    fun `no density takes a list row below the touch target`() {
        ContentDensity.entries.forEach { density ->
            assertTrue(density.name, OriAppearance(density = density).metrics.listItemMinHeight >= 48.dp)
        }
    }

    @Test
    fun `the start area opens the chosen part of the app`() {
        assertEquals(MainDestination.HOME, startDestinationFor(StartArea.HOME))
        assertEquals(MainDestination.ROUTINE, startDestinationFor(StartArea.ROUTINE))
        assertEquals(MainDestination.COMMUNICATE, startDestinationFor(StartArea.COMMUNICATION))
    }

    @Test
    fun `text in the dark theme keeps the AA contrast`() {
        assertReadable(OriDarkColorScheme)
        assertTrue(contrast(OriDarkExtraColors.errorText, OriDarkColorScheme.background) >= AA_TEXT)
        assertTrue(contrast(OriDarkExtraColors.successText, OriDarkColorScheme.background) >= AA_TEXT)
        assertTrue(contrast(OriDarkExtraColors.subtleText, OriDarkColorScheme.background) >= AA_TEXT)
    }

    @Test
    fun `secondary text and the profile header keep the AA contrast in the light theme`() {
        val colors = OriLightExtraColors
        assertTrue(contrast(colors.subtleText, OriLightColorScheme.background) >= AA_TEXT)
        assertTrue(contrast(colors.fieldPlaceholder, OriLightColorScheme.surface) >= AA_TEXT)
        assertTrue(contrast(colors.onStrong, colors.strong) >= AA_TEXT)
        assertTrue(contrast(colors.onStrong, colors.strongSoft) >= AA_TEXT)
    }

    @Test
    fun `the name in the profile header stays readable along the whole gradient`() {
        assertTrue(contrast(OriDarkExtraColors.onStrong, OriDarkExtraColors.strong) >= AA_TEXT)
        assertTrue(contrast(OriDarkExtraColors.onStrong, OriDarkExtraColors.strongSoft) >= AA_TEXT)
    }

    @Test
    fun `dark teal on a background and text on the colored rows keep the AA contrast`() {
        val scheme = OriDarkColorScheme
        listOf(scheme.background, scheme.surface).forEach { background ->
            assertTrue(contrast(OriDarkExtraColors.strongOnSurface, background) >= AA_TEXT)
        }
        OriDarkExtraColors.accents.forEach { accent ->
            assertTrue(contrast(scheme.onSurface, accent.container) >= AA_TEXT)
            assertTrue(contrast(scheme.onSurfaceVariant, accent.container) >= AA_TEXT)
        }
    }

    @Test
    fun `text on every soft color keeps the AA contrast, in both themes`() {
        listOf(OriLightColorScheme to OriLightExtraColors, OriDarkColorScheme to OriDarkExtraColors)
            .forEach { (scheme, extra) ->
                softColors(extra).forEach { (name, accent) ->
                    assertTrue("$name", contrast(scheme.onSurface, accent.container) >= AA_TEXT)
                    assertTrue("$name", contrast(scheme.onSurfaceVariant, accent.container) >= AA_TEXT)
                }
            }
    }

    @Test
    fun `the icon of a settings row stands out from its tile, in both themes`() {
        listOf(OriLightColorScheme, OriDarkColorScheme).forEach { scheme ->
            assertTrue(contrast(scheme.primary, scheme.primaryContainer) >= AA_GRAPHICS)
        }
    }

    @Test
    fun `inside the app the teal is the main color, readable in both themes`() {
        assertReadable(OriLightAppColorScheme)
        assertReadable(OriDarkAppColorScheme)
        // The same teal as the selected tabs and the action pill, not a second main color.
        assertEquals(OriLightExtraColors.strong, OriLightAppColorScheme.primary)
        assertEquals(OriDarkExtraColors.strongOnSurface, OriDarkAppColorScheme.primary)
    }

    @Test
    fun `a switch that is off can be seen, in both themes`() {
        listOf(OriLightColorScheme, OriDarkColorScheme).forEach { scheme ->
            listOf(scheme.background, scheme.surface).forEach { background ->
                // The border and the thumb of an off switch (SettingsSwitchItem).
                assertTrue(contrast(scheme.onSurfaceVariant, background) >= AA_GRAPHICS)
            }
        }
    }

    @Test
    fun `the edge of a field, a choice or the done circle can be seen, in both themes`() {
        listOf(OriLightColorScheme, OriDarkColorScheme).forEach { scheme ->
            listOf(scheme.background, scheme.surface).forEach { background ->
                // The light theme had a blue-gray of 1.5:1: a white field on the light background almost disappeared.
                assertTrue(contrast(scheme.outline, background) >= AA_GRAPHICS)
            }
        }
    }

    @Test
    fun `in the dark theme a soft color can be told from the card it sits on`() {
        val card = OriDarkColorScheme.surface
        softColors(OriDarkExtraColors).forEach { (name, accent) ->
            // The first dark colors were almost the card's gray: the rows and symbols looked uncolored.
            assertTrue("$name", distance(accent.container, card) >= MIN_VISIBLE_DISTANCE)
        }
    }

    private fun softColors(extra: OriExtraColors): List<Pair<String, OriAccent>> =
        extra.accents.mapIndexed { index, accent -> "area $index" to accent } + listOf(
            "person" to extra.words.person,
            "action" to extra.words.action,
            "feeling" to extra.words.feeling,
            "social" to extra.words.social,
            "morning" to extra.dayParts.morning,
            "afternoon" to extra.dayParts.afternoon,
            "night" to extra.dayParts.night,
            "any time" to extra.dayParts.anyTime,
            "settings: accessibility" to extra.settings.accessibility,
            "settings: theme" to extra.settings.theme,
            "settings: communication" to extra.settings.communication,
            "settings: screen" to extra.settings.screen,
            "settings: privacy" to extra.settings.privacy,
            "settings: about" to extra.settings.about
        )

    /** How far apart two colors are, channel by channel, from 0 to 765. */
    private fun distance(first: Color, second: Color): Float =
        (
            kotlin.math.abs(first.red - second.red) + kotlin.math.abs(first.green - second.green) +
                kotlin.math.abs(first.blue - second.blue)
            ) * CHANNEL_MAX

    private fun assertReadable(scheme: ColorScheme) {
        listOf(
            "text on background" to (scheme.onBackground to scheme.background),
            "text on surface" to (scheme.onSurface to scheme.surface),
            "secondary text on surface" to (scheme.onSurfaceVariant to scheme.surface),
            "secondary text on background" to (scheme.onSurfaceVariant to scheme.background),
            "button text" to (scheme.onPrimary to scheme.primary),
            "text on selection" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "accent on surface" to (scheme.primary to scheme.surface)
        ).forEach { (name, pair) ->
            val ratio = contrast(pair.first, pair.second)
            assertTrue("$name: $ratio", ratio >= AA_TEXT)
        }
    }

    private fun contrast(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    private companion object {
        const val AA_TEXT = 4.5f
        const val AA_GRAPHICS = 3f
        const val LUMINANCE_OFFSET = 0.05f
        const val CHANNEL_MAX = 255f
        const val MIN_VISIBLE_DISTANCE = 40f
    }
}
