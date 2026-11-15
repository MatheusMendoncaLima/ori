package com.oriteam.ori.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.list.OriMenuRow
import com.oriteam.ori.ui.theme.OriAreaAccents
import com.oriteam.ori.ui.theme.areaAccent

/**
 * "Ocultar apps", as in One UI's settings of the home screen: a list of every app with a box, the hidden ones checked.
 * A hidden app leaves the home screen and all the apps, and stays installed; unchecked, it comes back to all the apps.
 * The row says how many are hidden.
 */
@Composable
internal fun HiddenAppsOption(state: LauncherUiState, editor: HomeBehaviorEditor) {
    var isChoosing by rememberSaveable { mutableStateOf(false) }
    val count = state.hiddenApps.size
    OriMenuRow(
        title = stringResource(R.string.launcher_hide_apps),
        icon = OriIcons.HideApps,
        accent = areaAccent(OriAreaAccents.PEOPLE),
        description = if (count == 0) {
            stringResource(R.string.launcher_hide_apps_none)
        } else {
            pluralStringResource(R.plurals.launcher_hidden_apps_count, count, count)
        },
        onClick = { isChoosing = true }
    )
    if (!isChoosing) return
    // Every app, hidden or not, in alphabetical order: the hidden ones are checked.
    val apps = remember(state.apps, state.hiddenApps) {
        (state.apps + state.hiddenApps).sortedBy { it.label.lowercase() }
    }
    AppChecklist(
        texts = ChecklistTexts(
            title = stringResource(R.string.launcher_hide_apps),
            hint = stringResource(R.string.launcher_hide_apps_hint)
        ),
        apps = apps,
        checked = state.hiddenApps.map { it.key },
        isMonochrome = state.settings.isMonochrome,
        onDone = { hidden ->
            isChoosing = false
            editor.onHiddenAppsChange(hidden.toSet())
        },
        onDismiss = { isChoosing = false }
    )
}
