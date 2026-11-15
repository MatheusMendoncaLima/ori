package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * The voice spoke with the volume at zero, as a messaging app warns with a voice note on a silent phone: the person
 * and whoever listens see that nobody heard it, and can turn the volume up from here or close the warning. Said by
 * the screen reader as soon as it shows. It goes when the next text is said with the volume up.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VolumeOffWarning(onRaise: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = OriSpacing.Small)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(OriSpacing.Medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Only a picture: the sentence beside it says the same.
                Icon(painterResource(R.drawable.ic_volume_off), contentDescription = null)
                Text(
                    text = stringResource(R.string.aac_volume_off),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = OriSpacing.Small)
                )
            }
            OriPrimaryButton(
                text = stringResource(R.string.aac_volume_raise),
                onClick = onRaise,
                isFullWidth = false
            )
            OriTextButton(
                text = stringResource(R.string.aac_volume_dismiss),
                onClick = onDismiss,
                isFullWidth = false
            )
        }
    }
}
