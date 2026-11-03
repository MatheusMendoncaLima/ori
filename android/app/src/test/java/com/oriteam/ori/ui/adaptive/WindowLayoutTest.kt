package com.oriteam.ori.ui.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Window sizes below are real devices in dp (portrait unless noted). */
class WindowLayoutTest {

    private val phonePortrait = windowLayoutFor(widthDp = 411, heightDp = 891)
    private val phoneLandscape = windowLayoutFor(widthDp = 891, heightDp = 411)
    private val smallPhoneLandscape = windowLayoutFor(widthDp = 640, heightDp = 360)
    private val foldableUnfolded = windowLayoutFor(widthDp = 673, heightDp = 841)
    private val tabletPortrait = windowLayoutFor(widthDp = 800, heightDp = 1280)
    private val tabletLandscape = windowLayoutFor(widthDp = 1280, heightDp = 800)

    @Test
    fun `breakpoints follow Material 3 boundaries`() {
        assertEquals(WindowWidthClass.COMPACT, windowLayoutFor(599, 800).widthClass)
        assertEquals(WindowWidthClass.MEDIUM, windowLayoutFor(600, 800).widthClass)
        assertEquals(WindowWidthClass.MEDIUM, windowLayoutFor(839, 800).widthClass)
        assertEquals(WindowWidthClass.EXPANDED, windowLayoutFor(840, 800).widthClass)
        assertEquals(WindowHeightClass.COMPACT, windowLayoutFor(800, 479).heightClass)
        assertEquals(WindowHeightClass.MEDIUM, windowLayoutFor(800, 480).heightClass)
        assertEquals(WindowHeightClass.EXPANDED, windowLayoutFor(800, 900).heightClass)
    }

    @Test
    fun `navigation follows the available space, not the device type`() {
        assertEquals(NavigationLayout.BOTTOM_BAR, phonePortrait.navigationLayout)
        assertEquals(NavigationLayout.RAIL, phoneLandscape.navigationLayout)
        assertEquals(NavigationLayout.RAIL, foldableUnfolded.navigationLayout)
        assertEquals(NavigationLayout.RAIL, tabletPortrait.navigationLayout)
        assertEquals(NavigationLayout.DRAWER, tabletLandscape.navigationLayout)
    }

    @Test
    fun `half-opened foldable keeps navigation in the lower half`() {
        val tabletop = windowLayoutFor(widthDp = 1280, heightDp = 800, isTabletop = true)

        assertEquals(NavigationLayout.BOTTOM_BAR, tabletop.navigationLayout)
    }

    @Test
    fun `list and detail share the window only when it is expanded`() {
        assertFalse(phonePortrait.showsListAndDetailTogether)
        assertFalse(foldableUnfolded.showsListAndDetailTogether)
        assertFalse(tabletPortrait.showsListAndDetailTogether)
        assertTrue(phoneLandscape.showsListAndDetailTogether)
        assertTrue(tabletLandscape.showsListAndDetailTogether)
    }

    @Test
    fun `forms get a supporting pane on wide or short-and-wide windows`() {
        assertFalse(phonePortrait.showsSupportingPane)
        assertFalse(tabletPortrait.showsSupportingPane)
        assertTrue(smallPhoneLandscape.showsSupportingPane)
        assertTrue(tabletLandscape.showsSupportingPane)
    }

    @Test
    fun `only short windows prefer side by side content`() {
        assertFalse(phonePortrait.prefersHorizontalArrangement)
        assertFalse(tabletLandscape.prefersHorizontalArrangement)
        assertTrue(phoneLandscape.prefersHorizontalArrangement)
    }

    @Test
    fun `wider windows use larger margins and larger communication symbols`() {
        assertTrue(phonePortrait.horizontalPadding < tabletLandscape.horizontalPadding)
        assertTrue(phonePortrait.aacSymbolMinWidth < tabletLandscape.aacSymbolMinWidth)
    }

    @Test
    fun `short windows keep small communication symbols so that several rows fit`() {
        assertTrue(phoneLandscape.aacSymbolMinWidth == phonePortrait.aacSymbolMinWidth)
    }
}
