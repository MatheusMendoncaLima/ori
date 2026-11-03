package com.oriteam.ori.ui.app.screens.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.common.AppLocale
import com.oriteam.ori.domain.reports.DayResult
import com.oriteam.ori.domain.reports.ReportPeriod
import com.oriteam.ori.domain.reports.RoutineReport
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.navigation.OriFilterTab
import com.oriteam.ori.ui.components.navigation.OriFilterTabs
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.StatefulContent
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriAreaAccents
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("dd/MM", AppLocale)
private const val FULL_PERCENT = 100f

@Composable
fun ReportsRoute(
    ownerUid: String,
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(key = "reports-$ownerUid")
) {
    LaunchedEffect(ownerUid) { viewModel.show(ownerUid) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(title = stringResource(R.string.reports_title), onNavigateBack = onNavigateBack)
        CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
            OriFilterTabs(
                tabs = listOf(
                    OriFilterTab(ReportPeriod.WEEK, stringResource(R.string.reports_this_week)),
                    OriFilterTab(ReportPeriod.MONTH, stringResource(R.string.reports_this_month))
                ),
                selected = state.period,
                onSelect = viewModel::onPeriodChange
            )
        }
        StatefulContent(
            state = state.report,
            onRetry = viewModel::onRetry,
            emptyContent = {
                EmptyState(
                    title = stringResource(R.string.reports_no_data_title),
                    message = stringResource(R.string.reports_no_data_message)
                )
            },
            modifier = Modifier.weight(1f)
        ) { report ->
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) { ReportContent(report) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportContent(report: RoutineReport) {
    var showsDetails by rememberSaveable { mutableStateOf(false) }
    val accents = LocalOriColors.current.accents
    Spacer(modifier = Modifier.height(OriSpacing.Small))
    SectionHeader(stringResource(R.string.reports_summary))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        maxItemsInEachRow = 3,
        modifier = Modifier.fillMaxWidth()
    ) {
        // The same height in a row, whatever line each label breaks into.
        val tile = Modifier.weight(1f).widthIn(min = 96.dp).fillMaxRowHeight()
        // Each tile has the color of the area it counts, the same as in the menus.
        val routine = accents[OriAreaAccents.ROUTINE].container
        SummaryTile(report.done.toString(), stringResource(R.string.reports_tasks_done), routine, tile)
        SummaryTile(
            stringResource(R.string.reports_percent, report.completionPercent),
            stringResource(R.string.reports_completion),
            routine,
            tile
        )
        SummaryTile(
            report.speechCount.toString(),
            stringResource(R.string.reports_aac_usage),
            accents[OriAreaAccents.COMMUNICATION].container,
            tile
        )
    }
    Text(
        text = stringResource(R.string.reports_aac_usage_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = OriSpacing.Small)
    )
    Spacer(modifier = Modifier.height(OriSpacing.Medium))
    SectionHeader(stringResource(R.string.reports_daily))
    OriCard(modifier = Modifier.fillMaxWidth()) {
        DailyChart(days = report.daysWithActivities)
    }
    Spacer(modifier = Modifier.height(OriSpacing.Medium))
    OriSecondaryButton(
        text = stringResource(if (showsDetails) R.string.reports_details_hide else R.string.reports_details_show),
        onClick = { showsDetails = !showsDetails }
    )
    if (showsDetails) {
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        report.daysWithActivities.forEach { day -> DayLine(day) }
    }
    Spacer(modifier = Modifier.height(OriSpacing.Large))
}

@Composable
private fun SummaryTile(value: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            // Read as one thing ("15, Atividades concluídas"), not as two loose texts.
            .semantics(mergeDescendants = true) {}
            .clip(MaterialTheme.shapes.large)
            .background(color)
            .padding(OriSpacing.Small)
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The line of the wireframe. The same values are in the details, for who cannot read a chart. */
@Composable
private fun DailyChart(days: List<DayResult>) {
    val description = stringResource(R.string.reports_chart_description)
    val lineColor = LocalOriColors.current.strongOnSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = description }
    ) {
        repeat(3) { index ->
            val y = size.height * index / 2
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        if (days.isEmpty()) return@Canvas
        val points = days.mapIndexed { index, day ->
            val x = if (days.size == 1) size.width / 2 else size.width * index / (days.size - 1)
            Offset(x, size.height * (1 - day.percent / FULL_PERCENT))
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(line, lineColor, style = Stroke(width = 3.dp.toPx()))
        points.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = it) }
    }
    if (days.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = days.first().date.format(dayFormat),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = days.last().date.format(dayFormat),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayLine(day: DayResult) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OriSpacing.ExtraSmall)
    ) {
        Text(text = day.date.format(dayFormat), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = pluralStringResource(R.plurals.reports_day_line, day.total, day.done, day.total),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
