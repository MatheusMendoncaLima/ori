package com.oriteam.ori.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.oriteam.ori.domain.preferences.AppPreferences
import com.oriteam.ori.domain.preferences.TextScale
import com.oriteam.ori.domain.preferences.ThemeMode
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.adaptive.WindowLayout
import com.oriteam.ori.ui.adaptive.currentWindowLayout

private val textScaleFactors = mapOf(
    TextScale.NORMAL to 1f,
    TextScale.LARGE to 1.15f,
    TextScale.LARGER to 1.3f,
    TextScale.LARGEST to 1.5f
)

/** Whether the app is dark for this choice; the system bars need the same answer. */
@Composable
@ReadOnlyComposable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * Root of the ORI design system, and the one place where preferences become appearance: brand
 * colors, typography, shapes, text size, the current [WindowLayout] and [OriAppearance].
 *
 * Dynamic (wallpaper) colors are not used because they would replace the brand identity.
 *
 * @param windowLayout overrides the measured window, for tests and previews.
 */
@Composable
fun OriTheme(
    windowLayout: WindowLayout = currentWindowLayout(),
    preferences: AppPreferences = AppPreferences(),
    content: @Composable () -> Unit
) {
    val isDark = preferences.account.theme.isDark()
    val context = LocalContext.current
    val systemRemovedAnimations = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val appearance = remember(preferences, systemRemovedAnimations) {
        OriAppearance(
            motion = OriMotion.resolve(preferences.account.motion, systemRemovedAnimations),
            isSimplified = preferences.account.isSimplified,
            density = preferences.device.density,
            listPresentation = preferences.device.listPresentation,
            isUppercase = preferences.device.isUppercase
        )
    }
    // At the normal size the system density stays untouched, with its own scaling curve.
    val systemDensity = LocalDensity.current
    val textFactor = textScaleFactors.getValue(preferences.account.textScale)
    val density = if (textFactor == 1f) {
        systemDensity
    } else {
        Density(systemDensity.density, systemDensity.fontScale * textFactor)
    }

    MaterialTheme(
        colorScheme = if (isDark) OriDarkColorScheme else OriLightColorScheme,
        typography = Typography,
        shapes = OriShapes
    ) {
        CompositionLocalProvider(
            LocalDensity provides density,
            LocalWindowLayout provides windowLayout,
            LocalOriAppearance provides appearance,
            LocalOriColors provides if (isDark) OriDarkExtraColors else OriLightExtraColors,
            content = content
        )
    }
}

/** The colors inside the app, after login: the teal as the main color ([OriLightAppColorScheme]). */
@Composable
fun OriAppColors(content: @Composable () -> Unit) {
    val isDark = LocalOriColors.current == OriDarkExtraColors
    MaterialTheme(
        colorScheme = if (isDark) OriDarkAppColorScheme else OriLightAppColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
