package com.oriteam.ori.ui.launcher

import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.domain.launcher.AppOrigin
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.domain.launcher.LauncherIconSize
import com.oriteam.ori.ui.components.motion.pressScale
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing
import kotlin.math.roundToInt

/** While organizing, the icons sink a little: the screen says "you are changing me" without moving all the time. */
private const val EDITING_SCALE = 0.9f
private const val EDITING_MILLIS = 220

/** Room for the name under an icon: the cell is this much wider than the icon. */
private val TileNameRoom = 20.dp

/**
 * The size of an icon on the home screen. Medium is One UI's own (measured on the test tablet: 113 px at 312 dpi,
 * 58 dp); huge is twice as big and a little more, for whoever has much difficulty with the movements of the hands;
 * small and large are the steps around medium.
 */
internal val LauncherIconSize.iconDp: Dp
    get() = when (this) {
        LauncherIconSize.SMALL -> 48.dp
        LauncherIconSize.MEDIUM -> 58.dp
        LauncherIconSize.LARGE -> 84.dp
        LauncherIconSize.EXTRA_LARGE -> 120.dp
        // The tiles of "Máximo" are measured as a huge icon with no name under it (the name is inside the tile).
        LauncherIconSize.MAXIMUM -> 120.dp
    }

/**
 * How many icons fit in a row of [width], never fewer than two and never more than Android puts on a tablet for
 * this size (7, 6, 5, 4 from small to huge): a wider screen gets roomier cells instead of tiny columns.
 */
internal fun LauncherIconSize.columnsFor(width: Dp): Int =
    (width / (iconDp + TileNameRoom)).toInt().coerceIn(2, maxColumns)

private val LauncherIconSize.maxColumns: Int
    get() = when (this) {
        LauncherIconSize.SMALL -> 7
        LauncherIconSize.MEDIUM -> 6
        LauncherIconSize.LARGE -> 5
        LauncherIconSize.EXTRA_LARGE -> 4
        LauncherIconSize.MAXIMUM -> 3
    }

/** The writing of the names under the icons, larger with larger icons. */
@Composable
internal fun LauncherIconSize.labelStyle(): TextStyle = when (this) {
    LauncherIconSize.SMALL -> MaterialTheme.typography.labelLarge
    LauncherIconSize.MEDIUM, LauncherIconSize.LARGE -> MaterialTheme.typography.titleSmall
    LauncherIconSize.EXTRA_LARGE -> MaterialTheme.typography.titleMedium
    LauncherIconSize.MAXIMUM -> MaterialTheme.typography.titleLarge
}

/**
 * One icon with its name, read as one button. [onOpen] is null when something else handles the tap (or a tap does
 * nothing, while editing); it gets where the icon is on the screen, so the app grows out of it, when the motion level
 * lets things move. While [isEditing] the icon sinks a little, once. It shrinks a little while pressed. In the
 * dock the name is not shown ([labelLines] 0), as on Android, but it is still what the screen reader says; on a
 * screen turned the cells are lower and the name may get one line. The screen reader says [description] (the name, or
 * more for a folder: what it is and how many apps it has). With [tile] ("Máximo"), the item is a tile filling its
 * cell, with the icon at the top and the name inside ([TileContent]).
 */
// One tile of every kind (app, shortcut, folder; in a cell, the dock or all the apps): the caller picks its look.
@Suppress("LongParameterList")
@Composable
internal fun LauncherTile(
    label: String,
    size: LauncherIconSize,
    onOpen: ((AppOrigin?) -> Unit)?,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    labelLines: Int = 2,
    description: String = label,
    tile: TileLook? = null,
    icon: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val motion = LocalOriAppearance.current.motion
    val view = LocalView.current
    val place = remember { IconPlace() }
    val open = onOpen?.let { open ->
        {
            open(place.coordinates?.takeIf { motion.movesThings && it.isAttached }?.let { originOf(it, view) })
        }
    }
    val scale = animateFloatAsState(
        targetValue = if (isEditing) EDITING_SCALE else 1f,
        animationSpec = motion.feedback(EDITING_MILLIS),
        label = "TileEditing"
    )
    val touch = Modifier
        .then(
            if (open == null) {
                Modifier
            } else {
                Modifier.clickable(interactionSource = interaction, indication = null, onClick = open)
            }
        )
        .clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            if (open != null) {
                onClick {
                    open()
                    true
                }
            }
        }
    if (tile != null) {
        TileContent(label, tile, modifier.then(touch), scale, interaction) {
            Box(modifier = Modifier.onGloballyPositioned { place.coordinates = it }) { icon() }
        }
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(touch)
            .padding(vertical = OriSpacing.Small)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .pressScale(interaction)
    ) {
        Box(modifier = Modifier.onGloballyPositioned { place.coordinates = it }) { icon() }
        if (labelLines > 0) {
            Spacer(modifier = Modifier.height(OriSpacing.Small))
            Text(
                text = label,
                // A long word ("Configurações") is split with a hyphen where the language allows, not at any letter.
                style = size.labelStyle().copy(
                    shadow = LocalHomeInk.current?.shadow,
                    hyphens = Hyphens.Auto,
                    lineBreak = LineBreak.Heading
                ),
                textAlign = TextAlign.Center,
                // The room of every line is kept, so the icons of a row stay level when one name takes two lines.
                minLines = labelLines,
                maxLines = labelLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * A tile of "Máximo": the whole cell (a little in from its edges), rounded, of the tile's color, with the icon at the
 * top and the name inside at the bottom, in two lines at most. It sinks while editing and shrinks while pressed, as an
 * icon does.
 */
@Composable
private fun TileContent(
    label: String,
    tile: TileLook,
    modifier: Modifier,
    scale: State<Float>,
    interaction: MutableInteractionSource,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(OriSpacing.ExtraSmall)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .pressScale(interaction)
            .clip(if (LocalIconShape.current == IconShape.SQUARE) RectangleShape else MaterialTheme.shapes.extraLarge)
            .background(tile.color)
    ) {
        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = OriSpacing.Medium)) { icon() }
        if (tile.showsName) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = tile.nameColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = OriSpacing.Medium, vertical = OriSpacing.Small)
            )
        }
    }
}

/** Where the icon of a tile is, read when it is tapped (not a state: a move of the icon draws nothing again). */
private class IconPlace {
    var coordinates: LayoutCoordinates? = null
}

/** The icon on the screen: its place in the window of ORI Home, moved by where that window is. */
private fun originOf(coordinates: LayoutCoordinates, view: View): AppOrigin {
    val bounds = coordinates.boundsInRoot()
    val window = IntArray(2).also(view::getLocationOnScreen)
    return AppOrigin(
        left = window[0] + bounds.left.roundToInt(),
        top = window[1] + bounds.top.roundToInt(),
        width = bounds.width.roundToInt(),
        height = bounds.height.roundToInt()
    )
}
