package com.oriteam.ori.ui.app.screens.today

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.oriteam.ori.R
import com.oriteam.ori.ui.app.screens.routine.RoutineStepUiModel
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberTimeFormat
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriSpacing
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private const val PROGRESS_MILLIS = 700

/** What the summary of the day says: how much is done, what comes next and how much was left earlier. */
internal data class DayGlance(val done: Int, val total: Int, val next: RoutineStepUiModel?, val earlierPending: Int)

/**
 * "A seguir" is the first activity not done whose time is still ahead, or that has no time. One whose time has
 * passed is counted apart, instead of being shown as next at three in the afternoon.
 */
internal fun glanceAt(steps: List<RoutineStepUiModel>, now: LocalTime): DayGlance {
    val pending = steps.filterNot { it.isDone }
    return DayGlance(
        done = steps.size - pending.size,
        total = steps.size,
        next = pending.firstOrNull { it.time == null || it.time >= now },
        earlierPending = pending.count { it.time != null && it.time < now }
    )
}

/** The time of day, moved on at each new minute while the screen is shown. */
@Composable
internal fun rememberCurrentMinute(): LocalTime {
    val time by produceState(LocalTime.now().truncatedTo(ChronoUnit.MINUTES)) {
        while (true) {
            val now = LocalTime.now()
            delay(ChronoUnit.MILLIS.between(now, now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)))
            value = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
        }
    }
    return time
}

/** Each state says what is true: only a successful load may say that the day has no activities. */
@Composable
internal fun TodaySummary(
    state: ContentState<List<RoutineStepUiModel>>,
    now: LocalTime,
    onRetry: () -> Unit,
    onClick: () -> Unit
) {
    when (state) {
        ContentState.Loading -> OriCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.state_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        // The card opens the Routine; the second line says so, or nothing hints that it can be tapped.
        ContentState.Empty -> OriCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.today_empty), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.today_empty_hint, stringResource(R.string.main_routine)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = OriSpacing.ExtraSmall)
            )
        }
        is ContentState.Error -> OriCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(state.messageRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            if (state.canRetry) {
                OriTextButton(text = stringResource(R.string.action_retry), onClick = onRetry)
            }
        }
        is ContentState.Content -> OriCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            DayProgress(glanceAt(state.value, now))
        }
    }
}

@Composable
private fun DayProgress(glance: DayGlance) {
    Text(
        text = pluralStringResource(R.plurals.today_progress, glance.total, glance.done, glance.total),
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(modifier = Modifier.height(OriSpacing.Small))
    // The bar fills up when the screen opens and moves when an activity is marked.
    val motion = LocalOriAppearance.current.motion
    val target = glance.done.toFloat() / glance.total
    val progress = remember { Animatable(if (motion.millis(PROGRESS_MILLIS) == 0) target else 0f) }
    LaunchedEffect(target) { progress.animateTo(target, motion.feedback(PROGRESS_MILLIS)) }
    LinearProgressIndicator(
        progress = { progress.value },
        color = LocalOriColors.current.strongOnSurface,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(OriSpacing.Small))
    val next = glance.next
    if (next == null && glance.earlierPending == 0) {
        Text(text = stringResource(R.string.today_all_done), style = MaterialTheme.typography.bodyMedium)
    }
    if (next != null) {
        Text(
            text = stringResource(R.string.today_next),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = listOfNotNull(next.time?.format(rememberTimeFormat()), next.title).joinToString(" · "),
            style = MaterialTheme.typography.titleMedium
        )
    }
    if (glance.earlierPending > 0) {
        Text(
            text = pluralStringResource(R.plurals.today_earlier_pending, glance.earlierPending, glance.earlierPending),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = OriSpacing.ExtraSmall)
        )
    }
}
