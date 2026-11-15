package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.text.LocalAppLocale
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.LocalDate
import java.time.format.TextStyle

/** Days of another month are there to keep the shape of the week, not to be read first. */
private const val OTHER_MONTH_ALPHA = 0.3f

/** The mark of a day of the month that has activities. */
private val ActivityDotSize = 4.dp

private const val SELECTION_MILLIS = 250

/** The circle of a newly chosen day grows from this share of its size. */
private const val SELECTION_START_SCALE = 0.6f

/** The circle of a day: the touch target stays 48 dp, the circle a step smaller, so neighbors do not touch. */
private val DayCircleSize = OriDimensions.MinTouchTarget - OriSpacing.Small

/** The height of a row of days, for a row kept empty so the month does not change height. */
internal val DayCellHeight = DayCircleSize + OriSpacing.ExtraSmall * 2

/** What a screen reader says for a day: the circle, the dimming and the dot are only seen. */
@Composable
private fun dayDescription(day: LocalDate, isDimmed: Boolean, isToday: Boolean, hasActivities: Boolean): String {
    val locale = LocalAppLocale.current
    val fullName = day.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val todayMark = stringResource(R.string.week_mark_today)
    val activitiesMark = stringResource(R.string.week_mark_has_activities)
    // A dimmed day belongs to another month, and only the eyes would know it: the month is said too.
    return if (isDimmed) {
        val month = day.month.getDisplayName(TextStyle.FULL, locale)
        stringResource(R.string.week_day_other_month_description, fullName, day.dayOfMonth, month)
    } else {
        stringResource(R.string.week_day_description, fullName, day.dayOfMonth)
    }.let { if (isToday) "$it, $todayMark" else it }
        .let { if (hasActivities) "$it, $activitiesMark" else it }
}

/** One day: its number, in a circle when selected, dimmed when it belongs to another month. */
@Composable
internal fun DayCell(
    day: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    hasActivities: Boolean = false
) {
    val colors = LocalOriColors.current
    val selectedColor = MaterialTheme.colorScheme.primary
    val motion = LocalOriAppearance.current.motion
    val selection by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = if (isSelected) motion.playful(SELECTION_MILLIS) else motion.feedback(SELECTION_MILLIS),
        label = "DaySelection"
    )
    val description = dayDescription(day, isDimmed = isDimmed, isToday = isToday, hasActivities = hasActivities)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            // The touch answers with a circle the size of the selection, not a square behind it.
            .selectable(
                selected = isSelected,
                interactionSource = null,
                indication = ripple(bounded = false, radius = DayCircleSize / 2),
                role = Role.Tab,
                onClick = onClick
            )
            .clearAndSetSemantics { contentDescription = description }
            .padding(vertical = OriSpacing.ExtraSmall)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DayCircleSize)
                .drawBehind {
                    if (selection > 0f) {
                        val grown = SELECTION_START_SCALE + (1f - SELECTION_START_SCALE) * selection
                        drawCircle(
                            selectedColor.copy(alpha = selection.coerceIn(0f, 1f)),
                            radius = size.minDimension / 2 * grown
                        )
                    }
                }
        ) {
            val textColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> colors.strongOnSurface
                    isDimmed -> MaterialTheme.colorScheme.onSurface.copy(alpha = OTHER_MONTH_ALPHA)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                animationSpec = motion.feedback(SELECTION_MILLIS),
                label = "DayText"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    maxLines = 1
                )
                // The same dot as in the Samsung calendar; the description also says it.
                if (hasActivities) {
                    Box(
                        modifier = Modifier
                            .size(ActivityDotSize)
                            .background(if (isSelected) colors.onStrong else colors.strongOnSurface, CircleShape)
                    )
                }
            }
        }
    }
}
