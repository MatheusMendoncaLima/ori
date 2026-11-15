package com.oriteam.ori.ui.launcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.ui.theme.LocalOriAppearance

/** A folder shows its first apps, small, two by two, as on One UI. */
private const val SHOWN_APPS = 4
private const val SHOWN_PER_ROW = 2
private const val MINI_ICON_SHARE = 0.36f

/** The folder is a soft square of the writing color of the home screen, so it shows on any wallpaper. */
private const val FOLDER_ALPHA = 0.28f
private const val CORNER_PERCENT = 30
private const val CORNER_SHARE = CORNER_PERCENT / 100f
private const val HALF = 0.5f

/** An app or folder an app is held over grows a little, with a folder drawn around it: let go, they make a folder. */
private const val TARGET_SCALE = 1.12f
private const val TARGET_MILLIS = 180
private const val TARGET_MARGIN = 0.12f

/**
 * A folder of the pages: its icon (its first apps) and its name. A tap opens it, also while editing, as on One UI;
 * the screen reader says it is a folder and how many apps it has. Held, it opens its menu or is carried like an app.
 */
@Composable
internal fun FolderCell(entry: HomeEntry.Folder, context: HomeContext, isDropTarget: Boolean, modifier: Modifier) {
    val settings = context.state.settings
    val metrics = context.metrics
    val label = folderLabel(entry)
    val count = entry.apps.size
    val badge = LocalHomeBadges.current.countOf(entry.apps)
    val folder = pluralStringResource(R.plurals.launcher_folder_description, count, label, count)
    val description = tileDescription(folder, badge)
    val tile = if (metrics.isTiles) folderTile(settings.showsLabels) else null
    val icon = if (metrics.isTiles) tileIconSize(metrics) else metrics.iconSize
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        LauncherTile(
            label = label,
            description = description,
            size = settings.iconSize,
            onOpen = { context.viewModel.folders.onOpen(entry.id) },
            isEditing = context.isEditing,
            labelLines = metrics.labelLines,
            tile = tile,
            modifier = modifier
        ) {
            DropTargetIcon(isDropTarget) {
                BadgedIcon(badge, icon) { FolderIcon(entry.apps, icon, settings.isMonochrome) }
            }
        }
    }
}

/** The name of a folder as shown: the one the person gave, or "Pasta" (in the app's language) until then. */
@Composable
internal fun folderLabel(folder: HomeEntry.Folder): String =
    folder.name.ifEmpty { stringResource(R.string.launcher_folder_default_name) }

/**
 * The icon of a folder: a soft square of [ink] (the writing color of the home screen) with its first apps, small, two
 * by two. Only a picture.
 */
@Composable
internal fun FolderIcon(
    apps: List<LauncherApp>,
    size: Dp,
    isMonochrome: Boolean,
    ink: Color = LocalContentColor.current
) {
    val mini = size * MINI_ICON_SHARE
    val gap = (size - mini * SHOWN_PER_ROW) / (SHOWN_PER_ROW + 1)
    Box(
        modifier = Modifier
            .size(size)
            .clip(LocalIconShape.current.outline())
            .background(ink.copy(alpha = FOLDER_ALPHA))
    ) {
        apps.take(SHOWN_APPS).forEachIndexed { index, app ->
            AppIcon(
                app = app,
                size = mini,
                isMonochrome = isMonochrome,
                modifier = Modifier.offset(
                    x = gap + (mini + gap) * (index % SHOWN_PER_ROW),
                    y = gap + (mini + gap) * (index / SHOWN_PER_ROW)
                )
            )
        }
    }
}

/**
 * The icon of an app or folder of the pages, which grows with a folder drawn around it while an app is held over its
 * middle ([isTarget]): let go there, they make a folder (or the app joins it). Read in the drawing, so the finger
 * moving draws only this.
 */
@Composable
internal fun DropTargetIcon(isTarget: Boolean, icon: @Composable () -> Unit) {
    val motion = LocalOriAppearance.current.motion
    val grow = animateFloatAsState(
        targetValue = if (isTarget) TARGET_SCALE else 1f,
        animationSpec = motion.feedback(TARGET_MILLIS),
        label = "FolderTarget"
    )
    val color = LocalContentColor.current.copy(alpha = FOLDER_ALPHA)
    val corner = when (LocalIconShape.current) {
        IconShape.ROUND -> HALF
        IconShape.ROUNDED -> CORNER_SHARE
        IconShape.SQUARE -> 0f
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                scaleX = grow.value
                scaleY = grow.value
            }
            .drawBehind {
                if (!isTarget) return@drawBehind
                val margin = size.width * TARGET_MARGIN
                val around = Size(size.width + margin * 2, size.height + margin * 2)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(-margin, -margin),
                    size = around,
                    cornerRadius = CornerRadius(around.width * corner)
                )
            }
    ) { icon() }
}
