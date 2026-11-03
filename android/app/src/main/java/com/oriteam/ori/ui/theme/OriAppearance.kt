package com.oriteam.ori.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.preferences.ContentDensity
import com.oriteam.ori.domain.preferences.ListPresentation

/** Sizes of each [ContentDensity]. No value lets a list row go below the 48 dp touch target. */
@Immutable
data class OriDensityMetrics(
    val cardPadding: Dp,
    val listItemMinHeight: Dp,
    val listItemVerticalPadding: Dp,
    val listSpacing: Dp,
    val symbolSpacing: Dp
)

private val densityMetrics = mapOf(
    ContentDensity.COMPACT to OriDensityMetrics(12.dp, 48.dp, 4.dp, 4.dp, 8.dp),
    ContentDensity.COMFORTABLE to OriDensityMetrics(16.dp, 56.dp, 12.dp, 8.dp, 12.dp),
    // Symbols keep the comfortable gap: a larger one would take a column away on phones.
    ContentDensity.SPACIOUS to OriDensityMetrics(24.dp, 72.dp, 16.dp, 16.dp, 12.dp)
)

/**
 * How shared components present themselves, derived from the preferences by [OriTheme]. Screens
 * do not read preferences: they use the components, and the components read this. That is what
 * keeps each choice working in the whole app, without one check per screen.
 *
 * [isSimplified] and [listPresentation] remove only ornaments and secondary text. Actions and
 * their labels stay, because controls are never what makes a screen hard to use for everyone.
 */
@Immutable
data class OriAppearance(
    val motion: OriMotion = OriMotion(),
    val isSimplified: Boolean = false,
    val density: ContentDensity = ContentDensity.COMFORTABLE,
    val listPresentation: ListPresentation = ListPresentation.DETAILED,
    val isUppercase: Boolean = false
) {
    val metrics: OriDensityMetrics get() = densityMetrics.getValue(density)
    val showsDecoration: Boolean get() = !isSimplified
    val showsSecondaryText: Boolean get() = listPresentation == ListPresentation.DETAILED
}

val LocalOriAppearance = staticCompositionLocalOf { OriAppearance() }
