package com.oriteam.ori.ui.components.form

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing

private const val MESSAGE_ANIMATION_MILLIS = 250

/** Animated feedback line below a form. Hidden when [messageRes] is null. */
@Composable
fun FormMessage(
    @StringRes messageRes: Int?,
    type: FormMessageType,
    modifier: Modifier = Modifier
) = FormMessage(message = messageRes?.let { stringResource(it) }, type = type, modifier = modifier)

/** The same, for a [message] that takes values (an example in the format of the device, a name). */
@Composable
fun FormMessage(
    message: String?,
    type: FormMessageType,
    modifier: Modifier = Modifier
) {
    val motion = LocalOriAppearance.current.motion
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(motion.feedback(MESSAGE_ANIMATION_MILLIS)) +
            expandVertically(motion.feedback(MESSAGE_ANIMATION_MILLIS)),
        exit = fadeOut(motion.feedback(MESSAGE_ANIMATION_MILLIS)) +
            shrinkVertically(motion.feedback(MESSAGE_ANIMATION_MILLIS)),
        modifier = modifier
    ) {
        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = type.color,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = OriSpacing.Small)
                    // Screen readers announce the message as soon as it appears.
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}
