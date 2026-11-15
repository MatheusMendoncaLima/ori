package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.PlacedHomeItem
import com.oriteam.ori.domain.launcher.canDrop
import com.oriteam.ori.domain.launcher.folderIdOf
import com.oriteam.ori.domain.launcher.isAppKey
import com.oriteam.ori.ui.theme.OriSpacing

/** Lets tests find the area while an item is carried (it is hidden from the screen reader). */
internal const val DROP_PREVIEW_TAG = "home-drop-preview"

private const val FILL_ALPHA = 0.18f
private const val OUTLINE_ALPHA = 0.6f

/** An app, a shortcut or a folder: one cell, whose frame is a choice ("Mostrar onde o app vai ficar"). */
private fun isOneCell(key: String): Boolean = isAppKey(key) || folderIdOf(key) != null

/**
 * Where the carried item will go, as One UI shows it: a soft rectangle over the cells it will take, the size of the
 * item (a widget or the clock take several). Red when it will not stay there ([canDrop]): in free icons, the cells
 * are taken by an item of another size (the item goes back; an app from all the apps goes to the first free cell),
 * or the home screen is full. Only a picture: the screen reader moves items with its own actions.
 */
@Composable
internal fun DropPreview(page: Int, allPlaced: List<PlacedHomeItem>, context: HomeContext) {
    val drag = context.drag
    val metrics = context.metrics
    val targets = context.folderTargets
    val cell by remember(drag, metrics, targets) { derivedStateOf { drag.hoveredCell(metrics, targets) } }
    val key = drag.draggedKey
    val target = cell
    if (key == null || target == null || target.page != page) return
    // An app or a folder shows it only when the person chose so; a widget and the clock always do.
    if (isOneCell(key) && !context.state.settings.showsDropPreview) return
    val grid = metrics.grid
    val span = allPlaced.firstOrNull { it.key == key }?.span ?: HomeSpan()
    val settings = context.state.settings
    val keys = remember(context.state.home) { context.state.home.map { it.key } }
    val fits = remember(key, target, settings, keys, grid) { settings.canDrop(keys, key, target, grid) }
    val tint = if (fits) LocalContentColor.current else MaterialTheme.colorScheme.error
    val columns = minOf(span.columns, grid.columns - target.column)
    val rows = minOf(span.rows, grid.rows - target.row)
    Box(
        modifier = Modifier
            .offset(metrics.cellWidth * target.column, metrics.cellHeight * target.row)
            .size(metrics.cellWidth * columns, metrics.cellHeight * rows)
            .padding(OriSpacing.ExtraSmall)
            .clip(MaterialTheme.shapes.large)
            .background(tint.copy(alpha = FILL_ALPHA))
            .border(BorderStroke(1.dp, tint.copy(alpha = OUTLINE_ALPHA)), MaterialTheme.shapes.large)
            .testTag(DROP_PREVIEW_TAG)
            .clearAndSetSemantics { }
    )
}
