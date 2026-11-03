package com.oriteam.ori.ui.app.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.oriteam.ori.ui.components.photo.StoredPhotoImage
import com.oriteam.ori.ui.theme.LocalOriColors

private const val ICON_SHARE_OF_AVATAR = 0.6f

/** The picture of the person in a circle, or the default icon. Decorative: the name is next to it. */
@Composable
fun ProfileAvatar(photoPath: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        StoredPhotoImage(path = photoPath, size = size, shape = CircleShape) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = LocalOriColors.current.strongOnSurface,
                modifier = Modifier.size(size * ICON_SHARE_OF_AVATAR)
            )
        }
    }
}
