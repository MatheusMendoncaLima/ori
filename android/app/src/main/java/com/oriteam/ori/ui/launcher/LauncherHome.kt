package com.oriteam.ori.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.launcher.LauncherPanel
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.MAX_HOME_PAGES
import com.oriteam.ori.domain.launcher.PlacedHomeItem
import com.oriteam.ori.domain.launcher.arranged
import com.oriteam.ori.domain.launcher.pagesShown
import com.oriteam.ori.domain.launcher.wallpaperShown
import com.oriteam.ori.ui.components.state.rememberScreenRested
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A pull down this long opens the notifications (shorter ones are taken as a slip of the finger). */
private val PullDownDistance = 48.dp

/** While all the apps come up, the home screen goes up this much and fades, as on Android. */
private val HomeLift = 48.dp

/**
 * The home screen, made like Android's. The panel of ORI (the routine or the phrases) is to the left of the first
 * page; the pages go to the right, side by side, as on One UI; the dock is at the bottom of every page.
 *
 * - Drag up: all the apps come up with the finger, and settle open or closed when it lets go.
 * - Hold an item: let go to open its menu, or move to carry it (to another cell, page, the dock or "Remover").
 * - Hold the screen: edit, with the tools of Android at the bottom (wallpaper, widgets, apps, options) and "Pronto".
 *
 * Screen readers have all of it as actions of the clock and of each item, since gestures are not for everyone.
 */
@Composable
internal fun HomePane(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    slots: LauncherSlots,
    metrics: HomeMetrics
) {
    val settings = state.settings
    val panelPages = if (settings.panel == LauncherPanel.NONE) 0 else 1
    val ink = rememberHomeInk(settings.wallpaperShown)
    // One writing color for the whole home screen: the clock, the names, the dots, the hint.
    CompositionLocalProvider(LocalHomeInk provides ink, LocalContentColor provides ink.color) {
        HomeLayers(state, viewModel, slots, panelPages, metrics)
    }
}

@Composable
private fun HomeLayers(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    slots: LauncherSlots,
    panelPages: Int,
    metrics: HomeMetrics
) {
    val settings = state.settings
    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackground(settings.wallpaperShown)
        EditingDim(state.isEditingHome)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val sheet = rememberHomeDrawer(state, viewModel, with(density) { maxHeight.toPx() })
            val drag = remember { HomeDragState() }
            val pagerKeys = remember(state.home) { state.home.map { it.key } }
            val placed = remember(pagerKeys, settings, metrics.grid) { settings.arranged(pagerKeys, metrics.grid) }
            val isCarrying = drag.draggedKey != null && !state.isEditingHome
            val homePages = homePageCount(settings, placed, isCarrying, drag.landingPage)
            KeepLandingPage(drag, placed)
            // While editing, "+" waits after the last page, as on One UI.
            val canAddPage = state.isEditingHome && homePages < MAX_HOME_PAGES
            val addSlot = if (canAddPage) 1 else 0
            val mainPage = panelPages + settings.mainPage.coerceIn(0, homePages - 1)
            val pager = rememberPagerState(initialPage = mainPage) { panelPages + homePages + addSlot }
            val clock = rememberClockActions(viewModel, pager, panelPages)
            val context = remember(state, metrics, slots, clock) {
                HomeContext(state, viewModel, metrics, drag, slots, clock)
            }
            val pageEditing = rememberPageEditing(context, placed, homePages)
            ShowsAPageAfterTheLastGoes(pager, lastPage = panelPages + homePages - 1)
            LaunchedEffect(state.homeRequest) { if (state.homeRequest > 0) pager.animateScrollToPage(mainPage) }
            BackFromThePanel(pager, panelPages, isHome = state.pane == LauncherPane.HOME)
            TurnPagesAtEdges(drag, pager, panelPages..<panelPages + homePages, with(density) { maxWidth.toPx() })
            // The hold is felt on the pages and on the dock alike, so it lives on what holds both.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - sheet.value
                        translationY = -sheet.value * HomeLift.toPx()
                    }
                    .onGloballyPositioned { drag.origin = it.positionInRoot() }
                    .homeHoldGestures(drag, rememberHoldActions(context, pager, panelPages))
            ) {
                HomePages(context, pager, panelPages, placed, sheet, pageEditing)
                HomeOverlays(context, pager, panelPages, homePages, sheet)
            }
            // While an app is carried out of all the apps, the list stays (out of sight) so the finger keeps it.
            val isFromDrawer = drag.isFromOutside && state.openFolder == null
            val isDrawerShown = sheet.isVisible || state.pane == LauncherPane.DRAWER || isFromDrawer
            // Made ahead, out of sight, once the home screen has rested: a swipe up then only moves it (M18).
            val isDrawerMade = rememberScreenRested()
            val drawerState = rememberStateWhileShown(state, isDrawerShown)
            if (isDrawerShown || isDrawerMade) {
                DrawerLayer(drawerState, viewModel, sheet, rememberDrawerCarry(context), metrics, isDrawerShown)
            }
            FolderLayer(context)
            HomeItemMenu(context)
            CarriedItem(context)
            IntoFolderLanding(context)
        }
    }
}

/** All the apps, opened and closed by the finger, the button, back or an app opened, all in step with the pane. */
@Composable
private fun rememberHomeDrawer(state: LauncherUiState, viewModel: LauncherViewModel, heightPx: Float): DrawerSheet {
    val sheet = rememberDrawerSheet(heightPx) { isOpen ->
        val isDrawer = state.pane == LauncherPane.DRAWER
        if (isOpen && !isDrawer) viewModel.onOpenPane(LauncherPane.DRAWER)
        if (!isOpen && isDrawer) viewModel.onBack()
    }
    LaunchedEffect(state.pane) { sheet.animateTo(state.pane == LauncherPane.DRAWER) }
    return sheet
}

/** The pages made and the ones the items need; while carrying, an empty page waits at the end for a new page. */
private fun homePageCount(
    settings: LauncherSettings,
    placed: List<PlacedHomeItem>,
    isCarrying: Boolean,
    landingPage: Int?
): Int = maxOf(settings.pagesShown(placed) + if (isCarrying) 1 else 0, (landingPage ?: -1) + 1)
    .coerceAtMost(MAX_HOME_PAGES)

/**
 * The page of a drop stays until the drop is written (the arrangement changes), or a moment at most, for a drop
 * that changed nothing.
 */
@Composable
private fun KeepLandingPage(drag: HomeDragState, placed: List<PlacedHomeItem>) {
    LaunchedEffect(placed) { drag.landingPage = null }
    val landing = drag.landingPage
    LaunchedEffect(landing) {
        if (landing != null) {
            delay(LANDING_WAIT_MILLIS)
            drag.landingPage = null
        }
    }
    // A copy whose item never took over (the drop was not written, or its item is not shown) goes after a while.
    val item = drag.landing
    LaunchedEffect(item) {
        if (item != null) {
            delay(LANDING_WAIT_MILLIS)
            drag.finishLanding(item)
        }
    }
}

/**
 * Back on the panel of ORI goes to the first page, as back on the screen to the left of other home screens. On the
 * pages themselves back does nothing (LauncherScreen takes it): a home screen is never closed or opened again.
 */
@Composable
private fun BackFromThePanel(pager: PagerState, panelPages: Int, isHome: Boolean) {
    val scope = rememberCoroutineScope()
    BackHandler(enabled = isHome && pager.currentPage < panelPages) {
        scope.launch { pager.animateScrollToPage(panelPages) }
    }
}

@Composable
private fun HomePages(
    context: HomeContext,
    pager: PagerState,
    panelPages: Int,
    placed: List<PlacedHomeItem>,
    sheet: DrawerSheet,
    pageEditing: PageEditing
) {
    val metrics = context.metrics
    HorizontalPager(
        state = pager,
        // Only the page on the screen is built: the panel (and its routine) only while it is shown. While editing, the
        // pages next to it show at its sides, so they are built too.
        beyondViewportPageCount = if (context.isEditing) 1 else 0,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val homePage = page - panelPages
        // A page built while the pages slide is turned to, not shown for the first time: its items do not rise in.
        val arrives = remember { !pager.isScrollInProgress }
        val beside = Modifier.fillMaxSize().besideTheCurrentPage(pager, page, pageEditing.scale)
        when {
            page < panelPages -> context.slots.panel(context.state.settings.panel)
            homePage >= pageEditing.pages -> AddPageCard(
                onAdd = pageEditing.onAdd,
                pageSize = DpSize(metrics.gridWidth, metrics.cellHeight * metrics.grid.rows),
                scale = pageEditing.scale,
                modifier = beside.insidePageArea(metrics)
            )
            else -> Box(modifier = beside) { HomePageFrame(homePage, placed, context, sheet, pageEditing, arrives) }
        }
    }
}

/**
 * What a hold does on the home screen. The gesture handler starts at the first touch and lives on, so this object is
 * made once and reads the screen as it is at each hold ([rememberUpdatedState]): the grid, the editing and the dock
 * of that moment, never those of the first touch.
 */
@Composable
private fun rememberHoldActions(context: HomeContext, pager: PagerState, panelPages: Int): HoldActions {
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(context)
    val firstHomePage by rememberUpdatedState(panelPages)
    return remember(pager, haptics) {
        HoldActions(
            // While a widget is resized, its handles take the finger: a hold there does not carry it.
            isEnabled = { pager.currentPage >= firstHomePage && current.drag.resizingKey == null },
            carryStart = {
                when {
                    current.state.settings.isLayoutLocked -> CarryStart.NEVER
                    current.isEditing -> CarryStart.AT_HOLD
                    else -> CarryStart.ON_MOVE
                }
            },
            onHold = { key ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                liftHeld(key, current)
            },
            onEmpty = { touch ->
                when (touch) {
                    EmptyTouch.HOLD -> current.viewModel.onEditingChange(true)
                    // A tap on the empty screen leaves the editing, as "Pronto" does; outside it, a tap does nothing.
                    EmptyTouch.TAP -> if (current.isEditing) current.viewModel.onEditingChange(false)
                }
            },
            onMoveWhileLocked = { current.viewModel.layout.onMoveWhileLocked() },
            onDrop = { key -> dropCarried(key, current) }
        )
    }
}

@Composable
private fun rememberClockActions(viewModel: LauncherViewModel, pager: PagerState, panelPages: Int): ClockActions {
    val scope = rememberCoroutineScope()
    val hasPanel = panelPages > 0
    return remember(viewModel, pager, scope, hasPanel) {
        ClockActions(
            onAllApps = { viewModel.onOpenPane(LauncherPane.DRAWER) },
            onCustomize = { viewModel.onOpenPane(LauncherPane.OPTIONS) },
            onEdit = { viewModel.onEditingChange(true) },
            onPanel = if (hasPanel) ({ scope.launch { pager.animateScrollToPage(0) } }) else null
        )
    }
}

/**
 * A page of the home screen inside the system bars, above the dock. It does not scroll: a drag up anywhere on it
 * brings all the apps up with the finger.
 */
@Composable
private fun HomePageFrame(
    page: Int,
    placed: List<PlacedHomeItem>,
    context: HomeContext,
    sheet: DrawerSheet,
    pageEditing: PageEditing,
    arrives: Boolean
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .pullsDrawerUp(sheet, isEnabled = !context.isEditing, onPullDown = context.viewModel::onSwipeDown)
            .insidePageArea(context.metrics)
    ) {
        val items = remember(placed, page) { placed.filter { it.cell.page == page } }
        EditablePage(page, items, placed, context, pageEditing, arrives)
    }
}

/**
 * The finger moves all the apps directly; letting go settles them by the speed of the flick. A pull down while all
 * the apps are closed is not theirs: past [PullDownDistance], letting go calls [onPullDown] (Android's notifications,
 * as on One UI), once per gesture.
 */
@Composable
internal fun Modifier.pullsDrawerUp(sheet: DrawerSheet, isEnabled: Boolean, onPullDown: () -> Unit = {}): Modifier {
    val pull = remember { PullDown() }
    val distance = with(LocalDensity.current) { PullDownDistance.toPx() }
    val pullDown by rememberUpdatedState(onPullDown)
    return draggable(
        state = rememberDraggableState { delta ->
            if (sheet.value == 0f && (delta > 0f || pull.distance > 0f)) {
                pull.distance = (pull.distance + delta).coerceAtLeast(0f)
            } else {
                sheet.dragBy(delta)
            }
        },
        orientation = Orientation.Vertical,
        enabled = isEnabled,
        onDragStarted = { pull.distance = 0f },
        onDragStopped = { velocity ->
            if (pull.distance > distance) pullDown() else sheet.settle(velocity)
            pull.distance = 0f
        }
    )
}

/** How far a pull down goes, on a home screen with all the apps closed; not a state (it draws nothing). */
private class PullDown {
    var distance = 0f
}
