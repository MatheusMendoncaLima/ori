package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.oriteam.ori.domain.launcher.HomeGrid
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.PlacedHomeItem
import com.oriteam.ori.ui.app.screens.today.rememberCurrentMinute
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberTimeFormat
import com.oriteam.ori.ui.theme.OriSpacing

/** An icon takes this part of the smaller side of its cell, as on the home screen (with room for its name). */
private const val MINI_ICON_SHARE = 0.6f

/** A widget is a soft block over its cells, kept this part of a cell from their edges. */
private const val MINI_BLOCK_ALPHA = 0.35f
private const val MINI_BLOCK_INSET_SHARE = 0.12f
private const val CORNER_PERCENT = 25

/** The time of the clock takes this part of the height of its block. */
private const val CLOCK_TIME_SHARE = 0.55f

/**
 * The home screen as the small preview of the wallpaper screen draws it ("Como está agora"): the cells of the main
 * page with what is on them, where the person put them, and the apps of the dock, with their own icons.
 */
internal data class MiniHome(val columns: Int, val rows: Int, val items: List<MiniItem>, val dock: List<LauncherApp>)

/** One item of the preview, in cells, and what it shows. */
internal data class MiniItem(val column: Int, val row: Int, val columns: Int, val rows: Int, val look: MiniLook)

/** What an item of the preview shows: an app's icon, a folder with its apps, the clock's time, or a widget's block. */
internal sealed interface MiniLook {
    data class App(val app: LauncherApp) : MiniLook
    data class Folder(val apps: List<LauncherApp>) : MiniLook
    data object Clock : MiniLook
    data object Widget : MiniLook
}

/**
 * The preview of the home screen [placed] on [grid]: the items of [mainPage] as [entries] shows them (an app no longer
 * installed is left out, as on the home screen) and the apps of the [dock].
 */
internal fun miniHomeOf(
    placed: List<PlacedHomeItem>,
    entries: Map<String, HomeEntry>,
    grid: HomeGrid,
    mainPage: Int,
    dock: List<LauncherApp>
) = MiniHome(
    columns = grid.columns,
    rows = grid.rows,
    items = placed.filter { it.cell.page == mainPage }.mapNotNull { item ->
        val look = when (val entry = entries[item.key]) {
            is HomeEntry.App -> MiniLook.App(entry.app)
            is HomeEntry.Folder -> MiniLook.Folder(entry.apps)
            HomeEntry.Clock -> MiniLook.Clock
            is HomeEntry.Widget -> MiniLook.Widget
            null -> null
        }
        look?.let { MiniItem(item.cell.column, item.cell.row, item.span.columns, item.span.rows, it) }
    },
    dock = dock
)

/**
 * [home] drawn small, as the home screen looks: the icons of the apps, the folders with their apps, the time in the
 * clock, the widgets as blocks (a widget is drawn by its app in one place only), and the dock at the bottom, in the
 * writing color of the home screen ([ink]). Only a picture: the name under it says what it is.
 */
@Composable
internal fun MiniHomePicture(home: MiniHome, ink: Color, isMonochrome: Boolean) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(OriSpacing.Small)) {
        val cellWidth = maxWidth / home.columns
        val cellHeight = maxHeight / (home.rows + 1)
        val icon = minOf(cellWidth, cellHeight) * MINI_ICON_SHARE
        home.items.forEach { item ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(cellWidth * item.column, cellHeight * item.row)
                    .size(cellWidth * item.columns, cellHeight * item.rows)
            ) { MiniItemPicture(item, icon, ink, isMonochrome, cellHeight) }
        }
        val dockSlot = maxWidth / maxOf(home.dock.size, 1)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset(y = cellHeight * home.rows)
                .fillMaxWidth()
                .height(cellHeight)
        ) {
            home.dock.forEach { app ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.width(dockSlot)) {
                    AppIcon(app, icon, isMonochrome)
                }
            }
        }
    }
}

@Composable
private fun MiniItemPicture(item: MiniItem, icon: Dp, ink: Color, isMonochrome: Boolean, cellHeight: Dp) {
    when (val look = item.look) {
        is MiniLook.App -> AppIcon(look.app, icon, isMonochrome)
        is MiniLook.Folder -> FolderIcon(look.apps, icon, isMonochrome, ink)
        MiniLook.Clock -> {
            val size = with(LocalDensity.current) { (cellHeight * item.rows * CLOCK_TIME_SHARE).toSp() }
            Text(
                text = rememberCurrentMinute().format(rememberTimeFormat()),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = size, lineHeight = size),
                color = ink,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = OriSpacing.ExtraSmall)
            )
        }
        MiniLook.Widget -> {
            val inset = cellHeight * MINI_BLOCK_INSET_SHARE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inset)
                    .background(ink.copy(alpha = MINI_BLOCK_ALPHA), RoundedCornerShape(percent = CORNER_PERCENT))
            )
        }
    }
}
