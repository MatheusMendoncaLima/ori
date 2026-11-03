package com.oriteam.ori.ui.components.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * A quiet line that explains something: a help text, the state of the connection, a limit. The same on
 * every screen. A [isLive] notice is announced by screen readers when it appears or changes, for things
 * that happen while the person looks (the device is offline, a change waits for a connection); help that
 * is always there is not, so it is not read twice. Feedback of an action uses [FormMessage].
 */
@Composable
fun OriNotice(text: String, modifier: Modifier = Modifier, isLive: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = OriSpacing.Small)
            .then(if (isLive) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier)
    )
}
