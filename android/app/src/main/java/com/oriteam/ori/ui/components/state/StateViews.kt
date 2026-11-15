package com.oriteam.ori.ui.components.state

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.adaptive.FillOrScrollColumn
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.contentSwap

/**
 * Renders [state] with the matching view. [emptyContent] is required because only the
 * feature knows what "nothing here yet" means and what the user can do about it.
 */
@Composable
fun <T> StatefulContent(
    state: ContentState<T>,
    onRetry: () -> Unit,
    emptyContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val motion = LocalOriAppearance.current.motion
    // Only a change of situation is animated (loading, then the list): new data in the same list is not.
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        transitionSpec = { contentSwap(motion).using(null) },
        contentKey = { it.situation },
        label = "ContentState"
    ) { shown ->
        Box {
            when (shown) {
                ContentState.Loading -> LoadingState()
                ContentState.Empty -> emptyContent()
                is ContentState.Error -> ErrorState(
                    message = stringResource(shown.messageRes),
                    onRetry = onRetry.takeIf { shown.canRetry }
                )
                is ContentState.Content -> content(shown.value)
            }
        }
    }
}

/** Which of the four situations this is, whatever the data inside: what a cross-fade is keyed by. */
internal val ContentState<*>.situation: Int
    get() = when (this) {
        ContentState.Loading -> 0
        ContentState.Empty -> 1
        is ContentState.Error -> 2
        is ContentState.Content -> 3
    }

@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String = stringResource(R.string.state_loading)) {
    StateLayout(modifier = modifier) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(OriSpacing.Medium))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

/** Nothing to show yet. Offer the next step through [actionLabel] whenever there is one. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    StateLayout(modifier = modifier) {
        // The title already says it. The error icon stays: a warning is not ornament.
        if (LocalOriAppearance.current.showsDecoration) StateIcon(icon = icon, tint = MaterialTheme.colorScheme.primary)
        StateTexts(title = title, message = message)
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(OriSpacing.Large))
            OriPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Something failed. The message is already user-facing; technical details belong to logs. */
@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.state_error_title),
    retryLabel: String = stringResource(R.string.action_retry)
) {
    StateLayout(modifier = modifier) {
        StateIcon(icon = Icons.Filled.Warning, tint = MaterialTheme.colorScheme.error)
        StateTexts(title = title, message = message, isAnnounced = true)
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(OriSpacing.Large))
            OriSecondaryButton(text = retryLabel, onClick = onRetry)
        }
    }
}

/** Centered, width-limited and scrollable, so long messages and large fonts are never clipped. */
@Composable
private fun StateLayout(modifier: Modifier, content: @Composable () -> Unit) {
    FillOrScrollColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        CenteredContent(maxWidth = OriDimensions.FormMaxWidth) { content() }
    }
}

@Composable
private fun StateIcon(icon: ImageVector, tint: Color) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp))
    Spacer(modifier = Modifier.height(OriSpacing.Medium))
}

@Composable
private fun StateTexts(title: String, message: String, isAnnounced: Boolean = false) {
    val announce = if (isAnnounced) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(OriSpacing.Small))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = announce
    )
}
