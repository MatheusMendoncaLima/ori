package com.oriteam.ori.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.motion.EntranceStyle
import com.oriteam.ori.ui.components.motion.entrance
import com.oriteam.ori.ui.theme.OriSpacing

/** An open folder has at most this many columns, as on One UI; fewer when the home screen has fewer. */
private const val FOLDER_COLUMNS = 4

/** The home screen dims behind an open folder, so the folder is what is seen. */
private const val SCRIM_ALPHA = 0.32f

/** The open folder takes at most this part of the screen's height; more apps scroll inside it. */
private const val MAX_HEIGHT_SHARE = 0.7f

/**
 * The folder open over the home screen, as on One UI: the home screen dims, and a card in the middle has the name
 * (changed right there), "+" to choose its apps from a list, and its apps. A tap opens an app; holding one opens its
 * menu ("Tirar da pasta") or, moved, carries it out onto the pages. Back or a tap outside closes the folder.
 */
@Composable
internal fun FolderLayer(context: HomeContext) {
    val state = context.state
    val folders = context.viewModel.folders
    val folder = state.openFolder
    // A folder whose apps were all uninstalled while it was open is gone: it is closed, not left waiting.
    LaunchedEffect(state.openFolderId, folder == null) {
        if (state.openFolderId != null && folder == null) folders.onClose()
    }
    if (folder == null) return
    val drag = context.drag
    BackHandler(onBack = folders::onClose)
    var isChoosing by rememberSaveable(folder.id) { mutableStateOf(false) }
    // An app of it let go: the folder closes, and stays out of sight until it is gone (drawn again, it blinked).
    var isLeaving by remember(folder.id) { mutableStateOf(false) }
    val carry = rememberFolderCarry(context) { isLeaving = true }
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            // While one of its apps is carried out, the folder stays (out of sight) so the finger keeps the app.
            .graphicsLayer { alpha = if (drag.draggedKey == null && !isLeaving) 1f else 0f }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = folders::onClose
                )
                // The screen reader closes it with back or the action of the card, not with a button the size of
                // the screen.
                .clearAndSetSemantics { }
        )
        // The writing of the folder is the theme's, not the home screen's (which follows the wallpaper).
        CompositionLocalProvider(LocalHomeInk provides null) {
            FolderCard(
                folder = folder,
                context = context,
                carry = carry,
                onAddApps = { isChoosing = true },
                maxHeight = maxHeight * MAX_HEIGHT_SHARE
            )
        }
    }
    if (isChoosing) {
        AppChecklist(
            texts = ChecklistTexts(
                title = stringResource(R.string.launcher_folder_choose_title),
                hint = stringResource(R.string.launcher_folder_choose_hint)
            ),
            apps = state.apps,
            checked = folder.apps.map { it.key },
            isMonochrome = state.settings.isMonochrome,
            onDone = { chosen ->
                isChoosing = false
                folders.onChooseApps(folder.id, chosen)
            },
            onDismiss = { isChoosing = false }
        )
    }
}

@Composable
private fun FolderCard(
    folder: HomeEntry.Folder,
    context: HomeContext,
    carry: AppCarry,
    onAddApps: () -> Unit,
    maxHeight: Dp
) {
    val metrics = context.metrics
    val settings = context.state.settings
    val folders = context.viewModel.folders
    val columns = minOf(FOLDER_COLUMNS, metrics.grid.columns)
    val close = stringResource(R.string.launcher_folder_close)
    val label = folderLabel(folder)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(OriSpacing.Medium)
            .width(metrics.cellWidth * columns + OriSpacing.Medium * 2)
            .entrance(style = EntranceStyle.POP)
            .semantics {
                paneTitle = label
                customActions = listOf(
                    CustomAccessibilityAction(close) {
                        folders.onClose()
                        true
                    }
                )
            }
    ) {
        Column(modifier = Modifier.padding(vertical = OriSpacing.Small)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = OriSpacing.Small)
            ) {
                FolderNameField(
                    folder = folder,
                    isLocked = settings.isLayoutLocked,
                    onRename = { folders.onRename(folder.id, it) },
                    modifier = Modifier.weight(1f)
                )
                if (!settings.isLayoutLocked) {
                    IconButton(onClick = onAddApps) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.launcher_folder_add_apps))
                    }
                }
            }
            // The apps look as on the home screen and in all the apps: with "Máximo", the same tiles.
            val tile = drawerTileOf(settings, metrics, hasHome = true)
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(horizontal = OriSpacing.Medium, vertical = OriSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
                modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)
            ) {
                items(folder.apps, key = { it.key }) { app ->
                    DrawerApp(app, tile, context.viewModel, carry) {
                        val takeOut = MenuEntry(R.string.launcher_menu_take_out_folder, OriIcons.TakeOutOfFolder) {
                            folders.onTakeOut(app.key)
                        }
                        if (tile.isLayoutLocked) emptyList() else listOf(takeOut)
                    }
                }
            }
        }
    }
}

/**
 * An app of the open folder carried out onto the pages: the folder fades out of sight (it stays, so the finger keeps
 * the app), and it closes when the app is dropped. Made once, and reads the screen as it is at the drop.
 */
@Composable
private fun rememberFolderCarry(context: HomeContext, onLeave: () -> Unit): AppCarry {
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(context)
    val leave by rememberUpdatedState(onLeave)
    val drag = context.drag
    return remember(drag, haptics) {
        AppCarry(
            onStart = { key, bounds, finger ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                drag.startFromOutside(key, bounds, finger)
            },
            onMove = drag::followInRoot,
            onDrop = {
                leave()
                drag.draggedKey?.let { dropCarried(it, current) }
                drag.end()
                current.viewModel.folders.onClose()
            }
        )
    }
}
