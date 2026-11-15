package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.LauncherPanel
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.domain.session.SessionState
import com.oriteam.ori.ui.adaptive.LocalAllowsTwoPanes
import com.oriteam.ori.ui.app.screens.aac.AacRoute
import com.oriteam.ori.ui.app.screens.legal.ConsentStatus
import com.oriteam.ori.ui.app.screens.legal.ConsentViewModel
import com.oriteam.ori.ui.app.screens.routine.RoutineRoute
import com.oriteam.ori.ui.app.session.SessionViewModel
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.LoadingState
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * The panel to the left of the home screen, in the place of the news of other phones: the routine of today or the
 * phrases of ORI, working right there (checking an activity, saying a phrase). It is the same screen as in ORI, for
 * the same person and with the same rules: only an active session that already agreed to the terms (J2) shows it;
 * otherwise it asks to open ORI, where signing in and agreeing happen. It is composed only while it is on the screen,
 * so the routine is not followed while the panel is closed.
 */
@Composable
internal fun OriPanel(panel: LauncherPanel, onOpenOri: (StartArea?) -> Unit) {
    val area = if (panel == LauncherPanel.PHRASES) StartArea.COMMUNICATION else StartArea.ROUTINE
    val session: SessionViewModel = hiltViewModel()
    val state by session.state.collectAsStateWithLifecycle()
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        // One column, also on a tablet: an activity opens over the list, as on a phone.
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            CompositionLocalProvider(LocalAllowsTwoPanes provides false) {
                when (val current = state) {
                    is SessionState.Active -> AgreedPanel(current.profile.uid, panel) { onOpenOri(area) }
                    SessionState.Initializing -> LoadingState()
                    else -> OpenOriFirst { onOpenOri(area) }
                }
            }
        }
    }
}

@Composable
private fun AgreedPanel(ownerUid: String, panel: LauncherPanel, onOpenOri: () -> Unit) {
    val consent: ConsentViewModel = hiltViewModel(key = "consent-$ownerUid")
    LaunchedEffect(ownerUid) { consent.start(ownerUid) }
    val status by consent.uiState.collectAsStateWithLifecycle()
    when (status.status) {
        ConsentStatus.ACCEPTED -> when (panel) {
            // "Abrir no ORI" in the title row of the routine: one bar, as in ORI.
            LauncherPanel.ROUTINE -> RoutineRoute(
                ownerUid = ownerUid,
                viewer = RoutineViewer.Owner,
                title = stringResource(R.string.shortcut_routine),
                topBarActions = { OpenInOriButton(onOpenOri) }
            )
            // The phrases have no title row: "Abrir no ORI" gets a row of its own over them.
            LauncherPanel.PHRASES -> Column(modifier = Modifier.fillMaxSize()) {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    OpenInOriButton(onOpenOri)
                }
                Box(modifier = Modifier.weight(1f)) { AacRoute(ownerUid = ownerUid) }
            }
            LauncherPanel.NONE -> Unit
        }
        ConsentStatus.CHECKING -> LoadingState()
        ConsentStatus.REQUIRED, ConsentStatus.UNAVAILABLE -> OpenOriFirst(onOpenOri)
    }
}

/** "Abrir no ORI": the same area (the routine or the phrases) in ORI itself, with all of it. */
@Composable
private fun OpenInOriButton(onOpenOri: () -> Unit) {
    OriTextButton(
        text = stringResource(R.string.launcher_panel_open_ori),
        onClick = onOpenOri,
        isFullWidth = false,
        modifier = Modifier.padding(horizontal = OriSpacing.Small)
    )
}

/** Signing in, choosing to go on without an account and agreeing happen in ORI, never in the panel. */
@Composable
private fun OpenOriFirst(onOpenOri: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        EmptyState(
            title = stringResource(R.string.launcher_panel_start_title),
            message = stringResource(R.string.launcher_panel_start_message),
            actionLabel = stringResource(R.string.launcher_open_ori),
            onAction = onOpenOri
        )
    }
}
