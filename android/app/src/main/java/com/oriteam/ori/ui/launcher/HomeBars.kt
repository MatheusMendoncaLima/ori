package com.oriteam.ori.ui.launcher

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.motion.entrance
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing

private val ToolHeight = 64.dp
private val RoundButtonSize = 56.dp
private const val DOT_MILLIS = 200

/**
 * The row at the bottom of the pages: the dock or, with "Mostrar o dock" off, the button of all the apps alone
 * ("Mostrar botões"), the way to them for whoever does not swipe up. Without either, the cells take its room.
 */
internal val LauncherSettings.hasDockRow: Boolean get() = showsDock || showsButtons

/**
 * The dock, as on Android: one row of apps without names at the bottom, the same on every page. Dragging an app here
 * puts it in the dock; while an item is carried, the dock shows its outline. With "Mostrar botões", the way to all the
 * apps sits in its middle, as on older Android phones. On a tablet, the last apps opened can follow the dock's own,
 * after a line ([RecentDockApps]), as on One UI. With "Mostrar o dock" off there is no dock: nothing is dropped on it,
 * and only the button of all the apps stays, when there is one.
 */
@Composable
internal fun HomeDock(context: HomeContext, onAllApps: (() -> Unit)?) {
    if (!context.state.settings.showsDock) {
        if (onAllApps != null) AllAppsRow(context.metrics.dockHeight, onAllApps)
        return
    }
    val drag = context.drag
    val apps = context.state.dock
    val outline = if (drag.draggedKey != null) MaterialTheme.colorScheme.outline else Color.Transparent
    Row(
        // With the button of all the apps it sits in the very middle, the apps on both sides; without it, the apps
        // share the width.
        horizontalArrangement = if (onAllApps == null) Arrangement.SpaceEvenly else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(context.metrics.dockHeight)
            .padding(horizontal = OriSpacing.Medium)
            .border(BorderStroke(1.dp, outline), MaterialTheme.shapes.extraLarge)
            .onGloballyPositioned { drag.places.dock = it.boundsInRoot() }
    ) {
        DisposableEffect(drag) { onDispose { drag.places.dock = null } }
        if (onAllApps == null) {
            // A dock filled on a wide tablet may have more apps than a phone has columns: they share the width.
            val recent = context.state.recentApps
            val slotWidth = context.metrics.gridWidth / maxOf(apps.size + recent.size, 1)
            apps.forEachIndexed { index, app -> DockApp(app, index, slotWidth, context) }
            RecentDockApps(recent, slotWidth.coerceAtMost(context.metrics.cellWidth), context)
        } else {
            DockAroundAllApps(apps, context, onAllApps)
        }
    }
}

/**
 * The dock with the button of all the apps in its very middle: the first half of the apps on its left, the rest on
 * its right, and empty slots on the shorter side, so the button never leaves the middle (one app sits to its right).
 */
@Composable
private fun DockAroundAllApps(apps: List<LauncherApp>, context: HomeContext, onAllApps: () -> Unit) {
    val left = apps.take(apps.size / 2)
    val right = apps.drop(left.size)
    val side = maxOf(left.size, right.size)
    val slotWidth = context.metrics.gridWidth / (side * 2 + 1)
    val slot = slotWidth.coerceAtMost(context.metrics.cellWidth)
    repeat(side - left.size) { Spacer(modifier = Modifier.width(slot)) }
    left.forEachIndexed { index, app -> DockApp(app, index, slotWidth, context) }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.width(slot)) {
        HomeRoundButton(OriIcons.Apps, stringResource(R.string.launcher_all_apps), onAllApps)
    }
    right.forEachIndexed { index, app -> DockApp(app, left.size + index, slotWidth, context) }
    repeat(side - right.size) { Spacer(modifier = Modifier.width(slot)) }
}

/** Without the dock, the button of all the apps alone, in the middle where it was. */
@Composable
private fun AllAppsRow(height: Dp, onAllApps: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(height)) {
        HomeRoundButton(OriIcons.Apps, stringResource(R.string.launcher_all_apps), onAllApps)
    }
}

@Composable
private fun DockApp(app: LauncherApp, index: Int, slotWidth: Dp, context: HomeContext) {
    val drag = context.drag
    val settings = context.state.settings
    val metrics = context.metrics
    val iconSize = metrics.iconSize.coerceAtMost(slotWidth - OriSpacing.Small * 2)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(slotWidth.coerceAtMost(metrics.cellWidth), metrics.dockHeight)
            .onGloballyPositioned { drag.places.placeItem(app.key, it) }
            .landsWhereDropped(app.key, drag)
            .graphicsLayer { alpha = if (drag.isCarriedOrLanding(app.key)) CARRIED_ITEM_ALPHA else 1f }
    ) {
        DisposableEffect(app.key) { onDispose { drag.places.removeItem(app.key) } }
        val badge = LocalHomeBadges.current.countOf(app)
        LauncherTile(
            label = app.label,
            description = tileDescription(app.label, badge),
            size = settings.iconSize,
            onOpen = { from -> context.viewModel.onOpenApp(app, from) },
            labelLines = 0,
            modifier = Modifier.itemActions(app.key, index, context.state.dock.lastIndex, context)
        ) {
            BadgedIcon(badge, iconSize) {
                AppIcon(app = app, size = iconSize, isMonochrome = settings.isMonochrome)
            }
        }
    }
}

/**
 * A round button of the home screen, only with "Mostrar botões": all the apps in the dock, "Editar a tela inicial" at
 * the top. On its own background, so it reads on any wallpaper.
 */
@Composable
internal fun HomeRoundButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(RoundButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Icon(icon, contentDescription = label)
    }
}

/**
 * "Remover", at the top while an item is carried, as on Android: dropped here, the item leaves the home screen (the
 * app stays in all the apps). It lights up under the finger.
 */
@Composable
internal fun RemoveZone(drag: HomeDragState, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        targetValue = if (drag.isOverRemove) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = LocalOriAppearance.current.motion.feedback(DOT_MILLIS),
        label = "RemoveZone"
    )
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = color,
        modifier = modifier
            .entrance()
            .onGloballyPositioned { drag.places.remove = it.boundsInRoot() }
    ) {
        DisposableEffect(drag) { onDispose { drag.places.remove = null } }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            modifier = Modifier.padding(horizontal = OriSpacing.Large, vertical = RemovePillPadding)
        ) {
            Icon(Icons.Filled.Close, contentDescription = null)
            Text(text = stringResource(R.string.launcher_menu_remove), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** What editing offers, as the long press of Android's home screen: wallpaper, widgets, apps and options. */
internal data class EditTools(
    val onWallpaper: () -> Unit,
    val onWidgets: () -> Unit,
    val onApps: () -> Unit,
    val onOptions: () -> Unit
)

@Composable
internal fun EditToolbar(tools: EditTools, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.entrance()
    ) {
        Row(modifier = Modifier.padding(OriSpacing.ExtraSmall)) {
            ToolButton(OriIcons.Palette, stringResource(R.string.launcher_edit_wallpaper), tools.onWallpaper)
            ToolButton(OriIcons.Widgets, stringResource(R.string.launcher_edit_widgets), tools.onWidgets)
            ToolButton(OriIcons.Apps, stringResource(R.string.launcher_edit_apps), tools.onApps)
            ToolButton(Icons.Filled.Settings, stringResource(R.string.launcher_edit_options), tools.onOptions)
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .heightIn(min = ToolHeight)
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = OriSpacing.Small, vertical = OriSpacing.ExtraSmall)
    ) {
        Icon(icon, contentDescription = null)
        Text(text = label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** "Pronto" at the top while editing, where it is always looked for. */
@Composable
internal fun DoneButton(onDone: () -> Unit, modifier: Modifier = Modifier) {
    OriPrimaryButton(
        text = stringResource(R.string.launcher_done),
        onClick = onDone,
        isFullWidth = false,
        modifier = modifier.entrance()
    )
}
