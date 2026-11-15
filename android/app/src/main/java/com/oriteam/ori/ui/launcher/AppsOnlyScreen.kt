package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.wallpaperShown
import com.oriteam.ori.ui.app.screens.today.rememberCurrentMinute
import com.oriteam.ori.ui.components.text.DateSkeletons
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.rememberDateFormat
import com.oriteam.ori.ui.components.text.rememberTimeFormat
import com.oriteam.ori.ui.theme.OriDimensions
import java.time.LocalDate

/** The apps sit on a sheet this opaque: they read on any wallpaper, which still shows a little through it. */
private const val APPS_ONLY_SHEET_OPACITY = 0.9f

/**
 * ORI Home with only all the apps ("Só todos os apps"), for whoever finds the pages, the dock and the widgets too much:
 * the wallpaper and, over it, the time and the date, the way to the options, the search and every app (as a list or
 * pages, as chosen). Nothing is carried to a home screen there is not; back and Home leave it as it is. The items of
 * the home screen are kept for when the person goes back to it.
 */
@Composable
internal fun AppsOnlyPane(state: LauncherUiState, viewModel: LauncherViewModel, metrics: HomeMetrics) {
    val settings = state.settings
    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackground(settings.wallpaperShown)
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = APPS_ONLY_SHEET_OPACITY),
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize()
        ) {
            // The writing color of the theme, as in the sheet of all the apps.
            CompositionLocalProvider(LocalHomeInk provides null) {
                Box(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                    AppDrawer(
                        state = state,
                        viewModel = viewModel,
                        sheet = null,
                        carry = null,
                        metrics = metrics,
                        header = { AppsOnlyHeader(onOptions = { viewModel.onOpenPane(LauncherPane.OPTIONS) }) }
                    )
                }
            }
        }
    }
}

/** The time and the date (read together), and the button of the options, the only way to them here. */
@Composable
private fun AppsOnlyHeader(onOptions: () -> Unit) {
    val time = rememberCurrentMinute().format(rememberTimeFormat())
    val date = LocalDate.now().format(rememberDateFormat(DateSkeletons.WEEKDAY_DAY_MONTH))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(max = OriDimensions.ContentMaxWidth).fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { }) {
            Text(text = time, style = MaterialTheme.typography.displaySmall.copy(fontWeight = ClockTimeStyleWeight))
            Text(text = date, style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = onOptions) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.launcher_options))
        }
    }
}
