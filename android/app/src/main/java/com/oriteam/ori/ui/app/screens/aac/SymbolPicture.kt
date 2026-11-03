package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.oriteam.ori.ui.components.photo.StoredPhotoImage
import com.oriteam.ori.ui.components.text.Text

/** An emoji fills about three quarters of its box, like a pictogram. */
private const val EMOJI_SHARE_OF_BOX = 0.75f

/**
 * Draws the picture of a symbol in a [size] square. It is decorative for screen readers: the
 * label next to it already says what the symbol means.
 */
@Composable
fun SymbolPicture(image: SymbolImageUi, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        when (image) {
            is SymbolImageUi.Emoji -> EmojiPicture(image.value, size)
            is SymbolImageUi.Photo -> PhotoPicture(image.path, size)
        }
    }
}

@Composable
private fun EmojiPicture(value: String, size: Dp) {
    // Converted from dp so the emoji follows the tile size, not the system text scale.
    val fontSize = with(LocalDensity.current) { (size * EMOJI_SHARE_OF_BOX).toSp() }
    Text(text = value, fontSize = fontSize, lineHeight = fontSize, textAlign = TextAlign.Center, maxLines = 1)
}

/** A photo missing on this device shows a neutral placeholder until it arrives. */
@Composable
private fun PhotoPicture(path: String?, size: Dp) {
    StoredPhotoImage(path = path, size = size, shape = MaterialTheme.shapes.medium) {
        Icon(
            imageVector = Icons.Filled.AccountBox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size)
        )
    }
}
