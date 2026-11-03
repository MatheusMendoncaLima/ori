package com.oriteam.ori.ui.app.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.local.LocalDataMoveResult
import com.oriteam.ori.domain.profile.UserProfile
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.app.screens.carerecipients.CareRecipientsRoute
import com.oriteam.ori.ui.app.screens.preferences.SettingsRoute
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.list.OriMenuRow
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.navigation.rememberOpenSubscreen
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val HeaderAvatarSize = 64.dp

/** The screens reached from the profile. */
private enum class ProfilePane {
    PERSONAL,
    CAREGIVERS,
    CARE_RECIPIENTS,
    SETTINGS,
    CONNECT
}

/**
 * Perfil: who is using the app and the way to everything about them. Without an account the
 * people-related rows give way to the invitation to create one, because links need an account.
 */
@Composable
fun ProfileRoute(profile: UserProfile, actions: ProfileActions) {
    val subscreen = rememberOpenSubscreen<ProfilePane>()
    val back = subscreen::close

    when (subscreen.current) {
        null -> ProfileScreen(profile = profile, actions = actions, onOpen = subscreen::open)
        ProfilePane.PERSONAL -> PersonalDataRoute(profile = profile, actions = actions, onNavigateBack = back)
        ProfilePane.CAREGIVERS -> CaregiversRoute(profile = profile, onNavigateBack = back)
        ProfilePane.CARE_RECIPIENTS -> CareRecipientsRoute(profile = profile, onNavigateBack = back)
        ProfilePane.SETTINGS -> SettingsRoute(
            isLocal = profile.isLocal,
            onEraseLocalData = actions.onEraseLocalData,
            onNavigateBack = back
        )
        ProfilePane.CONNECT -> ConnectAccountRoute(onNavigateBack = back)
    }
}

@Composable
private fun ProfileScreen(profile: UserProfile, actions: ProfileActions, onOpen: (ProfilePane) -> Unit) {
    var isConfirmingSignOut by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        // The same title bar as every screen, so "Aa" is where it always is.
        OriTopBar(title = stringResource(R.string.main_profile))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                ProfileHeader(
                    profile = profile,
                    photoPath = actions.photoPath,
                    onEdit = { onOpen(ProfilePane.PERSONAL) }
                )
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                actions.localDataMove?.let { result ->
                    LocalDataMoveNotice(result = result, onRetry = actions.onRetryLocalDataMove)
                    Spacer(modifier = Modifier.height(OriSpacing.Medium))
                }
                if (profile.isLocal) {
                    LocalProfileCard(onConnect = { onOpen(ProfilePane.CONNECT) })
                    Spacer(modifier = Modifier.height(OriSpacing.Medium))
                }
                Column(verticalArrangement = Arrangement.spacedBy(OriSpacing.Small)) {
                    OriMenuRow(
                        title = stringResource(R.string.profile_personal),
                        icon = Icons.Filled.Person,
                        accentIndex = 0,
                        onClick = { onOpen(ProfilePane.PERSONAL) }
                    )
                    if (!profile.isLocal) {
                        OriMenuRow(
                            title = stringResource(R.string.profile_caregivers),
                            description = stringResource(R.string.profile_caregivers_description),
                            icon = OriIcons.People,
                            accentIndex = 3,
                            onClick = { onOpen(ProfilePane.CAREGIVERS) }
                        )
                        OriMenuRow(
                            title = stringResource(R.string.main_care_recipients),
                            description = stringResource(R.string.profile_care_recipients_description),
                            icon = Icons.Filled.Favorite,
                            accentIndex = 5,
                            onClick = { onOpen(ProfilePane.CARE_RECIPIENTS) }
                        )
                    }
                    OriMenuRow(
                        title = stringResource(R.string.profile_settings),
                        icon = Icons.Filled.Settings,
                        accentIndex = 1,
                        onClick = { onOpen(ProfilePane.SETTINGS) }
                    )
                }
                if (!profile.isLocal) {
                    Spacer(modifier = Modifier.height(OriSpacing.Medium))
                    // Leaving deletes nothing, so it is not red; it still asks, because it is easy to tap by mistake.
                    OriTextButton(
                        text = stringResource(R.string.account_sign_out),
                        onClick = { isConfirmingSignOut = true }
                    )
                }
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
    }
    if (isConfirmingSignOut) {
        OriConfirmationDialog(
            title = stringResource(R.string.account_sign_out_title),
            message = stringResource(R.string.account_sign_out_message),
            confirmLabel = stringResource(R.string.account_sign_out),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                isConfirmingSignOut = false
                actions.onSignOut()
            },
            onDismiss = { isConfirmingSignOut = false }
        )
    }
}

/** The colored card of the wireframe, below the title bar: picture, name, kind of profile and "Editar perfil". */
@Composable
private fun ProfileHeader(profile: UserProfile, photoPath: String?, onEdit: () -> Unit) {
    val colors = LocalOriColors.current
    val name = profile.displayName.ifBlank { stringResource(R.string.profile_no_name) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(colors.strong, colors.strongSoft)),
                MaterialTheme.shapes.large
            )
            .padding(OriSpacing.Medium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(photoPath = photoPath, size = HeaderAvatarSize)
            Spacer(modifier = Modifier.width(OriSpacing.Medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onStrong,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(
                        if (profile.isLocal) R.string.profile_on_this_device else R.string.profile_account_label
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onStrong
                )
            }
        }
        Spacer(modifier = Modifier.height(OriSpacing.Medium))
        Button(
            onClick = onEdit,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = OriDimensions.ControlMinHeight)
        ) {
            Text(text = stringResource(R.string.profile_edit), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LocalProfileCard(onConnect: () -> Unit) {
    OriCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.profile_local_title), style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(R.string.profile_local_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        OriPrimaryButton(text = stringResource(R.string.profile_connect), onClick = onConnect)
    }
}

@Composable
private fun LocalDataMoveNotice(result: LocalDataMoveResult, onRetry: () -> Unit) {
    val message = when (result) {
        LocalDataMoveResult.COMPLETED -> return
        LocalDataMoveResult.WAITING_FOR_CONNECTION -> R.string.profile_move_waiting
        LocalDataMoveResult.SOME_ITEMS_DID_NOT_FIT -> R.string.profile_move_partial
    }
    OriCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(message), style = MaterialTheme.typography.bodyMedium)
        OriTextButton(text = stringResource(R.string.profile_move_retry), onClick = onRetry)
    }
}
