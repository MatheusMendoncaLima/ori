package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.style.TextOverflow
import com.oriteam.ori.R
import com.oriteam.ori.ui.app.screens.today.rememberCurrentMinute
import com.oriteam.ori.ui.components.text.DateSkeletons
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberDateFormat
import com.oriteam.ori.ui.components.text.rememberTimeFormat
import com.oriteam.ori.ui.theme.OriSpacing
import java.time.LocalDate

/**
 * The time and the date, read as one sentence. The screen reader finds every way out (all the apps, the options,
 * editing, the panel) as an action of the time; "Mostrar botões" shows a button to edit at the top of the screen.
 */
@Composable
internal fun HomeClock(actions: ClockActions, modifier: Modifier = Modifier) {
    val time = rememberCurrentMinute().format(rememberTimeFormat())
    val date = LocalDate.now().format(rememberDateFormat(DateSkeletons.WEEKDAY_DAY_MONTH))
    val labels = listOf(
        stringResource(R.string.launcher_all_apps) to actions.onAllApps,
        stringResource(R.string.launcher_customize_home) to actions.onCustomize,
        stringResource(R.string.launcher_organize_favorites) to actions.onEdit
    ) + listOfNotNull(actions.onPanel?.let { stringResource(R.string.launcher_panel_open) to it })
    Column(
        modifier = modifier.fillMaxSize().padding(top = OriSpacing.Medium).clearAndSetSemantics {
            contentDescription = "$time, $date"
            customActions = labels.map { (label, action) ->
                CustomAccessibilityAction(label) {
                    action()
                    true
                }
            }
        }
    ) {
        val shadow = LocalHomeInk.current?.shadow
        // One line each, never wrapping: what is drawn stays inside the cells the clock takes.
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = ClockTimeStyleWeight, shadow = shadow),
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium.copy(shadow = shadow),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
