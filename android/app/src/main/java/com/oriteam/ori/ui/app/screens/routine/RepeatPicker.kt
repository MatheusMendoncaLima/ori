package com.oriteam.ori.ui.app.screens.routine

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.AppLocale
import com.oriteam.ori.domain.routine.MAX_REPEAT_INTERVAL_WEEKS
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.daysOfWeekInOrder
import com.oriteam.ori.domain.routine.togglingDay
import com.oriteam.ori.ui.components.form.OriChoiceChip
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val startFormat = DateTimeFormatter.ofPattern("d/MM", AppLocale)

private data class RepeatOption(@StringRes val labelRes: Int, val rule: RepeatRule?)

private val repeatOptions = listOf(
    RepeatOption(R.string.repeat_none, null),
    RepeatOption(R.string.repeat_every_day, RepeatRule.EveryDay),
    RepeatOption(R.string.repeat_business_days, RepeatRule.BusinessDays),
    RepeatOption(R.string.repeat_weekend, RepeatRule.Weekend),
    RepeatOption(R.string.repeat_last_day, RepeatRule.LastDayOfMonth),
    RepeatOption(R.string.repeat_last_business_day, RepeatRule.LastBusinessDayOfMonth)
)

/**
 * The circles of the Samsung Clock alarm: one tap per day of the week, with a sentence saying what
 * was chosen, so nobody has to interpret the circles. The options are shortcuts for what is common.
 */
@Composable
fun RepeatPicker(rule: RepeatRule?, startDate: LocalDate, onRuleChange: (RepeatRule?) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.repeat_title), style = MaterialTheme.typography.labelMedium)
        Text(
            text = repeatSummary(rule, startDate),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        // Each day takes an equal share of the row: on a 360 dp phone a 44 dp wide, 48 dp tall target.
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeekInOrder.forEach { day ->
                DayCircle(
                    day = day,
                    isSelected = (rule as? RepeatRule.Weekly)?.days?.contains(day) == true,
                    onToggle = { onRuleChange(rule.togglingDay(day)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            modifier = Modifier.selectableGroup()
        ) {
            repeatOptions.forEach { option ->
                OriChoiceChip(
                    label = stringResource(option.labelRes),
                    isSelected = rule == option.rule,
                    onClick = { onRuleChange(option.rule) }
                )
            }
        }
        if (rule is RepeatRule.Weekly) IntervalChoice(rule, onRuleChange)
    }
}

@Composable
private fun DayCircle(day: DayOfWeek, isSelected: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val colors = LocalOriColors.current
    val outline = MaterialTheme.colorScheme.outline
    val name = day.getDisplayName(TextStyle.FULL, AppLocale)
    val letter = day.getDisplayName(TextStyle.NARROW, AppLocale).uppercase(AppLocale)
    // The whole share of the row is the target; the circle only shows the choice.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = OriDimensions.MinTouchTarget)
            .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = name }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DayCircleSize)
                .clip(CircleShape)
                .background(if (isSelected) colors.strong else Color.Transparent)
                .border(1.dp, if (isSelected) colors.strong else outline, CircleShape)
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) colors.onStrong else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Seven 48 dp circles do not fit a 360 dp phone; the target is bigger than the circle. */
private val DayCircleSize = 40.dp

@Composable
private fun IntervalChoice(rule: RepeatRule.Weekly, onRuleChange: (RepeatRule?) -> Unit) {
    Spacer(modifier = Modifier.height(OriSpacing.Small))
    Text(text = stringResource(R.string.repeat_interval), style = MaterialTheme.typography.labelMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = Modifier.selectableGroup()
    ) {
        for (weeks in 1..MAX_REPEAT_INTERVAL_WEEKS) {
            OriChoiceChip(
                label = if (weeks == 1) {
                    stringResource(R.string.repeat_interval_one)
                } else {
                    stringResource(R.string.repeat_interval_many, weeks)
                },
                isSelected = rule.intervalWeeks == weeks,
                onClick = { onRuleChange(rule.copy(intervalWeeks = weeks)) }
            )
        }
    }
}

/** The rule in one sentence, for the form and for the details of an activity. */
@Composable
fun repeatSummary(rule: RepeatRule?, startDate: LocalDate): String {
    val start = startDate.format(startFormat)
    return when (rule) {
        null -> stringResource(R.string.repeat_summary_none, start)
        RepeatRule.LastDayOfMonth -> stringResource(R.string.repeat_summary_last_day)
        RepeatRule.LastBusinessDayOfMonth -> stringResource(R.string.repeat_summary_last_business_day)
        is RepeatRule.Weekly -> weeklySummary(rule, start)
    }
}

@Composable
private fun weeklySummary(rule: RepeatRule.Weekly, start: String): String {
    val days = daysOfWeekInOrder.filter { it in rule.days }.joinToString(", ") {
        it.getDisplayName(TextStyle.SHORT, AppLocale).removeSuffix(".")
    }
    return when {
        rule.isEveryDay -> stringResource(R.string.repeat_summary_every_day, start)
        rule.isBusinessDays -> stringResource(R.string.repeat_summary_business_days, start)
        rule.isWeekend -> stringResource(R.string.repeat_summary_weekend, start)
        rule.intervalWeeks == 1 -> stringResource(R.string.repeat_summary_weekly, days, start)
        else -> stringResource(R.string.repeat_summary_interval, rule.intervalWeeks, days, start)
    }
}
