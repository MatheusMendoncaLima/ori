package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.ui.theme.OriSpacing

private const val DIVIDER_ALPHA = 0.4f
private const val DIVIDER_SHARE = 0.5f

/**
 * The last apps opened from ORI Home, after the dock's own and a short line, as One UI's tablet shows them ("Mostrar
 * apps recentes na barra"). Each opens with a tap; held, it opens its menu or, moved, is carried to the home screen,
 * as from all the apps. Nothing shows while the list is empty or the choice is off.
 */
@Composable
internal fun RecentDockApps(apps: List<LauncherApp>, slotWidth: Dp, context: HomeContext) {
    if (apps.isEmpty()) return
    val settings = context.state.settings
    val metrics = context.metrics
    VerticalDivider(
        color = LocalContentColor.current.copy(alpha = DIVIDER_ALPHA),
        modifier = Modifier
            .height(metrics.dockHeight * DIVIDER_SHARE)
            .padding(horizontal = OriSpacing.Small)
            .clearAndSetSemantics { }
    )
    val carry = rememberDrawerCarry(context)
    val tile = DrawerTile(
        size = settings.iconSize,
        icon = metrics.iconSize.coerceAtMost(slotWidth - OriSpacing.Small * 2),
        isMonochrome = settings.isMonochrome,
        isLayoutLocked = settings.isLayoutLocked,
        labelLines = 0
    )
    apps.forEach { app ->
        val isOnHome = app.key in context.state.favoriteKeys
        val toggle = when {
            !isOnHome -> MenuEntry(R.string.launcher_menu_add_home, Icons.Filled.Add) {
                context.viewModel.onToggleFavorite(app)
            }
            settings.isLayoutLocked -> null
            else -> MenuEntry(R.string.launcher_menu_remove_home, Icons.Filled.Close) {
                context.viewModel.onToggleFavorite(app)
            }
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(slotWidth).padding(vertical = 2.dp)) {
            DrawerApp(app, tile, context.viewModel, carry) { listOfNotNull(toggle) }
        }
    }
}
