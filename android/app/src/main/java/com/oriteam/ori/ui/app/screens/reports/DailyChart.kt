package com.oriteam.ori.ui.app.screens.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.reports.DayResult
import com.oriteam.ori.ui.components.text.DateSkeletons
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberDateFormat
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriSpacing

private const val CHART_DRAW_MILLIS = 900
private const val FULL_PERCENT = 100f
private const val HALF_PERCENT = 50

/** A period of one day has its point in the middle. */
private const val MIDDLE = 0.5f
private val ChartHeight = 140.dp

/**
 * Where each day of the period sits on the chart, from 0 (its first day) to 1 (its last), and how high:
 * null for a day without activities, which has no point. Being by date, a week with data only on Sunday
 * and Saturday shows the five days between, instead of a line that suggests values on them.
 */
internal fun chartPoints(days: List<DayResult>): List<Pair<Float, Float>?> = days.mapIndexed { index, day ->
    val x = if (days.size == 1) MIDDLE else index.toFloat() / (days.size - 1)
    if (day.total == 0) null else x to day.percent / FULL_PERCENT
}

/**
 * The line of the wireframe, over every day of the period: only neighboring days with activities are joined.
 * The same values are in the details, for who cannot read a chart, so the chart is one description to a
 * screen reader and its axis labels are only seen.
 */
@Composable
internal fun DailyChart(days: List<DayResult>) {
    val description = stringResource(R.string.reports_chart_description)
    // The line is revealed from the first day to the last; with less motion it is simply there.
    val motion = LocalOriAppearance.current.motion
    val drawn = remember(days) { Animatable(if (motion.movesThings) 0f else 1f) }
    LaunchedEffect(drawn) { drawn.animateTo(1f, motion.decorative(CHART_DRAW_MILLIS)) }
    Row {
        PercentAxis()
        Spacer(modifier = Modifier.width(OriSpacing.Small))
        Column(modifier = Modifier.weight(1f)) {
            ChartLine(days, progress = { drawn.value }, description = description)
            if (days.isNotEmpty()) DateLabels(days)
        }
    }
}

@Composable
private fun ChartLine(days: List<DayResult>, progress: () -> Float, description: String) {
    val lineColor = LocalOriColors.current.strongOnSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .semantics { contentDescription = description }
            .drawWithCache {
                // Built once for this size and these days; each frame only reveals a wider part.
                val lineStroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val pointRadius = 4.dp.toPx()
                val gridWidth = 1.dp.toPx()
                // Points at 0% and 100% stay whole inside the chart, instead of over the dates below it.
                val inset = pointRadius + lineStroke.width / 2
                val plotWidth = size.width - inset * 2
                val plotHeight = size.height - inset * 2
                val points = chartPoints(days).map { point ->
                    point?.let { (x, share) -> Offset(inset + plotWidth * x, inset + plotHeight * (1 - share)) }
                }
                val line = Path().apply {
                    points.zipWithNext().forEach { (from, to) ->
                        if (from != null && to != null) {
                            moveTo(from.x, from.y)
                            lineTo(to.x, to.y)
                        }
                    }
                }
                onDrawBehind {
                    repeat(3) { index ->
                        val y = inset + plotHeight * index / 2
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = gridWidth)
                    }
                    // Line and points are revealed by the same edge, so a point appears when the line reaches it.
                    val edge = inset + plotWidth * progress() + pointRadius
                    clipRect(right = edge) { drawPath(line, lineColor, style = lineStroke) }
                    points.forEach { point ->
                        if (point != null && point.x <= edge) drawCircle(lineColor, pointRadius, point)
                    }
                }
            }
    )
}

/** The first and the last day of the period, under the ends of the line. */
@Composable
private fun DateLabels(days: List<DayResult>) {
    val dayFormat = rememberDateFormat(DateSkeletons.DAY_MONTH_NUMBERS)
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }
    ) {
        ChartLabel(days.first().date.format(dayFormat))
        ChartLabel(days.last().date.format(dayFormat))
    }
}

/** 100%, 50% and 0% beside the three lines of the grid, so the height of a point can be read. */
@Composable
private fun PercentAxis() {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
        modifier = Modifier.height(ChartHeight).clearAndSetSemantics { }
    ) {
        listOf(FULL_PERCENT.toInt(), HALF_PERCENT, 0).forEach { percent ->
            ChartLabel(stringResource(R.string.reports_percent, percent))
        }
    }
}

@Composable
private fun ChartLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
