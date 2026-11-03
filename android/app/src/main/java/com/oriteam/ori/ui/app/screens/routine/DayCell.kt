package com.oriteam.ori.ui.app.screens.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.AppLocale
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.LocalDate
import java.time.format.TextStyle

/** Days of another month are there to keep the shape of the week, not to be read first. */
private const val OTHER_MONTH_ALPHA = 0.3f

/** The mark of a day of the month that has activities. */
private val ActivityDotSize = 4.dp

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
    val fullName = day.dayOfWeek.getDisplayName(TextStyle.FULL, AppLocale)
    val todayMark = stringResource(R.string.week_mark_today)
    val activitiesMark = stringResource(R.string.week_mark_has_activities)
    // A dimmed day belongs to another month, and only the eyes would know it: the month is said too.
    val description = if (isDimmed) {
        val month = day.month.getDisplayName(TextStyle.FULL, AppLocale)
        stringResource(R.string.week_day_other_month_description, fullName, day.dayOfMonth, month)
    } else {
        stringResource(R.string.week_day_description, fullName, day.dayOfMonth)
    }.let { if (isToday) "$it, $todayMark" else it }
        .let { if (hasActivities) "$it, $activitiesMark" else it }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(vertical = OriSpacing.ExtraSmall)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(OriDimensions.MinTouchTarget - 8.dp)
                .background(if (isSelected) colors.strong else Color.Transparent, CircleShape)
        ) {
            val textColor = when {
                isSelected -> colors.onStrong
                isToday -> colors.strongOnSurface
                isDimmed -> MaterialTheme.colorScheme.onSurface.copy(alpha = OTHER_MONTH_ALPHA)
                else -> MaterialTheme.colorScheme.onSurface
            }
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
