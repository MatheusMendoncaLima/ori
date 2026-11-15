package com.oriteam.ori.ui.components.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.appear
import com.oriteam.ori.ui.theme.disappear

/**
 * A quiet line that explains something: a help text, the state of the connection, a limit. The same on
 * every screen. A [isLive] notice is announced by screen readers when it appears or changes, for things
 * that happen while the person looks (the device is offline, a change waits for a connection); help that
 * is always there is not, so it is not read twice. Feedback of an action uses [FormMessage].
 */
@Composable
fun OriNotice(text: String, modifier: Modifier = Modifier, isLive: Boolean = false) {
    OriNoticeText(text, modifier, isLive)
}

/**
 * A live [OriNotice] that comes and goes (offline, changes waiting, a full message): it fades in and opens
 * its space, and leaves the same way, instead of pushing what is below at once. [text] null hides it.
 */
@Composable
fun OriTransientNotice(text: String?, modifier: Modifier = Modifier) {
    val motion = LocalOriAppearance.current.motion
    // The last text stays while the notice leaves, so it does not empty out before it is gone.
    var lastText by remember { mutableStateOf(text) }
    LaunchedEffect(text) { if (text != null) lastText = text }
    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = motion.appear(),
        exit = motion.disappear(),
        label = "Notice"
    ) {
        (text ?: lastText)?.let { OriNoticeText(it, Modifier, isLive = true) }
    }
}

@Composable
private fun OriNoticeText(text: String, modifier: Modifier, isLive: Boolean) {
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
