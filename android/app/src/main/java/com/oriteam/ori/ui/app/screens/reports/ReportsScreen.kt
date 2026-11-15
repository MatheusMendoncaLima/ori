package com.oriteam.ori.ui.app.screens.reports

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.reports.DayResult
import com.oriteam.ori.domain.reports.ReportPeriod
import com.oriteam.ori.domain.reports.RoutineReport
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.motion.entrance
import com.oriteam.ori.ui.components.navigation.OriFilterTab
import com.oriteam.ori.ui.components.navigation.OriFilterTabs
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.StatefulContent
import com.oriteam.ori.ui.components.text.DateSkeletons
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberDateFormat
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriAreaAccents
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.appear
import com.oriteam.ori.ui.theme.disappear

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
                    icon = ImageVector.vectorResource(R.drawable.ic_chart),
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
        // Three colors, as in the wireframe, the same as in the menus: what was done is the Routine's, the share
        // of the plan is the Reports' own measure, and the use of the board is the Communication's.
        SummaryTile(
            report.done.toString(),
            pluralStringResource(R.plurals.reports_tasks_done, report.done),
            accents[OriAreaAccents.ROUTINE].container,
            tile.entrance(order = 0)
        )
        SummaryTile(
            stringResource(R.string.reports_percent, report.completionPercent),
            stringResource(R.string.reports_completion),
            accents[OriAreaAccents.REPORTS].container,
            tile.entrance(order = 1)
        )
        SummaryTile(
            report.speechCount.toString(),
            pluralStringResource(R.plurals.reports_aac_usage, report.speechCount),
            accents[OriAreaAccents.COMMUNICATION].container,
            tile.entrance(order = 2)
        )
    }
    Text(
        text = stringResource(R.string.reports_aac_usage_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // The whole width, so it starts where the tiles start instead of being centered under them.
        modifier = Modifier.fillMaxWidth().padding(top = OriSpacing.Small)
    )
    Spacer(modifier = Modifier.height(OriSpacing.Medium))
    SectionHeader(stringResource(R.string.reports_daily))
    OriCard(modifier = Modifier.fillMaxWidth()) {
        DailyChart(days = report.days)
    }
    Spacer(modifier = Modifier.height(OriSpacing.Medium))
    OriSecondaryButton(
        text = stringResource(if (showsDetails) R.string.reports_details_hide else R.string.reports_details_show),
        onClick = { showsDetails = !showsDetails }
    )
    val motion = LocalOriAppearance.current.motion
    AnimatedVisibility(visible = showsDetails, enter = motion.appear(), exit = motion.disappear(), label = "Details") {
        Column {
            Spacer(modifier = Modifier.height(OriSpacing.Small))
            report.daysWithActivities.forEach { day -> DayLine(day) }
        }
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

@Composable
private fun DayLine(day: DayResult) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OriSpacing.ExtraSmall)
    ) {
        Text(
            text = day.date.format(rememberDateFormat(DateSkeletons.DAY_MONTH_NUMBERS)),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = pluralStringResource(R.plurals.reports_day_line, day.total, day.done, day.total),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
