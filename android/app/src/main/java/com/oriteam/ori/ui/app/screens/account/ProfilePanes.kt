package com.oriteam.ori.ui.app.screens.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.carelink.CareLinkSide
import com.oriteam.ori.domain.profile.UserProfile
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.app.screens.carelinks.CareLinksSection
import com.oriteam.ori.ui.app.screens.carelinks.CareLinksViewModel
import com.oriteam.ori.ui.app.screens.carelinks.InvitationSection
import com.oriteam.ori.ui.app.screens.carelinks.InvitationViewModel
import com.oriteam.ori.ui.app.screens.common.messageRes
import com.oriteam.ori.ui.app.screens.login.LoginRoute
import com.oriteam.ori.ui.app.screens.register.RegisterRoute
import com.oriteam.ori.ui.components.form.EqualWidthRow
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.LoadingButton
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.text.LocalTextCaseToggle
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val PhotoPreviewSize = 112.dp

/** "Dados pessoais": the picture and the name, which is all the app asks about a person. */
@Composable
fun PersonalDataRoute(
    profile: UserProfile,
    actions: ProfileActions,
    onNavigateBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(profile.displayName) }
    var errorRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var noticeRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    val save = {
        val trimmed = name.trim()
        noticeRes = null
        if (trimmed.isEmpty()) {
            errorRes = R.string.error_missing_profile_name
        } else if (!isSaving) {
            isSaving = true
            actions.onRename(trimmed) { error ->
                isSaving = false
                errorRes = error?.messageRes()
                noticeRes = R.string.profile_name_saved.takeIf { error == null }
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(title = stringResource(R.string.profile_personal), onNavigateBack = onNavigateBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
                ProfilePhotoChoice(actions)
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                LabeledTextField(
                    label = stringResource(R.string.profile_name_label),
                    value = name,
                    onValueChange = {
                        name = it
                        errorRes = null
                        noticeRes = null
                    },
                    placeholder = stringResource(R.string.register_name_placeholder),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { save() })
                )
                FormMessage(messageRes = errorRes, type = FormMessageType.ERROR)
                FormMessage(messageRes = noticeRes, type = FormMessageType.SUCCESS)
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                LoadingButton(text = stringResource(R.string.profile_save), isLoading = isSaving, onClick = save)
            }
        }
    }
}

/** The system photo picker needs no permission and gives the app only the picture chosen. */
@Composable
private fun ProfilePhotoChoice(actions: ProfileActions) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { actions.onPhotoPicked(it.toString()) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ProfileAvatar(photoPath = actions.photoPath, size = PhotoPreviewSize)
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        EqualWidthRow(modifier = Modifier.fillMaxWidth()) {
            OriSecondaryButton(
                text = stringResource(
                    if (actions.photoPath == null) R.string.profile_photo_choose else R.string.profile_photo_change
                ),
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
            if (actions.photoPath != null) {
                OriSecondaryButton(
                    text = stringResource(R.string.profile_photo_remove),
                    onClick = actions.onRemovePhoto,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        Text(
            text = stringResource(R.string.profile_photo_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FormMessage(messageRes = actions.photoErrorRes, type = FormMessageType.ERROR)
    }
}

/** "Responsáveis vinculados": who supports this person, what they may do, and how to add one. */
@Composable
fun CaregiversRoute(
    profile: UserProfile,
    onNavigateBack: () -> Unit,
    careLinksViewModel: CareLinksViewModel = hiltViewModel(),
    invitationViewModel: InvitationViewModel = hiltViewModel()
) {
    LaunchedEffect(profile.uid) { careLinksViewModel.start(profile.uid, CareLinkSide.PRIMARY_USER) }
    val linksState by careLinksViewModel.uiState.collectAsStateWithLifecycle()
    val invitationState by invitationViewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(title = stringResource(R.string.profile_caregivers), onNavigateBack = onNavigateBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.ContentMaxWidth) {
                CareLinksSection(
                    state = linksState,
                    onRevokeRequest = careLinksViewModel::onRevokeRequest,
                    onRevokeConfirm = careLinksViewModel::onRevokeConfirm,
                    onRevokeDismiss = careLinksViewModel::onRevokeDismiss,
                    onManagePermissionChange = careLinksViewModel::onManagePermissionChange,
                    onCommunicationPermissionChange = careLinksViewModel::onCommunicationPermissionChange,
                    onRetry = careLinksViewModel::onRetry
                )
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                InvitationSection(
                    side = CareLinkSide.PRIMARY_USER,
                    state = invitationState,
                    onCreateInvitationClick = invitationViewModel::onCreateInvitationClick,
                    onCodeInputChange = invitationViewModel::onCodeInputChange,
                    onAcceptInvitationClick = invitationViewModel::onAcceptInvitationClick
                )
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
    }
}

/**
 * Sign in or sign up from inside the app, for someone using it without an account. The notice is
 * the consent: it says that the data leaves the device, before the person chooses to continue.
 * After signing in nothing is navigated here: the session replaces this area by itself.
 */
@Composable
fun ConnectAccountRoute(onNavigateBack: () -> Unit) {
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(title = stringResource(R.string.connect_title), onNavigateBack = onNavigateBack)
        CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
            Text(
                text = stringResource(R.string.connect_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            // The top bar above already has "Aa"; the forms would show a second one.
            CompositionLocalProvider(LocalTextCaseToggle provides null) {
                if (isRegistering) {
                    RegisterRoute(onNavigateToLogin = { isRegistering = false })
                } else {
                    LoginRoute(onLoginSuccess = {}, onNavigateToRegister = { isRegistering = true })
                }
            }
        }
    }
}
