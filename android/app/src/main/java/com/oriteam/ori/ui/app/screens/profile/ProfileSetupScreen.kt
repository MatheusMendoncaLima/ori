package com.oriteam.ori.ui.app.screens.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.auth.AuthenticatedUser
import com.oriteam.ori.ui.app.screens.auth.AuthFormScaffold
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.LoadingButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

@Composable
fun ProfileSetupRoute(
    user: AuthenticatedUser,
    onSignOut: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel()
) {
    LaunchedEffect(user) { viewModel.start(user) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileSetupScreen(
        state = state,
        onNameInputChange = viewModel::onNameInputChange,
        onSaveClick = viewModel::onSaveClick,
        onSignOut = onSignOut
    )
}

/**
 * Shown when an account has no profile: an account created before profiles existed, or a
 * registration whose profile could not be saved. Without a profile there is no role and no access.
 */
@Composable
fun ProfileSetupScreen(
    state: ProfileSetupUiState,
    onNameInputChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onSignOut: () -> Unit
) {
    AuthFormScaffold {
        Text(
            text = stringResource(R.string.profile_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() }
        )
        Spacer(modifier = Modifier.height(OriSpacing.ExtraSmall))
        Text(
            text = stringResource(R.string.profile_setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(OriSpacing.Large))

        LabeledTextField(
            label = stringResource(R.string.register_name_label),
            value = state.nameInputValue,
            onValueChange = onNameInputChange,
            placeholder = stringResource(R.string.register_name_placeholder),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions.Default
        )
        Spacer(modifier = Modifier.height(OriSpacing.Medium))

        FormMessage(messageRes = state.errorMessageRes, type = FormMessageType.ERROR)
        Spacer(modifier = Modifier.height(OriSpacing.Large))
        LoadingButton(
            text = stringResource(R.string.profile_setup_save),
            isLoading = state.isSaving,
            onClick = onSaveClick
        )
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        OriTextButton(text = stringResource(R.string.account_sign_out), onClick = onSignOut)
    }
}
