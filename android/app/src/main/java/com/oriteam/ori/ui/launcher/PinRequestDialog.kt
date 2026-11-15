// The whole of PinRequestActivity, a window of its own opened by another app: a dialog of Android is what it shows.
@file:Suppress("ForbiddenImport")

package com.oriteam.ori.ui.launcher

import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.PinRefusal
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

private val PictureSize = 64.dp

/** What an app asks to put on the home screen: its name, the app that asks, and its picture (when it has one). */
internal sealed interface PinnedItem {
    val label: String
    val appLabel: String
    val picture: Bitmap?

    data class Shortcut(
        override val label: String,
        override val appLabel: String,
        override val picture: Bitmap?,
        val key: String
    ) : PinnedItem

    data class Widget(
        override val label: String,
        override val appLabel: String,
        override val picture: Bitmap?,
        val provider: AppWidgetProviderInfo
    ) : PinnedItem
}

/**
 * "Adicionar à tela inicial?": the picture, the name and the app that asks, so the person knows what goes there and
 * from whom. A locked or full home screen ([refusal]) says why it takes nothing and offers only to close.
 */
@Composable
internal fun PinRequestDialog(item: PinnedItem, refusal: PinRefusal?, onAdd: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = item.picture?.let { picture ->
            {
                Image(picture.asImageBitmap(), contentDescription = null, modifier = Modifier.size(PictureSize))
            }
        },
        title = { Text(text = stringResource(R.string.launcher_pin_title), textAlign = TextAlign.Center) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)
            ) {
                Text(text = item.label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    text = stringResource(R.string.launcher_pin_from, item.appLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                refusal?.let {
                    Text(
                        text = stringResource(it.message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            if (refusal == null) OriTextButton(text = stringResource(R.string.launcher_pin_add), onClick = onAdd)
        },
        dismissButton = {
            OriTextButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
        }
    )
}

private val PinRefusal.message: Int
    get() = when (this) {
        PinRefusal.LOCKED -> R.string.launcher_layout_locked
        PinRefusal.FULL -> R.string.launcher_favorites_full
    }
