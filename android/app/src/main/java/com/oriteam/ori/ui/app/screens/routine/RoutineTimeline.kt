package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriSpacing

/** Width of the time column at the normal text size; it grows with enlarged text. */
private val TimeColumnWidth = 52.dp

/**
 * The day as a timeline: the time on the left, a colored card for each activity on the right. Rotina and
 * Tarefas both show the day this way, so an activity looks the same in both. Without [onOpen] (Tarefas)
 * the card only marks the activity as done.
 */
@Composable
fun RoutineTimeline(
    steps: List<RoutineStepUiModel>,
    onToggleDone: (String) -> Unit,
    onOpen: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LocalOriAppearance.current.metrics.listSpacing)
    ) {
        items(items = steps, key = { step -> step.id }) { step ->
            TimelineRow(step = step, onToggleDone = onToggleDone, onOpen = onOpen)
        }
    }
}

@Composable
private fun TimelineRow(step: RoutineStepUiModel, onToggleDone: (String) -> Unit, onOpen: ((String) -> Unit)?) {
    val fontScale = LocalDensity.current.fontScale
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = step.timeLabel.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(TimeColumnWidth * fontScale)
        )
        TimelineCard(
            step = step,
            onToggleDone = onToggleDone,
            onOpen = onOpen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimelineCard(
    step: RoutineStepUiModel,
    onToggleDone: (String) -> Unit,
    onOpen: ((String) -> Unit)?,
    modifier: Modifier
) {
    val appearance = LocalOriAppearance.current
    val colors = MaterialTheme.colorScheme
    val accents = LocalOriColors.current.accents
    val accent = accents[accentIndexOf(step, accents.size)]
    // A card, so the corners of every card: controls use the medium ones.
    val shape = MaterialTheme.shapes.large
    val openLabel = stringResource(R.string.activity_open_details)
    val highlight = if (step.isCurrent) Modifier.border(2.dp, colors.primary, shape) else Modifier
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(if (appearance.showsDecoration) accent.container else colors.surface)
            .then(highlight)
    ) {
        if (appearance.showsDecoration) {
            Box(
                modifier = Modifier
                    .padding(vertical = OriSpacing.Small, horizontal = OriSpacing.Small)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent.mark, MaterialTheme.shapes.small)
            )
        } else {
            Spacer(modifier = Modifier.width(OriSpacing.Medium))
        }
        val openModifier = onOpen?.let { open ->
            Modifier.clickable(onClickLabel = openLabel, role = Role.Button) { open(step.id) }
        } ?: Modifier
        Column(
            modifier = Modifier
                .weight(1f)
                .then(openModifier)
                .padding(vertical = OriSpacing.Medium)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (step.isDone) colors.onSurfaceVariant else colors.onSurface
            )
            if (appearance.showsSecondaryText) ActivityBadges(step.badges)
        }
        DoneToggle(step = step, onToggle = { onToggleDone(step.id) })
    }
}

/**
 * The color of an activity comes from the activity, not from its place in the list: it stays the same in
 * Rotina and Tarefas, with any filter, and on every day a repeating activity falls on.
 */
internal fun accentIndexOf(step: RoutineStepUiModel, accentCount: Int): Int =
    Math.floorMod((step.seriesId ?: step.id).hashCode(), accentCount)
