package com.oriteam.ori.ui.launcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * While editing, a page is shown this much smaller, as on One UI (about two thirds): it reads as a page, with the
 * pages next to it in sight and room for its tools.
 */
internal const val EDITING_PAGE_SCALE = 0.7f

/** While editing, the wallpaper is dimmed this much, as on One UI: the pages and the tools stand out. */
private const val EDITING_DIM_ALPHA = 0.35f
private const val DIM_MILLIS = 260

/** The house and the bin are buttons of this height (the touch size of Android). */
private val PageToolsHeight = 48.dp

private const val SOFT_ALPHA = 0.45f
private const val CARD_ALPHA = 0.35f
private val PageShape = RoundedCornerShape(28.dp)
private val PageOutline = 1.dp
private val AddIconSize = 40.dp

/**
 * The tools of a page while editing, over its corners as on One UI: the house, at the left, makes it the main page
 * (where the Home button goes; full on the main one); at the right, the arrows change its place with the page before
 * or after ([onMove], only for free icons, whose pages are their own), and the bin takes it away (not there when it is
 * the only page, nor on a locked home screen).
 */
@Composable
internal fun PageTools(
    page: Int,
    pages: Int,
    isMain: Boolean,
    canRemove: Boolean,
    onMain: () -> Unit,
    onRemove: () -> Unit,
    onMove: ((step: Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val mainLabel = stringResource(R.string.launcher_page_main)
    val ink = LocalContentColor.current
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        IconButton(
            onClick = onMain,
            modifier = Modifier.semantics {
                contentDescription = mainLabel
                selected = isMain
            }
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = if (isMain) ink else ink.copy(alpha = SOFT_ALPHA))
        }
        Row {
            if (onMove != null && page > 0) {
                IconButton(onClick = { onMove(-1) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.launcher_page_move_earlier, page + 1)
                    )
                }
            }
            if (onMove != null && page < pages - 1) {
                IconButton(onClick = { onMove(1) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.launcher_page_move_later, page + 1)
                    )
                }
            }
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.launcher_page_remove, page + 1)
                    )
                }
            }
        }
    }
}

/** A page while editing is a card, a soft fill and an outline, so the smaller page reads as one screen. */
internal fun Modifier.pageCard(ink: Color, fill: Color): Modifier = this
    .background(fill.copy(alpha = CARD_ALPHA), PageShape)
    .border(BorderStroke(PageOutline, ink.copy(alpha = SOFT_ALPHA)), PageShape)

/**
 * Where the tools of a page go while editing: just over the smaller page (it shrinks around its middle, [gridHeight]
 * tall when whole), never higher than half the strip over the pages.
 */
internal fun pageToolsOffset(gridHeight: Dp, topStripHeight: Dp): Dp =
    maxOf(gridHeight * (1f - EDITING_PAGE_SCALE) / 2 - PageToolsHeight, -topStripHeight / 2)

/** While editing, the wallpaper is dimmed, as on One UI. The dim is read where it is drawn. */
@Composable
internal fun EditingDim(isEditing: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (isEditing) EDITING_DIM_ALPHA else 0f,
        animationSpec = LocalOriAppearance.current.motion.feedback(DIM_MILLIS),
        label = "EditingDim"
    )
    val scrim = MaterialTheme.colorScheme.scrim
    Box(modifier = Modifier.fillMaxSize().drawBehind { if (alpha > 0f) drawRect(scrim, alpha = alpha) })
}

/**
 * "+" at the end of the pages while editing: an empty page, as on One UI, as big as a page and drawn as small as
 * the pages ([scale]), in the same place.
 */
@Composable
internal fun AddPageCard(onAdd: () -> Unit, pageSize: DpSize, scale: () -> Float, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.launcher_page_add)
    Box(contentAlignment = Alignment.TopCenter, modifier = modifier.fillMaxSize()) {
        Surface(
            shape = PageShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = CARD_ALPHA),
            border = BorderStroke(PageOutline, LocalContentColor.current.copy(alpha = SOFT_ALPHA)),
            modifier = Modifier
                .size(pageSize)
                .graphicsLayer {
                    scaleX = scale()
                    scaleY = scale()
                }
                .clip(PageShape)
                .clickable(onClick = onAdd)
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    onClick {
                        onAdd()
                        true
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(OriSpacing.Medium).size(AddIconSize)
                    )
                }
            }
        }
    }
}

/** Taking away a page with items on it asks first, as on One UI: the apps stay in all the apps. */
@Composable
internal fun RemovePageDialog(page: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    OriConfirmationDialog(
        title = stringResource(R.string.launcher_page_remove_title, page + 1),
        message = stringResource(R.string.launcher_page_remove_message),
        confirmLabel = stringResource(R.string.launcher_page_remove_confirm),
        dismissLabel = stringResource(R.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true
    )
}
