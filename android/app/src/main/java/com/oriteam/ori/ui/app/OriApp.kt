package com.oriteam.ori.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.domain.session.SessionState
import com.oriteam.ori.ui.app.main.MainScaffold
import com.oriteam.ori.ui.app.screens.account.ProfileActions
import com.oriteam.ori.ui.app.screens.account.ProfilePhotoViewModel
import com.oriteam.ori.ui.app.screens.common.messageRes
import com.oriteam.ori.ui.app.screens.home.HomeRoute
import com.oriteam.ori.ui.app.screens.profile.ProfileSetupRoute
import com.oriteam.ori.ui.app.session.SessionViewModel
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.state.ErrorState
import com.oriteam.ori.ui.components.state.LoadingState

/**
 * Root of the app. The visible area follows the session, never the other way around:
 * signing in, creating the profile or signing out changes [SessionState] and this switches.
 */
@Composable
fun OriApp(startArea: StartArea, sessionViewModel: SessionViewModel = hiltViewModel()) {
    val session by sessionViewModel.state.collectAsStateWithLifecycle()
    val localDataMove by sessionViewModel.localDataMove.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val state = session) {
            SessionState.Initializing -> LoadingState(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
            SessionState.SignedOut -> HomeRoute(onContinueWithoutAccount = sessionViewModel::onContinueWithoutAccount)
            is SessionState.ProfileRequired -> ProfileSetupRoute(
                user = state.user,
                onSignOut = sessionViewModel::signOut
            )
            is SessionState.ProfileUnavailable -> ProfileUnavailableScreen(
                messageRes = state.error.messageRes(),
                onRetry = sessionViewModel::retry,
                onSignOut = sessionViewModel::signOut
            )
            is SessionState.Active -> {
                val profile = state.profile
                LaunchedEffect(profile) { if (!profile.isLocal) sessionViewModel.onAccountActive(profile) }
                val photoViewModel: ProfilePhotoViewModel = hiltViewModel()
                LaunchedEffect(profile.uid) { photoViewModel.show(profile.uid) }
                val photoPath by photoViewModel.photoPath.collectAsStateWithLifecycle()
                val photoErrorRes by photoViewModel.errorRes.collectAsStateWithLifecycle()
                MainScaffold(
                    profile = profile,
                    startArea = startArea,
                    profileActions = ProfileActions(
                        localDataMove = localDataMove,
                        onRetryLocalDataMove = { sessionViewModel.onRetryLocalDataMove(profile) },
                        onSignOut = sessionViewModel::signOut,
                        onEraseLocalData = sessionViewModel::eraseLocalData,
                        onRename = { name, onResult -> sessionViewModel.rename(profile, name, onResult) },
                        photoPath = photoPath,
                        photoErrorRes = photoErrorRes,
                        onPhotoPicked = photoViewModel::onPhotoPicked,
                        onRemovePhoto = photoViewModel::onRemovePhoto
                    )
                )
            }
        }
    }
}

/** The account is signed in, but its profile could not be read. Leaving is always possible. */
@Composable
private fun ProfileUnavailableScreen(messageRes: Int, onRetry: () -> Unit, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f)) {
            ErrorState(message = stringResource(messageRes), onRetry = onRetry)
        }
        OriTextButton(text = stringResource(R.string.account_sign_out), onClick = onSignOut)
    }
}
