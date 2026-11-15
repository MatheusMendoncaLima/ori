package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.MAX_HOME_PAGES
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import kotlinx.coroutines.delay

/** How long a message stays on the home screen: long enough to read twice, as a long message of Android. */
internal const val HOME_MESSAGE_MILLIS = 6_000L

/**
 * What sits over the pages: "Remover" while carrying, "Pronto" while editing (or, with "Mostrar botões", a button to
 * edit, in the same place), the messages, the dots, and the dock (or the tools while editing).
 */
@Composable
internal fun HomeOverlays(
    context: HomeContext,
    pager: PagerState,
    panelPages: Int,
    homePages: Int,
    sheet: DrawerSheet
) {
    val viewModel = context.viewModel
    val onHomePage = pager.currentPage >= panelPages
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        when {
            context.drag.draggedKey != null -> RemoveZone(
                context.drag,
                Modifier.align(Alignment.TopCenter).padding(top = OriSpacing.Small)
            )
            context.isEditing -> DoneButton(
                onDone = { viewModel.onEditingChange(false) },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = OriSpacing.Small, end = OriSpacing.Medium)
            )
            // Without "Mostrar botões" the home screen is edited by holding it, as on One UI.
            onHomePage && context.state.settings.showsButtons -> HomeRoundButton(
                icon = Icons.Filled.Edit,
                label = stringResource(R.string.launcher_menu_edit_home),
                onClick = { viewModel.onEditingChange(true) },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = OriSpacing.Small, end = OriSpacing.Medium)
            )
        }
        if (onHomePage) {
            HomeMessages(
                state = context.state,
                onSeen = viewModel::onMessageSeen,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = context.metrics.bottomHeight, start = OriSpacing.Medium, end = OriSpacing.Medium)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // Without the dock, the strip is only the dots: the tools of editing rise over the page, which
                    // is smaller then.
                    .heightIn(min = context.metrics.bottomHeight)
                    .pullsDrawerUp(sheet, isEnabled = !context.isEditing, onPullDown = viewModel::onSwipeDown)
            ) {
                PageDots(
                    count = homePages,
                    current = pager.currentPage - panelPages,
                    mainPage = context.state.settings.mainPage,
                    hasPanel = panelPages > 0,
                    hasAddPage = context.isEditing && homePages < MAX_HOME_PAGES
                )
                if (context.isEditing) {
                    EditToolbar(editTools(viewModel), Modifier.padding(bottom = OriSpacing.Small))
                } else {
                    val onAllApps = { viewModel.onOpenPane(LauncherPane.DRAWER) }
                    HomeDock(context, onAllApps.takeIf { context.state.settings.showsButtons })
                }
            }
        }
    }
}

/**
 * A message of the home screen (an app that would not open, Android refused the lock screen, the home screen is
 * locked), over the dots and never in the room of the dock: on a card, so it reads on any wallpaper, no wider than text
 * is read. A failure is red, a neutral word in the usual color (a change that worked shows itself: no message). Like
 * a message of Android, it goes away after a while ([HOME_MESSAGE_MILLIS]), so it does not stay over the names of the
 * icons; the screen reader says it when it shows.
 */
@Composable
private fun HomeMessages(state: LauncherUiState, onSeen: () -> Unit, modifier: Modifier = Modifier) {
    if (state.messageRes == null && state.infoRes == null) return
    LaunchedEffect(state.messageRes, state.infoRes) {
        delay(HOME_MESSAGE_MILLIS)
        onSeen()
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.widthIn(max = OriDimensions.ContentMaxWidth)
    ) {
        Column(modifier = Modifier.padding(horizontal = OriSpacing.Medium, vertical = OriSpacing.Small)) {
            FormMessage(messageRes = state.messageRes, type = FormMessageType.ERROR)
            FormMessage(messageRes = state.infoRes, type = FormMessageType.INFO)
        }
    }
}

private fun editTools(viewModel: LauncherViewModel) = EditTools(
    onWallpaper = { viewModel.onOpenPane(LauncherPane.WALLPAPER) },
    onWidgets = { viewModel.onOpenPane(LauncherPane.WIDGETS) },
    onApps = { viewModel.onOpenPane(LauncherPane.DRAWER) },
    onOptions = { viewModel.onOpenPane(LauncherPane.OPTIONS) }
)
