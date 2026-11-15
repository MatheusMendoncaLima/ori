package com.oriteam.ori.ui.launcher

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * A thumbnail is a small screen, as in the wallpaper picker of One UI: a grid of them, each at least this wide (the
 * grid makes them wider to fill the row), in the shape of the screen now: upright, or turned when the screen is
 * turned, as the previews of "Como está agora".
 */
internal val ThumbnailMinWidthUpright = 96.dp
internal val ThumbnailMinWidthTurned = 150.dp
private val SelectedBorder = 3.dp

/** Reads a small copy of a wallpaper photo, for the thumbnails (off the main thread); provided by the activity. */
val LocalWallpaperThumbnailLoader = staticCompositionLocalOf<suspend (String) -> Bitmap?> { { null } }

/** A picture that is not a wallpaper: "new photo", or one being copied. */
@Composable
internal fun IconPicture(icon: ImageVector, isBusy: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (isBusy) CircularProgressIndicator() else Icon(icon, contentDescription = null)
    }
}

@Composable
internal fun PhotoThumbnail(fileName: String) {
    val load = LocalWallpaperThumbnailLoader.current
    val picture by produceState<Bitmap?>(initialValue = null, fileName) { value = load(fileName) }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        picture?.let { image ->
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * A choice: a small screen with the wallpaper, its name under it, and a check when it is in use. With [menu],
 * holding it opens the menu (read as an action by the screen reader).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChoiceFrame(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    menu: List<MenuEntry>? = null,
    picture: @Composable () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val menuLabel = stringResource(R.string.launcher_menu_open)
    val openMenu = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        isMenuOpen = true
    }
    val outline = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val ratio = screenRatio()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = openMenu.takeIf { menu != null })
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.RadioButton
                selected = isSelected
                onClick {
                    onClick()
                    true
                }
                if (menu != null) {
                    onLongClick(menuLabel) {
                        openMenu()
                        true
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.medium)
                .border(BorderStroke(if (isSelected) SelectedBorder else 1.dp, outline), MaterialTheme.shapes.medium)
        ) {
            picture()
            if (isSelected) SelectedMark(Modifier.align(Alignment.TopEnd))
            if (menu != null) {
                AppMenu(isOpen = isMenuOpen, onDismiss = { isMenuOpen = false }, entries = menu, title = label)
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = OriSpacing.ExtraSmall)
        )
    }
}

@Composable
private fun SelectedMark(modifier: Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(OriSpacing.ExtraSmall)
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(2.dp))
    }
}
