package com.oriteam.ori.ui.launcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.oriteam.ori.domain.launcher.HomeArrangement
import com.oriteam.ori.domain.launcher.PlacedHomeItem
import com.oriteam.ori.ui.theme.LocalOriAppearance

private const val SCALE_MILLIS = 260

/**
 * While editing, the pages next to the one on the screen come this part of the room the smaller page leaves, so their
 * edges show at its sides with a gap between them, as on One UI.
 */
private const val NEIGHBOR_SHIFT = 0.9f

/**
 * What editing does with the pages: how many, which is the main one, "+", the house and the bin, and how small the
 * pages are drawn ([scale], read where it is drawn: it moves while editing starts and ends).
 */
internal class PageEditing(
    val pages: Int,
    val mainPage: Int,
    val scale: () -> Float,
    val onAdd: () -> Unit,
    val onMain: (Int) -> Unit,
    val onRemove: (Int) -> Unit
)

/**
 * The pages while editing, as on One UI: a page with items asks before it goes away (its apps stay in all the apps),
 * an empty one goes at once.
 */
@Composable
internal fun rememberPageEditing(context: HomeContext, placed: List<PlacedHomeItem>, pages: Int): PageEditing {
    var asking by remember { mutableStateOf<Int?>(null) }
    val scale = animateFloatAsState(
        targetValue = if (context.isEditing) EDITING_PAGE_SCALE else 1f,
        animationSpec = LocalOriAppearance.current.motion.feedback(SCALE_MILLIS),
        label = "PageScale"
    )
    val layout = context.viewModel.layout
    asking?.let { page ->
        RemovePageDialog(
            page = page,
            onConfirm = {
                asking = null
                layout.onRemovePage(page, placed, context.metrics.grid)
            },
            onDismiss = { asking = null }
        )
    }
    return PageEditing(
        pages = pages,
        mainPage = context.state.settings.mainPage,
        scale = { scale.value },
        onAdd = { layout.onAddPage(placed) },
        onMain = { page -> layout.onMainPage(page, placed) },
        onRemove = { page ->
            if (placed.any { it.cell.page == page }) {
                asking = page
            } else {
                layout.onRemovePage(page, placed, context.metrics.grid)
            }
        }
    )
}

/**
 * One page, smaller while editing (as on One UI), with its outline and its tools over it: the house and the bin.
 * The smaller page still takes items: the drop measures the page as drawn.
 */
@Composable
internal fun EditablePage(
    page: Int,
    items: List<PlacedHomeItem>,
    allPlaced: List<PlacedHomeItem>,
    context: HomeContext,
    editing: PageEditing,
    arrives: Boolean
) {
    val isEditing = context.isEditing
    val metrics = context.metrics
    val ink = LocalContentColor.current
    val fill = MaterialTheme.colorScheme.surface
    Box(contentAlignment = Alignment.TopCenter) {
        HomePageGrid(
            page = page,
            items = items,
            rows = context.metrics.grid.rows,
            context = context,
            allPlaced = allPlaced,
            arrives = arrives,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = editing.scale()
                    scaleY = editing.scale()
                }
                .then(if (isEditing) Modifier.pageCard(ink, fill) else Modifier)
        )
        if (isEditing) {
            val settings = context.state.settings
            val hasOwnPages = settings.arrangement == HomeArrangement.FREE && !settings.isLayoutLocked
            PageTools(
                page = page,
                pages = editing.pages,
                isMain = page == editing.mainPage,
                canRemove = editing.pages > 1 && !settings.isLayoutLocked,
                onMain = { editing.onMain(page) },
                onRemove = { editing.onRemove(page) },
                onMove = { step: Int -> context.viewModel.layout.onMovePage(page, step, allPlaced, metrics.grid) }
                    .takeIf { hasOwnPages },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = pageToolsOffset(metrics.cellHeight * metrics.grid.rows, metrics.topStripHeight))
                    .width(metrics.gridWidth * EDITING_PAGE_SCALE)
            )
        }
    }
}

/**
 * Page [page] of the pager moved toward the page on the screen by part of the room the smaller pages leave: while
 * editing, the edges of the pages next to it show at its sides, as on One UI. Nothing moves when not editing.
 */
internal fun Modifier.besideTheCurrentPage(pager: PagerState, page: Int, scale: () -> Float): Modifier = graphicsLayer {
    val fromCurrent = page - pager.currentPage - pager.currentPageOffsetFraction
    translationX = -fromCurrent * size.width * (1f - scale()) * NEIGHBOR_SHIFT
}

/** After the last page is taken away while editing, the page before it is shown, not "+" that took its place. */
@Composable
internal fun ShowsAPageAfterTheLastGoes(pager: PagerState, lastPage: Int) {
    LaunchedEffect(lastPage) { if (pager.currentPage > lastPage) pager.animateScrollToPage(lastPage) }
}

/**
 * An app carried onto the pages from outside them (all the apps or an open folder): what was open goes away and the
 * home screen takes it.
 */
internal class AppCarry(
    val onStart: (key: String, bounds: Rect, fingerInRoot: Offset) -> Unit,
    val onMove: (fingerInRoot: Offset) -> Unit,
    val onDrop: () -> Unit
)

/**
 * Made once: the hold handler of each app in all the apps keeps it, so it reads the screen as it is at the drop (the
 * grid, the dock), never as it was when the app was first touched.
 */
@Composable
internal fun rememberDrawerCarry(context: HomeContext): AppCarry {
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(context)
    val drag = context.drag
    return remember(drag, haptics) {
        AppCarry(
            onStart = { key, bounds, finger ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                drag.startFromOutside(key, bounds, finger)
                current.viewModel.onOpenPane(LauncherPane.HOME)
            },
            onMove = drag::followInRoot,
            onDrop = {
                dropCarried(drag.draggedKey ?: return@AppCarry, current)
                drag.end()
            }
        )
    }
}
