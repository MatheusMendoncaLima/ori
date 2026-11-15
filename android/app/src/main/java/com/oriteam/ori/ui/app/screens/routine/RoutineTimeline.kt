package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.motion.itemAnimation
import com.oriteam.ori.ui.components.motion.pressScale
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberTimeFormat
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriAccent
import com.oriteam.ori.ui.theme.OriDayPartColors
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.LocalTime

private const val TITLE_FADE_MILLIS = 300

private val MarkWidth = 4.dp

/** The space the colored mark takes at the start of a card. */
private val MarkLane = OriSpacing.Small * 2 + MarkWidth

/** The colored mark of an activity: a rounded bar at the start of the card, inset from its edges. */
private fun DrawScope.drawActivityMark(color: Color) {
    val inset = OriSpacing.Small.toPx()
    val width = MarkWidth.toPx()
    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - inset - width else inset
    drawRoundRect(
        color = color,
        topLeft = Offset(x, inset),
        size = Size(width, size.height - inset * 2),
        cornerRadius = CornerRadius(width / 2)
    )
}

/** Narrowest time column, at the normal text size; it grows with enlarged text. */
private val TimeColumnWidth = 52.dp

/** The widest times of the day in a 12-hour and in a 24-hour format. */
private val widestTimes = listOf(LocalTime.of(12, 59), LocalTime.of(22, 59))

/**
 * The time column fits the widest time in the current format ("12:59 PM" in English of the United States, "22:59"
 * in Portuguese), with a gap before the card. A fixed width broke "8:00 AM" into two lines.
 */
@Composable
private fun rememberTimeColumnWidth(): Dp {
    val format = rememberTimeFormat()
    val style = MaterialTheme.typography.labelMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(format, style, density) {
        val widest = widestTimes.maxOf { measurer.measure(it.format(format), style).size.width }
        maxOf(with(density) { widest.toDp() } + OriSpacing.Small, TimeColumnWidth * density.fontScale)
    }
}

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
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listKey: Any? = null
) {
    val timeWidth = rememberTimeColumnWidth()
    // Another day is another list: it shows at once, instead of every card fading out and in together.
    key(listKey) {
        LazyColumn(
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(LocalOriAppearance.current.metrics.listSpacing)
        ) {
            items(items = steps, key = { step -> step.id }) { step ->
                TimelineRow(
                    step = step,
                    timeWidth = timeWidth,
                    onToggleDone = onToggleDone,
                    onOpen = onOpen,
                    modifier = Modifier.itemAnimation(this, LocalOriAppearance.current.motion)
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    step: RoutineStepUiModel,
    timeWidth: Dp,
    onToggleDone: (String) -> Unit,
    onOpen: ((String) -> Unit)?,
    modifier: Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Text(
            text = step.time?.format(rememberTimeFormat()).orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(timeWidth)
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
    val accent = LocalOriColors.current.dayParts.of(dayPartOf(step.time))
    // A card, so the corners of every card: controls use the medium ones.
    val shape = MaterialTheme.shapes.large
    val openLabel = stringResource(R.string.activity_open_details)
    val highlight = if (step.isCurrent) Modifier.border(2.dp, colors.primary, shape) else Modifier
    val interaction = remember { MutableInteractionSource() }
    val titleColor by animateColorAsState(
        targetValue = if (step.isDone) colors.onSurfaceVariant else colors.onSurface,
        animationSpec = appearance.motion.feedback(TITLE_FADE_MILLIS),
        label = "TitleColor"
    )
    val showsMark = appearance.showsDecoration
    // The whole card opens the activity, the colored strip included; the done circle answers its own tap.
    val openModifier = onOpen?.let { open ->
        Modifier.clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            onClickLabel = openLabel,
            role = Role.Button
        ) { open(step.id) }
    } ?: Modifier
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .then(if (onOpen != null) Modifier.pressScale(interaction) else Modifier)
            .clip(shape)
            .background(if (showsMark) accent.container else colors.surface)
            // Drawn at the start of the card at its full height: no intrinsic measurement is needed for it.
            .then(if (showsMark) Modifier.drawBehind { drawActivityMark(accent.mark) } else Modifier)
            .then(highlight)
            .then(openModifier)
    ) {
        Spacer(modifier = Modifier.width(if (showsMark) MarkLane else OriSpacing.Medium))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = OriSpacing.Medium)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor
            )
            if (appearance.showsSecondaryText) ActivityBadges(step.badges)
        }
        DoneToggle(step = step, onToggle = { onToggleDone(step.id) })
    }
}

/** The part of the day of an activity, which gives it its color ([OriDayPartColors]). */
internal enum class DayPart { MORNING, AFTERNOON, NIGHT, ANY_TIME }

private const val MORNING_STARTS = 5
private const val AFTERNOON_STARTS = 12
private const val NIGHT_STARTS = 18

/**
 * Morning from 5:00, afternoon from 12:00, night from 18:00 until 5:00, and no time apart. The color of an
 * activity says when it happens, the same in Rotina and Tarefas; it used to come from the activity's id, which
 * was stable but looked random.
 */
internal fun dayPartOf(time: LocalTime?): DayPart = when {
    time == null -> DayPart.ANY_TIME
    time.hour < MORNING_STARTS || time.hour >= NIGHT_STARTS -> DayPart.NIGHT
    time.hour < AFTERNOON_STARTS -> DayPart.MORNING
    else -> DayPart.AFTERNOON
}

private fun OriDayPartColors.of(part: DayPart): OriAccent = when (part) {
    DayPart.MORNING -> morning
    DayPart.AFTERNOON -> afternoon
    DayPart.NIGHT -> night
    DayPart.ANY_TIME -> anyTime
}
