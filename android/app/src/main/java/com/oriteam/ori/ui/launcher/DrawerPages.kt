package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * All the apps on pages side by side, as the home screen ("Telas"): the columns of the home screen and as many rows as
 * fit, in alphabetical order, with the dots of the pages under them. A swipe sideways turns the page; a new search
 * goes back to the first.
 */
@Composable
internal fun AppPages(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    carry: AppCarry?,
    metrics: HomeMetrics,
    isOnScreen: Boolean,
    modifier: Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = metrics.grid.columns
        val rows = ((maxHeight - DotsHeight) / metrics.cellHeight).toInt().coerceAtLeast(1)
        val pages = state.drawer.chunked(columns * rows)
        val pager = rememberPagerState { pages.size }
        LaunchedEffect(state.query) { pager.scrollToPage(0) }
        LaunchedEffect(isOnScreen) { if (!isOnScreen) pager.scrollToPage(0) }
        val onHome = state.favoriteKeys
        val tile = drawerTileOf(state.settings, metrics, hasHome = carry != null)
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = OriSpacing.Medium)) {
                    pages[page].chunked(columns).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth().height(metrics.cellHeight)) {
                            row.forEach { app ->
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                                    val isOnHome = app.key in onHome
                                    DrawerApp(app, tile, viewModel, carry) {
                                        listOfNotNull(homeToggle(app, isOnHome, tile, viewModel))
                                    }
                                }
                            }
                            // The last row of a page keeps the width of a cell for each missing app.
                            repeat(columns - row.size) { Box(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
            PageDots(
                count = pages.size,
                current = pager.currentPage,
                mainPage = NO_MAIN_PAGE,
                hasPanel = false,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/** All the apps have no main page: no page gets the house among the dots. */
private const val NO_MAIN_PAGE = -1
