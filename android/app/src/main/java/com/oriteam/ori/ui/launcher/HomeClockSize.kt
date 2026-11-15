package com.oriteam.ori.ui.launcher

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import com.oriteam.ori.ui.components.text.DateSkeletons
import com.oriteam.ori.ui.components.text.rememberDateFormat
import com.oriteam.ori.ui.components.text.rememberTimeFormat
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.LocalDate
import java.time.LocalTime

/** A wide time ("23:58", or "11:58 PM"), so the clock never grows as the minutes pass. */
private val WideTime = LocalTime.of(23, 58)

private const val SAMPLE_YEAR = 2026
private const val MONTHS = 12

/** Every weekday of every month (a week from the 22nd), so no day of the year is wider than the room measured. */
private val SampleDays = (1..MONTHS).flatMap { month -> (22..28).map { LocalDate.of(SAMPLE_YEAR, month, it) } }

/**
 * Lays the content out at [natural] (the size the clock needs) and draws it smaller, never larger, when the room it
 * is given is smaller: the clock keeps the cells it has upright, which are lower on a tablet turned.
 */
internal fun Modifier.scaledToFit(natural: DpSize): Modifier = layout { measurable, constraints ->
    val width = natural.width.roundToPx().coerceAtLeast(1)
    val height = natural.height.roundToPx().coerceAtLeast(1)
    val placeable = measurable.measure(Constraints.fixed(width, height))
    val scale = minOf(1f, constraints.maxWidth.toFloat() / width, constraints.maxHeight.toFloat() / height)
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

/** The time and the date as the clock writes them. */
internal val ClockTimeStyleWeight = FontWeight.Light

/**
 * The room the clock widget of ORI needs: the widest time and the widest date of the person's language, written as
 * the clock writes them (the person's text size and, with "Aa", in capitals), with the clock's own margins. Measured
 * once (not each minute or day), so the widget keeps its size and its cells; the clock draws inside them.
 */
@Composable
internal fun rememberClockSize(): DpSize {
    val measurer = rememberTextMeasurer()
    val timeFormat = rememberTimeFormat()
    val dateFormat = rememberDateFormat(DateSkeletons.WEEKDAY_DAY_MONTH)
    val timeStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = ClockTimeStyleWeight)
    val dateStyle = MaterialTheme.typography.titleMedium
    val isUppercase = LocalOriAppearance.current.isUppercase
    val density = LocalDensity.current
    return remember(timeFormat, dateFormat, timeStyle, dateStyle, isUppercase, density) {
        fun written(text: String) = if (isUppercase) text.uppercase() else text
        val time = measurer.measure(written(WideTime.format(timeFormat)), timeStyle, maxLines = 1).size
        val dates = SampleDays.map { measurer.measure(written(it.format(dateFormat)), dateStyle, maxLines = 1).size }
        val widthPx = maxOf(time.width, dates.maxOf { it.width })
        val heightPx = time.height + dates.maxOf { it.height }
        with(density) {
            DpSize(widthPx.toDp() + OriSpacing.Small * 2, heightPx.toDp() + OriSpacing.Medium)
        }
    }
}
