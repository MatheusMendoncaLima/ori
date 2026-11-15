package com.oriteam.ori.ui.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Width buckets defined by Material 3: < 600 dp, 600–839 dp and ≥ 840 dp. */
enum class WindowWidthClass { COMPACT, MEDIUM, EXPANDED }

/** Height buckets defined by Material 3: < 480 dp, 480–899 dp and ≥ 900 dp. */
enum class WindowHeightClass { COMPACT, MEDIUM, EXPANDED }

/** How top-level destinations are presented. */
enum class NavigationLayout { BOTTOM_BAR, RAIL }

/**
 * Layout decisions derived only from the space of the current window.
 *
 * Every rule is a plain property, so it can be unit tested and read by any screen through
 * [LocalWindowLayout]. Decisions never depend on the device type ("tablet", "phone"):
 * a phone in landscape, a foldable and a resized desktop window follow the same rules.
 */
data class WindowLayout(
    val widthClass: WindowWidthClass,
    val heightClass: WindowHeightClass,
    /** Foldable half-opened like a laptop: the lower half is the comfortable touch area. */
    val isTabletop: Boolean = false
) {
    val navigationLayout: NavigationLayout
        get() = when {
            // Controls stay in reach on the lower half of a half-opened foldable.
            isTabletop || widthClass == WindowWidthClass.COMPACT -> NavigationLayout.BOTTOM_BAR
            // A bottom bar would waste the little height of a landscape phone.
            heightClass == WindowHeightClass.COMPACT -> NavigationLayout.RAIL
            // Also on wide windows: the rail keeps its labels, and a drawer took about a quarter of a tablet
            // for four destinations, squeezing the two panes of Rotina.
            else -> NavigationLayout.RAIL
        }

    /** List and detail side by side only when both panes keep a comfortable width. */
    val showsListAndDetailTogether: Boolean
        get() = widthClass == WindowWidthClass.EXPANDED

    /**
     * Forms get a brand pane beside them when the window is wide (tablet landscape,
     * desktop) or short and wide (phone landscape), instead of a stretched single column.
     */
    val showsSupportingPane: Boolean
        get() = widthClass == WindowWidthClass.EXPANDED ||
            (widthClass == WindowWidthClass.MEDIUM && heightClass == WindowHeightClass.COMPACT)

    /** Short windows place content side by side because stacking it would require scrolling. */
    val prefersHorizontalArrangement: Boolean
        get() = heightClass == WindowHeightClass.COMPACT && widthClass != WindowWidthClass.COMPACT

    /** Side margin of screen content. */
    val horizontalPadding: Dp
        get() = when (widthClass) {
            WindowWidthClass.COMPACT -> 24.dp
            WindowWidthClass.MEDIUM, WindowWidthClass.EXPANDED -> 32.dp
        }

    /** Minimum width of a communication (AAC) symbol; the grid adds columns as space grows. */
    val aacSymbolMinWidth: Dp
        get() = when {
            // Three columns on a common phone, whose grid is 312 dp wide (360 minus the margins).
            // A short window also keeps this size: larger symbols would not fit two rows.
            widthClass == WindowWidthClass.COMPACT || heightClass == WindowHeightClass.COMPACT -> 96.dp
            widthClass == WindowWidthClass.MEDIUM -> 120.dp
            else -> 136.dp
        }

    companion object {
        /** Used by previews and as the default before the real window is known. */
        val Compact = WindowLayout(WindowWidthClass.COMPACT, WindowHeightClass.MEDIUM)
    }
}

private const val MEDIUM_WIDTH_DP = 600
private const val EXPANDED_WIDTH_DP = 840
private const val MEDIUM_HEIGHT_DP = 480
private const val EXPANDED_HEIGHT_DP = 900

/** Classifies a window size in dp using the Material 3 breakpoints. */
fun windowLayoutFor(widthDp: Int, heightDp: Int, isTabletop: Boolean = false): WindowLayout {
    val widthClass = when {
        widthDp >= EXPANDED_WIDTH_DP -> WindowWidthClass.EXPANDED
        widthDp >= MEDIUM_WIDTH_DP -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.COMPACT
    }
    val heightClass = when {
        heightDp >= EXPANDED_HEIGHT_DP -> WindowHeightClass.EXPANDED
        heightDp >= MEDIUM_HEIGHT_DP -> WindowHeightClass.MEDIUM
        else -> WindowHeightClass.COMPACT
    }
    return WindowLayout(widthClass, heightClass, isTabletop)
}
